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

    /**
     * Кол-во фото загружаемое за 1 раз.
     */
    private static final int PHOTO_FETCH_PER_TIME = 15;

    private DataManager mDataManager = DataManager.get();
    private PhotoManager mPhotoManager = PhotoManager.get();
    private FriendAdapter mFriendAdapter = null;
    private VKUsersArray mFriends;
    private int mPhotoFetchingBound = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
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
        final ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mDataManager.getFetchingState() == finished) {
                    Intent intent = new Intent(getActivity(), GroupsListActivity.class);
                    VKApiUserFull friend = (VKApiUserFull) listView.getAdapter().getItem(position);
                    intent.putExtra(GroupsListActivity.EXTRA_FRIEND_ID, friend.id);
                    startActivity(intent);
                }
            }
        });

        TextView no_friends_text_view = (TextView) view.findViewById(R.id.empty_list);
        no_friends_text_view.setText(R.string.no_friends_in_group);

        if (mFriends == null || mFriends.isEmpty()) {
            listView.setVisibility(View.INVISIBLE);
        }
        else {
            no_friends_text_view.setVisibility(View.INVISIBLE);
            mFriendAdapter = new FriendAdapter(mFriends);
            listView.setAdapter(mFriendAdapter);

            // загружаем первую порцию фото.
            fetchElsePhotos();
        }
        return view;
    }

    /**
     * Загрузить еще одну порцию фото.
     */
    private void fetchElsePhotos() {
        mPhotoManager.fetchFriendsPhotos(mFriends, mPhotoFetchingBound, PHOTO_FETCH_PER_TIME, mPhotoFetchingListener);
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
            }

            // Если пролистали до нужного места, загружаем новую порцию фото.
            if (position == mPhotoFetchingBound - PHOTO_FETCH_PER_TIME /2.5) {
                fetchElsePhotos();
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
    }

}