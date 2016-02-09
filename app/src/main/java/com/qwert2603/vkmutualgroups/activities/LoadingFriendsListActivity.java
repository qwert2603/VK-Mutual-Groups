package com.qwert2603.vkmutualgroups.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.ScrollCallbackableFriendsListFragment;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;
import com.qwert2603.vkmutualgroups.util.InternetUtils;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.model.VKUsersArray;

import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.calculatingMutual;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.finished;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.loadingFriends;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.notStarted;

/**
 * Activity, отображающая фрагмент-список друзей пользователя, предварительно его загружая.
 */
public class LoadingFriendsListActivity extends BaseVkActivity implements ScrollCallbackableFriendsListFragment.Callbacks {

    private static final String[] LOGIN_SCOPE = new String[] { VKScope.FRIENDS, VKScope.GROUPS, VKScope.MESSAGES };

    private DataManager mDataManager;
    private PhotoManager mPhotoManager;

    private CoordinatorLayout mCoordinatorLayout;
    private TextView mErrorTextView;
    private SwipeRefreshLayout mRefreshLayout;
    private FloatingActionButton mActionButton;

    /**
     * Пустое View для нормального отображения mRefreshLayout.
     */
    private View mFakeView;

    private boolean mIsFetchingErrorHappened = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_friends_list);

        // Чтобы mRefreshLayout нормально отображался.
        // Просто пустой фрагмент для фона.
        ListFragment fragment = new ListFragment();
        fragment.setListAdapter(new ArrayAdapter<>(this, 0, new String[]{}));
        getFragmentManager().beginTransaction()
                .replace(R.id.fake_fragment_container, fragment)
                .commitAllowingStateLoss();

        mFakeView = findViewById(R.id.fake_fragment_container);

        mDataManager = DataManager.get(this);
        mPhotoManager = PhotoManager.get(this);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);

        mErrorTextView = (TextView) findViewById(R.id.error_text_view);

        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        mRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary);
        mRefreshLayout.setOnRefreshListener(this::refreshData);

        mActionButton = (FloatingActionButton) findViewById(R.id.action_button);
        mActionButton.setOnClickListener((v) -> {
            if (mDataManager.getFetchingState() == finished) {
                switch (mDataManager.getFriendsSortState()) {
                    case byAlphabet:
                        mDataManager.sortFriendsByMutual();
                        mActionButton.setIcon(android.R.drawable.ic_menu_sort_by_size);
                        break;
                    case byMutual:
                        mDataManager.sortFriendsByAlphabet();
                        mActionButton.setIcon(android.R.drawable.ic_menu_sort_alphabetically);
                        break;
                }
                refreshScrollCallbackableFriendsListFragment();
            }
        });

        if (VKSdk.isLoggedIn()) {
            if (mDataManager.getFetchingState() == notStarted || mDataManager.getFetchingState() == finished) {
                loadFromDevice();
            }
        } else {
            VKSdk.login(this, LOGIN_SCOPE);
        }

        removeScrollCallbackableFriendsListFragment();
        updateUI();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (! VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                if (mDataManager.getFetchingState() == notStarted || mDataManager.getFetchingState() == finished) {
                    fetchFromVK();
                    updateUI();
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

    private void updateUI() {
        invalidateOptionsMenu();
        mErrorTextView.setVisibility(View.INVISIBLE);
        if (! VKSdk.isLoggedIn()) {
            removeScrollCallbackableFriendsListFragment();
            return;
        }
        if (mIsFetchingErrorHappened) {
            onFetchingErrorUI();
        } else {
            switch (mDataManager.getFetchingState()) {
                case notStarted:
                    removeScrollCallbackableFriendsListFragment();
                    break;
                case loadingFriends:
                    break;
                case calculatingMutual:
                    notifyFragmentDataSetChanged();
                    break;
                case finished:
                    notifyFragmentDataSetChanged();
                    break;
            }
        }
    }

    private void onFetchingErrorUI() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment_container);
        if(fragment != null && fragment instanceof ScrollCallbackableFriendsListFragment) {
            ((ScrollCallbackableFriendsListFragment) fragment).notifyDataSetChanged();
            Snackbar.make(mCoordinatorLayout, R.string.error_message, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.refresh, (v) -> refreshData())
                    .show();
        } else {
            mErrorTextView.setVisibility(View.VISIBLE);
        }
    }

    private void notifyFragmentDataSetChanged() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment_container);
        if(fragment != null && fragment instanceof ScrollCallbackableFriendsListFragment) {
            ((ScrollCallbackableFriendsListFragment) fragment).notifyDataSetChanged();
        } else {
            refreshScrollCallbackableFriendsListFragment();
        }
    }

    private void loadFromDevice() {
        mActionButton.setIcon(android.R.drawable.ic_menu_sort_alphabetically);
        mRefreshLayout.post(() -> mRefreshLayout.setRefreshing(true));
        mIsFetchingErrorHappened = false;
        mDataManager.loadFromDevice(new DataManager.DataManagerListener() {
            @Override
            public void onFriendsLoaded() {
                refreshScrollCallbackableFriendsListFragment();
                updateUI();
            }

            @Override
            public void onCompleted(Void v) {
                updateUI();
                mRefreshLayout.setRefreshing(false);
                Snackbar.make(mCoordinatorLayout, R.string.loading_completed, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress() {
                updateUI();
            }

            @Override
            public void onError(String e) {
                Log.e("AASSDD", e);
                mRefreshLayout.setRefreshing(false);
                fetchFromVK();
            }
        });
    }

    private void fetchFromVK() {
        mActionButton.setIcon(android.R.drawable.ic_menu_sort_alphabetically);
        mRefreshLayout.post(() -> mRefreshLayout.setRefreshing(true));
        mIsFetchingErrorHappened = false;
        mDataManager.fetchFromVK(new DataManager.DataManagerListener() {
            @Override
            public void onFriendsLoaded() {
                refreshScrollCallbackableFriendsListFragment();
                updateUI();
            }

            @Override
            public void onCompleted(Void v) {
                updateUI();
                mRefreshLayout.setRefreshing(false);
                Snackbar.make(mCoordinatorLayout, R.string.loading_completed, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress() {
                updateUI();
            }

            @Override
            public void onError(String e) {
                mIsFetchingErrorHappened = true;
                updateUI();
                mRefreshLayout.setRefreshing(false);
                Log.e("AASSDD", e);
            }
        });
    }

    private void refreshData() {
        if (mDataManager.getFetchingState() == loadingFriends || mDataManager.getFetchingState() == calculatingMutual) {
            return;
        }
        if (InternetUtils.isInternetConnected(this)) {
            fetchFromVK();
            invalidateOptionsMenu();
        }
        else {
            mRefreshLayout.setRefreshing(false);
            Snackbar.make(mCoordinatorLayout, R.string.no_internet_connection, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.refresh, (v) -> refreshData())
                    .show();
        }
    }

    private void refreshScrollCallbackableFriendsListFragment() {
        mActionButton.setVisibility(View.VISIBLE);

        VKUsersArray friends = mDataManager.getUsersFriends();
        mFakeView.setVisibility(friends == null || friends.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        mRefreshLayout.setEnabled(true);

        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, ScrollCallbackableFriendsListFragment.newInstance(0))
                .commitAllowingStateLoss();
    }

    private void removeScrollCallbackableFriendsListFragment() {
        mActionButton.setVisibility(View.INVISIBLE);
        mActionButton.setIcon(android.R.drawable.ic_menu_sort_alphabetically);

        mFakeView.setVisibility(View.VISIBLE);
        mRefreshLayout.setEnabled(true);

        FragmentManager fm = getFragmentManager();

        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            fm.beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.loading_friends_list_activity, menu);

        MenuItem groupsListMenuItem = menu.findItem(R.id.menu_groups_list);
        if (mDataManager.getFetchingState() != finished) {
            groupsListMenuItem.setEnabled(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_groups_list:
                Intent intent = new Intent(this, UserGroupsListActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_logout:
                if (VKSdk.isLoggedIn()) {
                    VKSdk.logout();
                    mDataManager.clear();
                    mDataManager.clearDataOnDevice();
                    mPhotoManager.clearPhotosOnDevice();
                    updateUI();
                    VKSdk.login(this, LOGIN_SCOPE);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @NonNull
    @Override
    public CoordinatorLayout getCoordinatorLayout() {
        return mCoordinatorLayout;
    }

    @Override
    protected void notifyDataSetChanged() {
        notifyFragmentDataSetChanged();
    }

    @Override
    public void onListViewScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        boolean b = (firstVisibleItem == 0) && (view.getChildAt(0) != null) && (view.getChildAt(0).getTop() == 0);
        mRefreshLayout.setEnabled(b);
    }

}
