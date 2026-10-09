package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.PermissionRequester;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.TileRows;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityListScreenBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemTileRowBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PermissionManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels.ObservingViewModel;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MergeDuplicatesActivity extends BaseActivity {

    private static class Group {
        final List<ContactModel> contacts = new ArrayList<>();
        boolean byName;
        boolean selected = true;
        MaterialCheckBox check;
    }

    private ActivityListScreenBinding binding;
    private PermissionRequester permissionRequester;
    private final List<Group> groups = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionRequester = new PermissionRequester(this, this::load);
        binding = ActivityListScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        binding.topBar.topTitle.setText(R.string.merge_duplicates);
        setupBack(binding.topBar.backButton);
        setupBackAd(AdScreens.MERGE_BACK);
        binding.listGroup.setVisibility(View.GONE);
        binding.topBar.topAction.setText(R.string.merge_select_all);
        binding.topBar.topAction.setOnClickListener(v -> {
            boolean all = true;
            for (Group group : groups) {
                all &= group.selected;
            }
            for (Group group : groups) {
                group.selected = !all;
                group.check.setChecked(!all);
            }
            updateButton();
        });
        binding.bottomButton.setIconResource(R.drawable.ic_merge);
        binding.bottomButton.setOnClickListener(v -> showFullscreen(AdScreens.MERGE_SELECTED, this::mergeSelected));
        showNativeBig(binding.ads.nativeBigContainer, AdScreens.MERGE);
        load();
    }

    private void load() {
        if (!ContactsService.canRead(this) || !ContactsService.canWrite(this)) {
            permissionRequester.request(this, PermissionManager.Group.CONTACTS);
            return;
        }
        binding.progress.setVisibility(View.VISIBLE);
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            List<Group> found = findDuplicates(ContactsService.getContacts(app, true));
            AppExecutors.main(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    render(found);
                }
            });
        });
    }

    private static String nameKey(ContactModel contact) {
        if (TextUtils.isEmpty(contact.name) || !contact.name.matches(".*\\p{L}.*")) {
            return null;
        }
        return contact.name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String numberKey(ContactModel contact) {
        if (TextUtils.isEmpty(contact.primaryNumber)) {
            return null;
        }
        String digits = contact.primaryNumber.replaceAll("\\D", "");
        if (digits.length() < 6) {
            return null;
        }
        return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    }

    private static int find(int[] parent, int index) {
        while (parent[index] != index) {
            parent[index] = parent[parent[index]];
            index = parent[index];
        }
        return index;
    }

    private static List<Group> findDuplicates(List<ContactModel> contacts) {
        int size = contacts.size();
        int[] parent = new int[size];
        boolean[] nameLinked = new boolean[size];
        for (int i = 0; i < size; i++) {
            parent[i] = i;
        }
        Map<String, Integer> byName = new HashMap<>();
        Map<String, Integer> byNumber = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ContactModel contact = contacts.get(i);
            String name = nameKey(contact);
            if (name != null) {
                Integer other = byName.get(name);
                if (other == null) {
                    byName.put(name, i);
                } else {
                    parent[find(parent, i)] = find(parent, other);
                    nameLinked[i] = true;
                    nameLinked[other] = true;
                }
            }
            String number = numberKey(contact);
            if (number != null) {
                Integer other = byNumber.get(number);
                if (other == null) {
                    byNumber.put(number, i);
                } else {
                    parent[find(parent, i)] = find(parent, other);
                }
            }
        }
        Map<Integer, Group> grouped = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            int root = find(parent, i);
            Group group = grouped.get(root);
            if (group == null) {
                group = new Group();
                grouped.put(root, group);
            }
            group.contacts.add(contacts.get(i));
            group.byName |= nameLinked[i];
        }
        List<Group> result = new ArrayList<>();
        for (Group group : grouped.values()) {
            if (group.contacts.size() > 1) {
                result.add(group);
            }
        }
        return result;
    }

    private void render(List<Group> found) {
        binding.progress.setVisibility(View.GONE);
        groups.clear();
        groups.addAll(found);
        binding.headerSlot.removeAllViews();
        boolean empty = groups.isEmpty();
        binding.topBar.topAction.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.bottomBar.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            binding.emptyState.setContent(R.drawable.ic_merge, R.string.merge_empty_title, R.string.merge_empty_body);
            binding.emptyState.show(true);
            return;
        }
        binding.emptyState.show(false);
        int margin = getResources().getDimensionPixelSize(R.dimen.space_12);
        for (Group group : groups) {
            LinearLayout card = (LinearLayout) LayoutInflater.from(this)
                    .inflate(R.layout.view_settings_group, binding.headerSlot, false);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
            params.topMargin = margin;
            binding.headerSlot.addView(card, params);

            ContactModel first = group.contacts.get(0);
            ItemTileRowBinding header = TileRows.add(card, getString(R.string.merge_group_title,
                    first.getDisplayName(), group.contacts.size()),
                    getString(group.byName ? R.string.merge_reason_name : R.string.merge_reason_number));
            TileRows.icon(header, R.drawable.ic_merge);
            header.tileSubtitle.setTextColor(getColor(R.color.primary));
            MaterialCheckBox check = new MaterialCheckBox(this);
            check.setChecked(group.selected);
            check.setOnCheckedChangeListener((button, checked) -> {
                group.selected = checked;
                updateButton();
            });
            header.tileActions.addView(check);
            header.tileRow.setOnClickListener(v -> check.toggle());
            group.check = check;

            for (ContactModel contact : group.contacts) {
                String number = PhoneUtils.isValidNumber(contact.primaryNumber)
                        ? PhoneUtils.format(this, contact.primaryNumber) : null;
                ItemTileRowBinding row = TileRows.add(card, contact.getDisplayName(), number);
                TileRows.avatar(row, contact.name, contact.photoUri);
                row.getRoot().setPaddingRelative(getResources().getDimensionPixelSize(R.dimen.space_32),
                        row.getRoot().getPaddingTop(), row.getRoot().getPaddingEnd(), row.getRoot().getPaddingBottom());
            }
        }
        updateButton();
    }

    private int selectedCount() {
        int count = 0;
        for (Group group : groups) {
            if (group.selected) {
                count++;
            }
        }
        return count;
    }

    private void updateButton() {
        int count = selectedCount();
        binding.bottomButton.setText(getString(R.string.merge_selected, count));
        binding.bottomButton.setEnabled(count > 0);
    }

    private void mergeSelected() {
        List<List<Long>> selected = new ArrayList<>();
        for (Group group : groups) {
            if (group.selected) {
                List<Long> ids = new ArrayList<>();
                for (ContactModel contact : group.contacts) {
                    ids.add(contact.id);
                }
                selected.add(ids);
            }
        }
        if (selected.isEmpty()) {
            return;
        }
        binding.bottomButton.setEnabled(false);
        binding.bottomButton.setText(R.string.processing);
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            int merged = 0;
            for (List<Long> contactIds : selected) {
                if (merge(app, contactIds)) {
                    merged++;
                }
            }
            int done = merged;
            AppExecutors.main(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                ObservingViewModel.invalidateAll();
                HapticUtils.confirm(binding.bottomButton);
                Toast.makeText(this, done > 0 ? getString(R.string.merge_done, done) : getString(R.string.merge_failed),
                        Toast.LENGTH_SHORT).show();
                load();
            });
        });
    }

    private static boolean merge(Context context, List<Long> contactIds) {
        List<Long> rawIds = new ArrayList<>();
        for (long contactId : contactIds) {
            try (Cursor cursor = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI,
                    new String[]{ContactsContract.RawContacts._ID},
                    ContactsContract.RawContacts.CONTACT_ID + "=? AND " + ContactsContract.RawContacts.DELETED + "=0",
                    new String[]{String.valueOf(contactId)}, null)) {
                while (cursor != null && cursor.moveToNext()) {
                    rawIds.add(cursor.getLong(0));
                }
            } catch (Exception ignored) {
            }
        }
        if (rawIds.size() < 2) {
            return false;
        }
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        long anchor = rawIds.get(0);
        for (int i = 1; i < rawIds.size(); i++) {
            operations.add(ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI)
                    .withValue(ContactsContract.AggregationExceptions.TYPE,
                            ContactsContract.AggregationExceptions.TYPE_KEEP_TOGETHER)
                    .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID1, anchor)
                    .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID2, rawIds.get(i))
                    .build());
        }
        try {
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operations);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
