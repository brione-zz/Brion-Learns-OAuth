package com.eyebrowssoftware.bloa.activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class ProgressDialogFragment extends DialogFragment {

    private static final String TITLE = "title";
    private static final String MESSAGE = "message";

    public static ProgressDialogFragment newInstance(int titleResource, int messageResource) {
        ProgressDialogFragment dialog = new ProgressDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(TITLE, titleResource);
        arguments.putInt(MESSAGE, messageResource);
        dialog.setArguments(arguments);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = this.getArguments();
        ProgressDialog dialog = new ProgressDialog(this.getActivity());
        dialog.setCancelable(false);
        dialog.setMessage(getResources().getText(arguments.getInt(MESSAGE)));
        dialog.setTitle(getResources().getText(arguments.getInt(TITLE)));
        return dialog;
    }

}
