package com.alex.vkcommonpublics;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiUserFull;

/**
 * Отображает список групп из DataManager в соответствии с id пользователя, переданным в {@link #newInstance(int)}
 */
public class GroupsListFragment extends Fragment {

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

    private DataManager mDataManager = DataManager.get();
    private PhotoManager mPhotoManager;
    private VKApiCommunityArray mGroups;
    private ListView mListView;
    private GroupAdapter mGroupsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mPhotoManager = PhotoManager.get(getActivity());
        int friendId = getArguments().getInt(friendIdKey);
        if (friendId != 0) {
            VKApiUserFull friend = mDataManager.getFriendById(friendId);
            mGroups = mDataManager.getGroupsCommonWithFriend(friend);
        }
        else {
            mGroups = mDataManager.getUsersGroups();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mDataManager.getFetchingState() == DataManager.FetchingState.finished) {
                    Intent intent = new Intent(getActivity(), FriendsListActivity.class);
                    VKApiCommunityFull group = (VKApiCommunityFull) mListView.getAdapter().getItem(position);
                    intent.putExtra(FriendsListActivity.EXTRA_GROUP_ID, group.id);
                    startActivity(intent);
                }
            }
        });
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    // загружаем фото для групп, ближайших к отображаемым.
                    int padding = 3;
                    int b = mListView.getFirstVisiblePosition();
                    int e = mListView.getLastVisiblePosition() + 1;
                    int pb = Math.max(0, b - padding);
                    int pe = Math.min(mGroups.size(), e + padding);
                    for (int i = e; i < pe; ++i) {
                        mPhotoManager.fetchPhoto(mGroups.get(i).photo_50, null);
                    }
                    for (int i = pb; i < b; ++i) {
                        mPhotoManager.fetchPhoto(mGroups.get(i).photo_50, null);
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        TextView no_commons_text_view = (TextView) view.findViewById(R.id.empty_list);
        no_commons_text_view.setText(R.string.no_common_groups);

        if (mGroups == null || mGroups.isEmpty()) {
            mListView.setVisibility(View.INVISIBLE);
        }
        else {
            no_commons_text_view.setVisibility(View.INVISIBLE);
            mGroupsAdapter = new GroupAdapter(mGroups);
            mListView.setAdapter(mGroupsAdapter);
        }
        return view;
    }

    /**
     * Обновить адаптер ListView.
     */
    @SuppressWarnings("unused")
    private void notifyDataSetChanged() {
        if (mGroupsAdapter != null) {
            mGroupsAdapter.notifyDataSetChanged();
        }
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
                viewHolder.mCommonsTextView = (TextView) convertView.findViewById(R.id.common_count);
                convertView.setTag(viewHolder);
            }

            VKApiCommunityFull group = getItem(position);
            ViewHolder viewHolder = (ViewHolder) convertView.getTag();

            viewHolder.mPosition = position;
            if (mPhotoManager.getPhoto(group.photo_50) != null) {
                viewHolder.mPhotoImageView.setImageBitmap(mPhotoManager.getPhoto(group.photo_50));
            }
            else {
                viewHolder.mPhotoImageView.setImageBitmap(null);
                mPhotoManager.setPhotoToImageViewHolder(viewHolder, group.photo_50);
            }

            viewHolder.mTitleTextView.setText(group.name);
            viewHolder.mCommonsTextView.setText(getString(R.string.friends, mDataManager.getFriendsInGroup(group).size()));

            return convertView;
        }
    }

    private static class ViewHolder implements ImageViewHolder {
        int mPosition;
        ImageView mPhotoImageView;
        TextView mTitleTextView;
        TextView mCommonsTextView;

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