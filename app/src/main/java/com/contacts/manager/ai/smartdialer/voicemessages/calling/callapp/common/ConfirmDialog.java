package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.analytics.Analytics;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.DialogConfirmBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;

public final class ConfirmDialog {

    private ConfirmDialog() {
    }

    public static Dialog show(Context context, CharSequence title, @Nullable CharSequence message,
                              @StringRes int action, Runnable onConfirm) {
        return show(context, title, message, action, false, onConfirm);
    }

    public static Dialog show(Context context, CharSequence title, @Nullable CharSequence message,
                              @StringRes int action, boolean destructive, Runnable onConfirm) {
        return show(context, title, message, action, destructive, 0, R.string.cancel, onConfirm);
    }

    public static Dialog show(Context context, CharSequence title, @Nullable CharSequence message,
                              @StringRes int action, boolean destructive, @DrawableRes int icon,
                              @StringRes int cancel, Runnable onConfirm) {
        return show(context, title, message, action, destructive, icon, cancel, onConfirm, null);
    }

    public static Dialog show(Context context, CharSequence title, @Nullable CharSequence message,
                              @StringRes int action, boolean destructive, @DrawableRes int icon,
                              @StringRes int cancel, Runnable onConfirm, @Nullable Runnable onCancel) {
        return create(context, title, message, action, destructive, icon, cancel, onConfirm, onCancel, true,
                "confirm_dialog");
    }

    public static Dialog required(Context context, CharSequence title, @Nullable CharSequence message,
                                  @StringRes int action, Runnable onConfirm, String analyticsName) {
        return create(context, title, message, action, false, 0, 0, onConfirm, null, false, analyticsName);
    }

    private static Dialog create(Context context, CharSequence title, @Nullable CharSequence message,
                                 @StringRes int action, boolean destructive, @DrawableRes int icon,
                                 @StringRes int cancel, Runnable onConfirm, @Nullable Runnable onCancel,
                                 boolean cancelable, String analyticsName) {
        DialogConfirmBinding binding = DialogConfirmBinding.inflate(LayoutInflater.from(context));
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(binding.getRoot());
        dialog.setCancelable(cancelable);

        binding.confirmTitle.setText(title);
        binding.confirmMessage.setText(message);
        binding.confirmMessage.setVisibility(TextUtils.isEmpty(message) ? View.GONE : View.VISIBLE);
        binding.confirmAction.setText(action);
        if (cancel != 0) {
            binding.confirmCancel.setText(cancel);
        } else {
            binding.confirmCancel.setVisibility(View.GONE);
            binding.confirmSpace.setVisibility(View.GONE);
        }
        if (destructive) {
            binding.confirmAction.setBackgroundResource(R.drawable.bg_dialog_button_danger);
        }
        if (icon != 0) {
            binding.confirmIconContainer.setVisibility(View.VISIBLE);
            binding.confirmIcon.setImageResource(icon);
            if (!destructive) {
                binding.confirmIconContainer.setBackgroundResource(R.drawable.bg_circle_tint);
                binding.confirmIcon.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.primary)));
            }
            binding.confirmIconContainer.setScaleX(0.6f);
            binding.confirmIconContainer.setScaleY(0.6f);
            binding.confirmIconContainer.setAlpha(0f);
            binding.confirmIconContainer.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setStartDelay(120)
                    .setDuration(320)
                    .setInterpolator(new OvershootInterpolator(2.2f))
                    .start();
        }
        binding.confirmCancel.setOnClickListener(v -> {
            HapticUtils.tap(v);
            dialog.dismiss();
            if (onCancel != null) {
                onCancel.run();
            }
        });
        binding.confirmAction.setOnClickListener(v -> {
            HapticUtils.confirm(v);
            dialog.dismiss();
            onConfirm.run();
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int margin = context.getResources().getDimensionPixelSize(R.dimen.space_8);
            int width = context.getResources().getDisplayMetrics().widthPixels - margin * 2;
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            WindowManager.LayoutParams params = window.getAttributes();
            params.y = context.getResources().getDimensionPixelSize(R.dimen.space_16);
            window.setAttributes(params);
            window.setDimAmount(0.45f);
            window.setWindowAnimations(R.style.Animation_App_Rise);
        }
        Analytics.trackDialog(dialog, analyticsName);
        dialog.show();
        return dialog;
    }
}
