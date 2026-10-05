package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote;

import androidx.annotation.Nullable;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Typed, immutable remote configuration. Missing values fall back to defaults that never block the app. */
public final class RemoteConfig {

    public static final String SCREEN_WELCOME = "welcome";
    public static final String SCREEN_THEME_SELECTION = "theme_selection";
    public static final String SCREEN_LANGUAGE_SELECTION = "language_selection";
    public static final String SCREEN_AFTER_CALL_ORGANIC = "after_call_organic";
    public static final String SCREEN_AFTER_CALL_MARKETING = "after_call_marketing";
    public static final String SCREEN_LAUNCHER = "launcher";
    public static final String SCREEN_DEFAULT_PHONE = "default_phone";

    static final String[] SECTIONS = {"app", "force_update", "screens", "onboarding", "features", "marketing", "ads"};

    private static final List<String> INTRO_SCREENS = Arrays.asList(
            SCREEN_WELCOME, SCREEN_THEME_SELECTION, SCREEN_DEFAULT_PHONE, SCREEN_LANGUAGE_SELECTION);

    private static final Map<String, Boolean> DEFAULT_SCREENS = new HashMap<>();

    static {
        DEFAULT_SCREENS.put(SCREEN_WELCOME, true);
        DEFAULT_SCREENS.put(SCREEN_THEME_SELECTION, true);
        DEFAULT_SCREENS.put(SCREEN_LANGUAGE_SELECTION, false);
        DEFAULT_SCREENS.put(SCREEN_AFTER_CALL_ORGANIC, true);
        DEFAULT_SCREENS.put(SCREEN_AFTER_CALL_MARKETING, true);
    }

    public final int configVersion;
    public final String appVersion;
    public final String appLink;
    public final String privacyPolicyUrl;
    public final boolean forceUpdateEnabled;
    public final String minimumVersion;
    public final String forceUpdateMessage;
    public final boolean splashAfterFullscreenAd;
    public final boolean appOpenOnResume;
    public final boolean launcherEnabled;
    public final boolean inAppReviewEnabled;
    public final String facebookAppId;
    public final String facebookClientToken;
    public final AdsConfig ads;
    private final Map<String, Boolean> screens;
    private final List<String> introFlow;
    private final List<String> marketingIntroFlow;

    private RemoteConfig(JSONObject root) throws JSONException {
        configVersion = root.optInt("config_version", 0);

        JSONObject app = section(root, "app");
        appVersion = app.optString("app_version", "");
        appLink = app.optString("app_link", "").trim();
        privacyPolicyUrl = app.optString("privacy_policy", "").trim();

        JSONObject force = section(root, "force_update");
        forceUpdateEnabled = force.optBoolean("enabled", false);
        minimumVersion = force.optString("minimum_version", "").trim();
        forceUpdateMessage = force.optString("message", "");

        JSONObject screensJson = section(root, "screens");
        screens = new HashMap<>(DEFAULT_SCREENS);
        Iterator<String> keys = screensJson.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            screens.put(key, screensJson.optBoolean(key, Boolean.TRUE.equals(DEFAULT_SCREENS.get(key))));
        }

        JSONObject onboarding = section(root, "onboarding");
        introFlow = introFlow(onboarding.optJSONArray("organic"), legacyIntroFlow());
        marketingIntroFlow = introFlow(onboarding.optJSONArray("marketing"), introFlow);

        JSONObject features = section(root, "features");
        splashAfterFullscreenAd = features.optBoolean("splash_after_fullscreen_ad", false);
        appOpenOnResume = features.optBoolean("app_open_on_resume", false);
        launcherEnabled = features.optBoolean("launcher_enabled", false);
        inAppReviewEnabled = features.optBoolean("in_app_review", true);

        JSONObject marketing = section(root, "marketing");
        facebookAppId = marketing.optString("facebook_app_id", "").trim();
        facebookClientToken = marketing.optString("facebook_client_token", "").trim();

        ads = AdsConfig.fromJson(section(root, "ads"));
    }

    public static RemoteConfig defaults() {
        try {
            return new RemoteConfig(new JSONObject());
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    public static RemoteConfig fromJson(@Nullable JSONObject root) {
        if (root == null) return defaults();
        try {
            return new RemoteConfig(root);
        } catch (JSONException e) {
            return defaults();
        }
    }

    /** Screens without a remote switch (or unknown keys) are treated as enabled. */
    public boolean isScreenEnabled(String screenKey) {
        Boolean enabled = screens.get(screenKey);
        return enabled == null || enabled;
    }

    /** Onboarding screen keys in the admin's order for the user source; always contains the default phone screen. */
    public List<String> introFlow(boolean isMarketing) {
        return isMarketing ? marketingIntroFlow : introFlow;
    }

    private List<String> legacyIntroFlow() {
        List<String> flow = new ArrayList<>();
        for (String key : new String[]{SCREEN_LANGUAGE_SELECTION, SCREEN_WELCOME, SCREEN_THEME_SELECTION}) {
            if (isScreenEnabled(key)) flow.add(key);
        }
        flow.add(SCREEN_DEFAULT_PHONE);
        return Collections.unmodifiableList(flow);
    }

    private static List<String> introFlow(@Nullable JSONArray keys, List<String> fallback) {
        if (keys == null) return fallback;
        List<String> flow = new ArrayList<>();
        for (int i = 0; i < keys.length(); i++) {
            String key = keys.optString(i);
            if (INTRO_SCREENS.contains(key) && !flow.contains(key)) flow.add(key);
        }
        if (!flow.contains(SCREEN_DEFAULT_PHONE)) flow.add(SCREEN_DEFAULT_PHONE);
        return Collections.unmodifiableList(flow);
    }

    /**
     * Copies every well-formed section of {@code fresh} over {@code previous}. A malformed section keeps the
     * previous copy, so one bad value never wipes the rest of the cached configuration.
     */
    static JSONObject mergeValidSections(@Nullable JSONObject previous, JSONObject fresh) {
        JSONObject merged;
        try {
            merged = previous != null ? new JSONObject(previous.toString()) : new JSONObject();
        } catch (JSONException e) {
            merged = new JSONObject();
        }
        for (String key : SECTIONS) {
            if (!fresh.has(key)) continue;
            try {
                JSONObject single = new JSONObject().put(key, fresh.get(key));
                new RemoteConfig(single);
                merged.put(key, fresh.get(key));
            } catch (JSONException ignored) {
                // keep previous section
            }
        }
        try {
            merged.put("config_version", fresh.optInt("config_version", merged.optInt("config_version", 0)));
        } catch (JSONException ignored) {
        }
        return merged;
    }

    private static JSONObject section(JSONObject root, String key) throws JSONException {
        if (!root.has(key) || root.isNull(key)) return new JSONObject();
        return root.getJSONObject(key);
    }
}
