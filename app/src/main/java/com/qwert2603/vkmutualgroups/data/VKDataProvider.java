package com.qwert2603.vkmutualgroups.data;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.qwert2603.vkmutualgroups.Listener;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKUsersArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Загрузчик данных их VK (через vkapi).
 */
public class VKDataProvider implements DataProvider {

    @SuppressWarnings("unused")
    public static final String TAG = "VKDataProvider";

    /**
     * Задержка перед следующим запросом.
     * Чтобы запросы не посылались слишком часто. (Не больше 3 в секунду).
     */
    public static final long nextRequestDelay = 350;

    /**
     * Объект-сохранятель json в память устройства.
     * Если == null, сохранение не происходит.
     */
    private volatile DataSaver mDataSaver;

    public VKDataProvider(@Nullable DataSaver dataSaver) {
        mDataSaver = dataSaver;
    }

    /**
     * Загрузить друзей, группы пользователя и инфо о друзьях в группах.
     */
    @Override
    public void load(Listener<Data> listener) {
        Data data = new Data();
        loadFriends(data, listener);
    }

    private void loadFriends(Data data, Listener<Data> listener) {
        VKParameters friendsParameters = VKParameters.from(VKApiConst.FIELDS, "photo_50, can_write_private_message");
        VKRequest requestFriends = VKApi.friends().get(friendsParameters);
        requestFriends.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                data.mFriends = (VKUsersArray) response.parsedModel;
                data.mFriends.fields = response.json;
                new Handler(Looper.getMainLooper()).postDelayed(() -> loadGroups(data, listener), nextRequestDelay);
            }

