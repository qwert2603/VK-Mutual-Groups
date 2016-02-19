package com.qwert2603.vkmutualgroups.activities.vk_list_activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.GroupsListFragment;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiCommunityFull;

/**
 * Activity, содержащая список групп пользователя.
 */
public class UserGroupsListActivity extends AbstractVkListActivity {

    private DataManager mDataManager;

    private String mQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.my_groups));
        }
        mDataManager = DataManager.get(this);

        setErrorTextViewVisibility(View.INVISIBLE);
        setRefreshLayoutEnable(false);

        switch (mDataManager.getGroupsSortState()) {
            case byDefault:
                setActionButtonIcon(android.R.drawable.ic_menu_sort_alphabetically);
                break;
            case byFriends:
                setActionButtonIcon(android.R.drawable.ic_menu_sort_by_size);
                break;
        }
        setActionButtonOnClickListener((v) -> {
            switch (mDataManager.getGroupsSortState()) {
                case byDefault:
                    mDataManager.sortGroupsByFriends();
                    setActionButtonIcon(android.R.drawable.ic_menu_sort_by_size);
                    break;
                case byFriends:
                    mDataManager.sortGroupsByDefault();
                    setActionButtonIcon(android.R.drawable.ic_menu_sort_alphabetically);
                    break;
            }
            refreshGroupsListFragment();
        });

        refreshGroupsListFragment();
    }

    private void refreshGroupsListFragment() {
        VKApiCommunityArray groups = mDataManager.getUsersGroups();
        if (groups == null) {
            return;
        }
        VKApiCommunityArray showingGroups;
        if (mQuery == null || mQuery.equals("")) {
            showingGroups = groups;
        } else {
            showingGroups = new VKApiCommunityArray();

            // поиск не зависит от регистра.
            mQuery = mQuery.toLowerCase();
            for (VKApiCommunityFull group : groups) {
                if (group.name.toLowerCase().contains(mQuery)) {
                    showingGroups.add(group);
                }
            }
        }
        setListFragment(GroupsListFragment.newInstance(showingGroups, getString(R.string.no_groups)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.user_groups_list_activity, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setQuery(mQuery, false);
        searchView.setSubmitButtonEnabled(false);
        searchView.setQueryHint(getString(R.string.search_groups));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mQuery = newText;
                refreshGroupsListFragment();
                return true;
            }
        });

        return true;
    }
}
