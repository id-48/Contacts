package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppExecutors {

    private static final ExecutorService IO = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AppExecutors() {
    }

    public static void io(Runnable runnable) {
        IO.execute(runnable);
    }

    public static void main(Runnable runnable) {
        MAIN.post(runnable);
    }

    public static Handler mainHandler() {
        return MAIN;
    }
}
