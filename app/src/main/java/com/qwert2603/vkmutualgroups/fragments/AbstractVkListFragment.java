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
import android.widget.ListView;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.qwert2603.vkmutualgroups.Listener;
import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.behaviors.FloatingActionButtonBehavior;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;
import com.vk.sdk.api.model.Identifiable;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiModel;
import com.vk.sdk.api.model.VKApiUserFull;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

/**
 * Фрагмент для отображения списка друзей или групп.
 * Автоматически загружает фото для видимых элементов списка при остановке прокрутки.
 * Позволяет запускать диалог отправки сообщения и показывает Snackbar, если сообщение было успешно отправлено.
 */
public abstract class AbstractVkListFragment<T extends VKApiModel & Identifiable> extends Fragment {

    @SuppressWarnings("unused")
    public static final String TAG = "AbstractVkListFragment";

    private static final int REQUEST_SEND_MESSAGE = 1;
    private static final int REQUEST_DELETE_FRIEND = 2;
    private static final int REQUEST_LEAVE_GROUP = 3;

    private static final String friendToDeleteId = "friendToDeleteId";
    private static final String groupToLeaveId = "groupToLeaveId";

    public interface Callbacks {
        @NonNull
        CoordinatorLayout getCoordinatorLayout();
    }
    private DataManager mDataManager;
    private PhotoManager mPhotoManager;

    protected ListView mListView;
    private int mListViewScrollState;

    protected FloatingActionButton mActionButton;

    protected abstract String getEmptyListText();

    protected abstract int getItemsCount();

    protected abstract String getPhotoURL(int index);

    protected abstract ArrayAdapter<T> getListAdapter();

