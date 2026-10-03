package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.DisconnectCause;
import android.telecom.InCallService;
import android.telecom.VideoProfile;
import android.text.TextUtils;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("deprecation")
public final class CallManager {

    public interface Listener {
        void onCallsChanged();
    }

    public static class EndedCall {
        public String number;
        public ContactModel contact;
        public int callLogType;
        public long durationSeconds;
    }

    private static final List<Call> CALLS = new ArrayList<>();
    private static final Map<Call, ContactModel> CONTACTS = new HashMap<>();
    private static final Set<Call> INCOMING = new HashSet<>();
    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static InCallService service;

    private static final Call.Callback CALLBACK = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            notifyChanged();
        }

        @Override
        public void onDetailsChanged(Call call, Call.Details details) {
            notifyChanged();
        }

        @Override
        public void onConferenceableCallsChanged(Call call, List<Call> conferenceableCalls) {
            notifyChanged();
        }
    };

    private CallManager() {
    }

    static void setService(InCallService inCallService) {
        service = inCallService;
    }

    static InCallService getService() {
        return service;
    }

    static void onCallAdded(Context context, Call call) {
        CALLS.add(call);
        if (getState(call) == Call.STATE_RINGING || isIncomingDirection(call)) {
            INCOMING.add(call);
        }
        call.registerCallback(CALLBACK);
        Context app = context.getApplicationContext();
        String number = getNumber(call);
        AppExecutors.io(() -> {
            ContactModel contact = ContactsService.lookupNumber(app, number);
            AppExecutors.main(() -> {
                if (CALLS.contains(call) && contact != null) {
                    CONTACTS.put(call, contact);
                    notifyChanged();
                }
            });
        });
        notifyChanged();
    }

    static EndedCall onCallRemoved(Call call) {
        call.unregisterCallback(CALLBACK);
        EndedCall ended = new EndedCall();
        ended.number = getNumber(call);
        ended.contact = CONTACTS.get(call);
        long connectTime = getConnectTime(call);
        ended.durationSeconds = connectTime > 0 ? Math.max(0, (System.currentTimeMillis() - connectTime) / 1000) : 0;
        boolean incoming = INCOMING.contains(call) || isIncomingDirection(call);
        DisconnectCause cause = call.getDetails().getDisconnectCause();
        int code = cause == null ? DisconnectCause.UNKNOWN : cause.getCode();
        if (!incoming) {
            ended.callLogType = CallLog.Calls.OUTGOING_TYPE;
        } else if (connectTime > 0) {
            ended.callLogType = CallLog.Calls.INCOMING_TYPE;
        } else if (code == DisconnectCause.REJECTED || code == DisconnectCause.LOCAL) {
            ended.callLogType = CallLog.Calls.REJECTED_TYPE;
        } else {
            ended.callLogType = CallLog.Calls.MISSED_TYPE;
        }
        CALLS.remove(call);
        CONTACTS.remove(call);
        INCOMING.remove(call);
        notifyChanged();
        return ended;
    }

    private static boolean isIncomingDirection(Call call) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && call.getDetails().getCallDirection() == Call.Details.DIRECTION_INCOMING;
    }

    public static boolean hasCalls() {
        return !CALLS.isEmpty();
    }

    public static List<Call> getCalls() {
        return new ArrayList<>(CALLS);
    }

    public static int getState(Call call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return call.getDetails().getState();
        }
        return call.getState();
    }

    public static Call getPrimaryCall() {
        Call ringing = null;
        Call dialing = null;
        Call active = null;
        Call holding = null;
        Call other = null;
        for (Call call : CALLS) {
            int state = getState(call);
            if (state == Call.STATE_RINGING) {
                ringing = call;
            } else if (state == Call.STATE_DIALING || state == Call.STATE_CONNECTING
                    || state == Call.STATE_SELECT_PHONE_ACCOUNT || state == Call.STATE_NEW
                    || state == Call.STATE_PULLING_CALL) {
                dialing = call;
            } else if (state == Call.STATE_ACTIVE) {
                active = call;
            } else if (state == Call.STATE_HOLDING) {
                holding = call;
            } else if (call.getParent() == null) {
                other = call;
            }
        }
        if (ringing != null) {
            return ringing;
        }
        if (dialing != null) {
            return dialing;
        }
        if (active != null) {
            return active.getParent() != null ? active.getParent() : active;
        }
        if (holding != null) {
            return holding;
        }
        return other;
    }

    public static Call getSecondaryCall() {
        Call primary = getPrimaryCall();
        for (Call call : CALLS) {
            int state = getState(call);
            if (call != primary && call.getParent() == null
                    && state != Call.STATE_DISCONNECTED && state != Call.STATE_DISCONNECTING) {
                return call;
            }
        }
        return null;
    }

    public static String getNumber(Call call) {
        if (call == null) {
            return "";
        }
        Uri handle = call.getDetails().getHandle();
        return handle == null ? "" : handle.getSchemeSpecificPart();
    }

    public static ContactModel getContact(Call call) {
        return call == null ? null : CONTACTS.get(call);
    }

    public static boolean isConference(Call call) {
        return call != null && call.getDetails().hasProperty(Call.Details.PROPERTY_CONFERENCE);
    }

    public static String getDisplayName(Context context, Call call) {
        if (isConference(call)) {
            return context.getString(R.string.conference);
        }
        ContactModel contact = getContact(call);
        if (contact != null && !TextUtils.isEmpty(contact.name)) {
            return contact.name;
        }
        String number = getNumber(call);
        if (PhoneUtils.isPrivate(number)) {
            return context.getString(R.string.private_number);
        }
        return PhoneUtils.format(context, number);
    }

    public static long getConnectTime(Call call) {
        return call == null ? 0 : call.getDetails().getConnectTimeMillis();
    }

    public static void answer(Call call) {
        if (call != null) {
            call.answer(VideoProfile.STATE_AUDIO_ONLY);
        }
    }

    public static void reject(Call call) {
        if (call != null) {
            call.reject(false, null);
        }
    }

    public static void rejectWithMessage(Call call, String message) {
        if (call != null) {
            call.reject(true, message);
        }
    }

    public static void hangup(Call call) {
        if (call == null) {
            return;
        }
        if (getState(call) == Call.STATE_RINGING) {
            reject(call);
        } else {
            call.disconnect();
        }
    }

    public static boolean canHold(Call call) {
        return call != null && call.getDetails().can(Call.Details.CAPABILITY_HOLD);
    }

    public static void toggleHold(Call call) {
        if (call == null) {
            return;
        }
        if (getState(call) == Call.STATE_HOLDING) {
            call.unhold();
        } else {
            call.hold();
        }
    }

    public static void swap() {
        for (Call call : CALLS) {
            if (getState(call) == Call.STATE_HOLDING && call.getParent() == null) {
                call.unhold();
                return;
            }
        }
    }

    public static boolean canMerge() {
        Call primary = getPrimaryCall();
        if (primary == null) {
            return false;
        }
        return !primary.getConferenceableCalls().isEmpty()
                || primary.getDetails().can(Call.Details.CAPABILITY_MERGE_CONFERENCE);
    }

    public static void merge() {
        Call primary = getPrimaryCall();
        if (primary == null) {
            return;
        }
        List<Call> conferenceable = primary.getConferenceableCalls();
        if (!conferenceable.isEmpty()) {
            primary.conference(conferenceable.get(0));
        } else if (primary.getDetails().can(Call.Details.CAPABILITY_MERGE_CONFERENCE)) {
            primary.mergeConference();
        }
    }

    public static void playDtmf(char digit) {
        Call call = getPrimaryCall();
        if (call != null && getState(call) == Call.STATE_ACTIVE) {
            call.playDtmfTone(digit);
            call.stopDtmfTone();
        }
    }

    public static CallAudioState getAudioState() {
        return service == null ? null : service.getCallAudioState();
    }

    public static boolean isMuted() {
        CallAudioState state = getAudioState();
        return state != null && state.isMuted();
    }

    public static void setMuted(boolean muted) {
        if (service != null) {
            service.setMuted(muted);
        }
    }

    public static int getAudioRoute() {
        CallAudioState state = getAudioState();
        return state == null ? CallAudioState.ROUTE_EARPIECE : state.getRoute();
    }

    public static boolean isBluetoothAvailable() {
        CallAudioState state = getAudioState();
        return state != null && (state.getSupportedRouteMask() & CallAudioState.ROUTE_BLUETOOTH) != 0;
    }

    public static void setAudioRoute(int route) {
        if (service != null) {
            service.setAudioRoute(route);
        }
    }

    public static void addListener(Listener listener) {
        if (!LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    static void notifyChanged() {
        for (Listener listener : LISTENERS) {
            listener.onCallsChanged();
        }
    }
}
