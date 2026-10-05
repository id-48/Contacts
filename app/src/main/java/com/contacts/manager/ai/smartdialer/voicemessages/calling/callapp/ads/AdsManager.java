package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads;

import android.app.Activity;
import android.app.Application;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.MainThread;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.providers.AdMobProvider;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.providers.AdProvider;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.providers.AdxProvider;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.providers.CustomAdProvider;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.providers.TradPlusProvider;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.RemoteConfigManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.UserSourceManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The only entry point screens use. Every call is driven by the remote config: a screen/slot that is
 * off, or has no configured provider, hides its container and (for fullscreen) continues immediately.
 */
@MainThread
public final class AdsManager {

    private static final String TAG = "AdsManager";

    /** Default order when the admin has not set a priority: AdMob, TradPlus, ADX, Custom. */
    private static final List<AdProvider> PROVIDERS = Collections.unmodifiableList(Arrays.asList(
            new AdMobProvider(), new TradPlusProvider(), new AdxProvider(), new CustomAdProvider()));

    static final AdsRequestGuard GUARD = new AdsRequestGuard();

    private static boolean initialized;

    private AdsManager() {
    }

    public static void initialize(Application app) {
        if (initialized) return;
        initialized = true;
        new Thread(() -> {
            try {
                MobileAds.initialize(app, status -> AdsLog.d(TAG, "MobileAds initialized"));
            } catch (RuntimeException e) {
                AdsLog.d(TAG, "MobileAds init failed: " + e.getClass().getSimpleName());
            }
        }, "ads-init").start();
        TradPlusProvider.init(app);
        AppOpenAdManager.register(app);
    }

    public static void showBanner(Activity activity, ViewGroup container, String screenKey) {
        AdsConfig config = config();
        if (!config.screen(screenKey).banner) {
            container.setVisibility(View.GONE);
            return;
        }
        InlineAdSlot.load(activity, container, AdPlacement.banner(screenKey), providersFor(AdType.BANNER, config),
                InlineAdSlot.Shape.BANNER, (provider, callback) -> provider.loadBanner(activity, container, config, callback));
    }

    public static void showNativeBig(Activity activity, ViewGroup container, String screenKey) {
        AdsConfig config = config();
        showNative(activity, container, screenKey, true, config.screen(screenKey).nativeBig, config);
    }

    public static void showNativeSmall(Activity activity, ViewGroup container, String screenKey) {
        AdsConfig config = config();
        showNative(activity, container, screenKey, false, config.screen(screenKey).nativeSmall, config);
    }

    private static void showNative(Activity activity, ViewGroup container, String screenKey, boolean big,
                                   boolean enabled, AdsConfig config) {
        if (!enabled) {
            container.setVisibility(View.GONE);
            return;
        }
        InlineAdSlot.load(activity, container, AdPlacement.nativeAd(screenKey, big), providersFor(AdType.NATIVE, config),
                big ? InlineAdSlot.Shape.NATIVE_BIG : InlineAdSlot.Shape.NATIVE_SMALL,
                (provider, callback) -> provider.loadNative(activity, container, big, config, callback));
    }

    /**
     * Shows the next fullscreen ad of the admin's sequence when this screen's fullscreen switch is on.
     * {@code onDone} always runs exactly once (after dismiss, failure or the loader timeout) while the
     * activity is alive; a duplicate call for a placement already in progress is ignored.
     */
    public static void showFullscreen(Activity activity, String screenKey, Runnable onDone) {
        if (!config().screen(screenKey).fullscreen) {
            onDone.run();
            return;
        }
        FullscreenAdManager.showNext(activity, AdPlacement.fullscreen(screenKey), onDone);
    }

    /** Fullscreen ad between splash and the next screen, controlled by "Splash Screen After Full Screen Ad". */
    public static void showSplashFullscreen(Activity activity, Runnable onDone) {
        if (!RemoteConfigManager.get().splashAfterFullscreenAd) {
            onDone.run();
            return;
        }
        FullscreenAdManager.showNext(activity, AdPlacement.SPLASH_FULLSCREEN, onDone);
    }

    static AdsConfig config() {
        return RemoteConfigManager.get().ads.forSource(UserSourceManager.isMarketing());
    }

    static List<AdProvider> providersFor(AdType type, AdsConfig config) {
        AdsConfig.Priority priority = config.priority(type);
        List<AdProviderType> order = null;
        if (priority != null) {
            String key = (UserSourceManager.isMarketing() ? "marketing_" : "organic_") + type.key;
            int position = StorageService.getPriorityPosition(key);
            StorageService.setPriorityPosition(key, (position + 1) % priority.sequence.size());
            order = priority.order(position);
            AdsLog.d(TAG, "Priority " + type.key + " position " + position + " -> " + order);
        }
        List<AdProvider> available = new ArrayList<>();
        if (order == null) {
            for (AdProvider provider : PROVIDERS) {
                if (provider.isAvailable(type, config)) available.add(provider);
            }
        } else {
            for (AdProviderType providerType : order) {
                for (AdProvider provider : PROVIDERS) {
                    if (provider.type() == providerType && provider.isAvailable(type, config)) available.add(provider);
                }
            }
        }
        return available;
    }

    static List<AdProvider> customProviders(AdsConfig config) {
        List<AdProvider> available = new ArrayList<>();
        for (AdProvider provider : PROVIDERS) {
            if (provider.type() == AdProviderType.CUSTOM && provider.isAvailable(AdType.INTERSTITIAL, config)) {
                available.add(provider);
            }
        }
        return available;
    }
}
