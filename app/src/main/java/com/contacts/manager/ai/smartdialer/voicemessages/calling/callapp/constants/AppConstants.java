package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants;

public final class AppConstants {

    public static final String PREFS_NAME = "contacts_prefs";
    public static final long SEARCH_DEBOUNCE_MS = 250;
    public static final int RECENT_CALL_LIMIT = 1500;
    public static final int DIALER_SUGGESTION_LIMIT = 30;
    public static final int RECENT_ACTIVITY_LIMIT = 3;

    public static final String CHANNEL_INCOMING = "incoming_calls";
    public static final String CHANNEL_ONGOING = "ongoing_calls";
    public static final String CHANNEL_MISSED = "missed_calls";
    public static final String CHANNEL_REMINDERS = "call_reminders";

    public static final int NOTIFICATION_CALL_ID = 1001;
    public static final int NOTIFICATION_MISSED_SUMMARY_ID = 2000;
    public static final String NOTIFICATION_MISSED_GROUP = "missed_calls_group";

    public static final String[] LANGUAGE_TAGS = {
            "en", "hi", "fr", "es", "de", "tr", "zh", "pt", "bn", "it", "ja", "ru", "vi"};
    public static final String[] LANGUAGE_NATIVE_NAMES = {
            "English", "हिंदी", "Français", "Español", "Deutsch", "Türkçe", "中文", "Português",
            "বাংলা", "Italiano", "日本語", "Русский", "Tiếng Việt"};
    public static final String[] LANGUAGE_ENGLISH_NAMES = {
            "English", "Hindi", "French", "Spanish", "German", "Turkish", "Chinese", "Portuguese",
            "Bengali", "Italian", "Japanese", "Russian", "Vietnamese"};

    public static final String PLAY_STORE_WEB = "https://play.google.com/store/apps/details?id=";
    public static final String PLAY_STORE_MARKET = "market://details?id=";

    private AppConstants() {
    }
}
