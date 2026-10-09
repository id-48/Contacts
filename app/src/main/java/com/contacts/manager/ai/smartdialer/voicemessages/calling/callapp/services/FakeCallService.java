package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom.FakeCallReceiver;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FakeCallService {

    public static final String EXTRA_FAKE_CALL_ID = "extra_fake_call_id";
    public static final int STATUS_UPCOMING = 0;
    public static final int STATUS_ANSWERED = 1;
    public static final int STATUS_DECLINED = 2;
    public static final int STATUS_MISSED = 3;
    public static final long RING_TIMEOUT_MS = 45_000L;

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    public static class FakeCall {
        public long id;
        public String name;
        public String number;
        public long time;
        public int duration;
        public int status;
        public long completedAt;

        public boolean isUpcoming() {
            return status == STATUS_UPCOMING;
        }
    }

    private FakeCallService() {
    }

    public static synchronized List<FakeCall> getAll() {
        List<FakeCall> items = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(StorageService.getFakeCallsJson());
            long now = System.currentTimeMillis();
            boolean changed = false;
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                FakeCall call = new FakeCall();
                call.id = object.optLong("id");
                call.name = object.optString("name");
                call.number = object.optString("number");
                call.time = object.optLong("time");
                call.duration = object.optInt("duration", 30);
                call.status = object.optInt("status");
                call.completedAt = object.optLong("completedAt");
                if (call.isUpcoming() && call.time + RING_TIMEOUT_MS + 15_000L < now) {
                    call.status = STATUS_MISSED;
                    call.completedAt = call.time;
                    changed = true;
                }
                items.add(call);
            }
            if (changed) {
                save(items);
            }
        } catch (Exception ignored) {
        }
        return items;
    }

    public static List<FakeCall> getUpcoming() {
        List<FakeCall> result = new ArrayList<>();
        for (FakeCall call : getAll()) {
            if (call.isUpcoming()) {
                result.add(call);
            }
        }
        Collections.sort(result, (a, b) -> Long.compare(a.time, b.time));
        return result;
    }

    public static List<FakeCall> getCompleted() {
        List<FakeCall> result = new ArrayList<>();
        for (FakeCall call : getAll()) {
            if (!call.isUpcoming()) {
                result.add(call);
            }
        }
        Collections.sort(result, (a, b) -> Long.compare(b.completedAt, a.completedAt));
        return result;
    }

    public static synchronized FakeCall find(long id) {
        for (FakeCall call : getAll()) {
            if (call.id == id) {
                return call;
            }
        }
        return null;
    }

    public static synchronized FakeCall schedule(Context context, String name, String number, long delayMs, int durationSec) {
        List<FakeCall> items = getAll();
        FakeCall call = new FakeCall();
        call.id = System.currentTimeMillis();
        call.name = name;
        call.number = number == null ? "" : number;
        call.time = System.currentTimeMillis() + delayMs;
        call.duration = durationSec;
        call.status = STATUS_UPCOMING;
        items.add(call);
        save(items);
        arm(context.getApplicationContext(), call);
        return call;
    }

    public static synchronized void cancel(Context context, long id) {
        List<FakeCall> items = getAll();
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).id == id) {
                items.remove(i);
            }
        }
        save(items);
        disarm(context.getApplicationContext(), id);
    }

    public static synchronized void complete(long id, int status) {
        List<FakeCall> items = getAll();
        for (FakeCall call : items) {
            if (call.id == id && call.isUpcoming()) {
                call.status = status;
                call.completedAt = System.currentTimeMillis();
            }
        }
        save(items);
    }

    public static synchronized void remove(long id) {
        List<FakeCall> items = getAll();
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).id == id) {
                items.remove(i);
            }
        }
        save(items);
    }

    public static synchronized void clearCompleted() {
        List<FakeCall> items = getAll();
        for (int i = items.size() - 1; i >= 0; i--) {
            if (!items.get(i).isUpcoming()) {
                items.remove(i);
            }
        }
        save(items);
    }

    private static void save(List<FakeCall> items) {
        JSONArray array = new JSONArray();
        try {
            for (FakeCall call : items) {
                JSONObject object = new JSONObject();
                object.put("id", call.id);
                object.put("name", call.name);
                object.put("number", call.number);
                object.put("time", call.time);
                object.put("duration", call.duration);
                object.put("status", call.status);
                object.put("completedAt", call.completedAt);
                array.put(object);
            }
        } catch (Exception ignored) {
        }
        StorageService.setFakeCallsJson(array.toString());
    }

    private static void arm(Context context, FakeCall call) {
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        if (alarms != null) {
            PendingIntent intent = alarmIntent(context, call.id);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, call.time, intent);
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, call.time, intent);
            }
        }
        long id = call.id;
        HANDLER.postAtTime(() -> FakeCallReceiver.trigger(context, id), token(id),
                android.os.SystemClock.uptimeMillis() + Math.max(0, call.time - System.currentTimeMillis()));
    }

    private static void disarm(Context context, long id) {
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        if (alarms != null) {
            alarms.cancel(alarmIntent(context, id));
        }
        HANDLER.removeCallbacksAndMessages(token(id));
    }

    private static String token(long id) {
        return ("fake_call_" + id).intern();
    }

    private static PendingIntent alarmIntent(Context context, long id) {
        Intent intent = new Intent(context, FakeCallReceiver.class).putExtra(EXTRA_FAKE_CALL_ID, id);
        return PendingIntent.getBroadcast(context, (int) (id % Integer.MAX_VALUE), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
