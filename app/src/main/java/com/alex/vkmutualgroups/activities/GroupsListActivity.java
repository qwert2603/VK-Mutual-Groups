package com.alex.vkmutualgroups.activities;

import android.app.Fragment;
import android.os.Bundle;

import com.alex.vkmutualgroups.fragments.GroupsListFragment;
import com.alex.vkmutualgroups.R;
import com.alex.vkmutualgroups.data.DataManager;
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
    protected Fragment getListFragment() {
        return GroupsListFragment.newInstance(mFriendId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mFriendId = getIntent().getIntExtra(EXTRA_FRIEND_ID, 0);
        super.onCreate(savedInstanceState);
    }
}