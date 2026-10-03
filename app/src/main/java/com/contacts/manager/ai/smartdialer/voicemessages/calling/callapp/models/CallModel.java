package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models;

import android.provider.CallLog;
import android.text.TextUtils;

public class CallModel {

    public long id;
    public String number;
    public String name;
    public String photoUri;
    public int type;
    public long date;
    public long duration;
    public long contactId;
    public String lookupKey;

    public boolean isSavedContact() {
        return contactId > 0;
    }

    public boolean isMissed() {
        return type == CallLog.Calls.MISSED_TYPE;
    }

    public String getDisplayName() {
        if (!TextUtils.isEmpty(name)) {
            return name;
        }
        return number == null ? "" : number;
    }
}
