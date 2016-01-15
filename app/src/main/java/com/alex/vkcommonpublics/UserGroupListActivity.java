package com.alex.vkcommonpublics;

import android.app.Fragment;
import android.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;

public class UserGroupListActivity extends AbstractVkListActivity {

    private DataManager mDataManager = DataManager.get();

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.my_groups);
    }

    @Override
    protected Fragment getListFragment() {
        return GroupsListFragment.newInstance(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_groups_list_activity, menu);
        MenuItem sortMenuItem = menu.findItem(R.id.menu_sort);
        switch (mDataManager.getGroupsSortState()) {
            case notSorted:
                sortMenuItem.setTitle(R.string.sort);
                sortMenuItem.setEnabled(false);
            case byDefault:
                sortMenuItem.setTitle(R.string.sort_by_default);
                break;
            case byFriends:
                sortMenuItem.setTitle(R.string.sort_by_friends);
                break;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort:
                switch (mDataManager.getGroupsSortState()) {
                    case byDefault:
                        mDataManager.sortGroupsByFriends();
                        break;
                    case byFriends:
                        mDataManager.sortGroupsByDefault();
                        break;
                }
                refreshGroupsListFragment();
                invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshGroupsListFragment() {
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction().replace(R.id.fragment_container, getListFragment()).commitAllowingStateLoss();
    }
}
