package com.qwert2603.vkmutualgroups.errors_show;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.util.VkRequestsSender;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;

public class ErrorsShowDialog extends DialogFragment {

    private static final int DEVELOPER_VK_ID = 137183400;

    public static ErrorsShowDialog newInstance() {
        return new ErrorsShowDialog();
    }

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_errors_show, null);
        TextView errorsTextView = (TextView) view.findViewById(R.id.errors_text_view);

        String e = ErrorsHolder.get().getErrors(getActivity());
        String errors = (e == null ? "Loading errors failed!!!" : e);
        errorsTextView.setText(errors);

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNeutralButton(R.string.send_to_developer, (dialog, which) -> {
                    VKParameters parameters = VKParameters.from(
                            VKApiConst.USER_ID, DEVELOPER_VK_ID, VKApiConst.MESSAGE, errors);
                    VKRequest request = new VKRequest("messages.send", parameters);
                    VkRequestsSender.sendRequest(request, new VKRequest.VKRequestListener() {
                    });
                })
                .setNegativeButton(R.string.clear_errors, (dialog, which) -> {
                    ErrorsHolder.get().clearErrors(getActivity());
                })
                .create();
    }
}
