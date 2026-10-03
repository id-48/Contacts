package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.view.ViewCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityPrivacyPolicyBinding;

public class PrivacyPolicyActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPrivacyPolicyBinding binding = ActivityPrivacyPolicyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        binding.topBar.topTitle.setText(R.string.privacy_policy);
        setupBack(binding.topBar.backButton);

        String[] titles = getResources().getStringArray(R.array.privacy_titles);
        String[] bodies = getResources().getStringArray(R.array.privacy_bodies);
        int sectionGap = getResources().getDimensionPixelSize(R.dimen.space_24);
        int bodyGap = getResources().getDimensionPixelSize(R.dimen.space_8);
        for (int i = 0; i < titles.length && i < bodies.length; i++) {
            TextView title = new TextView(this);
            title.setTextAppearance(R.style.TextAppearance_App_TitleSmall);
            title.setText(titles[i]);
            ViewCompat.setAccessibilityHeading(title, true);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            titleParams.topMargin = sectionGap;
            binding.content.addView(title, titleParams);

            TextView body = new TextView(this);
            body.setTextAppearance(R.style.TextAppearance_App_Body);
            body.setTextColor(getColor(R.color.text_secondary));
            body.setText(bodies[i]);
            body.setLineSpacing(0, 1.15f);
            LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            bodyParams.topMargin = bodyGap;
            binding.content.addView(body, bodyParams);
        }
    }
}
