package com.qwert2603.vkmutualgroups.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.qwert2603.vkmutualgroups.R;
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
 * Отображает список групп из DataManager в соответствии с id пользователя, переданным в {@link #newInstance(int)}
 */
public class GroupsListFragment extends AbstractVkListFragment<VKApiCommunityFull> {

    @SuppressWarnings("unused")
    private static final String TAG = "GroupsListFragment";

    private static final String friendIdKey = "friendIdKey";

    /**
     * friendId - id друга. В списке будут выведены группы, общие с этим другом.
     * Если friendId == 0, будут выведены группы позователя.
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
        setHasOptionsMenu(true);
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

        mGroupsAdapter = new GroupAdapter(mGroups);
        mListView.setAdapter(mGroupsAdapter);


        mActionButton.setIcon(R.drawable.message);
        mActionButton.setOnClickListener((v) -> sendMessage(mFriendId));
        if (mFriendId == 0 || mDataManager.getFetchingState() != finished) {
            mActionButton.setVisibility(View.INVISIBLE);
        }

        return view;
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
