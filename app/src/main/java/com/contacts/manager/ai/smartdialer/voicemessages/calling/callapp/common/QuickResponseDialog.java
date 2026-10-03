package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.CycleInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.DialogQuickResponseBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;

public final class QuickResponseDialog {

    public interface Listener {
        void onSubmit(String value);
    }

    public interface DuplicateCheck {
        boolean isDuplicate(String value);
    }

    public static final int MAX_LENGTH = 160;
    private static final int MAX_LINES = 4;
    private static final float DISABLED_ALPHA = 0.45f;

    private QuickResponseDialog() {
    }

    public static Dialog show(Activity activity, @StringRes int title, @Nullable String initial,
                              @StringRes int action, DuplicateCheck duplicateCheck, Listener listener) {
        return show(activity, title, initial, action, 0, duplicateCheck, listener);
    }

    public static Dialog show(Activity activity, @StringRes int title, @Nullable String initial,
                              @StringRes int action, @DrawableRes int icon,
                              @Nullable DuplicateCheck duplicateCheck, Listener listener) {
        DialogQuickResponseBinding binding = DialogQuickResponseBinding.inflate(activity.getLayoutInflater());
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(binding.getRoot());

        if (icon != 0) {
            binding.dialogIcon.setImageResource(icon);
            binding.dialogIconContainer.setVisibility(View.VISIBLE);
            binding.dialogIconContainer.setScaleX(0.6f);
            binding.dialogIconContainer.setScaleY(0.6f);
            binding.dialogIconContainer.setAlpha(0f);
            binding.dialogIconContainer.animate().scaleX(1f).scaleY(1f).alpha(1f)
                    .setStartDelay(120).setDuration(320)
                    .setInterpolator(new OvershootInterpolator(2.2f)).start();
        }

        int focusedLine = ContextCompat.getColor(activity, R.color.primary);
        int idleLine = ContextCompat.getColor(activity, R.color.divider);
        int errorLine = ContextCompat.getColor(activity, R.color.error);
        String original = initial == null ? "" : initial.trim();

        binding.dialogTitle.setText(title);
        binding.actionButton.setText(action);
        binding.responseField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_LENGTH)});
        binding.responseField.setHorizontallyScrolling(false);
        binding.responseField.setMaxLines(MAX_LINES);
        if (initial != null) {
            binding.responseField.setText(initial);
            binding.responseField.setSelection(binding.responseField.length());
        }
        updateCounter(activity, binding);
        setActionEnabled(binding, false);

        binding.responseField.setOnFocusChangeListener((v, hasFocus) -> {
            if (binding.responseError.getVisibility() != View.VISIBLE) {
                binding.responseUnderline.setBackgroundColor(hasFocus ? focusedLine : idleLine);
            }
        });
        binding.responseField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().trim();
                setActionEnabled(binding, !value.isEmpty() && !value.equals(original));
                updateCounter(activity, binding);
                if (binding.responseError.getVisibility() == View.VISIBLE) {
                    binding.responseError.setVisibility(View.INVISIBLE);
                    binding.responseUnderline.setBackgroundColor(focusedLine);
                }
            }
        });

        Runnable submit = () -> {
            String value = binding.responseField.getText().toString().trim();
            if (value.isEmpty() || value.equals(original)) {
                return;
            }
            if (duplicateCheck != null && duplicateCheck.isDuplicate(value)) {
                binding.responseError.setVisibility(View.VISIBLE);
                binding.responseUnderline.setBackgroundColor(errorLine);
                HapticUtils.longPress(binding.responseField);
                binding.responseField.animate().cancel();
                binding.responseField.setTranslationX(0f);
                binding.responseField.animate().translationX(activity.getResources().getDisplayMetrics().density * 8f)
                        .setInterpolator(new CycleInterpolator(3)).setDuration(360).start();
                return;
            }
            HapticUtils.confirm(binding.actionButton);
            dialog.dismiss();
            listener.onSubmit(value);
        };
        binding.actionButton.setOnClickListener(v -> submit.run());
        binding.cancelButton.setOnClickListener(v -> {
            HapticUtils.tap(v);
            dialog.dismiss();
        });
        binding.responseField.setOnEditorActionListener((v, actionId, event) -> {
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
        dialog.setOnShowListener(d -> binding.responseField.requestFocus());
        dialog.show();
        return dialog;
    }

    private static void updateCounter(Activity activity, DialogQuickResponseBinding binding) {
        int length = binding.responseField.length();
        binding.responseCounter.setText(activity.getString(R.string.quick_response_counter, length, MAX_LENGTH));
        binding.responseCounter.setTextColor(ContextCompat.getColor(activity,
                length >= MAX_LENGTH ? R.color.error : R.color.text_tertiary));
    }

    private static void setActionEnabled(DialogQuickResponseBinding binding, boolean enabled) {
        binding.actionButton.setEnabled(enabled);
        binding.actionButton.animate().alpha(enabled ? 1f : DISABLED_ALPHA).setDuration(150).start();
    }
}
