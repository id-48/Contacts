package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote;

import android.app.Activity;
import android.content.Intent;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.BuildConfig;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.update.ForceUpdateActivity;

public final class ForceUpdateHelper {

    private ForceUpdateHelper() {
    }

    public static boolean isUpdateRequired(RemoteConfig config, String currentVersion) {
        return config.forceUpdateEnabled
                && !config.minimumVersion.isEmpty()
                && VersionUtils.compare(currentVersion, config.minimumVersion) < 0;
    }

    public static boolean isUpdateRequired() {
        return isUpdateRequired(RemoteConfigManager.get(), BuildConfig.VERSION_NAME);
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
}
