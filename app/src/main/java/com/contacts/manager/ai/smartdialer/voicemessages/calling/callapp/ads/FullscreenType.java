package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads;

import androidx.annotation.Nullable;

/** Items of the admin-configured fullscreen sequence. */
public enum FullscreenType {
    INTERSTITIAL("interstitial", AdType.INTERSTITIAL),
    REWARD("reward", AdType.REWARD),
    APP_OPEN("app_open", AdType.APP_OPEN),
    CUSTOM("custom", null);

    public final String key;
    /** Network ad type to request, or null for the custom-only item. */
    @Nullable
    public final AdType adType;

    FullscreenType(String key, @Nullable AdType adType) {
        this.key = key;
        this.adType = adType;
    }

    @Nullable
    public static FullscreenType fromKey(String key) {
        for (FullscreenType type : values()) {
            if (type.key.equals(key)) return type;
        }
        return null;
    }
}
