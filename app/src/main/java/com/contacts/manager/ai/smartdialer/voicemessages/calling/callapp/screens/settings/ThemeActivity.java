package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.ThemePreview;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.IntentKeys;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityThemeBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ViewThemeOptionBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.onboarding.DefaultPhoneActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;

public class ThemeActivity extends BaseActivity {

    private static final long PREVIEW_DURATION = 460L;
    private static final long CROSSFADE_DURATION = 260L;
    private static final long GLOBAL_APPLY_DELAY = 900L;

    private ActivityThemeBinding binding;
    private boolean onboarding;
    private View lightPhone;
    private View darkPhone;
    private int shownMode;
    private int previousMode;
    private int nightMask;
    private int stageWidth;
    private int stageHeight;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable applyGlobally = this::applyGlobalNightMode;
    private final Interpolator previewInterpolator = new OvershootInterpolator(0.7f);

    public static Intent intent(Context context, boolean onboarding) {
        return new Intent(context, ThemeActivity.class).putExtra(IntentKeys.EXTRA_ONBOARDING, onboarding);
    }

    public static String label(Context context, int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            return context.getString(R.string.theme_light);
        }
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            return context.getString(R.string.theme_dark);
        }
        return context.getString(R.string.theme_system);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onboarding = getIntent().getBooleanExtra(IntentKeys.EXTRA_ONBOARDING, false);
        shownMode = StorageService.getThemeMode();
        previousMode = shownMode;
        nightMask = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        setupBackAd(AdScreens.THEME_BACK);
        buildContent(savedInstanceState == null, shownMode);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int mask = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (mask == nightMask) {
            return;
        }
        nightMask = mask;
        int from = previousMode;
        previousMode = StorageService.getThemeMode();
        ImageView snapshot = snapshot();
        EdgeToEdge.enable(this);
        buildContent(false, from);
        if (snapshot != null) {
            ViewGroup content = findViewById(android.R.id.content);
            content.addView(snapshot, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            snapshot.animate().alpha(0f).setDuration(CROSSFADE_DURATION)
                    .withEndAction(() -> content.removeView(snapshot)).start();
        }
    }

    @Nullable
    private ImageView snapshot() {
        View root = binding.getRoot();
        if (root.getWidth() == 0 || root.getHeight() == 0) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        ImageView image = new ImageView(this);
        image.setImageBitmap(bitmap);
        image.setScaleType(ImageView.ScaleType.FIT_XY);
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        return image;
    }

    private void buildContent(boolean animateIn, int fromMode) {
        binding = ActivityThemeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        setupBack(binding.backButton);
        AdsManager.showNativeSmall(this, binding.nativeSmallContainer, AdScreens.THEME);
        if (onboarding) {
            binding.backButton.setVisibility(View.GONE);
            binding.themeTitle.setPaddingRelative(getResources().getDimensionPixelSize(R.dimen.space_12), 0, 0, 0);
            binding.doneButton.setVisibility(View.VISIBLE);
            binding.doneButton.setOnClickListener(v -> {
                startActivity(new Intent(this, DefaultPhoneActivity.class));
                finish();
            });
        }

        setupOption(binding.optionSystem, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.theme_system, R.drawable.ic_contrast);
        setupOption(binding.optionLight, AppCompatDelegate.MODE_NIGHT_NO, R.string.theme_light, R.drawable.ic_light_mode);
        setupOption(binding.optionDark, AppCompatDelegate.MODE_NIGHT_YES, R.string.theme_dark, R.drawable.ic_dark_mode);
        bindSelection(false);

        setupStage(animateIn, fromMode);
    }

    private void setupStage(boolean animateIn, int fromMode) {
        FrameLayout stage = binding.previewStage;
        stage.setClipToOutline(true);
        lightPhone = addPhone(stage, false);
        darkPhone = addPhone(stage, true);
        int target = StorageService.getThemeMode();
        boolean knownSize = stageWidth > 0 && stageHeight > 0;
        boolean[] pendingAnimation = {knownSize && fromMode != target};
        if (knownSize) {
            showPreview(pendingAnimation[0] ? fromMode : target, false);
        } else {
            lightPhone.setVisibility(View.INVISIBLE);
            darkPhone.setVisibility(View.INVISIBLE);
        }
        stage.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l == or - ol && b - t == ob - ot) {
                return;
            }
            stageWidth = r - l;
            stageHeight = b - t;
            boolean animate = pendingAnimation[0];
            pendingAnimation[0] = false;
            if (!animate) {
                showPreview(target, false);
            }
            lightPhone.setVisibility(View.VISIBLE);
            darkPhone.setVisibility(View.VISIBLE);
            if (animate) {
                v.post(() -> showPreview(target, true));
            }
        });
        if (animateIn) {
            animateIn();
        }
    }

    private View addPhone(FrameLayout stage, boolean dark) {
        Context themed = ThemePreview.themedContext(this, dark);
        View phone = ThemePreview.inflate(this, themed, stage).getRoot();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) phone.getLayoutParams();
        params.gravity = Gravity.CENTER;
        phone.setElevation(getResources().getDisplayMetrics().density * 10f);
        phone.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        stage.addView(phone, params);
        return phone;
    }

    private void showPreview(int mode, boolean animate) {
        shownMode = mode;
        ViewGroup.LayoutParams phone = lightPhone.getLayoutParams();
        if (stageWidth == 0 || stageHeight == 0 || phone.width <= 0 || phone.height <= 0) {
            return;
        }
        float width = stageWidth;
        float single = Math.min(stageHeight * 0.86f / phone.height, width * 0.62f / phone.width);
        float fan = single * 0.8f;
        float offset = width * 0.17f;

        if (mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            place(lightPhone, -offset, -7f, fan, 1f, 0f, animate);
            place(darkPhone, offset, 7f, fan, 1f, 1f, animate);
        } else if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            place(lightPhone, -offset, -9f, fan * 0.9f, 0f, 0f, animate);
            place(darkPhone, 0f, 0f, single, 1f, 1f, animate);
        } else {
            place(lightPhone, 0f, 0f, single, 1f, 1f, animate);
            place(darkPhone, offset, 9f, fan * 0.9f, 0f, 0f, animate);
        }
    }

    private void place(View phone, float x, float rotation, float scale, float alpha, float z, boolean animate) {
        phone.setTranslationZ(z * getResources().getDisplayMetrics().density * 4f);
        phone.animate().cancel();
        if (!animate) {
            phone.setTranslationX(x);
            phone.setRotation(rotation);
            phone.setScaleX(scale);
            phone.setScaleY(scale);
            phone.setAlpha(alpha);
            return;
        }
        phone.animate().translationX(x).rotation(rotation).scaleX(scale).scaleY(scale).alpha(alpha)
                .setDuration(PREVIEW_DURATION).setInterpolator(previewInterpolator).start();
    }

    private void animateIn() {
        float offset = getResources().getDimension(R.dimen.space_24);
        DecelerateInterpolator interpolator = new DecelerateInterpolator(1.6f);
        binding.previewStage.setAlpha(0f);
        binding.previewStage.setScaleX(0.96f);
        binding.previewStage.setScaleY(0.96f);
        binding.previewStage.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(40).setDuration(360)
                .setInterpolator(interpolator).start();

        View[] options = {binding.optionSystem.getRoot(), binding.optionLight.getRoot(), binding.optionDark.getRoot()};
        for (int i = 0; i < options.length; i++) {
            View option = options[i];
            option.setAlpha(0f);
            option.setTranslationY(offset);
            option.animate().alpha(1f).translationY(0f).setStartDelay(140 + i * 60L).setDuration(320)
                    .setInterpolator(interpolator).start();
        }
    }

    private void setupOption(ViewThemeOptionBinding option, int mode, int labelRes, @DrawableRes int icon) {
        option.themeLabel.setText(labelRes);
        option.themeIcon.setImageResource(icon);
        option.themeOption.setContentDescription(getString(labelRes));
        option.themeOption.setOnClickListener(v -> select(mode, v));
    }

    private void bindSelection(boolean animate) {
        int mode = StorageService.getThemeMode();
        bindOption(binding.optionSystem, mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, animate);
        bindOption(binding.optionLight, mode == AppCompatDelegate.MODE_NIGHT_NO, animate);
        bindOption(binding.optionDark, mode == AppCompatDelegate.MODE_NIGHT_YES, animate);
    }

    private void bindOption(ViewThemeOptionBinding option, boolean selected, boolean animate) {
        boolean changed = option.themeOption.isSelected() != selected;
        option.themeOption.setSelected(selected);
        ViewCompat.setStateDescription(option.themeOption, selected ? getString(R.string.selected) : null);
        if (animate && changed && selected) {
            option.themeIcon.setScaleX(0.8f);
            option.themeIcon.setScaleY(0.8f);
            option.themeIcon.animate().scaleX(1f).scaleY(1f).setDuration(280)
                    .setInterpolator(new OvershootInterpolator(2f)).start();
        }
    }

    private void select(int mode, View view) {
        if (mode == StorageService.getThemeMode()) {
            return;
        }
        HapticUtils.tick(view);
        previousMode = shownMode;
        StorageService.setThemeMode(mode);
        bindSelection(true);
        showPreview(mode, true);
        getDelegate().setLocalNightMode(mode);
        handler.removeCallbacks(applyGlobally);
        handler.postDelayed(applyGlobally, GLOBAL_APPLY_DELAY);
    }

    private void applyGlobalNightMode() {
        handler.removeCallbacks(applyGlobally);
        int mode = StorageService.getThemeMode();
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode);
        }
    }

    @Override
    protected void onPause() {
        applyGlobalNightMode();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(applyGlobally);
        super.onDestroy();
    }
}
