package com.qwert2603.vkmutualgroups.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.fragments.AbstractVkListFragment;

/**
 * Activity, отображающая фрагмент-список (друзей или групп).
 */
public abstract class AbstractVkListActivity extends NavigableActivity implements AbstractVkListFragment.Callbacks {

    protected abstract String getActionBarTitle();
    protected abstract Fragment getListFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getActionBarTitle());
        }
        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment == null) {
            fragment = getListFragment();
            fm.beginTransaction().add(R.id.fragment_container, fragment).commitAllowingStateLoss();
        }
    }

    @NonNull
    @Override
    public CoordinatorLayout getCoordinatorLayout() {
        return (CoordinatorLayout) findViewById(R.id.coordinator_layout);
    }
}
