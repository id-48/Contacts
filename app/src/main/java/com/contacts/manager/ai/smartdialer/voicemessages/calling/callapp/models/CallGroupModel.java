package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models;

import java.util.ArrayList;
import java.util.List;

public class CallGroupModel {

    public final List<CallModel> calls = new ArrayList<>();

    public CallGroupModel(CallModel first) {
        calls.add(first);
    }

    public CallModel getLatest() {
        return calls.get(0);
    }

    public int getCount() {
        return calls.size();
    }

    public String getNumber() {
        return getLatest().number;
    }

    public String getDisplayName() {
        return getLatest().getDisplayName();
    }

    public long getStableId() {
        return getLatest().id;
    }
}
