package com.qwert2603.vkmutualgroups.activities.vk_list_activities;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.Menu;
import android.view.View;
import android.widget.AbsListView;

import com.qwert2603.vkmutualgroups.Listener;
import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.AbstractVkListFragment;
import com.qwert2603.vkmutualgroups.fragments.GroupsListFragment;
import com.qwert2603.vkmutualgroups.util.InternetUtils;
import com.qwert2603.vkmutualgroups.util.VKApiCommunityArray_Fix;
import com.vk.sdk.api.model.VKApiUserFull;

import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.loading;

/**
 * Все группы друга.
 */
public class FriendGroupsListActivity extends GroupsListActivity implements AbstractVkListFragment.Callbacks {

    private VKApiUserFull mFriend;
    private VKApiCommunityArray_Fix mGroups;

    private DataManager mDataManager;

    private boolean mIsLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFriend = getIntent().getParcelableExtra(EXTRA_FRIEND);
        mDataManager = DataManager.get(this);

        setErrorTextViewText(getString(R.string.loading_failed));
        setErrorTextViewVisibility(View.INVISIBLE);

        setRefreshLayoutOnRefreshListener(this::fetchFriendGroups);

        setActionButtonVisibility(View.INVISIBLE);

        setListFragment(null);
        fetchFriendGroups();
    }

    private void fetchFriendGroups() {
        if (mDataManager.getFetchingState() == loading) {
            return;
        }
        if (InternetUtils.isInternetConnected(this)) {
            mIsLoading=true;
            setRefreshLayoutRefreshing(true);
            setErrorTextViewVisibility(View.INVISIBLE);
            mDataManager.fetchUsersGroups(mFriend.id, new Listener<VKApiCommunityArray_Fix>() {
                @Override
                public void onCompleted(VKApiCommunityArray_Fix vkApiCommunityFulls) {
                    mIsLoading=false;
                    mGroups = vkApiCommunityFulls;
                    setErrorTextViewVisibility(View.INVISIBLE);
                    setListFragment(GroupsListFragment.newInstance(mGroups, getString(R.string.groups_of_friend_empty_list)));
                    setRefreshLayoutRefreshing(false);
                    setRefreshLayoutEnable(true);
                    showSnackbar(R.string.groups_list_loaded);
                }

                @Override
                public void onError(String e) {
                    mIsLoading=false;
                    if (!(getListFragment() instanceof AbstractVkListFragment)) {
                        setErrorTextViewVisibility(View.VISIBLE);
                    }
                    setRefreshLayoutRefreshing(false);
                    setRefreshLayoutEnable(true);
                    showSnackbar(R.string.loading_failed, Snackbar.LENGTH_SHORT, R.string.refresh, (v) -> fetchFriendGroups());
                }
            });
        } else {
            showSnackbar(R.string.no_internet_connection, Snackbar.LENGTH_SHORT, R.string.refresh, (v) -> fetchFriendGroups());
            setRefreshLayoutRefreshing(false);
            if (!(getListFragment() instanceof AbstractVkListFragment)) {
                setErrorTextViewVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_view_groups).setVisible(false);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onListViewScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        boolean b =  mIsLoading || mGroups == null || mGroups.isEmpty()
                || (firstVisibleItem == 0) && (view.getChildAt(0) != null) && (view.getChildAt(0).getTop() == 0);

        setRefreshLayoutEnable(b);
    }

}
