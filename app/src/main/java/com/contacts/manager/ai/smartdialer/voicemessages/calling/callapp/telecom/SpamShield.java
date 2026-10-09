package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom;

import android.content.Context;
import android.text.TextUtils;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class SpamShield {

    private static final String[] PREMIUM_PREFIXES = {"+881", "+882", "+883", "+870", "+979", "+808", "+900"};
    private static final Map<String, String> CALLING_CODES = new HashMap<>();

    static {
        CALLING_CODES.put("in", "+91");
        CALLING_CODES.put("us", "+1");
        CALLING_CODES.put("ca", "+1");
        CALLING_CODES.put("gb", "+44");
        CALLING_CODES.put("ae", "+971");
        CALLING_CODES.put("sa", "+966");
        CALLING_CODES.put("pk", "+92");
        CALLING_CODES.put("bd", "+880");
        CALLING_CODES.put("np", "+977");
        CALLING_CODES.put("lk", "+94");
        CALLING_CODES.put("id", "+62");
        CALLING_CODES.put("ph", "+63");
        CALLING_CODES.put("vn", "+84");
        CALLING_CODES.put("br", "+55");
        CALLING_CODES.put("mx", "+52");
        CALLING_CODES.put("de", "+49");
        CALLING_CODES.put("fr", "+33");
        CALLING_CODES.put("es", "+34");
        CALLING_CODES.put("it", "+39");
        CALLING_CODES.put("tr", "+90");
        CALLING_CODES.put("ru", "+7");
        CALLING_CODES.put("jp", "+81");
        CALLING_CODES.put("cn", "+86");
        CALLING_CODES.put("ng", "+234");
        CALLING_CODES.put("eg", "+20");
        CALLING_CODES.put("au", "+61");
    }

    private SpamShield() {
    }

    public static boolean isTelemarketer(String number) {
        if (TextUtils.isEmpty(number)) {
            return false;
        }
        String digits = number.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+91")) {
            digits = digits.substring(3);
        } else if (digits.startsWith("0091")) {
            digits = digits.substring(4);
        } else if (digits.startsWith("0") && digits.length() == 11) {
            digits = digits.substring(1);
        }
        return digits.length() == 10 && digits.startsWith("140");
    }

    public static boolean shouldBlock(String number) {
        return PhoneUtils.isPrivate(number) || isTelemarketer(number);
    }

    public static boolean isSuspicious(Context context, String number, boolean saved) {
        if (saved) {
            return false;
        }
        if (PhoneUtils.isPrivate(number) || isTelemarketer(number)) {
            return true;
        }
        String normalized = number.replaceAll("[^0-9+]", "");
        if (normalized.startsWith("00")) {
            normalized = "+" + normalized.substring(2);
        }
        for (String prefix : PREMIUM_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                return true;
            }
        }
        if (normalized.startsWith("+")) {
            String home = CALLING_CODES.get(PhoneUtils.countryIso(context).toLowerCase(Locale.ROOT));
            return home != null && !normalized.startsWith(home);
        }
        return false;
    }
}