            @Override
            public void onError(VKError error) {
                listener.onError(String.valueOf(error));
            }
        });
    }

    private void loadGroups(Data data, Listener<Data> listener) {
        VKRequest requestGroups = VKApi.groups().get(VKParameters.from(VKApiConst.EXTENDED, 1));
        requestGroups.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                data.mGroups = (VKApiCommunityArray) response.parsedModel;
                data.mGroups.fields = response.json;
                new Handler(Looper.getMainLooper()).postDelayed(() -> loadIsMember(data, listener), nextRequestDelay);
            }

            @Override
            public void onError(VKError error) {
                listener.onError(String.valueOf(error));
            }
        });
    }

    /**
     * Загрузить данные о друзьях в группах. (Переданных в data)
     * Тот же объект data с данными о друзьях в группах будет передан в listener.
     */
    public void loadIsMember(Data data, Listener<Data> listener) {
        new LoadTask(data, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class LoadTask extends AsyncTask<Void, Void, Void> {
        /**
         * Кол-во друзей, обрабатываемое в 1 запросе.
         * Не больше 500.
         */
        private static final int friendsPerRequest = 200;

        /**
         * Кол-во групп, обрабатываемое в 1 запросе.
         * Не больше 25. (ограничение в 25 запросов к api в vkapi.execute).
         */
        private static final int groupPerRequest = 25;

        /**
         * Данные с друзьями и группами, которые надо обработать.
         */
        private volatile Data mData;

        /**
         * Слушатель выполнения загрузки.
         */
        private volatile Listener<Data> mListener;

        /**
         * Ошибка, произошедшая во время {@link #doInBackground(Void...)}.
         * Если == null, то ошибок не было.
         */
        private volatile String mErrorMessage = null;

        /**
         * Сколько запросов осталось выполнить (vkapi.execute).
         */
        private volatile int mRequestsRemain;

        /**
         * Время, когда можно отправлять следующий запрос.
         */
        private volatile long mNextRequestTime = 0;

        public LoadTask(Data data, Listener<Data> listener) {
            mData = data;
            mListener = listener;
        }

        @Override
        protected Void doInBackground(Void... params) {
            mRequestsRemain = calculateRequestCount(mData);

            mData.mIsMember = new HashMap<>();
            for (int friendNumber = 0; friendNumber < mData.mFriends.size(); friendNumber += friendsPerRequest) {
                String varFriends = getVarFriends(mData, friendNumber);
                for (int groupNumber = 0; groupNumber < mData.mGroups.size(); groupNumber += groupPerRequest) {
                    if (mErrorMessage != null) {
                        --mRequestsRemain;
                        continue;
                    }
                    String varGroups = getVarGroups(mData, groupNumber);
                    String code = getCodeToExecute(varFriends, varGroups);
                    VKRequest request = new VKRequest("execute", VKParameters.from("code", code));
                    request.setUseLooperForCallListener(false);
                    request.executeWithListener(new VKRequest.VKRequestListener() {
                        @Override
                        public void onComplete(VKResponse response) {
                            try {
                                parseIsMemberJSON(response.json, mData);
                            } catch (JSONException e) {
                                Log.e(TAG, e.toString(), e);
                                mErrorMessage = String.valueOf(e);
                            }
                            --mRequestsRemain;
                        }

                        @Override
                        public void onError(VKError error) {
                            mErrorMessage = String.valueOf(error);
                            --mRequestsRemain;
                        }
                    });
                    waitNextRequestDelay();
                }
            }

            waitRequestsRemain();
            if (mErrorMessage != null) {
                return null;
            }

            if (mDataSaver != null) {
                mDataSaver.save(mData);
            }

            return null;
        }

        private void waitNextRequestDelay() {
            while (System.currentTimeMillis() < mNextRequestTime) {
                Thread.yield();
            }
            mNextRequestTime = System.currentTimeMillis() + nextRequestDelay;
        }

        /**
         * Ждем, пока не выполнятся все выполняемые запросы.
         */
        private void waitRequestsRemain() {
            while (mRequestsRemain > 0) {
                try {
                    Thread.sleep(15);
                } catch (InterruptedException ignored) {
                }
            }
        }

        /**
         * @return кол-во запросов, которое надо выполнить.
         */
        private int calculateRequestCount(Data data) {
            int partsOfFriends = data.mFriends.size() / friendsPerRequest;
            if (data.mFriends.size() % friendsPerRequest > 0) {
                ++partsOfFriends;
            }
            int partsOfGroups = data.mGroups.size() / groupPerRequest;
            if (data.mGroups.size() % groupPerRequest > 0) {
                ++partsOfGroups;
            }
            return partsOfFriends * partsOfGroups;
        }

        /**
         * Получить значение для переменной в запросе - id друзей
         * @param friendStart - друг, начиная с которого формируется список id.
         */
        private String getVarFriends(Data data, int friendStart) {
            StringBuilder s = new StringBuilder();
            s.append("\"");
            int friendEnd = Math.min(friendStart + friendsPerRequest, data.mFriends.size());
            for (int i = friendStart; i < friendEnd; ++i) {
                s.append(data.mFriends.get(i).id).append(',');
            }
            s.append("\"");
            return s.toString();
        }

        /**
         * Получить значение для переменной в запросе - id групп
         * @param groupStart - группа, начиная с которого формируется список id.
         */
        private String getVarGroups(Data data, int groupStart) {
            StringBuilder s = new StringBuilder();
            int groupEnd = Math.min(groupStart + groupPerRequest, data.mGroups.size());
            s.append("{count:\"").append(groupEnd - groupStart).append("\",items:[");
            for (int i = groupStart; i < groupEnd; ++i) {
                s.append(data.mGroups.get(i).id).append(',');
            }
            s.append("]}");
            return s.toString();
        }

        /**
         * Получить код для vkapi.execute c заданными переменными - списками id друзей и групп.
         */
        private String getCodeToExecute(String varFriends, String varGroups) {
            return "var friends = " + varFriends + ";" +
                    "var groups = " + varGroups + ";" +
                    "var res = [];" +
                    "var i = 0;" +
                    "while(i<groups.count)" +
                    "{var group_id = groups.items[i];\n" +
                    "res=res+[{\"group_id\":group_id," +
                    "\"members\":API.groups.isMember({\"group_id\": group_id,\"user_ids\":friends})}];\n" +
                    "i=i+1;}" +
                    "return res;";
        }

        /**
         * Разобрать и добавить в переданный объект Data данные об общих группах.
         */
        private void parseIsMemberJSON(JSONObject jsonObject, Data data) throws JSONException {
            JSONArray responseJSONArray = jsonObject.getJSONArray("response");
            int responseJSONArrayLength = responseJSONArray.length();
            for (int i = 0; i < responseJSONArrayLength; ++i) {
                JSONObject groupJSONObject = responseJSONArray.getJSONObject(i);
                int groupId = groupJSONObject.getInt("group_id");
                JSONArray membersJSONArray = groupJSONObject.getJSONArray("members");
                int membersJSONArrayLength = membersJSONArray.length();
                for (int j = 0; j < membersJSONArrayLength; ++j) {
                    JSONObject memberJSONObject = membersJSONArray.getJSONObject(j);
                    if (memberJSONObject.getInt("member") == 1) {
                        int friendId = memberJSONObject.getInt("user_id");
                        if (data.mIsMember.get(friendId) == null) {
                            data.mIsMember.put(friendId, new ArrayList<>());
                        }
                        data.mIsMember.get(friendId).add(groupId);
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(Void a_void) {
            if (mErrorMessage == null) {
                mListener.onCompleted(mData);
            } else {
                mListener.onError(mErrorMessage);
            }
        }
    }

}
