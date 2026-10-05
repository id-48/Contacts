package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.AppConstants;

import java.util.concurrent.TimeUnit;

/** Shows one of the built-in messages every 3 to 4 hours (a 4 hour period that may run in its last hour). */
public class LocalNotificationWorker extends Worker {

    private static final String WORK_NAME = "local_notification";
    private static final long PERIOD_HOURS = 4;
    private static final long FLEX_HOURS = 1;

    private static final int[][] MESSAGES = {
            {R.string.local_notification_recent_title, R.string.local_notification_recent_body},
            {R.string.local_notification_favorites_title, R.string.local_notification_favorites_body},
            {R.string.local_notification_block_title, R.string.local_notification_block_body},
            {R.string.local_notification_quick_reply_title, R.string.local_notification_quick_reply_body},
            {R.string.local_notification_theme_title, R.string.local_notification_theme_body},
            {R.string.local_notification_history_title, R.string.local_notification_history_body},
    };

    public LocalNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void schedule(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(LocalNotificationWorker.class,
                PERIOD_HOURS, TimeUnit.HOURS, FLEX_HOURS, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!StorageService.isOnboardingDone()) return Result.success();
        Context context = getApplicationContext();
        int[] message = MESSAGES[StorageService.nextLocalNotificationIndex(MESSAGES.length)];
        AppNotifications.show(context, AppConstants.NOTIFICATION_LOCAL_ID,
                context.getString(message[0]), context.getString(message[1]), null, null);
        return Result.success();
    }
}
