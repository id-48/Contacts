package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.onboarding;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.ConfirmDialog;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.PermissionSheet;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PermissionManager;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Asks every runtime permission the app needs (plus "display over other apps") and does not finish
 * until all are allowed. Create it as an activity field so its result launchers register in time.
 */
final class OnboardingPermissions {

    private enum Requirement { CALL_LOG, CONTACTS, PHONE, NOTIFICATIONS, OVERLAY }

    private static final String STATE_ASKED = "permissions_asked";
    private static final String STATE_AWAITING = "permissions_awaiting";

    private final AppCompatActivity activity;
    private final boolean showSheet;
    private final Runnable onComplete;
    private final EnumSet<Requirement> asked = EnumSet.noneOf(Requirement.class);
    private final ActivityResultLauncher<String[]> permissionLauncher;
    private final ActivityResultLauncher<Intent> settingsLauncher;
    @Nullable
    private Dialog settingsDialog;
    @Nullable
    private Requirement settingsDialogFor;
    private boolean awaitingResult;
    private boolean running;

    OnboardingPermissions(AppCompatActivity activity, boolean showSheet, Runnable onComplete) {
        this.activity = activity;
        this.showSheet = showSheet;
        this.onComplete = onComplete;
        permissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> onReturned());
        settingsLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> onReturned());
    }

    void start() {
        if (running || isGone()) return;
        List<PermissionSheet.Item> items = new ArrayList<>();
        for (Requirement requirement : Requirement.values()) {
            if (!isSatisfied(activity, requirement)) items.add(sheetItem(requirement));
        }
        if (items.isEmpty()) {
            onComplete.run();
            return;
        }
        running = true;
        if (showSheet) PermissionSheet.show(activity, items, this::continueFlow, null);
        else continueFlow();
    }

    void onResume() {
        if (settingsDialog != null && settingsDialog.isShowing() && isSatisfied(activity, settingsDialogFor)) {
            settingsDialog.dismiss();
            continueFlow();
        }
    }

    void save(@NonNull Bundle outState) {
        int[] steps = new int[asked.size()];
        int i = 0;
        for (Requirement requirement : asked) steps[i++] = requirement.ordinal();
        outState.putIntArray(STATE_ASKED, steps);
        outState.putBoolean(STATE_AWAITING, awaitingResult);
    }

    void restore(@Nullable Bundle state) {
        if (state == null) return;
        int[] steps = state.getIntArray(STATE_ASKED);
        if (steps != null) {
            for (int step : steps) asked.add(Requirement.values()[step]);
        }
        awaitingResult = state.getBoolean(STATE_AWAITING);
        running = awaitingResult;
    }

    private void onReturned() {
        awaitingResult = false;
        continueFlow();
    }

    private void continueFlow() {
        if (!running || awaitingResult || isGone()) return;
        Requirement next = firstMissing();
        if (next == null) {
            running = false;
            onComplete.run();
            return;
        }
        if (asked.add(next)) request(next);
        else showSettingsDialog(next);
    }

    @Nullable
    private Requirement firstMissing() {
        for (Requirement requirement : Requirement.values()) {
            if (!isSatisfied(activity, requirement)) return requirement;
        }
        return null;
    }

    private static boolean isSatisfied(Context context, @Nullable Requirement requirement) {
        if (requirement == null) return true;
        if (requirement == Requirement.OVERLAY) return Settings.canDrawOverlays(context);
        return PermissionManager.isGranted(context, group(requirement));
    }

    private static PermissionManager.Group group(Requirement requirement) {
        switch (requirement) {
            case CALL_LOG:
                return PermissionManager.Group.CALL_LOG;
            case CONTACTS:
                return PermissionManager.Group.CONTACTS;
            case PHONE:
                return PermissionManager.Group.PHONE;
            default:
                return PermissionManager.Group.NOTIFICATIONS;
        }
    }

    private static PermissionSheet.Item sheetItem(Requirement requirement) {
        switch (requirement) {
            case CALL_LOG:
                return new PermissionSheet.Item(R.drawable.ic_phone_in_talk,
                        R.string.perm_sheet_call_history_title, R.string.perm_sheet_call_history_body);
            case CONTACTS:
                return new PermissionSheet.Item(R.drawable.ic_person,
                        R.string.perm_sheet_contacts_title, R.string.perm_sheet_contacts_body);
            case PHONE:
                return new PermissionSheet.Item(R.drawable.ic_call, R.string.perm_phone_title, R.string.perm_phone_body);
            case NOTIFICATIONS:
                return new PermissionSheet.Item(R.drawable.ic_notifications,
                        R.string.perm_notifications_title, R.string.perm_notifications_body);
            default:
                return new PermissionSheet.Item(R.drawable.ic_layers,
                        R.string.perm_sheet_overlay_title, R.string.perm_sheet_overlay_body);
        }
    }

    private static int titleRes(Requirement requirement) {
        switch (requirement) {
            case CALL_LOG:
                return R.string.perm_sheet_call_history_title;
            case CONTACTS:
                return R.string.perm_sheet_contacts_title;
            case PHONE:
                return R.string.perm_phone_title;
            default:
                return R.string.perm_notifications_title;
        }
    }

    private void request(Requirement requirement) {
        if (requirement == Requirement.OVERLAY) {
            openOverlaySettings();
            return;
        }
        PermissionManager.Group group = group(requirement);
        if (PermissionManager.getStatus(activity, group) == PermissionManager.Status.PERMANENTLY_DENIED) {
            showSettingsDialog(requirement);
            return;
        }
        PermissionManager.markRequested(group);
        awaitingResult = true;
        permissionLauncher.launch(group.permissions);
    }

    private void showSettingsDialog(Requirement requirement) {
        if (settingsDialog != null && settingsDialog.isShowing()) {
            settingsDialog.dismiss();
        }
        boolean overlay = requirement == Requirement.OVERLAY;
        String message = overlay
                ? activity.getString(R.string.perm_required_overlay_body)
                : activity.getString(R.string.perm_required_settings_body, activity.getString(titleRes(requirement)));
        settingsDialogFor = requirement;
        settingsDialog = ConfirmDialog.required(activity, activity.getString(R.string.perm_required_title), message,
                overlay ? R.string.allow : R.string.open_settings, () -> {
                    if (overlay) openOverlaySettings();
                    else openAppSettings();
                }, "permission_required_dialog");
    }

    private void openAppSettings() {
        launchSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", activity.getPackageName(), null)));
    }

    private void openOverlaySettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + activity.getPackageName()));
        if (!launchSettings(intent)) openAppSettings();
    }

    private boolean launchSettings(Intent intent) {
        try {
            awaitingResult = true;
            settingsLauncher.launch(intent);
            return true;
        } catch (Exception e) {
            awaitingResult = false;
            return false;
        }
    }

    private boolean isGone() {
        return activity.isFinishing() || activity.isDestroyed();
    }
}
