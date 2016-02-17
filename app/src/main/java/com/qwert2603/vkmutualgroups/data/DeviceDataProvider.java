package com.qwert2603.vkmutualgroups.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.qwert2603.vkmutualgroups.Listener;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKUsersArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Загрузчик данных из памяти устройства.
 */
public class DeviceDataProvider implements DataProvider, DeviceDataNames {

    @SuppressWarnings("unused")
    private static final String TAG = "DeviceDataProvider";

    private static DeviceDataProvider sDeviceDataProvider;

    public static DeviceDataProvider get(Context context) {
        if (sDeviceDataProvider == null) {
            sDeviceDataProvider = new DeviceDataProvider(context);
        }
        return sDeviceDataProvider;
    }

    private Context mContext;

    private LoadingThread mLoadingThread;

    private DeviceDataProvider(Context context) {
        mContext = context.getApplicationContext();
        mLoadingThread = new LoadingThread(new Handler(Looper.getMainLooper()));
        mLoadingThread.start();
        mLoadingThread.getLooper();
    }

    @Override
    public void load(Listener<Data> listener) {
        mLoadingThread.load(listener);
    }

    private class LoadingThread extends HandlerThread {

        private volatile Handler mHandler;
        private Handler mResponseHandler;

        public LoadingThread(Handler responseHandler) {
            super("LoadingThread");
            mResponseHandler = responseHandler;
        }

        @Override
        @SuppressLint("HandlerLeak")
        @SuppressWarnings("unchecked")
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    handleLoad((Listener<Data>) msg.obj);
                }
            };
        }

        public void load(Listener<Data> listener) {
            while (mHandler == null) {
                Thread.yield();
            }

            mHandler.obtainMessage(0, listener).sendToTarget();
        }

        private void handleLoad(Listener<Data> listener) {
            try {
                Data data = new Data();

                File friendsFile = new File(mContext.getFilesDir(), FILENAME_FRIENDS);
                VKUsersArray friends = new VKUsersArray();
                JSONObject jsonObjectFriends = new JSONObject(loadFile(friendsFile));
                friends.parse(jsonObjectFriends);
                data.mFriends = friends;

                File groupsFile = new File(mContext.getFilesDir(), FILENAME_GROUPS);
                VKApiCommunityArray groups = new VKApiCommunityArray();
                JSONObject jsonObjectGroups = new JSONObject(loadFile(groupsFile));
                groups.parse(jsonObjectGroups);
                data.mGroups = groups;

                File isMemberFile = new File(mContext.getFilesDir(), FILENAME_IS_MEMBER);
                JSONArray isMemberJSONArray = new JSONArray(loadFile(isMemberFile));
                data.mIsMember = parseIsMember(isMemberJSONArray);

                mResponseHandler.post(() -> listener.onCompleted(data));
            } catch (IOException | JSONException e) {
                Log.e(TAG, e.toString(), e);
                mResponseHandler.post(() -> listener.onError(String.valueOf(e)));
            }
        }

        private String loadFile(File file) throws IOException {
            InputStream inputStream = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            inputStream.close();
            return stringBuilder.toString();
        }

        private HashMap<Integer, ArrayList<Integer>> parseIsMember(JSONArray jsonArray) throws JSONException {
            HashMap<Integer, ArrayList<Integer>> result = new HashMap<>();
            int jsonArrayLength = jsonArray.length();
            for (int i = 0; i < jsonArrayLength; ++i) {
                JSONObject jsonObjectFriend = jsonArray.getJSONObject(i);
                ArrayList<Integer> groups = new ArrayList<>();
                JSONArray jsonArrayGroups = jsonObjectFriend.getJSONArray(JSON_GROUPS_ID_LIST);
                int jsonArrayGroupsLength = jsonArrayGroups.length();
                for (int j = 0; j < jsonArrayGroupsLength; ++j) {
                    groups.add(jsonArrayGroups.getInt(j));
                }
                result.put(jsonObjectFriend.getInt(JSON_FRIEND_ID), groups);
            }
            return result;
        }
    }

}
