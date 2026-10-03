package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemThemePreviewRowBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ViewThemePreviewBinding;

public final class ThemePreview {

    private ThemePreview() {
    }

    public static Context themedContext(Context base, boolean dark) {
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | (dark ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        return new ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Contacts);
    }

    public static ViewThemePreviewBinding inflate(Activity activity, Context themed, ViewGroup parent) {
        LayoutInflater inflater = activity.getLayoutInflater().cloneInContext(themed);
        ViewThemePreviewBinding preview = ViewThemePreviewBinding.inflate(inflater, parent, false);
        preview.getRoot().setClipToOutline(true);
        bindRows(preview, inflater);
        return preview;
    }

    private static void bindRows(ViewThemePreviewBinding preview, LayoutInflater inflater) {
        addRow(inflater, preview.rowsToday, "Emily Clark", R.drawable.avatar_preview_emily, null,
                R.drawable.ic_call_received, R.color.call_incoming, R.string.incoming_call);
        addRow(inflater, preview.rowsToday, "Grace Walker", R.drawable.avatar_preview_grace, null,
                R.drawable.ic_call_missed, R.color.call_missed, R.string.missed_call);
        addRow(inflater, preview.rowsToday, "Philip Harris", 0, null,
                R.drawable.ic_call_made, R.color.call_outgoing, R.string.outgoing_call);
        addRow(inflater, preview.rowsYesterday, "James Moore", R.drawable.avatar_preview_james, null,
                R.drawable.ic_call_made, R.color.call_outgoing, R.string.outgoing_call);
        addRow(inflater, preview.rowsYesterday, "Daniel Kim", 0, "D",
                R.drawable.ic_call_received, R.color.call_incoming, R.string.incoming_call);
        View last = addRow(inflater, preview.rowsYesterday, "William Young", 0, "W",
                R.drawable.ic_block, R.color.text_secondary, R.string.block);
        last.setAlpha(0.45f);
    }

    private static View addRow(LayoutInflater inflater, LinearLayout parent, String name, @DrawableRes int photo,
                               String letter, @DrawableRes int typeIcon, @ColorRes int typeColor,
                               @StringRes int typeLabel) {
        Context context = inflater.getContext();
        ItemThemePreviewRowBinding row = ItemThemePreviewRowBinding.inflate(inflater, parent, true);
        if (photo != 0) {
            row.rowPhoto.setVisibility(View.VISIBLE);
            row.rowPhoto.setImageResource(photo);
        } else if (letter != null) {
            row.rowLetter.setVisibility(View.VISIBLE);
            row.rowLetter.setText(letter);
        } else {
            row.rowIcon.setVisibility(View.VISIBLE);
        }
        row.rowName.setText(name);
        row.rowTypeIcon.setImageResource(typeIcon);
        ImageViewCompat.setImageTintList(row.rowTypeIcon,
                ColorStateList.valueOf(ContextCompat.getColor(context, typeColor)));
        row.rowMeta.setText(context.getString(R.string.theme_preview_meta,
                context.getString(typeLabel), context.getString(R.string.theme_preview_time)));
        return row.getRoot();
    }
}
