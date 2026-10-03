package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.callhistory;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.AppDialogs;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.IntentKeys;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityCallHistoryBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.IncludeBottomActionBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.CallModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.contactdetails.ContactDetailsActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.editcontact.EditContactActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.CallLogService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.WhatsAppService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.IntentUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CallHistoryActivity extends BaseActivity implements CallEntryAdapter.Listener {

    private ActivityCallHistoryBinding binding;
    private CallEntryAdapter adapter;
    private String number;
    private long contactId;
    private String lookupKey;
    private ContactModel contact;
    private List<CallModel> calls = Collections.emptyList();

    public static Intent intent(Context context, String number) {
        return new Intent(context, CallHistoryActivity.class).putExtra(IntentKeys.EXTRA_NUMBER, number);
    }

    public static Intent contactIntent(Context context, long contactId, String lookupKey) {
        return new Intent(context, CallHistoryActivity.class)
                .putExtra(IntentKeys.EXTRA_CONTACT_ID, contactId)
                .putExtra(IntentKeys.EXTRA_LOOKUP_KEY, lookupKey);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCallHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        number = getIntent().getStringExtra(IntentKeys.EXTRA_NUMBER);
        contactId = getIntent().getLongExtra(IntentKeys.EXTRA_CONTACT_ID, -1);
        lookupKey = getIntent().getStringExtra(IntentKeys.EXTRA_LOOKUP_KEY);

        adapter = new CallEntryAdapter(this);
        binding.historyList.setLayoutManager(new LinearLayoutManager(this));
        binding.historyList.setAdapter(adapter);
        setupBack(binding.backButton);
        bindHeader();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            ContactModel loadedContact = null;
            List<CallModel> loaded;
            if (contactId > 0) {
                long resolved = ContactsService.resolveContactId(app, contactId, lookupKey);
                loadedContact = ContactsService.getContactDetails(app, resolved);
                loaded = CallLogService.getCallsForContact(app, loadedContact, Integer.MAX_VALUE);
            } else {
                loaded = CallLogService.getCallsForNumber(app, number);
                if (!PhoneUtils.isPrivate(number)) {
                    loadedContact = ContactsService.lookupNumber(app, number);
                }
            }
            ContactModel finalContact = loadedContact;
            List<CallModel> finalCalls = loaded;
            AppExecutors.main(() -> {
                if (isDestroyed()) {
                    return;
                }
                contact = finalContact;
                calls = finalCalls;
                if (contact != null && TextUtils.isEmpty(number) && !contact.phones.isEmpty()) {
                    number = contact.phones.get(0).value;
                }
                render();
            });
        });
    }

    private void bindHeader() {
        boolean isPrivate = contactId <= 0 && PhoneUtils.isPrivate(number);
        String formatted = PhoneUtils.format(this, number);
        if (contact != null) {
            binding.historyName.setText(contact.getDisplayName());
            binding.historyNumber.setText(formatted);
            binding.historyNumber.setVisibility(TextUtils.isEmpty(formatted) ? View.GONE : View.VISIBLE);
            binding.historyAvatar.bind(contact.name, contact.photoUri);
        } else if (isPrivate) {
            binding.historyName.setText(R.string.private_number);
            binding.historyNumber.setVisibility(View.GONE);
            binding.historyAvatar.bind(null, null);
        } else {
            binding.historyName.setText(formatted);
            binding.historyNumber.setVisibility(View.GONE);
            binding.historyAvatar.bind(null, null);
        }
        binding.historyCall.setVisibility(isPrivate || TextUtils.isEmpty(number) ? View.INVISIBLE : View.VISIBLE);
        binding.historyCall.setOnClickListener(v -> PhoneService.call(this, number));
        binding.headerInfo.setOnClickListener(contact == null ? null : v ->
                startActivity(ContactDetailsActivity.intent(this, contact.id, contact.lookupKey)));
        binding.headerInfo.setClickable(contact != null);

        boolean known = contact != null;
        if (isPrivate) {
            binding.actionPrimary.bottomActionRoot.setVisibility(View.GONE);
            binding.actionMessage.bottomActionRoot.setVisibility(View.GONE);
        } else if (!known) {
            bindAction(binding.actionPrimary, R.drawable.ic_person_add, R.string.add_contact, false,
                    v -> startActivity(EditContactActivity.createIntent(this, number)));
        } else if (WhatsAppService.isInstalled(this)) {
            bindAction(binding.actionPrimary, R.drawable.ic_videocam, R.string.video_call, false,
                    v -> WhatsAppService.videoCall(this, number));
        } else {
            bindAction(binding.actionPrimary, R.drawable.ic_info, R.string.contact_info, false,
                    v -> startActivity(ContactDetailsActivity.intent(this, contact.id, contact.lookupKey)));
        }
        if (!isPrivate) {
            bindAction(binding.actionMessage, R.drawable.ic_chat, R.string.messages, false,
                    v -> IntentUtils.openSms(this, number));
        }
        bindAction(binding.actionDelete, R.drawable.ic_delete, R.string.delete, true, v -> confirmDeleteAll());
        binding.actionDelete.bottomActionRoot.setVisibility(CallLogService.canWrite(this) ? View.VISIBLE : View.GONE);
    }

    private void bindAction(IncludeBottomActionBinding action, int icon, int label, boolean destructive,
                            View.OnClickListener listener) {
        action.bottomActionRoot.setVisibility(View.VISIBLE);
        action.bottomActionIcon.setImageResource(icon);
        action.bottomActionLabel.setText(label);
        int color = getColor(destructive ? R.color.error : R.color.text_primary);
        action.bottomActionIcon.setImageTintList(android.content.res.ColorStateList.valueOf(color));
        action.bottomActionLabel.setTextColor(color);
        action.bottomActionRoot.setContentDescription(getString(label));
        action.bottomActionRoot.setOnClickListener(listener);
    }

    private void render() {
        bindHeader();
        binding.progress.setVisibility(View.GONE);
        boolean multipleNumbers = contact != null && contact.phones.size() > 1 && contactId > 0;
        adapter.setShowNumber(multipleNumbers);
        adapter.submitList(CallEntryAdapter.build(this, calls));
        if (calls.isEmpty()) {
            binding.emptyState.setContent(R.drawable.ic_history, R.string.history_empty_title, R.string.history_empty_body);
        }
        binding.emptyState.show(calls.isEmpty());
        binding.actionDelete.bottomActionRoot.setEnabled(!calls.isEmpty());
        binding.actionDelete.bottomActionRoot.setAlpha(calls.isEmpty() ? 0.4f : 1f);
    }

    private void confirmDeleteAll() {
        if (calls.isEmpty()) {
            return;
        }
        List<CallModel> toDelete = new ArrayList<>(calls);
        AppDialogs.confirm(this, getString(R.string.delete_history_title),
                getResources().getQuantityString(R.plurals.delete_calls_body, toDelete.size(), toDelete.size()),
                R.string.delete, true, () -> delete(toDelete, true));
    }

    @Override
    public void onLongClick(CallModel call, View view) {
        if (!CallLogService.canWrite(this)) {
            return;
        }
        List<CallModel> single = new ArrayList<>();
        single.add(call);
        AppDialogs.confirm(this, getString(R.string.delete_entry_title), null, R.string.delete, true,
                () -> delete(single, false));
    }

    private void delete(List<CallModel> toDelete, boolean all) {
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            boolean ok = CallLogService.deleteCalls(app, toDelete);
            AppExecutors.main(() -> {
                if (isDestroyed()) {
                    return;
                }
                if (!ok) {
                    Toast.makeText(app, R.string.delete_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (all) {
                    Toast.makeText(app, R.string.history_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    load();
                }
            });
        });
    }
}
