package com.alex.vkmutualgroups;

import android.widget.ImageView;

/**
 * Интерфейс для ViewHolder для convertView в ArrayAdapter, который содержит ImageView.
 */
public interface ImageViewHolder {
    int getPosition();
    ImageView getImageView();
}