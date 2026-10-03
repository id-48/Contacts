package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.onboarding;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.PermissionSheet;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityDefaultPhoneBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.main.MainActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PermissionManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class DefaultPhoneActivity extends BaseActivity {

    private enum Requirement { CALL_LOG, CONTACTS, OVERLAY }

    private static final int MAX_ROLE_PROMPTS = 2;
    private static final long AUTO_DENIED_WINDOW_MS = 500L;

    private ActivityDefaultPhoneBinding binding;
    private final EnumSet<Requirement> asked = EnumSet.noneOf(Requirement.class);
    private AlertDialog settingsDialog;
    private Requirement settingsDialogFor;
    private AlertDialog defaultDialog;
    private int rolePrompts;
    private long roleLaunchTime;
    private boolean awaitingResult;
    private boolean finishing;
    private boolean notificationAsked;

    private final ActivityResultLauncher<Intent> roleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (SystemClock.elapsedRealtime() - roleLaunchTime < AUTO_DENIED_WINDOW_MS) {
                    rolePrompts = MAX_ROLE_PROMPTS;
                }
                onRoleResult();
            });

    private final ActivityResultLauncher<Intent> defaultAppsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> onRoleResult());

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> onReturned());

    private final ActivityResultLauncher<String> notificationLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                awaitingResult = false;
                openMain();
            });

    private final ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> onReturned());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDefaultPhoneBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        if (savedInstanceState != null) {
            awaitingResult = savedInstanceState.getBoolean("awaiting_result");
            rolePrompts = savedInstanceState.getInt("role_prompts");
            notificationAsked = savedInstanceState.getBoolean("notification_asked");
            int[] steps = savedInstanceState.getIntArray("asked");
            if (steps != null) {
                for (int step : steps) {
                    asked.add(Requirement.values()[step]);
                }
            }
        }

        binding.defaultIllustration.setScaleX(0.9f);
        binding.defaultIllustration.setScaleY(0.9f);
        binding.defaultIllustration.setAlpha(0f);
        binding.defaultIllustration.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(380).start();

        binding.setDefaultButton.setOnClickListener(v -> requestRole());
        binding.skipButton.setOnClickListener(v -> {
            if (isDefaultSatisfied()) {
                showPermissions();
            } else {
                showDefaultRequiredDialog();
            }
        });

        if (savedInstanceState == null && PhoneService.isDefaultDialer(this)) {
            binding.getRoot().post(this::showPermissions);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (defaultDialog != null && defaultDialog.isShowing() && isDefaultSatisfied()) {
            defaultDialog.dismiss();
            showPermissions();
        }
        if (settingsDialog != null && settingsDialog.isShowing() && isSatisfied(settingsDialogFor)) {
            settingsDialog.dismiss();
            continueFlow();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        int[] steps = new int[asked.size()];
        int i = 0;
        for (Requirement requirement : asked) {
            steps[i++] = requirement.ordinal();
        }
        outState.putIntArray("asked", steps);
        outState.putBoolean("awaiting_result", awaitingResult);
        outState.putInt("role_prompts", rolePrompts);
        outState.putBoolean("notification_asked", notificationAsked);
    }

    private boolean isDefaultSatisfied() {
        return PhoneService.isDefaultDialer(this) || PhoneService.createDefaultDialerIntent(this) == null;
    }

    private void requestRole() {
        if (isDefaultSatisfied()) {
            showPermissions();
            return;
        }
        if (rolePrompts >= MAX_ROLE_PROMPTS) {
            openDefaultAppsSettings();
            return;
        }
        rolePrompts++;
        try {
            roleLaunchTime = SystemClock.elapsedRealtime();
            roleLauncher.launch(PhoneService.createDefaultDialerIntent(this));
        } catch (Exception e) {
            openDefaultAppsSettings();
        }
    }

    private void openDefaultAppsSettings() {
        try {
            defaultAppsLauncher.launch(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
        } catch (Exception e) {
            showPermissions();
        }
    }

    private void onRoleResult() {
        if (isFinishing() || finishing) {
            return;
        }
        if (isDefaultSatisfied()) {
            if (defaultDialog != null && defaultDialog.isShowing()) {
                defaultDialog.dismiss();
            }
            showPermissions();
        } else {
            showDefaultRequiredDialog();
        }
    }

    private void showDefaultRequiredDialog() {
        if (defaultDialog != null && defaultDialog.isShowing()) {
            return;
        }
        boolean viaSettings = rolePrompts >= MAX_ROLE_PROMPTS;
        defaultDialog = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_Dialog)
                .setTitle(R.string.default_required_title)
                .setMessage(viaSettings ? R.string.default_required_settings_body : R.string.default_required_body)
                .setCancelable(false)
                .setPositiveButton(viaSettings ? R.string.open_settings : R.string.set_as_default,
                        (d, w) -> requestRole())
                .show();
    }

    private void showPermissions() {
        if (isFinishing() || finishing) {
            return;
        }
        if (!isDefaultSatisfied()) {
            showDefaultRequiredDialog();
            return;
        }
        List<PermissionSheet.Item> items = new ArrayList<>();
        if (!isSatisfied(Requirement.CALL_LOG)) {
            items.add(new PermissionSheet.Item(R.drawable.ic_phone_in_talk,
                    R.string.perm_sheet_call_history_title, R.string.perm_sheet_call_history_body));
        }
        if (!isSatisfied(Requirement.CONTACTS)) {
            items.add(new PermissionSheet.Item(R.drawable.ic_person,
                    R.string.perm_sheet_contacts_title, R.string.perm_sheet_contacts_body));
        }
        if (!isSatisfied(Requirement.OVERLAY)) {
            items.add(new PermissionSheet.Item(R.drawable.ic_layers,
                    R.string.perm_sheet_overlay_title, R.string.perm_sheet_overlay_body));
        }
        if (items.isEmpty()) {
            complete();
            return;
        }
        PermissionSheet.show(this, items, this::continueFlow, null);
    }

    private void onReturned() {
        awaitingResult = false;
        continueFlow();
    }

    private void continueFlow() {
        if (isFinishing() || finishing || awaitingResult) {
            return;
        }
        Requirement next = firstMissing();
        if (next == null) {
            complete();
            return;
        }
        if (asked.add(next)) {
            request(next);
        } else {
            showSettingsDialog(next);
        }
    }

    private Requirement firstMissing() {
        for (Requirement requirement : Requirement.values()) {
            if (!isSatisfied(requirement)) {
                return requirement;
            }
        }
        return null;
    }

    private boolean isSatisfied(Requirement requirement) {
        if (requirement == null) {
            return true;
        }
        switch (requirement) {
            case CALL_LOG:
                return PermissionManager.isGranted(this, PermissionManager.Group.CALL_LOG);
            case CONTACTS:
                return PermissionManager.isGranted(this, PermissionManager.Group.CONTACTS);
            default:
                return Settings.canDrawOverlays(this);
        }
    }

    private PermissionManager.Group groupFor(Requirement requirement) {
        return requirement == Requirement.CALL_LOG
                ? PermissionManager.Group.CALL_LOG : PermissionManager.Group.CONTACTS;
    }

    private void request(Requirement requirement) {
        if (requirement == Requirement.OVERLAY) {
            openOverlaySettings();
            return;
        }
        PermissionManager.Group group = groupFor(requirement);
        if (PermissionManager.getStatus(this, group) == PermissionManager.Status.PERMANENTLY_DENIED) {
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
        String message;
        if (overlay) {
            message = getString(R.string.perm_required_overlay_body);
        } else {
            int title = requirement == Requirement.CALL_LOG
                    ? R.string.perm_sheet_call_history_title : R.string.perm_sheet_contacts_title;
            message = getString(R.string.perm_required_settings_body, getString(title));
        }
        settingsDialogFor = requirement;
        settingsDialog = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_Dialog)
                .setTitle(R.string.perm_required_title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(overlay ? R.string.allow : R.string.open_settings, (d, w) -> {
                    if (overlay) {
                        openOverlaySettings();
                    } else {
                        openAppSettings();
                    }
                })
                .show();
    }

    private void openAppSettings() {
        launchSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null)));
    }

    private void openOverlaySettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        if (!launchSettings(intent)) {
            openAppSettings();
        }
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

    private void complete() {
        if (finishing || awaitingResult) {
            return;
        }
        if (shouldAskNotifications()) {
            notificationAsked = true;
            PermissionManager.markRequested(PermissionManager.Group.NOTIFICATIONS);
            awaitingResult = true;
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        openMain();
    }

    private boolean shouldAskNotifications() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !notificationAsked
                && !PermissionManager.isGranted(this, PermissionManager.Group.NOTIFICATIONS)
                && PermissionManager.getStatus(this, PermissionManager.Group.NOTIFICATIONS)
                != PermissionManager.Status.PERMANENTLY_DENIED;
    }

    private void openMain() {
        if (finishing) {
            return;
        }
        finishing = true;
        StorageService.setOnboardingDone(true);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
