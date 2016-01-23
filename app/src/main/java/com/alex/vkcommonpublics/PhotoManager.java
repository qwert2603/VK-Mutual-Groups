package com.alex.vkcommonpublics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.LruCache;

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
    }

    public static PhotoManager get(Context context) {
        if (sPhotoManager == null) {
            sPhotoManager = new PhotoManager(context);
        }
        return sPhotoManager;
    }

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
     * Получить фото по указанному url, если оно было загружено ранее.
     */
    @Nullable
    public Bitmap getPhoto(String url) {
        return mPhotos.get(url);
    }

    /**
     * Загрузить фото по url, если его нет в {@link #mPhotos}.
     */
    @SuppressWarnings("unused")
    public void fetchPhoto(final String url, @Nullable final Listener listener) {
        if (mPhotos.get(url) != null) {
            if (listener != null) {
                listener.onCompleted();
            }
            return;
        }
        new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... params) {
                return getBitmap(params[0]);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    mPhotos.put(url, bitmap);
                    if (listener != null) {
                        listener.onCompleted();
                    }
                }
                else {
                    if (listener != null) {
                        listener.onError("Fetching photo failed!");
                    }
                }
            }
        }.execute(url);
    }

    /**
     * Установить фото в {@link ImageViewHolder}.
     */
    public void setPhotoToImageViewHolder(ImageViewHolder imageViewHolder, String url) {
        new PhotoSettingTask(imageViewHolder).execute(url);
    }

    /**
     * Класс для установки фото в {@link ImageViewHolder}.
     */
    private class PhotoSettingTask extends AsyncTask<String, Void, Bitmap> {
        private ImageViewHolder mImageViewHolder;
        private String mUrl;
        private int mPosition;

        public PhotoSettingTask(ImageViewHolder imageViewHolder) {
            mImageViewHolder = imageViewHolder;
            mPosition = mImageViewHolder.getPosition();
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            if (mImageViewHolder.getPosition() == mPosition) {
                mUrl = params[0];
                return getBitmap(mUrl);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                mPhotos.put(mUrl, bitmap);
                if (mImageViewHolder.getPosition() == mPosition) {
                    mImageViewHolder.getImageView().setImageBitmap(bitmap);
                }
            }
        }
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
        }.execute();
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

        try {
            result = downloadBitmap(url);
        } catch (IOException e) {
            Log.e(TAG, e.toString(), e);
            return null;
        }
        if (result != null) {
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
        } catch (IOException e) {
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

}