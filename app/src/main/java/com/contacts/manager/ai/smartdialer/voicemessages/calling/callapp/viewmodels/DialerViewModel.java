package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;

public class DialerViewModel extends SearchViewModel {

    public DialerViewModel(@NonNull Application application) {
        super(application);
    }

    @Override
    protected boolean isDialpad() {
        return true;
    }
}
