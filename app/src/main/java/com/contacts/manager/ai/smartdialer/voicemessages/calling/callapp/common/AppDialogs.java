package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.app.Dialog;
import android.content.Context;

import androidx.annotation.StringRes;

public final class AppDialogs {

    private AppDialogs() {
    }

    public static Dialog confirm(Context context, CharSequence title, CharSequence message,
                                 @StringRes int positive, boolean destructive, Runnable onConfirm) {
        return ConfirmDialog.show(context, title, message, positive, destructive, onConfirm);
    }
}
