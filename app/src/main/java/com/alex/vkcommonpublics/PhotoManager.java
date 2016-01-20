package com.alex.vkcommonpublics;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.LruCache;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Загружает и хранит фотографии - аватарки друзей и групп.
 *
 * При закрытии приложения надо обязательно вызвать {@link #quitDownloadingThread()} для завершения потока загрузки фото.
 */
public class PhotoManager {

    private static PhotoManager sPhotoManager = new PhotoManager();

    private PhotoManager() {
        mPhotoDownloadingThread = new PhotoDownloadingThread(new Handler(Looper.getMainLooper()));
        mPhotoDownloadingThread.start();
        mPhotoDownloadingThread.getLooper();
    }

    public static PhotoManager get() {
        return sPhotoManager;
    }

    /**
     * Поток для загрузки фото.
     */
    private PhotoDownloadingThread mPhotoDownloadingThread;

    /**
     * Аватарки друзей и групп.
     */
    //private Map<String, Bitmap> mPhotos = new PhotosMap();
    // FIXME: 20.01.2016
    private LruCache<String, Bitmap> mPhotos = new LruCache<>(2048);
    /*private static class PhotosMap extends HashMap<String, Bitmap> {
        @Override
        public Bitmap put(String key, Bitmap value) {
            // Чтобы не хранилось слишком много.
            if (size() >= 2048) {
                clear();
            }
            return super.put(key, value);
        }
    }*/

    @Nullable
    public Bitmap getPhoto(String url) {
        return mPhotos.get(url);
    }

    /**
     * Загрузить фото по url.
     * Результат будет передан в listener.
     * Также загруженное фото будет сохранено в mPhotos.
     */
    public void fetchPhoto(final String url, final Listener<Bitmap> listener) {
        if (mPhotos.get(url) != null) {
            listener.onCompleted(mPhotos.get(url));
        }

        mPhotoDownloadingThread.downloadPhoto(url, new Listener<Bitmap>() {
            @Override
            public void onCompleted(Bitmap bitmap) {
                mPhotos.put(url, bitmap);
                listener.onCompleted(bitmap);
            }

            @Override
            public void onError(String e) {
                listener.onError(e);
            }
        });
    }

    /**
     * Завершить поток загрузки фото.
     */
    public void quitDownloadingThread() {
        mPhotoDownloadingThread.quit();
    }

    /**
     * Класс-поток-загрузчик фото.
     */
    private class PhotoDownloadingThread extends HandlerThread {

        private static final int MESSAGE_DOWNLOAD_PHOTO = 1;

        /**
         * Обработчик сообщений о загрузке.
         */
        private Handler mDownloadingHandler;

        /**
         * Для соотнесения фото и слушателя его загрузки.
         * И чтобы повторно не загружать одно и тоже.
         */
        private Map<String, Listener<Bitmap>> mListenerMap = Collections.synchronizedMap(new HashMap<String, Listener<Bitmap>>());

        /**
         * Обработчик для результатов загрузки (и ошибок тоже)
         */
        private Handler mResponseHandler;

        public PhotoDownloadingThread(Handler responseHandler) {
            super("PhotoDownloadingThread");
            mResponseHandler = responseHandler;
        }

        @SuppressWarnings("HandlerLeak")
        @Override
        protected void onLooperPrepared() {
            mDownloadingHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == MESSAGE_DOWNLOAD_PHOTO) {
                        handleDownloadingPhoto((String) msg.obj);
                    }
                }
            };
        }

        /**
         * Загрузить фото по url.
         * Результат будет передан в listener.
         */
        public void downloadPhoto(String url, Listener<Bitmap> listener) {
            while (mDownloadingHandler == null) {
                Thread.yield();
            }
            mListenerMap.put(url, listener);
            mDownloadingHandler.obtainMessage(MESSAGE_DOWNLOAD_PHOTO, url).sendToTarget();
        }

        /**
         * Выполнить загрузку, сохранить в PhotoManager#mPhotos и передать результат.
         */
        private void handleDownloadingPhoto(final String urlString) {
            final Listener<Bitmap> listener = mListenerMap.get(urlString);
            if (listener == null) {
                return;
            }

            if (mPhotos.get(urlString) != null) {
                mResponseHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onCompleted(mPhotos.get(urlString));
                        mListenerMap.remove(urlString);
                    }
                });
            }

            try {
                final Bitmap bitmap = downloadBitmap(urlString);
                mResponseHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onCompleted(bitmap);
                        mListenerMap.remove(urlString);
                    }
                });
            }
            catch (final IOException e) {
                mResponseHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onError(String.valueOf(e));
                        mListenerMap.remove(urlString);
                    }
                });
            }
        }

        /**
         * Загрузить изображение по переданному адресу.
         */
        private Bitmap downloadBitmap(String urlString) throws IOException {
            URL url = new URL(urlString);
            InputStream inputStream = url.openStream();
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            return bitmap;
        }
    }

}