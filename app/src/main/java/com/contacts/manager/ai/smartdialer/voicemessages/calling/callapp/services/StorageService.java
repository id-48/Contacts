package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.AppConstants;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.PrefKeys;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class StorageService {

    private static SharedPreferences prefs;

    private StorageService() {
    }

    public static void init(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static int getThemeMode() {
        return prefs.getInt(PrefKeys.THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void setThemeMode(int mode) {
        prefs.edit().putInt(PrefKeys.THEME_MODE, mode).apply();
    }

    public static boolean isOnboardingDone() {
        return prefs.getBoolean(PrefKeys.ONBOARDING_DONE, false);
    }

    public static void setOnboardingDone(boolean done) {
        prefs.edit().putBoolean(PrefKeys.ONBOARDING_DONE, done).apply();
    }

    public static boolean isAfterCallEnabled() {
        return prefs.getBoolean(PrefKeys.AFTER_CALL_ENABLED, true);
    }

    public static void setAfterCallEnabled(boolean enabled) {
        prefs.edit().putBoolean(PrefKeys.AFTER_CALL_ENABLED, enabled).apply();
    }

    public static boolean isSmartNotificationsEnabled() {
        return prefs.getBoolean(PrefKeys.SMART_NOTIFICATIONS_ENABLED, true);
    }

    public static void setSmartNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(PrefKeys.SMART_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    public static boolean isBlockUnknownEnabled() {
        return prefs.getBoolean(PrefKeys.BLOCK_UNKNOWN, false);
    }

    public static void setBlockUnknownEnabled(boolean enabled) {
        prefs.edit().putBoolean(PrefKeys.BLOCK_UNKNOWN, enabled).apply();
    }

    public static boolean isDefaultBannerDismissed() {
        return prefs.getBoolean(PrefKeys.DEFAULT_BANNER_DISMISSED, false);
    }

    public static void setDefaultBannerDismissed(boolean dismissed) {
        prefs.edit().putBoolean(PrefKeys.DEFAULT_BANNER_DISMISSED, dismissed).apply();
    }

    public static boolean wasPermissionRequested(String key) {
        return prefs.getBoolean(PrefKeys.PERMISSION_REQUESTED_PREFIX + key, false);
    }

    public static void markPermissionRequested(String key) {
        prefs.edit().putBoolean(PrefKeys.PERMISSION_REQUESTED_PREFIX + key, true).apply();
    }

    public static List<String> getQuickResponses(Context context) {
        List<String> responses = getDefaultQuickResponses(context);
        responses.addAll(getCustomQuickResponses(context));
        return responses;
    }

    public static List<String> getDefaultQuickResponses(Context context) {
        return new ArrayList<>(Arrays.asList(context.getResources().getStringArray(R.array.default_quick_responses)));
    }

    public static List<String> getCustomQuickResponses(Context context) {
        String stored = prefs.getString(PrefKeys.CUSTOM_QUICK_RESPONSES, null);
        if (stored != null) {
            return parseList(stored);
        }
        String legacy = prefs.getString(PrefKeys.QUICK_RESPONSES, null);
        List<String> custom = new ArrayList<>();
        if (legacy != null) {
            List<String> defaults = new ArrayList<>();
            for (String response : getDefaultQuickResponses(context)) {
                defaults.add(normalizeResponse(response));
            }
            for (String response : parseList(legacy)) {
                if (!defaults.contains(normalizeResponse(response))) {
                    custom.add(response);
                }
            }
            setCustomQuickResponses(custom);
            prefs.edit().remove(PrefKeys.QUICK_RESPONSES).apply();
        }
        return custom;
    }

    public static void setCustomQuickResponses(List<String> responses) {
        JSONArray array = new JSONArray();
        for (String response : responses) {
            array.put(response);
        }
        prefs.edit().putString(PrefKeys.CUSTOM_QUICK_RESPONSES, array.toString()).apply();
    }

    private static List<String> parseList(String stored) {
        List<String> values = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(stored);
            for (int i = 0; i < array.length(); i++) {
                values.add(array.getString(i));
            }
        } catch (JSONException ignored) {
        }
        return values;
    }

    private static String normalizeResponse(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                builder.append(Character.toLowerCase(c));
            }
        }
        return builder.toString();
    }
}
