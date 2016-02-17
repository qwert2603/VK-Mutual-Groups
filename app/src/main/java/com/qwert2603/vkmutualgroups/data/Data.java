package com.qwert2603.vkmutualgroups.data;

import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKUsersArray;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Структура данных, которые надо загрузить или сохранить.
 */
public class Data {
    public volatile VKUsersArray mFriends;
    public volatile VKApiCommunityArray mGroups;
    public volatile HashMap<Integer, ArrayList<Integer>> mIsMember;
}
