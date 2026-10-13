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

    static String nativeAd(String screenKey, NativeStyle style) {
        return screenKey + ":" + style.key;
    }

    static String listNative(String screenKey, int slot) {
        return screenKey + ":" + NativeStyle.SMALL_2.key + ":" + slot;
    }

    static String fullscreen(String screenKey) {
        return screenKey + ":fullscreen";
    }
}
