package com.alex.vkmutualgroups.util;

import android.content.Context;
import android.net.ConnectivityManager;

public class InternetUtils {

    /**
     * Проверить наличие подключения к интернету.
     */
    public static boolean isInternetConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
}
