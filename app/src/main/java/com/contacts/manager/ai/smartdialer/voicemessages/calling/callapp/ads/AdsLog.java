package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads;

import android.os.Bundle;
import android.util.Log;

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
    }

    private static void track(String event, String placement, String provider) {
        Bundle params = new Bundle();
        params.putString(PARAM_AD_PLACEMENT, placement);
        params.putString(PARAM_AD_NETWORK, provider.toLowerCase(Locale.ROOT));
        Analytics.logEvent(event, params);
    }
}
