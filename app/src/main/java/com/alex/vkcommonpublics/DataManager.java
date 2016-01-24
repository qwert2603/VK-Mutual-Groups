package com.alex.vkcommonpublics;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKUsersArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Загружает и хранит список друзей в афлавитном порядке и в порядке убывания кол-ва общих групп.
 * Загружает и хранит список групп в порядке по умолчанию и в порядке убывания друзей в них.
 * Также подсчитывает кол-во общих групп.
 * Для загрузки и подсчета надо вызвать {@link #fetch()}.
 * Перед загрузкой надо назначить слушателя {@link #setDataManagerListener(DataManagerListener)}.
 *
 * При закрытии приложения надо обязательно вызвать {@link #quitProcessingThread()}
 * для завершения потока обработки результатов загрузки.
 */
public class DataManager {

    private static final String TAG = "DataManager";

    private static DataManager sDataManager = new DataManager();

    private DataManager() {
        clear();
    }

    public static DataManager get() {
        return sDataManager;
    }

    /**
     * Поток для обработки результатов запросов. (Сортировка и парсинг).
     */
    private DataProcessingThread mDataProcessingThread;

    /**
     * Друзья пользователя в алфавитном порядке.
     */
    private VKUsersArray mUsersFriendsByAlphabet;

    /**
     * Друзья пользователя в порядке уменьшения кол-ва общих групп.
     */
    private VKUsersArray mUsersFriendsByCommons;

    /**
     * Карта: "id друга" - "объект этого друга".
     */
    private HashMap<Integer, VKApiUserFull> mUserFriendsMap;

    /**
     * Группы пользователя в порядке по умолчанию.
     */
    private VKApiCommunityArray mUsersGroupsByDefault;

    /**
     * Группы пользователя в порядке уменьшения кол-ва друзей в них.
     */
    private VKApiCommunityArray mUsersGroupsByFriends;

    /**
     * Карта: "id группы" - "объект этой группы".
     */
    private HashMap<Integer, VKApiCommunityFull> mUserGroupsMap;

    /**
     * Карта: "друг" - "общие с ним группы"
     */
    private Map<VKApiUserFull, VKApiCommunityArray> mGroupsCommonWithFriend;

    /**
     * Карта: "группа" - "друзья в ней"
     */
    private Map<VKApiCommunityFull, VKUsersArray> mFriendsInGroup;

    /**
     * Какая сортировка друзей применена в настоящий момент.
     */
    private FriendsSortState mFriendsSortState;

    /**
     * Возможные состояния сортировки друзей.
     * Сортируется только список всех друзей.
     * Списки друзей в отдельной группе не сортируются.
     */
    public enum FriendsSortState {
        notSorted,
        byCommons,
        byAlphabet
    }

    /**
     * Какая сортировка групп применена в настоящий момент.
     */
    private GroupsSortState mGroupsSortState;

    /**
     * Возможные состояния сортировки групп.
     * Сортируется только список всех групп.
     * Списки групп общих с отдельным другом не сортируются.
     */
    public enum GroupsSortState{
        notSorted,
        byDefault,
        byFriends
    }

    /**
     * Текущее состояние загрузки.
     */
    private FetchingState mFetchingState;

    /**
     * Возможные состояния загрузки.
     */
    public enum FetchingState {
        notStarted,
        loadingFriends,
        calculatingCommons,
        finished
    }

    /**
     * Надо ли прервать скачивание и вызвать {@link #clear()}.
     */
    private volatile boolean mNeedClearing = false;

    /**
     * Listener для оповещения об изменении состояния загрузки и об ошибках.
     */
    public interface DataManagerListener extends Listener {
        void onFriendsFetched();
        void onProgress();
    }

    private DataManagerListener mDataManagerListener;

    public void setDataManagerListener(DataManagerListener dataManagerListener) {
        mDataManagerListener = dataManagerListener;
    }

    /**
     * Последовательно:
     * - загрузить друзей пользователя,
     * - его группы,
     * - посчитать кол-во общих групп с друзьями и друзей в группах.
     * mDataManagerListener оповещается о завершении скачивания и подсчета и о прогрессе.
     * Перед вызовом надо назначить mDataManagerListener с помощью {@link #setDataManagerListener(DataManagerListener)}.
     */
    public void fetch() {
        if (mDataManagerListener == null) {
            return;
        }
        if (mFetchingState == FetchingState.loadingFriends || mFetchingState == FetchingState.calculatingCommons) {
            mDataManagerListener.onError("Fetching is already on!");
            return;
        }
        clear();
        loadFriends();
    }

    /**
     * Друзья пользователя, отсортированные в соответствии с {@link #mFriendsSortState}.
     */
    public VKUsersArray getUsersFriends() {
        switch (mFriendsSortState) {
            case notSorted:
                return null;
            case byAlphabet:
                return mUsersFriendsByAlphabet;
            case byCommons:
                return mUsersFriendsByCommons;
        }
        return null;
    }

    /**
     * Группы пользователя, отсортированные в соответствии с {@link #mGroupsSortState}.
     */
    public VKApiCommunityArray getUsersGroups() {
        switch (mGroupsSortState) {
            case notSorted:
                return null;
            case byDefault:
                return mUsersGroupsByDefault;
            case byFriends:
                return mUsersGroupsByFriends;
        }
        return null;
    }

    /**
     * Группы, общие с другом.
     */
    @NonNull
    public VKApiCommunityArray getGroupsCommonWithFriend(VKApiUserFull user) {
        return (mGroupsCommonWithFriend.get(user) == null) ? new VKApiCommunityArray() : mGroupsCommonWithFriend.get(user);
    }

    /**
     * Список друзей в группе.
     */
    @NonNull
    public VKUsersArray getFriendsInGroup(VKApiCommunityFull group) {
        return (mFriendsInGroup.get(group) == null) ? new VKUsersArray() : mFriendsInGroup.get(group);
    }

    /**
     * Получить друга с требуемым id.
     */
    public VKApiUserFull getFriendById(int id) {
        return mUserFriendsMap.get(id);
    }

    /**
     * Получить группу с требуемым id.
     */
    public VKApiCommunityFull getGroupById(int id) {
        return mUserGroupsMap.get(id);
    }

    /**
     * Отсортировать друзей в порядке уменьшения кол-ва общих групп.
     */
    public void sortFriendsByCommons() {
        if (mUsersFriendsByCommons != null) {
            mFriendsSortState = FriendsSortState.byCommons;
        }
    }

    /**
     * Отсортировать друзей в алфавитном порядке.
     */
    public void sortFriendsByAlphabet() {
        if (mUsersFriendsByAlphabet != null) {
            mFriendsSortState = FriendsSortState.byAlphabet;
        }
    }

    /**
     * Текущий вид сортировки друзей.
     */
    public FriendsSortState getFriendsSortState() {
        return mFriendsSortState;
    }

    /**
     * Отсортировать группы в порядке по умолчанию.
     */
    public void sortGroupsByDefault() {
        if (mUsersGroupsByDefault != null) {
            mGroupsSortState = GroupsSortState.byDefault;
        }
    }

    /**
     * Отсортировать гурппы в порядке убывания друзей в них.
     */
    public void sortGroupsByFriends() {
        if (mUsersGroupsByFriends != null) {
            mGroupsSortState = GroupsSortState.byFriends;
        }
    }

    /**
     * Текущий вид сортировки групп.
     */
    public GroupsSortState getGroupsSortState() {
        return mGroupsSortState;
    }

    /**
     * Очистить все поля.
     * Если эта функция вызвана во время за грузки, то mNeedClearing присваивается true;
     * и функция будет вызвана заново после завершения загрузки, которая завершится скоро,
     * так как mNeedClearing будет равно true.
     */
    public void clear() {
        if (mFetchingState == FetchingState.loadingFriends || mFetchingState == FetchingState.calculatingCommons) {
            mNeedClearing = true;
        }
        else {
            if (mDataProcessingThread == null) {
                mDataProcessingThread = new DataProcessingThread(new Handler(Looper.getMainLooper()));
                mDataProcessingThread.start();
                mDataProcessingThread.getLooper();
            }
            mDataProcessingThread.clearMessages();

            mUsersFriendsByAlphabet = null;
            mUsersFriendsByCommons = null;
            mUserFriendsMap = new HashMap<>();

            mUsersGroupsByDefault = null;
            mUsersGroupsByFriends = null;
            mUserGroupsMap = new HashMap<>();

            mGroupsCommonWithFriend = new HashMap<>();
            mFriendsInGroup = new HashMap<>();

            mFriendsSortState = FriendsSortState.notSorted;
            mGroupsSortState = GroupsSortState.notSorted;
            mFetchingState = FetchingState.notStarted;
            mNeedClearing = false;
        }
    }

    /**
     * Текущее состояние загрузки.
     */
    public FetchingState getFetchingState() {
        return mFetchingState;
    }

    /**
     * Завершить поток обработки результатов запросов.
     */
    public void quitProcessingThread() {
        mDataProcessingThread.quit();
    }

    /**
     * Listener для результатов запросов к vkapi.
     * В случае ошибки всегда происходит одно и тоже.
     */
    private abstract class DataManagerVKRequestListener extends VKRequest.VKRequestListener {
        @Override
        public abstract void onComplete(VKResponse response);

        @Override
        public void onError(VKError error) {
            mFetchingState = FetchingState.notStarted;
            clear();
            mDataManagerListener.onError(String.valueOf(error));
        }
    }

    /**
     * Listener для результатов выполнения обработки данных в {@link DataProcessingThread}.
     * В случае ошибки всегда происходит одно и тоже.
     */
    private abstract class DataProcessingThreadListener implements Listener {
        @Override
        public void onError(String error) {
            mFetchingState = FetchingState.notStarted;
            clear();
            mDataManagerListener.onError(String.valueOf(error));
        }
    }

    /**
     * Загрузить друзей пользователя.
     */
    private void loadFriends() {
        Log.d(TAG, "loadFriends");
        mFetchingState = FetchingState.loadingFriends;
        VKRequest request = VKApi.friends().get(VKParameters.from(VKApiConst.FIELDS, "photo_50"));
        request.executeWithListener(new DataManagerVKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                if (mNeedClearing) {
                    mFetchingState = FetchingState.notStarted;
                    clear();
                    return;
                }
                mUsersFriendsByAlphabet = (VKUsersArray) response.parsedModel;

                mDataProcessingThread.processFriendsLoaded(new DataProcessingThreadListener() {
                    @Override
                    public void onCompleted() {
                        mFriendsSortState = FriendsSortState.byAlphabet;
                        mFetchingState = FetchingState.calculatingCommons;
                        mDataManagerListener.onFriendsFetched();
                        loadGroups();
                    }
                });
            }
        });
    }

    /**
     * Загрузить группы пользователя.
     */
    private void loadGroups() {
        Log.d(TAG, "loadGroups");
        if (mNeedClearing) {
            mFetchingState = FetchingState.notStarted;
            clear();
            return;
        }
        VKRequest request = VKApi.groups().get(VKParameters.from(VKApiConst.EXTENDED, 1, VKApiConst.FIELDS, "photo_50"));
        request.executeWithListener(new DataManagerVKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                if (mNeedClearing) {
                    mFetchingState = FetchingState.notStarted;
                    clear();
                    return;
                }
                mUsersGroupsByDefault = (VKApiCommunityArray) response.parsedModel;

                mDataProcessingThread.processGroupsLoaded(new DataProcessingThreadListener() {
                    @Override
                    public void onCompleted() {
                        mGroupsSortState = GroupsSortState.byDefault;
                        new CalculateCommonsTask().execute();
                    }
                });
            }
        });
    }

    /**
     * Класс для выполения запроса к vkapi и подсчета друзей в группах и кол-ва общих групп с друзьями.
     * Это AsyncTask потому что формирование запроса (и строк - списов id) может быть продолжительным,
     * и потому что подсчет общих групп представляет собой более менее обособленное цельное действие.
     */
    private class CalculateCommonsTask extends AsyncTask<Void, Void, Void> {
        /**
         * Для сообщений об ошибках, которые могут произойти во время работы этого класса, используем следующие переменные.
         */
        private volatile boolean mIsCalculatingErrorHappened = false;
        private volatile String mCalculatingErrorString = null;

        /**
         * Сколько запросов осталось выполнить (vkapi.execute).
         */
        private volatile int mRequestsRemain;

        /**
         * Кол-во друзей, обрабатываемое в 1 запросе.
         * Не больше 500.
         */
        private static final int friendsPerRequest = 100;

        /**
         * Кол-во групп, обрабатываемое в 1 запросе.
         * Не больше 25. (ограничение в 25 запросов к api в vkapi.execute).
         */
        private static final int groupPerRequest = 25;

        public CalculateCommonsTask() {
            int partsOfFriends = mUsersFriendsByAlphabet.size() / friendsPerRequest;
            if (mUsersFriendsByAlphabet.size() % friendsPerRequest > 0) {
                ++partsOfFriends;
            }
            int partsOfGroups = mUsersGroupsByDefault.size() / groupPerRequest;
            if (mUsersGroupsByDefault.size() % groupPerRequest > 0) {
                ++partsOfGroups;
            }
            mRequestsRemain = partsOfFriends * partsOfGroups;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (mNeedClearing) {
                return null;
            }

            Log.d(TAG, "doInBackground ## 1");

            calculateCommonGroups();

            if (mNeedClearing || mIsCalculatingErrorHappened) {
                return null;
            }

            Log.d(TAG, "doInBackground ## 2");

            // Копируем друзей в mUsersFriendsByCommons и сортируем их по убыванию кол-ва общих групп.
            mUsersFriendsByCommons = new VKUsersArray();
            for (VKApiUserFull friend : mUsersFriendsByAlphabet) {
                mUsersFriendsByCommons.add(friend);
            }
            Collections.sort(mUsersFriendsByCommons, Collections.reverseOrder(new Comparator<VKApiUserFull>() {
                @Override
                public int compare(VKApiUserFull lhs, VKApiUserFull rhs) {
                    int l = getGroupsCommonWithFriend(lhs).size();
                    int r = getGroupsCommonWithFriend(rhs).size();
                    return (l == r) ? 0 : ((l > r) ? 1 : -1);
                }
            }));

            if (mNeedClearing || mIsCalculatingErrorHappened) {
                return null;
            }

            Log.d(TAG, "doInBackground ## 3");

            // Копируем группы в mUsersGroupsByFriends и сортируем их по убыванию кол-ва друзей.
            mUsersGroupsByFriends = new VKApiCommunityArray();
            for (VKApiCommunityFull group : mUsersGroupsByDefault) {
                mUsersGroupsByFriends.add(group);
            }
            Collections.sort(mUsersGroupsByFriends, Collections.reverseOrder(new Comparator<VKApiCommunityFull>() {
                @Override
                public int compare(VKApiCommunityFull lhs, VKApiCommunityFull rhs) {
                    int l = getFriendsInGroup(lhs).size();
                    int r = getFriendsInGroup(rhs).size();
                    return (l == r) ? 0 : ((l > r) ? 1 : -1);
                }
            }));

            Log.d(TAG, "doInBackground ## 4");
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.d(TAG, "onPostExecute ##");

            if (mIsCalculatingErrorHappened) {
                // во время выполнения подсчета общих групп произошла ошибка, сообщаем о ней.
                mDataManagerListener.onError(mCalculatingErrorString);
            }
            if (mNeedClearing || mIsCalculatingErrorHappened) {
                mFetchingState = FetchingState.notStarted;
                clear();
                return;
            }
            // если не надо было вызвать {#clear} и не было ошибок, сообщаем о завершении подсчета общих групп.
            mFetchingState = FetchingState.finished;
            mDataManagerListener.onCompleted();
            Log.d(TAG, "onPostExecute ## finish");
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            mDataManagerListener.onProgress();
        }

        private void calculateCommonGroups() {
            for (int friendNumber = 0; friendNumber < mUsersFriendsByAlphabet.size(); friendNumber += friendsPerRequest) {
                String varFriends = getVarFriends(friendNumber);
                for (int groupNumber = 0; groupNumber < mUsersGroupsByDefault.size(); groupNumber += groupPerRequest) {
                    if (mNeedClearing || mIsCalculatingErrorHappened) {
                        --mRequestsRemain;
                        Log.d(TAG, "calculateCommonGroups ## continue");
                        continue;
                    }
                    String varGroups = getVarGroups(groupNumber);
                    String code = getCodeToExecute(varFriends, varGroups);
                    VKRequest request = new VKRequest("execute", VKParameters.from("code", code));
                    Log.d(TAG, "calculateCommonGroups ## executeWithListener");
                    request.executeWithListener(mExecuteRequestListener);
                    try {
                        // Чтобы запросы не посылались слишком часто. (Не больше 3 в секунду).
                        Thread.sleep(350);
                    } catch (InterruptedException e) {
                        mIsCalculatingErrorHappened = true;
                        mCalculatingErrorString = String.valueOf(e);
                    }
                }
            }

            Log.d(TAG, "calculateCommonGroups ## waiting");
            // Ждем, пока не выполнятся все запросы.
            while (mRequestsRemain > 0) {
                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    //nth
                }
                Log.v(TAG, "calculateCommonGroups ## waiting ## mRequestsRemain == " + mRequestsRemain);
            }
            Log.d(TAG, "calculateCommonGroups ## finish");
        }

        /**
         * Получить значение для переменной в запросе - id друзей
         * @param friendStart - друг, начиная с которого формируется список id.
         */
        private String getVarFriends(int friendStart) {
            StringBuilder s = new StringBuilder();
            s.append("\"");
            int friendEnd = Math.min(friendStart + friendsPerRequest, mUsersFriendsByAlphabet.size());
            for (int i = friendStart; i < friendEnd; ++i) {
                s.append(mUsersFriendsByAlphabet.get(i).id).append(',');
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
            int groupEnd = Math.min(groupStart + groupPerRequest, mUsersGroupsByDefault.size());
            s.append("{count:\"").append(groupEnd - groupStart).append("\",items:[");
            for (int i = groupStart; i < groupEnd; ++i) {
                s.append(mUsersGroupsByDefault.get(i).id).append(',');
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
         * Слушатель результатов выполнения запроса vkapi.execute.
         */
        private VKRequest.VKRequestListener mExecuteRequestListener = new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                Log.d(TAG, "mExecuteRequestListener ## onComplete()");
                if (!mNeedClearing && !mIsCalculatingErrorHappened) {
                    mDataProcessingThread.parseJSON(response.json, mParseJSONDataProcessingThreadListener);
                }
                else {
                    --mRequestsRemain;
                }
            }

            @Override
            public void onError(VKError error) {
                Log.d(TAG, "mExecuteRequestListener ## onError()");
                mIsCalculatingErrorHappened = true;
                mCalculatingErrorString = String.valueOf(error);
                --mRequestsRemain;
            }
        };

        /**
         * Слушатель результатов выполения парсинга.
         */
        private Listener mParseJSONDataProcessingThreadListener = new Listener() {
            @Override
            public void onCompleted() {
                Log.d(TAG, "mParseJSONDataProcessingThreadListener ## onCompleted()");
                if (!mNeedClearing && !mIsCalculatingErrorHappened) {
                    publishProgress();
                }
                --mRequestsRemain;
            }

            @Override
            public void onError(String e) {
                Log.d(TAG, "mParseJSONDataProcessingThreadListener ## onError()");
                mIsCalculatingErrorHappened = true;
                mCalculatingErrorString = String.valueOf(e);
                --mRequestsRemain;
            }
        };
    }

    /**
     * Класс-поток-обработчик результатов запросов.
     */
    private class DataProcessingThread extends HandlerThread {

        private static final int MESSAGE_PROCESS_FRIENDS_LOADED_RESULT = 1;
        private static final int MESSAGE_PROCESS_GROUPS_LOADED_RESULT = 2;
        private static final int MESSAGE_PARSE_JSON = 3;

        /**
         * Обработчик сообщений.
         */
        private Handler mHandler;

        /**
         * Обработчик для результатов загрузки (и ошибок тоже)
         */
        private Handler mResponseHandler;

        /**
         * Для соотнесения объекта для парсинга и слушателя.
         * И чтобы повторно не парсить одно и тоже.
         */
        private Map<JSONObject, Listener> mListenerMap = Collections.synchronizedMap(new HashMap<JSONObject, Listener>());

        public DataProcessingThread(Handler responseHandler) {
            super("DataProcessingThread");
            mResponseHandler = responseHandler;
        }

        @SuppressWarnings("unchecked")
        @SuppressLint("handlerLeak")
        @Override
        protected void onLooperPrepared() {
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MESSAGE_PROCESS_FRIENDS_LOADED_RESULT:
                            handleProcessFriendsLoaded((Listener) msg.obj);
                            break;
                        case MESSAGE_PROCESS_GROUPS_LOADED_RESULT:
                            handleProcessGroupsLoaded((Listener) msg.obj);
                            break;
                        case MESSAGE_PARSE_JSON:
                            handleParseJSON((JSONObject) msg.obj);
                            break;
                    }
                }
            };
        }

        public void processFriendsLoaded(Listener listener) {
            while (mHandler == null) {
                Thread.yield();
            }
            Log.d(TAG, "processFriendsLoaded ##");
            mHandler.obtainMessage(MESSAGE_PROCESS_FRIENDS_LOADED_RESULT, listener).sendToTarget();
        }

        public void processGroupsLoaded(Listener listener) {
            while (mHandler == null) {
                Thread.yield();
            }
            Log.d(TAG, "processGroupsLoaded ##");
            mHandler.obtainMessage(MESSAGE_PROCESS_GROUPS_LOADED_RESULT, listener).sendToTarget();
        }

        public void parseJSON(JSONObject jsonObject, Listener listener) {
            while (mHandler == null) {
                Thread.yield();
            }
            Log.d(TAG, "parseJSON ##");
            mListenerMap.put(jsonObject, listener);
            mHandler.obtainMessage(MESSAGE_PARSE_JSON, jsonObject).sendToTarget();
        }

        public void clearMessages() {
            while (mHandler == null) {
                Thread.yield();
            }
            mHandler.removeMessages(MESSAGE_PROCESS_FRIENDS_LOADED_RESULT);
            mHandler.removeMessages(MESSAGE_PROCESS_GROUPS_LOADED_RESULT);
            mHandler.removeMessages(MESSAGE_PARSE_JSON);
            mListenerMap.clear();
        }

        private void handleProcessFriendsLoaded(final Listener listener) {
            Log.d(TAG, "handleProcessFriendsLoaded ##");
            // Отсортировать друзей в алфавитном порядке.
            Collections.sort(mUsersFriendsByAlphabet, new Comparator<VKApiUserFull>() {
                @Override
                public int compare(VKApiUserFull lhs, VKApiUserFull rhs) {
                    int r = lhs.first_name.compareTo(rhs.first_name);
                    if (r != 0) {
                        return r;
                    }
                    return lhs.last_name.compareTo(rhs.last_name);
                }
            });
            Log.d(TAG, "handleProcessFriendsLoaded ## after_sort ##");
            for (VKApiUserFull friend : mUsersFriendsByAlphabet) {
                mGroupsCommonWithFriend.put(friend, new VKApiCommunityArray());
                mUserFriendsMap.put(friend.id, friend);
            }
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onCompleted();
                    Log.d(TAG, "handleProcessFriendsLoaded ## listener.onCompleted();");
                }
            });
        }

        private void handleProcessGroupsLoaded(final Listener listener) {
            Log.d(TAG, "handleProcessGroupsLoaded ##");
            for (VKApiCommunityFull group : mUsersGroupsByDefault) {
                mFriendsInGroup.put(group, new VKUsersArray());
                mUserGroupsMap.put(group.id, group);
            }
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onCompleted();
                    Log.d(TAG, "handleProcessGroupsLoaded ## listener.onCompleted(); ##");
                }
            });
        }

        /**
         * Разобрать и сохранить результат запроса.
         */
        private void handleParseJSON(final JSONObject resultedJSONObject) {
            Log.d(TAG, "handleParseJSON ##");
            final Listener listener = mListenerMap.get(resultedJSONObject);
            if (listener == null) {
                Log.d(TAG, "handleParseJSON ## returning!!! ## listener == null");
                return;
            }
            try {
                JSONArray responseJSONArray = resultedJSONObject.getJSONArray("response");
                int responseJSONArrayLength = responseJSONArray.length();
                for (int i = 0; i < responseJSONArrayLength; ++i) {
                    JSONObject groupJSONObject = responseJSONArray.getJSONObject(i);
                    int groupId = groupJSONObject.getInt("group_id");
                    VKApiCommunityFull group = mUserGroupsMap.get(groupId);
                    JSONArray membersJSONArray = groupJSONObject.getJSONArray("members");
                    int membersJSONArrayLength = membersJSONArray.length();
                    for (int j = 0; j < membersJSONArrayLength; ++j) {
                        JSONObject memberJSONObject = membersJSONArray.getJSONObject(j);
                        if (memberJSONObject.getInt("member") == 1) {
                            int friendId = memberJSONObject.getInt("user_id");
                            VKApiUserFull friend = mUserFriendsMap.get(friendId);
                            mGroupsCommonWithFriend.get(friend).add(group);
                            mFriendsInGroup.get(group).add(friend);
                        }
                    }
                }
                mResponseHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onCompleted();
                        mListenerMap.remove(resultedJSONObject);
                        Log.d(TAG, "handleParseJSON ## listener.onCompleted(); ##");
                    }
                });
            } catch (final JSONException e) {
                mResponseHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onError(String.valueOf(e));
                        mListenerMap.remove(resultedJSONObject);
                        Log.d(TAG, "handleParseJSON ## listener.onError(); ##");
                    }
                });
            }
        }
    }

}