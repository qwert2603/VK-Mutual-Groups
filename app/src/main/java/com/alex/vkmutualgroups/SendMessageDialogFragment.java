package com.alex.vkmutualgroups;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

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
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int friendId = getArguments().getInt(friendIdKey);
        mFriend = DataManager.get().getFriendById(friendId);
        mContext = getActivity().getApplicationContext();
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
        int mutual = DataManager.get().getGroupsMutualWithFriend(mFriend).size();

        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_fragment_write, null);
        final EditText editText = (EditText) view.findViewById(R.id.edit_text);

        String text = getString(R.string.send_dialog_message_prefix);
        text += (mutual != 0 ? mutual : getString(R.string.no));
        text += getMessageSuffix(mutual);
        editText.setText(text);

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (editText.getText().length() == 0) {
                            Toast.makeText(mContext, R.string.empty_message, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        VKParameters parameters = VKParameters.from(
                                VKApiConst.USER_ID, mFriend.id, VKApiConst.MESSAGE, editText.getText());
                        VKRequest request = new VKRequest("messages.send", parameters);
                        request.executeWithListener(new VKRequest.VKRequestListener() {
                            @Override
                            public void onComplete(VKResponse response) {
                                Toast.makeText(mContext, R.string.message_sent, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(VKError error) {
                                Log.e(TAG, error.toString());
                                Toast.makeText(mContext, R.string.message_sending_error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
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