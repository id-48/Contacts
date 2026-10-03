package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.app.Activity;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PermissionManager;

import java.util.ArrayList;
import java.util.List;

public class PermissionRequester {

    private final ActivityResultLauncher<String[]> launcher;

    public PermissionRequester(ActivityResultCaller caller, Runnable onResult) {
        launcher = caller.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                result -> onResult.run());
    }

    public void request(Activity activity, PermissionManager.Group... groups) {
        List<String> permissions = new ArrayList<>();
        boolean permanentlyDenied = false;
        for (PermissionManager.Group group : groups) {
            PermissionManager.Status status = PermissionManager.getStatus(activity, group);
            if (status == PermissionManager.Status.PERMANENTLY_DENIED) {
                permanentlyDenied = true;
            } else if (status == PermissionManager.Status.DENIED) {
                PermissionManager.markRequested(group);
                for (String permission : group.permissions) {
                    permissions.add(permission);
                }
            }
        }
        if (!permissions.isEmpty()) {
            launcher.launch(permissions.toArray(new String[0]));
        } else if (permanentlyDenied) {
            PermissionManager.openAppSettings(activity);
        }
    }
}
