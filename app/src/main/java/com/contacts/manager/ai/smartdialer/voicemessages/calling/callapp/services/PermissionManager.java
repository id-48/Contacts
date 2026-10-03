package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.IntentUtils;

import java.util.ArrayList;
import java.util.List;

public final class PermissionManager {

    public enum Group {
        CONTACTS(new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS},
                R.string.perm_contacts_title, R.string.perm_contacts_body, R.drawable.ic_person, 0),
        CALL_LOG(new String[]{Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG},
                R.string.perm_call_log_title, R.string.perm_call_log_body, R.drawable.ic_history, 0),
        PHONE(new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE},
                R.string.perm_phone_title, R.string.perm_phone_body, R.drawable.ic_call, 0),
        NOTIFICATIONS(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                R.string.perm_notifications_title, R.string.perm_notifications_body, R.drawable.ic_notifications,
                Build.VERSION_CODES.TIRAMISU);

        public final String[] permissions;
        public final int titleRes;
        public final int bodyRes;
        public final int iconRes;
        public final int minSdk;

        Group(String[] permissions, int titleRes, int bodyRes, int iconRes, int minSdk) {
            this.permissions = permissions;
            this.titleRes = titleRes;
            this.bodyRes = bodyRes;
            this.iconRes = iconRes;
            this.minSdk = minSdk;
        }
    }

    public enum Status {
        GRANTED, DENIED, PERMANENTLY_DENIED, UNSUPPORTED
    }

    private PermissionManager() {
    }

    public static boolean isSupported(Group group) {
        return Build.VERSION.SDK_INT >= group.minSdk;
    }

    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isGranted(Context context, Group group) {
        if (!isSupported(group)) {
            return true;
        }
        for (String permission : group.permissions) {
            if (!hasPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }

    public static Status getStatus(Activity activity, Group group) {
        if (!isSupported(group)) {
            return Status.UNSUPPORTED;
        }
        if (isGranted(activity, group)) {
            return Status.GRANTED;
        }
        boolean rationale = false;
        for (String permission : group.permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                rationale = true;
                break;
            }
        }
        if (StorageService.wasPermissionRequested(group.name()) && !rationale) {
            return Status.PERMANENTLY_DENIED;
        }
        return Status.DENIED;
    }

    public static void markRequested(Group group) {
        StorageService.markPermissionRequested(group.name());
    }

    public static List<Group> getMissingCoreGroups(Context context) {
        List<Group> missing = new ArrayList<>();
        Group[] core = {Group.CONTACTS, Group.CALL_LOG, Group.PHONE};
        for (Group group : core) {
            if (!isGranted(context, group)) {
                missing.add(group);
            }
        }
        return missing;
    }

    public static void openAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        IntentUtils.safeStart(context, intent);
    }
}
