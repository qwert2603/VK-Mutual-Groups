package com.qwert2603.vkmutualgroups.data;

import android.content.Context;
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
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKUsersArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
     * Карта: "id друга" - "общие с ним группы"
     */
    private Map<Integer, VKApiCommunityArray> mGroupsMutualWithFriend;

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
        loadingFriends,
        calculatingMutual,
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
     * Загрузить список групп пользователя (друга, например) по его id.
     */
    public void fetchUsersGroups(int userId, Listener<VKApiCommunityArray> listener) {
        VKParameters parameters = VKParameters.from(VKApiConst.USER_ID, userId, VKApiConst.EXTENDED, 1, VKApiConst.FIELDS, "photo_50");
        VKRequest request = VKApi.groups().get(parameters);
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                listener.onCompleted((VKApiCommunityArray) response.parsedModel);
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
    public VKApiCommunityArray getGroupsMutualWithFriend(int userId) {
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
     * Удалить из друзей.
     */
    public void deleteFriend(int friendId, Listener<Void> listener) {
        VKRequest request = VKApi.friends().delete(VKParameters.from(VKApiConst.USER_ID, friendId));
        request.executeWithListener(new VKRequest.VKRequestListener() {
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
        request.executeWithListener(new VKRequest.VKRequestListener() {
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
        request.executeWithListener(new VKRequest.VKRequestListener() {
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
        request.executeWithListener(new VKRequest.VKRequestListener() {
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
        VKRequest requestIsMember = VKApi.groups().isMember(parameters);
        requestIsMember.executeWithListener(new VKRequest.VKRequestListener() {
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

        VKApiCommunityArray groups = mGroupsMutualWithFriend.get(friend.id);
        if (groups != null) {
            for (VKApiCommunityFull group : groups) {
                mFriendsInGroup.get(group.id).remove(friend);
            }
        }
        mGroupsMutualWithFriend.remove(friend.id);

        mUserFriendsMap.remove(friendId);
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
        mGroupsMutualWithFriend.put(friend.id, new VKApiCommunityArray());

        VKUsersArray friends = new VKUsersArray();
        friends.add(friend);

        fetchAndAddDataAboutMutuals(new VKDataProvider(null), friends, mUsersGroupsByDefault, new Listener<Void>() {
            @Override
            public void onCompleted(Void aVoid) {
                if (checkAndClear()) {
                    return;
                }
                doSortFriendsByMutuals();
                doSortGroupsByMutuals();
                listener.onCompleted(aVoid);
            }

            @Override
            public void onError(String e) {
                mNeedClearing = true;
                checkAndClear();
                listener.onError(e);
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

        VKApiCommunityArray groups = new VKApiCommunityArray();
        groups.add(group);

        fetchAndAddDataAboutMutuals(new VKDataProvider(null), mUsersFriendsByAlphabet, groups, new Listener<Void>() {
            @Override
            public void onCompleted(Void aVoid) {
                if (checkAndClear()) {
                    return;
                }
                doSortFriendsByMutuals();
                doSortGroupsByMutuals();
                listener.onCompleted(aVoid);
            }

            @Override
            public void onError(String e) {
                mNeedClearing = true;
                checkAndClear();
                listener.onError(e);
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
        DeviceDataSaver.get(mContext).clear();
    }

    /**
     * Проверить и, если надо, выполнить {@link #clear()} и  {@link #clearDataOnDevice()} ()}.
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
     * Listener для оповещения об изменении состояния загрузки и об ошибках.
     */
    public interface DataManagerListener extends Listener<Void> {
        void onFriendsLoaded();
    }

    /**
     * Загрузить данные с помощью vkapi.
     */
    public void fetchFromVK(DataManagerListener listener) {
        load(new VKDataProvider(DeviceDataSaver.get(mContext)), listener);
    }

    /**
     * Загрузить данные с устройства.
     */
    public void loadFromDevice(DataManagerListener listener) {
        load(DeviceDataProvider.get(mContext), listener);
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
        dataProvider.loadFriends(new Listener<VKUsersArray>() {
            @Override
            public void onCompleted(VKUsersArray vkApiUserFulls) {
                if (checkAndClear()) {
                    return;
                }
                mUsersFriendsByAlphabet = vkApiUserFulls;
                onFriendsLoaded(dataProvider, listener);
            }

            @Override
            public void onError(String e) {
                mNeedClearing = true;
                checkAndClear();
                listener.onError(e);
                Log.e(TAG, e);
            }
        });
    }

    /**
     * Действия выполняемые по случаю окончания загрузки друзей.
     */
    private void onFriendsLoaded(DataProvider dataProvider, DataManagerListener listener) {
        doSortFriendsByAlphabet();
        mFriendsSortState = FriendsSortState.byAlphabet;

        for (VKApiUserFull friend : mUsersFriendsByAlphabet) {
            mGroupsMutualWithFriend.put(friend.id, new VKApiCommunityArray());
            mUserFriendsMap.put(friend.id, friend);
        }

        mFetchingState = FetchingState.calculatingMutual;
        listener.onFriendsLoaded();
        dataProvider.loadGroups(new Listener<VKApiCommunityArray>() {
            @Override
            public void onCompleted(VKApiCommunityArray vkApiCommunityFulls) {
                if (checkAndClear()) {
                    return;
                }
                mUsersGroupsByDefault = vkApiCommunityFulls;
                onGroupsLoaded(dataProvider, listener);
            }

            @Override
            public void onError(String e) {
                mNeedClearing = true;
                checkAndClear();
                listener.onError(e);
                Log.e(TAG, e);
            }
        });
    }

    /**
     * Действия выполняемые по случаю окончания загрузки групп.
     */
    private void onGroupsLoaded(DataProvider dataProvider, DataManagerListener listener) {
        mGroupsSortState = GroupsSortState.byDefault;

        for (VKApiCommunityFull group : mUsersGroupsByDefault) {
            mFriendsInGroup.put(group.id, new VKUsersArray());
            mUserGroupsMap.put(group.id, group);
        }

        fetchAndAddDataAboutMutuals(dataProvider, mUsersFriendsByAlphabet, mUsersGroupsByDefault,
                new Listener<Void>() {
                    @Override
                    public void onCompleted(Void result) {
                        if (checkAndClear()) {
                            return;
                        }
                        onMutualsLoaded();
                        mFetchingState = FetchingState.finished;
                        listener.onCompleted(null);
                    }

                    @Override
                    public void onError(String e) {
                        mNeedClearing = true;
                        checkAndClear();
                        listener.onError(e);
                        Log.e(TAG, e);
                    }
                }

        );
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
        mUsersGroupsByFriends = new VKApiCommunityArray();
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
                VKApiCommunityArray lg = mGroupsMutualWithFriend.get(lhs.id);
                VKApiCommunityArray rg = mGroupsMutualWithFriend.get(rhs.id);
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

    /**
     * Загрузить и добавить данные об общих группах.
     */
    private void fetchAndAddDataAboutMutuals(DataProvider dataProvider, VKUsersArray friends,
                                             VKApiCommunityArray groups, Listener<Void> listener) {
        dataProvider.loadIsMembers(friends, groups, new Listener<ArrayList<JSONObject>>() {
            @Override
            public void onCompleted(ArrayList<JSONObject> result) {
                for (JSONObject jsonObject : result) {
                    if (!parseIsMemberJSON(jsonObject)) {
                        onError("parsing is_member error!!!");
                        return;
                    }
                }
                listener.onCompleted(null);
            }

            @Override
            public void onError(String e) {
                mNeedClearing = true;
                checkAndClear();
                listener.onError(e);
                Log.e(TAG, e);
            }
        });
    }

    /**
     * Разобрать и добавить данные об общих группах.
     */
    private boolean parseIsMemberJSON(JSONObject jsonObject) {
        try {
            JSONArray responseJSONArray = jsonObject.getJSONArray("response");
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
                        mGroupsMutualWithFriend.get(friend.id).add(group);
                        mFriendsInGroup.get(group.id).add(friend);
                    }
                }
            }
            return true;
        } catch (JSONException e) {
            Log.e(TAG, e.toString(), e);
            return false;
        }
    }

}
