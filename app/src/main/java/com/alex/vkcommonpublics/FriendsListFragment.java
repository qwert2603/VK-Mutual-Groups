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

import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKUsersArray;

import static com.alex.vkcommonpublics.DataManager.FetchingState.calculatingCommons;
import static com.alex.vkcommonpublics.DataManager.FetchingState.finished;

/**
 * Отображает список друзей из DataManager в соответствии с id группы, переданным в {@link #newInstance(int)}.
 */
public class FriendsListFragment extends Fragment {

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

    private int mGroupId;
    private DataManager mDataManager = DataManager.get();
    private PhotoManager mPhotoManager = PhotoManager.get();
    private ListView mListView;
    private FriendAdapter mFriendAdapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mGroupId = getArguments().getInt(groupIdKey);
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

        TextView no_friends_text_view = (TextView) view.findViewById(R.id.empty_list);
        no_friends_text_view.setText(R.string.no_friends_in_group);

        VKUsersArray users;
        if (mGroupId != 0) {
            VKApiCommunityFull group = mDataManager.getGroupById(mGroupId);
            users = mDataManager.getFriendsInGroup(group);
        }
        else {
            users =  mDataManager.getUsersFriends();
        }

        if (users == null || users.isEmpty()) {
            mListView.setVisibility(View.INVISIBLE);
        }
        else {
            no_friends_text_view.setVisibility(View.INVISIBLE);
            mFriendAdapter = new FriendAdapter(users);
            mListView.setAdapter(mFriendAdapter);

            // загружаем фото первых 256 друзей.
            Listener<Bitmap> bitmapListener = new Listener<Bitmap>() {
                @Override
                public void onCompleted(Bitmap bitmap) {
                    // nth
                }

                @Override
                public void onError(String e) {
                    Log.e("AASSDD", e);
                }
            };
            int count = Math.min(256, users.size());
            for (int i = 0; i < count; ++i) {
                VKApiUserFull friend = users.get(i);
                if (mPhotoManager.getPhoto(friend.photo_100) == null) {
                    mPhotoManager.fetchPhoto(friend.photo_100, bitmapListener);
                }
            }
        }
        return view;
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
            }
            VKApiUserFull friend = getItem(position);

            final ImageView photoImageView = (ImageView) convertView.findViewById(R.id.photoImageView);
            Bitmap photoBitmap = mPhotoManager.getPhoto(friend.photo_100);
            if (photoBitmap != null) {
                photoImageView.setImageBitmap(photoBitmap);
            }
            else {
                photoImageView.setImageResource(R.drawable.camera_100);
                mPhotoManager.fetchPhoto(friend.photo_100, mPhotoFetchingListener);
            }

            TextView nameTextView = (TextView) convertView.findViewById(R.id.item_title);
            nameTextView.setText(getString(R.string.friend_name, friend.first_name, friend.last_name));
            TextView commonTextView = (TextView) convertView.findViewById(R.id.common_count);
            if (mDataManager.getFetchingState() == calculatingCommons || mDataManager.getFetchingState() == finished) {
                commonTextView.setText(getString(R.string.commons, mDataManager.getGroupsCommonWithFriend(friend).size()));
            }
            else {
                commonTextView.setText("");
            }
            return convertView;
        }

        Listener<Bitmap> mPhotoFetchingListener = new Listener<Bitmap>() {
            @Override
            public void onCompleted(Bitmap bitmap) {
                notifyDataSetChanged();
            }

            @Override
            public void onError(String e) {
                Log.e("AASSDD", e);
            }
        };
    }

}