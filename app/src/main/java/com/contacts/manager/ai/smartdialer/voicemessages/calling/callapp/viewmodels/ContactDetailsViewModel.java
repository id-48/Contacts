package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels;

import android.app.Application;
import android.provider.CallLog;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.AppConstants;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.CallModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.LabeledValue;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.BlockService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.CallLogService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;

import java.util.Collections;
import java.util.List;

public class ContactDetailsViewModel extends ObservingViewModel {

    public static class State {
        public final ContactModel contact;
        public final List<CallModel> recentCalls;
        public final boolean blocked;
        public final boolean canBlock;

        State(ContactModel contact, List<CallModel> recentCalls, boolean blocked, boolean canBlock) {
            this.contact = contact;
            this.recentCalls = recentCalls;
            this.blocked = blocked;
            this.canBlock = canBlock;
        }
    }

    private final MutableLiveData<State> state = new MutableLiveData<>();
    private long contactId;
    private String lookupKey;

    public ContactDetailsViewModel(@NonNull Application application) {
        super(application);
        observe(ContactsContract.Contacts.CONTENT_URI, CallLog.Calls.CONTENT_URI);
    }

    public void init(long contactId, String lookupKey) {
        if (this.contactId == 0) {
            this.contactId = contactId;
            this.lookupKey = lookupKey;
        }
    }

    public LiveData<State> getState() {
        return state;
    }

    @Override
    protected void load() {
        Application app = getApplication();
        long resolved = ContactsService.resolveContactId(app, contactId, lookupKey);
        ContactModel contact = ContactsService.getContactDetails(app, resolved);
        if (contact == null) {
            state.postValue(new State(null, Collections.emptyList(), false, false));
            return;
        }
        contactId = contact.id;
        lookupKey = contact.lookupKey;
        List<CallModel> calls = CallLogService.getCallsForContact(app, contact, AppConstants.RECENT_ACTIVITY_LIMIT);
        boolean canBlock = BlockService.canBlock(app);
        boolean blocked = false;
        if (canBlock) {
            for (LabeledValue phone : contact.phones) {
                if (BlockService.isBlocked(app, phone.value)) {
                    blocked = true;
                    break;
                }
            }
        }
        state.postValue(new State(contact, calls, blocked, canBlock));
    }
}
