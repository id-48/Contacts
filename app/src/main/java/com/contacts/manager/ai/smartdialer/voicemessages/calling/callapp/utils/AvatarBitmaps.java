package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.ContactAvatarView;

import java.io.InputStream;

public final class AvatarBitmaps {

    private AvatarBitmaps() {
    }

    public static Bitmap create(Context context, String name, String photoUri, int size) {
        Bitmap photo = loadPhoto(context, photoUri, size);
        if (photo != null) {
            return circle(photo, size);
        }
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
        background.setColor(ContextCompat.getColor(context, R.color.primary_container));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, background);

        String letter = ContactAvatarView.initialOf(name);
        if (letter != null) {
            Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
            text.setColor(ContextCompat.getColor(context, R.color.on_primary_container));
            text.setTextSize(size * 0.42f);
            text.setTextAlign(Paint.Align.CENTER);
            Typeface typeface = ResourcesCompat.getFont(context, R.font.roboto_medium);
            if (typeface != null) {
                text.setTypeface(typeface);
            }
            Rect bounds = new Rect();
            text.getTextBounds(letter, 0, letter.length(), bounds);
            canvas.drawText(letter, size / 2f, size / 2f + bounds.height() / 2f, text);
        } else {
            Drawable glyph = ContextCompat.getDrawable(context, R.drawable.ic_person_filled);
            if (glyph != null) {
                glyph = glyph.mutate();
                glyph.setTint(ContextCompat.getColor(context, R.color.primary));
                int inset = (int) (size * 0.22f);
                glyph.setBounds(inset, inset, size - inset, size - inset);
                glyph.draw(canvas);
            }
        }
        return bitmap;
    }

    private static Bitmap loadPhoto(Context context, String photoUri, int size) {
        if (TextUtils.isEmpty(photoUri)) {
            return null;
        }
        try (InputStream stream = context.getContentResolver().openInputStream(Uri.parse(photoUri))) {
            if (stream == null) {
                return null;
            }
            Bitmap decoded = BitmapFactory.decodeStream(stream);
            if (decoded == null) {
                return null;
            }
            return Bitmap.createScaledBitmap(decoded, size, size, true);
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap circle(Bitmap source, int size) {
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        return output;
    }
}
