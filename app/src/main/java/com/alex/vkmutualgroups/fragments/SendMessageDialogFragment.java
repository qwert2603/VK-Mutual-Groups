package com.alex.vkmutualgroups.fragments;

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

import com.alex.vkmutualgroups.R;
import com.alex.vkmutualgroups.data.DataManager;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiUserFull;

/**
 * Диалог фрагмент для отправки сообщения о кол-ве общих групп.
 * Если отправка сообщения пользователю невозможна (он закрыл личные сообщения),
 * выводится соответствующая надпись.
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
        mFriend = DataManager.get(getActivity()).getFriendById(friendId);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return mFriend.can_write_private_message ? getDialogComposeSendMessage() : getDialogCantWrite();

    }

    /**
     * Диалог-фрагмент, сообщающий о том, что нельзя писать этому другу.
     */
    @SuppressLint("InflateParams")
    private Dialog getDialogCantWrite() {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_fragment_cant_write, null);
        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .create();
    }

    /**
     * Диалог-фрагмент для ввода и отправки сообщения.
     */
    @SuppressLint("InflateParams")
    private Dialog getDialogComposeSendMessage() {
        int mutual = DataManager.get(getActivity()).getGroupsMutualWithFriend(mFriend).size();

        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_fragment_write, null);

        TextView friendName = (TextView) view.findViewById(R.id.friend_name_text_view);
        friendName.setText(getString(R.string.friend_name, mFriend.first_name, mFriend.last_name));

        final EditText editText = (EditText) view.findViewById(R.id.edit_text);

        String text = getString(R.string.send_dialog_message_prefix);
        text += (mutual != 0 ? mutual : getString(R.string.no));
        text += getMessageSuffix(mutual);
        editText.setText(text);

        Button cancelButton = (Button) view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener((v) -> dismiss());

        Button sendButton = (Button) view.findViewById(R.id.send_button);
        sendButton.setOnClickListener((v) -> {
            VKParameters parameters = VKParameters.from(
                    VKApiConst.USER_ID, mFriend.id, VKApiConst.MESSAGE, editText.getText());
            VKRequest request = new VKRequest("messages.send", parameters);
            request.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
                    dismiss();
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

    /**
     * Окончание слова (например, существительного) в зависимости от кол-ва.
     */
    private String getMessageSuffix(int count) {
        count = count % 100;
        if (count % 10 == 0 || count / 10 == 1) {
            return getString(R.string.send_dialog_message_suffix_5);   // -
        }
        if (count % 10 == 1) {
            return getString(R.string.send_dialog_message_suffix_1);   // а
        }
        if (count % 10 >= 2 && count % 10 <= 4) {
            return getString(R.string.send_dialog_message_suffix_2);   // ы
        }
        return getString(R.string.send_dialog_message_suffix_5);   // -
    }
}