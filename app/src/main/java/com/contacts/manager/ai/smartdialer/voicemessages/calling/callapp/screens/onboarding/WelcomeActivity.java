package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityWelcomeBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemWelcomeFeatureBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings.PrivacyPolicyActivity;

public class WelcomeActivity extends BaseActivity {

    private ActivityWelcomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        String appName = getString(R.string.app_name);
        String title = getString(R.string.welcome_title, appName);
        SpannableString titleSpan = new SpannableString(title);
        int start = title.indexOf(appName);
        if (start >= 0) {
            int end = start + appName.length();
            if (title.startsWith("!", end)) {
                end++;
            } else if (title.startsWith(" !", end)) {
                end += 2;
            }
            titleSpan.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary)),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        binding.welcomeTitle.setText(titleSpan);

        addFeature(R.drawable.ic_phone_in_talk, R.string.feature_call_access_title, R.string.feature_call_access_body, 0);
        addFeature(R.drawable.ic_block, R.string.feature_blocking_title, R.string.feature_blocking_body, 1);
        addFeature(R.drawable.ic_layers, R.string.feature_after_call_title, R.string.feature_after_call_body, 2);

        setupPrivacyText();
        binding.agreeButton.setOnClickListener(v -> next());
    }

    private void addFeature(int icon, int title, int body, int index) {
        ItemWelcomeFeatureBinding row = ItemWelcomeFeatureBinding.inflate(getLayoutInflater(), binding.featureList, false);
        row.featureIcon.setImageResource(icon);
        row.featureTitle.setText(title);
        row.featureBody.setText(body);
        binding.featureList.addView(row.getRoot());
        View view = row.getRoot();
        view.setAlpha(0f);
        view.setTranslationY(getResources().getDimension(R.dimen.space_16));
        view.animate().alpha(1f).translationY(0f).setStartDelay(120L + index * 70L).setDuration(320).start();
    }

    private void setupPrivacyText() {
        String link = getString(R.string.privacy_policy);
        String text = getString(R.string.welcome_terms, link);
        SpannableString span = new SpannableString(text);
        int start = text.indexOf(link);
        if (start >= 0) {
            span.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    startActivity(new Intent(WelcomeActivity.this, PrivacyPolicyActivity.class));
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setColor(ContextCompat.getColor(WelcomeActivity.this, R.color.primary));
                    ds.setUnderlineText(false);
                }
            }, start, start + link.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        binding.privacyText.setText(span);
        binding.privacyText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void next() {
        startActivity(new Intent(this, ThemeIntroActivity.class));
        finish();
    }
}
