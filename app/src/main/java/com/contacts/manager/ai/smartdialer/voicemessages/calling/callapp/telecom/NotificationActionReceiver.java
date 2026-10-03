package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.IntentKeys;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.dialer.DialerActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.IntentUtils;

public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        switch (action) {
            case IntentKeys.ACTION_DECLINE:
                CallManager.reject(CallManager.getPrimaryCall());
                break;
            case IntentKeys.ACTION_HANGUP:
                CallManager.hangup(CallManager.getPrimaryCall());
                break;
            case IntentKeys.ACTION_CALL_BACK:
                int notificationId = intent.getIntExtra(IntentKeys.EXTRA_NOTIFICATION_ID, 0);
                if (notificationId != 0) {
                    NotificationManagerCompat.from(context).cancel(notificationId);
                }
                String number = intent.getStringExtra(IntentKeys.EXTRA_NUMBER);
                if (number == null) {
                    break;
                }
                if (PhoneService.canPlaceCalls(context)) {
                    PhoneService.placeCall(context, number, null, false);
                } else {
                    IntentUtils.safeStart(context, DialerActivity.intent(context, number)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
                break;
            default:
                break;
        }
    }
}
