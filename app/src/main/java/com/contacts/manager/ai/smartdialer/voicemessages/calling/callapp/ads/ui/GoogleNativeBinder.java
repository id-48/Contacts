package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

/** Inflates the big or small Google native layout and binds every asset the ad provides. */
public final class GoogleNativeBinder {

    private GoogleNativeBinder() {
    }

    public static NativeAdView bind(Activity activity, NativeAd ad, boolean big) {
        NativeAdView view = (NativeAdView) LayoutInflater.from(activity).inflate(
                big ? R.layout.ad_native_big_google : R.layout.ad_native_small_google, null, false);

        TextView headline = view.findViewById(R.id.ad_headline);
        headline.setText(ad.getHeadline());
        view.setHeadlineView(headline);

        TextView body = view.findViewById(R.id.ad_body);
        setText(body, ad.getBody());
        view.setBodyView(body);

        Button cta = view.findViewById(R.id.ad_call_to_action);
        setText(cta, ad.getCallToAction());
        view.setCallToActionView(cta);

        ImageView icon = view.findViewById(R.id.ad_icon);
        if (icon != null) {
            if (ad.getIcon() != null && ad.getIcon().getDrawable() != null) {
                icon.setImageDrawable(ad.getIcon().getDrawable());
                icon.setVisibility(View.VISIBLE);
            } else {
                icon.setVisibility(View.GONE);
            }
            view.setIconView(icon);
        }

        MediaView media = view.findViewById(R.id.ad_media);
        if (media != null) {
            if (ad.getMediaContent() != null) media.setMediaContent(ad.getMediaContent());
            media.setImageScaleType(big ? ImageView.ScaleType.FIT_CENTER : ImageView.ScaleType.CENTER_CROP);
            view.setMediaView(media);
        }

        view.setNativeAd(ad);
        return view;
    }

    private static void setText(TextView view, CharSequence text) {
        if (text == null || text.length() == 0) {
            view.setVisibility(View.GONE);
        } else {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        }
    }
}
