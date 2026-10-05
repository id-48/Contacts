package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.providers;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.BuildConfig;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdProviderType;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdType;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsConfig;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsLog;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.AppConstants;
import com.facebook.ads.AdSettings;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeBannerAd;
import com.tradplus.ads.base.bean.TPAdError;
import com.tradplus.ads.base.bean.TPAdInfo;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.mgr.nativead.TPCustomNativeAd;
import com.tradplus.ads.open.LoadAdEveryLayerListener;
import com.tradplus.ads.open.TradPlusSdk;
import com.tradplus.ads.open.banner.BannerAdListener;
import com.tradplus.ads.open.banner.TPBanner;
import com.tradplus.ads.open.interstitial.InterstitialAdListener;
import com.tradplus.ads.open.interstitial.TPInterstitial;
import com.tradplus.ads.open.nativead.NativeAdListener;
import com.tradplus.ads.open.nativead.TPNative;
import com.tradplus.ads.open.reward.RewardAdListener;
import com.tradplus.ads.open.reward.TPReward;
import com.tradplus.ads.open.splash.SplashAdListener;
import com.tradplus.ads.open.splash.TPSplash;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TradPlus mediation. Requests made before the SDK finishes initializing are queued, and native
 * loads for the same unit run one at a time because TradPlus drops a second concurrent load.
 */
public final class TradPlusProvider implements AdProvider {

    private static final String TAG = "AdsTradPlus";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final int BUSY_RETRIES = 4;
    private static final long BUSY_RETRY_MS = 1_500L;
    private static final List<Runnable> pending = new ArrayList<>();
    private static final Map<String, ArrayDeque<Runnable>> nativeQueues = new HashMap<>();
    private static boolean initStarted;
    private static boolean initReady;

    public static void init(Application app) {
        if (initStarted) return;
        initStarted = true;
        if (BuildConfig.DEBUG) AdSettings.setTestMode(true);
        try {
            if (TradPlusSdk.getIsInit()) {
                onInitDone();
                return;
            }
            TradPlusSdk.setTradPlusInitListener(TradPlusProvider::onInitDone);
            TradPlusSdk.initSdk(app, AppConstants.TRADPLUS_APP_ID);
            if (TradPlusSdk.getIsInit()) onInitDone();
        } catch (RuntimeException e) {
            AdsLog.d(TAG, "Init failed: " + e.getClass().getSimpleName());
        }
    }

    private static void onInitDone() {
        List<Runnable> queued;
        synchronized (pending) {
            if (initReady) return;
            initReady = true;
            queued = new ArrayList<>(pending);
            pending.clear();
        }
        AdsLog.d(TAG, "SDK initialized");
        for (Runnable action : queued) action.run();
    }

    private static void whenReady(Runnable action) {
        synchronized (pending) {
            if (!initReady && !TradPlusSdk.getIsInit()) {
                pending.add(action);
                return;
            }
        }
        action.run();
    }

    private static String message(@Nullable TPAdError error) {
        return error == null ? "unknown" : error.getErrorCode() + " " + error.getErrorMsg();
    }

    private static String network(@Nullable TPAdInfo info) {
        return info == null || info.adSourceName == null ? "unknown" : info.adSourceName;
    }

    private static void runNative(String unitId, Runnable load) {
        ArrayDeque<Runnable> queue = nativeQueues.get(unitId);
        if (queue != null) {
            queue.add(load);
            return;
        }
        nativeQueues.put(unitId, new ArrayDeque<>());
        load.run();
    }

    private static void nativeDone(String unitId) {
        ArrayDeque<Runnable> queue = nativeQueues.get(unitId);
        if (queue == null) return;
        Runnable next = queue.poll();
        if (next == null) nativeQueues.remove(unitId);
        else next.run();
    }

    /** Logs each mediation network TradPlus tries, so a Meta/AdMob no-fill shows its own reason. */
    private static LoadAdEveryLayerListener layerLog(String format, @Nullable Runnable onAlreadyLoading,
                                                     @Nullable Runnable onAllLoaded) {
        return new LoadAdEveryLayerListener() {
            @Override
            public void onAdAllLoaded(boolean success) {
                if (success && onAllLoaded != null) onAllLoaded.run();
            }

            @Override
            public void oneLayerLoadFailed(TPAdError error, TPAdInfo info) {
                AdsLog.d(TAG, format + " | " + network(info) + " failed | " + message(error));
            }

            @Override
            public void oneLayerLoaded(TPAdInfo info) {
                AdsLog.d(TAG, format + " | " + network(info) + " loaded");
            }

            @Override
            public void onAdStartLoad(String unitId) {
            }

            @Override
            public void oneLayerLoadStart(TPAdInfo info) {
                AdsLog.d(TAG, format + " | " + network(info) + " request");
            }

            @Override
            public void onBiddingStart(TPAdInfo info) {
            }

            @Override
            public void onBiddingEnd(TPAdInfo info, TPAdError error) {
                if (error != null && error.getErrorCode() != 0) {
                    AdsLog.d(TAG, format + " | " + network(info) + " bid failed | " + message(error));
                }
            }

            @Override
            public void onAdIsLoading(String unitId) {
                AdsLog.d(TAG, format + " | unit already loading");
                if (onAlreadyLoading != null) onAlreadyLoading.run();
            }
        };
    }

