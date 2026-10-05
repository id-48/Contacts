package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import androidx.annotation.NonNull;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.AppConstants;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class PushMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        PushTokenManager.register(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> data = message.getData();
        String title = value(data, "title");
        String body = value(data, "body");
        if (title.isEmpty() && message.getNotification() != null) {
            title = nonNull(message.getNotification().getTitle());
            body = nonNull(message.getNotification().getBody());
        }
        if (title.isEmpty()) return;
        AppNotifications.show(getApplicationContext(), AppConstants.NOTIFICATION_PUSH_BASE_ID + notificationNumber(data),
                title, body, value(data, "image"), value(data, "link"));
    }

    private static int notificationNumber(Map<String, String> data) {
        try {
            return Integer.parseInt(value(data, "id")) % 100_000;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String value(Map<String, String> data, String key) {
        return nonNull(data.get(key)).trim();
    }

    private static String nonNull(String value) {
        return value == null ? "" : value;
    }
}
