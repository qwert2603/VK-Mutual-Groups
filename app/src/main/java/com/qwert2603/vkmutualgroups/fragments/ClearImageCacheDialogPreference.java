package com.qwert2603.vkmutualgroups.fragments;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;

public class ClearImageCacheDialogPreference extends DialogPreference {

    public ClearImageCacheDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogMessage(context.getString(R.string.clear_images_cache_on_device) + "?");
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            PhotoManager.get(getContext()).clearPhotosOnDevice();
            Toast.makeText(getContext(), R.string.image_cache_cleared, Toast.LENGTH_SHORT).show();
        }
    }

}
