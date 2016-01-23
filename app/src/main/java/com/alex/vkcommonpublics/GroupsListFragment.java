package com.alex.vkcommonpublics;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
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

import java.util.ArrayList;

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
                    fetchVisibleFriendsPhotos(4);
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
     * Загрузить фото для групп отображаемых сейчас.
     * @param padding - для скольких друзей сверху и снизу в списке будет также загружено фото.
     */
    private void fetchVisibleFriendsPhotos(int padding) {
        int b = mListView.getFirstVisiblePosition();
        int e = mListView.getLastVisiblePosition();
        fetchFriendsPhotos(b, e, new Listener() {
            @Override
            public void onCompleted() {
                notifyDataSetChanged();
            }

            @Override
            public void onError(String e) {
                Log.e(TAG, e);
            }
        });

        Listener noUpdateListener = new Listener() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(String e) {
                Log.e(TAG, e);
            }
        };
        if (padding > 0) {
            fetchFriendsPhotos(e, Math.min(e + padding, mGroups.size()), noUpdateListener);
            fetchFriendsPhotos(Math.max(b - padding, 0), b, noUpdateListener);
        }
    }

    /**
     * Загрузить фото групп с 'b' по 'е'.
     * Будут загружены только те фото, что не были загружены ранее.
     */
    private void fetchFriendsPhotos(int b, int e, Listener listener) {
        ArrayList<String> urlsArrayList = new ArrayList<>();
        for (int i = b; i < e; ++i) {
            if (mPhotoManager.getPhoto(mGroups.get(i).photo_100) == null) {
                urlsArrayList.add(mGroups.get(i).photo_100);
            }
        }
        String[] urls = new String[urlsArrayList.size()];
        urlsArrayList.toArray(urls);
        mPhotoManager.fetchPhotos(urls, listener);
    }

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

            viewHolder.mPhotoImageView.setImageBitmap(mPhotoManager.getPhoto(group.photo_100));
            viewHolder.mTitleTextView.setText(group.name);
            viewHolder.mCommonsTextView.setText(getString(R.string.friends, mDataManager.getFriendsInGroup(group).size()));

            return convertView;
        }
    }

    private static class ViewHolder {
        ImageView mPhotoImageView;
        TextView mTitleTextView;
        TextView mCommonsTextView;
    }

}