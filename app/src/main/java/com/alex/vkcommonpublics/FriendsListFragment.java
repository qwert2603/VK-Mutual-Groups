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
                if (mDataManager.getFetchingState() == DataManager.FetchingState.finished) {
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
        /**
         * Ссылка на фото последнего друга.
         */
        private String mLastUserPhotoUrl;

        public FriendAdapter(VKUsersArray users) {
            super(getActivity(),0, users);
            mLastUserPhotoUrl = users.get(users.size() - 1).photo_100;
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
                mPhotoManager.fetchOnePhoto(friend.photo_100, mFetchingListener);
            }

            TextView nameTextView = (TextView) convertView.findViewById(R.id.item_title);
            nameTextView.setText(getString(R.string.friend_name, friend.first_name, friend.last_name));
            TextView commonTextView = (TextView) convertView.findViewById(R.id.common_count);
            commonTextView.setText(getString(R.string.commons, mDataManager.getGroupsCommonWithFriend(friend).size()));
            return convertView;
        }

        private Listener<Bitmap> mFetchingListener = new Listener<Bitmap>() {
            private int i = 0;

            @Override
            public void onCompleted(Bitmap bitmap) {
                // FIXME: 19.01.2016 
                // Чтобы не обновлять адаптер слишком часто, но точно обновить его после загрузки последнего фото.
                /*++i;
                if (i == 8 || bitmap == mPhotoManager.getPhoto(mLastUserPhotoUrl)) {
                    i = 0;*/
                    notifyDataSetChanged();
                //}
            }

            @Override
            public void onError(String e) {
                Log.e("AASSDD", e);
            }
        };
    }

}