package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.ContactPickerSheet;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.PermissionRequester;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.TileRows;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityListScreenBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemTileRowBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.contactdetails.ContactDetailsActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.lock.PasscodeActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.AppLockManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PermissionManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels.ObservingViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VaultActivity extends BaseActivity {

    private ActivityListScreenBinding binding;
    private PermissionRequester permissionRequester;
    private ActivityResultLauncher<android.content.Intent> passcodeLauncher;
    private boolean verified;
    private boolean internalLaunch;
    private boolean relockOnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        permissionRequester = new PermissionRequester(this, this::load);
        passcodeLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                verified = true;
                binding.scroll.setVisibility(View.VISIBLE);
                binding.bottomBar.setVisibility(View.VISIBLE);
                load();
            } else {
                finish();
            }
        });
        binding = ActivityListScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        binding.topBar.topTitle.setText(R.string.private_vault);
        setupBack(binding.topBar.backButton);
        setupBackAd(AdScreens.VAULT_BACK);
        binding.bottomButton.setText(R.string.vault_add);
        binding.bottomButton.setIconResource(R.drawable.ic_person_add);
        binding.bottomButton.setOnClickListener(v -> pickContact());
        showNativeBig(binding.ads.nativeBigContainer, AdScreens.VAULT);
        binding.scroll.setVisibility(View.INVISIBLE);
        requestAccess();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (relockOnStart) {
            relockOnStart = false;
            requestAccess();
        } else if (internalLaunch && verified) {
            load();
        }
        internalLaunch = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (verified && !internalLaunch && !isChangingConfigurations() && !isFinishing()) {
            verified = false;
            relockOnStart = true;
            binding.scroll.setVisibility(View.INVISIBLE);
            binding.bottomBar.setVisibility(View.GONE);
        }
    }

    private void requestAccess() {
        if (!AppLockManager.hasPasscode()) {
            Toast.makeText(this, R.string.vault_needs_passcode, Toast.LENGTH_LONG).show();
            passcodeLauncher.launch(PasscodeActivity.setupIntent(this));
        } else {
            passcodeLauncher.launch(PasscodeActivity.verifyIntent(this));
        }
    }

    private void load() {
        if (!verified) {
            return;
        }
        if (!ContactsService.canRead(this)) {
            permissionRequester.request(this, PermissionManager.Group.CONTACTS);
            return;
        }
        binding.progress.setVisibility(View.VISIBLE);
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            Set<String> keys = StorageService.getVaultKeys();
            List<ContactModel> hidden = new ArrayList<>();
            if (!keys.isEmpty()) {
                for (ContactModel contact : ContactsService.getContacts(app, true)) {
                    if (contact.lookupKey != null && keys.contains(contact.lookupKey)) {
                        hidden.add(contact);
                    }
                }
            }
            AppExecutors.main(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    render(hidden);
                }
            });
        });
    }

    private void render(List<ContactModel> contacts) {
        binding.progress.setVisibility(View.GONE);
        binding.listGroup.removeAllViews();
        binding.listGroup.setVisibility(contacts.isEmpty() ? View.GONE : View.VISIBLE);
        if (contacts.isEmpty()) {
            binding.emptyState.setContent(R.drawable.ic_lock, R.string.vault_empty_title, R.string.vault_empty_body);
            binding.emptyState.show(true);
            return;
        }
        binding.emptyState.show(false);
        for (ContactModel contact : contacts) {
            String number = PhoneUtils.isValidNumber(contact.primaryNumber)
                    ? PhoneUtils.format(this, contact.primaryNumber) : null;
            ItemTileRowBinding row = TileRows.add(binding.listGroup, contact.getDisplayName(), number);
            TileRows.avatar(row, contact.name, contact.photoUri);
            if (!TextUtils.isEmpty(number)) {
                TileRows.action(row, R.drawable.ic_call, getString(R.string.call), R.color.primary, v -> {
                    internalLaunch = true;
                    PhoneService.call(this, contact.primaryNumber);
                });
            }
            TileRows.action(row, R.drawable.ic_close, getString(R.string.vault_remove), R.color.text_secondary,
                    v -> setHidden(contact, false));
            row.tileRow.setOnClickListener(v -> {
                internalLaunch = true;
                startActivity(ContactDetailsActivity.intent(this, contact.id, contact.lookupKey));
            });
        }
    }

    private void pickContact() {
        if (!ContactsService.canRead(this)) {
            permissionRequester.request(this, PermissionManager.Group.CONTACTS);
            return;
        }
        ContactPickerSheet.show(this, getString(R.string.vault_pick_title), false, false,
                contact -> !TextUtils.isEmpty(contact.lookupKey), contact -> setHidden(contact, true));
    }

    private void setHidden(ContactModel contact, boolean hidden) {
        Set<String> keys = new HashSet<>(StorageService.getVaultKeys());
        if (hidden) {
            keys.add(contact.lookupKey);
        } else {
            keys.remove(contact.lookupKey);
        }
        StorageService.setVaultKeys(keys);
        ObservingViewModel.invalidateAll();
        HapticUtils.confirm(binding.listGroup);
        Toast.makeText(this, getString(hidden ? R.string.vault_added : R.string.vault_removed,
                contact.getDisplayName()), Toast.LENGTH_SHORT).show();
        load();
    }
}
