package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.providers.AdProvider;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.ui.FullscreenAdLoadingDialog;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;

import java.util.List;

/**
 * Walks the admin's fullscreen sequence (the position survives restarts), shows the "Ads Loading..."
 * card while loading, and gives up after {@link #LOADER_TIMEOUT_MS} so the user is never stuck.
 */
@MainThread
public final class FullscreenAdManager {

    private static final String TAG = "AdsFullscreen";
    static final long LOADER_TIMEOUT_MS = 9_000L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    @Nullable
    private static Flow active;
    private static long lastDismissedAt;

    private FullscreenAdManager() {
    }

    /** True while a fullscreen ad is loading or on screen. */
    public static boolean isBusy() {
        return active != null;
    }

    static long lastDismissedAt() {
        return lastDismissedAt;
    }

    static void showNext(Activity activity, String placement, Runnable onDone) {
        AdsConfig config = AdsManager.config();
        List<FullscreenType> sequence = config.fullscreenSequence;
        if (sequence.isEmpty()) {
            AdsLog.d(TAG, "Fullscreen sequence empty, skipping " + placement);
            onDone.run();
            return;
        }
        if (AdsManager.GUARD.isBusy(placement)) {
            AdsLog.d(TAG, "Duplicate request prevented for " + placement);
            return;
        }
        if (active != null) {
            onDone.run();
            return;
        }
        int index = Math.floorMod(StorageService.getFullscreenSequenceIndex(), sequence.size());
        FullscreenType item = sequence.get(index);
        StorageService.setFullscreenSequenceIndex((index + 1) % sequence.size());
        AdsLog.d(TAG, "Sequence position " + index + " -> " + item.key + " for " + placement);
        show(activity, placement, item, onDone);
    }

    static void show(Activity activity, String placement, FullscreenType item, Runnable onDone) {
        show(activity, placement, item, onDone, true);
    }

    private static void show(Activity activity, String placement, FullscreenType item, Runnable onDone,
                             boolean allowCustom) {
        AdsConfig config = AdsManager.config();
        List<AdProvider> providers = item == FullscreenType.CUSTOM
                ? AdsManager.customProviders(config)
                : AdsManager.providersFor(item.adType, config);
        if (!allowCustom) providers.removeIf(provider -> provider.type() == AdProviderType.CUSTOM);
        if (providers.isEmpty()) {
            AdsLog.d(TAG, "No provider for " + item.key + ", skipping " + placement);
            onDone.run();
            return;
        }
        if (active != null || activity.isFinishing() || activity.isDestroyed()) {
            onDone.run();
            return;
        }
        if (!AdsManager.GUARD.tryBeginRequest(placement)) return;
        AdType adType = item.adType != null ? item.adType : AdType.INTERSTITIAL;
        active = new Flow(activity, placement, onDone);
        active.start(providers, adType, config);
    }

    private static final class Flow implements DefaultLifecycleObserver {
        private final Activity activity;
        private final String placement;
        private final Runnable onDone;
        private final Runnable timeout = this::onTimeout;
        @Nullable
        private FullscreenAdLoadingDialog loader;
        @Nullable
        private AdFallbackChain.Handle handle;
        @Nullable
        private AdProvider.FullscreenAd ad;
        private boolean showing;
        private boolean finished;

        Flow(Activity activity, String placement, Runnable onDone) {
            this.activity = activity;
            this.placement = placement;
            this.onDone = onDone;
        }

        void start(List<AdProvider> providers, AdType adType, AdsConfig config) {
            if (activity instanceof LifecycleOwner) ((LifecycleOwner) activity).getLifecycle().addObserver(this);
            loader = FullscreenAdLoadingDialog.show(activity);
            MAIN.postDelayed(timeout, LOADER_TIMEOUT_MS);
            handle = AdFallbackChain.run(placement, providers,
                    (provider, callback) -> provider.loadFullscreen(activity, adType, config, callback),
                    AdProvider.FullscreenAd::destroy,
                    new AdFallbackChain.Result<AdProvider.FullscreenAd>() {
                        @Override
                        public void onLoaded(AdProvider.FullscreenAd loaded, String provider) {
                            onAdLoaded(loaded, provider);
                        }

                        @Override
                        public void onFailed(String reason) {
                            finish(true);
                        }
                    });
        }

        private void onAdLoaded(AdProvider.FullscreenAd loaded, String provider) {
            if (finished) {
                loaded.destroy();
                return;
            }
            MAIN.removeCallbacks(timeout);
            dismissLoader();
            if (!isAlive()) {
                loaded.destroy();
                finish(false);
                return;
            }
            ad = loaded;
            showing = true;
            AdsManager.GUARD.onLoaded(placement);
            AdsManager.GUARD.onShowing(placement);
            AdsLog.d(TAG, "Showing " + placement);
            loaded.show(activity, new AdProvider.ShowCallback() {
                @Override
                public void onShown() {
                    AdsLog.adImpression(placement, provider);
                }

                @Override
                public void onDismissed() {
                    AdsLog.d(TAG, "Dismissed " + placement);
                    lastDismissedAt = SystemClock.elapsedRealtime();
                    if (!AdProviderType.CUSTOM.name().equals(provider)) {
                        finish(true);
                        return;
                    }
                    finish(false);
                    if (!isAlive()) return;
                    AdsLog.d(TAG, "Custom tab closed, showing app open for " + placement);
                    MAIN.post(() -> {
                        if (isAlive()) show(activity, placement, FullscreenType.APP_OPEN, onDone, false);
                    });
                }

                @Override
                public void onFailedToShow(String reason) {
                    AdsLog.d(TAG, "Failed to show " + placement + ": " + reason);
                    finish(true);
                }
            });
        }

        private void onTimeout() {
            if (finished || showing) return;
            AdsLog.d(TAG, "Loader timeout for " + placement);
            finish(true);
        }

        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {
            if (showing) return;
            finish(false);
        }

        private void finish(boolean continueFlow) {
            if (finished) return;
            finished = true;
            MAIN.removeCallbacks(timeout);
            if (handle != null) handle.cancel();
            dismissLoader();
            if (activity instanceof LifecycleOwner) ((LifecycleOwner) activity).getLifecycle().removeObserver(this);
            if (ad != null) ad.destroy();
            ad = null;
            showing = false;
            AdsManager.GUARD.release(placement);
            if (active == this) active = null;
            if (continueFlow && isAlive()) onDone.run();
        }

        private void dismissLoader() {
            if (loader != null) loader.dismiss();
            loader = null;
        }

        private boolean isAlive() {
            return !activity.isFinishing() && !activity.isDestroyed();
        }
    }
}
