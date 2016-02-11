package com.qwert2603.vkmutualgroups.activities;

import android.os.Bundle;
import android.view.View;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.GroupsListFragment;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiUserFull;

/**
 * Группы, общте с другом.
 */
public class MutualGroupsListActivity extends GroupsListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        VKApiUserFull mFriend = getIntent().getParcelableExtra(EXTRA_FRIEND);
        DataManager mDataManager = DataManager.get(this);

        if (getSupportActionBar() != null) {
            VKApiUserFull friend = mDataManager.getUsersFriendById(mFriend.id);
            String title = (friend == null)
                    ? getString(R.string.app_name)
                    : getString(R.string.friend_name, friend.first_name, friend.last_name);
            getSupportActionBar().setTitle(title);
        }

        getErrorTextView().setVisibility(View.INVISIBLE);
        getRefreshLayout().setEnabled(false);

        VKApiCommunityArray group = null;
        if (mFriend.id != 0) {
            VKApiUserFull friend = mDataManager.getUsersFriendById(mFriend.id);
            if (friend != null) {
                group = mDataManager.getGroupsMutualWithFriend(friend.id);
            }
        } else {
            group = mDataManager.getUsersGroups();
        }

        if (group == null) {
            group = new VKApiCommunityArray();
        }
        setListFragment(GroupsListFragment.newInstance(group));
    }

}