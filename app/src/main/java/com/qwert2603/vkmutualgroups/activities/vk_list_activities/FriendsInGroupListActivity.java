package com.qwert2603.vkmutualgroups.activities.vk_list_activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.FriendsListFragment;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKUsersArray;

/**
 * Друзья в группе.
 */
public class FriendsInGroupListActivity extends AbstractVkListActivity {

    public static final String EXTRA_GROUP = "com.alex.vkcommonpublics.EXTRA_GROUP";

    private VKApiCommunityFull mGroup;

    private DataManager mDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGroup = getIntent().getParcelableExtra(EXTRA_GROUP);
        mDataManager = DataManager.get(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(mGroup.name);
        }

        setErrorTextViewVisibility(View.INVISIBLE);
        setRefreshLayoutEnable(false);
        setActionButtonVisibility(View.INVISIBLE);

        VKUsersArray friends;
        if (mGroup.id != 0) {
            friends = mDataManager.getFriendsInGroup(mGroup.id);
        } else {
            friends = mDataManager.getUsersFriends();
        }

        if (friends == null) {
            friends = new VKUsersArray();
        }
        setListFragment(FriendsListFragment.newInstance(friends, getString(R.string.no_friends_in_group)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.one_group, menu);

        if (mGroup.id == 0) {
            menu.findItem(R.id.menu_join_group).setVisible(false);
            menu.findItem(R.id.menu_leave_group).setVisible(false);
        }

        if (mDataManager.getUsersGroupById(mGroup.id) != null) {
            menu.findItem(R.id.menu_join_group).setVisible(false);
        } else {
            menu.findItem(R.id.menu_leave_group).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_open_in_browser:
                navigateTo("http://vk.com/" + mGroup.screen_name);
                return true;
            case R.id.menu_leave_group:
                leaveGroup(mGroup);
                return true;
            case R.id.menu_join_group:
                joinGroup(mGroup);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        invalidateOptionsMenu();
    }

}
