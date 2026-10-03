package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;

import java.io.File;
import java.util.Locale;

public class ContactAvatarView extends FrameLayout {

    private final ImageView imageView;
    private final TextView letterView;
    private boolean showingGlyph;

    public ContactAvatarView(Context context) {
        this(context, null);
    }

    public ContactAvatarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setBackgroundResource(R.drawable.bg_circle_tint);
        setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        setClipToOutline(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(imageView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        letterView = new TextView(context);
        letterView.setGravity(Gravity.CENTER);
        letterView.setTextColor(ContextCompat.getColor(context, R.color.on_primary_container));
        letterView.setTypeface(ResourcesCompat.getFont(context, R.font.roboto_medium));
        letterView.setIncludeFontPadding(false);
        addView(letterView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        bind(null, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        letterView.setTextSize(TypedValue.COMPLEX_UNIT_PX, h * 0.42f);
        applyGlyphPadding();
    }

    private void applyGlyphPadding() {
        int padding = showingGlyph ? (int) (getHeight() * 0.26f) : 0;
        imageView.setPadding(padding, padding, padding, padding);
    }

    public void bind(String name, String photoUri) {
        setBackgroundResource(R.drawable.bg_circle_tint);
        if (!TextUtils.isEmpty(photoUri)) {
            showPhoto(Uri.parse(photoUri));
            return;
        }
        String letter = initialOf(name);
        if (letter != null) {
            clearImage();
            imageView.setVisibility(GONE);
            letterView.setVisibility(VISIBLE);
            letterView.setText(letter);
        } else {
            showGlyph(R.drawable.ic_person_filled, R.color.primary);
        }
    }

    public void bindFile(File file) {
        setBackgroundResource(R.drawable.bg_circle_tint);
        showingGlyph = false;
        applyGlyphPadding();
        imageView.setImageTintList(null);
        imageView.setVisibility(VISIBLE);
        letterView.setVisibility(GONE);
        if (isValidContextForGlide()) {
            Glide.with(this).load(file).circleCrop().into(imageView);
        }
    }

    public void bindBlocked() {
        setBackgroundResource(R.drawable.bg_circle_error);
        showGlyph(R.drawable.ic_block, R.color.error);
    }

    private void showPhoto(Uri uri) {
        showingGlyph = false;
        applyGlyphPadding();
        imageView.setImageTintList(null);
        imageView.setVisibility(VISIBLE);
        letterView.setVisibility(GONE);
        if (isValidContextForGlide()) {
            Glide.with(this).load(uri).circleCrop().into(imageView);
        } else {
            imageView.setImageURI(uri);
        }
    }

    private void showGlyph(int iconRes, int tintRes) {
        clearImage();
        showingGlyph = true;
        applyGlyphPadding();
        letterView.setVisibility(GONE);
        imageView.setVisibility(VISIBLE);
        imageView.setImageResource(iconRes);
        imageView.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), tintRes)));
    }

    private void clearImage() {
        if (isValidContextForGlide()) {
            Glide.with(this).clear(imageView);
        }
    }

    private boolean isValidContextForGlide() {
        Context context = getContext();
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            return !activity.isDestroyed() && !activity.isFinishing();
        }
        return true;
    }

    public static String initialOf(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int codePoint = trimmed.codePointAt(0);
        if (!Character.isLetter(codePoint)) {
            return null;
        }
        return new String(Character.toChars(codePoint)).toUpperCase(Locale.getDefault());
    }
}
