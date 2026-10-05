package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.launcher;

import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.RemoteConfigManager;

public final class LauncherMode {

    private static final long RETURN_WINDOW_MS = 4000L;

    private static boolean holdingApp;
    private static long returnUntil;

    private LauncherMode() {
    }

    public static void holdApp() {
        holdingApp = true;
    }

    public static void releaseApp() {
        if (!holdingApp) return;
        holdingApp = false;
        returnUntil = SystemClock.elapsedRealtime() + RETURN_WINDOW_MS;
    }

    static boolean shouldReturnToApp() {
        return holdingApp || SystemClock.elapsedRealtime() < returnUntil;
    }

    public static boolean isEnabled() {
        return RemoteConfigManager.get().launcherEnabled;
    }

    public static void sync(Context context) {
        PackageManager pm = context.getPackageManager();
        ComponentName component = new ComponentName(context, LauncherActivity.class);
        int wanted = isEnabled()
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
        try {
            if (pm.getComponentEnabledSetting(component) != wanted) {
                pm.setComponentEnabledSetting(component, wanted, PackageManager.DONT_KILL_APP);
            }
        } catch (RuntimeException ignored) {
        }
    }

    public static boolean isDefaultHome(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roles = context.getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roles.isRoleHeld(RoleManager.ROLE_HOME);
            }
        }
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo info = context.getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        return info != null && info.activityInfo != null
                && context.getPackageName().equals(info.activityInfo.packageName);
    }

    public static boolean isSatisfied(Context context) {
        return !isEnabled() || isDefaultHome(context);
    }

    @Nullable
    public static Intent createRoleIntent(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roles = context.getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roles.createRequestRoleIntent(RoleManager.ROLE_HOME);
            }
        }
        return null;
    }

    public static Intent createSettingsIntent() {
        return new Intent(Settings.ACTION_HOME_SETTINGS);
    }
}
