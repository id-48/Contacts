package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.RemoteConfigManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.aftercall.AfterCallLauncher;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WhatsAppCallListenerService extends NotificationListenerService {

    private static final String[] PACKAGES = {"com.whatsapp", "com.whatsapp.w4b"};
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long OPEN_DELAY_MS = 500L;

    private final Map<String, Session> active = new ConcurrentHashMap<>();

    public static boolean isEnabled(Context context) {
        String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        if (TextUtils.isEmpty(flat)) return false;
        ComponentName me = new ComponentName(context, WhatsAppCallListenerService.class);
        for (String entry : flat.split(":")) {
            ComponentName parsed = ComponentName.unflattenFromString(entry);
            if (me.equals(parsed)) return true;
        }
        return false;
    }

    public static Intent settingsIntent(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent detail = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);
            detail.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    new ComponentName(context, WhatsAppCallListenerService.class).flattenToString());
            if (detail.resolveActivity(context.getPackageManager()) != null) {
                return detail;
            }
        }
        return new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        active.clear();
        StatusBarNotification[] current = getActiveNotifications();
        if (current == null) return;
        for (StatusBarNotification sbn : current) {
            trackPosted(sbn);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        trackPosted(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Session session = active.remove(keyOf(sbn));
        if (session == null && sbn != null) {
            session = active.remove(sbn.getKey());
        }
        if (session == null) return;
        if (!RemoteConfigManager.get().whatsappAfterCallEnabled) return;
        String name = session.name;
        long startedAt = session.startedAt;
        MAIN.postDelayed(() -> AfterCallLauncher.openWhatsApp(getApplicationContext(), name, startedAt),
                OPEN_DELAY_MS);
    }

    private void trackPosted(StatusBarNotification sbn) {
        if (!RemoteConfigManager.get().whatsappAfterCallEnabled) return;
        if (!isWhatsAppCall(sbn)) return;
        String key = keyOf(sbn);
        Session existing = active.get(key);
        if (existing != null) {
            existing.name = preferName(existing.name, extractName(sbn));
            return;
        }
        Session session = new Session();
        session.startedAt = System.currentTimeMillis();
        session.name = extractName(sbn);
        active.put(key, session);
        if (!TextUtils.equals(key, sbn.getKey())) {
            active.put(sbn.getKey(), session);
        }
    }

    private static String keyOf(StatusBarNotification sbn) {
        return sbn.getPackageName() + "|" + sbn.getId() + "|" + String.valueOf(sbn.getTag());
    }

    private static boolean isWhatsAppCall(StatusBarNotification sbn) {
        if (sbn == null || !isWhatsAppPackage(sbn.getPackageName())) return false;
        Notification notification = sbn.getNotification();
        if (notification == null) return false;
        if (Notification.CATEGORY_CALL.equals(notification.category)) return true;
        String template = notification.extras == null ? null
                : notification.extras.getString(Notification.EXTRA_TEMPLATE);
        if (template != null && template.toLowerCase(Locale.ROOT).contains("call")) return true;
        if (NotificationCompat.getCategory(notification) != null
                && NotificationCompat.CATEGORY_CALL.equals(NotificationCompat.getCategory(notification))) {
            return true;
        }
        String channel = notification.getChannelId();
        if (channel != null) {
            String id = channel.toLowerCase(Locale.ROOT);
            if (id.contains("call") || id.contains("voip") || id.contains("ringing")
                    || id.contains("voice") || id.contains("video")) {
                return true;
            }
        }
        String text = combinedText(notification).toLowerCase(Locale.ROOT);
        if (text.contains("voice call")
                || text.contains("video call")
                || text.contains("ongoing call")
                || text.contains("incoming call")
                || text.contains("outgoing call")
                || text.contains("ringing")
                || text.contains("calling")
                || text.contains("whatsapp call")
                || text.contains("end-to-end")
                || text.contains("आवाज़")
                || text.contains("वीडियो")
                || text.contains("कॉल")
                || text.contains("مكالمة")
                || text.contains("chamada")
                || text.contains("llamada")
                || text.contains("appel")) {
            return true;
        }
        return (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0
                && (text.contains("call") || text.contains("voip") || TextUtils.isEmpty(text));
    }

    private static boolean isWhatsAppPackage(String packageName) {
        for (String name : PACKAGES) {
            if (name.equals(packageName)) return true;
        }
        return false;
    }

    @Nullable
    private static String extractName(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        if (notification == null || notification.extras == null) return null;
        CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        if (title == null) title = notification.extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
        if (title == null) return null;
        String value = title.toString().trim();
        if (value.isEmpty()) return null;
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("whatsapp") || lower.contains("voice call") || lower.contains("video call")
                || lower.contains("ongoing") || lower.contains("incoming") || lower.contains("ringing")
                || lower.contains("कॉल") || lower.contains("call")) {
            return null;
        }
        return value;
    }

    private static String combinedText(Notification notification) {
        if (notification.extras == null) return "";
        CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence big = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        CharSequence sub = notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
        StringBuilder builder = new StringBuilder();
        if (title != null) builder.append(title).append(' ');
        if (text != null) builder.append(text).append(' ');
        if (big != null) builder.append(big).append(' ');
        if (sub != null) builder.append(sub);
        return builder.toString();
    }

    @Nullable
    private static String preferName(@Nullable String current, @Nullable String next) {
        return TextUtils.isEmpty(current) ? next : current;
    }

    private static final class Session {
        long startedAt;
        @Nullable
        String name;
    }
}
