package com.qwert2603.vkmutualgroups.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.qwert2603.vkmutualgroups.Listener;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKUsersArray;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Загрузчик данных из памяти устройства.
 */
public class DeviceDataProvider implements DataProvider, DeviceDataFilenames {

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
    public void loadFriends(final Listener<VKUsersArray> listener) {
        File file = new File(mContext.getFilesDir(), JSON_FILENAME_FRIENDS);
        mLoadingThread.loadFriends(file, listener);
    }

    @Override
    public void loadGroups(final Listener<VKApiCommunityArray> listener) {
        File file = new File(mContext.getFilesDir(), JSON_FILENAME_GROUPS);
        mLoadingThread.loadGroups(file, listener);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void loadIsMembers(VKUsersArray friends, VKApiCommunityArray groups, Listener<ArrayList<JSONObject>> listener) {
        File folder = new File(mContext.getFilesDir(), JSON_FOLDER_MUTUALS);
        folder.mkdirs();
        mLoadingThread.loadIsMember(folder, listener);
    }

    private class LoadingThread extends HandlerThread {

        private static final int MESSAGE_LOAD_FRIENDS = 1;
        private static final int MESSAGE_LOAD_GROUPS = 2;
        private static final int MESSAGE_LOAD_IS_MEMBER = 3;

        private volatile Handler mHandler;
        private Handler mResponseHandler;

        public LoadingThread(Handler responseHandler) {
            super("LoadingThread");
            mResponseHandler = responseHandler;
        }

        @Override
        @SuppressLint("HandlerLeak")
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MESSAGE_LOAD_FRIENDS:
                            BlobFriends blobFriends = (BlobFriends) msg.obj;
                            handleLoadFriends(blobFriends.mFile, blobFriends.mListener);
                            break;
                        case MESSAGE_LOAD_GROUPS:
                            BlobGroups blobGroups = (BlobGroups) msg.obj;
                            handleLoadGroups(blobGroups.mFile, blobGroups.mListener);
                            break;
                        case MESSAGE_LOAD_IS_MEMBER:
                            BlobIsMember blobIsMember = (BlobIsMember) msg.obj;
                            handleLoadIsMember(blobIsMember.mDir, blobIsMember.mListener);
                            break;
                    }
                }
            };
        }

        private class BlobFriends {
            File mFile;
            Listener<VKUsersArray> mListener;
        }

        public void loadFriends(File file, Listener<VKUsersArray> listener) {
            while (mHandler == null) {
                Thread.yield();
            }

            BlobFriends blob = new BlobFriends();
            blob.mFile = file;
            blob.mListener = listener;
            mHandler.obtainMessage(MESSAGE_LOAD_FRIENDS, blob).sendToTarget();
        }

        private class BlobGroups {
            File mFile;
            Listener<VKApiCommunityArray> mListener;
        }

        public void loadGroups(File file, Listener<VKApiCommunityArray> listener) {
            while (mHandler == null) {
                Thread.yield();
            }

            BlobGroups blob = new BlobGroups();
            blob.mFile = file;
            blob.mListener = listener;
            mHandler.obtainMessage(MESSAGE_LOAD_GROUPS, blob).sendToTarget();
        }

        private class BlobIsMember {
            File mDir;
            Listener<ArrayList<JSONObject>> mListener;
        }

        public void loadIsMember(File dir, Listener<ArrayList<JSONObject>> listener) {
            while (mHandler == null) {
                Thread.yield();
            }

            BlobIsMember blob = new BlobIsMember();
            blob.mDir = dir;
            blob.mListener = listener;
            mHandler.obtainMessage(MESSAGE_LOAD_IS_MEMBER, blob).sendToTarget();
        }

        private void handleLoadFriends(File file, Listener<VKUsersArray> listener) {
            try {
                JSONObject jsonObject = loadJSONObjectFromJSONFile(file);
                VKUsersArray friends = new VKUsersArray();
                friends.parse(jsonObject);
                mResponseHandler.post(() -> listener.onCompleted(friends));
            } catch (IOException | JSONException e) {
                mResponseHandler.post(() -> listener.onError(String.valueOf(e)));
            }
        }

        private void handleLoadGroups(File file, Listener<VKApiCommunityArray> listener) {
            try {
                JSONObject jsonObject = loadJSONObjectFromJSONFile(file);
                VKApiCommunityArray groups = new VKApiCommunityArray();
                groups.parse(jsonObject);
                mResponseHandler.post(() -> listener.onCompleted(groups));
            } catch (IOException | JSONException e) {
                mResponseHandler.post(() -> listener.onError(String.valueOf(e)));
            }
        }

        private void handleLoadIsMember(File dir, Listener<ArrayList<JSONObject>> listener) {
            String errorMessage = null;
            ArrayList<JSONObject> result = new ArrayList<>();
            for (File file : dir.listFiles()) {
                if (errorMessage != null) {
                    continue;
                }
                JSONObject jsonObject = null;
                try {
                    jsonObject = loadJSONObjectFromJSONFile(file);
                } catch (IOException | JSONException e) {
                    errorMessage = String.valueOf(e);
                }
                result.add(jsonObject);
            }
            if (errorMessage == null) {
                mResponseHandler.post(() -> listener.onCompleted(result));
            }
            else {
                String err = errorMessage;
                mResponseHandler.post(() -> listener.onError(err));
            }
        }

        private JSONObject loadJSONObjectFromJSONFile(File file) throws IOException, JSONException {
            InputStream inputStream = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            inputStream.close();
            JSONObject jsonObject = new JSONObject(stringBuilder.toString());
            new JSONObject();
            return jsonObject;
        }
    }

}
