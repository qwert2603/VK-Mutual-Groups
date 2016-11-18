package com.qwert2603.vkmutualgroups.activities.vk_list_activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.qwert2603.vkmutualgroups.Listener;
import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.activities.SettingsActivity;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.AbstractVkListFragment;
import com.qwert2603.vkmutualgroups.fragments.ConfirmationDialogFragment;
import com.qwert2603.vkmutualgroups.fragments.SendMessageDialogFragment;
import com.qwert2603.vkmutualgroups.util.VkLogOutUtil;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiUserFull;

import static com.qwert2603.vkmutualgroups.activities.vk_list_activities.LoadingListActivity.FragmentType.myFriends;
import static com.qwert2603.vkmutualgroups.activities.vk_list_activities.LoadingListActivity.FragmentType.myGroups;

/**
 * Activity, отображающая фрагмент-список (друзей или групп).
 * Это самая базовая Activity.
 * Она позволяет отправлять сообщения, удалять и добавлять друзей,
 * выходить из групп, вступать в группы, переходить по переданному url.
 * Также она предоставлят доступ к элементам UI: TextView-ошибка, RefreshLayout, ActionButton и методы для показа Snackbar.
 */
public abstract class AbstractVkListActivity extends AppCompatActivity {

    @SuppressWarnings("unused")
    public static final String TAG = "AbstractVkListActivity";

    private static final int REQUEST_SEND_MESSAGE = 1;
    private static final int REQUEST_DELETE_FRIEND = 2;
    private static final int REQUEST_ADD_FRIEND = 3;
    private static final int REQUEST_LEAVE_GROUP = 4;
    private static final int REQUEST_JOIN_GROUP = 5;

    private static final String friendToDeleteId = "friendToDeleteId";
    private static final String friendToAdd = "friendToAdd";
    private static final String groupToLeaveId = "groupToLeaveId";
    private static final String groupToJoin = "groupToJoin";

    private DataManager mDataManager;

    private Bundle mArgs;

    private TextView mErrorTextView;
    private SwipeRefreshLayout mRefreshLayout;
    private FloatingActionButton mActionButton;
    private CoordinatorLayout mCoordinatorLayout;

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private ActionBarDrawerToggle mActionBarDrawerToggle;

    private volatile boolean isResumed = false;

    private DataManager.DataLoadingListener mDataLoadingListener = new DataManager.DataLoadingListener() {
        @Override
        public void onLoadingStarted() {
            setRefreshLayoutRefreshing(true);
            notifyDataSetChanged();
        }

        @Override
        public void onCompleted(Void v) {
            setRefreshLayoutRefreshing(false);
            notifyDataSetChanged();
        }

        @Override
        public void onError(String e) {
            Log.e(TAG, e);
            setRefreshLayoutRefreshing(false);
            notifyDataSetChanged();
        }
    };

    private TargetFragment mTargetFragment;

