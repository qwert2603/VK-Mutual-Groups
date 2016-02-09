package com.qwert2603.vkmutualgroups.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.fragments.AbstractVkListFragment;

/**
 * Activity, отображающая фрагмент-список (друзей или групп).
 */
public abstract class AbstractVkListActivity extends NavigableActivity {

    private AbstractVkListFragment mFragment;

    protected abstract String getActionBarTitle();

    @Nullable
    protected abstract AbstractVkListFragment createListFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getActionBarTitle());
        }
        updateListFragment();
    }

    /**
     * Обновить фрагмент.
     */
    protected void updateListFragment() {
        FragmentManager fm = getFragmentManager();
        mFragment = createListFragment();
        if (mFragment != null) {
            fm.beginTransaction().replace(R.id.fragment_container, mFragment).commitAllowingStateLoss();
        } else {
            Fragment fragmentBefore = fm.findFragmentById(R.id.fragment_container);
            if (fragmentBefore != null) {
                fm.beginTransaction().remove(fragmentBefore).commitAllowingStateLoss();
            }
        }
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
