package com.qwert2603.vkmutualgroups.data;

import com.qwert2603.vkmutualgroups.Listener;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKUsersArray;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Загрузчик данных.
 */
public interface DataProvider {
    void loadFriends(Listener<VKUsersArray> listener);
    void loadGroups(Listener<VKApiCommunityArray> listener);
    void loadIsMembers(VKUsersArray friends, VKApiCommunityArray groups, Listener<ArrayList<JSONObject>> listener);
}