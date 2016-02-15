package com.qwert2603.vkmutualgroups.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

/**
 * Класс для сохранения jsonObject в файл в фоновом режиме.
 * И для удаления сохраненных файлов с устройства.
 */
public class DeviceDataSaver implements DataSaver, DeviceDataFilenames {

    public static final String TAG = "DeviceDataSaver";

    private static DeviceDataSaver sDeviceDataSaver;

    public static DeviceDataSaver get(Context context) {
        if (sDeviceDataSaver == null) {
            sDeviceDataSaver = new DeviceDataSaver(context);
        }
        return sDeviceDataSaver;
    }

    private DeviceDataSavingThread mDeviceDataSavingThread;

    private Context mContext;

    private DeviceDataSaver(Context context) {
        mContext = context.getApplicationContext();
        mDeviceDataSavingThread = new DeviceDataSavingThread();
        mDeviceDataSavingThread.start();
        mDeviceDataSavingThread.getLooper();
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
    private void saveToJSONFile(File file, JSONObject jsonObject) {
        mDeviceDataSavingThread.saveToJSONFile(file, jsonObject);
    }

    /**
     * Удалить все сохраненные на устройстве данные.
     */
    @Override
    public void clear() {
        mDeviceDataSavingThread.clear();
    }

    /**
     * Поток для сохранения файлов и удаления сохраненных файлов.
     */
    private class DeviceDataSavingThread extends HandlerThread {

        private static final int MESSAGE_SAVE = 1;
        private static final int MESSAGE_CLEAR = 2;

        private Handler mHandler;

        public DeviceDataSavingThread() {
            super("DeviceDataSavingThread");
        }

        @Override
        @SuppressLint("HandlerLeak")
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MESSAGE_SAVE:
                            Blob blob = (Blob) msg.obj;
                            handleSaveToJSONFile(blob.mFile, blob.mJSONObject);
                            break;
                        case MESSAGE_CLEAR:
                            handleClear();
                            break;
                    }
                }
            };
        }

        private class Blob {
            File mFile;
            JSONObject mJSONObject;
        }

        public void saveToJSONFile(File file, JSONObject jsonObject) {
            while (mHandler == null) {
                Thread.yield();
            }

            Blob blob = new Blob();
            blob.mFile = file;
            blob.mJSONObject = jsonObject;
            mHandler.obtainMessage(MESSAGE_SAVE, blob).sendToTarget();
        }

        public void clear() {
            while (mHandler == null) {
                Thread.yield();
            }

            mHandler.obtainMessage(MESSAGE_CLEAR).sendToTarget();
        }

        private void handleSaveToJSONFile(File file, JSONObject jsonObject) {
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
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        private void handleClear() {
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

}