    protected AbsListView.OnScrollListener mListViewOnScrollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            mListViewScrollState = scrollState;
            if (mListViewScrollState == SCROLL_STATE_IDLE) {
                // при остановке скроллинга загружаем фото видимых друзей или групп.
                notifyDataSetChanged();
                fetchVisibleFriendsPhoto();
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mDataManager = DataManager.get(getActivity());
        mPhotoManager = PhotoManager.get(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setOnScrollListener(mListViewOnScrollListener);

        TextView empty_list_text_view = (TextView) view.findViewById(android.R.id.empty);
        empty_list_text_view.setText(getEmptyListText());

        mListView.setEmptyView(empty_list_text_view);

        mActionButton = (FloatingActionButton) view.findViewById(R.id.fragment_list_action_button);

        // Прикрепляем actionButton к CoordinatorLayout активности, чтобы actionButton смещался при появлении Snackbar.
        CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        layoutParams.gravity = Gravity.END | Gravity.BOTTOM;
        int margin = (int) getResources().getDimension(R.dimen.floatingActionButtonMargin);
        layoutParams.bottomMargin = layoutParams.rightMargin = margin;
        layoutParams.setBehavior(new FloatingActionButtonBehavior(getActivity(), null));

        ((ViewGroup) mActionButton.getParent()).removeView(mActionButton);
        CoordinatorLayout coordinatorLayout = getCoordinatorLayout();
        coordinatorLayout.addView(mActionButton, layoutParams);

        return view;
    }

    private boolean isEverResumed = false;
    @Override
    public void onResume() {
        super.onResume();
        if (! isEverResumed) {
            isEverResumed = true;
            // при первом запуске загружаем фото первых 20 друзей.
            int e = Math.min(20, getItemsCount());
            for (int i = 0; i < e; ++i) {
                if (mPhotoManager.getPhoto(getPhotoURL(i)) == null) {
                    mPhotoManager.fetchPhoto(getPhotoURL(i), listenerToUpdate);
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Загрузить фото для отображаемых друзей и ближайших к отображаемым.
     */
    private void fetchVisibleFriendsPhoto() {
        int padding = 3;
        int b = mListView.getFirstVisiblePosition();
        int e = mListView.getLastVisiblePosition() + 1;
        int pb = Math.max(0, b - padding);
        int pe = Math.min(getItemsCount(), e + padding);
        for (int i = b; i < e; ++i) {
            if (mPhotoManager.getPhoto(getPhotoURL(i)) == null) {
                mPhotoManager.fetchPhoto(getPhotoURL(i), listenerToUpdate);
            }
        }
        for (int i = e; i < pe; ++i) {
            if (mPhotoManager.getPhoto(getPhotoURL(i)) == null) {
                mPhotoManager.fetchPhoto(getPhotoURL(i), null);
            }
        }
        for (int i = pb; i < b; ++i) {
            if (mPhotoManager.getPhoto(getPhotoURL(i)) == null) {
                mPhotoManager.fetchPhoto(getPhotoURL(i), null);
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
    public void notifyDataSetChanged() {
        ArrayAdapter<T> listAdapter = getListAdapter();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    public void sendMessage(int friendId) {
        SendMessageDialogFragment sendMessageDialogFragment = SendMessageDialogFragment.newInstance(friendId);
        sendMessageDialogFragment.setTargetFragment(this, REQUEST_SEND_MESSAGE);
        sendMessageDialogFragment.show(getFragmentManager(), SendMessageDialogFragment.TAG);
    }

    public void deleteFriend(int friendId) {
        VKApiUserFull friend = mDataManager.getFriendById(friendId);
        if (friend == null) {
            Log.e(TAG, "deleteFriend ## ERROR!!! friend == null");
            return;
        }
        getArguments().putInt(friendToDeleteId, friendId);

        String title = getString(R.string.friend_name, friend.first_name, friend.last_name);
        String question = getString(R.string.delete_friend) + "?";
        ConfirmationDialogFragment dialogFragment = ConfirmationDialogFragment.newInstance(title, question);
        dialogFragment.setTargetFragment(this, REQUEST_DELETE_FRIEND);
        dialogFragment.show(getFragmentManager(), ConfirmationDialogFragment.TAG);
    }

    public void leaveGroup(int groupId) {
        VKApiCommunityFull group = mDataManager.getGroupById(groupId);
        if (group == null) {
            Log.e(TAG, "leaveGroup ## ERROR!!! group == null");
            return;
        }
        getArguments().putInt(groupToLeaveId, groupId);

        String title = group.name;
        String question = getString(R.string.leave_group) + "?";
        ConfirmationDialogFragment dialogFragment = ConfirmationDialogFragment.newInstance(title, question);
        dialogFragment.setTargetFragment(this, REQUEST_LEAVE_GROUP);
        dialogFragment.show(getFragmentManager(), ConfirmationDialogFragment.TAG);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SEND_MESSAGE:
                    Snackbar.make(getCoordinatorLayout(), R.string.message_sent, Snackbar.LENGTH_SHORT).show();
                    break;
                case REQUEST_DELETE_FRIEND:
                    if (mDataManager.deleteFriend(getArguments().getInt(friendToDeleteId))) {
                        Snackbar.make(getCoordinatorLayout(), R.string.friend_deleted_successfully, Snackbar.LENGTH_SHORT).show();
                        notifyDataSetChanged();
                    } else {
                        Snackbar.make(getCoordinatorLayout(), R.string.friend_deleting_error, Snackbar.LENGTH_SHORT).show();
                    }
                    break;
                case REQUEST_LEAVE_GROUP:
                    if (mDataManager.leaveGroup(getArguments().getInt(groupToLeaveId))) {
                        Snackbar.make(getCoordinatorLayout(), R.string.group_left_successfully, Snackbar.LENGTH_SHORT).show();
                        notifyDataSetChanged();
                    } else {
                        Snackbar.make(getCoordinatorLayout(), R.string.group_leaving_error, Snackbar.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * CoordinatorLayout для показа Snackbar.
     */
    @NonNull
    protected CoordinatorLayout getCoordinatorLayout() {
        return ((Callbacks) getActivity()).getCoordinatorLayout();
    }

}
