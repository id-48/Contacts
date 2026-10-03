package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telecom.TelecomManager;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.NotificationService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;

public class MissedCallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!TelecomManager.ACTION_SHOW_MISSED_CALLS_NOTIFICATION.equals(intent.getAction())) {
            return;
        }
        int count = intent.getIntExtra(TelecomManager.EXTRA_NOTIFICATION_COUNT, 0);
        Context app = context.getApplicationContext();
        if (count <= 0) {
            NotificationService.cancelMissedCallNotifications(app);
            return;
        }
        if (!StorageService.isSmartNotificationsEnabled()) {
            return;
        }
        PendingResult pending = goAsync();
        AppExecutors.io(() -> {
            try {
                NotificationService.showMissedCalls(app);
            } finally {
                pending.finish();
            }
        });
    }
}
