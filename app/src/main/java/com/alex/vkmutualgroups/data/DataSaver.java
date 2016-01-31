package com.alex.vkmutualgroups.data;

import org.json.JSONObject;

/**
 * Класс для сохранения jsonObject.
 */
public interface DataSaver {
    void saveFriends(JSONObject friends);
    void saveGroups(JSONObject groups);
    void saveIsMember(JSONObject jsonObject);
    void clear();
}
