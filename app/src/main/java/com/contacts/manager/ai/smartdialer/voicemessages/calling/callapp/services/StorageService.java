package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.AppConstants;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.PrefKeys;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class StorageService {

    private static final String BROKEN_ANDROID_ID = "9774d56d682e549c";

    private static SharedPreferences prefs;
    private static Context appContext;

    private StorageService() {
    }

    public static void init(Context context) {
        appContext = context.getApplicationContext();
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

    public static int getMainOpenCount() {
        return prefs.getInt(PrefKeys.MAIN_OPEN_COUNT, 0);
    }

    public static void incrementMainOpenCount() {
        prefs.edit().putInt(PrefKeys.MAIN_OPEN_COUNT, getMainOpenCount() + 1).apply();
    }

    public static boolean isReviewPrompted() {
        return prefs.getBoolean(PrefKeys.REVIEW_PROMPTED, false);
    }

    public static void setReviewPrompted() {
        prefs.edit().putBoolean(PrefKeys.REVIEW_PROMPTED, true).apply();
    }

    public static Set<String> getOnboardingStepsDone() {
        return new HashSet<>(prefs.getStringSet(PrefKeys.ONBOARDING_STEPS_DONE, Collections.emptySet()));
    }

    public static void markOnboardingStepDone(String step) {
        Set<String> done = getOnboardingStepsDone();
        done.add(step);
        prefs.edit().putStringSet(PrefKeys.ONBOARDING_STEPS_DONE, done).apply();
    }

    public static int nextLocalNotificationIndex(int count) {
        int index = Math.floorMod(prefs.getInt(PrefKeys.LOCAL_NOTIFICATION_INDEX, 0), count);
        prefs.edit().putInt(PrefKeys.LOCAL_NOTIFICATION_INDEX, (index + 1) % count).apply();
        return index;
    }

    @Nullable
    public static String getRegisteredPushToken() {
        return prefs.getString(PrefKeys.PUSH_TOKEN_REGISTERED, null);
    }

    public static void setRegisteredPushToken(String token) {
        prefs.edit().putString(PrefKeys.PUSH_TOKEN_REGISTERED, token).apply();
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

    public static String getAnalyticsUserId() {
        String id = prefs.getString(PrefKeys.ANALYTICS_USER_ID, null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString(PrefKeys.ANALYTICS_USER_ID, id).apply();
        }
        return id;
    }

    /** Stays the same across reinstalls on the same device, so one device is counted as one user. */
    public static String getDeviceId() {
        String androidId = Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null || androidId.length() < 8 || BROKEN_ANDROID_ID.equals(androidId)) {
            return getAnalyticsUserId();
        }
        return androidId;
    }

    @Nullable
    public static String getRegisteredDeviceId() {
        return prefs.getString(PrefKeys.REGISTERED_DEVICE_ID, null);
    }

    public static void setRegisteredDeviceId(String deviceId) {
        prefs.edit().putString(PrefKeys.REGISTERED_DEVICE_ID, deviceId).apply();
    }

    public static long getAfterCallOpenCount() {
        return prefs.getLong(PrefKeys.AFTER_CALL_OPEN_COUNT, 0);
    }

    public static long getAfterCallOpenCountToday() {
        return prefs.getInt(PrefKeys.AFTER_CALL_OPEN_DAY, 0) == today()
                ? prefs.getLong(PrefKeys.AFTER_CALL_OPEN_DAY_COUNT, 0) : 0;
    }

    public static void incrementAfterCallOpenCount() {
        prefs.edit()
                .putLong(PrefKeys.AFTER_CALL_OPEN_COUNT, getAfterCallOpenCount() + 1)
                .putInt(PrefKeys.AFTER_CALL_OPEN_DAY, today())
                .putLong(PrefKeys.AFTER_CALL_OPEN_DAY_COUNT, getAfterCallOpenCountToday() + 1)
                .apply();
    }

    public static String getRemoteConfigJson() {
        return prefs.getString(PrefKeys.REMOTE_CONFIG_JSON, null);
    }

    public static void setRemoteConfigJson(String json) {
        prefs.edit().putString(PrefKeys.REMOTE_CONFIG_JSON, json).apply();
    }

    public static String getUserSource() {
        return prefs.getString(PrefKeys.USER_SOURCE, AppConstants.SOURCE_ORGANIC);
    }

    public static boolean isUserSourceResolved() {
        return prefs.getBoolean(PrefKeys.USER_SOURCE_RESOLVED, false);
    }

    public static void setUserSource(String source) {
        prefs.edit()
                .putString(PrefKeys.USER_SOURCE, source)
                .putBoolean(PrefKeys.USER_SOURCE_RESOLVED, true)
                .apply();
    }

    public static String getRegisteredUserSource() {
        return prefs.getString(PrefKeys.USER_SOURCE_REGISTERED, null);
    }

    public static void setRegisteredUserSource(String source) {
        prefs.edit().putString(PrefKeys.USER_SOURCE_REGISTERED, source).apply();
    }

    public static int getFullscreenSequenceIndex() {
        return prefs.getInt(PrefKeys.FULLSCREEN_SEQUENCE_INDEX, 0);
    }

    public static void setFullscreenSequenceIndex(int index) {
        prefs.edit().putInt(PrefKeys.FULLSCREEN_SEQUENCE_INDEX, index).apply();
    }

    public static int getPriorityPosition(String key) {
        return prefs.getInt(PrefKeys.PRIORITY_POSITION_PREFIX + key, 0);
    }

    public static void setPriorityPosition(String key, int position) {
        prefs.edit().putInt(PrefKeys.PRIORITY_POSITION_PREFIX + key, position).apply();
    }

    public static String getSimPreference() {
        return prefs.getString(PrefKeys.SIM_PREFERENCE, "");
    }

    public static void setSimPreference(String accountId) {
        prefs.edit().putString(PrefKeys.SIM_PREFERENCE, accountId == null ? "" : accountId).apply();
    }

    @Nullable
    public static String getSpeedDial(int digit) {
        return prefs.getString(PrefKeys.SPEED_DIAL_PREFIX + digit, null);
    }

    public static void setSpeedDial(int digit, @Nullable String json) {
        if (json == null) {
            prefs.edit().remove(PrefKeys.SPEED_DIAL_PREFIX + digit).apply();
        } else {
            prefs.edit().putString(PrefKeys.SPEED_DIAL_PREFIX + digit, json).apply();
        }
    }

    public static boolean isFlashOnCall() {
        return prefs.getBoolean(PrefKeys.FLASH_ON_CALL, false);
    }

    public static void setFlashOnCall(boolean enabled) {
        prefs.edit().putBoolean(PrefKeys.FLASH_ON_CALL, enabled).apply();
    }

    public static boolean isAnswerOnLeft() {
        return prefs.getInt(PrefKeys.ANSWER_POSITION, 0) == 1;
    }

    public static void setAnswerOnLeft(boolean left) {
        prefs.edit().putInt(PrefKeys.ANSWER_POSITION, left ? 1 : 0).apply();
    }

    public static int getCallStyle() {
        return prefs.getInt(PrefKeys.CALL_STYLE, 1);
    }

    public static void setCallStyle(int style) {
        prefs.edit().putInt(PrefKeys.CALL_STYLE, style).apply();
    }

    @Nullable
    public static String getCallWallpaper() {
        return prefs.getString(PrefKeys.CALL_WALLPAPER, null);
    }

    public static void setCallWallpaper(@Nullable String path) {
        prefs.edit().putString(PrefKeys.CALL_WALLPAPER, path).apply();
    }

    public static boolean isCallAnnouncer() {
        return prefs.getBoolean(PrefKeys.CALL_ANNOUNCER, false);
    }

    public static void setCallAnnouncer(boolean enabled) {
        prefs.edit().putBoolean(PrefKeys.CALL_ANNOUNCER, enabled).apply();
    }

    public static String getFakeCallsJson() {
        return prefs.getString(PrefKeys.FAKE_CALLS, "[]");
    }

    public static void setFakeCallsJson(String json) {
        prefs.edit().putString(PrefKeys.FAKE_CALLS, json).commit();
    }

    public static boolean isAppLockEnabled() {
        return prefs.getBoolean(PrefKeys.APP_LOCK_ENABLED, false) && getPasscodeHash() != null;
    }

    public static void setAppLockEnabled(boolean enabled) {
        prefs.edit().putBoolean(PrefKeys.APP_LOCK_ENABLED, enabled).apply();
    }

    @Nullable
    public static String getPasscodeHash() {
        return prefs.getString(PrefKeys.PASSCODE_HASH, null);
    }

    @Nullable
    public static String getPasscodeSalt() {
        return prefs.getString(PrefKeys.PASSCODE_SALT, null);
    }

    public static void setPasscode(String salt, String hash) {
        prefs.edit().putString(PrefKeys.PASSCODE_SALT, salt).putString(PrefKeys.PASSCODE_HASH, hash).apply();
    }

    @Nullable
    public static String getSecurityQuestion() {
        return prefs.getString(PrefKeys.SECURITY_QUESTION, null);
    }

    @Nullable
    public static String getSecurityAnswerHash() {
        return prefs.getString(PrefKeys.SECURITY_ANSWER_HASH, null);
    }

    public static void setSecurityQuestion(String question, String answerHash) {
        prefs.edit().putString(PrefKeys.SECURITY_QUESTION, question)
                .putString(PrefKeys.SECURITY_ANSWER_HASH, answerHash).apply();
    }

    public static Set<String> getVaultKeys() {
        return new HashSet<>(prefs.getStringSet(PrefKeys.VAULT_KEYS, Collections.emptySet()));
    }

    public static void setVaultKeys(Set<String> keys) {
        prefs.edit().putStringSet(PrefKeys.VAULT_KEYS, new HashSet<>(keys)).apply();
    }

    public static boolean isSpamShieldEnabled() {
        return prefs.getBoolean(PrefKeys.SPAM_SHIELD, false);
    }

    public static void setSpamShieldEnabled(boolean enabled) {
        prefs.edit().putBoolean(PrefKeys.SPAM_SHIELD, enabled).apply();
    }

    public static boolean isSortByLastName() {
        return prefs.getInt(PrefKeys.SORT_ORDER, 0) == 1;
    }

    public static void setSortByLastName(boolean lastName) {
        prefs.edit().putInt(PrefKeys.SORT_ORDER, lastName ? 1 : 0).apply();
    }

    public static boolean isLastNameFirst() {
        return prefs.getInt(PrefKeys.NAME_FORMAT, 0) == 1;
    }

    public static void setLastNameFirst(boolean lastFirst) {
        prefs.edit().putInt(PrefKeys.NAME_FORMAT, lastFirst ? 1 : 0).apply();
    }

    public static boolean isDialpadSoundEnabled() {
        return prefs.getBoolean(PrefKeys.DIALPAD_SOUND, true);
    }

    public static void setDialpadSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(PrefKeys.DIALPAD_SOUND, enabled).apply();
    }

    public static boolean isVibrateOnAnswer() {
        return prefs.getBoolean(PrefKeys.VIBRATE_ON_ANSWER, true);
    }

    public static void setVibrateOnAnswer(boolean enabled) {
        prefs.edit().putBoolean(PrefKeys.VIBRATE_ON_ANSWER, enabled).apply();
    }

    private static int today() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.YEAR) * 10000 + (now.get(Calendar.MONTH) + 1) * 100 + now.get(Calendar.DAY_OF_MONTH);
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
