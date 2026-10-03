package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils;

import android.content.Context;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.util.Locale;

public final class PhoneUtils {

    private static final String T9_KEYS = "22233344455566677778889999";

    private PhoneUtils() {
    }

    public static String normalize(String number) {
        if (number == null) {
            return "";
        }
        String normalized = PhoneNumberUtils.normalizeNumber(number);
        return normalized == null ? "" : normalized;
    }

    public static String key(String number) {
        if (number == null) {
            return "";
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }
        if (digits.length() > 10) {
            return digits.substring(digits.length() - 10);
        }
        return digits.toString();
    }

    public static boolean sameNumber(String first, String second) {
        String a = key(first);
        String b = key(second);
        return !a.isEmpty() && a.equals(b);
    }

    public static boolean isPrivate(String number) {
        return TextUtils.isEmpty(number) || number.startsWith("-");
    }

    public static boolean isValidNumber(String number) {
        if (TextUtils.isEmpty(number)) {
            return false;
        }
        int digits = 0;
        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            if (Character.isDigit(c)) {
                digits++;
            } else if ("+*#()- .,;".indexOf(c) < 0) {
                return false;
            }
        }
        return digits >= 2;
    }

    public static String countryIso(Context context) {
        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String iso = null;
        if (telephony != null) {
            iso = telephony.getNetworkCountryIso();
            if (TextUtils.isEmpty(iso)) {
                iso = telephony.getSimCountryIso();
            }
        }
        if (TextUtils.isEmpty(iso)) {
            iso = Locale.getDefault().getCountry();
        }
        return iso == null ? "" : iso.toUpperCase(Locale.US);
    }

    public static String format(Context context, String number) {
        if (TextUtils.isEmpty(number) || isPrivate(number)) {
            return number == null ? "" : number;
        }
        String formatted = PhoneNumberUtils.formatNumber(number, countryIso(context));
        return formatted == null ? number : formatted;
    }

    public static String toT9(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(text.length());
        String lower = text.toLowerCase(Locale.US);
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c >= 'a' && c <= 'z') {
                builder.append(T9_KEYS.charAt(c - 'a'));
            } else if (c >= '0' && c <= '9') {
                builder.append(c);
            } else {
                builder.append(' ');
            }
        }
        return builder.toString();
    }

    public static boolean matchesT9(String name, String digits) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(digits)) {
            return false;
        }
        String t9 = toT9(name);
        if (t9.replace(" ", "").startsWith(digits)) {
            return true;
        }
        for (String word : t9.split(" ")) {
            if (word.startsWith(digits)) {
                return true;
            }
        }
        return false;
    }
}
