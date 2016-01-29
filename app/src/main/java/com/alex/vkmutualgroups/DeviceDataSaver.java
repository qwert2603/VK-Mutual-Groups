package com.alex.vkmutualgroups;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

/**
 * Класс для сохранения jsonObject в файл в фоновом режиме.
 */
public class DeviceDataSaver implements DataSaver, DeviceDataFilenames {

    public static final String TAG = "DeviceDataSaver";

    private Context mContext;

    public DeviceDataSaver(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void saveFriends(JSONObject friends) {
        File file = new File(mContext.getFilesDir(), JSON_FILENAME_FRIENDS);
        saveToJSONFile(file, friends);
    }

    @Override
    public void saveGroups(JSONObject groups) {
        File file = new File(mContext.getFilesDir(), JSON_FILENAME_GROUPS);
        saveToJSONFile(file, groups);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void saveIsMember(JSONObject jsonObject) {
        File folder = new File(mContext.getFilesDir(), JSON_FOLDER_MUTUALS);
        folder.mkdirs();
        File file = new File(folder, new Random().nextInt() + JSON_FILENAME_SUFFIX);
        saveToJSONFile(file, jsonObject);
    }

    /**
     * Сохранить jsonObject в файл в фоновом режиме.
     */
    private void saveToJSONFile(final File file, final JSONObject jsonObject) {
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                OutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(file);
                    outputStream.write(jsonObject.toString().getBytes());
                }
                catch (IOException e) {
                    Log.e(TAG, e.toString(), e);
                }
                finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Удалить все сохраненные на устройстве данные.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void clear() {
        File friends = new File(mContext.getFilesDir(), JSON_FILENAME_FRIENDS);
        friends.delete();
        File groups = new File(mContext.getFilesDir(), JSON_FILENAME_GROUPS);
        groups.delete();
        File folder = new File(mContext.getFilesDir(), JSON_FOLDER_MUTUALS);
        folder.mkdirs();
        for (File file : folder.listFiles()) {
            file.delete();
        }
    }

}