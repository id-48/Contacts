package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels;

import android.app.Application;
import android.provider.CallLog;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.AppConstants;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.CallModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.CallLogService;

import java.util.List;

public class RecentViewModel extends ObservingViewModel {

    private final MutableLiveData<List<CallModel>> calls = new MutableLiveData<>();

    public RecentViewModel(@NonNull Application application) {
        super(application);
        observe(CallLog.Calls.CONTENT_URI, ContactsContract.Contacts.CONTENT_URI);
    }

    public LiveData<List<CallModel>> getCalls() {
        return calls;
    }

    @Override
    protected void load() {
        calls.postValue(CallLogService.getRecentCalls(getApplication(), AppConstants.RECENT_CALL_LIMIT));
    }
}
