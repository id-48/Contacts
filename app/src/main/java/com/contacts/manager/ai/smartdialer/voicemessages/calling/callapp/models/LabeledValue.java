package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models;

public class LabeledValue {

    public String value;
    public int type;
    public String label;

    public LabeledValue(String value, int type, String label) {
        this.value = value;
        this.type = type;
        this.label = label;
    }
}
