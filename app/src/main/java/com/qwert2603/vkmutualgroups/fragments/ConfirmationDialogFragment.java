package com.qwert2603.vkmutualgroups.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.qwert2603.vkmutualgroups.R;

public class ConfirmationDialogFragment extends DialogFragment {

    public static final String TAG = "ConfirmationDialogFragm";

    private static final String titleKey = "titleKey";
    private static final String questionKey = "questionKey";

    public static ConfirmationDialogFragment newInstance(String title, String question) {
        ConfirmationDialogFragment result = new ConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putString(titleKey, title);
        args.putString(questionKey, question);
        result.setArguments(args);
        return result;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams")
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_fragment_confirmation, null);

        ((TextView) view.findViewById(R.id.title_text_view)).setText(getArguments().getString(titleKey));
        ((TextView) view.findViewById(R.id.question_text_view)).setText(getArguments().getString(questionKey));
        view.findViewById(R.id.cancel_button).setOnClickListener((v) -> setResult(false));
        view.findViewById(R.id.ok_button).setOnClickListener((v) -> setResult(true));

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .create();
    }

    private void setResult(boolean ok) {
        getTargetFragment().onActivityResult(getTargetRequestCode(), (ok ? Activity.RESULT_OK : Activity.RESULT_CANCELED), null);
        dismissAllowingStateLoss();
    }
}
