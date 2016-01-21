package com.alex.vkcommonpublics;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    /**
     * Кол-во фото загружаемое за 1 раз.
     */
    private static final int PHOTO_FETCH_PER_TIME = 15;

    private DataManager mDataManager = DataManager.get();
    private PhotoManager mPhotoManager;
    private VKApiCommunityArray mGroups;
    private GroupAdapter mGroupsAdapter;

    /**
     * До какого фото выполнена загрузка.
     */
    private int mPhotoFetchingBound = 0;

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
        final ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mDataManager.getFetchingState() == DataManager.FetchingState.finished) {
                    Intent intent = new Intent(getActivity(), FriendsListActivity.class);
                    VKApiCommunityFull group = (VKApiCommunityFull) listView.getAdapter().getItem(position);
                    intent.putExtra(FriendsListActivity.EXTRA_GROUP_ID, group.id);
                    startActivity(intent);
                }
            }
        });

        TextView no_commons_text_view = (TextView) view.findViewById(R.id.empty_list);
        no_commons_text_view.setText(R.string.no_common_groups);

        if (mGroups == null || mGroups.isEmpty()) {
            listView.setVisibility(View.INVISIBLE);
        }
        else {
            no_commons_text_view.setVisibility(View.INVISIBLE);
            mGroupsAdapter = new GroupAdapter(mGroups);
            listView.setAdapter(mGroupsAdapter);

            // загружаем первую порцию фото.
            fetchElsePhotos();
        }
        return view;
    }

    /**
     * Загрузить еще одну порцию фото.
     */
    private void fetchElsePhotos() {
        mPhotoManager.fetchGroupsPhotos(mGroups, mPhotoFetchingBound, PHOTO_FETCH_PER_TIME, mPhotoFetchingListener);
        mPhotoFetchingBound += PHOTO_FETCH_PER_TIME;
    }

    /**
     * Слушатель загрузки фото.
     */
    private Listener<Bitmap> mPhotoFetchingListener = new Listener<Bitmap>() {
        @Override
        public void onCompleted(Bitmap bitmap) {
            notifyDataSetChanged();
        }

        @Override
        public void onError(String e) {
            Log.e("AASSDD", e);
        }
    };

    /**
     * Обновить адаптер ListView.
     */
    public void notifyDataSetChanged() {
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
            }
            VKApiCommunityFull group = getItem(position);

            final ImageView photoImageView = (ImageView) convertView.findViewById(R.id.photoImageView);
            Bitmap photoBitmap = mPhotoManager.getPhoto(group.photo_100);
            if (photoBitmap != null) {
                photoImageView.setImageBitmap(photoBitmap);
            }
            else {
                photoImageView.setImageResource(R.drawable.community_100);
            }

            // Если пролистали до нужного места, загружаем новую порцию фото.
            if (position == mPhotoFetchingBound - PHOTO_FETCH_PER_TIME /2.5) {
                fetchElsePhotos();
            }

            TextView titleTextView = (TextView) convertView.findViewById(R.id.item_title);
            titleTextView.setText(group.name);
            TextView friendsTextView = (TextView) convertView.findViewById(R.id.common_count);
            friendsTextView.setText(getString(R.string.friends, mDataManager.getFriendsInGroup(group).size()));
            return convertView;
        }
    }

}