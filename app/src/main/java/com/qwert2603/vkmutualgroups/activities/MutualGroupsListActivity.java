package com.qwert2603.vkmutualgroups.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.GroupsListFragment;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiUserFull;

/**
 * Группы, общте с другом.
 */
public class MutualGroupsListActivity extends AbstractVkListActivity {

    public static final String EXTRA_FRIEND = "com.alex.vkcommonpublics.EXTRA_FRIEND";

    private VKApiUserFull mFriend;

    private DataManager mDataManager;

    private FloatingActionButton mActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFriend = getIntent().getParcelableExtra(EXTRA_FRIEND);
        mDataManager = DataManager.get(this);

        if (getSupportActionBar() != null) {
            VKApiUserFull friend = mDataManager.getUsersFriendById(mFriend.id);
            String title = (friend == null)
                    ? getString(R.string.app_name)
                    : getString(R.string.friend_name, friend.first_name, friend.last_name);
            getSupportActionBar().setTitle(title);
        }

        getErrorTextView().setVisibility(View.INVISIBLE);
        getRefreshLayout().setEnabled(false);

        mActionButton = getActionButton();
        mActionButton.setIcon(R.drawable.message);
        mActionButton.setOnClickListener((v) -> sendMessage(mFriend.id));

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.groups_list_activity, menu);

        // если это список групп пользователя
        // или пользователь, общие группы с которым отображаются, был удален, то удалить его нельзя.
        if (mFriend.id == 0 || mDataManager.getUsersFriendById(mFriend.id) == null) {
            menu.findItem(R.id.menu_delete_friend).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_delete_friend:
                deleteFriend(mFriend);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void notifyOperationCompleted() {
        super.notifyOperationCompleted();
        invalidateOptionsMenu();
        if (mDataManager.getUsersFriendById(mFriend.id) == null) {
            mActionButton.setVisibility(View.INVISIBLE);
        }
    }

}