package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.call.FakeIncomingCallActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.FakeCallService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.util.HashSet;
import java.util.Set;

public class FakeCallReceiver extends BroadcastReceiver {

    public static final String ACTION_DECLINE = "com.contacts.fakecall.DECLINE";
    public static final String CHANNEL_FAKE_CALLS = "fake_calls";
    public static final int NOTIFICATION_ID = 4711;

    private static final Set<Long> TRIGGERED = new HashSet<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(FakeCallService.EXTRA_FAKE_CALL_ID, -1);
        if (id < 0) {
            return;
        }
        if (ACTION_DECLINE.equals(intent.getAction())) {
            FakeCallService.complete(id, FakeCallService.STATUS_DECLINED);
            cancelNotification(context);
            return;
        }
        trigger(context, id);
    }

    public static void trigger(Context context, long id) {
        synchronized (TRIGGERED) {
            if (!TRIGGERED.add(id)) {
                return;
            }
        }
        FakeCallService.FakeCall call = FakeCallService.find(id);
        if (call == null || !call.isUpcoming()) {
            return;
        }
        Context app = context.getApplicationContext();
        Intent screen = FakeIncomingCallActivity.intent(app, id, false).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        boolean foreground = ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
        if (foreground) {
            try {
                app.startActivity(screen);
                return;
            } catch (Exception ignored) {
            }
        }
        postNotification(app, call, screen);
    }

    public static void cancelNotification(Context context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_FAKE_CALLS) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_FAKE_CALLS,
                context.getString(R.string.fake_call_channel), NotificationManager.IMPORTANCE_HIGH);
        channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 800, 800});
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
    }

    private static void postNotification(Context context, FakeCallService.FakeCall call, Intent screen) {
        ensureChannel(context);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent full = PendingIntent.getActivity(context, 0, screen, flags);
        PendingIntent answer = PendingIntent.getActivity(context, 1,
                FakeIncomingCallActivity.intent(context, call.id, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags);
        PendingIntent decline = PendingIntent.getBroadcast(context, 2,
                new Intent(context, FakeCallReceiver.class).setAction(ACTION_DECLINE)
                        .putExtra(FakeCallService.EXTRA_FAKE_CALL_ID, call.id), flags);
        String title = TextUtils.isEmpty(call.name) ? PhoneUtils.format(context, call.number) : call.name;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_FAKE_CALLS)
                .setSmallIcon(R.drawable.ic_call)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.incoming_call))
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(true)
                .setTimeoutAfter(FakeCallService.RING_TIMEOUT_MS)
                .setContentIntent(full)
                .setFullScreenIntent(full, true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                .addAction(R.drawable.ic_call_end_filled, context.getString(R.string.decline), decline)
                .addAction(R.drawable.ic_call_filled, context.getString(R.string.answer), answer);
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_INSISTENT;
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification);
        } catch (SecurityException ignored) {
        }
    }
}
