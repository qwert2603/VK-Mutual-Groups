package com.qwert2603.vkmutualgroups.activities;

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
import android.widget.Toast;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.AbstractVkListFragment;
import com.qwert2603.vkmutualgroups.fragments.FriendsListFragment;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;
import com.qwert2603.vkmutualgroups.util.InternetUtils;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKUsersArray;

import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.calculatingMutual;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.finished;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.loadingFriends;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.notStarted;

/**
 * Activity, отображающая фрагмент-список друзей пользователя, предварительно его загружая.
 */
public class LoadingFriendsListActivity extends AbstractVkListActivity implements AbstractVkListFragment.Callbacks {

    private static final String[] LOGIN_SCOPE = new String[] { VKScope.FRIENDS, VKScope.GROUPS, VKScope.MESSAGES };

    private DataManager mDataManager;
    private PhotoManager mPhotoManager;

    private String mQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDataManager = DataManager.get(this);
        mPhotoManager = PhotoManager.get(this);

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

        if (VKSdk.isLoggedIn()) {
            if (mDataManager.getFetchingState() == notStarted || mDataManager.getFetchingState() == finished) {
                loadFromDevice();
            }
        } else {
            VKSdk.login(this, LOGIN_SCOPE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (! VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                if (mDataManager.getFetchingState() == notStarted || mDataManager.getFetchingState() == finished) {
                    fetchFromVK();
                }
            }

            @Override
            public void onError(VKError error) {
                Toast.makeText(LoadingFriendsListActivity.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                VKSdk.login(LoadingFriendsListActivity.this, LOGIN_SCOPE);
            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void loadFromDevice() {
        setActionButtonIcon(android.R.drawable.ic_menu_sort_alphabetically);
        setRefreshLayoutRefreshing(true);
        setErrorTextViewVisibility(View.INVISIBLE);
        mDataManager.loadFromDevice(new DataManager.DataManagerListener() {
            @Override
            public void onFriendsLoaded() {
                refreshFriendsListFragment();
            }

            @Override
            public void onCompleted(Void v) {
                notifyDataSetChanged();
                setRefreshLayoutRefreshing(false);
                showSnackbar(R.string.loading_completed);
            }

            @Override
            public void onProgress() {
                notifyDataSetChanged();
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
        setActionButtonIcon(android.R.drawable.ic_menu_sort_alphabetically);
        setRefreshLayoutRefreshing(true);
        setErrorTextViewVisibility(View.INVISIBLE);
        mDataManager.fetchFromVK(new DataManager.DataManagerListener() {
            @Override
            public void onFriendsLoaded() {
                refreshFriendsListFragment();
            }

            @Override
            public void onCompleted(Void v) {
                notifyDataSetChanged();
                setRefreshLayoutRefreshing(false);
                showSnackbar(R.string.loading_completed);
            }

            @Override
            public void onProgress() {
                notifyDataSetChanged();
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
        if (mDataManager.getFetchingState() == loadingFriends || mDataManager.getFetchingState() == calculatingMutual) {
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
            case R.id.menu_logout:
                if (VKSdk.isLoggedIn()) {
                    VKSdk.logout();
                    mDataManager.clear();
                    mDataManager.clearDataOnDevice();
                    mPhotoManager.clearPhotosOnDevice();
                    removeFriendsListFragment();
                    VKSdk.login(this, LOGIN_SCOPE);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListViewScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        VKUsersArray friends = mDataManager.getUsersFriends();
        boolean b = friends == null
                || friends.isEmpty()
                || (firstVisibleItem == 0) && (view.getChildAt(0) != null) && (view.getChildAt(0).getTop() == 0);

        setRefreshLayoutEnable(b);
    }

}
