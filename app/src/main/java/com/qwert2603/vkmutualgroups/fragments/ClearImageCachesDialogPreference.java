package com.qwert2603.vkmutualgroups.fragments;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;

public class ClearImageCachesDialogPreference extends DialogPreference {

    public ClearImageCachesDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setNegativeButtonText(R.string.cancel);
        setPositiveButtonText(R.string.ok);
        setDialogMessage(context.getString(R.string.clear_images_cache_on_device) + "?");
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            PhotoManager.get(getContext()).clearPhotosOnDevice();
        }
    }
}
