package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils;

import android.content.Context;
import android.text.format.DateFormat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class DateUtils {

    private static final long RELATIVE_WINDOW_MS = 6 * 60 * 60 * 1000L;

    private DateUtils() {
    }

    public static boolean isSameDay(long first, long second) {
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(first);
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(second);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean isToday(long time) {
        return isSameDay(time, System.currentTimeMillis());
    }

    public static boolean isYesterday(long time) {
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        return isSameDay(time, yesterday.getTimeInMillis());
    }

    public static String sectionTitle(Context context, long time) {
        if (isToday(time)) {
            return context.getString(R.string.today);
        }
        if (isYesterday(time)) {
            return context.getString(R.string.yesterday);
        }
        return new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(new Date(time));
    }

    public static String rowTime(Context context, long time) {
        long now = System.currentTimeMillis();
        long diff = now - time;
        if (diff >= 0 && diff < 60_000L) {
            return context.getString(R.string.just_now);
        }
        if (diff >= 0 && diff < RELATIVE_WINDOW_MS && isToday(time)) {
            return android.text.format.DateUtils.getRelativeTimeSpanString(time, now,
                    android.text.format.DateUtils.MINUTE_IN_MILLIS).toString();
        }
        return clockTime(context, time);
    }

    public static String agoLabel(Context context, long time) {
        long now = System.currentTimeMillis();
        long diff = now - time;
        if (diff >= 0 && diff < 60_000L) {
            return context.getString(R.string.just_now);
        }
        if (diff >= 0 && diff < android.text.format.DateUtils.DAY_IN_MILLIS) {
            return android.text.format.DateUtils.getRelativeTimeSpanString(time, now,
                    android.text.format.DateUtils.MINUTE_IN_MILLIS).toString();
        }
        if (isYesterday(time)) {
            return context.getString(R.string.yesterday);
        }
        return new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date(time));
    }

    public static String clockTime(Context context, long time) {
        return DateFormat.getTimeFormat(context).format(new Date(time));
    }

    public static String fullDate(long time) {
        return new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(new Date(time));
    }

    public static String duration(Context context, long seconds) {
        if (seconds < 60) {
            return context.getString(R.string.duration_seconds, seconds);
        }
        if (seconds < 3600) {
            return context.getString(R.string.duration_minutes, seconds / 60, seconds % 60);
        }
        return context.getString(R.string.duration_hours, seconds / 3600, (seconds % 3600) / 60);
    }

    public static String timer(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);
    }

    public static String birthdayLabel(String raw) {
        if (raw == null) {
            return "";
        }
        try {
            if (raw.startsWith("--")) {
                Date date = new SimpleDateFormat("--MM-dd", Locale.US).parse(raw);
                return date == null ? raw : new SimpleDateFormat("d MMMM", Locale.getDefault()).format(date);
            }
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw);
            return date == null ? raw : new SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return raw;
        }
    }
}
