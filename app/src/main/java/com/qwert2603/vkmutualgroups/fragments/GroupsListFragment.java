package com.qwert2603.vkmutualgroups.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.qwert2603.vkmutualgroups.Listener;
import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.activities.FriendsListActivity;
import com.qwert2603.vkmutualgroups.behaviors.FloatingActionButtonBehavior;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.photo.ImageViewHolder;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiUserFull;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.finished;

/**
 * Отображает список групп из DataManager в соответствии с id пользователя, переданным в {@link #newInstance(int)}
 */
public class GroupsListFragment extends Fragment {

    @SuppressWarnings("unused")
    private static final String TAG = "GroupsListFragment";

    private static final String friendIdKey = "friendIdKey";

    private static final int REQUEST_SEND_MESSAGE = 1;

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

    public interface Callbacks {
        @NonNull
        CoordinatorLayout getCoordinatorLayout();
    }

    private DataManager mDataManager;
    private PhotoManager mPhotoManager;

    private ListView mListView;
    private int mListViewScrollState;
    private GroupAdapter mGroupsAdapter;

    private VKApiCommunityArray mGroups;
    private int mFriendId;

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
        mListView.setOnItemClickListener((parent, view1, position, id) -> {
            if (mDataManager.getFetchingState() == DataManager.FetchingState.finished) {
                Intent intent = new Intent(getActivity(), FriendsListActivity.class);
                VKApiCommunityFull group = (VKApiCommunityFull) mListView.getAdapter().getItem(position);
                intent.putExtra(FriendsListActivity.EXTRA_GROUP_ID, group.id);
                startActivity(intent);
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

        TextView no_commons_text_view = (TextView) view.findViewById(android.R.id.empty);
        no_commons_text_view.setText(mFriendId == 0 ? R.string.no_groups : R.string.no_mutual_groups);

        mListView.setEmptyView(no_commons_text_view);

        mGroupsAdapter = new GroupAdapter(mGroups);
        mListView.setAdapter(mGroupsAdapter);

        FloatingActionButton actionButton = (FloatingActionButton) view.findViewById(R.id.fragment_list_action_button);
        actionButton.setIcon(R.drawable.message);
        actionButton.setOnClickListener((v) -> {
            SendMessageDialogFragment sendMessageDialogFragment = SendMessageDialogFragment.newInstance(mFriendId);
            sendMessageDialogFragment.setTargetFragment(this, REQUEST_SEND_MESSAGE);
            sendMessageDialogFragment.show(getFragmentManager(), SendMessageDialogFragment.TAG);
        });
        if (mFriendId == 0 || mDataManager.getFetchingState() != finished) {
            actionButton.setVisibility(View.INVISIBLE);
        }

        // Прикрепляем actionButton к CoordinatorLayout активности, чтобы actionButton смещался при появлении Snackbar.
        CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        layoutParams.gravity = Gravity.END | Gravity.BOTTOM;
        int margin = (int) getResources().getDimension(R.dimen.floatingActionButtonMargin);
        layoutParams.bottomMargin = layoutParams.rightMargin = margin;
        layoutParams.setBehavior(new FloatingActionButtonBehavior(getActivity(), null));

        ((ViewGroup) actionButton.getParent()).removeView(actionButton);
        CoordinatorLayout coordinatorLayout = ((Callbacks) getActivity()).getCoordinatorLayout();
        coordinatorLayout.addView(actionButton, layoutParams);

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SEND_MESSAGE && resultCode == Activity.RESULT_OK) {
            if (getView() != null) {
                Snackbar.make(((Callbacks) getActivity()).getCoordinatorLayout(),
                        R.string.message_sent, Snackbar.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
