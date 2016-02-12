package com.qwert2603.vkmutualgroups.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.vk.sdk.api.model.VKApiUserFull;

/**
 * Activity для отображения списка групп.
 */
public abstract class GroupsListActivity extends AbstractVkListActivity {

    public static final String EXTRA_FRIEND = "com.alex.vkcommonpublics.EXTRA_FRIEND";

    private VKApiUserFull mFriend;

    private DataManager mDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFriend = getIntent().getParcelableExtra(EXTRA_FRIEND);
        mDataManager = DataManager.get(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.friend_name, mFriend.first_name, mFriend.last_name));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.one_friend, menu);

        menu.findItem(R.id.menu_message).setVisible(false);

        if (mFriend.id == 0) {
            menu.findItem(R.id.menu_join_group).setVisible(false);
            menu.findItem(R.id.menu_leave_group).setVisible(false);
        }

        if (mDataManager.getUsersFriendById(mFriend.id) != null) {
            menu.findItem(R.id.menu_add_friend).setVisible(false);
        } else {
            menu.findItem(R.id.menu_delete_friend).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_view_groups:
                Intent intent = new Intent(this, FriendGroupsListActivity.class);
                intent.putExtra(FriendGroupsListActivity.EXTRA_FRIEND, mFriend);
                startActivity(intent);
                return true;
            case R.id.menu_delete_friend:
                deleteFriend(mFriend);
                return true;
            case R.id.menu_add_friend:
                addFriend(mFriend);
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
