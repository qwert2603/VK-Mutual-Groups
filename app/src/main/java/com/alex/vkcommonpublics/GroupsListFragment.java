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

    private int mFriendId;
    private DataManager mDataManager = DataManager.get();
    private PhotoManager mPhotoManager = PhotoManager.get();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mFriendId = getArguments().getInt(friendIdKey);
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

        VKApiCommunityArray groups;
        if (mFriendId != 0) {
            VKApiUserFull friend = mDataManager.getFriendById(mFriendId);
            groups = mDataManager.getGroupsCommonWithFriend(friend);
        }
        else {
            groups = mDataManager.getUsersGroups();
        }

        if (groups == null || groups.isEmpty()) {
            listView.setVisibility(View.INVISIBLE);
        }
        else {
            no_commons_text_view.setVisibility(View.INVISIBLE);
            GroupAdapter adapter = new GroupAdapter(groups);
            listView.setAdapter(adapter);
        }
        return view;
    }

    private class GroupAdapter extends ArrayAdapter<VKApiCommunityFull> {
        /**
         * Ссылка на фото последней группы.
         */
        private String mLastGroupPhotoUrl;

        public GroupAdapter(VKApiCommunityArray groups) {
            super(getActivity(), 0, groups);
            mLastGroupPhotoUrl = groups.get(groups.size() - 1).photo_100;
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
                photoImageView.setImageResource(R.drawable.camera_100);
                mPhotoManager.fetchOnePhoto(group.photo_100, mFetchingListener);
            }

            TextView titleTextView = (TextView) convertView.findViewById(R.id.item_title);
            titleTextView.setText(group.name);
            TextView friendsTextView = (TextView) convertView.findViewById(R.id.common_count);
            friendsTextView.setText(getString(R.string.friends, mDataManager.getFriendsInGroup(group).size()));
            return convertView;
        }

        private Listener<Bitmap> mFetchingListener = new Listener<Bitmap>() {
            private int i = 0;

            @Override
            public void onCompleted(Bitmap bitmap) {
                // FIXME: 19.01.2016 
                // Чтобы не обновлять адаптер слишком часто, но точно обновить его после загрузки последнего фото.
                /*++i;
                if (i == 8 || bitmap == mPhotoManager.getPhoto(mLastGroupPhotoUrl)) {
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