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
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Загружает и хранит фотографии - аватарки друзей и групп.
 * Также позволяет отобразить в ImageView фото фото по нужному url.
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
    private BitmapHashMap mPhotos = new BitmapHashMap();

    private static class BitmapHashMap extends HashMap<String, Bitmap> {
        @Override
        public Bitmap put(String key, Bitmap value) {
            synchronized (BitmapHashMap.this) {
                // чтобы не хранить слишком много.
                if (size() > 2048) {
                    clear();
                }
            }
            return super.put(key, value);
        }
    }

    /**
     * Получить фото по указанному url.
     */
    @Nullable
    @SuppressWarnings("unused")
    public Bitmap getPhoto(String url) {
        return mPhotos.get(url);
    }

    /**
     * Загрузить фото по url, если его нет в {@link #mPhotos}.
     * Результат будет передан в listener, если он есть.
     */
    @SuppressWarnings("unused")
    public void fetchPhoto(final String url, @Nullable final Listener<Bitmap> listener) {
        if (mPhotos.get(url) != null) {
            if (listener != null) {
                listener.onCompleted(mPhotos.get(url));
            }
            return;
        }
        mPhotoProcessingThread.loadPhoto(url, listener);
    }

    /**
     * Отобразить в переданном imageView фото по переданному адресу.
     * Если позже метод будет заново вызван с тем же imageView, то будет отображнено фото для последнего вызова.
     */
    public void setPhotoToImageView(ImageView imageView, String url) {
        mPhotoProcessingThread.setPhotoToImageView(imageView, url);
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

        private static final int MESSAGE_LOAD_PHOTO = 1;
        private static final int MESSAGE_SET_PHOTO_TO_IMAGE_VIEW = 2;
        private static final int MESSAGE_CLEAR_PHOTOS_ON_DEVICE = 3;


        private static final String PHOTOS_FOLDER = "photos";

        /**
         * Папка с сохраненными фото на устройстве.
         */
        private File mPhotoFolder;

        /**
         * Обработчик сообщений о загрузке.
         */
        private Handler mProcessingHandler;

        /**
         * Для соотнесения фото и слушателя его загрузки.
         */
        private Map<String, Listener<Bitmap>> mListenerMap = new HashMap<>();

        /**
         * Для соотнесения ImageView и фото, которое надо в нем отобразить.
         * И чтобы в ImageView отобразилось последнее назначенное фото.
         */
        //private Map<ImageView, String> mImageViewMap = Collections.synchronizedMap(new HashMap<ImageView, String>());
        private Map<ImageView, String> mImageViewMap = new ImageViewMap();

        private class ImageViewMap extends HashMap<ImageView, String> {
            @Override
            public String put(ImageView key, String value) {
                synchronized (ImageViewMap.this) {
                    return super.put(key, value);
                }
            }

            @Override
            public String remove(Object key) {
                synchronized (ImageViewMap.this) {
                    return super.remove(key);
                }
            }
        }

        /**
         * Обработчик для результатов загрузки (и ошибок тоже)
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
                        case MESSAGE_LOAD_PHOTO:
                            handleLoadingPhoto((String) msg.obj);
                            break;
                        case MESSAGE_SET_PHOTO_TO_IMAGE_VIEW:
                            handleSetPhotoToImageView((ImageView) msg.obj);
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
        public void loadPhoto(String url, @Nullable Listener<Bitmap> listener) {
            while (mProcessingHandler == null) {
                Thread.yield();
            }
            if (listener != null) {
                mListenerMap.put(url, listener);
            }
            mProcessingHandler.obtainMessage(MESSAGE_LOAD_PHOTO, url).sendToTarget();
        }

        /**
         * Отобразить в переданном imageView фото по переданному адресу.
         */
        public void setPhotoToImageView(final ImageView imageView, final String url) {
            if (mPhotos.get(url) != null) {
                imageView.setImageBitmap(mPhotos.get(url));
                mImageViewMap.remove(imageView);
                return;
            }
            imageView.setImageBitmap(null);
            while (mProcessingHandler == null) {
                Thread.yield();
            }
            mImageViewMap.put(imageView, url);
            mProcessingHandler.obtainMessage(MESSAGE_SET_PHOTO_TO_IMAGE_VIEW, imageView).sendToTarget();
        }

        /**
         * Удалить сохраненные на устройстве фото.
         */
        public void clearPhotosOnDevice() {
            mProcessingHandler.obtainMessage(MESSAGE_CLEAR_PHOTOS_ON_DEVICE).sendToTarget();
        }

        /**
         * Выполнить загрузку и передать результат. (если есть куда передавать).
         */
        private void handleLoadingPhoto(final String urlString) {
            final Listener<Bitmap> listener = mListenerMap.get(urlString);
            final Bitmap bitmap = getBitmap(urlString);
            if (listener != null) {
                mResponseHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListenerMap.remove(urlString);
                        listener.onCompleted(bitmap);
                    }
                });
            }
        }

        /**
         * Выполнить названичение фото в ImageView.
         */
        private void handleSetPhotoToImageView(final ImageView imageView) {
            final String url = mImageViewMap.get(imageView);
            if (url == null) {
                return;
            }
            final Bitmap bitmap = getBitmap(url);
            if (bitmap != null) {
                mResponseHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (url == mImageViewMap.get(imageView)) {
                            mImageViewMap.remove(imageView);
                            imageView.setImageBitmap(bitmap);
                        }
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
         */
        private Bitmap getBitmap(String url) {
            Bitmap result = mPhotos.get(url);
            if (result != null) {
                return result;
            }

            result = loadBitmapFromDevice(url);
            if (result != null) {
                mPhotos.put(url, result);
                try {Thread.sleep(78);} catch (InterruptedException ignored) {}//todo: comment this
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
            Log.d(TAG, "saveBitmapToDevice");
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
        public void handleClearPhotosOnDevice() {
            for (File f : mPhotoFolder.listFiles()) {
                f.delete();
            }
        }
    }

}