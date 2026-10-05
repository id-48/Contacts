package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads;

/** Request-guard keys: one per screen and slot, e.g. {@code "welcome:native_big"}. */
final class AdPlacement {

    static final String SPLASH_FULLSCREEN = "splash:fullscreen";
    static final String APP_OPEN_RESUME = "app_open:resume";

    private AdPlacement() {
    }

    static String banner(String screenKey) {
        return screenKey + ":banner";
    }

    static String nativeAd(String screenKey, boolean big) {
        return screenKey + (big ? ":native_big" : ":native_small");
    }

    static String fullscreen(String screenKey) {
        return screenKey + ":fullscreen";
    }
}
