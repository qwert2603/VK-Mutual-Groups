package com.qwert2603.vkmutualgroups.data;

import org.json.JSONObject;

/**
 * Класс для сохранения и удаления jsonObject.
 */
public interface DataSaver {
    void saveFriends(JSONObject friends);
    void saveGroups(JSONObject groups);
    void saveIsMember(JSONObject jsonObject);
    void clear();
}