    /**
     * Network wrappers such as Meta's NativeAdLayout are added with MATCH_PARENT height, which
     * would stretch the ad over the whole screen; measuring unbounded keeps the layout's own height.
     */
    private static final class NativeHolder extends FrameLayout {
        NativeHolder(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        }
    }

    @LayoutRes
    private static int nativeLayout(@Nullable TPBaseAd baseAd, boolean big) {
        Object networkAd = baseAd == null ? null : baseAd.getNetworkObj();
        if (networkAd instanceof NativeAd) {
            return big ? R.layout.ad_native_big_tradplus_meta : R.layout.ad_native_small_tradplus_meta;
        }
        if (networkAd instanceof NativeBannerAd && big) return R.layout.ad_native_big_tradplus_meta;
        return big ? R.layout.ad_native_big_tradplus : R.layout.ad_native_small_tradplus;
    }

    @Override
    public AdProviderType type() {
        return AdProviderType.TRADPLUS;
    }

    @Override
    public boolean isAvailable(AdType type, AdsConfig config) {
        return config.unitId(AdProviderType.TRADPLUS, type) != null;
    }

    @Override
    public void loadBanner(Activity activity, ViewGroup container, AdsConfig config, LoadCallback<InlineAd> callback) {
        String unitId = config.unitId(AdProviderType.TRADPLUS, AdType.BANNER);
        if (unitId == null) {
            callback.onFailed("no unit");
            return;
        }
        whenReady(() -> {
            if (activity.isDestroyed()) {
                callback.onFailed("activity gone");
                return;
            }
            TPBanner banner = new TPBanner(activity);
            banner.setMinimumHeight(Math.round(50 * activity.getResources().getDisplayMetrics().density));
            // TradPlus only renders and counts impressions when the banner is attached during load.
            container.addView(banner, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            SimpleInlineAd ad = new SimpleInlineAd(banner, banner::onDestroy);
            banner.setAdListener(new BannerAdListener() {
                private boolean delivered;

                @Override
                public void onAdLoaded(TPAdInfo info) {
                    if (delivered) return;
                    delivered = true;
                    if (banner.isReady()) banner.showAd();
                    callback.onLoaded(ad);
                }

                @Override
                public void onAdImpression(TPAdInfo info) {
                    ad.impression();
                }

                @Override
                public void onAdLoadFailed(TPAdError error) {
                    if (delivered) return;
                    delivered = true;
                    container.removeView(banner);
                    banner.onDestroy();
                    callback.onFailed(message(error));
                }
            });
            banner.setAllAdLoadListener(layerLog("banner", null, null));
            banner.loadAd(unitId);
        });
    }

    @Override
    public void loadNative(Activity activity, ViewGroup container, boolean big, AdsConfig config,
                           LoadCallback<InlineAd> callback) {
        String unitId = config.unitId(AdProviderType.TRADPLUS, AdType.NATIVE);
        if (unitId == null) {
            callback.onFailed("no unit");
            return;
        }
        whenReady(() -> runNative(unitId, () -> {
            if (activity.isDestroyed()) {
                callback.onFailed("activity gone");
                nativeDone(unitId);
                return;
            }
            String format = big ? "native_big" : "native_small";
            TPNative tpNative = new TPNative(activity, unitId);
            var listener = new NativeAdListener() {
                private boolean delivered;
                private int busyRetries;
                @Nullable
                private SimpleInlineAd ad;

                @Override
                public void onAdLoaded(TPAdInfo info, TPBaseAd baseAd) {
                    show(info, baseAd, null);
                }

                /** The unit's background pre-load can fill without onAdLoaded; take the cached ad then. */
                void loadedWithoutCallback() {
                    if (delivered || !tpNative.isReady()) return;
                    TPCustomNativeAd cached = tpNative.getNativeAd();
                    if (cached != null) show(cached.getTPAdInfo(), cached.getNativeAd(), cached);
                }

                void show(@Nullable TPAdInfo info, @Nullable TPBaseAd baseAd, @Nullable TPCustomNativeAd cached) {
                    if (delivered) return;
                    delivered = true;
                    nativeDone(unitId);
                    if (activity.isDestroyed() || (cached == null && !tpNative.isReady())) {
                        tpNative.onDestroy();
                        callback.onFailed("not ready");
                        return;
                    }
                    AdsLog.d(TAG, format + " | filled by " + network(info));
                    FrameLayout holder = new NativeHolder(activity);
                    container.addView(holder, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    ad = new SimpleInlineAd(holder, () -> {
                        if (cached != null) cached.onDestroy();
                        tpNative.onDestroy();
                    });
                    int layout = nativeLayout(baseAd, big);
                    if (cached != null) cached.showAd(holder, layout, "");
                    else tpNative.showAd(holder, layout, "");
                    if (baseAd != null && baseAd.getNetworkObj() instanceof NativeBannerAd) {
                        View media = holder.findViewById(R.id.tp_native_main_image);
                        if (media != null) media.setVisibility(View.GONE);
                    }
                    callback.onLoaded(ad);
                }

                @Override
                public void onAdImpression(TPAdInfo info) {
                    if (ad != null) ad.impression();
                }

                @Override
                public void onAdShowFailed(TPAdError error, TPAdInfo info) {
                    AdsLog.d(TAG, format + " | show failed | " + network(info) + " | " + message(error));
                }

                @Override
                public void onAdLoadFailed(TPAdError error) {
                    fail(message(error));
                }

                void fail(String reason) {
                    if (delivered) return;
                    delivered = true;
                    nativeDone(unitId);
                    tpNative.onDestroy();
                    callback.onFailed(reason);
                }

                /** TradPlus pre-caches the next ad after a fill; once that finishes, loadAd serves from cache. */
                void retryWhenBusy() {
                    if (delivered) return;
                    if (busyRetries++ >= BUSY_RETRIES) {
                        fail("unit already loading");
                        return;
                    }
                    MAIN.postDelayed(() -> {
                        if (delivered) return;
                        if (activity.isDestroyed()) fail("activity gone");
                        else tpNative.loadAd();
                    }, BUSY_RETRY_MS);
                }
            };
            tpNative.setAdListener(listener);
            tpNative.setAutoLoadCallback(true);
            tpNative.setAllAdLoadListener(layerLog(format, () -> MAIN.post(listener::retryWhenBusy),
                    () -> MAIN.postDelayed(listener::loadedWithoutCallback, 300)));
            tpNative.loadAd();
        }));
    }

    @Override
    public void loadFullscreen(Activity activity, AdType type, AdsConfig config, LoadCallback<FullscreenAd> callback) {
        String unitId = config.unitId(AdProviderType.TRADPLUS, type);
        if (unitId == null) {
            callback.onFailed("no unit");
            return;
        }
        whenReady(() -> {
            switch (type) {
                case INTERSTITIAL:
                    loadInterstitial(activity, unitId, callback);
                    break;
                case REWARD:
                    loadReward(activity, unitId, callback);
                    break;
                case APP_OPEN:
                    loadSplash(activity, unitId, callback);
                    break;
                default:
                    callback.onFailed("unsupported " + type);
            }
        });
    }

    /** Routes SDK show events to whichever {@link ShowCallback} is attached at show time. */
    private static final class ShowState {
        boolean loaded;
        @Nullable
        ShowCallback show;
        @Nullable
        ViewGroup overlay;

        void shown() {
            if (show != null) show.onShown();
        }

        void dismissed() {
            removeOverlay();
            ShowCallback callback = show;
            show = null;
            if (callback != null) callback.onDismissed();
        }

        void failedToShow(String reason) {
            removeOverlay();
            ShowCallback callback = show;
            show = null;
            if (callback != null) callback.onFailedToShow(reason);
        }

        private void removeOverlay() {
            if (overlay != null && overlay.getParent() instanceof ViewGroup) {
                ((ViewGroup) overlay.getParent()).removeView(overlay);
            }
            overlay = null;
        }
    }

    private void loadInterstitial(Activity activity, String unitId, LoadCallback<FullscreenAd> callback) {
        TPInterstitial ad = new TPInterstitial(activity, unitId);
        ShowState state = new ShowState();
        ad.setAdListener(new InterstitialAdListener() {
            @Override
            public void onAdLoaded(TPAdInfo info) {
                if (state.loaded) return;
                state.loaded = true;
                callback.onLoaded(new FullscreenAd() {
                    @Override
                    public void show(Activity host, ShowCallback show) {
                        if (!ad.isReady()) {
                            show.onFailedToShow("not ready");
                            return;
                        }
                        state.show = show;
                        ad.showAd(host, "");
                    }

                    @Override
                    public void destroy() {
                        ad.onDestroy();
                    }
                });
            }

            @Override
            public void onAdFailed(TPAdError error) {
                if (state.loaded) return;
                state.loaded = true;
                ad.onDestroy();
                callback.onFailed(message(error));
            }

            @Override
            public void onAdImpression(TPAdInfo info) {
                state.shown();
            }

            @Override
            public void onAdClicked(TPAdInfo info) {
            }

            @Override
            public void onAdClosed(TPAdInfo info) {
                state.dismissed();
            }

            @Override
            public void onAdVideoError(TPAdInfo info, TPAdError error) {
                state.failedToShow(message(error));
            }

            @Override
            public void onAdVideoStart(TPAdInfo info) {
            }

            @Override
            public void onAdVideoEnd(TPAdInfo info) {
            }
        });
        ad.loadAd();
    }

    private void loadReward(Activity activity, String unitId, LoadCallback<FullscreenAd> callback) {
        TPReward ad = new TPReward(activity, unitId);
        ShowState state = new ShowState();
        ad.setAdListener(new RewardAdListener() {
            @Override
            public void onAdLoaded(TPAdInfo info) {
                if (state.loaded) return;
                state.loaded = true;
                callback.onLoaded(new FullscreenAd() {
                    @Override
                    public void show(Activity host, ShowCallback show) {
                        if (!ad.isReady()) {
                            show.onFailedToShow("not ready");
                            return;
                        }
                        state.show = show;
                        ad.showAd(host, "");
                    }

                    @Override
                    public void destroy() {
                        ad.onDestroy();
                    }
                });
            }

            @Override
            public void onAdFailed(TPAdError error) {
                if (state.loaded) return;
                state.loaded = true;
                ad.onDestroy();
                callback.onFailed(message(error));
            }

            @Override
            public void onAdClicked(TPAdInfo info) {
            }

            @Override
            public void onAdImpression(TPAdInfo info) {
                state.shown();
            }

            @Override
            public void onAdClosed(TPAdInfo info) {
                state.dismissed();
            }

            @Override
            public void onAdReward(TPAdInfo info) {
            }

            @Override
            public void onAdVideoStart(TPAdInfo info) {
            }

            @Override
            public void onAdVideoEnd(TPAdInfo info) {
            }

            @Override
            public void onAdVideoError(TPAdInfo info, TPAdError error) {
                state.failedToShow(message(error));
            }
        });
        ad.loadAd();
    }

    /** TradPlus "splash" is its app-open format; it renders into a full-window overlay we add. */
    private void loadSplash(Activity activity, String unitId, LoadCallback<FullscreenAd> callback) {
        TPSplash ad = new TPSplash(activity, unitId);
        ShowState state = new ShowState();
        ad.setAdListener(new SplashAdListener() {
            @Override
            public void onAdLoaded(TPAdInfo info, TPBaseAd baseAd) {
                if (state.loaded) return;
                state.loaded = true;
                callback.onLoaded(new FullscreenAd() {
                    @Override
                    public void show(Activity host, ShowCallback show) {
                        ViewGroup root = host.findViewById(android.R.id.content);
                        if (!ad.isReady() || root == null) {
                            show.onFailedToShow("not ready");
                            return;
                        }
                        FrameLayout overlay = new FrameLayout(host);
                        overlay.setClickable(true);
                        overlay.setElevation(1000f);
                        root.addView(overlay, new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                        state.show = show;
                        state.overlay = overlay;
                        ad.showAd(overlay);
                    }

                    @Override
                    public void destroy() {
                        ad.onDestroy();
                    }
                });
            }

            @Override
            public void onAdLoadFailed(TPAdError error) {
                if (state.loaded) return;
                state.loaded = true;
                ad.onDestroy();
                callback.onFailed(message(error));
            }

            @Override
            public void onAdImpression(TPAdInfo info) {
                state.shown();
            }

            @Override
            public void onAdShowFailed(TPAdInfo info, TPAdError error) {
                state.failedToShow(message(error));
            }

            @Override
            public void onAdClosed(TPAdInfo info) {
                state.dismissed();
            }
        });
        ad.loadAd(null);
    }
}
