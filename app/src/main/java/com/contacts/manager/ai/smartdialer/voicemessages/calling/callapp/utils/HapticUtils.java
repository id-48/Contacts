package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils;

import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;

public final class HapticUtils {

    private HapticUtils() {
    }

    public static void tap(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    public static void tick(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
    }

    public static void longPress(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    public static void confirm(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }
}
