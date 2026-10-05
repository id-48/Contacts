package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads;

import androidx.annotation.Nullable;

/** Ad unit formats as named by the backend. */
public enum AdType {
    BANNER("banner"),
    NATIVE("native"),
    INTERSTITIAL("interstitial"),
    REWARD("reward"),
    APP_OPEN("app_open");

    public final String key;

    AdType(String key) {
        this.key = key;
    }

    @Nullable
    public static AdType fromKey(String key) {
        for (AdType type : values()) {
            if (type.key.equals(key)) return type;
        }
        return null;
    }
}
