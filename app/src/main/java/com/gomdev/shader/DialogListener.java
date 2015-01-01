package com.gomdev.shader;

import android.app.DialogFragment;

public interface DialogListener {
    public void onDialogPositiveClick(DialogFragment dialog);

    public void onDialogNegativeClick(DialogFragment dialog);
}
