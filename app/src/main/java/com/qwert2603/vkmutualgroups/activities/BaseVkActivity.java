package com.qwert2603.vkmutualgroups.activities;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.qwert2603.vkmutualgroups.Listener;
import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.ConfirmationDialogFragment;
import com.qwert2603.vkmutualgroups.fragments.SendMessageDialogFragment;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiUserFull;

/**
 * Базовая Activity. Позволяет посылать сообщения, удалять из друзей и выходить из групп.
 */
public abstract class BaseVkActivity extends AppCompatActivity {

    @SuppressWarnings("unused")
    public static final String TAG = "BaseVkActivity";

    private static final int REQUEST_SEND_MESSAGE = 1;
    private static final int REQUEST_DELETE_FRIEND = 2;
    private static final int REQUEST_LEAVE_GROUP = 3;

    private static final String friendToDeleteId = "friendToDeleteId";
    private static final String groupToLeaveId = "groupToLeaveId";

    private DataManager mDataManager;

    private Bundle mArgs;

    /**
     * CoordinatorLayout для показа Snackbar.
     */
    @NonNull
    public abstract CoordinatorLayout getCoordinatorLayout();

    /**
     * Данные изменились после удаления из друзей или выхода из группы.
     */
    protected abstract void notifyDataSetChanged();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataManager = DataManager.get(this);
        mArgs = new Bundle();
    }

    public void sendMessage(int friendId) {
        SendMessageDialogFragment sendMessageDialogFragment = SendMessageDialogFragment.newInstance(friendId);
        sendMessageDialogFragment.setTargetFragment(mTargetFragment, REQUEST_SEND_MESSAGE);
        sendMessageDialogFragment.show(getFragmentManager(), SendMessageDialogFragment.TAG);
    }

    public void deleteFriend(int friendId) {
        VKApiUserFull friend = mDataManager.getUsersFriendById(friendId);
        if (friend == null) {
            Log.e(TAG, "deleteFriend ## ERROR!!! friend == null");
            return;
        }
        mArgs.putInt(friendToDeleteId, friendId);

        String title = getString(R.string.friend_name, friend.first_name, friend.last_name);
        String question = getString(R.string.delete_friend) + "?";
        ConfirmationDialogFragment dialogFragment = ConfirmationDialogFragment.newInstance(title, question);
        dialogFragment.setTargetFragment(mTargetFragment, REQUEST_DELETE_FRIEND);
        dialogFragment.show(getFragmentManager(), ConfirmationDialogFragment.TAG);
    }

    public void leaveGroup(int groupId) {
        VKApiCommunityFull group = mDataManager.getUsersGroupById(groupId);
        if (group == null) {
            Log.e(TAG, "leaveGroup ## ERROR!!! group == null");
            return;
        }
        mArgs.putInt(groupToLeaveId, groupId);

        String title = group.name;
        String question = getString(R.string.leave_group) + "?";
        ConfirmationDialogFragment dialogFragment = ConfirmationDialogFragment.newInstance(title, question);
        dialogFragment.setTargetFragment(mTargetFragment, REQUEST_LEAVE_GROUP);
        dialogFragment.show(getFragmentManager(), ConfirmationDialogFragment.TAG);
    }

    private Fragment mTargetFragment = new Fragment() {
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (resultCode == Activity.RESULT_OK) {
                switch (requestCode) {
                    case REQUEST_SEND_MESSAGE:
                        Snackbar.make(getCoordinatorLayout(), R.string.message_sent, Snackbar.LENGTH_SHORT).show();
                        break;
                    case REQUEST_DELETE_FRIEND:
                        mDataManager.deleteFriend(mArgs.getInt(friendToDeleteId), new Listener<Void>() {
                            @Override
                            public void onCompleted(Void aVoid) {
                                Snackbar.make(getCoordinatorLayout(), R.string.friend_deleted_successfully, Snackbar.LENGTH_SHORT).show();
                                notifyDataSetChanged();
                            }

                            @Override
                            public void onError(String e) {
                                Snackbar.make(getCoordinatorLayout(), R.string.friend_deleting_error, Snackbar.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case REQUEST_LEAVE_GROUP:
                        mDataManager.leaveGroup(mArgs.getInt(groupToLeaveId), new Listener<Void>() {
                            @Override
                            public void onCompleted(Void aVoid) {
                                Snackbar.make(getCoordinatorLayout(), R.string.group_left_successfully, Snackbar.LENGTH_SHORT).show();
                                notifyDataSetChanged();
                            }

                            @Override
                            public void onError(String e) {
                                Snackbar.make(getCoordinatorLayout(), R.string.group_leaving_error, Snackbar.LENGTH_SHORT).show();
                            }
                        });
                        break;
                }
            }
        }
    };

}
