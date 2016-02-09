package com.qwert2603.vkmutualgroups.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.AbstractVkListFragment;
import com.qwert2603.vkmutualgroups.fragments.FriendsListFragment;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKUsersArray;

public class FriendsListActivity extends AbstractVkListActivity {

    public static final String EXTRA_GROUP_ID = "com.alex.vkcommonpublics.EXTRA_GROUP_ID";

    private int mGroupId;

    private DataManager mDataManager;

    @Override
    protected String getActionBarTitle() {
        VKApiCommunityFull group = DataManager.get(this).getGroupById(mGroupId);
        return (group == null) ? getString(R.string.app_name) : group.name;
    }

    @Override
    protected AbstractVkListFragment createListFragment() {
        VKUsersArray friends;
        if (mGroupId != 0) {
            VKApiCommunityFull group = mDataManager.getGroupById(mGroupId);
            friends = mDataManager.getFriendsInGroup(group);
        }
        else {
            friends = mDataManager.getUsersFriends();
        }

        if (friends == null) {
            friends = new VKUsersArray();
        }
        return FriendsListFragment.newInstance(mGroupId, friends);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // mFriendId и mDataManager будет нужен в super.onCreate, поэтому его надо получить сейчас.
        mGroupId = getIntent().getIntExtra(EXTRA_GROUP_ID, 0);
        mDataManager = DataManager.get(this);

        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.friends_list_activity, menu);

        // если это список друзей пользователя
        // или группа, друзья в которой отображаются, была покинута, то выйти из нее нельзя.
        if (mGroupId == 0 || DataManager.get(this).getGroupById(mGroupId) == null) {
            menu.findItem(R.id.menu_leave_group).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_leave_group:
                leaveGroup(mGroupId);
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
