package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ConfigRefreshWorker extends Worker {

    private static final String WORK_NAME = "config_refresh";
    private static final long FETCH_TIMEOUT_SECONDS = 30;

    public ConfigRefreshWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void schedule(Context context, int timesPerDay) {
        long periodMinutes = TimeUnit.DAYS.toMinutes(1) / timesPerDay;
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ConfigRefreshWorker.class,
                periodMinutes, TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();
        ExistingPeriodicWorkPolicy policy = StorageService.getConfigRefreshScheduled() == timesPerDay
                ? ExistingPeriodicWorkPolicy.KEEP : ExistingPeriodicWorkPolicy.UPDATE;
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, policy, request);
        StorageService.setConfigRefreshScheduled(timesPerDay);
    }

    @NonNull
    @Override
    public Result doWork() {
        CountDownLatch done = new CountDownLatch(1);
        new Handler(Looper.getMainLooper()).post(() -> RemoteConfigManager.refreshIfDue(config -> done.countDown()));
        try {
            done.await(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Result.success();
    }
}
