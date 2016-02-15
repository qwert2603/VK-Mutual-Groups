package com.qwert2603.vkmutualgroups.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;

public class SettingsFragment extends PreferenceFragment {

    @SuppressWarnings("unused")
    public static final String TAG = "SettingsFragment";

    public static final String PREF_IS_CACHE_IMAGES_ON_DEVICE = "is_cache_images_on_device";

    private SharedPreferences mSharedPreferences;

    private PhotoManager mPhotoManager;

    private SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
        switch (key) {
            case PREF_IS_CACHE_IMAGES_ON_DEVICE:
                boolean isCache = sharedPreferences.getBoolean(PREF_IS_CACHE_IMAGES_ON_DEVICE, false);
                mPhotoManager.setIsCacheImagesOnDevice(isCache);
                break;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPhotoManager = PhotoManager.get(getActivity());
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
