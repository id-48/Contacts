package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.onboarding;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.ThemePreview;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityThemeIntroBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ViewThemePreviewBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.OnboardingFlow;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.RemoteConfig;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
import com.google.android.material.button.MaterialButton;

public class ThemeIntroActivity extends BaseActivity {

    private static final int PAGE_LIGHT = 0;
    private static final int PAGE_DARK = 1;
    private static final float SIDE_PAGE_SCALE = 0.9f;
    private static final float MAX_PREVIEW_SCALE = 1.12f;

    private ActivityThemeIntroBinding binding;
    private Context lightContext;
    private Context darkContext;
    private WindowInsetsControllerCompat insetsController;
    private final ArgbEvaluator evaluator = new ArgbEvaluator();
    private float previewScale = 1f;
    private int pagerSidePadding;
    private int sidePeek;
    private boolean leaving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityThemeIntroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        lightContext = themedContext(false);
        darkContext = themedContext(true);
        insetsController = new WindowInsetsControllerCompat(getWindow(), binding.root);

        binding.topBar.topTitle.setText(R.string.theme);
        binding.topBar.backButton.setImageResource(R.drawable.ic_chevron_left);
        binding.topBar.backButton.setOnClickListener(v -> finishIntro());
        binding.doneButton.setOnClickListener(v -> onDone());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishIntro();
            }
        });

        setupPager();

        int startPage = isDarkSelected() ? PAGE_DARK : PAGE_LIGHT;
        if (savedInstanceState != null) {
            startPage = savedInstanceState.getInt("page", startPage);
        }
        binding.themePager.setCurrentItem(startPage, false);
        applyFraction(startPage);
        bindDots(startPage, false);

        ViewGroup.OnHierarchyChangeListener adReady = new ViewGroup.OnHierarchyChangeListener() {
            @Override
            public void onChildViewAdded(View parent, View child) {
                tintThemeAds(binding.themePager.getCurrentItem());
            }

            @Override
            public void onChildViewRemoved(View parent, View child) {
            }
        };
        binding.nativeBigContainer.setOnHierarchyChangeListener(adReady);
        binding.nativeSmallContainer.setOnHierarchyChangeListener(adReady);
        AdsManager.showNativeBig(this, binding.nativeBigContainer, RemoteConfig.SCREEN_THEME_SELECTION);
        AdsManager.showNativeThemeSmall(this, binding.nativeSmallContainer, RemoteConfig.SCREEN_THEME_SELECTION);
        AdsManager.showBanner(this, binding.bannerContainer, RemoteConfig.SCREEN_THEME_SELECTION);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("page", binding.themePager.getCurrentItem());
    }

    private Context themedContext(boolean dark) {
        return ThemePreview.themedContext(this, dark);
    }

    private boolean isDarkSelected() {
        int mode = StorageService.getThemeMode();
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            return true;
        }
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            return false;
        }
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    private void setupPager() {
        pagerSidePadding = getResources().getDimensionPixelSize(R.dimen.space_40) + getResources().getDimensionPixelSize(R.dimen.space_16);
        sidePeek = getResources().getDimensionPixelSize(R.dimen.space_24);

        ViewPager2 pager = binding.themePager;
        pager.setAdapter(new PreviewAdapter());
        pager.setOffscreenPageLimit(1);
        RecyclerView recycler = (RecyclerView) pager.getChildAt(0);
        recycler.setPadding(pagerSidePadding, 0, pagerSidePadding, 0);
        recycler.setClipToPadding(false);
        recycler.setClipChildren(false);
        recycler.setOverScrollMode(View.OVER_SCROLL_NEVER);

        float phoneWidth = getResources().getDisplayMetrics().density * 216f;
        pager.setPageTransformer((page, position) -> {
            float clamped = Math.max(-1f, Math.min(1f, position));
            float scale = 1f - (1f - SIDE_PAGE_SCALE) * Math.abs(clamped);
            page.setScaleX(scale);
            page.setScaleY(scale);
            float sideShift = pagerSidePadding - sidePeek - page.getWidth() / 2f
                    + phoneWidth * previewScale * SIDE_PAGE_SCALE / 2f;
            page.setTranslationX(clamped * sideShift);
        });

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                applyFraction(position + positionOffset);
            }

            @Override
            public void onPageSelected(int position) {
                bindDots(position, true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_SETTLING) {
                    HapticUtils.tick(binding.themePager);
                }
            }
        });
    }

    private void applyFraction(float fraction) {
        float f = Math.max(0f, Math.min(1f, fraction));
        int background = blend(f, R.color.background);
        int text = blend(f, R.color.text_primary);
        binding.root.setBackgroundColor(background);
        binding.topBar.topTitle.setTextColor(text);
        binding.themeSubtitle.setTextColor(text);
        ImageViewCompat.setImageTintList(binding.topBar.backButton, ColorStateList.valueOf(text));

        boolean dark = f >= 0.5f;
        binding.themeLabel.setText(dark ? R.string.theme_dark_short : R.string.theme_light_short);
        float labelProgress = Math.abs(f - 0.5f) * 2f;
        binding.themeLabel.setAlpha(labelProgress);
        float labelScale = 0.92f + 0.08f * labelProgress;
        binding.themeLabel.setScaleX(labelScale);
        binding.themeLabel.setScaleY(labelScale);

        insetsController.setAppearanceLightStatusBars(!dark);
        insetsController.setAppearanceLightNavigationBars(!dark);
        tintThemeAds(f);
    }

    private void tintThemeAds(float fraction) {
        int background = blend(fraction, R.color.background);
        int card = blend(fraction, R.color.card_strong);
        int text = blend(fraction, R.color.text_primary);
        int muted = blend(fraction, R.color.text_muted);
        int primary = blend(fraction, R.color.primary);
        tintAdTree(binding.nativeSmallContainer, background, text, muted, primary);
        tintAdTree(binding.nativeBigContainer, card, text, muted, primary);
    }

    private void tintAdTree(ViewGroup root, int surface, int text, int muted, int primary) {
        if (root.getVisibility() != View.VISIBLE) {
            return;
        }
        walkViews(root, view -> {
            Object tag = view.getTag();
            if ("theme_ad_surface".equals(tag)) {
                setSurfaceColor(view, surface);
            } else if ("theme_ad_cta".equals(tag)) {
                setCtaColor(view, primary);
            } else if (view instanceof TextView) {
                int id = view.getId();
                if (id == R.id.ad_headline || id == R.id.tp_native_title) {
                    ((TextView) view).setTextColor(text);
                } else if (id == R.id.ad_body || id == R.id.tp_native_text
                        || id == R.id.tp_native_advertiser) {
                    ((TextView) view).setTextColor(muted);
                }
            }
        });
    }

    private void setSurfaceColor(View view, int color) {
        Drawable background = view.getBackground();
        if (background instanceof GradientDrawable) {
            ((GradientDrawable) background.mutate()).setColor(color);
            return;
        }
        if (background != null) {
            Drawable mutated = background.mutate();
            if (mutated instanceof GradientDrawable) {
                ((GradientDrawable) mutated).setColor(color);
                return;
            }
        }
        view.setBackgroundColor(color);
    }

    private void setCtaColor(View view, int color) {
        if (view instanceof MaterialButton) {
            ((MaterialButton) view).setBackgroundTintList(ColorStateList.valueOf(color));
            return;
        }
        Drawable background = view.getBackground();
        if (background != null) {
            background.mutate().setTint(color);
        }
    }

    private void walkViews(View view, ViewVisitor visitor) {
        visitor.visit(view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                walkViews(group.getChildAt(i), visitor);
            }
        }
    }

    private interface ViewVisitor {
        void visit(View view);
    }

    private int blend(float fraction, @ColorRes int color) {
        return (int) evaluator.evaluate(fraction,
                ContextCompat.getColor(lightContext, color), ContextCompat.getColor(darkContext, color));
    }

    private void bindDots(int page, boolean animate) {
        bindDot(binding.dotLight, page == PAGE_LIGHT, animate);
        bindDot(binding.dotDark, page == PAGE_DARK, animate);
    }

    private void bindDot(View dot, boolean active, boolean animate) {
        dot.setBackgroundResource(active ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
        if (animate && active) {
            dot.setScaleX(0.6f);
            dot.setScaleY(0.6f);
            dot.animate().scaleX(1f).scaleY(1f).setDuration(220).start();
        }
    }

    private void onDone() {
        if (leaving) {
            return;
        }
        leaving = true;
        AdsManager.showFullscreen(this, RemoteConfig.SCREEN_THEME_SELECTION, this::leave);
    }

    private void finishIntro() {
        if (leaving) {
            return;
        }
        leaving = true;
        leave();
    }

    private void leave() {
        int mode = binding.themePager.getCurrentItem() == PAGE_DARK
                ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        StorageService.setThemeMode(mode);
        OnboardingFlow.continueFrom(this, OnboardingFlow.Step.THEME);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    private void fitPreview(View phone, View page) {
        if (phone.getHeight() == 0 || page.getHeight() == 0) {
            return;
        }
        float byHeight = page.getHeight() / (float) phone.getHeight();
        float byWidth = (page.getWidth() + 2f * pagerSidePadding) * 0.6f / phone.getWidth();
        previewScale = Math.min(MAX_PREVIEW_SCALE, Math.min(byHeight, byWidth));
        phone.setScaleX(previewScale);
        phone.setScaleY(previewScale);
        binding.themePager.requestTransform();
    }

    private class PreviewAdapter extends RecyclerView.Adapter<PreviewAdapter.Holder> {

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = viewType == PAGE_DARK ? darkContext : lightContext;
            FrameLayout page = new FrameLayout(parent.getContext());
            page.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            page.setClipChildren(false);
            page.setClipToPadding(false);

            ViewThemePreviewBinding preview = ThemePreview.inflate(ThemeIntroActivity.this, context, page);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) preview.getRoot().getLayoutParams();
            params.gravity = Gravity.CENTER;
            page.addView(preview.getRoot(), params);

            page.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                if (r - l != or - ol || b - t != ob - ot) {
                    v.post(() -> fitPreview(preview.getRoot(), v));
                }
            });
            page.setContentDescription(getString(viewType == PAGE_DARK
                    ? R.string.theme_dark_short : R.string.theme_light_short));
            page.setOnClickListener(v -> binding.themePager.setCurrentItem(viewType, true));
            return new Holder(page);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        class Holder extends RecyclerView.ViewHolder {
            Holder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
