package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote;

/** Numeric dotted-version comparison: "1.10" > "1.9", "1.0" == "1.0.0". Non-digits are ignored. */
public final class VersionUtils {

    private VersionUtils() {
    }

    public static int compare(String a, String b) {
        String[] left = normalize(a).split("\\.");
        String[] right = normalize(b).split("\\.");
        int length = Math.max(left.length, right.length);
        for (int i = 0; i < length; i++) {
            long l = i < left.length ? parse(left[i]) : 0;
            long r = i < right.length ? parse(right[i]) : 0;
            if (l != r) return l < r ? -1 : 1;
        }
        return 0;
    }

    private static String normalize(String version) {
        return version == null ? "" : version.trim();
    }

    private static long parse(String part) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < part.length() && Character.isDigit(part.charAt(i)); i++) {
            digits.append(part.charAt(i));
        }
        if (digits.length() == 0) return 0;
        try {
            return Long.parseLong(digits.length() > 18 ? digits.substring(0, 18) : digits.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
