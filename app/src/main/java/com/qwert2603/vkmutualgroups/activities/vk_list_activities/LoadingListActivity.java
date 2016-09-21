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

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.errors_show.ErrorsHolder;
import com.qwert2603.vkmutualgroups.errors_show.ErrorsShowDialog;
import com.qwert2603.vkmutualgroups.fragments.AbstractVkListFragment;
import com.qwert2603.vkmutualgroups.fragments.FriendsListFragment;
import com.qwert2603.vkmutualgroups.fragments.GroupsListFragment;
import com.qwert2603.vkmutualgroups.util.InternetUtils;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKUsersArray;

import java.io.Serializable;

import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.finished;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.loading;

/**
 * Activity, загружающая данные и отображающая список друзей или групп.
 * Позволяет проводить поиск по отображаемому списку.
 */
public class LoadingListActivity extends AbstractVkListActivity implements AbstractVkListFragment.Callbacks {

    @SuppressWarnings("unused")
    public static final String TAG = "LoadingFriendsListActi";

    public static final String EXTRA_FRAGMENT_TYPE = "com.qwert2603.vkmutualgroups.EXTRA_FRAGMENT_TYPE";

    private DataManager mDataManager;

    private String mQuery = "";
    private boolean mSearchResultEmpty = true;

    public enum FragmentType implements Serializable {
        myFriends,
        myGroups
    }

    /**
     * Тип текущего отображаемого фрагмента.
     */
    private FragmentType mCurrentFragmentType;

    private DataManager.DataLoadingListener mDataLoadingListener = new DataManager.DataLoadingListener() {
        @Override
        public void onLoadingStarted() {
            setActionButtonIcon(android.R.drawable.ic_menu_sort_alphabetically);
            setRefreshLayoutRefreshing(true);
            setErrorTextViewVisibility(View.INVISIBLE);
            notifyDataSetChanged();
        }

        @Override
        public void onCompleted(Void v) {
            refreshFriendsListFragment();
            setRefreshLayoutRefreshing(false);
            showSnackbar(R.string.loading_completed);
        }

        @Override
        public void onError(String e) {
            Log.e(TAG, e);
            ErrorsHolder.get().addError(LoadingListActivity.this, new Throwable(e));
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
    };

    @Override
    protected String getActionBarTitle() {
        if (mCurrentFragmentType != null) {
            switch (mCurrentFragmentType) {
                case myFriends:
                    return getString(R.string.my_friends);
                case myGroups:
                    return getString(R.string.my_groups);
            }
        }
        return getString(R.string.app_name);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDataManager = DataManager.get(this);
        mDataManager.addDataLoadingListener(mDataLoadingListener);

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

        onFirstLoading();
    }

    @Override
    protected void onDestroy() {
        mDataManager.removeDataLoadingListener(mDataLoadingListener);
        super.onDestroy();
    }

    /**
     * Проверить, загружены ли данные.
     * Если загружены, то оторазить их, иначе - загрузить.
     */
    private void onFirstLoading() {
        switch (mDataManager.getFetchingState()) {
            case notStarted:
                mDataManager.load(false);
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
        if (mCurrentFragmentType == null) {
            mCurrentFragmentType = FragmentType.myFriends;
            onFirstLoading();
        }
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

    private void refreshData() {
        if (mDataManager.getFetchingState() == loading) {
            return;
        }
        if (InternetUtils.isInternetConnected(this)) {
            mDataManager.load(true);
        } else {
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
                    String emptyText;
                    if (mQuery == null || mQuery.equals("")) {
                        showingFriends = friends;
                        emptyText = getString(R.string.no_friends);
                    } else {
                        showingFriends = new VKUsersArray();
                        emptyText = getString(R.string.nothing_found);

                        // поиск не зависит от регистра.
                        mQuery = mQuery.toLowerCase();
                        for (VKApiUserFull friend : friends) {
                            if (friend.first_name.toLowerCase().startsWith(mQuery) || friend.last_name.toLowerCase().startsWith(mQuery)) {
                                showingFriends.add(friend);
                            }
                        }
                    }
                    mSearchResultEmpty = showingFriends.isEmpty();
                    setListFragment(FriendsListFragment.newInstance(showingFriends, emptyText));
                } else {
                    mSearchResultEmpty = true;
                    removeFriendsListFragment();
                }
                break;
            case myGroups:
                VKApiCommunityArray groups = mDataManager.getUsersGroups();
                if (groups != null) {
                    setActionButtonVisibility(View.VISIBLE);
                    VKApiCommunityArray showingGroups;
                    String emptyText;
                    if (mQuery == null || mQuery.equals("")) {
                        emptyText = getString(R.string.no_groups);
                        showingGroups = groups;
                    } else {
                        showingGroups = new VKApiCommunityArray();
                        emptyText = getString(R.string.nothing_found);

                        // поиск не зависит от регистра.
                        mQuery = mQuery.toLowerCase();
                        for (VKApiCommunityFull group : groups) {
                            if (group.name.toLowerCase().contains(mQuery)) {
                                showingGroups.add(group);
                            }
                        }
                    }
                    mSearchResultEmpty = showingGroups.isEmpty();
                    setListFragment(GroupsListFragment.newInstance(showingGroups, emptyText));
                } else {
                    mSearchResultEmpty = true;
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setQuery(mQuery, true);
        searchView.setSubmitButtonEnabled(false);
        searchView.setQueryHint(getString(R.string.search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mQuery = newText;
                refreshFriendsListFragment();
                if (mQuery.equals("mode show errors on")) {
                    menu.findItem(R.id.show_errors).setVisible(true);
                }
                return true;
            }
        });
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.show_errors) {
            ErrorsShowDialog.newInstance().show(getFragmentManager(), "show_errors");
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onListViewScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // TODO: 21.09.16 не скрывать крутящуюся зарузку при скроллинге вниз во время загрузки
        boolean b = mSearchResultEmpty
                || (firstVisibleItem == 0) && (view.getChildAt(0) != null) && (view.getChildAt(0).getTop() == 0);

        setRefreshLayoutEnable(b);
    }

}
