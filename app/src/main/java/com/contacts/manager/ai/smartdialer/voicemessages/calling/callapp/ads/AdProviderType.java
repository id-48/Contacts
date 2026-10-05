package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads;

import androidx.annotation.Nullable;

/** Providers in fallback order, used when the admin has not set a priority. */
public enum AdProviderType {
    ADMOB("admob"),
    TRADPLUS("tradplus"),
    ADX("adx"),
    CUSTOM("custom");

    public final String key;

    AdProviderType(String key) {
        this.key = key;
    }

    @Nullable
    public static AdProviderType fromKey(String key) {
        for (AdProviderType type : values()) {
            if (type.key.equals(key)) return type;
        }
        return null;
    }
}
