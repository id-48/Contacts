package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.AppConstants;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.IntentKeys;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityLanguageBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemLanguageBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.onboarding.WelcomeActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;

import java.util.List;
import java.util.Locale;

public class LanguageActivity extends BaseActivity {

    private static final String DEFAULT_TAG = "en";
    private static final Object PAYLOAD_SELECTION = new Object();

    private ActivityLanguageBinding binding;
    private boolean onboarding;
    private String selectedTag;
    private LanguageAdapter adapter;

    public static Intent intent(Context context, boolean onboarding) {
        return new Intent(context, LanguageActivity.class).putExtra(IntentKeys.EXTRA_ONBOARDING, onboarding);
    }

    public static String currentTag() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty() || locales.get(0) == null) {
            return "";
        }
        return supportedTag(locales.get(0).getLanguage());
    }

    public static String currentLabel(Context context) {
        String tag = currentTag();
        int index = indexOf(tag);
        return index >= 0 ? AppConstants.LANGUAGE_NATIVE_NAMES[index] : context.getString(R.string.language_system);
    }

    private static String deviceTag() {
        Locale locale = ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0);
        String tag = locale != null ? supportedTag(locale.getLanguage()) : "";
        return tag.isEmpty() ? DEFAULT_TAG : tag;
    }

    private static String supportedTag(String language) {
        return indexOf(language) >= 0 ? language : "";
    }

    private static int indexOf(String tag) {
        for (int i = 0; i < AppConstants.LANGUAGE_TAGS.length; i++) {
            if (AppConstants.LANGUAGE_TAGS[i].equals(tag)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLanguageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        onboarding = getIntent().getBooleanExtra(IntentKeys.EXTRA_ONBOARDING, false);
        String initial = currentTag().isEmpty() ? deviceTag() : currentTag();
        selectedTag = savedInstanceState != null ? savedInstanceState.getString("selected", initial) : initial;

        binding.backButton.setVisibility(onboarding ? View.GONE : View.VISIBLE);
        if (onboarding) {
            binding.languageTitle.setPaddingRelative(getResources().getDimensionPixelSize(R.dimen.space_12), 0, 0, 0);
        }
        setupBack(binding.backButton);
        binding.doneButton.setOnClickListener(v -> apply());
        binding.languageSubtitle.setVisibility(onboarding ? View.VISIBLE : View.GONE);

        binding.languageCard.setClipToOutline(true);
        adapter = new LanguageAdapter();
        binding.languageList.setLayoutManager(new LinearLayoutManager(this));
        binding.languageList.setAdapter(adapter);

        if (savedInstanceState == null) {
            animateIn();
        }
        int selected = indexOf(selectedTag);
        if (selected > 3) {
            binding.languageList.scrollToPosition(selected);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selected", selectedTag);
    }

    private void animateIn() {
        float offset = getResources().getDimension(R.dimen.space_24);
        DecelerateInterpolator interpolator = new DecelerateInterpolator(1.6f);

        binding.doneButton.setAlpha(0f);
        binding.doneButton.setScaleX(0.85f);
        binding.doneButton.setScaleY(0.85f);
        binding.doneButton.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(120).setDuration(260)
                .setInterpolator(interpolator).start();

        binding.languageCard.setAlpha(0f);
        binding.languageCard.setTranslationY(offset);
        binding.languageCard.animate().alpha(1f).translationY(0f).setStartDelay(60).setDuration(320)
                .setInterpolator(interpolator).start();

        AnimationSet row = new AnimationSet(true);
        row.addAnimation(new AlphaAnimation(0f, 1f));
        row.addAnimation(new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0.35f, Animation.RELATIVE_TO_SELF, 0f));
        row.setDuration(280);
        row.setInterpolator(interpolator);
        LayoutAnimationController controller = new LayoutAnimationController(row, 0.08f);
        binding.languageList.setLayoutAnimation(controller);
    }

    private void select(int position, View view) {
        String tag = AppConstants.LANGUAGE_TAGS[position];
        if (tag.equals(selectedTag)) {
            return;
        }
        HapticUtils.tick(view);
        int previous = indexOf(selectedTag);
        selectedTag = tag;
        if (previous >= 0) {
            adapter.notifyItemChanged(previous, PAYLOAD_SELECTION);
        }
        adapter.notifyItemChanged(position, PAYLOAD_SELECTION);
        binding.doneButton.animate().cancel();
        binding.doneButton.setScaleX(0.94f);
        binding.doneButton.setScaleY(0.94f);
        binding.doneButton.animate().scaleX(1f).scaleY(1f).setDuration(220)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    private void apply() {
        HapticUtils.confirm(binding.doneButton);
        if (!selectedTag.equals(currentTag())) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selectedTag));
        }
        if (onboarding) {
            startActivity(new Intent(this, WelcomeActivity.class));
        }
        finish();
    }

    private class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.Holder> {

        LanguageAdapter() {
            setHasStableIds(true);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemLanguageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position, @NonNull List<Object> payloads) {
            if (payloads.contains(PAYLOAD_SELECTION)) {
                holder.bindSelection(position, true);
                return;
            }
            super.onBindViewHolder(holder, position, payloads);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            String nativeName = AppConstants.LANGUAGE_NATIVE_NAMES[position];
            String englishName = AppConstants.LANGUAGE_ENGLISH_NAMES[position];
            holder.binding.languageNative.setText(getString(R.string.language_native_format, nativeName));
            holder.binding.languageEnglish.setText(englishName);
            holder.binding.languageDivider.setVisibility(position == getItemCount() - 1 ? View.GONE : View.VISIBLE);
            holder.itemView.setContentDescription(nativeName + ", " + englishName);
            holder.itemView.setOnClickListener(v -> {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    select(adapterPosition, v);
                }
            });
            holder.bindSelection(position, false);
        }

        @Override
        public int getItemCount() {
            return AppConstants.LANGUAGE_TAGS.length;
        }

        class Holder extends RecyclerView.ViewHolder {
            final ItemLanguageBinding binding;

            Holder(ItemLanguageBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bindSelection(int position, boolean animate) {
                boolean checked = AppConstants.LANGUAGE_TAGS[position].equals(selectedTag);
                binding.languageRadio.setChecked(checked);
                if (!animate) {
                    binding.languageRadio.jumpDrawablesToCurrentState();
                }
                ViewCompat.setStateDescription(itemView, checked ? getString(R.string.selected) : null);
            }
        }
    }
}
