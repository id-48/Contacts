package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.BuildConfig;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.analytics.Analytics;

import java.util.Locale;

/** Debug-only logging for the ads/config system, plus the ad funnel events sent to Firebase. Never pass secrets or personal data. */
public final class AdsLog {

    public static final String TAG = "AdManagerContact";
    private static final String PARAM_AD_PLACEMENT = "ad_placement";
    private static final String PARAM_AD_NETWORK = "ad_network";

    private AdsLog() {
    }

    public static void d(String source, String message) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[" + source + "] " + message);
        }
    }

    public static void adRequest(String placement, String provider) {
        d("Ads", "Ad Request | " + placement + " | " + provider);
        track("ad_request", placement, provider);
        trackFunnel(placement, "req", provider);
    }

    public static void adLoaded(String placement, String provider) {
        d("Ads", "Ad Loaded | " + placement + " | " + provider);
        track("ad_loaded", placement, provider);
    }

    public static void adFailed(String placement, String provider, String reason) {
        d("Ads", "Ad Failed | " + placement + " | " + provider + " | " + reason);
    }

    public static void adImpression(String placement, String provider) {
        d("Ads", "Ad Impression | " + placement + " | " + provider);
        track("ad_impression", placement, provider);
        trackFunnel(placement, "impression", provider);
    }

    public static void adClick(String placement, String provider) {
        d("Ads", "Ad Click | " + placement + " | " + provider);
        track("ad_click", placement, provider);
        trackFunnel(placement, "click", provider);
    }

    private static void track(String event, String placement, String provider) {
        Bundle params = new Bundle();
        params.putString(PARAM_AD_PLACEMENT, placement);
        params.putString(PARAM_AD_NETWORK, provider.toLowerCase(Locale.ROOT));
        Analytics.logEvent(event, params);
    }

    private static void trackFunnel(String placement, String kind, String provider) {
        String prefix = funnelPrefix(placement);
        if (prefix == null) return;
        track(prefix + "_" + kind, placement, provider);
    }

    @Nullable
    private static String funnelPrefix(String placement) {
        if (placement == null) return null;
        if (placement.equals("after_call_close") || placement.startsWith("after_call_close")) {
            return "after_call_close_ad";
        }
        if (placement.equals("wp_call_end_screen") || placement.startsWith("wp_call_end_screen:")) {
            return "wp_call_end_screen_ad";
        }
        if (placement.equals("after_call") || placement.startsWith("after_call:")) {
            return "after_call_ad";
        }
        return null;
    }
}
