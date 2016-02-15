package com.qwert2603.vkmutualgroups.activities;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.fragments.SettingsFragment;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment == null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, new SettingsFragment())
                    .commit();
        }
    }

}
