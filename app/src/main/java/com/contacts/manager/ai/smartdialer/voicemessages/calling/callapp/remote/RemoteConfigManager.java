package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsLog;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Remote config: cached JSON first, then one background refresh per process (re-fetched at most every
 * {@link #REFRESH_INTERVAL_MS}). If the backend is down the last valid cache, or safe defaults, are used.
 */
@MainThread
public final class RemoteConfigManager {

    public interface Listener {
        void onConfig(RemoteConfig config);
    }

    private static final String TAG = "AdsConfig";
    private static final String CONFIG_PATH = "api/app/config.php";
    private static final long REFRESH_INTERVAL_MS = 30 * 60 * 1000L;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final List<Listener> waiting = new ArrayList<>();

    private static RemoteConfig current = RemoteConfig.defaults();
    @Nullable
    private static JSONObject cachedJson;
    @Nullable
    private static Listener updateListener;
    private static boolean fetching;
    private static long lastFetchAt;

    private RemoteConfigManager() {
    }

    public static void init(@Nullable Listener onUpdated) {
        updateListener = onUpdated;
        String raw = StorageService.getRemoteConfigJson();
        if (raw != null) {
            try {
                cachedJson = new JSONObject(raw);
                current = RemoteConfig.fromJson(cachedJson);
                AdsLog.d(TAG, "Loaded cached config v" + current.configVersion);
            } catch (JSONException e) {
                cachedJson = null;
                AdsLog.d(TAG, "Cached config unreadable, using defaults");
            }
        }
        refresh();
    }

    public static RemoteConfig get() {
        return current;
    }

    /** The after-call screen key for this user's source; organic and marketing are configured separately. */
    public static String afterCallScreenKey() {
        return UserSourceManager.isMarketing()
                ? RemoteConfig.SCREEN_AFTER_CALL_MARKETING : RemoteConfig.SCREEN_AFTER_CALL_ORGANIC;
    }

    public static boolean isAfterCallScreenEnabled() {
        return current.isScreenEnabled(afterCallScreenKey());
    }

    public static void refresh() {
        refreshIfOlderThan(REFRESH_INTERVAL_MS);
    }

    public static void refreshIfOlderThan(long maxAgeMs) {
        boolean stale = lastFetchAt == 0 || SystemClock.elapsedRealtime() - lastFetchAt > maxAgeMs;
        if (fetching || !stale) return;
        fetching = true;
        AdsLog.d(TAG, "Fetching remote config");
        ApiClient.get(CONFIG_PATH, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject data) {
                finishFetch();
                apply(data);
                notifyWaiting();
            }

            @Override
            public void onError(String message) {
                finishFetch();
                AdsLog.d(TAG, "Remote config failed (" + message + "), using cache");
                notifyWaiting();
            }
        });
    }

    /**
     * Delivers the config once this process has finished one fetch attempt, or after {@code timeoutMs}
     * with whatever is available. Never waits longer than the timeout.
     */
    public static void whenReady(long timeoutMs, Listener listener) {
        if (lastFetchAt > 0 && !fetching) {
            listener.onConfig(current);
            return;
        }
        Listener once = new Listener() {
            private boolean delivered;

            @Override
            public void onConfig(RemoteConfig config) {
                if (delivered) return;
                delivered = true;
                waiting.remove(this);
                listener.onConfig(config);
            }
        };
        waiting.add(once);
        MAIN.postDelayed(() -> once.onConfig(current), timeoutMs);
        refresh();
    }

    private static void finishFetch() {
        fetching = false;
        lastFetchAt = SystemClock.elapsedRealtime();
    }

    private static void apply(JSONObject data) {
        int version = data.optInt("config_version", 0);
        if (version <= 0) {
            AdsLog.d(TAG, "Ignoring config without a valid config_version");
            return;
        }
        if (cachedJson != null && cachedJson.optInt("config_version", 0) == version) {
            AdsLog.d(TAG, "Config v" + version + " unchanged");
            return;
        }
        JSONObject merged = RemoteConfig.mergeValidSections(cachedJson, data);
        cachedJson = merged;
        current = RemoteConfig.fromJson(merged);
        StorageService.setRemoteConfigJson(merged.toString());
        AdsLog.d(TAG, "Applied config v" + version);
        if (updateListener != null) updateListener.onConfig(current);
    }

    private static void notifyWaiting() {
        for (Listener listener : new ArrayList<>(waiting)) {
            listener.onConfig(current);
        }
    }
}
