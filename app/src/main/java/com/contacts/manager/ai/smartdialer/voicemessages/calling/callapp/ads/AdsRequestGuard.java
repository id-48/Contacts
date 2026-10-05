package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads;

import androidx.annotation.MainThread;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-placement request state so the same placement is never requested twice at once (double
 * clicks, rotation, resume races). Placement keys look like {@code "welcome:banner"}.
 */
@MainThread
final class AdsRequestGuard {

    private static final String TAG = "AdsGuard";

    private static final class State {
        boolean isLoading;
        boolean isLoaded;
        boolean isShowing;
    }

    private final Map<String, State> states = new HashMap<>();

    /** Returns false (and logs) when the placement is already loading, loaded or showing. */
    boolean tryBeginRequest(String placement) {
        State state = states.get(placement);
        if (state != null && (state.isLoading || state.isLoaded || state.isShowing)) {
            AdsLog.d(TAG, "Duplicate request prevented for " + placement);
            return false;
        }
        state = new State();
        state.isLoading = true;
        states.put(placement, state);
        return true;
    }

    void onLoaded(String placement) {
        State state = states.get(placement);
        if (state == null) return;
        state.isLoading = false;
        state.isLoaded = true;
    }

    void onShowing(String placement) {
        State state = states.get(placement);
        if (state == null) return;
        state.isLoading = false;
        state.isShowing = true;
    }

    /** Ad failed, was dismissed/consumed, or its screen went away: the placement may request again. */
    void release(String placement) {
        states.remove(placement);
    }

    boolean isBusy(String placement) {
        return states.containsKey(placement);
    }
}
