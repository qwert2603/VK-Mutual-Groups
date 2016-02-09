package com.qwert2603.vkmutualgroups.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.AbstractVkListFragment;
import com.qwert2603.vkmutualgroups.fragments.GroupsListFragment;
import com.vk.sdk.api.model.VKApiUserFull;

public class GroupsListActivity extends AbstractVkListActivity {

    public static final String EXTRA_FRIEND_ID = "com.alex.vkcommonpublics.EXTRA_FRIEND_ID";

    private int mFriendId;

    @Override
    protected String getActionBarTitle() {
        VKApiUserFull friend = DataManager.get(this).getFriendById(mFriendId);
        return (friend == null) ? getString(R.string.app_name) : getString(R.string.friend_name, friend.first_name, friend.last_name);
    }

    @Override
    protected AbstractVkListFragment createListFragment() {
        return GroupsListFragment.newInstance(mFriendId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mFriendId = getIntent().getIntExtra(EXTRA_FRIEND_ID, 0);
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.groups_list_activityt, menu);

        // если это список групп пользователя
        // или пользователь, общие группы с которым отображаются, был удален, то удалить его нельзя.
        if (mFriendId == 0 || DataManager.get(this).getFriendById(mFriendId) == null) {
            menu.findItem(R.id.menu_delete_friend).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_delete_friend:
                deleteFriend(mFriendId);
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