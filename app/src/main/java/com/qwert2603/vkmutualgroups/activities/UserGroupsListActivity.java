package com.qwert2603.vkmutualgroups.activities;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.AbstractVkListFragment;
import com.qwert2603.vkmutualgroups.fragments.GroupsListFragment;

/**
 * Activity, содержащая список групп пользователя.
 */
public class UserGroupsListActivity extends NavigableActivity {

    private DataManager mDataManager;

    private FloatingActionButton mActionButton;

    private AbstractVkListFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_groups_list);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.my_groups));
        }
        mDataManager = DataManager.get(this);

        mActionButton = (FloatingActionButton) findViewById(R.id.action_button);
        switch (mDataManager.getGroupsSortState()) {
            case byDefault:
                mActionButton.setIcon(android.R.drawable.ic_menu_sort_alphabetically);
                break;
            case byFriends:
                mActionButton.setIcon(android.R.drawable.ic_menu_sort_by_size);
                break;
        }
        mActionButton.setOnClickListener((v) -> {
            switch (mDataManager.getGroupsSortState()) {
                case byDefault:
                    mDataManager.sortGroupsByFriends();
                    mActionButton.setIcon(android.R.drawable.ic_menu_sort_by_size);
                    break;
                case byFriends:
                    mDataManager.sortGroupsByDefault();
                    mActionButton.setIcon(android.R.drawable.ic_menu_sort_alphabetically);
                    break;
            }
            refreshGroupsListFragment();
        });

        refreshGroupsListFragment();
    }

    private void refreshGroupsListFragment() {
        FragmentManager fm = getFragmentManager();
        mFragment =  GroupsListFragment.newInstance(0, mDataManager.getUsersGroups());
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
