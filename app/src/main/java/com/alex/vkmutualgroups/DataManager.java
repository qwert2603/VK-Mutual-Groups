package com.alex.vkmutualgroups;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

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
 */
public class DataManager {

    public static final String TAG = "DataManager";

    private static DataManager sDataManager;

    private DataManager(Context context) {
        mContext = context.getApplicationContext();
        clear();
    }

    public static DataManager get(Context context) {
        if (sDataManager == null) {
            sDataManager = new DataManager(context);
        }
        return sDataManager;
    }

    private Context mContext;

    /**
     * Друзья пользователя в алфавитном порядке.
     */
    private VKUsersArray mUsersFriendsByAlphabet;

    /**
     * Друзья пользователя в порядке уменьшения кол-ва общих групп.
     */
    private VKUsersArray mUsersFriendsByMutual;

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
    private Map<VKApiUserFull, VKApiCommunityArray> mGroupsMutualWithFriend;

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
        byMutual,
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
    public enum GroupsSortState {
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
        calculatingMutual,
        finished
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
            case byMutual:
                return mUsersFriendsByMutual;
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
    public VKApiCommunityArray getGroupsMutualWithFriend(VKApiUserFull user) {
        return (mGroupsMutualWithFriend.get(user) == null) ? new VKApiCommunityArray() : mGroupsMutualWithFriend.get(user);
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
    public void sortFriendsByMutual() {
        if (mUsersFriendsByMutual != null) {
            mFriendsSortState = FriendsSortState.byMutual;
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
     * Текущее состояние загрузки.
     */
    public FetchingState getFetchingState() {
        return mFetchingState;
    }

    /**
     * Надо ли прервать скачивание и вызвать {@link #clear()}.
     */
    private volatile boolean mNeedClearing = false;

    /**
     * Очистить все поля.
     * Если эта функция вызвана во время за грузки, то mNeedClearing присваивается true;
     * и функция будет вызвана заново после завершения загрузки, которая завершится скоро,
     * так как mNeedClearing будет равно true.
     */
    public void clear() {
        if (mFetchingState == FetchingState.loadingFriends || mFetchingState == FetchingState.calculatingMutual) {
            mNeedClearing = true;
        } else {
            mUsersFriendsByAlphabet = null;
            mUsersFriendsByMutual = null;
            mUserFriendsMap = new HashMap<>();

            mUsersGroupsByDefault = null;
            mUsersGroupsByFriends = null;
            mUserGroupsMap = new HashMap<>();

            mGroupsMutualWithFriend = new HashMap<>();
            mFriendsInGroup = new HashMap<>();

            mFriendsSortState = FriendsSortState.notSorted;
            mGroupsSortState = GroupsSortState.notSorted;
            mFetchingState = FetchingState.notStarted;
            mNeedClearing = false;
        }
    }

    /**
     * Удалить все сохраненные на устройстве данные.
     */
    public void clearDataOnDevice() {
        new DeviceDataSaver(mContext).clear();
    }

    /**
     * Listener для оповещения об изменении состояния загрузки и об ошибках.
     */
    public interface DataManagerListener extends Listener<Void> {
        void onFriendsLoaded();
        void onProgress();
    }

    /**
     * Загрузить данные с помощью vkapi.
     */
    public void fetchFromVK(DataManagerListener listener) {
        load(new VKDataProvider(new DeviceDataSaver(mContext)), listener);
    }

    /**
     * Загрузить данные с устройства.
     */
    public void loadFromDevice(DataManagerListener listener) {
        load(new DeviceDataProvider(mContext), listener);
    }

    /**
     * Последовательно:
     * - загрузить друзей пользователя,
     * - его группы,
     * - посчитать кол-во общих групп с друзьями и друзей в группах.
     * listener оповещается о завершении скачивания и подсчета и о прогрессе, и об ошибках.
     */
    private void load(DataProvider dataProvider, DataManagerListener listener) {
        if (mFetchingState == FetchingState.loadingFriends || mFetchingState == FetchingState.calculatingMutual) {
            listener.onError("Loading is already on!");
            return;
        }
        clear();
        mFetchingState = FetchingState.loadingFriends;
        new LoadingTask(listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dataProvider);
    }

    private class LoadingTask extends AsyncTask<DataProvider, Integer, Void> {
        private DataManagerListener mListener;

        /**
         * Ошибка, произошедшая во время {@link #doInBackground(DataProvider...)}.
         * Если == null, то ошибок не было.
         */
        private volatile String doInBackgroundErrorMessage;

        /**
         * Сколько шагов осталось до конца выполнения очередной части загрузки в {@link #doInBackground(DataProvider...)}.
         */
        private volatile int stepsRemain;

        public LoadingTask(DataManagerListener listener) {
            mListener = listener;
        }

        @Override
        protected Void doInBackground(DataProvider... params) {
            if (mNeedClearing) {
                return null;
            }

            DataProvider dataProvider = params[0];

            doInBackgroundErrorMessage = null;

            // загрузка друзей.

            stepsRemain = 1;
            dataProvider.loadFriends(new Listener<VKUsersArray>() {
                @Override
                public void onCompleted(VKUsersArray vkApiUserFulls) {
                    mUsersFriendsByAlphabet = vkApiUserFulls;
                    --stepsRemain;
                }

                @Override
                public void onError(String e) {
                    doInBackgroundErrorMessage = e;
                    --stepsRemain;
                }
            });
            waitSteps();
            if (doInBackgroundErrorMessage != null || mNeedClearing) {
                return null;
            }
            onFriendsLoaded();
            publishProgress(PROGRESS_FRIENDS_LOADED);

            // загрузка групп.

            stepsRemain = 1;
            dataProvider.loadGroups(new Listener<VKApiCommunityArray>() {
                @Override
                public void onCompleted(VKApiCommunityArray vkApiCommunityFulls) {
                    mUsersGroupsByDefault = vkApiCommunityFulls;
                    --stepsRemain;
                }

                @Override
                public void onError(String e) {
                    doInBackgroundErrorMessage = e;
                    --stepsRemain;
                }
            });
            waitSteps();
            if (doInBackgroundErrorMessage != null || mNeedClearing) {
                return null;
            }
            onGroupsLoaded();

            // загрузка и вычисление общих групп.

            stepsRemain = dataProvider.loadIsMembers(mUsersFriendsByAlphabet, mUsersGroupsByDefault,
                    new DataProvider.LoadIsMemberListener() {
                        @Override
                        public void onProgress(JSONObject jsonObject) {
                            if (jsonObject == null || doInBackgroundErrorMessage != null || mNeedClearing) {
                                --stepsRemain;
                                return;
                            }
                            parseIsMemberJSON(jsonObject, new Listener<Void>() {
                                @Override
                                public void onCompleted(Void aVoid) {
                                    if (doInBackgroundErrorMessage == null && !mNeedClearing) {
                                        publishProgress(PROGRESS_MUTUALS_ADDED);
                                    }
                                    --stepsRemain;
                                }

                                @Override
                                public void onError(String e) {
                                    doInBackgroundErrorMessage = e;
                                    --stepsRemain;
                                }
                            });
                        }

                        @Override
                        public void onCompleted(Void aVoid) {
                        }

                        @Override
                        public void onError(String e) {
                            doInBackgroundErrorMessage = e;
                        }
                    });
            waitSteps();
            if (doInBackgroundErrorMessage != null || mNeedClearing) {
                return null;
            }
            onMutualsLoaded();
            return null;
        }

        /**
         * Ждат пока кол-во оставшихся шагов не станет равно 0.
         */
        private void waitSteps() {
            while (stepsRemain > 0) {
                try {
                    Thread.sleep(15);
                } catch (InterruptedException ignored) {
                }
            }
        }

        /**
         * Действия выполняемые по случаю окончания загрузки друзей.
         */
        private void onFriendsLoaded() {
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
            mFriendsSortState = FriendsSortState.byAlphabet;

            for (VKApiUserFull friend : mUsersFriendsByAlphabet) {
                mGroupsMutualWithFriend.put(friend, new VKApiCommunityArray());
                mUserFriendsMap.put(friend.id, friend);
            }
        }

        /**
         * Действия выполняемые по случаю окончания загрузки групп.
         */
        private void onGroupsLoaded() {
            mGroupsSortState = GroupsSortState.byDefault;

            for (VKApiCommunityFull group : mUsersGroupsByDefault) {
                mFriendsInGroup.put(group, new VKUsersArray());
                mUserGroupsMap.put(group.id, group);
            }
        }

        /**
         * Действия выполняемые по случаю окончания загрузки инфромации об общих группах.
         */
        private void onMutualsLoaded() {
            // Копировать друзей в mUsersFriendsByMutual и отсортировать их по убыванию кол-ва общих групп.
            mUsersFriendsByMutual = new VKUsersArray();
            for (VKApiUserFull friend : mUsersFriendsByAlphabet) {
                mUsersFriendsByMutual.add(friend);
            }
            Collections.sort(mUsersFriendsByMutual, Collections.reverseOrder(new Comparator<VKApiUserFull>() {
                @Override
                public int compare(VKApiUserFull lhs, VKApiUserFull rhs) {
                    int l = getGroupsMutualWithFriend(lhs).size();
                    int r = getGroupsMutualWithFriend(rhs).size();
                    return (l == r) ? 0 : ((l > r) ? 1 : -1);
                }
            }));

            // Копировать группы в mUsersGroupsByFriends и отсортировать их по убыванию кол-ва друзей.
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
        }

        /**
         * Разобрать resultedJSONObject с информацией о наличии друзей в группах.
         */
        private void parseIsMemberJSON(final JSONObject resultedJSONObject, final Listener<Void> listener) {
            new AsyncTask<Void, Void, Void>() {
                /**
                 * Ошибка, произошедшая во время {@link #doInBackground(Void...)}.
                 * Если == null, то ошибок не было.
                 */
                private volatile String mErrorMessage = null;

                @Override
                protected Void doInBackground(Void... params) {
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
                                    mGroupsMutualWithFriend.get(friend).add(group);
                                    mFriendsInGroup.get(group).add(friend);
                                }
                            }
                        }
                    } catch (JSONException e) {
                        mErrorMessage = String.valueOf(e);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    if (mErrorMessage == null) {
                        listener.onCompleted(null);
                    }
                    else {
                        listener.onError(mErrorMessage);
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        private static final int PROGRESS_FRIENDS_LOADED = 1;
        private static final int PROGRESS_MUTUALS_ADDED = 2;

        @Override
        protected void onProgressUpdate(Integer... values) {
            switch (values[0]) {
                case PROGRESS_FRIENDS_LOADED:
                    mFetchingState = FetchingState.calculatingMutual;
                    mListener.onFriendsLoaded();
                    break;
                case PROGRESS_MUTUALS_ADDED:
                    mListener.onProgress();
                    break;
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (doInBackgroundErrorMessage == null && !mNeedClearing) {
                mFetchingState = FetchingState.finished;
                mListener.onCompleted(null);
                return;
            }

            mFetchingState = FetchingState.notStarted;
            clear();
            clearDataOnDevice();

            if (doInBackgroundErrorMessage != null) {
                mListener.onError(doInBackgroundErrorMessage);
                Log.e(TAG, doInBackgroundErrorMessage);
            }
        }
    }

}
