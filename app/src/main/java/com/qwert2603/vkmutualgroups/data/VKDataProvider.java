package com.qwert2603.vkmutualgroups.data;

import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.qwert2603.vkmutualgroups.Listener;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKUsersArray;

import java.util.ArrayList;

/**
 * Загрузчик данных их VK (через vkapi).
 */
public class VKDataProvider implements DataProvider {

    @SuppressWarnings("unused")
    public static final String TAG = "VKDataProvider";

    /**
     * Объект-сохранятель json в память устройства.
     * Если == null, сохранение не происходит.
     */
    private volatile DataSaver mDataSaver;

    public VKDataProvider(@Nullable DataSaver dataSaver) {
        mDataSaver = dataSaver;
        if (mDataSaver != null) {
            mDataSaver.clear();
        }
    }

    @Override
    public void load(Listener<Data> listener) {
        new LoadTask(listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class LoadTask extends AsyncTask<Void, Void, Data> {
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
         * Задержка перед следующим запросом.
         * Чтобы запросы не посылались слишком часто. (Не больше 3 в секунду).
         */
        private static final long nextRequestDelay = 350;

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

        public LoadTask(Listener<Data> listener) {
            mListener = listener;
        }

        @Override
        protected Data doInBackground(Void... params) {
            Data data = new Data();

            mRequestsRemain = 1;

            VKParameters friendsParameters = VKParameters.from(VKApiConst.FIELDS, "photo_50, can_write_private_message");
            VKRequest requestFriends = VKApi.friends().get(friendsParameters);
            requestFriends.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    data.mFriends = (VKUsersArray) response.parsedModel;
                    --mRequestsRemain;
                }

                @Override
                public void onError(VKError error) {
                    mErrorMessage = String.valueOf(error);
                    --mRequestsRemain;
                }
            });

            waitNextRequestDelay();
            waitRequestsRemain();
            if (mErrorMessage != null) {
                return null;
            }

            mRequestsRemain = 1;

            VKRequest requestGroups = VKApi.groups().get(VKParameters.from(VKApiConst.EXTENDED, 1));
            requestGroups.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    data.mGroups = (VKApiCommunityArray) response.parsedModel;
                    --mRequestsRemain;
                }

                @Override
                public void onError(VKError error) {
                    mErrorMessage = String.valueOf(error);
                    --mRequestsRemain;
                }
            });

            waitNextRequestDelay();
            waitRequestsRemain();
            if (mErrorMessage != null) {
                return null;
            }

            mRequestsRemain = calculateRequestCount(data);

            data.mIsMember = new ArrayList<>();
            for (int friendNumber = 0; friendNumber < data.mFriends.size(); friendNumber += friendsPerRequest) {
                String varFriends = getVarFriends(data, friendNumber);
                for (int groupNumber = 0; groupNumber < data.mGroups.size(); groupNumber += groupPerRequest) {
                    if (mErrorMessage != null) {
                        --mRequestsRemain;
                        continue;
                    }
                    String varGroups = getVarGroups(data, groupNumber);
                    String code = getCodeToExecute(varFriends, varGroups);
                    VKRequest request = new VKRequest("execute", VKParameters.from("code", code));
                    request.setUseLooperForCallListener(false);
                    request.executeWithListener(new VKRequest.VKRequestListener() {
                        @Override
                        public void onComplete(VKResponse response) {
                            data.mIsMember.add(response.json);
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
                mDataSaver.save(data);
            }

            return data;
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

        @Override
        protected void onPostExecute(Data data) {
            if (mErrorMessage == null) {
                mListener.onCompleted(data);
            }
            else {
                mListener.onError(mErrorMessage);
            }
        }
    }

}
