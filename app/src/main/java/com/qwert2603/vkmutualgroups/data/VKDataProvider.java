package com.qwert2603.vkmutualgroups.data;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
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

import org.json.JSONObject;

/**
 * Загрузчик данных их VK (через vkapi).
 */
public class VKDataProvider implements DataProvider {

    @SuppressWarnings("unused")
    public static final String TAG = "VKDataProvider";

    /**
     * Объект сохранятель json в память устройства.
     * Если == null, сохранение не происходит.
     */
    private DataSaver mDataSaver;

    public VKDataProvider(@Nullable DataSaver dataSaver) {
        mDataSaver = dataSaver;
        if (mDataSaver != null) {
            mDataSaver.clear();
        }
    }

    @Override
    public void loadFriends(Listener<VKUsersArray> listener) {
        Log.d(TAG, "loadFriends");
        VKRequest request = VKApi.friends().get(VKParameters.from(VKApiConst.FIELDS, "photo_50, can_write_private_message"));
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                Log.d(TAG, "loadFriends ## onComplete");
                listener.onCompleted((VKUsersArray) response.parsedModel);
                if (mDataSaver != null) {
                    mDataSaver.saveFriends(response.json);
                }
            }

            @Override
            public void onError(VKError error) {
                listener.onError(String.valueOf(error));
            }
        });
    }

    @Override
    public void loadGroups(Listener<VKApiCommunityArray> listener) {
        VKRequest request = VKApi.groups().get(VKParameters.from(VKApiConst.EXTENDED, 1));
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                listener.onCompleted((VKApiCommunityArray) response.parsedModel);
                if (mDataSaver != null) {
                    mDataSaver.saveGroups(response.json);
                }
            }

            @Override
            public void onError(VKError error) {
                listener.onError(String.valueOf(error));
            }
        });
    }

    @Override
    public int loadIsMembers(@NonNull VKUsersArray friends, @NonNull VKApiCommunityArray groups, LoadIsMemberListener listener) {
        LoadIsMemberTask loadIsMemberTask = new LoadIsMemberTask(friends, groups, listener);
        int requestCount = loadIsMemberTask.mRequestsRemain;
        loadIsMemberTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return requestCount;
    }

    private class LoadIsMemberTask extends AsyncTask<Void, JSONObject, Void> {
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
         * Слушатель выполнения загрузки.
         */
        private volatile LoadIsMemberListener mListener;

        /**
         * Ошибка, произошедшая во время {@link #doInBackground(Void...)}.
         * Если == null, то ошибок не было.
         */
        private volatile String mErrorMessage;

        /**
         * Сколько запросов осталось выполнить (vkapi.execute).
         */
        private volatile int mRequestsRemain;

        /**
         * Друзья, которые обрабатываются.
         */
        private VKUsersArray mFriends;

        /**
         * Группы, которые обрабатываются.
         */
        private VKApiCommunityArray mGroups;

        public LoadIsMemberTask(@NonNull VKUsersArray friends, @NonNull VKApiCommunityArray groups, LoadIsMemberListener listener) {
            mListener = listener;
            mFriends = friends;
            mGroups = groups;
            mRequestsRemain = calculateRequestCount();
            mErrorMessage = null;
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (int friendNumber = 0; friendNumber < mFriends.size(); friendNumber += friendsPerRequest) {
                String varFriends = getVarFriends(friendNumber);
                for (int groupNumber = 0; groupNumber < mGroups.size(); groupNumber += groupPerRequest) {
                    if (mErrorMessage != null) {
                        publishProgress((JSONObject) null);
                        --mRequestsRemain;
                        continue;
                    }
                    String varGroups = getVarGroups(groupNumber);
                    String code = getCodeToExecute(varFriends, varGroups);
                    VKRequest request = new VKRequest("execute", VKParameters.from("code", code));
                    request.executeWithListener(new VKRequest.VKRequestListener() {
                        @Override
                        public void onComplete(VKResponse response) {
                            publishProgress(response.json);
                            --mRequestsRemain;
                            if (mDataSaver != null) {
                                mDataSaver.saveIsMember(response.json);
                            }
                        }

                        @Override
                        public void onError(VKError error) {
                            mErrorMessage = String.valueOf(error);
                            publishProgress((JSONObject) null);
                            --mRequestsRemain;
                        }
                    });
                    try {
                        // Чтобы запросы не посылались слишком часто. (Не больше 3 в секунду).
                        Thread.sleep(350);
                    } catch (InterruptedException e) {
                        mErrorMessage = String.valueOf(e);
                    }
                }
            }

            // Ждем, пока не выполнятся все запросы.
            while (mRequestsRemain > 0) {
                try {
                    Thread.sleep(15);
                } catch (InterruptedException ignored) {
                }

            }
            return null;
        }

        /**
         * Посчтитать кол-во запросов, учитывая размеры {@link @mFriends} и {@link #mGroups}.
         * @return кол-во запросов, которое надо выполнить.
         */
        private int calculateRequestCount() {
            int partsOfFriends = mFriends.size() / friendsPerRequest;
            if (mFriends.size() % friendsPerRequest > 0) {
                ++partsOfFriends;
            }
            int partsOfGroups = mGroups.size() / groupPerRequest;
            if (mGroups.size() % groupPerRequest > 0) {
                ++partsOfGroups;
            }
            return partsOfFriends * partsOfGroups;
        }

        /**
         * Получить значение для переменной в запросе - id друзей
         * @param friendStart - друг, начиная с которого формируется список id.
         */
        private String getVarFriends(int friendStart) {
            StringBuilder s = new StringBuilder();
            s.append("\"");
            int friendEnd = Math.min(friendStart + friendsPerRequest, mFriends.size());
            for (int i = friendStart; i < friendEnd; ++i) {
                s.append(mFriends.get(i).id).append(',');
            }
            s.append("\"");
            return s.toString();
        }

        /**
         * Получить значение для переменной в запросе - id групп
         * @param groupStart - группа, начиная с которого формируется список id.
         */
        private String getVarGroups(int groupStart) {
            StringBuilder s = new StringBuilder();
            int groupEnd = Math.min(groupStart + groupPerRequest, mGroups.size());
            s.append("{count:\"").append(groupEnd - groupStart).append("\",items:[");
            for (int i = groupStart; i < groupEnd; ++i) {
                s.append(mGroups.get(i).id).append(',');
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

        @Override
        protected void onProgressUpdate(JSONObject... values) {
            mListener.onProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mErrorMessage == null) {
                mListener.onCompleted(null);
            }
            else {
                mListener.onError(mErrorMessage);
            }
        }
    }

}
