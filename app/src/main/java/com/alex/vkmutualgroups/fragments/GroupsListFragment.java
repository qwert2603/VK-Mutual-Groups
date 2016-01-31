package com.alex.vkmutualgroups.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.alex.vkmutualgroups.Listener;
import com.alex.vkmutualgroups.R;
import com.alex.vkmutualgroups.activities.FriendsListActivity;
import com.alex.vkmutualgroups.data.DataManager;
import com.alex.vkmutualgroups.photo.ImageViewHolder;
import com.alex.vkmutualgroups.photo.PhotoManager;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiUserFull;

import static android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
import static com.alex.vkmutualgroups.data.DataManager.FetchingState.finished;

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

    private DataManager mDataManager;
    private PhotoManager mPhotoManager;

    private VKApiCommunityArray mGroups;
    private int mFriendId;

    private ListView mListView;
    private GroupAdapter mGroupsAdapter;
    private int mListViewScrollState;

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
                mListViewScrollState = scrollState;
                if (mListViewScrollState == SCROLL_STATE_IDLE) {
                    // при остановке скроллинга загружаем фото видимых групп.
                    notifyDataSetChanged();
                    fetchVisibleGroupsPhoto();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        TextView no_commons_text_view = (TextView) view.findViewById(R.id.empty_list);
        no_commons_text_view.setText(R.string.no_mutual_groups);

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

    private boolean isEverResumed = false;
    @Override
    public void onResume() {
        super.onResume();
        if (! isEverResumed) {
            isEverResumed = true;
            // при первом запуске загружаем фото первых 20 групп.
            int e = Math.min(20, mGroups.size());
            for (int i = 0; i < e; ++i) {
                if (mPhotoManager.getPhoto(mGroups.get(i).photo_50) == null) {
                    mPhotoManager.fetchPhoto(mGroups.get(i).photo_50, listenerToUpdate);
                }
            }
        }
    }

    /**
     * Загрузить фото для отображаемых групп и ближайших к отображаемым.
     */
    private void fetchVisibleGroupsPhoto() {
        int padding = 3;
        int b = mListView.getFirstVisiblePosition();
        int e = mListView.getLastVisiblePosition() + 1;
        int pb = Math.max(0, b - padding);
        int pe = Math.min(mGroups.size(), e + padding);
        for (int i = b; i < e; ++i) {
            if (mPhotoManager.getPhoto(mGroups.get(i).photo_50) == null) {
                mPhotoManager.fetchPhoto(mGroups.get(i).photo_50, listenerToUpdate);
            }
        }
        for (int i = e; i < pe; ++i) {
            if (mPhotoManager.getPhoto(mGroups.get(i).photo_50) == null) {
                mPhotoManager.fetchPhoto(mGroups.get(i).photo_50, null);
            }
        }
        for (int i = pb; i < b; ++i) {
            if (mPhotoManager.getPhoto(mGroups.get(i).photo_50) == null) {
                mPhotoManager.fetchPhoto(mGroups.get(i).photo_50, null);
            }
        }
    }

    private Listener<Bitmap> listenerToUpdate = new Listener<Bitmap>() {
        @Override
        public void onCompleted(Bitmap bitmap) {
            if (mListViewScrollState == SCROLL_STATE_IDLE) {
                notifyDataSetChanged();
            }
        }

        @Override
        public void onError(String e) {
            Log.e(TAG, e);
        }
    };

    /**
     * Обновить адаптер ListView.
     */
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
                //mPhotoManager.setPhotoToImageViewHolder(viewHolder, group.photo_50);
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.groups_list_fragment, menu);
        MenuItem sendMessageMenuItem = menu.findItem(R.id.menu_message);
        sendMessageMenuItem.setVisible(mFriendId != 0);
        sendMessageMenuItem.setEnabled(mDataManager.getFetchingState() == finished);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_message:
                SendMessageDialogFragment sendMessageDialogFragment = SendMessageDialogFragment.newInstance(mFriendId);
                sendMessageDialogFragment.show(getFragmentManager(), SendMessageDialogFragment.TAG);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}