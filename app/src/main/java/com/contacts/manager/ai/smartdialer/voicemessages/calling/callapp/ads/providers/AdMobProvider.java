package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.providers;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdProviderType;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdType;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsConfig;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.BaseAdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public final class AdMobProvider extends GoogleAdsProvider {

    @Override
    public AdProviderType type() {
        return AdProviderType.ADMOB;
    }

    @Override
    public boolean isAvailable(AdType type, AdsConfig config) {
        return config.unitId(AdProviderType.ADMOB, type) != null;
    }

    @Nullable
    @Override
    protected String pickUnitId(AdType type, AdsConfig config) {
        return config.unitId(AdProviderType.ADMOB, type);
    }

    @Override
    protected BaseAdView newBannerView(Activity activity) {
        return new AdView(activity);
    }

    @Override
    protected void loadBannerView(BaseAdView view) {
        ((AdView) view).loadAd(new AdRequest.Builder().build());
    }

    @Override
    protected void loadNative(AdLoader loader) {
        loader.loadAd(new AdRequest.Builder().build());
    }

    @Override
    protected void loadInterstitial(Activity activity, String unitId, LoadCallback<FullscreenAd> callback) {
        InterstitialAd.load(activity, unitId, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
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
        RewardedAd.load(activity, unitId, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
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
        AppOpenAd.load(activity, unitId, new AdRequest.Builder().build(), new AppOpenAd.AppOpenAdLoadCallback() {
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
