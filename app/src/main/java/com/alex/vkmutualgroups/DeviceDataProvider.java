package com.alex.vkmutualgroups;

import android.content.Context;
import android.os.AsyncTask;

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

/**
 * Загрузчик данных из памяти устройства.
 */
public class DeviceDataProvider implements DataProvider, DeviceDataFilenames {

    private Context mContext;

    public DeviceDataProvider(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void loadFriends(final Listener<VKUsersArray> listener) {
        new AsyncTask<Void, Void, VKUsersArray>(){
            /**
             * Ошибка, произошедшая во время {@link #doInBackground(Void...)} )}.
             * Если == null, то ошибок не было.
             */
            private volatile String mErrorMessage = null;

            @Override
            protected VKUsersArray doInBackground(Void... params) {
                VKUsersArray friends = new VKUsersArray();
                try {
                    File file = new File(mContext.getFilesDir(), JSON_FILENAME_FRIENDS);
                    JSONObject jsonObject = loadJSONObjectFromJSONFile(file);
                    friends = new VKUsersArray();
                    friends.parse(jsonObject);

                } catch (IOException | JSONException e) {
                    mErrorMessage = String.valueOf(e);
                }
                return friends;
            }

            @Override
            protected void onPostExecute(VKUsersArray friends) {
                if (mErrorMessage == null) {
                    listener.onCompleted(friends);
                }
                else {
                    listener.onError(mErrorMessage);
                }
            }
        }.execute();
    }

    @Override
    public void loadGroups(final Listener<VKApiCommunityArray> listener) {
        new AsyncTask<Void, Void, VKApiCommunityArray>(){
            /**
             * Ошибка, произошедшая во время {@link #doInBackground(Void...)} )}.
             * Если == null, то ошибок не было.
             */
            private volatile String mErrorMessage = null;

            @Override
            protected VKApiCommunityArray doInBackground(Void... params) {
                VKApiCommunityArray groups = new VKApiCommunityArray();
                try {
                    File file = new File(mContext.getFilesDir(), JSON_FILENAME_GROUPS);
                    JSONObject jsonObject = loadJSONObjectFromJSONFile(file);
                    groups = new VKApiCommunityArray();
                    groups.parse(jsonObject);

                } catch (IOException | JSONException e) {
                    mErrorMessage = String.valueOf(e);
                }
                return groups;
            }

            @Override
            protected void onPostExecute(VKApiCommunityArray friends) {
                if (mErrorMessage == null) {
                    listener.onCompleted(friends);
                }
                else {
                    listener.onError(mErrorMessage);
                }
            }
        }.execute();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public int loadIsMembers(VKUsersArray friends, VKApiCommunityArray groups, final LoadIsMemberListener listener) {
        final File folder = new File(mContext.getFilesDir(), JSON_FOLDER_MUTUALS);
        folder.mkdirs();

        new AsyncTask<Void, JSONObject, Void>(){
            /**
             * Ошибка, произошедшая во время {@link #doInBackground(Void...)} )}.
             * Если == null, то ошибок не было.
             */
            private volatile String mErrorMessage = null;

            @SuppressWarnings("NullArgumentToVariableArgMethod")
            @Override
            protected Void doInBackground(Void... params) {
                for (File file : folder.listFiles()) {
                    JSONObject jsonObject = null;
                    if (mErrorMessage == null) {
                        try {
                            jsonObject = loadJSONObjectFromJSONFile(file);
                        } catch (IOException | JSONException e) {
                            mErrorMessage = String.valueOf(e);
                        }
                    }
                    publishProgress(jsonObject);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(JSONObject... values) {
                listener.onProgress(values[0]);
            }

            @Override
            protected void onPostExecute(Void friends) {
                if (mErrorMessage == null) {
                    listener.onCompleted(null);
                }
                else {
                    listener.onError(mErrorMessage);
                }
            }
        }.execute();

        return folder.list().length;
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
        return new JSONObject(stringBuilder.toString());
    }
}
