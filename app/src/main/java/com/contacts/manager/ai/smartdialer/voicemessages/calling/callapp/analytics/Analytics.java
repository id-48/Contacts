package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.analytics;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Locale;

public final class Analytics {

    public static final String PARAM_SCREEN = "screen";
    public static final String PARAM_ELEMENT = "element";
    public static final String PARAM_LIST_POSITION = "list_position";
    public static final String PARAM_DIALOG = "dialog";

    private static final int MAX_EVENT_NAME = 40;
    private static final int MAX_USER_PROPERTY_VALUE = 36;

    private static FirebaseAnalytics analytics;
    private static FirebaseCrashlytics crashlytics;
    private static String currentScreen = "";

    private Analytics() {
    }

    public static void init(Context context) {
        analytics = FirebaseAnalytics.getInstance(context.getApplicationContext());
        crashlytics = FirebaseCrashlytics.getInstance();
        String userId = StorageService.getAnalyticsUserId();
        analytics.setUserId(userId);
        crashlytics.setUserId(userId);
    }

    public static String currentScreen() {
        return currentScreen;
    }

    public static void logScreen(@NonNull String screen, @NonNull String screenClass) {
        currentScreen = screen;
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.SCREEN_NAME, screen);
        params.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass);
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params);
        crashlytics.setCustomKey(PARAM_SCREEN, screen);
        crashlytics.log("screen_view " + screen);
    }

    public static void logEvent(@NonNull String name) {
        logEvent(name, null);
    }

    public static void logEvent(@NonNull String name, @Nullable Bundle params) {
        Bundle bundle = params == null ? new Bundle() : params;
        if (!bundle.containsKey(PARAM_SCREEN)) {
            bundle.putString(PARAM_SCREEN, currentScreen);
        }
        String event = eventName(name);
        analytics.logEvent(event, bundle);
        crashlytics.log(event);
    }

    public static void logClick(@NonNull String source, @NonNull String element, int listPosition) {
        Bundle params = new Bundle();
        params.putString(PARAM_SCREEN, currentScreen);
        params.putString(PARAM_ELEMENT, element);
        if (listPosition >= 0) {
            params.putLong(PARAM_LIST_POSITION, listPosition);
        }
        logEvent(source + "_" + element, params);
    }

    public static void setUserProperty(@NonNull String name, @Nullable String value) {
        if (value != null && value.length() > MAX_USER_PROPERTY_VALUE) {
            value = value.substring(0, MAX_USER_PROPERTY_VALUE);
        }
        analytics.setUserProperty(name, value);
        crashlytics.setCustomKey(name, value == null ? "" : value);
    }

    public static void recordException(@NonNull Throwable throwable) {
        crashlytics.recordException(throwable);
    }

    public static void setClickName(@NonNull View view, @NonNull String name) {
        view.setTag(R.id.analytics_name, name);
    }

    /** Call before {@link Dialog#show()} so the open event and every click inside the dialog are logged. */
    public static void trackDialog(@NonNull Dialog dialog, @NonNull String name) {
        Window window = dialog.getWindow();
        if (window == null || window.getCallback() instanceof TrackingWindowCallback) {
            return;
        }
        window.setCallback(new TrackingWindowCallback(window, window.getCallback(), name));
    }

    public static String screenName(@NonNull Class<?> type) {
        String name = type.getSimpleName();
        for (String suffix : new String[]{"Activity", "Fragment"}) {
            if (name.endsWith(suffix) && name.length() > suffix.length()) {
                name = name.substring(0, name.length() - suffix.length());
                break;
            }
        }
        return snakeCase(name);
    }

    static String snakeCase(@NonNull String value) {
        StringBuilder builder = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c) && i > 0 && !Character.isUpperCase(value.charAt(i - 1))
                    && value.charAt(i - 1) != '_') {
                builder.append('_');
            }
            builder.append(Character.toLowerCase(c));
        }
        return builder.toString();
    }

    private static String eventName(String raw) {
        String name = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_");
        name = name.replaceAll("^_+|_+$", "");
        if (name.isEmpty() || !Character.isLetter(name.charAt(0))) {
            name = "e_" + name;
        }
        if (name.startsWith("firebase_") || name.startsWith("google_") || name.startsWith("ga_")) {
            name = "app_" + name;
        }
        if (name.length() > MAX_EVENT_NAME) {
            name = name.substring(0, MAX_EVENT_NAME).replaceAll("_+$", "");
        }
        return name;
    }
}
