package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Minimal JSON-over-HTTP client for the admin backend. Callbacks run on the main thread. */
public final class ApiClient {

    public interface Callback {
        void onSuccess(JSONObject json);

        void onError(String message);
    }

    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 8000;
    private static final int MAX_RESPONSE_BYTES = 512 * 1024;
    private static final String PLACEHOLDER_HOST = "your-domain.com";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ApiClient() {
    }

    public static boolean isConfigured() {
        return !BuildConfig.API_BASE_URL.contains(PLACEHOLDER_HOST);
    }

    /** Backend API call; delivers the {@code data} object of a successful response. */
    public static void get(String path, Callback callback) {
        request("GET", backendUrl(path), null, callback);
    }

    public static void post(String path, JSONObject body, Callback callback) {
        request("POST", backendUrl(path), body, callback);
    }

    private static String backendUrl(String path) {
        String base = BuildConfig.API_BASE_URL;
        return (base.endsWith("/") ? base : base + "/") + path;
    }

    private static void request(String method, String url, @Nullable JSONObject body, Callback callback) {
        if (!isConfigured()) {
            deliverError(callback, "API base URL is not configured");
            return;
        }
        EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "application/json");
                if (body != null) {
                    byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    connection.setFixedLengthStreamingMode(bytes.length);
                    try (OutputStream out = connection.getOutputStream()) {
                        out.write(bytes);
                    }
                }
                int code = connection.getResponseCode();
                InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
                String text = stream == null ? "" : read(stream);
                JSONObject json = new JSONObject(text);
                if (code < 400 && json.optBoolean("success", false)) {
                    JSONObject data = json.optJSONObject("data");
                    deliverSuccess(callback, data != null ? data : new JSONObject());
                } else {
                    deliverError(callback, json.optString("message", "HTTP " + code));
                }
            } catch (IOException | JSONException | RuntimeException e) {
                deliverError(callback, e.getClass().getSimpleName());
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private static String read(InputStream stream) throws IOException {
        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int count;
            while ((count = in.read(buffer)) != -1) {
                total += count;
                if (total > MAX_RESPONSE_BYTES) throw new IOException("Response too large");
                out.write(buffer, 0, count);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static void deliverSuccess(@Nullable Callback callback, JSONObject json) {
        if (callback != null) MAIN.post(() -> callback.onSuccess(json));
    }

    private static void deliverError(@Nullable Callback callback, String message) {
        if (callback != null) MAIN.post(() -> callback.onError(message));
    }
}
