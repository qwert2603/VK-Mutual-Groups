package com.alex.vkmutualgroups;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;

import static com.alex.vkmutualgroups.DataManager.FetchingState.calculatingMutual;
import static com.alex.vkmutualgroups.DataManager.FetchingState.finished;
import static com.alex.vkmutualgroups.DataManager.FetchingState.loadingFriends;
import static com.alex.vkmutualgroups.DataManager.FetchingState.notStarted;
import static com.alex.vkmutualgroups.DataManager.FriendsSortState.byMutual;

/**
 * Activity, отображающая фрагмент-список друзей пользователя, предварительно его загружая.
 */
public class LoadingFriendsListActivity extends AppCompatActivity {

    private static final String[] LOGIN_SCOPE = new String[] { VKScope.FRIENDS, VKScope.GROUPS, VKScope.MESSAGES };

    private DataManager mDataManager;
    private PhotoManager mPhotoManager;

    private ProgressBar mProgressBar;
    private TextView mErrorTextView;

    private boolean mIsFetchingErrorHappened = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fetching_friends_list);

        mDataManager = DataManager.get(this);
        mPhotoManager = PhotoManager.get(this);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mErrorTextView = (TextView) findViewById(R.id.error_text_view);

        if (VKSdk.isLoggedIn()) {
            if (mDataManager.getFetchingState() == notStarted) {
                loadFromDevice();
            }
        } else {
            VKSdk.login(this, LOGIN_SCOPE);
        }

        updateUI();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (! VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                if (mDataManager.getFetchingState() == notStarted) {
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
        mProgressBar.setVisibility(View.INVISIBLE);
        mErrorTextView.setVisibility(View.INVISIBLE);
        if (! VKSdk.isLoggedIn()) {
            removeFriendsListFragment();
            return;
        }
        if (mIsFetchingErrorHappened) {
            onFetchingErrorUI();
        }
        else {
            switch (mDataManager.getFetchingState()) {
                case notStarted:
                    removeFriendsListFragment();
                    break;
                case loadingFriends:
                    mProgressBar.setVisibility(View.VISIBLE);
                    break;
                case calculatingMutual:
                    notifyFragmentDataSetChanged();
                    mProgressBar.setVisibility(View.VISIBLE);
                    break;
                case finished:
                    notifyFragmentDataSetChanged();
                    break;
            }
        }
    }

    private void onFetchingErrorUI() {
        FragmentManager fm = getFragmentManager();
        FriendsListFragment fragment = (FriendsListFragment) fm.findFragmentById(R.id.fragment_container);
        if(fragment != null) {
            fragment.notifyDataSetChanged();
            Toast.makeText(this, R.string.error_message, Toast.LENGTH_SHORT).show();
        }
        else {
            mErrorTextView.setVisibility(View.VISIBLE);
        }
    }

    private void notifyFragmentDataSetChanged() {
        FragmentManager fm = getFragmentManager();
        FriendsListFragment fragment = (FriendsListFragment) fm.findFragmentById(R.id.fragment_container);
        if(fragment == null) {
            refreshFriendsListFragment();
        }
        else {
            fragment.notifyDataSetChanged();
        }
    }

    private void loadFromDevice() {
        mIsFetchingErrorHappened = false;
        mDataManager.loadFromDevice(new DataManager.DataManagerListener() {
            @Override
            public void onFriendsLoaded() {
                refreshFriendsListFragment();
                updateUI();
            }

            @Override
            public void onCompleted(Void v) {
                updateUI();
                Toast.makeText(LoadingFriendsListActivity.this, R.string.loading_completed, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress() {
                updateUI();
            }

            @Override
            public void onError(String e) {
                Log.e("AASSDD", e);
                fetchFromVK();
            }
        });
    }

    private void fetchFromVK() {
        mIsFetchingErrorHappened = false;
        mDataManager.fetchFromVK(new DataManager.DataManagerListener() {
            @Override
            public void onFriendsLoaded() {
                refreshFriendsListFragment();
                updateUI();
            }

            @Override
            public void onCompleted(Void v) {
                updateUI();
                Toast.makeText(LoadingFriendsListActivity.this, R.string.loading_completed, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress() {
                updateUI();
            }

            @Override
            public void onError(String e) {
                mIsFetchingErrorHappened = true;
                updateUI();
                Log.e("AASSDD", e);
            }
        });
    }

    private void removeFriendsListFragment() {
        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if(fragment != null) {
            fm.beginTransaction().remove(fragment).commitAllowingStateLoss();
        }
    }

    private void refreshFriendsListFragment() {
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction().replace(R.id.fragment_container, FriendsListFragment.newInstance(0)).commitAllowingStateLoss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.loading_friends_list_activity, menu);

        MenuItem sortMenuItem = menu.findItem(R.id.menu_sort);
        sortMenuItem.setChecked(mDataManager.getFriendsSortState() == byMutual);

        MenuItem groupsListMenuItem = menu.findItem(R.id.menu_groups_list);
        if (mDataManager.getFetchingState() != finished) {
            sortMenuItem.setEnabled(false);
            groupsListMenuItem.setEnabled(false);
        }

        MenuItem refreshMenuItem = menu.findItem(R.id.menu_refresh);
        DataManager.FetchingState currentState = mDataManager.getFetchingState();
        if (currentState == loadingFriends || currentState == calculatingMutual) {
            refreshMenuItem.setEnabled(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort:
                switch (mDataManager.getFriendsSortState()) {
                    case byAlphabet:
                        mDataManager.sortFriendsByMutual();
                        break;
                    case byMutual:
                        mDataManager.sortFriendsByAlphabet();
                        break;
                }
                refreshFriendsListFragment();
                invalidateOptionsMenu();
                return true;
            case R.id.menu_refresh:
                if (isInternetConnected()) {
                    fetchFromVK();
                    invalidateOptionsMenu();
                }
                else {
                    Toast.makeText(LoadingFriendsListActivity.this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.menu_groups_list:
                Intent intent = new Intent(this, UserGroupListActivity.class);
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

    private boolean isInternetConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
}
