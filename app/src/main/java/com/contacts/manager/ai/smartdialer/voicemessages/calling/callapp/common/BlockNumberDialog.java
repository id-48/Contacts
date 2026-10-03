package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.CycleInterpolator;
import android.view.inputmethod.EditorInfo;

import androidx.core.content.ContextCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.DialogBlockNumberBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

public final class BlockNumberDialog {

    public interface Listener {
        void onAdd(String number);
    }

    private static final int MIN_DIGITS = 3;
    private static final float DISABLED_ALPHA = 0.45f;

    private BlockNumberDialog() {
    }

    public static Dialog show(Activity activity, Listener listener) {
        DialogBlockNumberBinding binding = DialogBlockNumberBinding.inflate(activity.getLayoutInflater());
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(binding.getRoot());

        int focusedLine = ContextCompat.getColor(activity, R.color.primary);
        int idleLine = ContextCompat.getColor(activity, R.color.divider);
        int errorLine = ContextCompat.getColor(activity, R.color.error);

        setAddEnabled(binding, false);
        binding.numberField.setOnFocusChangeListener((v, hasFocus) -> {
            if (binding.numberError.getVisibility() != View.VISIBLE) {
                binding.numberUnderline.setBackgroundColor(hasFocus ? focusedLine : idleLine);
            }
        });
        binding.numberField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                setAddEnabled(binding, digitCount(s) >= MIN_DIGITS);
                if (binding.numberError.getVisibility() == View.VISIBLE) {
                    binding.numberError.setVisibility(View.GONE);
                    binding.numberUnderline.setBackgroundColor(focusedLine);
                }
            }
        });

        Runnable submit = () -> {
            String value = binding.numberField.getText().toString().trim();
            if (digitCount(value) < MIN_DIGITS) {
                return;
            }
            if (!PhoneUtils.isValidNumber(value)) {
                binding.numberError.setVisibility(View.VISIBLE);
                binding.numberUnderline.setBackgroundColor(errorLine);
                HapticUtils.longPress(binding.numberField);
                binding.numberField.animate().cancel();
                binding.numberField.setTranslationX(0f);
                binding.numberField.animate().translationX(activity.getResources().getDisplayMetrics().density * 8f)
                        .setInterpolator(new CycleInterpolator(3)).setDuration(360).start();
                return;
            }
            HapticUtils.confirm(binding.addButton);
            dialog.dismiss();
            listener.onAdd(value);
        };
        binding.addButton.setOnClickListener(v -> submit.run());
        binding.cancelButton.setOnClickListener(v -> dialog.dismiss());
        binding.numberField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit.run();
                return true;
            }
            return false;
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int margin = activity.getResources().getDimensionPixelSize(R.dimen.space_20);
            int width = activity.getResources().getDisplayMetrics().widthPixels - margin * 2;
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setDimAmount(0.45f);
            window.setWindowAnimations(R.style.Animation_App_Pop);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.setOnShowListener(d -> binding.numberField.requestFocus());
        dialog.show();
        return dialog;
    }

    private static void setAddEnabled(DialogBlockNumberBinding binding, boolean enabled) {
        binding.addButton.setEnabled(enabled);
        binding.addButton.animate().alpha(enabled ? 1f : DISABLED_ALPHA).setDuration(150).start();
    }

    private static int digitCount(CharSequence value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                count++;
            }
        }
        return count;
    }
}
