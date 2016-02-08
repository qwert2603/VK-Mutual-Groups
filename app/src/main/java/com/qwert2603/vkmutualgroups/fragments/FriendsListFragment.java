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
import android.widget.ImageView;
import android.widget.TextView;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.activities.GroupsListActivity;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.photo.ImageViewHolder;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKUsersArray;

import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.calculatingMutual;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.finished;

/**
 * Отображает список друзей из DataManager в соответствии с id группы, переданным в {@link #newInstance(int)}.
 */
public class FriendsListFragment extends AbstractVkListFragment<VKApiUserFull> {

    @SuppressWarnings("unused")
    private static final String TAG = "FriendsListFragment";

    private static final String groupIdKey = "groupIdKey";

    /**
     * groupId - id группы. В списке будут выведены друзья в этой группе.
     * Если groupId == 0, будут выведены друзья пользователя в текущем порядке сортировки из mDataManager.
     */
    public static FriendsListFragment newInstance(int groupId) {
        FriendsListFragment result = new FriendsListFragment();
        Bundle args = new Bundle();
        args.putInt(groupIdKey, groupId);
        result.setArguments(args);
        return result;
    }

    private DataManager mDataManager;
    private PhotoManager mPhotoManager;

    private VKUsersArray mFriends;
    private int mGroupId;

    private FriendAdapter mFriendAdapter;

    @Override
    protected String getEmptyListText() {
        return getString(mGroupId == 0 ? R.string.no_friends : R.string.no_friends_in_group);
    }

    @Override
    protected int getItemsCount() {
        return mFriends.size();
    }

    @Override
    protected String getPhotoURL(int index) {
        return mFriends.get(index).photo_50;
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
        mGroupId = getArguments().getInt(groupIdKey);

        if (mGroupId != 0) {
            VKApiCommunityFull group = mDataManager.getGroupById(mGroupId);
            mFriends = mDataManager.getFriendsInGroup(group);
        }
        else {
            mFriends = mDataManager.getUsersFriends();
        }

        if (mFriends == null) {
            mFriends = new VKUsersArray();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mListView.setOnItemClickListener((parent, view1, position, id) -> {
            if (mDataManager.getFetchingState() == finished) {
                Intent intent = new Intent(getActivity(), GroupsListActivity.class);
                VKApiUserFull friend = (VKApiUserFull) mListView.getAdapter().getItem(position);
                intent.putExtra(GroupsListActivity.EXTRA_FRIEND_ID, friend.id);
                startActivity(intent);
            }
        });
        mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            private int mActionedPosition;

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                // чтобы выделялось не более 1 друга.
                if (mListView.getCheckedItemCount() > 1) {
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
                switch (item.getItemId()) {
                    case R.id.menu_message:
                        int friendId = mFriends.get(mActionedPosition).id;
                        sendMessage(friendId);
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

        mFriendAdapter = new FriendAdapter(mFriends);
        mListView.setAdapter(mFriendAdapter);

        mActionButton.setVisibility(View.INVISIBLE);

        return view;
    }

    private class FriendAdapter extends ArrayAdapter<VKApiUserFull> {
        public FriendAdapter(VKUsersArray users) {
            super(getActivity(),0, users);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item, parent, false);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.mPhotoImageView = (ImageView) convertView.findViewById(R.id.photoImageView);
                viewHolder.mTitleTextView = (TextView) convertView.findViewById(R.id.item_title);
                viewHolder.mMutualsTextView = (TextView) convertView.findViewById(R.id.common_count);
                convertView.setTag(viewHolder);
            }

            VKApiUserFull friend = getItem(position);
            ViewHolder viewHolder = (ViewHolder) convertView.getTag();

            viewHolder.mPosition = position;
            if (mPhotoManager.getPhoto(getPhotoURL(position)) != null) {
                viewHolder.mPhotoImageView.setImageBitmap(mPhotoManager.getPhoto(getPhotoURL(position)));
            }
            else {
                viewHolder.mPhotoImageView.setImageBitmap(null);
                //mPhotoManager.setPhotoToImageViewHolder(viewHolder, getPhotoURL(position));
            }

            viewHolder.mTitleTextView.setText(getString(R.string.friend_name, friend.first_name, friend.last_name));

            if (mDataManager.getFetchingState() == calculatingMutual || mDataManager.getFetchingState() == finished) {
                VKApiCommunityArray groups = mDataManager.getGroupsMutualWithFriend(friend);
                if (groups != null) {
                    viewHolder.mMutualsTextView.setText(getString(R.string.mutual, groups.size()));
                } else {
                    viewHolder.mMutualsTextView.setText("");
                }
            }
            else {
                viewHolder.mMutualsTextView.setText("");
            }

            return convertView;
        }
    }

    private static class ViewHolder implements ImageViewHolder {
        int mPosition;
        ImageView mPhotoImageView;
        TextView mTitleTextView;
        TextView mMutualsTextView;

        @Override
        public int getPosition() {
            return mPosition;
        }

        @Override
        public ImageView getImageView() {
            return mPhotoImageView;
        }
    }

}
