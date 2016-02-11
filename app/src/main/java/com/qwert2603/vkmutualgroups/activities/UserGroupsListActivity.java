package com.qwert2603.vkmutualgroups.activities;

import android.os.Bundle;
import android.view.View;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.GroupsListFragment;

/**
 * Activity, содержащая список групп пользователя.
 */
public class UserGroupsListActivity extends AbstractVkListActivity {

    private DataManager mDataManager;

    private FloatingActionButton mActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.my_groups));
        }
        mDataManager = DataManager.get(this);

        getErrorTextView().setVisibility(View.INVISIBLE);
        getRefreshLayout().setEnabled(false);

        mActionButton = getActionButton();
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
        setListFragment(GroupsListFragment.newInstance(mDataManager.getUsersGroups(), getString(R.string.no_groups)));
    }

}
