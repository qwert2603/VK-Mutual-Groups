package com.qwert2603.vkmutualgroups.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;

/**
 * Класс для сохранения данных в файлы в фоновом режиме.
 * И для удаления сохраненных файлов с устройства.
 */
public class DeviceDataSaver implements DataSaver, DeviceDataNames {

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

        @SuppressWarnings("ResultOfMethodCallIgnored")
        private void handleSave(Data data) {
            try {
                File fileFriends = new File(mContext.getFilesDir(), FILENAME_FRIENDS);
                JSONObject jsonObjectFriends = data.mFriends.fields;
                doSave(fileFriends, jsonObjectFriends);

                File fileGroups = new File(mContext.getFilesDir(), FILENAME_GROUPS);
                JSONObject jsonObjectGroups = data.mGroups.fields;
                doSave(fileGroups, jsonObjectGroups);

                File fileIsMember = new File(mContext.getFilesDir(), FILENAME_IS_MEMBER);
                JSONArray jsonArrayIsMember = new JSONArray();
                for (Map.Entry<Integer, ArrayList<Integer>> entry : data.mIsMember.entrySet()) {
                    JSONObject jsonObjectFriend = new JSONObject();
                    jsonObjectFriend.put(JSON_FRIEND_ID, entry.getKey());
                    JSONArray jsonArrayGroups = new JSONArray();
                    for (Integer group_id : entry.getValue()) {
                        jsonArrayGroups.put(group_id);
                    }
                    jsonObjectFriend.put(JSON_GROUPS_ID_LIST, jsonArrayGroups);
                    jsonArrayIsMember.put(jsonObjectFriend);
                }
                doSave(fileIsMember, jsonArrayIsMember);
            } catch (IOException | JSONException e) {
                Log.e(TAG, e.toString(), e);
                handleClear();
            }
        }

        private void doSave(File file, Object object) throws IOException {
            OutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(file);
                outputStream.write(object.toString().getBytes());
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
            File friends = new File(mContext.getFilesDir(), FILENAME_FRIENDS);
            friends.delete();
            File groups = new File(mContext.getFilesDir(), FILENAME_GROUPS);
            groups.delete();
            File is_member = new File(mContext.getFilesDir(), FILENAME_IS_MEMBER);
            is_member.delete();
        }
    }

}
