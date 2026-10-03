package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.AppConstants;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.IntentKeys;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.dialer.DialerActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.NotificationService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ReminderService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(ReminderService.EXTRA_REMINDER_ID, 0);
        ReminderService.Reminder reminder = ReminderService.find(context, id);
        if (reminder == null) {
            return;
        }
        ReminderService.remove(context, id);
        if (!NotificationService.canPost(context)) {
            return;
        }
        boolean dialable = !PhoneUtils.isPrivate(reminder.number);
        String who = !TextUtils.isEmpty(reminder.name) ? reminder.name
                : dialable ? PhoneUtils.format(context, reminder.number) : context.getString(R.string.private_number);
        int notificationId = (int) id;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, AppConstants.CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_notification_call)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setContentTitle(context.getString(R.string.reminder_title, who))
                .setContentText(context.getString(R.string.reminder_body))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        if (dialable) {
            Intent open = DialerActivity.intent(context, reminder.number)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            builder.setContentIntent(PendingIntent.getActivity(context, notificationId, open, flags));
            Intent call = new Intent(context, NotificationActionReceiver.class)
                    .setAction(IntentKeys.ACTION_CALL_BACK)
                    .putExtra(IntentKeys.EXTRA_NUMBER, reminder.number)
                    .putExtra(IntentKeys.EXTRA_NOTIFICATION_ID, notificationId);
            builder.addAction(R.drawable.ic_call, context.getString(R.string.call_back),
                    PendingIntent.getBroadcast(context, notificationId, call, flags));
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
        }
    }
}
