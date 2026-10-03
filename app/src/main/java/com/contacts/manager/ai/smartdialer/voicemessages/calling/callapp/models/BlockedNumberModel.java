package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models;

import android.text.TextUtils;

public class BlockedNumberModel {

    public long id;
    public String number;
    public String name;

    public String getDisplayName() {
        return TextUtils.isEmpty(name) ? number : name;
    }
}
