package com.qwert2603.vkmutualgroups.util;

import android.content.Context;
import android.util.Log;

import java.io.File;

/**
 * Класс для вывода в logcat содержимое внутреннего хранилища приложения.
 */
public class InternalStorageViewer {

    public static final String TAG = "InternalStorageViewer";

    /**
     * Вывести в logcat содержимое внутреннего хранилища приложения.
     */
    public static void print(Context context) {
        File internalStorage = context.getApplicationContext().getFilesDir();
        Log.d(TAG, "## INTERNAL STORAGE START ##");
        int totalLength = printDir(internalStorage);
        Log.d(TAG, "## INTERNAL STORAGE END ## total length == " + totalLength);
    }

    /**
     * Вывести в logcat содержмое папки. Рекурсивно.
     * Вернет общий размер папка с учетом размеров вложенных папок.
     */
    private static int printDir(File dir) {
        int length = 0;
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                length += printDir(file);
            } else {
                Log.d(TAG, file.toString() + " { length = " + file.length() + " }");
                length += file.length();
            }
        }
        return length;
    }
}
