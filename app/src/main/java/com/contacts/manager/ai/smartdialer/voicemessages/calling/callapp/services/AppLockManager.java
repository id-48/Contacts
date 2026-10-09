package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.aftercall.AfterCallActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.call.CallActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.call.FakeIncomingCallActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.launcher.LauncherActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.lock.PasscodeActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.splash.SplashActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.update.ForceUpdateActivity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;

public final class AppLockManager implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private static final long EXTERNAL_WINDOW_MS = 10 * 60_000L;
    private static final long EXTERNAL_RETURN_GRACE_MS = 3_000L;

    private static boolean locked = true;
    private static long externalUntil;
    private static boolean skipNextLock;
    private static long externalReturnAt;
    private static boolean prompting;

    private AppLockManager() {
    }

    public static void register(Application app) {
        AppLockManager manager = new AppLockManager();
        app.registerActivityLifecycleCallbacks(manager);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(manager);
    }

    public static void markExternalLaunch() {
        externalUntil = SystemClock.elapsedRealtime() + EXTERNAL_WINDOW_MS;
    }

    public static boolean isReturningFromExternal() {
        return externalReturnAt > 0 && SystemClock.elapsedRealtime() - externalReturnAt < EXTERNAL_RETURN_GRACE_MS;
    }

    public static void unlock() {
        locked = false;
        prompting = false;
    }

    public static boolean hasPasscode() {
        return StorageService.getPasscodeHash() != null;
    }

    public static void savePasscode(String pin) {
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        String salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP);
        StorageService.setPasscode(salt, hash(salt, pin));
    }

    public static boolean verifyPasscode(String pin) {
        String salt = StorageService.getPasscodeSalt();
        String stored = StorageService.getPasscodeHash();
        return salt != null && stored != null && MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8), hash(salt, pin).getBytes(StandardCharsets.UTF_8));
    }

    public static void saveSecurityQuestion(String question, String answer) {
        String salt = StorageService.getPasscodeSalt();
        StorageService.setSecurityQuestion(question, hash(salt == null ? "" : salt, normalizeAnswer(answer)));
    }

    public static boolean verifySecurityAnswer(String answer) {
        String salt = StorageService.getPasscodeSalt();
        String stored = StorageService.getSecurityAnswerHash();
        return stored != null && MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8),
                hash(salt == null ? "" : salt, normalizeAnswer(answer)).getBytes(StandardCharsets.UTF_8));
    }

    private static String normalizeAnswer(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String hash(String salt, String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((salt + ":" + value).getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (skipNextLock) {
            skipNextLock = false;
            externalReturnAt = SystemClock.elapsedRealtime();
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (SystemClock.elapsedRealtime() < externalUntil) {
            externalUntil = 0;
            skipNextLock = true;
            return;
        }
        locked = true;
        prompting = false;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (activity instanceof PasscodeActivity) {
            return;
        }
        externalUntil = 0;
        if (!locked || prompting || !StorageService.isAppLockEnabled() || isExempt(activity)) {
            return;
        }
        prompting = true;
        activity.startActivity(PasscodeActivity.unlockIntent(activity));
        activity.overridePendingTransition(0, 0);
    }

    private static boolean isExempt(Activity activity) {
        return !(activity instanceof BaseActivity)
                || activity instanceof SplashActivity
                || activity instanceof CallActivity
                || activity instanceof FakeIncomingCallActivity
                || activity instanceof AfterCallActivity
                || activity instanceof ForceUpdateActivity
                || activity instanceof LauncherActivity;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
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
