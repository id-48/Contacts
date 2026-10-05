package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.providers;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdType;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsConfig;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.ui.GoogleNativeBinder;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.BaseAdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.rewarded.RewardedAd;

/** Shared AdMob / Ad Manager (ADX) code; subclasses supply the unit ID and the request type. */
abstract class GoogleAdsProvider implements AdProvider {

    @Nullable
    protected abstract String pickUnitId(AdType type, AdsConfig config);

    protected abstract BaseAdView newBannerView(Activity activity);

    protected abstract void loadBannerView(BaseAdView view);

    protected abstract void loadNative(AdLoader loader);

    protected abstract void loadInterstitial(Activity activity, String unitId, LoadCallback<FullscreenAd> callback);

    protected abstract void loadRewarded(Activity activity, String unitId, LoadCallback<FullscreenAd> callback);

    protected abstract void loadAppOpen(Activity activity, String unitId, LoadCallback<FullscreenAd> callback);

    @Override
    public void loadBanner(Activity activity, ViewGroup container, AdsConfig config, LoadCallback<InlineAd> callback) {
        String unitId = pickUnitId(AdType.BANNER, config);
        if (unitId == null) {
            callback.onFailed("no unit");
            return;
        }
        BaseAdView view = newBannerView(activity);
        view.setAdUnitId(unitId);
        view.setAdSize(adaptiveSize(activity, container));
        SimpleInlineAd ad = new SimpleInlineAd(view, view::destroy);
        view.setAdListener(new AdListener() {
            private boolean delivered;

            @Override
            public void onAdLoaded() {
                if (delivered) return;
                delivered = true;
                callback.onLoaded(ad);
            }

            @Override
            public void onAdImpression() {
                ad.impression();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                if (delivered) return;
                delivered = true;
                view.destroy();
                callback.onFailed(error.getCode() + " " + error.getMessage());
            }
        });
        loadBannerView(view);
    }

    @Override
    public void loadNative(Activity activity, ViewGroup container, boolean big, AdsConfig config,
                           LoadCallback<InlineAd> callback) {
        String unitId = pickUnitId(AdType.NATIVE, config);
        if (unitId == null) {
            callback.onFailed("no unit");
            return;
        }
        SimpleInlineAd[] loaded = new SimpleInlineAd[1];
        AdLoader loader = new AdLoader.Builder(activity, unitId)
                .forNativeAd(nativeAd -> {
                    if (activity.isFinishing() || activity.isDestroyed()) {
                        nativeAd.destroy();
                        callback.onFailed("activity gone");
                        return;
                    }
                    View view = GoogleNativeBinder.bind(activity, nativeAd, big);
                    loaded[0] = new SimpleInlineAd(view, nativeAd::destroy);
                    callback.onLoaded(loaded[0]);
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        callback.onFailed(error.getCode() + " " + error.getMessage());
                    }

                    @Override
                    public void onAdImpression() {
                        if (loaded[0] != null) loaded[0].impression();
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .build())
                .build();
        loadNative(loader);
    }

    @Override
    public void loadFullscreen(Activity activity, AdType type, AdsConfig config, LoadCallback<FullscreenAd> callback) {
        String unitId = pickUnitId(type, config);
        if (unitId == null) {
            callback.onFailed("no unit");
            return;
        }
        switch (type) {
            case INTERSTITIAL:
                loadInterstitial(activity, unitId, callback);
                break;
            case REWARD:
                loadRewarded(activity, unitId, callback);
                break;
            case APP_OPEN:
                loadAppOpen(activity, unitId, callback);
                break;
            default:
                callback.onFailed("unsupported " + type);
        }
    }

    static FullscreenAd interstitial(InterstitialAd ad) {
        return new FullscreenAd() {
            @Override
            public void show(Activity activity, ShowCallback callback) {
                ad.setFullScreenContentCallback(contentCallback(callback));
                ad.show(activity);
            }

            @Override
            public void destroy() {
                ad.setFullScreenContentCallback(null);
            }
        };
    }

    static FullscreenAd rewarded(RewardedAd ad) {
        return new FullscreenAd() {
            @Override
            public void show(Activity activity, ShowCallback callback) {
                ad.setFullScreenContentCallback(contentCallback(callback));
                ad.show(activity, rewardItem -> {
                });
            }

            @Override
            public void destroy() {
                ad.setFullScreenContentCallback(null);
            }
        };
    }

    static FullscreenAd appOpen(AppOpenAd ad) {
        return new FullscreenAd() {
            @Override
            public void show(Activity activity, ShowCallback callback) {
                ad.setFullScreenContentCallback(contentCallback(callback));
                ad.show(activity);
            }

            @Override
            public void destroy() {
                ad.setFullScreenContentCallback(null);
            }
        };
    }

    private static FullScreenContentCallback contentCallback(ShowCallback callback) {
        return new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                callback.onShown();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                callback.onDismissed();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                callback.onFailedToShow(error.getCode() + " " + error.getMessage());
            }
        };
    }

    private static AdSize adaptiveSize(Activity activity, ViewGroup container) {
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int widthPx = container.getWidth() > 0 ? container.getWidth() : metrics.widthPixels;
        int widthDp = Math.max(1, (int) (widthPx / metrics.density));
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp);
    }
}
