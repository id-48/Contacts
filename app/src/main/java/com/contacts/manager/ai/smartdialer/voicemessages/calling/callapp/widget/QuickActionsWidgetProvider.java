package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.IntentKeys;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.dialer.DialerActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.main.MainActivity;

public class QuickActionsWidgetProvider extends AppWidgetProvider {

    private static final int REQUEST_RECENT = 1;
    private static final int REQUEST_DIALPAD = 2;
    private static final int REQUEST_CONTACTS = 3;

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        RemoteViews views = build(context);
        for (int id : appWidgetIds) {
            manager.updateAppWidget(id, views);
        }
    }

    private static RemoteViews build(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quick_actions);
        views.setOnClickPendingIntent(R.id.widgetRecent, openTab(context, MainActivity.TAB_RECENT, REQUEST_RECENT));
        views.setOnClickPendingIntent(R.id.widgetContacts, openTab(context, MainActivity.TAB_CONTACTS, REQUEST_CONTACTS));

        Intent dialpad = new Intent(context, DialerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        views.setOnClickPendingIntent(R.id.widgetDialpad, PendingIntent.getActivity(context, REQUEST_DIALPAD, dialpad,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        return views;
    }

    private static PendingIntent openTab(Context context, int tab, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class)
                .putExtra(IntentKeys.EXTRA_TAB, tab)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
