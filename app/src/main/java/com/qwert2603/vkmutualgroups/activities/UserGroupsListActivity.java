package com.qwert2603.vkmutualgroups.activities;

import android.app.FragmentManager;
import android.os.Bundle;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.GroupsListFragment;
import com.getbase.floatingactionbutton.FloatingActionButton;

/**
 * Activity, содержащая список групп пользователя.
 */
public class UserGroupsListActivity extends NavigableActivity {

    private DataManager mDataManager;

    private FloatingActionButton mActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_groups_list);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.my_groups));
        }
        mDataManager = DataManager.get(this);

        mActionButton = (FloatingActionButton) findViewById(R.id.action_button);
        mActionButton.setIcon(android.R.drawable.ic_menu_sort_alphabetically);
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
        fm.beginTransaction().replace(R.id.fragment_container, GroupsListFragment.newInstance(0)).commitAllowingStateLoss();
    }
}