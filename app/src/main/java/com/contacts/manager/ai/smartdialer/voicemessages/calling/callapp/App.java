package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.NotificationService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        StorageService.init(this);
        AppCompatDelegate.setDefaultNightMode(StorageService.getThemeMode());
        NotificationService.createChannels(this);
    }
}
