package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsLog;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.FullscreenAdManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.RemoteConfigManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

import java.lang.ref.WeakReference;

/** Google Play in-app review, asked once on the second app open that reaches the main screen. */
public final class InAppReview {

    private static final String TAG = "InAppReview";
    private static final int MIN_MAIN_OPENS = 2;
    private static final long DELAY_MS = 1_500L;

    private static boolean countedThisProcess;
    private static boolean requesting;

    private InAppReview() {
    }

    public static void onMainOpened(Activity activity) {
        if (!countedThisProcess) {
            countedThisProcess = true;
            StorageService.incrementMainOpenCount();
        }
        if (requesting || StorageService.isReviewPrompted() || !RemoteConfigManager.get().inAppReviewEnabled
                || StorageService.getMainOpenCount() < MIN_MAIN_OPENS) {
            return;
        }
        requesting = true;
        WeakReference<Activity> ref = new WeakReference<>(activity);
        new Handler(Looper.getMainLooper()).postDelayed(() -> request(ref), DELAY_MS);
    }

    private static void request(WeakReference<Activity> ref) {
        Activity activity = ref.get();
        if (!canShow(activity)) {
            requesting = false;
            return;
        }
        ReviewManager manager = ReviewManagerFactory.create(activity);
        manager.requestReviewFlow().addOnCompleteListener(task -> {
            Activity current = ref.get();
            if (!task.isSuccessful() || !canShow(current)) {
                AdsLog.d(TAG, "Review flow not available");
                requesting = false;
                return;
            }
            ReviewInfo info = task.getResult();
            manager.launchReviewFlow(current, info).addOnCompleteListener(done -> {
                StorageService.setReviewPrompted();
                requesting = false;
                AdsLog.d(TAG, "Review flow finished");
            });
        });
    }

    private static boolean canShow(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed() && !FullscreenAdManager.isBusy();
    }
}
