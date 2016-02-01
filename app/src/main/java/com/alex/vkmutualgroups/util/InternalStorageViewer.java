package com.alex.vkmutualgroups.util;

import android.content.Context;
import android.util.Log;

import java.io.File;

/**
 * Класс для вывода в logcat содержимое внутреннего хранилища приложения.
 */
public class InternalStorageViewer {

    public static final String TAG = "InternalStorageViewer";

    private Context mContext;

    public InternalStorageViewer(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Вывести в logcat содержимое внутреннего хранилища приложения.
     */
    public void print() {
        File internalStorage = mContext.getFilesDir();
        printDir(internalStorage);
    }

    /**
     * Вывести в logcat содержмое папки. Рекурсивно.
     */
    private void printDir(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                printDir(file);
            } else {
                Log.d(TAG, file.toString() + " { length = " + file.length() + " }");
            }
        }
    }
}
