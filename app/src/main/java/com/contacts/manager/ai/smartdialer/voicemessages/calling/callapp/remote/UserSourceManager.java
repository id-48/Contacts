package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.BuildConfig;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsLog;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.AppConstants;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Classifies the install as organic or marketing from the Play Install Referrer (read once), stores it
 * locally and registers it with the backend (retried on later launches until it succeeds).
 */
public final class UserSourceManager {

    private static final String TAG = "UserSource";
    private static final String REGISTER_PATH = "api/users/register_or_update.php";

    private static final Set<String> PAID_MEDIUMS = new HashSet<>(Arrays.asList(
            "cpc", "ppc", "cpm", "cpi", "paid", "ads", "ad", "display", "paidsocial", "paid_social", "social_paid"));
    private static final Set<String> ORGANIC_SOURCES = new HashSet<>(Arrays.asList(
            "", "google-play", "play-store", "organic", "(not set)", "(direct)", "direct"));
    private static final String[] CLICK_ID_PARAMS = {"gclid", "gbraid", "wbraid", "fbclid", "ttclid"};
    private static final String[] MARKETING_TOKENS = {"fb4a", "ig4a", "apps.facebook.com", "apps.instagram.com"};

    private static boolean detecting;

    private UserSourceManager() {
    }

    public static String getSource() {
        return StorageService.getUserSource();
    }

    public static boolean isMarketing() {
        return AppConstants.SOURCE_MARKETING.equals(getSource());
    }

    public static void detectOnce(Context context) {
        if (StorageService.isUserSourceResolved()) {
            registerIfNeeded();
            return;
        }
        if (detecting) return;
        detecting = true;
        Context app = context.getApplicationContext();
        InstallReferrerClient client = InstallReferrerClient.newBuilder(app).build();
        try {
            client.startConnection(new InstallReferrerStateListener() {
                @Override
                public void onInstallReferrerSetupFinished(int responseCode) {
                    String referrer = null;
                    boolean permanent = responseCode == InstallReferrerClient.InstallReferrerResponse.OK
                            || responseCode == InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED;
                    if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                        try {
                            ReferrerDetails details = client.getInstallReferrer();
                            referrer = details != null ? details.getInstallReferrer() : null;
                        } catch (Exception ignored) {
                        }
                    }
                    endQuietly(client);
                    String source = classify(referrer);
                    postMain(() -> finish(source, permanent));
                }

                @Override
                public void onInstallReferrerServiceDisconnected() {
                    endQuietly(client);
                    postMain(() -> finish(AppConstants.SOURCE_ORGANIC, false));
                }
            });
        } catch (Exception e) {
            finish(AppConstants.SOURCE_ORGANIC, false);
        }
    }

    /**
     * Marketing when the referrer carries an ad click ID, a paid medium, a non-organic utm_source or a
     * Meta campaign token; organic otherwise (including a missing referrer).
     */
    public static String classify(@Nullable String referrer) {
        if (referrer == null || referrer.trim().isEmpty()) return AppConstants.SOURCE_ORGANIC;
        String decoded;
        try {
            decoded = URLDecoder.decode(referrer, "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            decoded = referrer;
        }
        decoded = decoded.toLowerCase(Locale.ROOT);
        for (String token : MARKETING_TOKENS) {
            if (decoded.contains(token)) return AppConstants.SOURCE_MARKETING;
        }
        String source = "";
        String medium = "";
        for (String pair : decoded.split("&")) {
            int eq = pair.indexOf('=');
            String key = (eq >= 0 ? pair.substring(0, eq) : pair).trim();
            String value = eq >= 0 ? pair.substring(eq + 1).trim() : "";
            for (String clickId : CLICK_ID_PARAMS) {
                if (clickId.equals(key) && !value.isEmpty()) return AppConstants.SOURCE_MARKETING;
            }
            if ("utm_source".equals(key)) source = value;
            if ("utm_medium".equals(key)) medium = value;
        }
        if (PAID_MEDIUMS.contains(medium)) return AppConstants.SOURCE_MARKETING;
        if (!ORGANIC_SOURCES.contains(source) && !"organic".equals(medium)) return AppConstants.SOURCE_MARKETING;
        return AppConstants.SOURCE_ORGANIC;
    }

    private static void finish(String source, boolean permanent) {
        detecting = false;
        if (permanent) {
            StorageService.setUserSource(source);
            AdsLog.d(TAG, "Install source: " + source);
            registerIfNeeded();
        } else {
            // Register as organic for now; a later marketing result re-registers and the backend upgrades it.
            AdsLog.d(TAG, "Install referrer unavailable, will retry next launch");
            registerIfNeeded();
        }
    }

    private static void registerIfNeeded() {
        String source = getSource();
        String deviceId = StorageService.getDeviceId();
        if (source.equals(StorageService.getRegisteredUserSource())
                && deviceId.equals(StorageService.getRegisteredDeviceId())) return;
        JSONObject body = new JSONObject();
        try {
            body.put("device_id", deviceId);
            body.put("previous_device_id", StorageService.getAnalyticsUserId());
            body.put("source", source);
            body.put("app_version", BuildConfig.VERSION_NAME);
        } catch (JSONException e) {
            return;
        }
        ApiClient.post(REGISTER_PATH, body, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject json) {
                StorageService.setRegisteredUserSource(source);
                StorageService.setRegisteredDeviceId(deviceId);
            }

            @Override
            public void onError(String message) {
                AdsLog.d(TAG, "Register failed (" + message + "), will retry next launch");
            }
        });
    }

    private static void endQuietly(InstallReferrerClient client) {
        try {
            client.endConnection();
        } catch (Exception ignored) {
        }
    }

    private static void postMain(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
}
