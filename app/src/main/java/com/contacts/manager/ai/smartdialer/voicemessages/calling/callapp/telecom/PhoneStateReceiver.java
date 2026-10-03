package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.CallLog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.aftercall.AfterCallActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;

/**
 * Shows the after-call screen when another app is the default dialer; as the default dialer,
 * {@link AppInCallService} handles it instead.
 */
public class PhoneStateReceiver extends BroadcastReceiver {

    private static final String PREFS = "phone_state";
    private static final String KEY_STATE = "state";
    private static final String KEY_RANG = "rang";
    private static final String KEY_START = "start";
    private static final String KEY_OFFHOOK = "offhook";
    private static final String KEY_NUMBER = "number";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            return;
        }
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (state == null) {
            return;
        }
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        SharedPreferences.Editor editor = prefs.edit();
        if (!TextUtils.isEmpty(number)) {
            editor.putString(KEY_NUMBER, number);
        }
        String previous = prefs.getString(KEY_STATE, TelephonyManager.EXTRA_STATE_IDLE);
        long now = System.currentTimeMillis();
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            if (TelephonyManager.EXTRA_STATE_IDLE.equals(previous)) {
                editor.putLong(KEY_START, now);
            }
            editor.putBoolean(KEY_RANG, true);
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            if (TelephonyManager.EXTRA_STATE_IDLE.equals(previous)) {
                editor.putLong(KEY_START, now);
            }
            if (prefs.getLong(KEY_OFFHOOK, 0) == 0) {
                editor.putLong(KEY_OFFHOOK, now);
            }
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)
                && !TelephonyManager.EXTRA_STATE_IDLE.equals(previous)) {
            boolean rang = prefs.getBoolean(KEY_RANG, false);
            long offhook = prefs.getLong(KEY_OFFHOOK, 0);
            long start = prefs.getLong(KEY_START, now);
            String stored = TextUtils.isEmpty(number) ? prefs.getString(KEY_NUMBER, null) : number;
            editor.clear().putString(KEY_STATE, state).apply();
            if (StorageService.isAfterCallEnabled() && !PhoneService.isDefaultDialer(context)) {
                int type = !rang ? CallLog.Calls.OUTGOING_TYPE
                        : offhook > 0 ? CallLog.Calls.INCOMING_TYPE : CallLog.Calls.MISSED_TYPE;
                long duration = offhook > 0 ? Math.max(0, (now - offhook) / 1000) : 0;
                Intent screen = AfterCallActivity.intent(context, stored, type, duration, start)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(screen);
                } catch (RuntimeException ignored) {
                }
            }
            return;
        }
        editor.putString(KEY_STATE, state).apply();
    }
}
