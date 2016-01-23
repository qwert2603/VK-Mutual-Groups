package com.alex.vkcommonpublics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Загружает и хранит фотографии - аватарки друзей и групп.
 *
 * При закрытии приложения надо обязательно вызвать {@link #quitLoadingThread()} для завершения потока загрузки фото.
 */
public class PhotoManager {

    private static final String TAG = "PhotoManager";

    private static PhotoManager sPhotoManager;

    private PhotoManager(Context context) {
        mPhotoProcessingThread = new PhotoProcessingThread(new Handler(Looper.getMainLooper()), context);
        mPhotoProcessingThread.start();
        mPhotoProcessingThread.getLooper();
    }

    public static PhotoManager get(Context context) {
        if (sPhotoManager == null) {
            sPhotoManager = new PhotoManager(context);
        }
        return sPhotoManager;
    }

    /**
     * Поток для загрузки фото.
     */
    private PhotoProcessingThread mPhotoProcessingThread;

    /**
     * Аватарки друзей и групп.
     */
    private LruCache<String, Bitmap> mPhotos = new LruCache<>(2048);

    /**
     * Получить фото по указанному url, если оно было загружено ранее.
     */
    @Nullable
    public Bitmap getPhoto(String url) {
        return mPhotos.get(url);
    }

    /**
     * Загрузить фото по url, если его нет в {@link #mPhotos}.
     * Результат будет передан в listener, если он есть.
     */
    @SuppressWarnings("unused")
    public void fetchPhotos(final String[] urls, @Nullable final Listener listener) {
        mPhotoProcessingThread.loadPhotos(urls, listener);
    }

    /**
     * Удалить сохраненные на устройстве фото.
     */
    public void clearPhotosOnDevice() {
        mPhotoProcessingThread.clearPhotosOnDevice();
    }

    /**
     * Завершить поток загрузки фото.
     */
    public void quitLoadingThread() {
        mPhotoProcessingThread.quit();
    }

    /**
     * Класс-поток-загрузчик фото.
     */
    private class PhotoProcessingThread extends HandlerThread {

        private static final int MESSAGE_LOAD_PHOTOS = 1;
        private static final int MESSAGE_CLEAR_PHOTOS_ON_DEVICE = 2;

        /**
         * Название папки с сохраненными фото на устройстве.
         */
        private static final String PHOTOS_FOLDER = "photos";

        /**
         * Папка с сохраненными фото на устройстве.
         */
        private File mPhotoFolder;

        /**
         * Обработчик сообщений.
         */
        private Handler mProcessingHandler;

        /**
         * Для соотнесения загружаемых фото и слушателей их загрузки.
         */
        private Map<String[], Listener> mListenerMap = new HashMap<>();

        /**
         * Обработчик для результатов загрузки (и ошибок тоже).
         */
        private Handler mResponseHandler;

        @SuppressWarnings("ResultOfMethodCallIgnored")
        public PhotoProcessingThread(Handler responseHandler, Context context) {
            super("PhotoProcessingThread");
            mResponseHandler = responseHandler;

            mPhotoFolder = new File(context.getApplicationContext().getFilesDir(), PHOTOS_FOLDER);
            mPhotoFolder.mkdirs();
        }

        @SuppressWarnings("HandlerLeak")
        @Override
        protected void onLooperPrepared() {
            mProcessingHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MESSAGE_LOAD_PHOTOS:
                            handleLoadingPhotos((String[]) msg.obj);
                            break;
                        case MESSAGE_CLEAR_PHOTOS_ON_DEVICE:
                            handleClearPhotosOnDevice();
                            break;
                    }
                }
            };
        }

        /**
         * Загрузить фото по url.
         * Результат будет передан в listener. (если он есть).
         */
        public void loadPhotos(String[] urls, @Nullable Listener listener) {
            while (mProcessingHandler == null) {
                Thread.yield();
            }
            if (listener != null) {
                mListenerMap.put(urls, listener);
            }
            mProcessingHandler.obtainMessage(MESSAGE_LOAD_PHOTOS, urls).sendToTarget();
        }

        /**
         * Удалить сохраненные на устройстве фото.
         */
        public void clearPhotosOnDevice() {
            while (mProcessingHandler == null) {
                Thread.yield();
            }
            mProcessingHandler.obtainMessage(MESSAGE_CLEAR_PHOTOS_ON_DEVICE).sendToTarget();
        }

        /**
         * Выполнить загрузку и передать результат. (Если есть куда передавать).
         */
        private void handleLoadingPhotos(final String[] urls) {
            for (String url : urls) {
                getBitmap(url);
            }
            final Listener listener = mListenerMap.get(urls);
            if (listener != null) {
                mListenerMap.remove(urls);
                mResponseHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onCompleted();
                    }
                });
            }
        }

        /**
         * Поочередно искать фото:
         * - в {@link #mPhotos};
         * - в памяти устройства;
         * - в интернете.
         * Будет возвращено как только будет найдено.
         * Также оно будет сохранено в {@link #mPhotos} и в памяти телефона, если его там не было.
         */
        private Bitmap getBitmap(String url) {
            Bitmap result = mPhotos.get(url);
            if (result != null) {
                return result;
            }

            result = loadBitmapFromDevice(url);
            if (result != null) {
                mPhotos.put(url, result);
                return result;
            }

            try {
                result = downloadBitmap(url);
            }
            catch (IOException e) {
                Log.e(TAG, e.toString(), e);
                return null;
            }
            if (result != null) {
                mPhotos.put(url, result);
                saveBitmapToDevice(url, result);
            }
            return result;
        }

        /**
         * Сохранить фото в память устройства.
         */
        private void saveBitmapToDevice(String url, Bitmap bitmap) {
            File f = new File(mPhotoFolder, String.valueOf(url.hashCode()) + ".jpg");
            try {
                FileOutputStream outputStream = new FileOutputStream(f);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                outputStream.close();
            }
            catch (IOException e) {
                Log.e(TAG, e.toString(), e);
            }
        }

        /**
         * Загрузить фото из памяти устройства (если оно там было).
         */
        @Nullable
        private Bitmap loadBitmapFromDevice(String url) {
            File f = new File(mPhotoFolder, String.valueOf(url.hashCode()) + ".jpg");
            return BitmapFactory.decodeFile(f.getAbsolutePath());
        }

        /**
         * Загрузить изображение по переданному адресу из интернета.
         */
        private Bitmap downloadBitmap(String urlString) throws IOException {
            URL url = new URL(urlString);
            InputStream inputStream = url.openStream();
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            return bitmap;
        }

        /**
         * Выполнить удаление сохраненных на устройстве фото.
         */
        @SuppressWarnings("ResultOfMethodCallIgnored")
        private void handleClearPhotosOnDevice() {
            for (File f : mPhotoFolder.listFiles()) {
                f.delete();
            }
        }
    }

}