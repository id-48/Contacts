package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.PermissionRequester;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.TileRows;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityImportExportBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemTileRowBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.AppLockManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PermissionManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels.ObservingViewModel;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ImportExportActivity extends BaseActivity {

    private static class Card {
        String name;
        String given;
        String family;
        String middle;
        String prefix;
        String suffix;
        String org;
        String title;
        String note;
        final List<String[]> phones = new ArrayList<>();
        final List<String[]> emails = new ArrayList<>();
    }

    private static class ExportOption {
        final String label;
        final String type;
        final String name;
        final List<String> lookupKeys = new ArrayList<>();

        ExportOption(String label, String type, String name) {
            this.label = label;
            this.type = type;
            this.name = name;
        }
    }

    private ActivityImportExportBinding binding;
    private PermissionRequester permissionRequester;
    private ActivityResultLauncher<String[]> openDocument;
    private ActivityResultLauncher<String> createDocument;
    private boolean exportMode;
    private Uri importUri;
    private int importAccount;
    private int exportOption;
    private boolean busy;
    private List<ContactsService.AccountOption> accounts = new ArrayList<>();
    private final List<ExportOption> exportOptions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionRequester = new PermissionRequester(this, this::loadData);
        openDocument = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onFilePicked);
        createDocument = registerForActivityResult(new ActivityResultContracts.CreateDocument("text/x-vcard"),
                this::onExportTarget);
        binding = ActivityImportExportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        binding.topBar.topTitle.setText(R.string.import_export);
        setupBack(binding.topBar.backButton);
        setupBackAd(AdScreens.IMPORT_EXPORT_BACK);
        showNativeBig(binding.ads.nativeBigContainer, AdScreens.IMPORT_EXPORT);

        binding.tabs.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                exportMode = checkedId == R.id.exportTab;
                HapticUtils.tick(group);
                render();
            }
        });
        binding.chooseFile.setOnClickListener(v -> {
            AppLockManager.markExternalLaunch();
            openDocument.launch(new String[]{"text/x-vcard", "text/vcard", "text/directory", "application/octet-stream",
                    "text/plain"});
        });
        binding.actionButton.setOnClickListener(v -> {
            if (exportMode) {
                showFullscreen(AdScreens.IMPORT_EXPORT_EXPORT, this::startExport);
            } else {
                startImport();
            }
        });
        render();
        loadData();
    }

    private void loadData() {
        if (!ContactsService.canRead(this) || !ContactsService.canWrite(this)) {
            permissionRequester.request(this, PermissionManager.Group.CONTACTS);
            return;
        }
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            List<ContactsService.AccountOption> loadedAccounts = ContactsService.getAccounts(app);
            List<ExportOption> options = buildExportOptions(app);
            AppExecutors.main(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                accounts = loadedAccounts;
                exportOptions.clear();
                exportOptions.addAll(options);
                render();
            });
        });
    }

    private List<ExportOption> buildExportOptions(Context context) {
        List<ExportOption> options = new ArrayList<>();
        ExportOption all = new ExportOption(getString(R.string.export_all), null, null);
        options.add(all);
        Map<Long, String> lookup = new HashMap<>();
        for (ContactModel contact : ContactsService.getContacts(context, true)) {
            if (!TextUtils.isEmpty(contact.lookupKey)) {
                lookup.put(contact.id, contact.lookupKey);
                all.lookupKeys.add(contact.lookupKey);
            }
        }
        Map<String, ExportOption> byAccount = new HashMap<>();
        Map<String, Set<Long>> seen = new HashMap<>();
        String[] projection = {ContactsContract.RawContacts.CONTACT_ID, ContactsContract.RawContacts.ACCOUNT_TYPE,
                ContactsContract.RawContacts.ACCOUNT_NAME};
        try (Cursor cursor = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, projection,
                ContactsContract.RawContacts.DELETED + "=0", null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                long contactId = cursor.getLong(0);
                String type = cursor.getString(1);
                String name = cursor.getString(2);
                String key = lookup.get(contactId);
                if (key == null || TextUtils.isEmpty(name)) {
                    continue;
                }
                String id = type + "/" + name;
                ExportOption option = byAccount.get(id);
                if (option == null) {
                    option = new ExportOption(name, type, name);
                    byAccount.put(id, option);
                    seen.put(id, new HashSet<>());
                    options.add(option);
                }
                if (seen.get(id).add(contactId)) {
                    option.lookupKeys.add(key);
                }
            }
        } catch (Exception ignored) {
        }
        return options;
    }

    private void render() {
        binding.heroCard.setVisibility(exportMode ? View.GONE : View.VISIBLE);
        binding.heroBody.setText(R.string.import_body);
        binding.fileName.setVisibility(!exportMode && importUri != null ? View.VISIBLE : View.GONE);
        binding.accountTitle.setText(exportMode ? R.string.export_tab : R.string.import_save_to);
        binding.actionButton.setText(exportMode ? R.string.export_contacts : R.string.import_contacts);
        binding.actionButton.setIconResource(exportMode ? R.drawable.ic_import_export : R.drawable.ic_file);
        binding.actionButton.setEnabled(!busy);
        binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.accountGroup.removeAllViews();
        if (exportMode) {
            for (int i = 0; i < exportOptions.size(); i++) {
                ExportOption option = exportOptions.get(i);
                addChoice(i, option.label, getResources().getQuantityString(R.plurals.contacts_count,
                        option.lookupKeys.size(), option.lookupKeys.size()),
                        i == 0 ? R.drawable.ic_contacts : R.drawable.ic_sync, i == exportOption);
            }
        } else {
            for (int i = 0; i < accounts.size(); i++) {
                ContactsService.AccountOption account = accounts.get(i);
                addChoice(i, account.label, null, account.type == null ? R.drawable.ic_phone_in_talk : R.drawable.ic_sync,
                        i == importAccount);
            }
        }
        binding.accountTitle.setVisibility(binding.accountGroup.getChildCount() == 0 ? View.GONE : View.VISIBLE);
        binding.accountGroup.setVisibility(binding.accountGroup.getChildCount() == 0 ? View.GONE : View.VISIBLE);
    }

    private void addChoice(int index, String title, String subtitle, int icon, boolean checked) {
        ItemTileRowBinding row = TileRows.add(binding.accountGroup, title, subtitle);
        TileRows.icon(row, icon);
        android.widget.RadioButton radio = new android.widget.RadioButton(this);
        radio.setChecked(checked);
        radio.setClickable(false);
        radio.setFocusable(false);
        row.tileActions.addView(radio);
        row.tileRow.setOnClickListener(v -> {
            HapticUtils.tick(v);
            if (exportMode) {
                exportOption = index;
            } else {
                importAccount = index;
            }
            render();
        });
    }

    private void onFilePicked(Uri uri) {
        if (uri == null) {
            return;
        }
        importUri = uri;
        String name = null;
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(0);
            }
        } catch (Exception ignored) {
        }
        binding.fileName.setText(TextUtils.isEmpty(name) ? uri.getLastPathSegment() : name);
        render();
    }

    private void startImport() {
        if (busy) {
            return;
        }
        if (importUri == null) {
            Toast.makeText(this, R.string.import_no_file, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ContactsService.canWrite(this)) {
            permissionRequester.request(this, PermissionManager.Group.CONTACTS);
            return;
        }
        ContactsService.AccountOption account = accounts.isEmpty() ? null : accounts.get(Math.min(importAccount,
                accounts.size() - 1));
        Uri uri = importUri;
        busy = true;
        render();
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            int imported;
            try {
                imported = importCards(app, parse(app, uri), account);
            } catch (Exception e) {
                imported = -1;
            }
            int result = imported;
            AppExecutors.main(() -> {
                busy = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (result < 0) {
                    Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show();
                } else {
                    ObservingViewModel.invalidateAll();
                    HapticUtils.confirm(binding.actionButton);
                    Toast.makeText(this, getString(R.string.import_done, result), Toast.LENGTH_SHORT).show();
                    importUri = null;
                }
                loadData();
                render();
            });
        });
    }

    private void startExport() {
        if (busy) {
            return;
        }
        if (exportOptions.isEmpty() || exportOptions.get(Math.min(exportOption, exportOptions.size() - 1))
                .lookupKeys.isEmpty()) {
            Toast.makeText(this, R.string.export_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        AppLockManager.markExternalLaunch();
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date());
        createDocument.launch("contacts_" + stamp + ".vcf");
    }

    private void onExportTarget(Uri target) {
        if (target == null) {
            return;
        }
        ExportOption option = exportOptions.get(Math.min(exportOption, exportOptions.size() - 1));
        List<String> keys = new ArrayList<>(option.lookupKeys);
        busy = true;
        render();
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            int exported = 0;
            try (OutputStream out = app.getContentResolver().openOutputStream(target, "wt")) {
                if (out == null) {
                    throw new IllegalStateException();
                }
                byte[] buffer = new byte[8192];
                for (String key : keys) {
                    Uri card = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, key);
                    try (InputStream in = app.getContentResolver().openInputStream(card)) {
                        if (in == null) {
                            continue;
                        }
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        exported++;
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception e) {
                exported = -1;
            }
            int result = exported;
            AppExecutors.main(() -> {
                busy = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (result <= 0) {
                    Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
                } else {
                    HapticUtils.confirm(binding.actionButton);
                    Toast.makeText(this, getString(R.string.export_done, result), Toast.LENGTH_SHORT).show();
                }
                render();
            });
        });
    }

    private static List<Card> parse(Context context, Uri uri) throws Exception {
        List<String> lines = new ArrayList<>();
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IllegalStateException();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if ((line.startsWith(" ") || line.startsWith("\t")) && !lines.isEmpty()) {
                    lines.set(lines.size() - 1, lines.get(lines.size() - 1) + line.substring(1));
                } else if (!lines.isEmpty() && lines.get(lines.size() - 1).endsWith("=")
                        && lines.get(lines.size() - 1).toUpperCase(Locale.ROOT).contains("QUOTED-PRINTABLE")) {
                    String previous = lines.get(lines.size() - 1);
                    lines.set(lines.size() - 1, previous.substring(0, previous.length() - 1) + line);
                } else {
                    lines.add(line);
                }
            }
        }
        List<Card> cards = new ArrayList<>();
        Card current = null;
        for (String raw : lines) {
            int colon = raw.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String head = raw.substring(0, colon);
            String value = raw.substring(colon + 1);
            String[] parts = head.split(";");
            String property = parts[0].toUpperCase(Locale.ROOT);
            int dot = property.indexOf('.');
            if (dot >= 0) {
                property = property.substring(dot + 1);
            }
            String params = head.toUpperCase(Locale.ROOT);
            if (params.contains("QUOTED-PRINTABLE")) {
                value = decodeQuotedPrintable(value);
            }
            if ("BEGIN".equals(property) && "VCARD".equalsIgnoreCase(value.trim())) {
                current = new Card();
                continue;
            }
            if ("END".equals(property) && "VCARD".equalsIgnoreCase(value.trim())) {
                if (current != null) {
                    cards.add(current);
                }
                current = null;
                continue;
            }
            if (current == null) {
                continue;
            }
            switch (property) {
                case "FN":
                    current.name = unescape(value);
                    break;
                case "N": {
                    String[] n = value.split("(?<!\\\\);", -1);
                    current.family = n.length > 0 ? unescape(n[0]) : null;
                    current.given = n.length > 1 ? unescape(n[1]) : null;
                    current.middle = n.length > 2 ? unescape(n[2]) : null;
                    current.prefix = n.length > 3 ? unescape(n[3]) : null;
                    current.suffix = n.length > 4 ? unescape(n[4]) : null;
                    break;
                }
                case "TEL":
                    if (!value.trim().isEmpty()) {
                        current.phones.add(new String[]{value.replace("tel:", "").trim(), params});
                    }
                    break;
                case "EMAIL":
                    if (!value.trim().isEmpty()) {
                        current.emails.add(new String[]{unescape(value).trim(), params});
                    }
                    break;
                case "ORG":
                    current.org = unescape(value.split("(?<!\\\\);")[0]);
                    break;
                case "TITLE":
                    current.title = unescape(value);
                    break;
                case "NOTE":
                    current.note = unescape(value);
                    break;
                default:
                    break;
            }
        }
        return cards;
    }

    private static String unescape(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\\n", "\n").replace("\\N", "\n").replace("\\,", ",").replace("\\;", ";")
                .replace("\\\\", "\\").trim();
    }

    private static String decodeQuotedPrintable(String value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            if (b == '=' && i + 2 < bytes.length) {
                int high = Character.digit(bytes[i + 1], 16);
                int low = Character.digit(bytes[i + 2], 16);
                if (high >= 0 && low >= 0) {
                    out.write((high << 4) + low);
                    i += 2;
                    continue;
                }
            }
            out.write(b);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static int phoneType(String params) {
        if (params.contains("CELL") || params.contains("MOBILE")) {
            return ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
        }
        if (params.contains("WORK")) {
            return ContactsContract.CommonDataKinds.Phone.TYPE_WORK;
        }
        if (params.contains("HOME")) {
            return ContactsContract.CommonDataKinds.Phone.TYPE_HOME;
        }
        return ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
    }

    private static int emailType(String params) {
        if (params.contains("WORK")) {
            return ContactsContract.CommonDataKinds.Email.TYPE_WORK;
        }
        if (params.contains("HOME")) {
            return ContactsContract.CommonDataKinds.Email.TYPE_HOME;
        }
        return ContactsContract.CommonDataKinds.Email.TYPE_OTHER;
    }

    private static int importCards(Context context, List<Card> cards, ContactsService.AccountOption account)
            throws Exception {
        if (cards.isEmpty()) {
            throw new IllegalStateException();
        }
        int imported = 0;
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        int pending = 0;
        for (Card card : cards) {
            boolean hasName = !TextUtils.isEmpty(card.name) || !TextUtils.isEmpty(card.given)
                    || !TextUtils.isEmpty(card.family);
            if (!hasName && card.phones.isEmpty() && card.emails.isEmpty()) {
                continue;
            }
            int back = ops.size();
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account == null ? null : account.type)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account == null ? null : account.name)
                    .build());
            if (hasName) {
                ContentProviderOperation.Builder name = ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, back)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
                if (!TextUtils.isEmpty(card.given) || !TextUtils.isEmpty(card.family)) {
                    name.withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, card.given)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, card.family)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, card.middle)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX, card.prefix)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, card.suffix);
                } else {
                    name.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, card.name);
                }
                ops.add(name.build());
            }
            for (String[] phone : card.phones) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, back)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone[0])
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType(phone[1]))
                        .build());
            }
            for (String[] email : card.emails) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, back)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email[0])
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, emailType(email[1]))
                        .build());
            }
            if (!TextUtils.isEmpty(card.org) || !TextUtils.isEmpty(card.title)) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, back)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, card.org)
                        .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, card.title)
                        .build());
            }
            if (!TextUtils.isEmpty(card.note)) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, back)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, card.note)
                        .build());
            }
            pending++;
            if (ops.size() > 350) {
                context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                imported += pending;
                pending = 0;
                ops.clear();
            }
        }
        if (!ops.isEmpty()) {
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            imported += pending;
        }
        return imported;
    }
}
