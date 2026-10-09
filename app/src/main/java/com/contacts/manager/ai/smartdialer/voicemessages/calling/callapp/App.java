package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.analytics.Analytics;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.FacebookInitializer;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.RemoteConfigManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.UserSourceManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.launcher.LauncherMode;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.AppLockManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.LocalNotificationWorker;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.NotificationService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PushTokenManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        StorageService.init(this);
        Analytics.init(this);
        AppCompatDelegate.setDefaultNightMode(StorageService.getThemeMode());
        NotificationService.createChannels(this);
        AppLockManager.register(this);

        FacebookInitializer.initAudienceNetwork(this);
        RemoteConfigManager.init(config -> {
            FacebookInitializer.init(this, config);
            LauncherMode.sync(this);
        });
        FacebookInitializer.init(this, RemoteConfigManager.get());
        LauncherMode.sync(this);
        LocalNotificationWorker.schedule(this);
        PushTokenManager.sync();
        AdsManager.initialize(this);
        UserSourceManager.detectOnce(this);
    }
}
