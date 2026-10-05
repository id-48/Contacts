package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.providers;

import android.view.View;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

@MainThread
final class SimpleInlineAd implements AdProvider.InlineAd {

    private final View view;
    private final Runnable destroy;
    @Nullable
    private Runnable impressionListener;
    private int pendingImpressions;

    SimpleInlineAd(View view, Runnable destroy) {
        this.view = view;
        this.destroy = destroy;
    }

    void impression() {
        if (impressionListener != null) impressionListener.run();
        else pendingImpressions++;
    }

    @Override
    public View view() {
        return view;
    }

    @Override
    public void setImpressionListener(Runnable listener) {
        impressionListener = listener;
        for (; pendingImpressions > 0; pendingImpressions--) listener.run();
    }

    @Override
    public void destroy() {
        impressionListener = null;
        destroy.run();
    }
}
