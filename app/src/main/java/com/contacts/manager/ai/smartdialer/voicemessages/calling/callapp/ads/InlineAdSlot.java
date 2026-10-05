package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.providers.AdProvider;

import java.util.List;

/**
 * Banner / native slot: shows a matching shimmer while loading, swaps in the ad, hides the container
 * on no-fill, and destroys the ad with the activity.
 */
@MainThread
final class InlineAdSlot {

    enum Shape {
        BANNER(R.layout.ad_shimmer_banner),
        NATIVE_BIG(R.layout.ad_shimmer_native_big),
        NATIVE_SMALL(R.layout.ad_shimmer_native_small);

        @LayoutRes
        final int shimmerLayout;

        Shape(@LayoutRes int shimmerLayout) {
            this.shimmerLayout = shimmerLayout;
        }
    }

    private static final String TAG = "AdsInline";

    private InlineAdSlot() {
    }

    static void load(Activity activity, ViewGroup container, String placement, List<AdProvider> providers,
                     Shape shape, AdFallbackChain.Attempt<AdProvider.InlineAd> attempt) {
        if (providers.isEmpty()) {
            AdsLog.d(TAG, "No provider configured for " + placement);
            container.setVisibility(View.GONE);
            return;
        }
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (!AdsManager.GUARD.tryBeginRequest(placement)) return;

        container.removeAllViews();
        View shimmer = LayoutInflater.from(activity).inflate(shape.shimmerLayout, container, false);
        container.addView(shimmer);
        container.setVisibility(View.VISIBLE);

        AdProvider.InlineAd[] shown = new AdProvider.InlineAd[1];
        AdFallbackChain.Handle[] handle = new AdFallbackChain.Handle[1];
        DefaultLifecycleObserver observer = new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                owner.getLifecycle().removeObserver(this);
                if (handle[0] != null) handle[0].cancel();
                if (shown[0] != null) {
                    detach(shown[0].view());
                    shown[0].destroy();
                    shown[0] = null;
                }
                AdsManager.GUARD.release(placement);
            }
        };
        if (activity instanceof LifecycleOwner) {
            ((LifecycleOwner) activity).getLifecycle().addObserver(observer);
        }

        handle[0] = AdFallbackChain.run(placement, providers, attempt, ad -> {
            detach(ad.view());
            ad.destroy();
        }, new AdFallbackChain.Result<AdProvider.InlineAd>() {
            @Override
            public void onLoaded(AdProvider.InlineAd ad, String provider) {
                if (activity.isDestroyed()) {
                    detach(ad.view());
                    ad.destroy();
                    return;
                }
                shown[0] = ad;
                ad.setImpressionListener(() -> AdsLog.adImpression(placement, provider));
                View view = ad.view();
                for (int i = container.getChildCount() - 1; i >= 0; i--) {
                    if (container.getChildAt(i) != view) container.removeViewAt(i);
                }
                if (view.getParent() != container) {
                    detach(view);
                    container.addView(view);
                }
                view.setVisibility(View.VISIBLE);
                container.setVisibility(View.VISIBLE);
                AdsManager.GUARD.onLoaded(placement);
                AdsManager.GUARD.onShowing(placement);
                AdsLog.d(TAG, "Showing " + placement);
            }

            @Override
            public void onFailed(String reason) {
                container.removeAllViews();
                container.setVisibility(View.GONE);
                AdsManager.GUARD.release(placement);
            }
        });
    }

    private static void detach(View view) {
        if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
    }
}
