package com.qwert2603.vkmutualgroups.activities.vk_list_activities;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.SearchView;

import com.qwert2603.vkmutualgroups.Listener;
import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.AbstractVkListFragment;
import com.qwert2603.vkmutualgroups.fragments.FriendsListFragment;
import com.qwert2603.vkmutualgroups.fragments.GroupsListFragment;
import com.qwert2603.vkmutualgroups.util.InternetUtils;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKList;
import com.vk.sdk.api.model.VKUsersArray;

import java.io.Serializable;

import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.finished;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.loading;

/**
 * Activity, отображающая фрагмент-список друзей пользователя, предварительно его загружая.
 */
public class LoadingListActivity extends AbstractVkListActivity implements AbstractVkListFragment.Callbacks {

    @SuppressWarnings("unused")
    public static final String TAG = "LoadingFriendsListActi";

    public static final String EXTRA_FRAGMENT_TYPE = "com.qwert2603.vkmutualgroups.EXTRA_FRAGMENT_TYPE";

    private DataManager mDataManager;

    private String mQuery = "";

    public enum FragmentType implements Serializable {
        myFriends,
        myGroups
    }

    /**
     * Тип ткущего отображаемого фрагмента.
     */
    private FragmentType mCurrentFragmentType;

    @Override
    protected String getActionBarTitle() {
        switch (mCurrentFragmentType) {
            case myFriends:
                return getString(R.string.my_friends);
            case myGroups:
                return getString(R.string.my_groups);
            default:
                return getString(R.string.app_name);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDataManager = DataManager.get(this);

        mCurrentFragmentType = FragmentType.myFriends;

        setErrorTextViewText(getString(R.string.loading_failed));
        setErrorTextViewVisibility(View.INVISIBLE);

        setRefreshLayoutOnRefreshListener(this::refreshData);

        setActionButtonVisibility(View.INVISIBLE);
        setActionButtonOnClickListener((v) -> {
            if (mDataManager.getFetchingState() == finished) {
                switch (mCurrentFragmentType) {
                    case myFriends:
                        switch (mDataManager.getFriendsSortState()) {
                            case byAlphabet:
                                mDataManager.sortFriendsByMutual();
                                break;
                            case byMutual:
                                mDataManager.sortFriendsByAlphabet();
                                break;
                        }
                        break;
                    case myGroups:
                        switch (mDataManager.getGroupsSortState()) {
                            case byDefault:
                                mDataManager.sortGroupsByFriends();
                                break;
                            case byFriends:
                                mDataManager.sortGroupsByDefault();
                                break;
                        }
                }
                updateActionButtonIcon();
                refreshFriendsListFragment();
            }
        });

        switch (mDataManager.getFetchingState()) {
            case notStarted:
                loadFromDevice();
                break;
            case loading:
                // nth
                break;
            case finished:
                refreshFriendsListFragment();
                break;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mCurrentFragmentType = (FragmentType) intent.getSerializableExtra(EXTRA_FRAGMENT_TYPE);
        mQuery = "";
        invalidateOptionsMenu();
        updateActionBarTitle();
        updateActionButtonIcon();
        refreshFriendsListFragment();
    }

    private void updateActionButtonIcon() {
        switch (mCurrentFragmentType) {
            case myFriends:
                switch (mDataManager.getFriendsSortState()) {
                    case byAlphabet:
                        setActionButtonIcon(android.R.drawable.ic_menu_sort_alphabetically);
                        break;
                    case byMutual:
                        setActionButtonIcon(android.R.drawable.ic_menu_sort_by_size);
                        break;
                }
                break;
            case myGroups:
                switch (mDataManager.getGroupsSortState()) {
                    case byDefault:
                        setActionButtonIcon(android.R.drawable.ic_menu_sort_alphabetically);
                        break;
                    case byFriends:
                        setActionButtonIcon(android.R.drawable.ic_menu_sort_by_size);
                        break;
                }
        }
    }

    private void loadFromDevice() {
        long startTime = System.currentTimeMillis();
        setActionButtonIcon(android.R.drawable.ic_menu_sort_alphabetically);
        setRefreshLayoutRefreshing(true);
        setErrorTextViewVisibility(View.INVISIBLE);
        mDataManager.loadFromDevice(new Listener<Void>() {
            @Override
            public void onCompleted(Void v) {
                Log.d(TAG, "loadFromDevice ## onCompleted ## " + (System.currentTimeMillis() - startTime));
                refreshFriendsListFragment();
                setRefreshLayoutRefreshing(false);
                showSnackbar(R.string.loading_completed);
            }

            @Override
            public void onError(String e) {
                Log.e("AASSDD", e);
                setRefreshLayoutRefreshing(false);
                fetchFromVK();
            }
        });
    }

    private void fetchFromVK() {
        long startTime = System.currentTimeMillis();
        setActionButtonIcon(android.R.drawable.ic_menu_sort_alphabetically);
        setRefreshLayoutRefreshing(true);
        setErrorTextViewVisibility(View.INVISIBLE);
        mDataManager.fetchFromVK(new Listener<Void>() {
            @Override
            public void onCompleted(Void v) {
                Log.d(TAG, "fetchFromVK ## onCompleted ## " + (System.currentTimeMillis() - startTime));
                refreshFriendsListFragment();
                setRefreshLayoutRefreshing(false);
                showSnackbar(R.string.loading_completed);
            }

            @Override
            public void onError(String e) {
                Log.e("AASSDD", e);
                Fragment fragment = getListFragment();
                if (fragment instanceof AbstractVkListFragment) {
                    ((AbstractVkListFragment) fragment).notifyDataSetChanged();
                    showSnackbar(R.string.loading_failed, Snackbar.LENGTH_SHORT, R.string.refresh, (v) -> refreshData());
                } else {
                    setErrorTextViewVisibility(View.VISIBLE);
                }
                setRefreshLayoutRefreshing(false);
                setRefreshLayoutEnable(true);
            }
        });
    }

    private void refreshData() {
        if (mDataManager.getFetchingState() == loading) {
            return;
        }
        if (InternetUtils.isInternetConnected(this)) {
            fetchFromVK();
            notifyDataSetChanged();
        }
        else {
            showSnackbar(R.string.no_internet_connection, Snackbar.LENGTH_SHORT, R.string.refresh, (v) -> refreshData());
            setRefreshLayoutRefreshing(false);
        }
    }

    private void refreshFriendsListFragment() {
        setRefreshLayoutEnable(true);

        switch (mCurrentFragmentType) {
            case myFriends:
                VKUsersArray friends = mDataManager.getUsersFriends();
                if (friends != null) {
                    setActionButtonVisibility(View.VISIBLE);
                    VKUsersArray showingFriends;
                    if (mQuery == null || mQuery.equals("")) {
                        showingFriends = friends;
                    } else {
                        showingFriends = new VKUsersArray();

                        // поиск не зависит от регистра.
                        mQuery = mQuery.toLowerCase();
                        for (VKApiUserFull friend : friends) {
                            if (friend.first_name.toLowerCase().startsWith(mQuery) || friend.last_name.toLowerCase().startsWith(mQuery)) {
                                showingFriends.add(friend);
                            }
                        }
                    }
                    setListFragment(FriendsListFragment.newInstance(showingFriends, getString(R.string.no_friends)));
                } else {
                    removeFriendsListFragment();
                }
                break;
            case myGroups:
                VKApiCommunityArray groups = mDataManager.getUsersGroups();
                if (groups != null) {
                    VKApiCommunityArray showingGroups;
                    if (mQuery == null || mQuery.equals("")) {
                        showingGroups = groups;
                    } else {
                        showingGroups = new VKApiCommunityArray();

                        // поиск не зависит от регистра.
                        mQuery = mQuery.toLowerCase();
                        for (VKApiCommunityFull group : groups) {
                            if (group.name.toLowerCase().contains(mQuery)) {
                                showingGroups.add(group);
                            }
                        }
                    }
                    setListFragment(GroupsListFragment.newInstance(showingGroups, getString(R.string.no_groups)));
                } else {
                    removeFriendsListFragment();
                }
                break;
        }
    }

    private void removeFriendsListFragment() {
        setListFragment(null);
        setRefreshLayoutEnable(true);
        setActionButtonVisibility(View.INVISIBLE);
        setActionButtonIcon(android.R.drawable.ic_menu_sort_alphabetically);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.loading_list_activity, menu);

        MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setQuery(mQuery, false);
        searchView.setSubmitButtonEnabled(false);
        searchView.setQueryHint(getString(R.string.search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mQuery = newText;
                refreshFriendsListFragment();
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onListViewScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        VKList list = null;
        switch (mCurrentFragmentType) {
            case myFriends:
                list = mDataManager.getUsersFriends();
                break;
            case myGroups:
                list = mDataManager.getUsersGroups();
                break;
        }
        boolean b = ((list != null) && list.isEmpty())
                || (firstVisibleItem == 0) && (view.getChildAt(0) != null) && (view.getChildAt(0).getTop() == 0);

        setRefreshLayoutEnable(b);
    }

}
