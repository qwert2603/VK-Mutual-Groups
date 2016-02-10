package com.qwert2603.vkmutualgroups.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.Menu;
import android.view.MenuItem;

import com.qwert2603.vkmutualgroups.Listener;
import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.AbstractVkListFragment;
import com.qwert2603.vkmutualgroups.fragments.GroupsListFragment;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiUserFull;

/**
 * Все группы друга.
 */
public class FriendGroupsListActivity extends AbstractVkListActivity {

    public static final String EXTRA_FRIEND_ID = "com.alex.vkcommonpublics.EXTRA_FRIEND_ID";

    private int mFriendId;
    VKApiCommunityArray mGroups = null;

    private DataManager mDataManager;

    private boolean mIsDestroyed = false;

    @Override
    protected String getActionBarTitle() {
        VKApiUserFull friend = DataManager.get(this).getUsersFriendById(mFriendId);
        return (friend == null) ? getString(R.string.app_name) : getString(R.string.friend_name, friend.first_name, friend.last_name);
    }

    @Nullable
    @Override
    protected AbstractVkListFragment createListFragment() {
        // Если надо отобразить группы друга, и они были загружены раннее, возвращаем фрагмент.
        if (mGroups != null) {
            return GroupsListFragment.newInstance(mFriendId, mGroups);
        }

        // Если дошли до сюда, то надо отобразить группы друга, но они еще не были загружены.
        // Загружаем их, а пока что возвращаем null.
        // После окончания загрузки обновляем фрагмент,
        // из-за чего этот метод вызывается заново и уже возвращает готовый фрагмент.
        mDataManager.fetchUsersGroups(mFriendId, new Listener<VKApiCommunityArray>() {
            @Override
            public void onCompleted(VKApiCommunityArray vkApiCommunityFulls) {
                if (! mIsDestroyed) {
                    mGroups = vkApiCommunityFulls;
                    updateListFragment();
                }
            }

            @Override
            public void onError(String e) {
                Snackbar.make(getCoordinatorLayout(), R.string.loading_failed, Snackbar.LENGTH_SHORT)
                        .setAction(R.string.refresh, (v) -> updateListFragment())
                        .show();
            }
        });

        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // mFriendId и mDataManager будет нужен в super.onCreate, поэтому его надо получить сейчас.
        mFriendId = getIntent().getIntExtra(EXTRA_FRIEND_ID, 0);
        mDataManager = DataManager.get(this);

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        mIsDestroyed = true;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.groups_list_activityt, menu);

        // если это список групп пользователя
        // или пользователь, общие группы с которым отображаются, был удален, то удалить его нельзя.
        if (mFriendId == 0 || DataManager.get(this).getUsersFriendById(mFriendId) == null) {
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
