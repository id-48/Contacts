package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels;

import android.app.Application;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.LabeledValue;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;

import java.util.List;

public class EditContactViewModel extends AndroidViewModel {

    private final MutableLiveData<ContactModel> contact = new MutableLiveData<>();
    private final MutableLiveData<List<ContactsService.AccountOption>> accounts = new MutableLiveData<>();
    private final MutableLiveData<Long> saved = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private boolean initialized;
    private String initialSignature;

    public EditContactViewModel(@NonNull Application application) {
        super(application);
    }

    public void init(long contactId, String number) {
        if (initialized) {
            return;
        }
        initialized = true;
        Application app = getApplication();
        AppExecutors.io(() -> {
            ContactModel model = null;
            if (contactId > 0) {
                model = ContactsService.getContactForEdit(app, contactId);
            }
            if (model == null) {
                model = new ContactModel();
                if (!TextUtils.isEmpty(number)) {
                    model.phones.add(new LabeledValue(number, Phone.TYPE_MOBILE, null));
                }
            }
            if (contactId <= 0) {
                accounts.postValue(ContactsService.getAccounts(app));
            }
            contact.postValue(model);
        });
    }

    public LiveData<ContactModel> getContact() {
        return contact;
    }

    public LiveData<List<ContactsService.AccountOption>> getAccounts() {
        return accounts;
    }

    public LiveData<Long> getSaved() {
        return saved;
    }

    public LiveData<Boolean> getSaving() {
        return saving;
    }

    public String getInitialSignature() {
        return initialSignature;
    }

    public void setInitialSignature(String signature) {
        if (initialSignature == null) {
            initialSignature = signature;
        }
    }

    public void save(ContactModel model) {
        if (Boolean.TRUE.equals(saving.getValue())) {
            return;
        }
        saving.setValue(true);
        Application app = getApplication();
        AppExecutors.io(() -> {
            long id = ContactsService.saveContact(app, model);
            AppExecutors.main(() -> {
                saving.setValue(false);
                saved.setValue(id);
            });
        });
    }
}