    protected abstract String getActionBarTitle();

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vk_list);

        mDataManager = DataManager.get(this);
        mDataManager.addDataLoadingListener(mDataLoadingListener);

        mArgs = new Bundle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        mActionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.open, R.string.close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
                getSupportActionBar().setTitle(R.string.app_name);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
                getSupportActionBar().setTitle(getActionBarTitle());
            }
        };
        mDrawerLayout.setDrawerListener(mActionBarDrawerToggle);
        mNavigationView.setNavigationItemSelectedListener(item -> {
            mDrawerLayout.closeDrawer(mNavigationView);
            switch (item.getItemId()) {
                case R.id.menu_my_friends:
                    Intent intent = new Intent(this, LoadingListActivity.class);
                    intent.putExtra(LoadingListActivity.EXTRA_FRAGMENT_TYPE, myFriends);
                    startActivity(intent);
                    return true;
                case R.id.menu_my_groups:
                    Intent intent2 = new Intent(this, LoadingListActivity.class);
                    intent2.putExtra(LoadingListActivity.EXTRA_FRAGMENT_TYPE, myGroups);
                    startActivity(intent2);
                    return true;
                case R.id.menu_setting:
                    Intent intent3 = new Intent(this, SettingsActivity.class);
                    startActivity(intent3);
                    return true;
                case R.id.menu_log_out:
                    VkLogOutUtil.logOut(this);
                    return true;
            }
            return false;
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mTargetFragment = TargetFragment.newInstance(AbstractVkListActivity.this);
        getFragmentManager()
                .beginTransaction()
                .add(R.id.target_fragment_container, mTargetFragment)
                .commitAllowingStateLoss();

        setListFragment(null);

        mErrorTextView = (TextView) findViewById(R.id.error_text_view);

        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        mRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary);

        mActionButton = (FloatingActionButton) findViewById(R.id.action_button);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mActionBarDrawerToggle.syncState();
        updateActionBarTitle();
    }

    @Override
    protected void onDestroy() {
        mDataManager.removeDataLoadingListener(mDataLoadingListener);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        notifyDataSetChanged();
        isResumed = true;
    }

    @Override
    protected void onPause() {
        isResumed = false;
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mActionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    @SuppressWarnings("ConstantConditions")
    protected void updateActionBarTitle() {
        getSupportActionBar().setTitle(getActionBarTitle());
    }

    protected void setListFragment(AbstractVkListFragment fragment) {
        if (isFinishing()) {
            return;
        }

        // если предыдущий фрагмент запускал ActionMode, убираем его.
        startActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.finish();
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        });

        Fragment newFragment = fragment;
        if (newFragment == null) {
            // Чтобы mRefreshLayout нормально отображался.
            // Просто пустой фрагмент для фона.
            ListFragment fakeListFragment = new ListFragment();
            fakeListFragment.setListAdapter(new ArrayAdapter<>(this, 0, new String[]{}));
            newFragment = fakeListFragment;
        }
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, newFragment).commitAllowingStateLoss();
    }

    protected final Fragment getListFragment() {
        return getFragmentManager().findFragmentById(R.id.fragment_container);
    }

    /**
     * Уведомляет о том, что операция (удаления из друзей или выхода из группы) успешно завершилась.
     */
    @CallSuper
    protected void notifyDataSetChanged() {
        Fragment fragment = getListFragment();
        if (fragment instanceof AbstractVkListFragment) {
            ((AbstractVkListFragment) fragment).notifyDataSetChanged();
        }
    }

    protected void setErrorTextViewVisibility(int visibility) {
        mErrorTextView.setVisibility(visibility);
    }

    protected void setErrorTextViewText(String text) {
        mErrorTextView.setText(text);
    }

    protected void setRefreshLayoutEnable(boolean enable) {
        mRefreshLayout.setEnabled(enable);
    }

    protected void setRefreshLayoutRefreshing(boolean refreshing) {
        mRefreshLayout.post(() -> mRefreshLayout.setRefreshing(refreshing));
    }

    protected void setRefreshLayoutOnRefreshListener(SwipeRefreshLayout.OnRefreshListener listener) {
        mRefreshLayout.setOnRefreshListener(listener);
    }

    protected void setActionButtonVisibility(int visibility) {
        mActionButton.setVisibility(visibility);
    }

    protected void setActionButtonIcon(int icon) {
        mActionButton.setImageResource(icon);
    }

    protected void setActionButtonOnClickListener(View.OnClickListener listener) {
        mActionButton.setOnClickListener(listener);
    }

    protected void showSnackbar(int stringRes) {
        showSnackbar(stringRes, Snackbar.LENGTH_SHORT, 0, null);
    }

    protected void showSnackbar(int stringRes, int duration) {
        showSnackbar(stringRes, duration, 0, null);
    }

    protected void showSnackbar(int stringRes, int duration, int actionText, View.OnClickListener listener) {
        if (!isResumed) {
            return;
        }
        Snackbar snackbar = Snackbar.make(mCoordinatorLayout, stringRes, duration);
        if (listener != null) {
            snackbar.setAction(actionText, listener);
        }
        snackbar.show();
    }

    @Override
    @CallSuper
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    @CallSuper
    public boolean onPrepareOptionsMenu(Menu menu) {
        return !mDrawerLayout.isDrawerOpen(mNavigationView) && super.onPrepareOptionsMenu(menu);
    }

    @Override
    @CallSuper
    public boolean onOptionsItemSelected(MenuItem item) {
        return mActionBarDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    /**
     * Перейти по переданному url.
     */
    public void navigateTo(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        intent = Intent.createChooser(intent, getString(R.string.open_link_with));
        startActivity(intent);
    }

    public final void sendMessage(int friendId) {
        SendMessageDialogFragment sendMessageDialogFragment = SendMessageDialogFragment.newInstance(friendId);
        sendMessageDialogFragment.setTargetFragment(mTargetFragment, REQUEST_SEND_MESSAGE);
        sendMessageDialogFragment.show(getFragmentManager(), SendMessageDialogFragment.TAG);
    }

    public final void deleteFriend(VKApiUserFull friend) {
        mArgs.putInt(friendToDeleteId, friend.id);

        String title = getString(R.string.friend_name, friend.first_name, friend.last_name);
        String question = getString(R.string.delete_friend) + "?";
        ConfirmationDialogFragment dialogFragment = ConfirmationDialogFragment.newInstance(title, question);
        dialogFragment.setTargetFragment(mTargetFragment, REQUEST_DELETE_FRIEND);
        dialogFragment.show(getFragmentManager(), ConfirmationDialogFragment.TAG);
    }

    public final void addFriend(VKApiUserFull friend) {
        mArgs.putParcelable(friendToAdd, friend);

        String title = getString(R.string.friend_name, friend.first_name, friend.last_name);
        String question = getString(R.string.add_to_friends) + "?";
        ConfirmationDialogFragment dialogFragment = ConfirmationDialogFragment.newInstance(title, question);
        dialogFragment.setTargetFragment(mTargetFragment, REQUEST_ADD_FRIEND);
        dialogFragment.show(getFragmentManager(), ConfirmationDialogFragment.TAG);
    }

    public final void leaveGroup(VKApiCommunityFull group) {
        mArgs.putInt(groupToLeaveId, group.id);

        String title = group.name;
        String question = getString(R.string.leave_group) + "?";
        ConfirmationDialogFragment dialogFragment = ConfirmationDialogFragment.newInstance(title, question);
        dialogFragment.setTargetFragment(mTargetFragment, REQUEST_LEAVE_GROUP);
        dialogFragment.show(getFragmentManager(), ConfirmationDialogFragment.TAG);
    }

    public final void joinGroup(VKApiCommunityFull group) {
        mArgs.putParcelable(groupToJoin, group);

        String title = group.name;
        String question = getString(R.string.join_group) + "?";
        ConfirmationDialogFragment dialogFragment = ConfirmationDialogFragment.newInstance(title, question);
        dialogFragment.setTargetFragment(mTargetFragment, REQUEST_JOIN_GROUP);
        dialogFragment.show(getFragmentManager(), ConfirmationDialogFragment.TAG);
    }

    public static class TargetFragment extends Fragment {

        public static TargetFragment newInstance(AbstractVkListActivity abstractVkListActivity) {
            TargetFragment targetFragment = new TargetFragment();
            targetFragment.mAbstractVkListActivity = abstractVkListActivity;
            return targetFragment;
        }

        private AbstractVkListActivity mAbstractVkListActivity;

        public TargetFragment() {
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (resultCode == Activity.RESULT_OK) {
                switch (requestCode) {
                    case REQUEST_SEND_MESSAGE:
                        mAbstractVkListActivity.showSnackbar(R.string.message_sent);
                        break;
                    case REQUEST_DELETE_FRIEND:
                        mAbstractVkListActivity.mDataManager.deleteFriend(mAbstractVkListActivity.mArgs.getInt(friendToDeleteId), new Listener<Void>() {
                            @Override
                            public void onCompleted(Void aVoid) {
                                mAbstractVkListActivity.showSnackbar(R.string.friend_deleted_successfully);
                                mAbstractVkListActivity.notifyDataSetChanged();
                            }

                            @Override
                            public void onError(String e) {
                                mAbstractVkListActivity.showSnackbar(R.string.friend_deleting_error);
                            }
                        });
                        break;
                    case REQUEST_ADD_FRIEND:
                        mAbstractVkListActivity.mDataManager.addFriend(mAbstractVkListActivity.mArgs.getParcelable(friendToAdd), new Listener<Integer>() {
                            @Override
                            public void onCompleted(Integer integer) {
                                switch (integer) {
                                    case 1:
                                        mAbstractVkListActivity.showSnackbar(R.string.friend_request_sent_successfully, Snackbar.LENGTH_LONG);
                                        break;
                                    case 2:
                                        mAbstractVkListActivity.showSnackbar(R.string.friend_added_successfully);
                                        break;
                                }
                                mAbstractVkListActivity.notifyDataSetChanged();
                            }

                            @Override
                            public void onError(String e) {
                                mAbstractVkListActivity.showSnackbar(R.string.friend_adding_error);
                            }
                        });
                        break;
                    case REQUEST_LEAVE_GROUP:
                        mAbstractVkListActivity.mDataManager.leaveGroup(mAbstractVkListActivity.mArgs.getInt(groupToLeaveId), new Listener<Void>() {
                            @Override
                            public void onCompleted(Void aVoid) {
                                mAbstractVkListActivity.showSnackbar(R.string.group_left_successfully);
                                mAbstractVkListActivity.notifyDataSetChanged();
                            }

                            @Override
                            public void onError(String e) {
                                mAbstractVkListActivity.showSnackbar(R.string.group_leaving_error);
                            }
                        });
                        break;
                    case REQUEST_JOIN_GROUP:
                        mAbstractVkListActivity.mDataManager.joinGroup(mAbstractVkListActivity.mArgs.getParcelable(groupToJoin), new Listener<Integer>() {
                            @Override
                            public void onCompleted(Integer integer) {
                                switch (integer) {
                                    case 1:
                                        mAbstractVkListActivity.showSnackbar(R.string.group_request_sent_successfully, Snackbar.LENGTH_LONG);
                                        break;
                                    case 2:
                                        mAbstractVkListActivity.showSnackbar(R.string.group_join_successfully);
                                        break;
                                }
                                mAbstractVkListActivity.notifyDataSetChanged();
                            }

                            @Override
                            public void onError(String e) {
                                mAbstractVkListActivity.showSnackbar(R.string.group_joining_error);
                            }
                        });
                        break;
                }
            }
        }
    }

}
