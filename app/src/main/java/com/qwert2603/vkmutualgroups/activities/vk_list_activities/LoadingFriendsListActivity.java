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
import com.qwert2603.vkmutualgroups.activities.SettingsActivity;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.AbstractVkListFragment;
import com.qwert2603.vkmutualgroups.fragments.FriendsListFragment;
import com.qwert2603.vkmutualgroups.util.InternetUtils;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKUsersArray;

import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.finished;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.loading;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.notStarted;

/**
 * Activity, отображающая фрагмент-список друзей пользователя, предварительно его загружая.
 */
public class LoadingFriendsListActivity extends AbstractVkListActivity implements AbstractVkListFragment.Callbacks {

    @SuppressWarnings("unused")
    public static final String TAG = "LoadingFriendsListActi";

    private DataManager mDataManager;

    private String mQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDataManager = DataManager.get(this);

        setErrorTextViewText(getString(R.string.loading_failed));
        setErrorTextViewVisibility(View.INVISIBLE);

        setRefreshLayoutOnRefreshListener(this::refreshData);

        setActionButtonVisibility(View.INVISIBLE);
        setActionButtonOnClickListener((v) -> {
            if (mDataManager.getFetchingState() == finished) {
                switch (mDataManager.getFriendsSortState()) {
                    case byAlphabet:
                        mDataManager.sortFriendsByMutual();
                        setActionButtonIcon(android.R.drawable.ic_menu_sort_by_size);
                        break;
                    case byMutual:
                        mDataManager.sortFriendsByAlphabet();
                        setActionButtonIcon(android.R.drawable.ic_menu_sort_alphabetically);
                        break;
                }
                refreshFriendsListFragment();
            }
        });

        if (mDataManager.getFetchingState() == notStarted || mDataManager.getFetchingState() == finished) {
            loadFromDevice();
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
    }

    private void removeFriendsListFragment() {
        setListFragment(null);
        setRefreshLayoutEnable(true);
        setActionButtonVisibility(View.INVISIBLE);
        setActionButtonIcon(android.R.drawable.ic_menu_sort_alphabetically);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.loading_friends_list_activity, menu);

        MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setQuery(mQuery, false);
        searchView.setSubmitButtonEnabled(false);
        searchView.setQueryHint(getString(R.string.search_friends));
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

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_groups_list:
                if (mDataManager.getFetchingState() == finished) {
                    Intent intent = new Intent(this, UserGroupsListActivity.class);
                    startActivity(intent);
                }
                return true;
            case R.id.menu_setting:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListViewScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        VKUsersArray friends = mDataManager.getUsersFriends();
        boolean b = ((friends != null) && friends.isEmpty())
                || (firstVisibleItem == 0) && (view.getChildAt(0) != null) && (view.getChildAt(0).getTop() == 0);

        setRefreshLayoutEnable(b);
    }

}
