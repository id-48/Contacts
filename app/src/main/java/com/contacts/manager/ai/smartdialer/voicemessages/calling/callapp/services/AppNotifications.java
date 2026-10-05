package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.AppConstants;

import java.util.concurrent.TimeUnit;

/** Simple app notifications: optional big image, opens the link in a Chrome tab or the app when there is none. */
public final class AppNotifications {

    private static final long IMAGE_TIMEOUT_SECONDS = 20;

    private AppNotifications() {
    }

    @WorkerThread
    public static void show(Context context, int id, String title, String body,
                            @Nullable String imageUrl, @Nullable String link) {
        if (!NotificationService.canPost(context)) return;
        Bitmap image = loadImage(context, imageUrl);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, AppConstants.CHANNEL_CUSTOM)
                .setSmallIcon(R.drawable.ic_notifications_filled)
                .setColor(ContextCompat.getColor(context, R.color.brand_blue))
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(contentIntent(context, id, link));
        if (image != null) {
            builder.setLargeIcon(image)
                    .setStyle(new NotificationCompat.BigPictureStyle()
                            .bigPicture(image)
                            .bigLargeIcon((Bitmap) null)
                            .setSummaryText(body));
        } else {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(body));
        }
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    @Nullable
    private static Bitmap loadImage(Context context, @Nullable String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            return Glide.with(context).asBitmap().load(url).submit().get(IMAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            return null;
        }
    }

    private static PendingIntent contentIntent(Context context, int id, @Nullable String link) {
        Intent intent;
        if (link != null && !link.isEmpty()) {
            intent = new CustomTabsIntent.Builder().setShowTitle(true).build().intent.setData(Uri.parse(link));
        } else {
            intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (intent == null) intent = new Intent(Intent.ACTION_MAIN).setPackage(context.getPackageName());
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
