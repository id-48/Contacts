package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote;

import android.app.Application;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.BuildConfig;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsLog;
import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.appevents.AppEventsLogger;

/** Starts Audience Network on launch and the Facebook SDK with the App ID / Client Token from the admin panel. */
public final class FacebookInitializer {

    private static final String TAG = "Marketing";
    private static boolean audienceNetworkInitialized;
    private static boolean initialized;

    private FacebookInitializer() {
    }

    public static void initAudienceNetwork(Application app) {
        if (audienceNetworkInitialized) return;
        try {
            AudienceNetworkAds.initialize(app);
            audienceNetworkInitialized = true;
        } catch (RuntimeException e) {
            AdsLog.d(TAG, "Audience Network init failed: " + e.getClass().getSimpleName());
        }
    }

    @SuppressWarnings("deprecation")
    public static void init(Application app, RemoteConfig config) {
        String appId = config.facebookAppId;
        if (initialized || appId.isEmpty()) return;
        try {
            FacebookSdk.setApplicationId(appId);
            if (!config.facebookClientToken.isEmpty()) {
                FacebookSdk.setClientToken(config.facebookClientToken);
            }
            FacebookSdk.sdkInitialize(app);
            FacebookSdk.setAutoInitEnabled(true);
            FacebookSdk.fullyInitialize();
            FacebookSdk.setAutoLogAppEventsEnabled(true);
            FacebookSdk.setAdvertiserIDCollectionEnabled(true);
            if (BuildConfig.DEBUG) {
                FacebookSdk.setIsDebugEnabled(true);
                FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS);
            }
            AppEventsLogger.activateApp(app, appId);
            initialized = true;
            AdsLog.d(TAG, "Facebook SDK initialized");
        } catch (RuntimeException e) {
            AdsLog.d(TAG, "Facebook SDK init failed: " + e.getClass().getSimpleName());
        }
    }
}
