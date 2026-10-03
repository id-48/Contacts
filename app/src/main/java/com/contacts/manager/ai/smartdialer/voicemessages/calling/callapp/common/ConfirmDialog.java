package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.DialogConfirmBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;

public final class ConfirmDialog {

    private ConfirmDialog() {
    }

    public static Dialog show(Activity activity, CharSequence title, CharSequence message,
                              @StringRes int action, Runnable onConfirm) {
        return show(activity, title, message, action, false, onConfirm);
    }

    public static Dialog show(Activity activity, CharSequence title, CharSequence message,
                              @StringRes int action, boolean destructive, Runnable onConfirm) {
        return show(activity, title, message, action, destructive, 0, R.string.cancel, onConfirm);
    }

    public static Dialog show(Activity activity, CharSequence title, CharSequence message,
                              @StringRes int action, boolean destructive, @DrawableRes int icon,
                              @StringRes int cancel, Runnable onConfirm) {
        DialogConfirmBinding binding = DialogConfirmBinding.inflate(activity.getLayoutInflater());
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(binding.getRoot());

        binding.confirmTitle.setText(title);
        binding.confirmMessage.setText(message);
        binding.confirmAction.setText(action);
        binding.confirmCancel.setText(cancel);
        if (destructive) {
            binding.confirmAction.setBackgroundResource(R.drawable.bg_dialog_button_danger);
        }
        if (icon != 0) {
            binding.confirmIconContainer.setVisibility(View.VISIBLE);
            binding.confirmIcon.setImageResource(icon);
            if (!destructive) {
                binding.confirmIconContainer.setBackgroundResource(R.drawable.bg_circle_tint);
                binding.confirmIcon.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(activity, R.color.primary)));
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
        });
        binding.confirmAction.setOnClickListener(v -> {
            HapticUtils.confirm(v);
            dialog.dismiss();
            onConfirm.run();
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int margin = activity.getResources().getDimensionPixelSize(R.dimen.space_8);
            int width = activity.getResources().getDisplayMetrics().widthPixels - margin * 2;
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            WindowManager.LayoutParams params = window.getAttributes();
            params.y = activity.getResources().getDimensionPixelSize(R.dimen.space_16);
            window.setAttributes(params);
            window.setDimAmount(0.45f);
            window.setWindowAnimations(R.style.Animation_App_Rise);
        }
        dialog.show();
        return dialog;
    }
}
