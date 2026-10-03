package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.widget.Toast;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

public final class WhatsAppService {

    private static final String[][] APPS = {
            {"com.whatsapp", "vnd.android.cursor.item/vnd.com.whatsapp.video.call"},
            {"com.whatsapp.w4b", "vnd.android.cursor.item/vnd.com.whatsapp.w4b.video.call"}
    };

    private WhatsAppService() {
    }

    public static boolean isInstalled(Context context) {
        return installedPackage(context) != null;
    }

    public static void videoCall(Activity activity, String number) {
        if (!PhoneUtils.isValidNumber(number)) {
            Toast.makeText(activity, R.string.invalid_number, Toast.LENGTH_SHORT).show();
            return;
        }
        if (installedPackage(activity) == null) {
            Toast.makeText(activity, R.string.whatsapp_not_installed, Toast.LENGTH_SHORT).show();
            return;
        }
        Context app = activity.getApplicationContext();
        AppExecutors.io(() -> {
            Intent intent = findVideoCallIntent(app, number);
            AppExecutors.main(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }
                if (intent != null && tryStart(activity, intent)) {
                    return;
                }
                openChat(activity, number);
            });
        });
    }

    private static Intent findVideoCallIntent(Context context, String number) {
        if (!ContactsService.canRead(context)) {
            return null;
        }
        String key = PhoneUtils.key(number);
        if (key.isEmpty()) {
            return null;
        }
        for (String[] entry : APPS) {
            if (!isPackageInstalled(context, entry[0])) {
                continue;
            }
            try (Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                    new String[]{ContactsContract.Data._ID, ContactsContract.Data.DATA1},
                    ContactsContract.Data.MIMETYPE + "=?", new String[]{entry[1]}, null)) {
                if (cursor == null) {
                    continue;
                }
                while (cursor.moveToNext()) {
                    String jid = cursor.getString(1);
                    if (jid != null && PhoneUtils.key(jid.split("@")[0]).equals(key)) {
                        Uri uri = Uri.withAppendedPath(ContactsContract.Data.CONTENT_URI, cursor.getString(0));
                        return new Intent(Intent.ACTION_VIEW)
                                .setDataAndType(uri, entry[1])
                                .setPackage(entry[0])
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                }
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private static void openChat(Activity activity, String number) {
        String packageName = installedPackage(activity);
        String e164 = PhoneNumberUtils.formatNumberToE164(number, PhoneUtils.countryIso(activity));
        String digits = (TextUtils.isEmpty(e164) ? PhoneUtils.normalize(number) : e164).replaceAll("\\D", "");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + digits)).setPackage(packageName);
        if (tryStart(activity, intent)) {
            Toast.makeText(activity, R.string.whatsapp_video_hint, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(activity, R.string.whatsapp_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean tryStart(Activity activity, Intent intent) {
        try {
            activity.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    private static String installedPackage(Context context) {
        for (String[] entry : APPS) {
            if (isPackageInstalled(context, entry[0])) {
                return entry[0];
            }
        }
        return null;
    }

    private static boolean isPackageInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
