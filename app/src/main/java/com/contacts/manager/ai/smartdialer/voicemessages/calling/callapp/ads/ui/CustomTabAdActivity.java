package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.providers.AdProvider;

/** Opens the admin's Custom Ads URL in a Chrome Custom Tab; the ad is dismissed when the user comes back. */
public class CustomTabAdActivity extends AppCompatActivity {

    private static final String EXTRA_URL = "url";
    private static final String STATE_OPENED = "opened";

    @Nullable
    private static AdProvider.ShowCallback pendingCallback;

    @Nullable
    private AdProvider.ShowCallback callback;
    private boolean opened;
    private boolean left;

    public static void start(Activity host, String url, AdProvider.ShowCallback callback) {
        pendingCallback = callback;
        try {
            host.startActivity(new Intent(host, CustomTabAdActivity.class).putExtra(EXTRA_URL, url));
        } catch (RuntimeException e) {
            pendingCallback = null;
            callback.onFailedToShow(e.getClass().getSimpleName());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        callback = pendingCallback;
        pendingCallback = null;
        if (callback == null) {
            finish();
            return;
        }
        opened = savedInstanceState != null && savedInstanceState.getBoolean(STATE_OPENED);
        if (opened) {
            left = true;
            return;
        }
        String url = getIntent().getStringExtra(EXTRA_URL);
        try {
            new CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(this, Uri.parse(url));
            opened = true;
            callback.onShown();
        } catch (RuntimeException e) {
            AdProvider.ShowCallback failed = callback;
            callback = null;
            failed.onFailedToShow(e.getClass().getSimpleName());
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (opened) left = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (left) finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_OPENED, opened);
    }

    @Override
    public void finish() {
        super.finish();
        if (callback != null) {
            AdProvider.ShowCallback done = callback;
            callback = null;
            done.onDismissed();
        }
    }

    @Override
    protected void onDestroy() {
        if (callback != null) {
            AdProvider.ShowCallback current = callback;
            callback = null;
            if (isChangingConfigurations()) pendingCallback = current;
            else current.onDismissed();
        }
        super.onDestroy();
    }
}
