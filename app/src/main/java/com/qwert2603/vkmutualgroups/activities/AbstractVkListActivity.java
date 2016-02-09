package com.qwert2603.vkmutualgroups.activities;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.fragments.AbstractVkListFragment;

/**
 * Activity, отображающая фрагмент-список (друзей или групп).
 */
public abstract class AbstractVkListActivity extends NavigableActivity {

    private AbstractVkListFragment mFragment;

    protected abstract String getActionBarTitle();
    protected abstract AbstractVkListFragment createListFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getActionBarTitle());
        }
        FragmentManager fm = getFragmentManager();
        mFragment = createListFragment();
        fm.beginTransaction().replace(R.id.fragment_container, mFragment).commitAllowingStateLoss();
    }

    @NonNull
    @Override
    public CoordinatorLayout getCoordinatorLayout() {
        return (CoordinatorLayout) findViewById(R.id.coordinator_layout);
    }

    @Override
    protected void notifyDataSetChanged() {
        mFragment.notifyDataSetChanged();
    }
}
