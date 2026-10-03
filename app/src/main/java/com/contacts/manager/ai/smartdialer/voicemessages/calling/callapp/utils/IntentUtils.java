package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.AppConstants;

public final class IntentUtils {

    private IntentUtils() {
    }

    public static boolean safeStart(Context context, Intent intent) {
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(context, R.string.no_app_found, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public static Intent smsIntent(String number, String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number == null ? "" : number, null));
        if (!TextUtils.isEmpty(body)) {
            intent.putExtra("sms_body", body);
        }
        return intent;
    }

    public static void openSms(Context context, String number) {
        safeStart(context, smsIntent(number, null));
    }

    public static void openEmail(Context context, String email) {
        safeStart(context, new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", email, null)));
    }

    public static void openMap(Context context, String address) {
        safeStart(context, new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(address))));
    }

    public static void shareText(Context context, String text, CharSequence title) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        safeStart(context, Intent.createChooser(send, title));
    }

    public static void shareApp(Context context) {
        String link = AppConstants.PLAY_STORE_WEB + context.getPackageName();
        shareText(context, context.getString(R.string.share_app_text, link), context.getString(R.string.share_app));
    }

    public static void rateApp(Context context) {
        Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.PLAY_STORE_MARKET + context.getPackageName()));
        try {
            context.startActivity(market);
        } catch (ActivityNotFoundException e) {
            safeStart(context, new Intent(Intent.ACTION_VIEW,
                    Uri.parse(AppConstants.PLAY_STORE_WEB + context.getPackageName())));
        }
    }
}
