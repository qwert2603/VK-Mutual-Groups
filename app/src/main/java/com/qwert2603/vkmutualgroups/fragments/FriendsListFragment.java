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
import android.widget.ArrayAdapter;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.activities.AbstractVkListActivity;
import com.qwert2603.vkmutualgroups.activities.FriendGroupsListActivity;
import com.qwert2603.vkmutualgroups.activities.MutualGroupsListActivity;
import com.qwert2603.vkmutualgroups.adapters.FriendAdapter;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKUsersArray;

import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.finished;

/**
 * Отображает список друзей, переданный в {@link #newInstance(VKUsersArray)}.
 */
public class FriendsListFragment extends AbstractVkListFragment<VKApiUserFull> {

    @SuppressWarnings("unused")
    private static final String TAG = "FriendsListFragment";

    private static final String friendsKey = "friendsKey";

    public static FriendsListFragment newInstance(VKUsersArray friends) {
        FriendsListFragment result = new FriendsListFragment();
        Bundle args = new Bundle();
        args.putParcelable(friendsKey, friends);
        result.setArguments(args);
        return result;
    }

    private DataManager mDataManager;
    private PhotoManager mPhotoManager;

    private VKUsersArray mFriends;

    private FriendAdapter mFriendAdapter;

    @Override
    protected String getEmptyListText() {
        return getString(R.string.no_friends);
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
    protected ArrayAdapter<VKApiUserFull> getListAdapter() {
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

        mListView.setOnItemClickListener((parent, view1, position, id) -> {
            if (mDataManager.getFetchingState() == finished) {
                VKApiUserFull friend = (VKApiUserFull) mListView.getAdapter().getItem(position);
                if (mDataManager.getUsersFriendById(friend.id) != null) {
                    Intent intent = new Intent(getActivity(), MutualGroupsListActivity.class);
                    intent.putExtra(MutualGroupsListActivity.EXTRA_FRIEND, friend);
                    startActivity(intent);
                }
            }
        });
        mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            private int mActionedPosition;

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                // чтобы выделялось не более 1 друга.
                // И чтобы не выделялись те люди, которых нет в друзьях.
                if (mListView.getCheckedItemCount() > 1 || mDataManager.getUsersFriendById(mFriends.get(position).id) == null) {
                    mode.finish();
                } else {
                    mActionedPosition = position;
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                if (mDataManager.getFetchingState() != finished) {
                    mode.finish();
                    return false;
                }
                mode.getMenuInflater().inflate(R.menu.friends_list_item_action_mode, menu);
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
                    case R.id.menu_delete_friend:
                        ((AbstractVkListActivity) getActivity()).deleteFriend(friend);
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
        mListView.setAdapter(mFriendAdapter);

        return view;
    }

}
