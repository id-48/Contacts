package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom.ReminderReceiver;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ReminderService {

    public static final String EXTRA_REMINDER_ID = "extra_reminder_id";

    private static final String PREFS = "call_reminders";
    private static final String KEY_ITEMS = "items";

    public static class Reminder {
        public long id;
        public String number;
        public String name;
        public long time;
    }

    private ReminderService() {
    }

    public static synchronized List<Reminder> getAll(Context context) {
        List<Reminder> reminders = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs(context).getString(KEY_ITEMS, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                Reminder reminder = new Reminder();
                reminder.id = item.getLong("id");
                reminder.number = item.optString("number", "");
                reminder.name = item.optString("name", "");
                reminder.time = item.getLong("time");
                reminders.add(reminder);
            }
        } catch (JSONException ignored) {
        }
        Collections.sort(reminders, (a, b) -> Long.compare(a.time, b.time));
        return reminders;
    }

    public static List<Reminder> getUpcomingForNumber(Context context, String number) {
        String key = PhoneUtils.key(number == null ? "" : number);
        long now = System.currentTimeMillis();
        List<Reminder> result = new ArrayList<>();
        for (Reminder reminder : getAll(context)) {
            if (reminder.time > now && PhoneUtils.key(reminder.number).equals(key)) {
                result.add(reminder);
            }
        }
        return result;
    }

    public static synchronized Reminder find(Context context, long id) {
        for (Reminder reminder : getAll(context)) {
            if (reminder.id == id) {
                return reminder;
            }
        }
        return null;
    }

    public static synchronized Reminder add(Context context, String number, String name, long time) {
        Reminder reminder = new Reminder();
        reminder.id = System.currentTimeMillis();
        reminder.number = number == null ? "" : number;
        reminder.name = name == null ? "" : name;
        reminder.time = time;
        List<Reminder> reminders = getAll(context);
        reminders.add(reminder);
        save(context, reminders);
        schedule(context, reminder);
        return reminder;
    }

    public static synchronized void remove(Context context, long id) {
        List<Reminder> reminders = getAll(context);
        List<Reminder> kept = new ArrayList<>();
        for (Reminder reminder : reminders) {
            if (reminder.id != id) {
                kept.add(reminder);
            }
        }
        save(context, kept);
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        if (alarms != null) {
            alarms.cancel(alarmIntent(context, id));
        }
    }

    private static void schedule(Context context, Reminder reminder) {
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        if (alarms == null) {
            return;
        }
        PendingIntent intent = alarmIntent(context, reminder.id);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.time, intent);
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.time, intent);
        }
    }

    private static PendingIntent alarmIntent(Context context, long id) {
        Intent intent = new Intent(context, ReminderReceiver.class).putExtra(EXTRA_REMINDER_ID, id);
        return PendingIntent.getBroadcast(context, (int) id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void save(Context context, List<Reminder> reminders) {
        JSONArray array = new JSONArray();
        for (Reminder reminder : reminders) {
            try {
                array.put(new JSONObject()
                        .put("id", reminder.id)
                        .put("number", reminder.number)
                        .put("name", reminder.name)
                        .put("time", reminder.time));
            } catch (JSONException ignored) {
            }
        }
        prefs(context).edit().putString(KEY_ITEMS, array.toString()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
