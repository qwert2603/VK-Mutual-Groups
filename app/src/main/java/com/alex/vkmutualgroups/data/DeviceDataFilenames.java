package com.alex.vkmutualgroups.data;

/**
 * Имена файлов с данными на устройстве.
 */
public interface DeviceDataFilenames {
    String JSON_FILENAME_SUFFIX = ".json";
    String JSON_FILENAME_FRIENDS = "friends" + JSON_FILENAME_SUFFIX;
    String JSON_FILENAME_GROUPS = "groups" + JSON_FILENAME_SUFFIX;
    String JSON_FOLDER_MUTUALS = "is_member";
}
