package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;

/** Small non-cancelable "Ads Loading..." card shown while a fullscreen ad loads. */
public final class FullscreenAdLoadingDialog {

    @Nullable
    private Dialog dialog;

    private FullscreenAdLoadingDialog() {
    }

    public static FullscreenAdLoadingDialog show(Activity activity) {
        FullscreenAdLoadingDialog loader = new FullscreenAdLoadingDialog();
        if (activity.isFinishing() || activity.isDestroyed()) return loader;
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_ads_loading);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.45f);
        }
        try {
            dialog.show();
            loader.dialog = dialog;
        } catch (RuntimeException ignored) {
        }
        return loader;
    }

    public void dismiss() {
        if (dialog == null) return;
        try {
            if (dialog.isShowing()) dialog.dismiss();
        } catch (RuntimeException ignored) {
        }
        dialog = null;
    }
}
