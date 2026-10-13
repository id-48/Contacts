package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads;

import android.app.Activity;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.MainThread;

@MainThread
public final class ListNativeAds {

    private final Activity activity;
    private final String screenKey;
    private final SparseArray<FrameLayout> slots = new SparseArray<>();

    public ListNativeAds(Activity activity, String screenKey) {
        this.activity = activity;
        this.screenKey = screenKey;
    }

    public int interval() {
        return AdsManager.listAdInterval(screenKey);
    }

    public static boolean adAfter(int interval, int shown, int total) {
        return interval > 0 && shown < total && shown % interval == 0;
    }

    public void attach(int slot, ViewGroup host) {
        FrameLayout container = slots.get(slot);
        if (container != null) {
            place(container, host);
            return;
        }
        container = new FrameLayout(activity);
        container.setVisibility(View.GONE);
        slots.put(slot, container);
        place(container, host);
        AdsManager.showListNative(activity, container, screenKey, slot);
    }

    private static void place(FrameLayout container, ViewGroup host) {
        if (container.getParent() == host) return;
        if (container.getParent() instanceof ViewGroup) ((ViewGroup) container.getParent()).removeView(container);
        host.removeAllViews();
        host.addView(container, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
}
