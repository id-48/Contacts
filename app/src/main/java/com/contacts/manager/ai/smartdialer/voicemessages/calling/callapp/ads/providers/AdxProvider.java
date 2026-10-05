package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.providers;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdProviderType;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdType;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsConfig;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsLog;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.WeightedAdSelector;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.BaseAdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerAdView;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/** Google Ad Manager. Each request picks one of the admin's weighted IDs for that format. */
public final class AdxProvider extends GoogleAdsProvider {

    private static final String TAG = "AdsAdx";

    private final WeightedAdSelector selector = new WeightedAdSelector();

    @Override
    public AdProviderType type() {
        return AdProviderType.ADX;
    }

    @Override
    public boolean isAvailable(AdType type, AdsConfig config) {
        return !config.adxIds(type).isEmpty();
    }

    @Nullable
    @Override
    protected String pickUnitId(AdType type, AdsConfig config) {
        String id = selector.select(config.adxIds(type));
        if (id != null) AdsLog.d(TAG, "Selected weighted ADX id for " + type.key);
        return id;
    }

    @Override
    protected BaseAdView newBannerView(Activity activity) {
        return new AdManagerAdView(activity);
    }

    @Override
    protected void loadBannerView(BaseAdView view) {
        ((AdManagerAdView) view).loadAd(new AdManagerAdRequest.Builder().build());
    }

    @Override
    protected void loadNative(AdLoader loader) {
        loader.loadAd(new AdManagerAdRequest.Builder().build());
    }

    @Override
    protected void loadInterstitial(Activity activity, String unitId, LoadCallback<FullscreenAd> callback) {
        AdManagerInterstitialAd.load(activity, unitId, new AdManagerAdRequest.Builder().build(),
                new AdManagerInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AdManagerInterstitialAd ad) {
                        callback.onLoaded(interstitial(ad));
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        callback.onFailed(error.getCode() + " " + error.getMessage());
                    }
                });
    }

    @Override
    protected void loadRewarded(Activity activity, String unitId, LoadCallback<FullscreenAd> callback) {
        RewardedAd.load(activity, unitId, new AdManagerAdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                callback.onLoaded(rewarded(ad));
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                callback.onFailed(error.getCode() + " " + error.getMessage());
            }
        });
    }

    @Override
    protected void loadAppOpen(Activity activity, String unitId, LoadCallback<FullscreenAd> callback) {
        AppOpenAd.load(activity, unitId, new AdManagerAdRequest.Builder().build(), new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                callback.onLoaded(appOpen(ad));
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                callback.onFailed(error.getCode() + " " + error.getMessage());
            }
        });
    }
}
