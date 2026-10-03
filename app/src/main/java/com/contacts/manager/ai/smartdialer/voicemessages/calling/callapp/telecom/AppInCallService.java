package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom;

import android.content.Intent;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.aftercall.AfterCallActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.call.CallActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.NotificationService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;

@SuppressWarnings("deprecation")
public class AppInCallService extends InCallService {

    private Call lastPrimary;
    private int lastPrimaryState = -1;

    private final CallManager.Listener listener = () -> {
        NotificationService.updateCallNotification(this);
        Call primary = CallManager.getPrimaryCall();
        int state = primary == null ? -1 : CallManager.getState(primary);
        if (primary != null && primary == lastPrimary
                && lastPrimaryState == Call.STATE_RINGING && state == Call.STATE_ACTIVE) {
            openCallScreen();
        }
        lastPrimary = primary;
        lastPrimaryState = state;
    };

    @Override
    public void onCreate() {
        super.onCreate();
        CallManager.setService(this);
        CallManager.addListener(listener);
    }

    @Override
    public void onDestroy() {
        CallManager.removeListener(listener);
        if (CallManager.getService() == this) {
            CallManager.setService(null);
        }
        super.onDestroy();
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        CallManager.setService(this);
        CallManager.onCallAdded(this, call);
        openCallScreen();
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        CallManager.EndedCall ended = CallManager.onCallRemoved(call);
        if (!CallManager.hasCalls()) {
            NotificationService.cancelCallNotification(this);
            if (StorageService.isAfterCallEnabled()) {
                openAfterCall(ended);
            }
        }
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState audioState) {
        super.onCallAudioStateChanged(audioState);
        CallManager.notifyChanged();
    }

    private void openCallScreen() {
        Intent intent = CallActivity.intent(this);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private void openAfterCall(CallManager.EndedCall ended) {
        Intent intent = AfterCallActivity.intent(this, ended);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }
}
