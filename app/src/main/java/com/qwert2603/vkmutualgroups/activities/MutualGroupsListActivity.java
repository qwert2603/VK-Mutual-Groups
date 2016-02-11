package com.qwert2603.vkmutualgroups.activities;

import android.os.Bundle;
import android.view.View;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.GroupsListFragment;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiUserFull;

/**
 * Группы, общие с другом.
 */
public class MutualGroupsListActivity extends GroupsListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        VKApiUserFull friend = getIntent().getParcelableExtra(EXTRA_FRIEND);
        DataManager mDataManager = DataManager.get(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.friend_name, friend.first_name, friend.last_name));
        }

        getErrorTextView().setVisibility(View.INVISIBLE);
        getRefreshLayout().setEnabled(false);

        VKApiCommunityArray groups;
        if (friend.id != 0) {
            groups = mDataManager.getGroupsMutualWithFriend(friend.id);
        } else {
            groups = mDataManager.getUsersGroups();
        }

        if (groups == null) {
            groups = new VKApiCommunityArray();
        }
        setListFragment(GroupsListFragment.newInstance(groups, getString(R.string.no_mutual_groups)));
    }

}
