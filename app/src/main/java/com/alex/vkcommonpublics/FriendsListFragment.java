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

import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKUsersArray;

import java.util.ArrayList;

import static com.alex.vkcommonpublics.DataManager.FetchingState.calculatingCommons;
import static com.alex.vkcommonpublics.DataManager.FetchingState.finished;

/**
 * Отображает список друзей из DataManager в соответствии с id группы, переданным в {@link #newInstance(int)}.
 */
public class FriendsListFragment extends Fragment {

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

    private DataManager mDataManager = DataManager.get();
    private PhotoManager mPhotoManager;
    private ListView mListView;
    private FriendAdapter mFriendAdapter = null;
    private VKUsersArray mFriends;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mPhotoManager = PhotoManager.get(getActivity());
        int groupId = getArguments().getInt(groupIdKey);
        if (groupId != 0) {
            VKApiCommunityFull group = mDataManager.getGroupById(groupId);
            mFriends = mDataManager.getFriendsInGroup(group);
        }
        else {
            mFriends =  mDataManager.getUsersFriends();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mDataManager.getFetchingState() == finished) {
                    Intent intent = new Intent(getActivity(), GroupsListActivity.class);
                    VKApiUserFull friend = (VKApiUserFull) mListView.getAdapter().getItem(position);
                    intent.putExtra(GroupsListActivity.EXTRA_FRIEND_ID, friend.id);
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

        TextView no_friends_text_view = (TextView) view.findViewById(R.id.empty_list);
        no_friends_text_view.setText(R.string.no_friends_in_group);

        if (mFriends == null || mFriends.isEmpty()) {
            mListView.setVisibility(View.INVISIBLE);
        }
        else {
            no_friends_text_view.setVisibility(View.INVISIBLE);
            mFriendAdapter = new FriendAdapter(mFriends);
            mListView.setAdapter(mFriendAdapter);
        }
        return view;
    }


    /**
     * Загрузить фото для друзей отображаемых сейчас.
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
            fetchFriendsPhotos(e, Math.min(e + padding, mFriends.size()), noUpdateListener);
            fetchFriendsPhotos(Math.max(b - padding, 0), b, noUpdateListener);
        }
    }

    /**
     * Загрузить фото друзей с 'b' по 'е'.
     * Будут загружены только те фото, что не были загружены ранее.
     */
    private void fetchFriendsPhotos(int b, int e, Listener listener) {
        ArrayList<String> urlsArrayList = new ArrayList<>();
        for (int i = b; i < e; ++i) {
            if (mPhotoManager.getPhoto(mFriends.get(i).photo_100) == null) {
                urlsArrayList.add(mFriends.get(i).photo_100);
            }
        }
        String[] urls = new String[urlsArrayList.size()];
        urlsArrayList.toArray(urls);
        mPhotoManager.fetchPhotos(urls, listener);
    }

    /**
     * Обновить адаптер ListView.
     */
    public void notifyDataSetChanged() {
        if (mFriendAdapter != null) {
            mFriendAdapter.notifyDataSetChanged();
        }
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
                viewHolder.mCommonsTextView = (TextView) convertView.findViewById(R.id.common_count);
                convertView.setTag(viewHolder);
            }

            VKApiUserFull friend = getItem(position);
            ViewHolder viewHolder = (ViewHolder) convertView.getTag();

            viewHolder.mPhotoImageView.setImageBitmap(mPhotoManager.getPhoto(friend.photo_100));
            viewHolder.mTitleTextView.setText(getString(R.string.friend_name, friend.first_name, friend.last_name));
            if (mDataManager.getFetchingState() == calculatingCommons || mDataManager.getFetchingState() == finished) {
                int commons = mDataManager.getGroupsCommonWithFriend(friend).size();
                viewHolder.mCommonsTextView.setText(getString(R.string.commons, commons));
            }
            else {
                viewHolder.mCommonsTextView.setText("");
            }

            return convertView;
        }
    }

    private static class ViewHolder {
        ImageView mPhotoImageView;
        TextView mTitleTextView;
        TextView mCommonsTextView;
    }

}