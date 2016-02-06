package com.qwert2603.vkmutualgroups.photo;

import android.widget.ImageView;

/**
 * Интерфейс для ViewHolder для convertView в ArrayAdapter, который содержит ImageView.
 * Используется в {@link PhotoManager#setPhotoToImageViewHolder(ImageViewHolder, String)}.
 */
public interface ImageViewHolder {
    int getPosition();
    ImageView getImageView();
}