package com.alex.vkmutualgroups.data;

import android.support.annotation.Nullable;

import com.alex.vkmutualgroups.Listener;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKUsersArray;

import org.json.JSONObject;

/**
 * Загрузчик данных.
 */
public interface DataProvider {

    void loadFriends(Listener<VKUsersArray> listener);

    void loadGroups(Listener<VKApiCommunityArray> listener);

    /**
     *
     * @return кол-во JSONObject которое будет передано в {@link DataProvider.LoadIsMemberListener#onProgress(JSONObject)}.
     */
    int loadIsMembers(VKUsersArray friends, VKApiCommunityArray groups, LoadIsMemberListener listener);

    interface LoadIsMemberListener extends Listener<Void> {
        /**
         * Если произошла ошибка, будет передано jsonObject == null,
         * Если хоть раз было передано null, в конце процесса надо вызывать {@link #onError(String)}.
         * Если все успешно загрузилось, то {@link #onCompleted(Object)}.
         */
        void onProgress(@Nullable JSONObject jsonObject);
    }
}