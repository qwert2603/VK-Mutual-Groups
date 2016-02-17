package com.qwert2603.vkmutualgroups.data;

/**
 * Имена файлов с данными на устройстве.
 * И ключи для создания и парсинга JSON объектов с информацией о друзьях в группах.
 */
public interface DeviceDataNames {
    String FILENAME_SUFFIX = ".json";
    String FILENAME_FRIENDS = "friends" + FILENAME_SUFFIX;
    String FILENAME_GROUPS = "groups" + FILENAME_SUFFIX;
    String FILENAME_IS_MEMBER = "is_member" + FILENAME_SUFFIX;

    String JSON_FRIEND_ID = "friend_id";
    String JSON_GROUPS_ID_LIST = "groups_id_list";
}
