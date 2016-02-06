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
        printDir(internalStorage);
    }

    /**
     * Вывести в logcat содержмое папки. Рекурсивно.
     */
    private static void printDir(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                printDir(file);
            } else {
                Log.d(TAG, file.toString() + " { length = " + file.length() + " }");
            }
        }
    }
}
