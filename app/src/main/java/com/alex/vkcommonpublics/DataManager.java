package com.alex.vkcommonpublics;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

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
 * Для загрузки и подсчета надо вызвать {@link #fetch(Listener)}.
 */
@SuppressWarnings("unused")
public class DataManager {

    private static DataManager sDataManager = new DataManager();

    private DataManager() {
        clear();
    }

    public static DataManager get() {
        return sDataManager;
    }

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
    private Map<VKApiUserFull, VKApiCommunityArray> mGroupsCommonWithFriendMap;

    /**
     * Карта: "группа" - "друзья в ней"
     */
    private Map<VKApiCommunityFull, VKUsersArray> mFriendsInGroupMap;

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

    public interface Listener {
        void onFriendsFetched();
        void onCommonsCalculated();
        void onProgress();
        void onError(String e);
    }

    /**
     * Последовательно:
     * - загрузить друзей пользователя,
     * - его группы,
     * - посчитать кол-во общих групп с друзьями.
     */
    public void fetch(Listener listener) {
        if (mFetchingState == FetchingState.loadingFriends || mFetchingState == FetchingState.calculatingCommons) {
            listener.onError("Fetching is already on!");
            return;
        }
        clear();
        loadFriends(listener);
    }

    public void loadFriends(final Listener listener) {
        mFetchingState = FetchingState.loadingFriends;
        VKRequest request = VKApi.friends().get(VKParameters.from("fields", "name"));
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                if (mNeedClearing) {
                    mFetchingState = FetchingState.notStarted;
                    clear();
                    return;
                }
                mUsersFriendsByAlphabet = (VKUsersArray) response.parsedModel;
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
                for (VKApiUserFull friend : getUsersFriends()) {
                    mGroupsCommonWithFriendMap.put(friend, new VKApiCommunityArray());
                    mUserFriendsMap.put(friend.id, friend);
                }
                mFetchingState = FetchingState.calculatingCommons;
                listener.onFriendsFetched();
                loadGroups(listener);
            }

            @Override
            public void onError(VKError error) {
                mFetchingState = FetchingState.notStarted;
                clear();
                listener.onError(String.valueOf(error));
            }
        });
    }

    public void loadGroups(final Listener listener) {
        VKRequest requestIds = VKApi.groups().get(VKParameters.from(VKApiConst.EXTENDED, 1));
        requestIds.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                if (mNeedClearing) {
                    mFetchingState = FetchingState.notStarted;
                    clear();
                    return;
                }
                mUsersGroupsByDefault = (VKApiCommunityArray) response.parsedModel;
                mGroupsSortState = GroupsSortState.byDefault;
                for (VKApiCommunityFull group : mUsersGroupsByDefault) {
                    mFriendsInGroupMap.put(group, new VKUsersArray());
                    mUserGroupsMap.put(group.id, group);
                }
                new CalculateCommonsTask(listener).execute();
            }

            @Override
            public void onError(VKError error) {
                mFetchingState = FetchingState.notStarted;
                clear();
                listener.onError(String.valueOf(error));
            }
        });
    }

    private class CalculateCommonsTask extends AsyncTask<Void, Void, Void> {
        private Listener mListener;

        /**
         * Для сообщений об ошибках, котоорые могут произойти в фоновом режиме, используем следующие переменные.
         */
        private volatile boolean is_calculating_error_happened;
        private volatile String calculating_error;

        /**
         * Сколько запросов осталось выполнить (vkapi.execute).
         */
        private volatile int mRequestsRemain;

        /**
         * Кол-во групп, обрабатываемое в 1 запросе
         * fixme: 24
         */
        private static final int groupPerRequest = 24;

        public CalculateCommonsTask(Listener listener) {
            mListener = listener;
            is_calculating_error_happened = false;
            calculating_error = null;
            mRequestsRemain = mUsersGroupsByDefault.size() / groupPerRequest;
            if (mUsersGroupsByDefault.size() % groupPerRequest > 0) {
                ++mRequestsRemain;
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (mNeedClearing) {
                return null;
            }
            calculateCommonGroups();

            if (mNeedClearing) {
                return null;
            }

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
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (is_calculating_error_happened) {
                // во время выполнения подсчета общих групп произошла ошибка, сообщаем о ней.
                mListener.onError(calculating_error);
            }
            if (mNeedClearing || is_calculating_error_happened) {
                mFetchingState = FetchingState.notStarted;
                clear();
                return;
            }
            // если не надо было вызвать {#clear} и не было ошибок, сообщаем о завершении подсчета общих групп.
            mFetchingState = FetchingState.finished;
            mListener.onCommonsCalculated();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            mListener.onProgress();
        }

        private void calculateCommonGroups() {
            String friendIds = getFriendsIds();
            for (int i = 0; i < mUsersGroupsByDefault.size(); i += groupPerRequest) {
                if (mNeedClearing || is_calculating_error_happened) {
                    --mRequestsRemain;
                    continue;
                }
                final int groupStart = i;
                String code = getCodeToExecute(groupStart, groupPerRequest, friendIds);
                VKRequest request = new VKRequest("execute", VKParameters.from("code", code));
                final long beginTime = System.currentTimeMillis();
                request.executeWithListener(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(VKResponse response) {
                        if (!mNeedClearing && !is_calculating_error_happened) {
                            int currentFriendNumber = 0;
                            int currentGroupNumber = groupStart;
                            try {
                                JSONArray jsonArray = response.json.getJSONArray("response");
                                for (int j = 0; j != jsonArray.length(); ++j) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(j);
                                    if (jsonObject.getInt("member") == 1) {
                                        VKApiUserFull user = mUserFriendsMap.get(jsonObject.getInt("user_id"));
                                        VKApiCommunityFull group = mUsersGroupsByDefault.get(currentGroupNumber);
                                        mGroupsCommonWithFriendMap.get(user).add(group);
                                        mFriendsInGroupMap.get(group).add(user);
                                    }
                                    ++currentFriendNumber;
                                    if (currentFriendNumber == getUsersFriends().size()) {
                                        currentFriendNumber = 0;
                                        ++currentGroupNumber;
                                    }
                                }
                            } catch (JSONException e) {
                                is_calculating_error_happened = true;
                                calculating_error = String.valueOf(e);
                            }
                            publishProgress();
                        }
                        --mRequestsRemain;
                    }

                    @Override
                    public void onError(VKError error) {
                        is_calculating_error_happened = true;
                        calculating_error = String.valueOf(error);
                        --mRequestsRemain;
                    }
                });
                try {
                    // Что бы запросы не посылались слишком часто. (Не больше 3 в секунду).
                    Thread.sleep(350);
                }
                catch (InterruptedException e) {
                    is_calculating_error_happened = true;
                    calculating_error = String.valueOf(e);
                }
            }
            // Ждем, пока не выполнятся все запросы.
            while (mRequestsRemain > 0) {
                Thread.yield();
            }
        }

        private String getFriendsIds() {
            StringBuilder s = new StringBuilder();
            for (VKApiUserFull friend : getUsersFriends()) {
                s.append(friend.id).append(',');
            }
            return s.toString();
        }

        private String getCodeToExecute(int groupStart, int groupPerRequest, String friendIds) {
            return "var ids = \"" + friendIds + "\";" +
                    "var g = API.groups.get();" +
                    "var res = API.groups.isMember({\"group_id\": g.items[" + groupStart + "],\"user_ids\":ids});" +
                    ((groupStart + 1 < mUsersGroupsByDefault.size()) ?
                            "int i = " + (groupStart+1) + ";" +
                                    "while(i<" + Math.min(groupStart + groupPerRequest, mUsersGroupsByDefault.size()) + ")" +
                                    "{res=res+API.groups.isMember({\"group_id\": g.items[i],\"user_ids\":ids});i=i+1;}"
                            : "") +
                    "return  res;";
        }
    }

    /**
     * Друзья пользователя, отсортированные в соответствии с {@link #mFriendsSortState}.
     */
    public VKUsersArray getUsersFriends() {
        switch (mFriendsSortState) {
            case notSorted:
                return new VKUsersArray();
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
                return new VKApiCommunityArray();
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
        return (mGroupsCommonWithFriendMap.get(user) == null) ? new VKApiCommunityArray() : mGroupsCommonWithFriendMap.get(user);
    }

    /**
     * Список друзей в группе.
     */
    @NonNull
    public VKUsersArray getFriendsInGroup(VKApiCommunityFull group) {
        return (mFriendsInGroupMap.get(group) == null) ? new VKUsersArray() : mFriendsInGroupMap.get(group);
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
            mUsersFriendsByAlphabet = null;
            mUsersFriendsByCommons = null;
            mUserFriendsMap = new HashMap<>();

            mUsersGroupsByDefault = null;
            mUsersGroupsByFriends = null;
            mUserGroupsMap = new HashMap<>();

            mGroupsCommonWithFriendMap = new HashMap<>();
            mFriendsInGroupMap = new HashMap<>();

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

}