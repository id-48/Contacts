package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.RemoteConfigManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.aftercall.AfterCallActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.call.CallActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.launcher.LauncherActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.onboarding.DefaultPhoneActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.onboarding.WelcomeActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.splash.SplashActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.update.ForceUpdateActivity;

import java.lang.ref.WeakReference;

/**
 * "App Open Ad On Resume": shows an app-open ad when the app returns from the background, never on
 * the first launch, during calls, over splash/after-call/force-update or the permission onboarding
 * screens, or while another fullscreen ad is showing or has just closed.
 */
final class AppOpenAdManager implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private static final String TAG = "AdsAppOpen";
    private static final long AFTER_FULLSCREEN_GAP_MS = 2_000L;
    private static final long RESUME_DELAY_MS = 300L;

    private final Handler main = new Handler(Looper.getMainLooper());
    private WeakReference<Activity> current = new WeakReference<>(null);
    private boolean firstStart = true;

    private AppOpenAdManager() {
    }

    static void register(Application app) {
        AppOpenAdManager manager = new AppOpenAdManager();
        app.registerActivityLifecycleCallbacks(manager);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(manager);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (firstStart) {
            firstStart = false;
            return;
        }
        main.postDelayed(this::maybeShow, RESUME_DELAY_MS);
    }

    private void maybeShow() {
        if (!RemoteConfigManager.get().appOpenOnResume) return;
        Activity activity = current.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || isExcluded(activity)) return;
        if (FullscreenAdManager.isBusy()) return;
        long lastFullscreen = FullscreenAdManager.lastDismissedAt();
        if (lastFullscreen > 0 && SystemClock.elapsedRealtime() - lastFullscreen < AFTER_FULLSCREEN_GAP_MS) {
            AdsLog.d(TAG, "Skipped: a fullscreen ad just closed");
            return;
        }
        FullscreenAdManager.show(activity, AdPlacement.APP_OPEN_RESUME, FullscreenType.APP_OPEN, () -> {
        });
    }

    private static boolean isExcluded(Activity activity) {
        return !(activity instanceof BaseActivity)
                || activity instanceof SplashActivity
                || activity instanceof CallActivity
                || activity instanceof AfterCallActivity
                || activity instanceof ForceUpdateActivity
                || activity instanceof WelcomeActivity
                || activity instanceof DefaultPhoneActivity
                || activity instanceof LauncherActivity;
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        current = new WeakReference<>(activity);
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        current = new WeakReference<>(activity);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }
}
