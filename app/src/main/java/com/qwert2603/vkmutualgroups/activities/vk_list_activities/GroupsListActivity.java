package com.qwert2603.vkmutualgroups.activities.vk_list_activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.vk.sdk.api.model.VKApiUserFull;

import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.finished;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.loading;

/**
 * Activity для отображения списка групп.
 */
public abstract class GroupsListActivity extends AbstractVkListActivity {

    public static final String EXTRA_FRIEND = "com.alex.vkcommonpublics.EXTRA_FRIEND";

    private VKApiUserFull mFriend;

    private DataManager mDataManager;

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.friend_name, mFriend.first_name, mFriend.last_name);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFriend = getIntent().getParcelableExtra(EXTRA_FRIEND);
        mDataManager = DataManager.get(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.one_friend, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_message).setVisible(false);

        if (mFriend.id == 0) {
            menu.findItem(R.id.menu_join_group).setVisible(false);
            menu.findItem(R.id.menu_leave_group).setVisible(false);
        }

        if (mDataManager.getUsersFriendById(mFriend.id) != null) {
            menu.findItem(R.id.menu_delete_friend).setVisible(true);
            menu.findItem(R.id.menu_add_friend).setVisible(false);

        } else {
            menu.findItem(R.id.menu_delete_friend).setVisible(false);
            menu.findItem(R.id.menu_add_friend).setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        DataManager.FetchingState fetchingState = mDataManager.getFetchingState();
        switch (item.getItemId()) {
            case R.id.menu_view_groups:
                if (fetchingState == finished) {
                    Intent intent = new Intent(this, FriendGroupsListActivity.class);
                    intent.putExtra(EXTRA_FRIEND, mFriend);
                    startActivity(intent);
                } else {
                    showSnackbar(fetchingState == loading ? R.string.loading_is_on : R.string.data_was_not_loaded_else);
                }
                return true;
            case R.id.menu_open_in_browser:
                navigateTo("http://vk.com/" + mFriend.screen_name);
                return true;
            case R.id.menu_delete_friend:
                if (fetchingState == finished) {
                    deleteFriend(mFriend);
                } else {
                    showSnackbar(fetchingState == loading ? R.string.loading_is_on : R.string.data_was_not_loaded_else);
                }
                return true;
            case R.id.menu_add_friend:
                if (fetchingState == finished) {
                    addFriend(mFriend);
                } else {
                    showSnackbar(fetchingState == loading ? R.string.loading_is_on : R.string.data_was_not_loaded_else);
                }
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
