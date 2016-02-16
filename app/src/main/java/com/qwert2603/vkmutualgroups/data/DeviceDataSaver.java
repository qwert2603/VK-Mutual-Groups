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
import java.util.ArrayList;
import java.util.Random;

/**
 * Класс для сохранения данных в файлы в фоновом режиме.
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
    public void save(Data data) {
        mDeviceDataSavingThread.save(data);
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

        private volatile Handler mHandler;

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
                            handleSave((Data) msg.obj);
                            break;
                        case MESSAGE_CLEAR:
                            handleClear();
                            break;
                    }
                }
            };
        }

        public void save(Data data) {
            while (mHandler == null) {
                Thread.yield();
            }

            mHandler.obtainMessage(MESSAGE_SAVE, data).sendToTarget();
        }

        public void clear() {
            while (mHandler == null) {
                Thread.yield();
            }

            mHandler.removeMessages(MESSAGE_SAVE);
            mHandler.obtainMessage(MESSAGE_CLEAR).sendToTarget();
        }

        private class Blob {
            File mFile;
            JSONObject mJSONObject;
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        private void handleSave(Data data) {
            ArrayList<Blob> arrayList = new ArrayList<>();

            Blob blob = new Blob();
            blob.mFile = new File(mContext.getFilesDir(), JSON_FILENAME_FRIENDS);
            blob.mJSONObject = data.mFriends.fields;
            arrayList.add(blob);

            blob = new Blob();
            blob.mFile = new File(mContext.getFilesDir(), JSON_FILENAME_GROUPS);
            blob.mJSONObject = data.mGroups.fields;
            arrayList.add(blob);

            File folder = new File(mContext.getFilesDir(), JSON_FOLDER_MUTUALS);
            folder.mkdirs();
            for (JSONObject jsonObject : data.mIsMember) {
                blob = new Blob();
                blob.mFile = new File(folder, new Random().nextInt() + JSON_FILENAME_SUFFIX);
                blob.mJSONObject = jsonObject;
                arrayList.add(blob);
            }

            for (Blob b : arrayList) {
                if (! doSave(b.mFile, b.mJSONObject)) {
                    handleClear();
                    return;
                }
            }
        }

        private boolean doSave(File file, JSONObject jsonObject) {
            OutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(file);
                outputStream.write(jsonObject.toString().getBytes());
                return true;
            }
            catch (IOException e) {
                Log.e(TAG, e.toString(), e);
                return false;
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
