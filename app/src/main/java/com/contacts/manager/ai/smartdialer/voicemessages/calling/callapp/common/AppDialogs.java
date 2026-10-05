package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.analytics.Analytics;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.DialogInputBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class AppDialogs {

    public interface InputListener {
        void onInput(String value);
    }

    private AppDialogs() {
    }

    public static AlertDialog confirm(Context context, CharSequence title, CharSequence message,
                                      @StringRes int positive, boolean destructive, Runnable onConfirm) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context,
                destructive ? R.style.ThemeOverlay_App_Dialog_Destructive : R.style.ThemeOverlay_App_Dialog)
                .setTitle(title)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(positive, (d, w) -> onConfirm.run());
        if (!TextUtils.isEmpty(message)) {
            builder.setMessage(message);
        }
        AlertDialog dialog = builder.create();
        Analytics.trackDialog(dialog, "alert_dialog");
        dialog.show();
        return dialog;
    }

    public static AlertDialog input(Context context, CharSequence title, String initial, @StringRes int hint,
                                    InputListener listener) {
        return input(context, title, initial, hint, 0, listener);
    }

    public static AlertDialog input(Context context, CharSequence title, String initial, @StringRes int hint,
                                    int inputType, InputListener listener) {
        DialogInputBinding binding = DialogInputBinding.inflate(LayoutInflater.from(context));
        binding.inputLayout.setHint(hint);
        if (inputType != 0) {
            binding.inputField.setInputType(inputType);
            binding.inputField.setMaxLines(1);
            binding.inputLayout.setCounterEnabled(false);
        }
        if (initial != null) {
            binding.inputField.setText(initial);
            binding.inputField.setSelection(initial.length());
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_App_Dialog)
                .setTitle(title)
                .setView(binding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String value = binding.inputField.getText() == null ? "" : binding.inputField.getText().toString().trim();
                if (value.isEmpty()) {
                    binding.inputLayout.setError(context.getString(R.string.error_empty_message));
                    return;
                }
                listener.onInput(value);
                dialog.dismiss();
            });
            binding.inputField.requestFocus();
        });
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        Analytics.trackDialog(dialog, "input_dialog");
        dialog.show();
        return dialog;
    }
}
