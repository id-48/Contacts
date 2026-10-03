package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models;

import android.text.TextUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContactModel {

    public long id;
    public String lookupKey;
    public String name;
    public String photoUri;
    public String fullPhotoUri;
    public String primaryNumber;
    public boolean starred;

    public long rawContactId;
    public String accountType;
    public String accountName;

    public String prefix;
    public String firstName;
    public String middleName;
    public String lastName;
    public String suffix;
    public String company;
    public String jobTitle;
    public String address;
    public String birthday;
    public String nickname;
    public String note;

    public final List<LabeledValue> phones = new ArrayList<>();
    public final List<LabeledValue> emails = new ArrayList<>();
    public final List<Long> editableDataIds = new ArrayList<>();

    public byte[] photoBytes;
    public boolean removePhoto;

    public String getDisplayName() {
        if (!TextUtils.isEmpty(name)) {
            return name;
        }
        if (!TextUtils.isEmpty(primaryNumber)) {
            return primaryNumber;
        }
        return "";
    }

    public String getSectionLetter() {
        String display = getDisplayName();
        if (display.isEmpty()) {
            return "#";
        }
        int codePoint = display.codePointAt(0);
        if (!Character.isLetter(codePoint)) {
            return "#";
        }
        String first = new String(Character.toChars(codePoint));
        String plain = Normalizer.normalize(first, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toUpperCase(Locale.getDefault());
    }

    public boolean hasPhoto() {
        return !TextUtils.isEmpty(photoUri) || !TextUtils.isEmpty(fullPhotoUri);
    }
}
