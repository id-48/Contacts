package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telecom.PhoneAccountHandle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.AppBottomSheet;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivitySettingsBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.call.IncomingCallControlView;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.lock.PasscodeActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.AppLockManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.IntentUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.widget.QuickActionsWidgetProvider;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BaseActivity {

    private ActivitySettingsBinding binding;
    private ActivityResultLauncher<Intent> lockSetupLauncher;
    private ActivityResultLauncher<Intent> lockVerifyLauncher;
    private ActivityResultLauncher<Intent> ringtoneLauncher;
    private String pendingRingtone;
    private boolean enablingLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lockSetupLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                StorageService.setAppLockEnabled(true);
                AppLockManager.unlock();
                Toast.makeText(this, R.string.passcode_enabled, Toast.LENGTH_SHORT).show();
            }
            refresh();
        });
        lockVerifyLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                StorageService.setAppLockEnabled(enablingLock);
                if (enablingLock) {
                    AppLockManager.unlock();
                }
                Toast.makeText(this, enablingLock ? R.string.passcode_enabled : R.string.passcode_disabled,
                        Toast.LENGTH_SHORT).show();
            }
            refresh();
        });
        ringtoneLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                return;
            }
            Uri picked = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            applyRingtone(picked == null ? "" : picked.toString());
        });

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        binding.topBar.topTitle.setText(R.string.settings);
        setupBack(binding.topBar.backButton);
        setupBackAd(AdScreens.SETTINGS_BACK);
        showNativeAds(binding.nativeBigContainer, binding.nativeSmallContainer, AdScreens.SETTINGS);

        binding.simRow.setOnClickListener(v -> showSimOptions());
        binding.speedDialRow.setOnClickListener(v -> open(AdScreens.SETTINGS_SPEED_DIAL, SpeedDialActivity.class));
        binding.voicemailRow.setOnClickListener(v -> open(AdScreens.SETTINGS_VOICEMAIL, VoicemailActivity.class));
        binding.flashRow.setOnCheckedListener(checked -> {
            if (checked && !hasFlash()) {
                binding.flashRow.setChecked(false);
                Toast.makeText(this, R.string.flash_unavailable, Toast.LENGTH_SHORT).show();
                return;
            }
            StorageService.setFlashOnCall(checked);
        });
        binding.answerPositionRow.setOnClickListener(v -> showFullscreen(AdScreens.SETTINGS_ANSWER_POSITION,
                () -> startActivity(OptionPickerActivity.intent(this, OptionPickerActivity.MODE_ANSWER_POSITION))));
        binding.callStyleRow.setOnClickListener(v -> open(AdScreens.SETTINGS_CALL_STYLE, CallStyleActivity.class));
        binding.announcerRow.setOnCheckedListener(StorageService::setCallAnnouncer);
        binding.blockedRow.setOnClickListener(v -> open(AdScreens.SETTINGS_BLOCKED, BlockedNumbersActivity.class));
        binding.quickResponseRow.setOnClickListener(v -> open(AdScreens.SETTINGS_QUICK_RESPONSE,
                QuickResponseActivity.class));

        binding.fakeCallRow.setOnClickListener(v -> open(AdScreens.SETTINGS_FAKE_CALL, FakeCallActivity.class));

        binding.vaultRow.setOnClickListener(v -> open(AdScreens.SETTINGS_VAULT, VaultActivity.class));
        binding.appLockRow.setOnCheckedListener(this::onAppLockToggled);
        binding.spamRow.setOnCheckedListener(checked -> {
            StorageService.setSpamShieldEnabled(checked);
            if (checked) {
                Toast.makeText(this, R.string.spam_shield_on, Toast.LENGTH_LONG).show();
            }
        });

        binding.languageRow.setOnClickListener(v -> showFullscreen(AdScreens.SETTINGS_LANGUAGE,
                () -> startActivity(LanguageActivity.intent(this, false))));
        binding.themeRow.setOnClickListener(v -> showFullscreen(AdScreens.SETTINGS_THEME,
                () -> startActivity(ThemeActivity.intent(this, false))));
        binding.sortRow.setOnClickListener(v -> showFullscreen(AdScreens.SETTINGS_SORT_ORDER,
                () -> startActivity(OptionPickerActivity.intent(this, OptionPickerActivity.MODE_SORT_ORDER))));
        binding.nameFormatRow.setOnClickListener(v -> showFullscreen(AdScreens.SETTINGS_NAME_FORMAT,
                () -> startActivity(OptionPickerActivity.intent(this, OptionPickerActivity.MODE_NAME_FORMAT))));

        binding.ringtoneRow.setOnClickListener(v -> showFullscreen(AdScreens.SETTINGS_RINGTONE, this::pickRingtone));
        binding.dialpadSoundRow.setOnCheckedListener(StorageService::setDialpadSoundEnabled);
        binding.vibrateRow.setOnCheckedListener(StorageService::setVibrateOnAnswer);

        binding.accessibilityRow.setOnClickListener(v -> {
            AppLockManager.markExternalLaunch();
            IntentUtils.safeStart(this, new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });
        binding.mergeRow.setOnClickListener(v -> open(AdScreens.SETTINGS_MERGE, MergeDuplicatesActivity.class));
        binding.importExportRow.setOnClickListener(v -> open(AdScreens.SETTINGS_IMPORT_EXPORT,
                ImportExportActivity.class));
        binding.syncRow.setOnClickListener(v -> open(AdScreens.SETTINGS_SYNC, SyncAccountsActivity.class));
        binding.widgetRow.setOnClickListener(v -> showFullscreen(AdScreens.SETTINGS_WIDGET, this::pinWidget));

        binding.permissionsRow.setOnClickListener(v -> open(AdScreens.SETTINGS_PERMISSIONS, PermissionsActivity.class));
        binding.privacyRow.setOnClickListener(v -> PrivacyPolicyActivity.open(this));
        binding.shareRow.setOnClickListener(v -> IntentUtils.shareApp(this));
        binding.rateRow.setOnClickListener(v -> IntentUtils.rateApp(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pendingRingtone != null && canWriteSettings()) {
            String value = pendingRingtone;
            pendingRingtone = null;
            applyRingtone(value);
        }
        refresh();
    }

    private void open(String adKey, Class<?> screen) {
        showFullscreen(adKey, () -> startActivity(new Intent(this, screen)));
    }

    private void refresh() {
        binding.simRow.setValue(PhoneService.simPreferenceLabel(this));
        binding.flashRow.setChecked(StorageService.isFlashOnCall());
        binding.answerPositionRow.setValue(OptionPickerActivity.label(this, OptionPickerActivity.MODE_ANSWER_POSITION));
        binding.callStyleRow.setSubtitle(getString(IncomingCallControlView.styleName(StorageService.getCallStyle())));
        binding.announcerRow.setChecked(StorageService.isCallAnnouncer());
        binding.appLockRow.setChecked(StorageService.isAppLockEnabled());
        binding.spamRow.setChecked(StorageService.isSpamShieldEnabled());
        binding.languageRow.setValue(LanguageActivity.currentLabel(this));
        binding.themeRow.setValue(ThemeActivity.label(this, StorageService.getThemeMode()));
        binding.sortRow.setValue(OptionPickerActivity.label(this, OptionPickerActivity.MODE_SORT_ORDER));
        binding.nameFormatRow.setValue(OptionPickerActivity.label(this, OptionPickerActivity.MODE_NAME_FORMAT));
        binding.dialpadSoundRow.setChecked(StorageService.isDialpadSoundEnabled());
        binding.vibrateRow.setChecked(StorageService.isVibrateOnAnswer());
        loadRingtoneTitle();
    }

    private void showSimOptions() {
        List<PhoneAccountHandle> accounts = PhoneService.getCallAccounts(this);
        if (accounts.size() <= 1) {
            Toast.makeText(this, R.string.sim_single, Toast.LENGTH_SHORT).show();
            return;
        }
        String current = StorageService.getSimPreference();
        List<AppBottomSheet.Option> options = new ArrayList<>();
        options.add(new AppBottomSheet.Option(R.drawable.ic_sim_card, getString(R.string.sim_always_ask),
                () -> setSim(PhoneService.SIM_ASK)).checked(PhoneService.SIM_ASK.equals(current)));
        for (PhoneAccountHandle handle : accounts) {
            options.add(new AppBottomSheet.Option(R.drawable.ic_sim_card, PhoneService.accountLabel(this, handle),
                    () -> setSim(handle.getId())).checked(handle.getId().equals(current)));
        }
        options.add(new AppBottomSheet.Option(R.drawable.ic_call, getString(R.string.sim_system_default),
                () -> setSim("")).checked(current == null || current.isEmpty()));
        AppBottomSheet.showOptions(this, getString(R.string.sim_preference), options);
    }

    private void setSim(String value) {
        StorageService.setSimPreference(value);
        refresh();
    }

    private boolean hasFlash() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    private void onAppLockToggled(boolean checked) {
        binding.appLockRow.setChecked(!checked);
        enablingLock = checked;
        if (!checked) {
            lockVerifyLauncher.launch(PasscodeActivity.verifyIntent(this));
        } else if (AppLockManager.hasPasscode()) {
            showFullscreen(AdScreens.SETTINGS_APP_LOCK, () -> lockVerifyLauncher.launch(PasscodeActivity.verifyIntent(this)));
        } else {
            showFullscreen(AdScreens.SETTINGS_APP_LOCK, () -> lockSetupLauncher.launch(PasscodeActivity.setupIntent(this)));
        }
    }

    private boolean canWriteSettings() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(this);
    }

    private void pickRingtone() {
        Uri current = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE);
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.ringtones))
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current);
        AppLockManager.markExternalLaunch();
        try {
            ringtoneLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
        }
    }

    private void applyRingtone(String value) {
        if (!canWriteSettings()) {
            pendingRingtone = value;
            Toast.makeText(this, R.string.ringtone_needs_permission, Toast.LENGTH_LONG).show();
            AppLockManager.markExternalLaunch();
            IntentUtils.safeStart(this, new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
            return;
        }
        try {
            RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE,
                    value.isEmpty() ? null : Uri.parse(value));
            Toast.makeText(this, R.string.ringtone_saved, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
        }
        loadRingtoneTitle();
    }

    private void loadRingtoneTitle() {
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            String title;
            try {
                Uri uri = RingtoneManager.getActualDefaultRingtoneUri(app, RingtoneManager.TYPE_RINGTONE);
                if (uri == null) {
                    title = app.getString(R.string.ringtone_silent);
                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(app, uri);
                    title = ringtone == null ? null : ringtone.getTitle(app);
                }
            } catch (Exception e) {
                title = null;
            }
            String value = title;
            AppExecutors.main(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    binding.ringtoneRow.setValue(value);
                }
            });
        });
    }

    private void pinWidget() {
        AppWidgetManager manager = getSystemService(AppWidgetManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null
                && manager.isRequestPinAppWidgetSupported()) {
            manager.requestPinAppWidget(new ComponentName(this, QuickActionsWidgetProvider.class), null, null);
        } else {
            Toast.makeText(this, R.string.widget_manual_hint, Toast.LENGTH_LONG).show();
        }
    }
}
