package com.qwert2603.vkmutualgroups.photo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.LruCache;

import com.qwert2603.vkmutualgroups.Listener;
import com.qwert2603.vkmutualgroups.fragments.SettingsFragment;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiUserFull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Загружает и хранит фотографии - аватарки друзей и групп.
 */
public class PhotoManager {

    private static final String TAG = "PhotoManager";

    private static PhotoManager sPhotoManager;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private PhotoManager(Context context) {
        mPhotoFolder = new File(context.getApplicationContext().getFilesDir(), PHOTOS_FOLDER);
        mPhotoFolder.mkdirs();

        mIsCacheImagesOnDevice = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(SettingsFragment.PREF_IS_CACHE_IMAGES_ON_DEVICE, false);

        mPhotoFetchingThread = new PhotoFetchingThread(new Handler());
        mPhotoFetchingThread.start();
        mPhotoFetchingThread.getLooper();
    }

    public static PhotoManager get(Context context) {
        if (sPhotoManager == null) {
            sPhotoManager = new PhotoManager(context);
        }
        return sPhotoManager;
    }

    private PhotoFetchingThread mPhotoFetchingThread;

    /**
     * Название папки с сохраненными фото на устройстве.
     */
    private static final String PHOTOS_FOLDER = "VK_Common_Groups_Photos";

    /**
     * Папка с сохраненными фото на устройстве.
     */
    private File mPhotoFolder;

    /**
     * Аватарки друзей и групп.
     */
    private LruCache<String, Bitmap> mPhotos = new LruCache<>(2048);

    /**
     * Кешировать ли изображения в памяти телефона.
     */
    private volatile boolean mIsCacheImagesOnDevice;

    public void setIsCacheImagesOnDevice(boolean cacheImages) {
        mIsCacheImagesOnDevice = cacheImages;
    }

    @SuppressWarnings("unused")
    public boolean getIsCacheImagesOnDevice() {
        return mIsCacheImagesOnDevice;
    }

    /**
     * Получить фото по указанному url, если оно было загружено ранее.
     */
    @Nullable
    public Bitmap getPhoto(String url) {
        return mPhotos.get(url);
    }

    /**
     * Загрузить фото по url.
     */
    public void fetchPhoto(final String url, @Nullable final Listener<Bitmap> listener) {
        mPhotoFetchingThread.fetchPhoto(url, listener);
    }

    /**
     * Удалить сохраненные на устройстве фото.
     */
    public void clearPhotosOnDevice() {
        new AsyncTask<Void, Void, Void>() {
            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            protected Void doInBackground(Void... params) {
                for (File f : mPhotoFolder.listFiles()) {
                    f.delete();
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Поток для загрузки фото.
     */
    private class PhotoFetchingThread extends HandlerThread {
        private Handler mHandler;
        private Handler mResponseHandler;

        public PhotoFetchingThread(Handler responseHandler) {
            super("PhotoFetchingThread");
            mResponseHandler = responseHandler;
        }

        @Override
        @SuppressLint("HandlerLeak")
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Blob blob = (Blob) msg.obj;
                    handleFetch(blob.mUrl, blob.mListener);
                }
            };
        }

        private class Blob {
            String mUrl;
            Listener<Bitmap> mListener;
        }

        public void fetchPhoto(String url, @Nullable Listener<Bitmap> listener) {
            while (mHandler == null) {
                Thread.yield();
            }

            Blob blob = new Blob();
            blob.mUrl = url;
            blob.mListener = listener;
            mHandler.obtainMessage(0, blob).sendToTarget();
        }

        private void handleFetch(String url, @Nullable Listener<Bitmap> listener) {
            Bitmap bitmap = getBitmap(url);
            mResponseHandler.post(() -> {
                if (bitmap != null) {
                    mPhotos.put(url, bitmap);
                    if (listener != null) {
                        listener.onCompleted(bitmap);
                    }
                } else {
                    if (listener != null) {
                        listener.onError("Fetching photo failed!");
                    }
                }
            });
        }

        /**
         * Поочередно искать фото:
         * - в {@link #mPhotos};
         * - в памяти устройства;
         * - в интернете.
         * Будет возвращено как только будет найдено.
         * Также оно будет сохранено в памяти телефона, если его там не было.
         */
        private Bitmap getBitmap(String url) {
            Bitmap result = mPhotos.get(url);
            if (result != null) {
                return result;
            }

            result = loadBitmapFromDevice(url);
            if (result != null) {
                return result;
            }

            result = downloadBitmap(url);
            if (result != null && mIsCacheImagesOnDevice) {
                saveBitmapToDevice(url, result);
            }
            return result;
        }

        /**
         * Сохранить фото в память устройства.
         */
        private void saveBitmapToDevice(String url, Bitmap bitmap) {
            File f = new File(mPhotoFolder, url.substring(url.length() - 15));
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(f);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            } catch (IOException e) {
                Log.e(TAG, e.toString(), e);
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        /**
         * Загрузить фото из памяти устройства (если оно там было).
         */
        @Nullable
        private Bitmap loadBitmapFromDevice(String url) {
            File f = new File(mPhotoFolder, url.substring(url.length() - 15));
            return BitmapFactory.decodeFile(f.getAbsolutePath());
        }

        /**
         * Загрузить изображение по переданному адресу из интернета.
         */
        private Bitmap downloadBitmap(String urlString) {
            Bitmap bitmap = null;
            InputStream inputStream = null;
            try {
                URL url = new URL(urlString);
                inputStream = url.openStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                Log.e(TAG, e.toString(), e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
            return bitmap;
        }
    }

    public String getUserPhotoUrl(VKApiUserFull user) {
        return user.photo_50;
    }

    public String getGroupPhotoUrl(VKApiCommunityFull group) {
        return group.photo_50;
    }

}