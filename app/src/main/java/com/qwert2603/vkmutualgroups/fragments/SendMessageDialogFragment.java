package com.qwert2603.vkmutualgroups.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiUserFull;

/**
 * Диалог фрагмент для отправки сообщения о кол-ве общих групп.
 * Если отправка сообщения пользователю невозможна (он закрыл личные сообщения),
 * выводится соответствующая надпись.
 *
 * Для работы необходимо, чтобы {@link DataManager#getUsersFriendById(int)} для friendId возвращал объект друга, а не null.
 */
public class SendMessageDialogFragment extends DialogFragment {

    public static final String TAG = "SendMessageDialogFragm";

    private static final String friendIdKey = "friendIdKey";

    public static SendMessageDialogFragment newInstance(int friendId) {
        SendMessageDialogFragment result = new SendMessageDialogFragment();
        Bundle args = new Bundle();
        args.putInt(friendIdKey, friendId);
        result.setArguments(args);
        return result;
    }

    private VKApiUserFull mFriend;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int friendId = getArguments().getInt(friendIdKey);
        mFriend = DataManager.get(getActivity()).getUsersFriendById(friendId);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (mFriend == null) {
            Log.e(TAG, "onCreateDialog ## mFriend == null");
        }
        return (mFriend != null) && (mFriend.can_write_private_message)
                ? getDialogComposeSendMessage() : getDialogWithText(R.string.cant_send_message);
    }

    /**
     * Диалог-фрагмент, сообщающий о чем-либо.
     */
    private Dialog getDialogWithText(int stringRes) {
        @SuppressLint("InflateParams")
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_fragment_cant_write, null);
        ((TextView) view.findViewById(R.id.text_view)).setText(stringRes);
        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .create();
    }

    /**
     * Диалог-фрагмент для ввода и отправки сообщения.
     */
    private Dialog getDialogComposeSendMessage() {
        @SuppressLint("InflateParams")
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_fragment_write, null);

        TextView friendNameTextView = (TextView) view.findViewById(R.id.friend_name_text_view);
        friendNameTextView.setText(getString(R.string.friend_name, mFriend.first_name, mFriend.last_name));

        final EditText editText = (EditText) view.findViewById(R.id.edit_text);
        String text = "";
        VKApiCommunityArray groupsMutualWithFriend = DataManager.get(getActivity()).getGroupsMutualWithFriend(mFriend.id);
        if (groupsMutualWithFriend != null) {
            int mutual = groupsMutualWithFriend.size();
            text = getResources().getQuantityString(R.plurals.we_have_26_mutual_groups, mutual, mutual);
            if (mutual == 0) {
                text = text.replace("0", getString(R.string.no));
            }
        }
        editText.setText(text);

        Button cancelButton = (Button) view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener((v) -> dismissAllowingStateLoss());

        Button sendButton = (Button) view.findViewById(R.id.send_button);
        sendButton.setOnClickListener((v) -> {
            VKParameters parameters = VKParameters.from(
                    VKApiConst.USER_ID, mFriend.id, VKApiConst.MESSAGE, editText.getText());
            VKRequest request = new VKRequest("messages.send", parameters);
            request.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
                    dismissAllowingStateLoss();
                }

                @Override
                public void onError(VKError error) {
                    Log.e(TAG, error.toString());
                    Snackbar.make(view, R.string.message_sending_error, Snackbar.LENGTH_SHORT).show();
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, null);
                }
            });
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendButton.setEnabled(! s.toString().isEmpty());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .create();
    }
}