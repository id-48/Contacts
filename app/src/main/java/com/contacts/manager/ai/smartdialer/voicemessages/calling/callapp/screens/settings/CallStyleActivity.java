package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.view.ViewCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityCallStyleBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemCallStyleBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.call.IncomingCallControlView;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.AppLockManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class CallStyleActivity extends BaseActivity {

    private static final String WALLPAPER_FILE = "call_wallpaper.jpg";

    private ActivityCallStyleBinding binding;
    private final List<ItemCallStyleBinding> cards = new ArrayList<>();
    private ActivityResultLauncher<PickVisualMediaRequest> pickImage;

    public static File wallpaperFile(Context context) {
        String path = StorageService.getCallWallpaper();
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        File file = new File(path);
        return file.exists() ? file : null;
    }

    public static void loadWallpaper(ImageView view, File file) {
        Glide.with(view).load(file).signature(new ObjectKey(file.lastModified())).centerCrop().into(view);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickImage = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::onImagePicked);
        binding = ActivityCallStyleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        binding.topBar.topTitle.setText(R.string.call_style_short);
        setupBack(binding.topBar.backButton);
        setupBackAd(AdScreens.CALL_STYLE_BACK);

        binding.chooseWallpaper.setOnClickListener(v -> showFullscreen(AdScreens.CALL_STYLE_WALLPAPER, () -> {
            AppLockManager.markExternalLaunch();
            pickImage.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        }));
        binding.removeWallpaper.setOnClickListener(v -> {
            File file = wallpaperFile(this);
            if (file != null) {
                file.delete();
            }
            StorageService.setCallWallpaper(null);
            HapticUtils.tap(v);
            renderWallpaper();
        });

        for (int style = 1; style <= IncomingCallControlView.STYLE_COUNT; style++) {
            addStyleCard(style);
            if (style == 3) {
                moveToList(binding.ads.nativeBigContainer);
            } else if (style == 7) {
                moveToList(binding.ads.nativeSmallContainer);
            }
        }
        showNativeAds(binding.ads.nativeBigContainer, binding.ads.nativeSmallContainer, AdScreens.CALL_STYLE);
        renderWallpaper();
        renderSelection();
    }

    private void addStyleCard(int style) {
        ItemCallStyleBinding card = ItemCallStyleBinding.inflate(getLayoutInflater(), binding.styleList, false);
        card.styleNumber.setText(String.valueOf(style));
        card.styleName.setText(IncomingCallControlView.styleName(style));
        card.styleControl.setInteractive(false);
        card.styleControl.configure(style, StorageService.isAnswerOnLeft(), true);
        card.styleControl.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        View.OnClickListener select = v -> select(style, v);
        card.styleCard.setOnClickListener(select);
        card.styleTouchShield.setOnClickListener(select);
        cards.add(card);
        binding.styleList.addView(card.getRoot());
    }

    private void moveToList(View container) {
        ViewGroup parent = (ViewGroup) container.getParent();
        if (parent != null) {
            parent.removeView(container);
        }
        binding.styleList.addView(container);
    }

    private void select(int style, View view) {
        if (StorageService.getCallStyle() == style) {
            return;
        }
        HapticUtils.tick(view);
        StorageService.setCallStyle(style);
        renderSelection();
        Toast.makeText(this, getString(R.string.call_style_applied, getString(IncomingCallControlView.styleName(style))),
                Toast.LENGTH_SHORT).show();
    }

    private void renderSelection() {
        int selected = StorageService.getCallStyle();
        for (int i = 0; i < cards.size(); i++) {
            ItemCallStyleBinding card = cards.get(i);
            boolean checked = i + 1 == selected;
            card.styleCard.setSelected(checked);
            card.styleCheck.setSelected(checked);
            card.styleCheck.setImageResource(checked ? R.drawable.ic_check : 0);
            ViewCompat.setStateDescription(card.styleCard, checked ? getString(R.string.selected) : null);
        }
    }

    private void renderWallpaper() {
        File file = wallpaperFile(this);
        boolean has = file != null;
        binding.removeWallpaper.setVisibility(has ? View.VISIBLE : View.GONE);
        binding.wallpaperPlaceholder.setVisibility(has ? View.GONE : View.VISIBLE);
        if (has) {
            loadWallpaper(binding.wallpaperPreview, file);
        } else {
            Glide.with(this).clear(binding.wallpaperPreview);
            binding.wallpaperPreview.setImageDrawable(null);
        }
        for (ItemCallStyleBinding card : cards) {
            card.styleWallpaper.setVisibility(has ? View.VISIBLE : View.GONE);
            card.styleScrim.setVisibility(has ? View.VISIBLE : View.GONE);
            card.stylePreview.setClipToOutline(true);
            if (has) {
                loadWallpaper(card.styleWallpaper, file);
            } else {
                Glide.with(this).clear(card.styleWallpaper);
            }
        }
    }

    private void onImagePicked(Uri uri) {
        if (uri == null) {
            return;
        }
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            File target = new File(app.getFilesDir(), WALLPAPER_FILE);
            boolean ok;
            try (InputStream in = app.getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(target)) {
                if (in == null) {
                    throw new IllegalStateException();
                }
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                ok = true;
            } catch (Exception e) {
                ok = false;
            }
            boolean saved = ok;
            AppExecutors.main(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (saved) {
                    StorageService.setCallWallpaper(target.getAbsolutePath());
                    Toast.makeText(this, R.string.call_wallpaper_saved, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
                }
                renderWallpaper();
            });
        });
    }
}
