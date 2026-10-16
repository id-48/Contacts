package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads;

public enum NativeStyle {
    BIG("native_big"),
    SMALL("native_small"),
    SMALL_2("native_small2"),
    THEME_SMALL("native_theme_small");

    public final String key;

    NativeStyle(String key) {
        this.key = key;
    }
}
