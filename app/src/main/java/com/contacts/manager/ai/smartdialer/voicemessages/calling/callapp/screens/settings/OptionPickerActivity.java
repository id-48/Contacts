package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.view.ViewCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityOptionPickerBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemOptionCardBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels.ObservingViewModel;

import java.util.ArrayList;
import java.util.List;

public class OptionPickerActivity extends BaseActivity {

    public static final int MODE_ANSWER_POSITION = 0;
    public static final int MODE_SORT_ORDER = 1;
    public static final int MODE_NAME_FORMAT = 2;

    private static final String EXTRA_MODE = "option_mode";

    private ActivityOptionPickerBinding binding;
    private int mode;
    private final List<ItemOptionCardBinding> cards = new ArrayList<>();

    public static Intent intent(Context context, int mode) {
        return new Intent(context, OptionPickerActivity.class).putExtra(EXTRA_MODE, mode);
    }

    public static String label(Context context, int mode) {
        switch (mode) {
            case MODE_ANSWER_POSITION:
                return context.getString(StorageService.isAnswerOnLeft() ? R.string.answer_left : R.string.answer_right);
            case MODE_SORT_ORDER:
                return context.getString(StorageService.isSortByLastName() ? R.string.sort_last_name : R.string.sort_first_name);
            default:
                return context.getString(StorageService.isLastNameFirst() ? R.string.name_last_first : R.string.name_first_last);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOptionPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        mode = getIntent().getIntExtra(EXTRA_MODE, MODE_ANSWER_POSITION);
        setupBack(binding.topBar.backButton);

        switch (mode) {
            case MODE_ANSWER_POSITION:
                binding.topBar.topTitle.setText(R.string.answer_position);
                binding.optionHeader.setText(R.string.answer_position_header);
                addOption(R.drawable.ic_call, R.string.answer_right, R.string.answer_right_desc, false);
                addOption(R.drawable.ic_call, R.string.answer_left, R.string.answer_left_desc, true);
                binding.linkGroup.setVisibility(View.VISIBLE);
                binding.linkRow.setOnClickListener(v -> showFullscreen(AdScreens.SETTINGS_CALL_STYLE,
                        () -> startActivity(new Intent(this, CallStyleActivity.class))));
                setupBackAd(AdScreens.ANSWER_POSITION_BACK);
                showNativeBig(binding.ads.nativeBigContainer, AdScreens.ANSWER_POSITION);
                break;
            case MODE_SORT_ORDER:
                binding.topBar.topTitle.setText(R.string.contact_sort_order);
                binding.optionHeader.setText(R.string.sort_order_header);
                addOption(R.drawable.ic_sort, R.string.sort_first_name, R.string.sort_first_name_desc, false);
                addOption(R.drawable.ic_sort, R.string.sort_last_name, R.string.sort_last_name_desc, false);
                setupBackAd(AdScreens.SORT_ORDER_BACK);
                showNativeBig(binding.ads.nativeBigContainer, AdScreens.SORT_ORDER);
                break;
            default:
                binding.topBar.topTitle.setText(R.string.name_format);
                binding.optionHeader.setText(R.string.name_format_header);
                addOption(R.drawable.ic_person, R.string.name_first_last, R.string.name_first_last_desc, false);
                addOption(R.drawable.ic_person, R.string.name_last_first, R.string.name_last_first_desc, false);
                setupBackAd(AdScreens.NAME_FORMAT_BACK);
                showNativeBig(binding.ads.nativeBigContainer, AdScreens.NAME_FORMAT);
                break;
        }
        render();
    }

    private void addOption(@DrawableRes int icon, @StringRes int title, @StringRes int body, boolean answerLeft) {
        ItemOptionCardBinding card = ItemOptionCardBinding.inflate(getLayoutInflater(), binding.options, false);
        card.optionIcon.setImageResource(icon);
        card.optionTitle.setText(title);
        card.optionBody.setText(body);
        if (mode == MODE_ANSWER_POSITION) {
            card.optionIcon.setVisibility(View.GONE);
            card.optionPreview.setVisibility(View.VISIBLE);
            addPreviewCircle(card.optionPreview, !answerLeft ? R.drawable.bg_circle_decline : R.drawable.bg_circle_answer,
                    !answerLeft ? R.drawable.ic_call_end_filled : R.drawable.ic_call_filled);
            View spacer = new View(this);
            spacer.setLayoutParams(new LinearLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.space_40), 1));
            card.optionPreview.addView(spacer);
            addPreviewCircle(card.optionPreview, !answerLeft ? R.drawable.bg_circle_answer : R.drawable.bg_circle_decline,
                    !answerLeft ? R.drawable.ic_call_filled : R.drawable.ic_call_end_filled);
        }
        int index = cards.size();
        card.optionCard.setOnClickListener(v -> select(index, v));
        cards.add(card);
        binding.options.addView(card.getRoot());
    }

    private void addPreviewCircle(LinearLayout parent, @DrawableRes int background, @DrawableRes int icon) {
        ImageView circle = new ImageView(this);
        int size = getResources().getDimensionPixelSize(R.dimen.space_32);
        circle.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        circle.setBackgroundResource(background);
        circle.setImageResource(icon);
        int padding = getResources().getDimensionPixelSize(R.dimen.space_8);
        circle.setPadding(padding, padding, padding, padding);
        circle.setImageTintList(getColorStateList(R.color.white));
        circle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        parent.addView(circle);
    }

    private int selectedIndex() {
        switch (mode) {
            case MODE_ANSWER_POSITION:
                return StorageService.isAnswerOnLeft() ? 1 : 0;
            case MODE_SORT_ORDER:
                return StorageService.isSortByLastName() ? 1 : 0;
            default:
                return StorageService.isLastNameFirst() ? 1 : 0;
        }
    }

    private void select(int index, View view) {
        if (index == selectedIndex()) {
            return;
        }
        HapticUtils.tick(view);
        boolean second = index == 1;
        switch (mode) {
            case MODE_ANSWER_POSITION:
                StorageService.setAnswerOnLeft(second);
                break;
            case MODE_SORT_ORDER:
                StorageService.setSortByLastName(second);
                ObservingViewModel.invalidateAll();
                break;
            default:
                StorageService.setLastNameFirst(second);
                ObservingViewModel.invalidateAll();
                break;
        }
        render();
    }

    private void render() {
        int selected = selectedIndex();
        for (int i = 0; i < cards.size(); i++) {
            ItemOptionCardBinding card = cards.get(i);
            boolean checked = i == selected;
            card.optionCard.setSelected(checked);
            card.optionCheck.setSelected(checked);
            card.optionCheck.setImageResource(checked ? R.drawable.ic_check : 0);
            ViewCompat.setStateDescription(card.optionCard, checked ? getString(R.string.selected) : null);
        }
    }
}
