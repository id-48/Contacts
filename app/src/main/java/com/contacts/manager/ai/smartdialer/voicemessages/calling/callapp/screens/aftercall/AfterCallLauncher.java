package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.aftercall;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.provider.CallLog;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.IntentKeys;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.RemoteConfigManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.NotificationService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom.CallManager;

public final class AfterCallLauncher {

    private static final long COOLDOWN_MS = 2_500L;
    private static long lastOpenedAt;

    private AfterCallLauncher() {
    }

    public static boolean canOpen() {
        return StorageService.isAfterCallEnabled() && RemoteConfigManager.isAfterCallScreenEnabled();
    }

    public static void openPhone(Context context, Intent screen) {
        if (!canOpen()) return;
        markOpened();
        start(context, screen, false);
    }

    public static void openWhatsApp(Context context, @Nullable String name, long startedAtMs) {
        if (!canOpen()) return;
        if (!RemoteConfigManager.get().whatsappAfterCallEnabled) return;
        if (CallManager.hasCalls()) return;
        if (SystemClock.elapsedRealtime() - lastOpenedAt < COOLDOWN_MS) return;
        long now = System.currentTimeMillis();
        long duration = startedAtMs > 0 ? Math.max(0, (now - startedAtMs) / 1000L) : 0L;
        Intent screen = AfterCallActivity.intent(context, null, CallLog.Calls.INCOMING_TYPE, duration,
                startedAtMs > 0 ? startedAtMs : now);
        screen.putExtra(IntentKeys.EXTRA_FROM_WHATSAPP, true);
        if (!TextUtils.isEmpty(name)) {
            screen.putExtra(IntentKeys.EXTRA_NAME, name);
        }
        markOpened();
        start(context, screen, true);
    }

    private static void markOpened() {
        lastOpenedAt = SystemClock.elapsedRealtime();
    }

    private static void start(Context context, Intent screen, boolean fromBackground) {
        screen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        boolean started = false;
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setPendingIntentCreatorBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                context.startActivity(screen, options.toBundle());
            } else {
                context.startActivity(screen);
            }
            started = true;
        } catch (RuntimeException ignored) {
        }
        if (fromBackground || !started) {
            NotificationService.showAfterCallLaunch(context, screen);
        }
    }
}
