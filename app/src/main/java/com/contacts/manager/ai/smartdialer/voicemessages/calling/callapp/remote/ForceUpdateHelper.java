package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.BuildConfig;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.update.ForceUpdateActivity;

public final class ForceUpdateHelper {

    private ForceUpdateHelper() {
    }

    public static boolean isUpdateRequired(RemoteConfig config, int versionCode) {
        if (!config.forceUpdateEnabled) {
            return false;
        }
        Integer required = parseVersionCode(config.minimumVersion);
        if (required == null) {
            return false;
        }
        return versionCode != required;
    }

    public static boolean isUpdateRequired() {
        return isUpdateRequired(RemoteConfigManager.get(), BuildConfig.VERSION_CODE);
    }

    /** Replaces the task with the force update screen when needed. Returns true if the caller must stop. */
    public static boolean blockIfRequired(Activity activity) {
        if (!isUpdateRequired()) return false;
        Intent intent = new Intent(activity, ForceUpdateActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
        return true;
    }

    @Nullable
    static Integer parseVersionCode(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty() || !trimmed.matches("\\d{1,9}")) return null;
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
