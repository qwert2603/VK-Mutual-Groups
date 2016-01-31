package com.alex.vkmutualgroups.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.alex.vkmutualgroups.fragments.GroupsListFragment;
import com.alex.vkmutualgroups.R;
import com.alex.vkmutualgroups.data.DataManager;

import static com.alex.vkmutualgroups.data.DataManager.FetchingState.finished;
import static com.alex.vkmutualgroups.data.DataManager.GroupsSortState.byFriends;

public class UserGroupListActivity extends AbstractVkListActivity {

    private DataManager mDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataManager = DataManager.get(this);
    }

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
        sortMenuItem.setChecked(mDataManager.getGroupsSortState() == byFriends);
        sortMenuItem.setEnabled(mDataManager.getFetchingState() == finished);
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
