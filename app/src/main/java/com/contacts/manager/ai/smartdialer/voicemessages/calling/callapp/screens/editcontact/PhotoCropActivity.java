package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.editcontact;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.IntentKeys;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityPhotoCropBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PhotoCropActivity extends BaseActivity {

    private static final int MAX_DECODE_SIZE = 2048;
    private static final int OUTPUT_SIZE = 720;

    private ActivityPhotoCropBinding binding;

    public static Intent intent(Context context, Uri uri) {
        return new Intent(context, PhotoCropActivity.class)
                .putExtra(IntentKeys.EXTRA_IMAGE_URI, uri.toString())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhotoCropBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), binding.getRoot());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        binding.cancelButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        binding.rotateButton.setOnClickListener(v -> binding.cropView.rotate());
        binding.doneButton.setOnClickListener(v -> save());

        String value = getIntent().getStringExtra(IntentKeys.EXTRA_IMAGE_URI);
        if (value == null) {
            finish();
            return;
        }
        Uri uri = Uri.parse(value);
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            Bitmap bitmap = decode(app, uri);
            AppExecutors.main(() -> {
                if (isDestroyed()) {
                    return;
                }
                binding.progress.setVisibility(View.GONE);
                if (bitmap == null) {
                    Toast.makeText(app, R.string.photo_failed, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                binding.cropView.setBitmap(bitmap);
                binding.doneButton.setEnabled(true);
            });
        });
    }

    private static Bitmap decode(Context context, Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(input, null, bounds);
            }
            int sample = 1;
            while (bounds.outWidth / sample > MAX_DECODE_SIZE || bounds.outHeight / sample > MAX_DECODE_SIZE) {
                sample *= 2;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            Bitmap bitmap;
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(input, null, options);
            }
            if (bitmap == null) {
                return null;
            }
            int rotation = 0;
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                if (input != null) {
                    ExifInterface exif = new ExifInterface(input);
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL);
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                        rotation = 90;
                    } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                        rotation = 180;
                    } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                        rotation = 270;
                    }
                }
            } catch (Exception ignored) {
            }
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (rotated != bitmap) {
                    bitmap.recycle();
                }
                bitmap = rotated;
            }
            return bitmap;
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    private void save() {
        binding.doneButton.setEnabled(false);
        Bitmap cropped = binding.cropView.crop(OUTPUT_SIZE);
        if (cropped == null) {
            Toast.makeText(this, R.string.photo_failed, Toast.LENGTH_SHORT).show();
            binding.doneButton.setEnabled(true);
            return;
        }
        File dir = new File(getCacheDir(), "photos");
        AppExecutors.io(() -> {
            File file = new File(dir, "crop_" + System.currentTimeMillis() + ".jpg");
            boolean ok = false;
            try {
                if (dir.exists() || dir.mkdirs()) {
                    try (FileOutputStream output = new FileOutputStream(file)) {
                        ok = cropped.compress(Bitmap.CompressFormat.JPEG, 88, output);
                    }
                }
            } catch (Exception ignored) {
            }
            cropped.recycle();
            boolean success = ok;
            AppExecutors.main(() -> {
                if (isDestroyed()) {
                    return;
                }
                if (success) {
                    setResult(RESULT_OK, new Intent().putExtra(IntentKeys.EXTRA_IMAGE_URI, file.getAbsolutePath()));
                    finish();
                } else {
                    Toast.makeText(this, R.string.photo_failed, Toast.LENGTH_SHORT).show();
                    binding.doneButton.setEnabled(true);
                }
            });
        });
    }
}
