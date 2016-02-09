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
import com.qwert2603.vkmutualgroups.activities.BaseVkActivity;
import com.qwert2603.vkmutualgroups.activities.FriendsListActivity;
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
 * Отображает список общих групп из DataManager в соответствии с id пользователя, переданным в {@link #newInstance(int)}
 */
public class GroupsListFragment extends AbstractVkListFragment<VKApiCommunityFull> {

    @SuppressWarnings("unused")
    private static final String TAG = "GroupsListFragment";

    private static final String friendIdKey = "friendIdKey";

    /**
     * friendId - id друга. В списке будут выведены группы, общие с этим другом.
     * Если friendId == 0, будут выведены группы пользователя в текущем порядке сортировки из mDataManager.
     */
    public static GroupsListFragment newInstance(int friendId) {
        GroupsListFragment result = new GroupsListFragment();
        Bundle args = new Bundle();
        args.putInt(friendIdKey, friendId);
        result.setArguments(args);
        return result;
    }

    private DataManager mDataManager;
    private PhotoManager mPhotoManager;

    private VKApiCommunityArray mGroups;
    private int mFriendId;

    private GroupAdapter mGroupsAdapter;

    @Override
    protected String getEmptyListText() {
        return getString(mFriendId == 0 ? R.string.no_groups : R.string.no_mutual_groups);
    }

    @Override
    protected int getItemsCount() {
        return mGroups.size();
    }

    @Override
    protected String getPhotoURL(int index) {
        return mGroups.get(index).photo_50;
    }

    @Override
    protected ArrayAdapter<VKApiCommunityFull> getListAdapter() {
        return mGroupsAdapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mDataManager = DataManager.get(getActivity());
        mPhotoManager = PhotoManager.get(getActivity());
        mFriendId = getArguments().getInt(friendIdKey);

        if (mFriendId != 0) {
            VKApiUserFull friend = mDataManager.getFriendById(mFriendId);
            mGroups = mDataManager.getGroupsMutualWithFriend(friend);
        }
        else {
            mGroups = mDataManager.getUsersGroups();
        }

        if (mGroups == null) {
            mGroups = new VKApiCommunityArray();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mListView.setOnItemClickListener((parent, view1, position, id) -> {
            if (mDataManager.getFetchingState() == DataManager.FetchingState.finished) {
                Intent intent = new Intent(getActivity(), FriendsListActivity.class);
                VKApiCommunityFull group = (VKApiCommunityFull) mListView.getAdapter().getItem(position);
                intent.putExtra(FriendsListActivity.EXTRA_GROUP_ID, group.id);
                startActivity(intent);
            }
        });
        mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            private int mActionedPosition;

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                // чтобы выделялось не более 1 группы.
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
                mode.getMenuInflater().inflate(R.menu.groups_list_item_action_mode, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                int groupId = mGroups.get(mActionedPosition).id;
                switch (item.getItemId()) {
                    case R.id.menu_leave_group:
                        ((BaseVkActivity) getActivity()).leaveGroup(groupId);
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

        mGroupsAdapter = new GroupAdapter(mGroups);
        mListView.setAdapter(mGroupsAdapter);

        mActionButton.setIcon(R.drawable.message);
        mActionButton.setOnClickListener((v) -> ((BaseVkActivity) getActivity()).sendMessage(mFriendId));

        return view;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

        boolean b = mFriendId != 0 && mDataManager.getFriendById(mFriendId) != null && mDataManager.getFetchingState() == finished;
        mActionButton.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
    }

    private class GroupAdapter extends ArrayAdapter<VKApiCommunityFull> {
        public GroupAdapter(VKApiCommunityArray groups) {
            super(getActivity(), 0, groups);
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

            VKApiCommunityFull group = getItem(position);
            ViewHolder viewHolder = (ViewHolder) convertView.getTag();

            viewHolder.mPosition = position;
            if (mPhotoManager.getPhoto(getPhotoURL(position)) != null) {
                viewHolder.mPhotoImageView.setImageBitmap(mPhotoManager.getPhoto(getPhotoURL(position)));
            }
            else {
                viewHolder.mPhotoImageView.setImageBitmap(null);
                //mPhotoManager.setPhotoToImageViewHolder(viewHolder, getPhotoURL(position));
            }

            viewHolder.mTitleTextView.setText(group.name);

            if (mDataManager.getFetchingState() == calculatingMutual || mDataManager.getFetchingState() == finished) {
                VKUsersArray friends = mDataManager.getFriendsInGroup(group);
                if (friends != null) {
                    viewHolder.mMutualsTextView.setText(getString(R.string.friends, friends.size()));
                } else {
                    viewHolder.mMutualsTextView.setText("");
                }
            } else {
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
