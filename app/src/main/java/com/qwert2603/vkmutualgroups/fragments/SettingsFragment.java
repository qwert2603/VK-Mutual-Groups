package com.qwert2603.vkmutualgroups.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;
import com.qwert2603.vkmutualgroups.util.VkLogOutUtil;

public class SettingsFragment extends PreferenceFragment {

    @SuppressWarnings("unused")
    public static final String TAG = "SettingsFragment";

    public static final String PREF_IS_CACHE_IMAGES_ON_DEVICE = "is_cache_images_on_device";

    private static final int REQUEST_CLEAR_IMAGE_CACHE = 1;
    private static final int REQUEST_LOG_OUT = 2;

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

        findPreference("clear_images_cache_on_device").setOnPreferenceClickListener(preference -> {
            String title = getString(R.string.clear_cache);
            String question = getString(R.string.clear_images_cache_on_device) + "?";
            ConfirmationDialogFragment confirmationDialogFragment = ConfirmationDialogFragment
                    .newInstance(title, question);
            confirmationDialogFragment.setTargetFragment(this, REQUEST_CLEAR_IMAGE_CACHE);
            confirmationDialogFragment.show(getFragmentManager(), ConfirmationDialogFragment.TAG);
            return true;
        });

        findPreference("log_out").setOnPreferenceClickListener(preference -> {
            String title = getString(R.string.exit);
            String question = getString(R.string.log_out) + "?";
            ConfirmationDialogFragment confirmationDialogFragment = ConfirmationDialogFragment
                    .newInstance(title, question);
            confirmationDialogFragment.setTargetFragment(this, REQUEST_LOG_OUT);
            confirmationDialogFragment.show(getFragmentManager(), ConfirmationDialogFragment.TAG);
            return true;
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CLEAR_IMAGE_CACHE:
                    PhotoManager.get(getActivity()).clearPhotosOnDevice();
                    Toast.makeText(getActivity(), R.string.image_cache_cleared, Toast.LENGTH_SHORT).show();
                    break;
                case REQUEST_LOG_OUT:
                    VkLogOutUtil.logOut(getActivity());
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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
