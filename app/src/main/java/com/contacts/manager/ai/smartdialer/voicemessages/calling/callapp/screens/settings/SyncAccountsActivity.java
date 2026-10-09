package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.PermissionRequester;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.TileRows;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityListScreenBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemTileRowBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PermissionManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SyncAccountsActivity extends BaseActivity {

    private static class Entry {
        Account account;
        int contacts;
    }

    private ActivityListScreenBinding binding;
    private PermissionRequester permissionRequester;
    private ItemTileRowBinding masterRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionRequester = new PermissionRequester(this, this::load);
        binding = ActivityListScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        binding.topBar.topTitle.setText(R.string.sync_accounts);
        setupBack(binding.topBar.backButton);
        setupBackAd(AdScreens.SYNC_ACCOUNTS_BACK);
        LinearLayout masterCard = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.view_settings_group, binding.headerSlot, false);
        binding.headerSlot.addView(masterCard);
        masterRow = TileRows.add(masterCard, getString(R.string.sync_master), getString(R.string.sync_master_body));
        TileRows.icon(masterRow, R.drawable.ic_sync);
        masterRow.tileSwitch.setVisibility(View.VISIBLE);
        masterRow.tileSwitch.setClickable(false);
        masterRow.tileRow.setOnClickListener(v -> {
            boolean enabled = !ContentResolver.getMasterSyncAutomatically();
            try {
                ContentResolver.setMasterSyncAutomatically(enabled);
            } catch (SecurityException ignored) {
            }
            HapticUtils.tick(v);
            renderMaster();
            load();
        });

        binding.sectionTitle.setText(R.string.sync_connected);
        binding.sectionTitle.setVisibility(View.VISIBLE);
        showNativeBig(binding.ads.nativeBigContainer, AdScreens.SYNC_ACCOUNTS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderMaster();
        load();
    }

    private void renderMaster() {
        boolean enabled;
        try {
            enabled = ContentResolver.getMasterSyncAutomatically();
        } catch (SecurityException e) {
            enabled = false;
        }
        masterRow.tileSwitch.setChecked(enabled);
    }

    private void load() {
        if (!ContactsService.canRead(this)) {
            permissionRequester.request(this, PermissionManager.Group.CONTACTS);
            return;
        }
        binding.progress.setVisibility(View.VISIBLE);
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            List<Entry> entries = findAccounts(app);
            AppExecutors.main(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    render(entries);
                }
            });
        });
    }

    private static List<Entry> findAccounts(Context context) {
        Map<String, Entry> found = new LinkedHashMap<>();
        try {
            for (Account account : AccountManager.get(context).getAccounts()) {
                if (ContentResolver.getIsSyncable(account, ContactsContract.AUTHORITY) > 0) {
                    Entry entry = new Entry();
                    entry.account = account;
                    found.put(account.type + "/" + account.name, entry);
                }
            }
        } catch (SecurityException ignored) {
        }
        String[] projection = {ContactsContract.RawContacts.ACCOUNT_TYPE, ContactsContract.RawContacts.ACCOUNT_NAME};
        try (Cursor cursor = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, projection,
                ContactsContract.RawContacts.DELETED + "=0", null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                String type = cursor.getString(0);
                String name = cursor.getString(1);
                if (TextUtils.isEmpty(type) || TextUtils.isEmpty(name)) {
                    continue;
                }
                String key = type + "/" + name;
                Entry entry = found.get(key);
                if (entry == null) {
                    Account account = new Account(name, type);
                    if (ContentResolver.getIsSyncable(account, ContactsContract.AUTHORITY) <= 0) {
                        continue;
                    }
                    entry = new Entry();
                    entry.account = account;
                    found.put(key, entry);
                }
                entry.contacts++;
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>(found.values());
    }

    private String typeLabel(Account account) {
        if ("com.google".equals(account.type)) {
            return "Google";
        }
        if (account.type.contains("exchange") || account.type.contains("eas")) {
            return "Exchange";
        }
        if (account.type.contains("whatsapp")) {
            return "WhatsApp";
        }
        if (account.type.contains("telegram")) {
            return "Telegram";
        }
        String[] parts = account.type.split("\\.");
        String last = parts[parts.length - 1];
        return last.isEmpty() ? account.type : Character.toUpperCase(last.charAt(0)) + last.substring(1);
    }

    private void render(List<Entry> entries) {
        binding.progress.setVisibility(View.GONE);
        binding.listGroup.removeAllViews();
        boolean empty = entries.isEmpty();
        binding.listGroup.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            binding.emptyState.setContent(R.drawable.ic_sync, R.string.sync_accounts, R.string.sync_empty);
            binding.emptyState.show(true);
            return;
        }
        binding.emptyState.show(false);
        boolean master;
        try {
            master = ContentResolver.getMasterSyncAutomatically();
        } catch (SecurityException e) {
            master = false;
        }
        for (Entry entry : entries) {
            Account account = entry.account;
            String subtitle = typeLabel(account) + "  •  " + getResources().getQuantityString(R.plurals.contacts_count,
                    entry.contacts, entry.contacts);
            ItemTileRowBinding row = TileRows.add(binding.listGroup, account.name, subtitle);
            String label = typeLabel(account);
            TileRows.badge(row, label.isEmpty() ? "#" : label.substring(0, 1).toUpperCase(Locale.getDefault()));
            ImageButton syncNow = TileRows.action(row, R.drawable.ic_sync, getString(R.string.sync_now), R.color.primary,
                    null);
            syncNow.setOnClickListener(v -> requestSync(account, v));
            boolean auto;
            try {
                auto = ContentResolver.getSyncAutomatically(account, ContactsContract.AUTHORITY);
            } catch (SecurityException e) {
                auto = false;
            }
            row.tileSwitch.setVisibility(View.VISIBLE);
            row.tileSwitch.setChecked(auto);
            row.tileSwitch.setEnabled(master);
            row.tileSwitch.setOnCheckedChangeListener((button, checked) -> {
                try {
                    ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, checked);
                    HapticUtils.tick(button);
                } catch (SecurityException e) {
                    button.setChecked(!checked);
                    Toast.makeText(this, R.string.sync_failed, Toast.LENGTH_SHORT).show();
                }
            });
            row.tileRow.setOnClickListener(v -> requestSync(account, syncNow));
        }
    }

    private void requestSync(Account account, View source) {
        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        try {
            ContentResolver.requestSync(account, ContactsContract.AUTHORITY, extras);
            HapticUtils.confirm(source);
            source.animate().rotationBy(360f).setDuration(700).start();
            Toast.makeText(this, R.string.sync_started, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.sync_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
