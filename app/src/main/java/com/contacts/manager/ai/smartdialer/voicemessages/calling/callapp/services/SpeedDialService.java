package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public final class SpeedDialService {

    public static final int FIRST_DIGIT = 1;
    public static final int LAST_DIGIT = 9;

    public static class Entry {
        public final String name;
        public final String number;

        Entry(String name, String number) {
            this.name = name;
            this.number = number;
        }
    }

    private SpeedDialService() {
    }

    @Nullable
    public static Entry get(int digit) {
        String stored = StorageService.getSpeedDial(digit);
        if (stored == null) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(stored);
            String number = json.optString("number", "");
            return number.isEmpty() ? null : new Entry(json.optString("name", ""), number);
        } catch (JSONException e) {
            return null;
        }
    }

    public static void set(int digit, String name, String number) {
        try {
            StorageService.setSpeedDial(digit, new JSONObject()
                    .put("name", name == null ? "" : name)
                    .put("number", number).toString());
        } catch (JSONException ignored) {
        }
    }

    public static void clear(int digit) {
        StorageService.setSpeedDial(digit, null);
    }
}
