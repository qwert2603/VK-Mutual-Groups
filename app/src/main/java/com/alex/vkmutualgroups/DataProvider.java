package com.alex.vkmutualgroups;

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
     * @return кол-во JSONObject которое будет передано в
     * {@link DataProvider.LoadIsMemberListener#onProgress(JSONObject)}.
     */
    int loadIsMembers(VKUsersArray friends, VKApiCommunityArray groups, LoadIsMemberListener listener);

    interface LoadIsMemberListener extends Listener<Void> {
        void onProgress(JSONObject jsonObject);
    }
}