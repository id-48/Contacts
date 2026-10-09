package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.onboarding;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.ConfirmDialog;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityDefaultPhoneBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.OnboardingFlow;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.RemoteConfig;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.launcher.LauncherMode;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
public class DefaultPhoneActivity extends BaseActivity {

    private static final int MAX_ROLE_PROMPTS = 2;
    private static final long AUTO_DENIED_WINDOW_MS = 500L;

    private ActivityDefaultPhoneBinding binding;
    private final OnboardingPermissions permissions = new OnboardingPermissions(this, true, this::leave);
    private Dialog defaultDialog;
    private Dialog homeDialog;
    private int rolePrompts;
    private int homePrompts;
    private long roleLaunchTime;
    private long homeLaunchTime;
    private boolean homeUnavailable;
    private boolean finishing;
    private boolean cancelAdShowing;

    private final ActivityResultLauncher<Intent> roleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (SystemClock.elapsedRealtime() - roleLaunchTime < AUTO_DENIED_WINDOW_MS) {
                    rolePrompts = MAX_ROLE_PROMPTS;
                }
                onRoleResult();
            });

    private final ActivityResultLauncher<Intent> defaultAppsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> onRoleResult());

    private final ActivityResultLauncher<Intent> homeLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (SystemClock.elapsedRealtime() - homeLaunchTime < AUTO_DENIED_WINDOW_MS) {
                    homePrompts = MAX_ROLE_PROMPTS;
                }
                onHomeResult();
            });

    private final ActivityResultLauncher<Intent> homeSettingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> onHomeResult());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDefaultPhoneBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        if (savedInstanceState != null) {
            rolePrompts = savedInstanceState.getInt("role_prompts");
            homePrompts = savedInstanceState.getInt("home_prompts");
            permissions.restore(savedInstanceState);
        }

        binding.defaultIllustration.setScaleX(0.9f);
        binding.defaultIllustration.setScaleY(0.9f);
        binding.defaultIllustration.setAlpha(0f);
        binding.defaultIllustration.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(380).start();

        binding.setDefaultButton.setOnClickListener(v -> requestRole());
        binding.skipButton.setOnClickListener(v -> onCancel());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!cancelAdShowing) insist();
            }
        });

        AdsManager.showNativeSmall(this, binding.nativeSmallContainer, RemoteConfig.SCREEN_DEFAULT_PHONE);
        AdsManager.showNativeBig(this, binding.nativeBigContainer, RemoteConfig.SCREEN_DEFAULT_PHONE);
        AdsManager.showBanner(this, binding.bannerContainer, RemoteConfig.SCREEN_DEFAULT_PHONE);

        if (savedInstanceState == null && PhoneService.isDefaultDialer(this)) {
            binding.getRoot().post(this::showPermissions);
        }
    }

    private void onCancel() {
        if (cancelAdShowing) {
            return;
        }
        cancelAdShowing = true;
        AdsManager.showFullscreen(this, RemoteConfig.SCREEN_DEFAULT_PHONE, () -> {
            cancelAdShowing = false;
            insist();
        });
    }

    private void insist() {
        if (isFinishing() || finishing) {
            return;
        }
        if (isDefaultSatisfied()) {
            showPermissions();
        } else {
            showDefaultRequiredDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (defaultDialog != null && defaultDialog.isShowing() && isDefaultSatisfied()) {
            defaultDialog.dismiss();
            showPermissions();
        }
        if (homeDialog != null && homeDialog.isShowing() && isHomeSatisfied()) {
            homeDialog.dismiss();
            showPermissions();
        }
        permissions.onResume();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("role_prompts", rolePrompts);
        outState.putInt("home_prompts", homePrompts);
        permissions.save(outState);
    }

    private boolean isDefaultSatisfied() {
        return PhoneService.isDefaultDialerSatisfied(this);
    }

    private boolean isHomeSatisfied() {
        return homeUnavailable || LauncherMode.isSatisfied(this);
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
        defaultDialog = ConfirmDialog.required(this, getString(R.string.default_required_title),
                getString(viaSettings ? R.string.default_required_settings_body : R.string.default_required_body),
                viaSettings ? R.string.open_settings : R.string.set_as_default, this::requestRole,
                "default_required_dialog");
    }

    private void showPermissions() {
        if (isFinishing() || finishing) {
            return;
        }
        if (!isDefaultSatisfied()) {
            showDefaultRequiredDialog();
            return;
        }
        if (!isHomeSatisfied()) {
            requestHome();
            return;
        }
        LauncherMode.releaseApp();
        permissions.start();
    }

    private void requestHome() {
        if (isHomeSatisfied()) {
            showPermissions();
            return;
        }
        LauncherMode.sync(this);
        LauncherMode.holdApp();
        Intent roleIntent = LauncherMode.createRoleIntent(this);
        if (roleIntent == null || homePrompts >= MAX_ROLE_PROMPTS) {
            openHomeSettings();
            return;
        }
        homePrompts++;
        try {
            homeLaunchTime = SystemClock.elapsedRealtime();
            homeLauncher.launch(roleIntent);
        } catch (Exception e) {
            openHomeSettings();
        }
    }

    private void openHomeSettings() {
        try {
            homeSettingsLauncher.launch(LauncherMode.createSettingsIntent());
        } catch (Exception e) {
            homeUnavailable = true;
            LauncherMode.releaseApp();
            showPermissions();
        }
    }

    private void onHomeResult() {
        if (isFinishing() || finishing) {
            return;
        }
        if (isHomeSatisfied()) {
            if (homeDialog != null && homeDialog.isShowing()) {
                homeDialog.dismiss();
            }
            showPermissions();
        } else {
            showHomeRequiredDialog();
        }
    }

    private void showHomeRequiredDialog() {
        if (homeDialog != null && homeDialog.isShowing()) {
            return;
        }
        boolean viaSettings = homePrompts >= MAX_ROLE_PROMPTS || LauncherMode.createRoleIntent(this) == null;
        homeDialog = ConfirmDialog.required(this, getString(R.string.launcher_required_title),
                getString(viaSettings ? R.string.launcher_required_settings_body : R.string.launcher_required_body),
                viaSettings ? R.string.open_settings : R.string.set_as_default, this::requestHome,
                "launcher_required_dialog");
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) LauncherMode.releaseApp();
        super.onDestroy();
    }

    private void leave() {
        if (finishing) {
            return;
        }
        finishing = true;
        OnboardingFlow.continueFrom(this, OnboardingFlow.Step.DEFAULT_PHONE);
    }
}
