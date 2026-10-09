package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemTileRowBinding;

public final class TileRows {

    private TileRows() {
    }

    public static ItemTileRowBinding add(ViewGroup parent, CharSequence title, CharSequence subtitle) {
        ItemTileRowBinding row = ItemTileRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        row.tileTitle.setText(title);
        setSubtitle(row, subtitle);
        parent.addView(row.getRoot());
        return row;
    }

    public static void setSubtitle(ItemTileRowBinding row, CharSequence subtitle) {
        row.tileSubtitle.setText(subtitle);
        row.tileSubtitle.setVisibility(TextUtils.isEmpty(subtitle) ? View.GONE : View.VISIBLE);
    }

    public static void icon(ItemTileRowBinding row, @DrawableRes int icon) {
        row.tileIcon.setImageResource(icon);
        row.tileIcon.setVisibility(View.VISIBLE);
        row.tileBadge.setVisibility(View.GONE);
        row.tileAvatar.setVisibility(View.GONE);
    }

    public static void badge(ItemTileRowBinding row, CharSequence text) {
        row.tileBadge.setText(text);
        row.tileBadge.setVisibility(View.VISIBLE);
        row.tileIcon.setVisibility(View.GONE);
        row.tileAvatar.setVisibility(View.GONE);
    }

    public static void avatar(ItemTileRowBinding row, String name, String photoUri) {
        row.tileAvatar.bind(name, photoUri);
        row.tileAvatar.setVisibility(View.VISIBLE);
        row.tileBadge.setVisibility(View.GONE);
        row.tileIcon.setVisibility(View.GONE);
    }

    public static ImageButton action(ItemTileRowBinding row, @DrawableRes int icon, CharSequence description,
                                     @ColorRes int tint, View.OnClickListener listener) {
        ImageButton button = new ImageButton(row.getRoot().getContext());
        int size = row.getRoot().getResources().getDimensionPixelSize(R.dimen.space_40);
        int padding = row.getRoot().getResources().getDimensionPixelSize(R.dimen.space_8);
        button.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        button.setPadding(padding, padding, padding, padding);
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams();
        params.setMarginStart(row.getRoot().getResources().getDimensionPixelSize(R.dimen.space_4));
        int color = ContextCompat.getColor(button.getContext(), tint);
        Accent.soft(button, color);
        button.setImageResource(icon);
        button.setImageTintList(ColorStateList.valueOf(color));
        button.setContentDescription(description);
        button.setOnClickListener(listener);
        row.tileActions.addView(button);
        return button;
    }
}
