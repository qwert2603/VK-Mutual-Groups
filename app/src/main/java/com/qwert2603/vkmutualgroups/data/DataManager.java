package com.qwert2603.vkmutualgroups.data;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.qwert2603.vkmutualgroups.Listener;
import com.qwert2603.vkmutualgroups.util.VKApiCommunityArray_Fix;
import com.qwert2603.vkmutualgroups.util.VkRequestsSender;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKList;
import com.vk.sdk.api.model.VKUsersArray;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Загружает и хранит список друзей в алфавитном порядке и в порядке убывания кол-ва общих групп.
 * Загружает и хранит список групп в порядке по умолчанию и в порядке убывания друзей в них.
 * Также подсчитывает кол-во общих групп.
 * Позволяет добавлять и удалять друзей, вступать в группы и выходить из групп.
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
    private VKApiCommunityArray_Fix mUsersGroupsByDefault;

    /**
     * Группы пользователя в порядке уменьшения кол-ва друзей в них.
     */
    private VKApiCommunityArray_Fix mUsersGroupsByFriends;

    /**
     * Карта: "id группы" - "объект этой группы".
     */
    private HashMap<Integer, VKApiCommunityFull> mUserGroupsMap;

    /**
     * Карта: "id друга" - "общие с ним группы"
     */
    private Map<Integer, VKApiCommunityArray_Fix> mGroupsMutualWithFriend;

    /**
     * Карта: "id группы" - "друзья в ней"
     */
    private Map<Integer, VKUsersArray> mFriendsInGroup;

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
        loading,
        finished
    }

    /**
     * Друзья пользователя, отсортированные в соответствии с {@link #mFriendsSortState}.
     */
    @Nullable
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
    @Nullable
    public VKApiCommunityArray_Fix getUsersGroups() {
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
     * Загрузить список групп пользователя (друга, например) по его id.
     */
    public void fetchUsersGroups(int userId, Listener<VKApiCommunityArray_Fix> listener) {
        VKParameters parameters = VKParameters.from(VKApiConst.USER_ID, userId, VKApiConst.EXTENDED, 1, VKApiConst.FIELDS, "photo_50");
        VKRequest request = VKApi.groups().get(parameters);
        VkRequestsSender.sendRequest(request, new VKRequest.VKRequestListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void onComplete(VKResponse response) {
                listener.onCompleted(new VKApiCommunityArray_Fix((VKList<VKApiCommunityFull>) response.parsedModel));
            }

            @Override
            public void onError(VKError error) {
                Log.e(TAG, "fetchUsersGroups ## ERROR == " + error);
                listener.onError(String.valueOf(error));
            }
        });
    }

    /**
     * Группы, общие с другом.
     */
    @Nullable
    public VKApiCommunityArray_Fix getGroupsMutualWithFriend(int userId) {
        return mGroupsMutualWithFriend.get(userId);
    }

    /**
     * Список друзей в группе.
     */
    @Nullable
    public VKUsersArray getFriendsInGroup(int groupId) {
        return mFriendsInGroup.get(groupId);
    }

    /**
     * Получить друга пользователя с требуемым id.
     * Если не друг, вернет null.
     */
    @Nullable
    public VKApiUserFull getUsersFriendById(int id) {
        return mUserFriendsMap.get(id);
    }

    /**
     * Получить группу пользователя с требуемым id.
     * Если пользователь не состоит в группе, вернет null.
     */
    @Nullable
    public VKApiCommunityFull getUsersGroupById(int id) {
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
     * Слушатель событияй начала/окончания/ошибки загрузки.
     */
    public interface DataLoadingListener extends Listener<Void> {
        void onLoadingStarted();
    }

    private HashSet<DataLoadingListener> mDataLoadingListeners = new HashSet<>();

    public void addDataLoadingListener(DataLoadingListener listener) {
        mDataLoadingListeners.add(listener);
    }

    public void removeDataLoadingListener(DataLoadingListener listener) {
        mDataLoadingListeners.remove(listener);
    }

    private void notifyOnLoadingStarted() {
        for (DataLoadingListener listener : mDataLoadingListeners) {
            listener.onLoadingStarted();
        }
    }

    private void notifyOnLoadingCompleted() {
        for (DataLoadingListener listener : mDataLoadingListeners) {
            listener.onCompleted(null);
        }
    }

    private void notifyOnLoadingError(String e) {
        for (DataLoadingListener listener : mDataLoadingListeners) {
            listener.onError(e);
        }
    }

    /**
     * Удалить из друзей.
     */
    public void deleteFriend(int friendId, Listener<Void> listener) {
        VKRequest request = VKApi.friends().delete(VKParameters.from(VKApiConst.USER_ID, friendId));
        VkRequestsSender.sendRequest(request, new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                deleteFriendFromData(friendId);
                clearDataOnDevice();
                listener.onCompleted(null);
            }

            @Override
            public void onError(VKError error) {
                Log.e(TAG, "deleteFriend ERROR!!! == " + error);
                listener.onError(String.valueOf(error));
            }
        });
    }

    /**
     * Добавить в друзья.
     * Результат передается в listener.
     * 1 - заявка отправлена.
     * 2 - пользователь добавлен в друзья.
     */
    public void addFriend(VKApiUserFull friend, Listener<Integer> listener) {
        VKRequest request = VKApi.friends().add(VKParameters.from(VKApiConst.USER_ID, friend.id));
        VkRequestsSender.sendRequest(request, new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                int result = 0;
                try {
                    result = response.json.getInt("response");
                } catch (JSONException e) {
                    Log.e(TAG, e.toString(), e);
                }
                switch (result) {
                    case 1:
                        listener.onCompleted(1);
                        break;
                    case 2:
                        clearDataOnDevice();
                        addFriendToData(friend, new Listener<Void>() {
                            @Override
                            public void onCompleted(Void o) {
                                listener.onCompleted(2);
                            }

                            @Override
                            public void onError(String e) {
                                Log.e(TAG, "addFriend ## ERROR == " + e);
                                listener.onError(e);
                            }
                        });
                        break;
                }
            }

            @Override
            public void onError(VKError error) {
                Log.e(TAG, "addFriend ERROR!!! == " + error);
                listener.onError(String.valueOf(error));
            }
        });
    }

    /**
     * Выйти из группы.
     */
    public void leaveGroup(int groupId, Listener<Void> listener) {
        VKRequest request = VKApi.groups().leave(VKParameters.from(VKApiConst.GROUP_ID, groupId));
        VkRequestsSender.sendRequest(request, new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                deleteGroupFromData(groupId);
                clearDataOnDevice();
                listener.onCompleted(null);
            }

            @Override
            public void onError(VKError error) {
                Log.e(TAG, "leaveGroup ERROR!!! == " + error);
                listener.onError(String.valueOf(error));
            }
        });
    }

    /**
     * Вступить в группу.
     * Результат передается в listener.
     * 1 - заявка отправлена.
     * 2 - пользователь вступил в группу.
     */
    public void joinGroup(VKApiCommunityFull group, Listener<Integer> listener) {
        VKRequest request = VKApi.groups().join(VKParameters.from(VKApiConst.GROUP_ID, group.id));
        VkRequestsSender.sendRequest(request, new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                checkIsMember(group, listener);
            }

            @Override
            public void onError(VKError error) {
                Log.e(TAG, "joinGroup ERROR!!! == " + error);
                listener.onError(String.valueOf(error));
            }
        });
    }

    private void checkIsMember(VKApiCommunityFull group, Listener<Integer> listener) {
        VKParameters parameters = VKParameters.from(VKApiConst.GROUP_ID, group.id, VKApiConst.EXTENDED, 0);
        VKRequest request = VKApi.groups().isMember(parameters);
        VkRequestsSender.sendRequest(request, new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                int result = -1;
                try {
                    result = response.json.getInt("response");
                } catch (JSONException e) {
                    Log.e(TAG, e.toString(), e);
                }
                switch (result) {
                    case 0:
                        listener.onCompleted(1);
                        break;
                    case 1:
                        clearDataOnDevice();
                        addGroupToData(group, new Listener<Void>() {
                            @Override
                            public void onCompleted(Void aVoid) {
                                listener.onCompleted(2);
                            }

                            @Override
                            public void onError(String e) {
                                Log.e(TAG, "joinGroup ## ERROR == " + e);
                                listener.onError(e);
                            }
                        });

                        break;
                }
            }

            @Override
            public void onError(VKError error) {
                Log.e(TAG, "joinGroup ERROR!!! == " + error);
                listener.onError(String.valueOf(error));
            }
        });
    }

    /**
     * Удалить все данные о друге.
     */
    private void deleteFriendFromData(int friendId) {
        VKApiUserFull friend = mUserFriendsMap.get(friendId);

        mUsersFriendsByAlphabet.remove(friend);
        mUsersFriendsByMutual.remove(friend);

        VKApiCommunityArray_Fix groups = mGroupsMutualWithFriend.get(friend.id);
        if (groups != null) {
            for (VKApiCommunityFull group : groups) {
                mFriendsInGroup.get(group.id).remove(friend);
            }
        }
        mGroupsMutualWithFriend.remove(friend.id);

        mUserFriendsMap.remove(friendId);

        doSortFriendsByMutuals();
        doSortGroupsByMutuals();
    }

    /**
     * Удалить все данные о группе.
     */
    private void deleteGroupFromData(int groupId) {
        VKApiCommunityFull group = mUserGroupsMap.get(groupId);

        mUsersGroupsByDefault.remove(group);
        mUsersGroupsByFriends.remove(group);

        VKUsersArray friends = mFriendsInGroup.get(group.id);
        if (friends != null) {
            for (VKApiUserFull friend : friends) {
                mGroupsMutualWithFriend.get(friend.id).remove(group);
            }
        }
        mFriendsInGroup.remove(group.id);

        mUserGroupsMap.remove(groupId);

        doSortFriendsByMutuals();
        doSortGroupsByMutuals();
    }

    /**
     * Добавить данные о друге, который только что был добавлен.
     * При этом загрузить через vkapi список общих групп с ним.
     */
    private void addFriendToData(VKApiUserFull friend, Listener<Void> listener) {
        mUsersFriendsByAlphabet.add(friend);
        doSortFriendsByAlphabet();
        mUsersFriendsByMutual.add(friend);

        mUserFriendsMap.put(friend.id, friend);
        mGroupsMutualWithFriend.put(friend.id, new VKApiCommunityArray_Fix());

        VKUsersArray friends = new VKUsersArray();
        friends.add(friend);

        Data data = new Data();
        data.mFriends = friends;
        data.mGroups = mUsersGroupsByDefault;

        mFetchingState = FetchingState.loading;
        notifyOnLoadingStarted();
        new VKDataProvider(null).loadIsMember(data, new Listener<Data>() {
            @Override
            public void onCompleted(Data data1) {
                if (checkAndClear()) {
                    return;
                }
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        addIsMemberFromData(data);
                        doSortFriendsByMutuals();
                        doSortGroupsByMutuals();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        mFetchingState = FetchingState.finished;
                        listener.onCompleted(null);
                        notifyOnLoadingCompleted();
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            @Override
            public void onError(String e) {
                mNeedClearing = true;
                checkAndClear();
                listener.onError(e);
                notifyOnLoadingError(e);
                Log.e(TAG, e);
            }
        });
    }

    /**
     * Добавить данные о группе, в которую вступил пользователь.
     * При этом загрузить через vkapi список друзей, которые уже есть в этой группе.
     */
    private void addGroupToData(VKApiCommunityFull group, Listener<Void> listener) {
        mUsersGroupsByDefault.add(group);
        mUsersGroupsByFriends.add(group);

        mUserGroupsMap.put(group.id, group);
        mFriendsInGroup.put(group.id, new VKUsersArray());

        VKApiCommunityArray_Fix groups = new VKApiCommunityArray_Fix();
        groups.add(group);

        Data data = new Data();
        data.mFriends = mUsersFriendsByAlphabet;
        data.mGroups = groups;

        mFetchingState = FetchingState.loading;
        notifyOnLoadingStarted();
        new VKDataProvider(null).loadIsMember(data, new Listener<Data>() {
            @Override
            public void onCompleted(Data data1) {
                if (checkAndClear()) {
                    return;
                }
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        addIsMemberFromData(data);
                        doSortFriendsByMutuals();
                        doSortGroupsByMutuals();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        mFetchingState = FetchingState.finished;
                        listener.onCompleted(null);
                        notifyOnLoadingCompleted();
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            @Override
            public void onError(String e) {
                mNeedClearing = true;
                checkAndClear();
                listener.onError(e);
                notifyOnLoadingError(e);
                Log.e(TAG, e);
            }
        });
    }

    /**
     * Надо ли прервать скачивание и вызвать {@link #clear()}.
     */
    private volatile boolean mNeedClearing = false;

    /**
     * Очистить все поля.
     * Если эта функция вызвана во время за грузки, то mNeedClearing присваивается true;
     * и функция будет вызвана заново после завершения загрузки,
     * так как mNeedClearing будет равно true.
     */
    public void clear() {
        if (mFetchingState == FetchingState.loading) {
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
        DeviceDataSaver.get(mContext).clear();
    }

    /**
     * Проверить и, если надо, выполнить {@link #clear()} и {@link #clearDataOnDevice()} ()}.
     */
    private boolean checkAndClear() {
        if (mNeedClearing) {
            mFetchingState = FetchingState.notStarted;
            clear();
            clearDataOnDevice();
            return true;
        }
        return false;
    }


    /**
     * Загрузить данные с устройства, если они там есть. Иначе - загрузить с vk.com.
     * @param refresh - загрузить ли данные заново с vk.com.
     */
    public void load(boolean refresh) {
        if (mFetchingState == FetchingState.loading) {
            Log.e(TAG, "Loading is already on!");
            return;
        }
        clear();
        mFetchingState = FetchingState.loading;
        notifyOnLoadingStarted();

        DataProvider dataProvider;
        DeviceDataProvider deviceDataProvider = DeviceDataProvider.get(mContext);
        if (!refresh && deviceDataProvider.isDataExist()) {
            dataProvider = deviceDataProvider;
        } else {
            dataProvider = new VKDataProvider(DeviceDataSaver.get(mContext));
        }

        dataProvider.load(new Listener<Data>() {
            @Override
            public void onCompleted(Data data) {
                if (checkAndClear()) {
                    return;
                }

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        mUsersFriendsByAlphabet = data.mFriends;
                        onFriendsLoaded();

                        mUsersGroupsByDefault = data.mGroups;
                        onGroupsLoaded();

                        addIsMemberFromData(data);
                        onMutualsLoaded();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        mFetchingState = FetchingState.finished;
                        notifyOnLoadingCompleted();
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            @Override
            public void onError(String e) {
                mNeedClearing = true;
                checkAndClear();
                notifyOnLoadingError(e);
                Log.e(TAG, e);
            }
        });
    }

    /**
     * Добавить данные о друзьях в группах.
     */
    private void addIsMemberFromData(Data data) {
        for (Map.Entry<Integer, ArrayList<Integer>> entry : data.mIsMember.entrySet()) {
            int friendId = entry.getKey();
            for (Integer groupId : entry.getValue()) {
                mFriendsInGroup.get(groupId).add(mUserFriendsMap.get(friendId));
                mGroupsMutualWithFriend.get(friendId).add(mUserGroupsMap.get(groupId));
            }
        }
    }

    /**
     * Действия выполняемые по случаю окончания загрузки друзей.
     */
    private void onFriendsLoaded() {
        doSortFriendsByAlphabet();
        mFriendsSortState = FriendsSortState.byAlphabet;

        for (VKApiUserFull friend : mUsersFriendsByAlphabet) {
            mGroupsMutualWithFriend.put(friend.id, new VKApiCommunityArray_Fix());
            mUserFriendsMap.put(friend.id, friend);
        }
    }

    /**
     * Действия выполняемые по случаю окончания загрузки групп.
     */
    private void onGroupsLoaded() {
        mGroupsSortState = GroupsSortState.byDefault;

        for (VKApiCommunityFull group : mUsersGroupsByDefault) {
            mFriendsInGroup.put(group.id, new VKUsersArray());
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
        doSortFriendsByMutuals();

        // Копировать группы в mUsersGroupsByFriends и отсортировать их по убыванию кол-ва друзей.
        mUsersGroupsByFriends = new VKApiCommunityArray_Fix();
        for (VKApiCommunityFull group : mUsersGroupsByDefault) {
            mUsersGroupsByFriends.add(group);
        }
        doSortGroupsByMutuals();
    }

    /**
     * Отсортировать mUsersFriendsByAlphabet в алфавитном порядке.
     */
    private void doSortFriendsByAlphabet() {
        Collections.sort(mUsersFriendsByAlphabet, (lhs, rhs) -> {
            int r = lhs.first_name.compareTo(rhs.first_name);
            if (r != 0) {
                return r;
            }
            return lhs.last_name.compareTo(rhs.last_name);
        });
    }

    /**
     * Отсортировать mUsersFriendsByMutual убыванию кол-ва общих групп.
     */
    private void doSortFriendsByMutuals() {
        Collections.sort(mUsersFriendsByMutual, Collections.reverseOrder(new Comparator<VKApiUserFull>() {
            @Override
            public int compare(VKApiUserFull lhs, VKApiUserFull rhs) {
                VKApiCommunityArray_Fix lg = mGroupsMutualWithFriend.get(lhs.id);
                VKApiCommunityArray_Fix rg = mGroupsMutualWithFriend.get(rhs.id);
                if (lg == null || rg == null) {
                    Log.e(TAG, "ERROR!!! UNKNOWN FRIEND!!! SORT FAILED!!!");
                    return 0;
                }
                int l = lg.size();
                int r = rg.size();
                return (l == r) ? 0 : ((l > r) ? 1 : -1);
            }
        }));
    }

    /**
     * Отсортировать mUsersGroupsByFriends по убыванию друзей в группе.
     */
    private void doSortGroupsByMutuals() {
        Collections.sort(mUsersGroupsByFriends, Collections.reverseOrder(new Comparator<VKApiCommunityFull>() {
            @Override
            public int compare(VKApiCommunityFull lhs, VKApiCommunityFull rhs) {
                VKUsersArray lf = mFriendsInGroup.get(lhs.id);
                VKUsersArray rf = mFriendsInGroup.get(rhs.id);
                if (lf == null || rf == null) {
                    Log.e(TAG, "ERROR!!! UNKNOWN GROUP!!! SORT FAILED!!!");
                    return 0;
                }
                int l = lf.size();
                int r = rf.size();
                return (l == r) ? 0 : ((l > r) ? 1 : -1);
            }
        }));
    }

}
