package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import android.os.Handler;
import android.os.Looper;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.BuildConfig;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsLog;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.ApiClient;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.UserSourceManager;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

/** Sends this device's Firebase Cloud Messaging token to the backend whenever it changes (retried on later launches). */
public final class PushTokenManager {

    private static final String TAG = "PushToken";
    private static final String TOKEN_PATH = "api/users/push_token.php";

    private PushTokenManager() {
    }

    public static void sync() {
        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnSuccessListener(PushTokenManager::register)
                    .addOnFailureListener(e -> AdsLog.d(TAG, "Token unavailable, will retry next launch"));
        } catch (RuntimeException e) {
            AdsLog.d(TAG, "Firebase Messaging unavailable");
        }
    }

    public static void register(String token) {
        new Handler(Looper.getMainLooper()).post(() -> send(token));
    }

    private static void send(String token) {
        if (token == null || token.isEmpty()) return;
        String deviceId = StorageService.getDeviceId();
        String registered = deviceId + "|" + token;
        if (registered.equals(StorageService.getRegisteredPushToken())) return;
        JSONObject body = new JSONObject();
        try {
            body.put("device_id", deviceId);
            body.put("previous_device_id", StorageService.getAnalyticsUserId());
            body.put("source", UserSourceManager.getSource());
            body.put("app_version", BuildConfig.VERSION_NAME);
            body.put("fcm_token", token);
        } catch (JSONException e) {
            return;
        }
        ApiClient.post(TOKEN_PATH, body, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject data) {
                StorageService.setRegisteredPushToken(registered);
            }

            @Override
            public void onError(String message) {
                AdsLog.d(TAG, "Token register failed (" + message + "), will retry next launch");
            }
        });
    }
}
