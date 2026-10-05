package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.providers;

import android.app.Activity;
import android.view.ViewGroup;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdProviderType;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdType;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsConfig;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.ui.CustomTabAdActivity;

/** Fullscreen-only house ad: the admin's Custom Ads URL opened in a Chrome Custom Tab. */
public final class CustomAdProvider implements AdProvider {

    @Override
    public AdProviderType type() {
        return AdProviderType.CUSTOM;
    }

    @Override
    public boolean isAvailable(AdType type, AdsConfig config) {
        return config.customEnabled && type != AdType.BANNER && type != AdType.NATIVE;
    }

    @Override
    public void loadBanner(Activity activity, ViewGroup container, AdsConfig config, LoadCallback<InlineAd> callback) {
        callback.onFailed("fullscreen only");
    }

    @Override
    public void loadNative(Activity activity, ViewGroup container, boolean big, AdsConfig config,
                           LoadCallback<InlineAd> callback) {
        callback.onFailed("fullscreen only");
    }

    @Override
    public void loadFullscreen(Activity activity, AdType type, AdsConfig config, LoadCallback<FullscreenAd> callback) {
        if (!config.customEnabled) {
            callback.onFailed("disabled");
            return;
        }
        String url = config.customUrl;
        callback.onLoaded(new FullscreenAd() {
            @Override
            public void show(Activity host, ShowCallback show) {
                CustomTabAdActivity.start(host, url, show);
            }

            @Override
            public void destroy() {
            }
        });
    }
}
