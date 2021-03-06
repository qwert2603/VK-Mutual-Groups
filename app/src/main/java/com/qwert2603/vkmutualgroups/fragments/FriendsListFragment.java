package com.qwert2603.vkmutualgroups.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.activities.vk_list_activities.AbstractVkListActivity;
import com.qwert2603.vkmutualgroups.activities.vk_list_activities.FriendGroupsListActivity;
import com.qwert2603.vkmutualgroups.activities.vk_list_activities.MutualGroupsListActivity;
import com.qwert2603.vkmutualgroups.adapters.AbstractAdapter;
import com.qwert2603.vkmutualgroups.adapters.FriendAdapter;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKUsersArray;

import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.finished;

/**
 * Отображает список друзей, переданный в {@link #newInstance(VKUsersArray, String)}.
 */
public class FriendsListFragment extends AbstractVkListFragment<VKApiUserFull> {

    @SuppressWarnings("unused")
    private static final String TAG = "FriendsListFragment";

    private static final String friendsKey = "friendsKey";
    private static final String emptyListTextKey = "emptyListTextKey";

    public static FriendsListFragment newInstance(VKUsersArray friends, String emptyListText) {
        FriendsListFragment result = new FriendsListFragment();
        Bundle args = new Bundle();
        args.putParcelable(friendsKey, friends);
        args.putString(emptyListTextKey, emptyListText);
        result.setArguments(args);
        return result;
    }

    private DataManager mDataManager;
    private PhotoManager mPhotoManager;

    private VKUsersArray mFriends;

    private FriendAdapter mFriendAdapter;

    @Override
    protected String getEmptyListText() {
        return getArguments().getString(emptyListTextKey);
    }

    @Override
    protected int getItemsCount() {
        return mFriends.size();
    }

    @Override
    protected String getPhotoURL(int index) {
        return mPhotoManager.getUserPhotoUrl(mFriends.get(index));
    }

    @Override
    protected AbstractAdapter<VKApiUserFull> getListAdapter() {
        return mFriendAdapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mDataManager = DataManager.get(getActivity());
        mPhotoManager = PhotoManager.get(getActivity());

        mFriends = getArguments().getParcelable(friendsKey);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setListViewOnItemClickListener((parent, view1, position, id) -> {
            if (mDataManager.getFetchingState() == finished) {
                VKApiUserFull friend = mFriends.get(position);
                if (mDataManager.getUsersFriendById(friend.id) != null) {
                    Intent intent = new Intent(getActivity(), MutualGroupsListActivity.class);
                    intent.putExtra(MutualGroupsListActivity.EXTRA_FRIEND, friend);
                    startActivity(intent);
                }
            }
        });
        setListViewChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        setListViewMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            private int mActionedPosition;

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                // чтобы выделялось не более 1 друга.
                if (getListViewCheckedItemCount() > 1) {
                    mode.finish();
                } else {
                    mActionedPosition = position;

                    if (mDataManager.getUsersFriendById(mFriends.get(mActionedPosition).id) != null) {
                        mode.getMenu().findItem(R.id.menu_add_friend).setVisible(false);
                    } else {
                        mode.getMenu().findItem(R.id.menu_delete_friend).setVisible(false);
                    }
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                if (mDataManager.getFetchingState() != finished) {
                    mode.finish();
                    return false;
                }
                mode.getMenuInflater().inflate(R.menu.one_friend, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                VKApiUserFull friend = mFriends.get(mActionedPosition);
                switch (item.getItemId()) {
                    case R.id.menu_message:
                        ((AbstractVkListActivity) getActivity()).sendMessage(friend.id);
                        mode.finish();
                        return true;
                    case R.id.menu_view_groups:
                        Intent intent = new Intent(getActivity(), FriendGroupsListActivity.class);
                        intent.putExtra(FriendGroupsListActivity.EXTRA_FRIEND, friend);
                        startActivity(intent);
                        mode.finish();
                        return true;
                    case R.id.menu_open_in_browser:
                        ((AbstractVkListActivity) getActivity()).navigateTo("http://vk.com/" + friend.screen_name);
                        return true;
                    case R.id.menu_delete_friend:
                        ((AbstractVkListActivity) getActivity()).deleteFriend(friend);
                        mode.finish();
                        return true;
                    case R.id.menu_add_friend:
                        ((AbstractVkListActivity) getActivity()).addFriend(friend);
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        });

        mFriendAdapter = new FriendAdapter(getActivity(), mFriends);
        setListViewAdapter(mFriendAdapter);

        return view;
    }

}
