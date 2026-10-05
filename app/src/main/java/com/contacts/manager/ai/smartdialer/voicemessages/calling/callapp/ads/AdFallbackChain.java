package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.providers.AdProvider;

import java.util.List;

/**
 * Tries providers in order until one fills. Each attempt has its own timeout; a provider that
 * answers after its timeout (or after the chain was cancelled) has its ad destroyed, never shown.
 */
@MainThread
public final class AdFallbackChain {

    public interface Attempt<T> {
        void load(AdProvider provider, AdProvider.LoadCallback<T> callback);
    }

    public interface Discard<T> {
        void discard(T ad);
    }

    public interface Handle {
        void cancel();
    }

    public interface Result<T> {
        void onLoaded(T ad, String provider);

        void onFailed(String reason);
    }

    private static final String TAG = "AdsFallback";
    private static final long ATTEMPT_TIMEOUT_MS = 10_000L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AdFallbackChain() {
    }

    public static <T> Handle run(String placement, List<AdProvider> providers, Attempt<T> attempt,
                                 Discard<T> discard, Result<T> result) {
        Chain<T> chain = new Chain<>(placement, providers, attempt, discard, result);
        chain.next(0);
        return chain::cancel;
    }

    private static final class Chain<T> {
        private final String placement;
        private final List<AdProvider> providers;
        private final Attempt<T> attempt;
        private final Discard<T> discard;
        private final Result<T> result;
        private boolean cancelled;
        private int generation;
        private Runnable pendingTimeout;

        Chain(String placement, List<AdProvider> providers, Attempt<T> attempt, Discard<T> discard,
              Result<T> result) {
            this.placement = placement;
            this.providers = providers;
            this.attempt = attempt;
            this.discard = discard;
            this.result = result;
        }

        void cancel() {
            cancelled = true;
            generation++;
            clearTimeout();
        }

        void next(int index) {
            if (cancelled) return;
            if (index >= providers.size()) {
                AdsLog.d(TAG, "All providers failed for " + placement);
                result.onFailed("no fill");
                return;
            }
            AdProvider provider = providers.get(index);
            int token = ++generation;
            String name = provider.type().name();
            AdsLog.adRequest(placement, name);

            pendingTimeout = () -> {
                if (token != generation || cancelled) return;
                AdsLog.adFailed(placement, name, "timeout");
                generation++;
                next(index + 1);
            };
            MAIN.postDelayed(pendingTimeout, ATTEMPT_TIMEOUT_MS);

            AdProvider.LoadCallback<T> callback = new AdProvider.LoadCallback<T>() {
                private boolean settled;

                @Override
                public void onLoaded(T ad) {
                    MAIN.post(() -> {
                        if (settled) return;
                        settled = true;
                        if (token != generation || cancelled) {
                            AdsLog.d(TAG, "Late " + name + " response discarded for " + placement);
                            discard.discard(ad);
                            return;
                        }
                        generation++;
                        clearTimeout();
                        AdsLog.adLoaded(placement, name);
                        result.onLoaded(ad, name);
                    });
                }

                @Override
                public void onFailed(String reason) {
                    MAIN.post(() -> {
                        if (settled) return;
                        settled = true;
                        if (token != generation || cancelled) return;
                        generation++;
                        clearTimeout();
                        AdsLog.adFailed(placement, name, reason);
                        next(index + 1);
                    });
                }
            };
            try {
                attempt.load(provider, callback);
            } catch (RuntimeException e) {
                callback.onFailed(e.getClass().getSimpleName());
            }
        }

        private void clearTimeout() {
            if (pendingTimeout != null) {
                MAIN.removeCallbacks(pendingTimeout);
                pendingTimeout = null;
            }
        }
    }
}
