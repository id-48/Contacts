package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.providers;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdProviderType;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdType;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsConfig;

/**
 * One ad network. Every load must end in exactly one {@link LoadCallback} call; the fallback chain
 * guards against SDKs that call back twice or never.
 */
public interface AdProvider {

    interface LoadCallback<T> {
        void onLoaded(T ad);

        void onFailed(String reason);
    }

    /** Banner or native ad ready to be placed in a container. */
    interface InlineAd {
        View view();

        void setImpressionListener(Runnable listener);

        void destroy();
    }

    interface ShowCallback {
        void onShown();

        void onDismissed();

        void onFailedToShow(String reason);
    }

    interface FullscreenAd {
        void show(Activity activity, ShowCallback callback);

        void destroy();
    }

    AdProviderType type();

    /** True when this provider has an enabled, non-empty unit for {@code type}. */
    boolean isAvailable(AdType type, AdsConfig config);

    /**
     * {@code container} is the slot the ad will end up in. Providers whose SDK must render into an
     * attached view may add their view to it during loading.
     */
    void loadBanner(Activity activity, ViewGroup container, AdsConfig config, LoadCallback<InlineAd> callback);

    void loadNative(Activity activity, ViewGroup container, boolean big, AdsConfig config,
                    LoadCallback<InlineAd> callback);

    void loadFullscreen(Activity activity, AdType type, AdsConfig config, LoadCallback<FullscreenAd> callback);
}
