package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.telecom.Call;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.AppConstants;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.IntentKeys;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.CallModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.call.CallActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.main.MainActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom.CallManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom.NotificationActionReceiver;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AvatarBitmaps;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.IntentUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NotificationService {

    private static final int FLAGS = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

    private NotificationService() {
    }

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel incoming = new NotificationChannel(AppConstants.CHANNEL_INCOMING,
                context.getString(R.string.channel_incoming), NotificationManager.IMPORTANCE_HIGH);
        incoming.setSound(null, null);
        incoming.enableVibration(false);
        incoming.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationChannel ongoing = new NotificationChannel(AppConstants.CHANNEL_ONGOING,
                context.getString(R.string.channel_ongoing), NotificationManager.IMPORTANCE_DEFAULT);
        ongoing.setSound(null, null);
        ongoing.enableVibration(false);

        NotificationChannel missed = new NotificationChannel(AppConstants.CHANNEL_MISSED,
                context.getString(R.string.channel_missed), NotificationManager.IMPORTANCE_DEFAULT);
        missed.setShowBadge(true);

        NotificationChannel reminders = new NotificationChannel(AppConstants.CHANNEL_REMINDERS,
                context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_HIGH);

        manager.createNotificationChannel(incoming);
        manager.createNotificationChannel(ongoing);
        manager.createNotificationChannel(missed);
        manager.createNotificationChannel(reminders);
    }

    public static boolean canPost(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static void updateCallNotification(Context context) {
        Call call = CallManager.getPrimaryCall();
        if (call == null) {
            cancelCallNotification(context);
            return;
        }
        if (!canPost(context)) {
            return;
        }
        int state = CallManager.getState(call);
        ContactModel contact = CallManager.getContact(call);
        String title = CallManager.getDisplayName(context, call);
        Bitmap avatar = AvatarBitmaps.create(context, contact == null ? null : contact.name,
                contact == null ? null : contact.photoUri, 128);
        Person person = new Person.Builder()
                .setName(title)
                .setIcon(IconCompat.createWithBitmap(avatar))
                .setImportant(true)
                .build();
        PendingIntent content = PendingIntent.getActivity(context, 10, CallActivity.intent(context), FLAGS);

        NotificationCompat.Builder builder;
        if (state == Call.STATE_RINGING) {
            Intent answerIntent = CallActivity.intent(context).setAction(IntentKeys.ACTION_ANSWER);
            PendingIntent answer = PendingIntent.getActivity(context, 11, answerIntent, FLAGS);
            PendingIntent decline = PendingIntent.getBroadcast(context, 12,
                    new Intent(context, NotificationActionReceiver.class).setAction(IntentKeys.ACTION_DECLINE), FLAGS);
            builder = new NotificationCompat.Builder(context, AppConstants.CHANNEL_INCOMING)
                    .setContentText(context.getString(R.string.incoming_call))
                    .setStyle(NotificationCompat.CallStyle.forIncomingCall(person, decline, answer))
                    .setFullScreenIntent(content, true)
                    .setPriority(NotificationCompat.PRIORITY_MAX);
        } else {
            PendingIntent hangup = PendingIntent.getBroadcast(context, 13,
                    new Intent(context, NotificationActionReceiver.class).setAction(IntentKeys.ACTION_HANGUP), FLAGS);
            long connectTime = CallManager.getConnectTime(call);
            builder = new NotificationCompat.Builder(context, AppConstants.CHANNEL_ONGOING)
                    .setContentText(context.getString(state == Call.STATE_HOLDING ? R.string.on_hold
                            : state == Call.STATE_ACTIVE ? R.string.notification_ongoing : R.string.calling))
                    .setStyle(NotificationCompat.CallStyle.forOngoingCall(person, hangup))
                    .setFullScreenIntent(content, false)
                    .setOnlyAlertOnce(true)
                    .setUsesChronometer(connectTime > 0 && state == Call.STATE_ACTIVE)
                    .setWhen(connectTime > 0 ? connectTime : System.currentTimeMillis());
        }
        builder.setSmallIcon(R.drawable.ic_notification_call)
                .setContentTitle(title)
                .setContentIntent(content)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(ContextCompat.getColor(context, R.color.brand_blue));
        try {
            NotificationManagerCompat.from(context).notify(AppConstants.NOTIFICATION_CALL_ID, builder.build());
        } catch (SecurityException | IllegalArgumentException e) {
            postPlainCallNotification(context, title, state, content);
        }
    }

    private static void postPlainCallNotification(Context context, String title, int state, PendingIntent content) {
        boolean ringing = state == Call.STATE_RINGING;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                ringing ? AppConstants.CHANNEL_INCOMING : AppConstants.CHANNEL_ONGOING)
                .setSmallIcon(R.drawable.ic_notification_call)
                .setContentTitle(title)
                .setContentText(context.getString(ringing ? R.string.incoming_call : R.string.notification_ongoing))
                .setContentIntent(content)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_CALL);
        try {
            NotificationManagerCompat.from(context).notify(AppConstants.NOTIFICATION_CALL_ID, builder.build());
        } catch (SecurityException | IllegalArgumentException ignored) {
        }
    }

    public static void cancelCallNotification(Context context) {
        NotificationManagerCompat.from(context).cancel(AppConstants.NOTIFICATION_CALL_ID);
    }

    public static void showMissedCalls(Context context) {
        if (!canPost(context)) {
            return;
        }
        List<CallModel> missed = CallLogService.getNewMissedCalls(context);
        cancelMissedCallNotifications(context);
        if (missed.isEmpty()) {
            return;
        }
        Map<String, List<CallModel>> byNumber = new LinkedHashMap<>();
        for (CallModel call : missed) {
            String key = PhoneUtils.isPrivate(call.number) ? "private" : PhoneUtils.key(call.number);
            List<CallModel> list = byNumber.get(key);
            if (list == null) {
                list = new ArrayList<>();
                byNumber.put(key, list);
            }
            list.add(call);
        }
        Intent openMissed = new Intent(context, MainActivity.class)
                .putExtra(IntentKeys.EXTRA_TAB, MainActivity.TAB_RECENT)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 20, openMissed, FLAGS);
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        NotificationCompat.InboxStyle inbox = new NotificationCompat.InboxStyle();

        for (Map.Entry<String, List<CallModel>> entry : byNumber.entrySet()) {
            List<CallModel> calls = entry.getValue();
            CallModel latest = calls.get(0);
            String title = !TextUtils.isEmpty(latest.name) ? latest.name
                    : PhoneUtils.isPrivate(latest.number) ? context.getString(R.string.private_number)
                    : PhoneUtils.format(context, latest.number);
            String text = calls.size() > 1
                    ? context.getResources().getQuantityString(R.plurals.missed_calls_count, calls.size(), calls.size())
                    : context.getString(R.string.call_missed);
            int id = 3000 + Math.abs(entry.getKey().hashCode() % 100000);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, AppConstants.CHANNEL_MISSED)
                    .setSmallIcon(R.drawable.ic_notification_missed)
                    .setLargeIcon(AvatarBitmaps.create(context, latest.name, latest.photoUri, 128))
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(latest.date)
                    .setShowWhen(true)
                    .setAutoCancel(true)
                    .setGroup(AppConstants.NOTIFICATION_MISSED_GROUP)
                    .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
                    .setColor(ContextCompat.getColor(context, R.color.brand_blue))
                    .setContentIntent(contentIntent);
            if (!PhoneUtils.isPrivate(latest.number)) {
                Intent callBack = new Intent(context, NotificationActionReceiver.class)
                        .setAction(IntentKeys.ACTION_CALL_BACK)
                        .putExtra(IntentKeys.EXTRA_NUMBER, latest.number)
                        .putExtra(IntentKeys.EXTRA_NOTIFICATION_ID, id);
                PendingIntent callBackIntent = PendingIntent.getBroadcast(context, id, callBack, FLAGS);
                Intent sms = IntentUtils.smsIntent(latest.number, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent smsIntent = PendingIntent.getActivity(context, id + 1, sms, FLAGS);
                builder.addAction(R.drawable.ic_call, context.getString(R.string.call_back), callBackIntent);
                builder.addAction(R.drawable.ic_chat, context.getString(R.string.message), smsIntent);
            }
            inbox.addLine(title + " · " + text);
            try {
                manager.notify(id, builder.build());
            } catch (SecurityException ignored) {
            }
        }
        if (byNumber.size() > 1) {
            NotificationCompat.Builder summary = new NotificationCompat.Builder(context, AppConstants.CHANNEL_MISSED)
                    .setSmallIcon(R.drawable.ic_notification_missed)
                    .setContentTitle(context.getResources().getQuantityString(R.plurals.missed_calls_count,
                            missed.size(), missed.size()))
                    .setStyle(inbox)
                    .setGroup(AppConstants.NOTIFICATION_MISSED_GROUP)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setColor(ContextCompat.getColor(context, R.color.brand_blue))
                    .setContentIntent(contentIntent);
            try {
                manager.notify(AppConstants.NOTIFICATION_MISSED_SUMMARY_ID, summary.build());
            } catch (SecurityException ignored) {
            }
        }
    }

    public static void cancelMissedCallNotifications(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        try {
            for (StatusBarNotification notification : manager.getActiveNotifications()) {
                String group = notification.getNotification().getGroup();
                if (AppConstants.NOTIFICATION_MISSED_GROUP.equals(group)
                        || notification.getId() == AppConstants.NOTIFICATION_MISSED_SUMMARY_ID) {
                    manager.cancel(notification.getId());
                }
            }
        } catch (Exception ignored) {
        }
    }
}
