package com.qwert2603.vkmutualgroups.util;

import android.app.Activity;
import android.content.Intent;

import com.qwert2603.vkmutualgroups.activities.LoginActivity;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;
import com.vk.sdk.VKSdk;

/**
 * Класс для выхода из ВК.
 */
public class VkLogOutUtil {

    /**
     * Выйти из ВК.
     * @param activity - вызывающая Activity, она будет завершена.
     */
    public static void logOut(Activity activity) {
        if (VKSdk.isLoggedIn()) {
            VKSdk.logout();
            DataManager dataManager = DataManager.get(activity);
            dataManager.clear();
            dataManager.clearDataOnDevice();
            PhotoManager.get(activity).clearPhotosOnDevice();
            Intent intent = new Intent(activity, LoginActivity.class);
            activity.startActivity(intent);
            activity.finish();
        }
    }

}
