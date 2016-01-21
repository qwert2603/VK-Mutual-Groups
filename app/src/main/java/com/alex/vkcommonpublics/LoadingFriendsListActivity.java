package com.alex.vkcommonpublics;

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
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKUsersArray;

import static com.alex.vkcommonpublics.DataManager.FetchingState.calculatingCommons;
import static com.alex.vkcommonpublics.DataManager.FetchingState.loadingFriends;
import static com.alex.vkcommonpublics.DataManager.FetchingState.notStarted;

/**
 * Activity, отображающая фрагмент-список друзей пользователя, предварительно его загружая.
 */
public class LoadingFriendsListActivity extends AppCompatActivity {

    private static final String[] LOGIN_SCOPE = new String[] { VKScope.FRIENDS, VKScope.GROUPS };

    private DataManager mDataManager = DataManager.get();

    private ProgressBar mProgressBar;
    private TextView mErrorTextView;

    private boolean mIsFetchingErrorHappened = false;
    private String mLastFetchingError = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fetching_friends_list);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mErrorTextView = (TextView) findViewById(R.id.error_text_view);

        mDataManager.setDataManagerListener(new DataManager.DataManagerListener() {
            @Override
            public void onFriendsFetched() {
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
                mLastFetchingError = e;
                updateUI();
                Log.e("AASSDD", e);
            }
        });

        if (VKSdk.isLoggedIn()) {
            if (mDataManager.getFetchingState() == notStarted) {
                fetch();
            }
        } else {
            VKSdk.login(this, LOGIN_SCOPE);
        }

        updateUI();
    }

    @Override
    protected void onDestroy() {
        PhotoManager.get(this).quitDownloadingThread();
        DataManager.get().quitProcessingThread();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (! VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                if (mDataManager.getFetchingState() == notStarted) {
                    fetch();
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
                case calculatingCommons:
                    notifyDataSetChanged();
                    mProgressBar.setVisibility(View.VISIBLE);
                    break;
                case finished:
                    notifyDataSetChanged();
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

    private void notifyDataSetChanged() {
        FragmentManager fm = getFragmentManager();
        FriendsListFragment fragment = (FriendsListFragment) fm.findFragmentById(R.id.fragment_container);
        if(fragment != null) {
            fragment.notifyDataSetChanged();
        }
    }

    private void fetch() {
        mIsFetchingErrorHappened = false;
        mDataManager.fetch();
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
        MenuItem groupsListMenuItem = menu.findItem(R.id.menu_groups_list);
        switch (mDataManager.getFriendsSortState()) {
            case notSorted:
                sortMenuItem.setTitle(R.string.sort);
                sortMenuItem.setEnabled(false);
                break;
            case byAlphabet:
                sortMenuItem.setIcon(android.R.drawable.ic_menu_sort_alphabetically);
                sortMenuItem.setTitle(R.string.sort_alphabetically);
                break;
            case byCommons:
                sortMenuItem.setIcon(android.R.drawable.ic_menu_sort_by_size);
                sortMenuItem.setTitle(R.string.sort_by_commons);
                break;
        }
        if (mDataManager.getFetchingState() != DataManager.FetchingState.finished) {
            sortMenuItem.setEnabled(false);
            groupsListMenuItem.setEnabled(false);
        }

        MenuItem refreshMenuItem = menu.findItem(R.id.menu_refresh);
        DataManager.FetchingState currentState = mDataManager.getFetchingState();
        if (currentState == loadingFriends || currentState == calculatingCommons) {
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
                        mDataManager.sortFriendsByCommons();
                        break;
                    case byCommons:
                        mDataManager.sortFriendsByAlphabet();
                        break;
                }
                refreshFriendsListFragment();
                invalidateOptionsMenu();
                return true;
            case R.id.menu_refresh:
                if (isInternetConnected()) {
                    fetch();
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
                    updateUI();
                    VKSdk.login(this, LOGIN_SCOPE);
                }
                return true;
            // FIXME: 21.01.2016 удалить тестовые пункты меню и mLastFetchingError.
            case R.id.menu_test_check_api:
                VKApi.friends().get(VKParameters.from("fields", "name")).executeWithListener(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(VKResponse response) {
                        Toast.makeText(LoadingFriendsListActivity.this,
                                "_check_api onComplete\n" + ((VKUsersArray) response.parsedModel).size(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
                        Toast.makeText(LoadingFriendsListActivity.this,
                                "_check_api attemptFailed\n" + attemptNumber, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(VKError error) {
                        Toast.makeText(LoadingFriendsListActivity.this,
                                "_check_api onError\n" + error, Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            case R.id.menu_test_last_error:
                Toast.makeText(LoadingFriendsListActivity.this,
                        "_last_error\n" + mLastFetchingError, Toast.LENGTH_SHORT).show();
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