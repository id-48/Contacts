package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.ConfirmDialog;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.QuickResponseDialog;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityQuickResponseBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemQuickResponseBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;

import java.util.ArrayList;
import java.util.List;

public class QuickResponseActivity extends BaseActivity {

    private static final int MAX_CUSTOM_RESPONSES = 10;

    private ActivityQuickResponseBinding binding;
    private final List<String> defaults = new ArrayList<>();
    private final List<String> customResponses = new ArrayList<>();
    private final ResponseAdapter adapter = new ResponseAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuickResponseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        setupBack(binding.backButton);
        setupBackAd(AdScreens.QUICK_RESPONSE_BACK);
        AdsManager.showNativeSmall(this, binding.nativeSmallContainer, AdScreens.QUICK_RESPONSE);

        defaults.addAll(StorageService.getDefaultQuickResponses(this));
        customResponses.addAll(StorageService.getCustomQuickResponses(this));
        bindDefaults();

        binding.customCard.setClipToOutline(true);
        binding.customList.setLayoutManager(new LinearLayoutManager(this));
        binding.customList.setAdapter(adapter);
        binding.addButton.setOnClickListener(v -> add());
        updateCustomSection(false);

        if (savedInstanceState == null) {
            animateIn();
        }
    }

    private void bindDefaults() {
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < defaults.size(); i++) {
            ItemQuickResponseBinding row = ItemQuickResponseBinding.inflate(inflater, binding.defaultCard, false);
            row.responseText.setText(defaults.get(i));
            row.responseDelete.setVisibility(View.GONE);
            row.responseDivider.setVisibility(i == defaults.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            binding.defaultCard.addView(row.getRoot());
        }
    }

    private void animateIn() {
        float offset = getResources().getDimension(R.dimen.space_24);
        DecelerateInterpolator interpolator = new DecelerateInterpolator(1.6f);
        View[] views = {binding.subtitle, binding.defaultCard, binding.addButton, binding.customEmptyHint,
                binding.customHeader, binding.customCard};
        for (int i = 0; i < views.length; i++) {
            View view = views[i];
            view.setAlpha(0f);
            view.setTranslationY(offset);
            view.animate().alpha(1f).translationY(0f).setStartDelay(40 + i * 70L).setDuration(320)
                    .setInterpolator(interpolator).start();
        }
    }

    private void add() {
        if (customResponses.size() >= MAX_CUSTOM_RESPONSES) {
            HapticUtils.longPress(binding.addButton);
            Toast.makeText(this, getString(R.string.quick_response_limit, MAX_CUSTOM_RESPONSES), Toast.LENGTH_SHORT).show();
            return;
        }
        QuickResponseDialog.show(this, R.string.add_response, null, R.string.add,
                value -> isDuplicate(value, -1), value -> showFullscreen(AdScreens.QUICK_RESPONSE_ADD, () -> {
                    customResponses.add(value);
                    persist();
                    int position = customResponses.size() - 1;
                    adapter.notifyItemInserted(position);
                    if (position > 0) {
                        adapter.notifyItemChanged(position - 1);
                    }
                    updateCustomSection(true);
                    binding.scroll.post(() -> binding.scroll.smoothScrollTo(0, binding.content.getHeight()));
                }));
    }

    private void edit(int position) {
        if (position < 0 || position >= customResponses.size()) {
            return;
        }
        QuickResponseDialog.show(this, R.string.edit_response, customResponses.get(position), R.string.save,
                value -> isDuplicate(value, position), value -> {
                    if (position >= customResponses.size()) {
                        return;
                    }
                    customResponses.set(position, value);
                    persist();
                    adapter.notifyItemChanged(position);
                });
    }

    private void delete(int position) {
        if (position < 0 || position >= customResponses.size()) {
            return;
        }
        String message = "\u201C" + customResponses.get(position) + "\u201D";
        ConfirmDialog.show(this, getString(R.string.delete_response_title), message, R.string.delete, true, () -> {
            if (position >= customResponses.size()) {
                return;
            }
            customResponses.remove(position);
            persist();
            adapter.notifyItemRemoved(position);
            if (position > 0 && position == customResponses.size()) {
                adapter.notifyItemChanged(position - 1);
            }
            updateCustomSection(true);
        });
    }

    private boolean isDuplicate(String value, int ignorePosition) {
        for (String response : defaults) {
            if (response.trim().equalsIgnoreCase(value)) {
                return true;
            }
        }
        for (int i = 0; i < customResponses.size(); i++) {
            if (i != ignorePosition && customResponses.get(i).trim().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private void persist() {
        StorageService.setCustomQuickResponses(new ArrayList<>(customResponses));
    }

    private void updateCustomSection(boolean animate) {
        boolean empty = customResponses.isEmpty();
        binding.customCount.setText(String.valueOf(customResponses.size()));
        boolean wasEmpty = binding.customCard.getVisibility() != View.VISIBLE;
        binding.customEmptyHint.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.customHeader.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.customCard.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (animate && wasEmpty != empty) {
            View[] views = empty
                    ? new View[]{binding.customEmptyHint}
                    : new View[]{binding.customHeader, binding.customCard};
            float offset = getResources().getDimension(R.dimen.space_12);
            for (int i = 0; i < views.length; i++) {
                View view = views[i];
                view.setAlpha(0f);
                view.setTranslationY(offset);
                view.animate().alpha(1f).translationY(0f).setStartDelay(i * 60L).setDuration(260)
                        .setInterpolator(new DecelerateInterpolator(1.6f)).start();
            }
        }
    }

    private class ResponseAdapter extends RecyclerView.Adapter<ResponseAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemQuickResponseBinding row = ItemQuickResponseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            row.responseRow.setBackgroundResource(R.drawable.bg_row);
            row.responseRow.setPaddingRelative(row.responseRow.getPaddingStart(), 0, getResources().getDimensionPixelSize(R.dimen.space_8), 0);
            row.responseDelete.setVisibility(View.VISIBLE);
            return new Holder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            String text = customResponses.get(position);
            holder.binding.responseText.setText(text);
            holder.binding.responseDivider.setVisibility(position == customResponses.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            holder.binding.responseDelete.setContentDescription(getString(R.string.delete_named, text));
            holder.binding.responseRow.setOnClickListener(v -> edit(holder.getBindingAdapterPosition()));
            holder.binding.responseDelete.setOnClickListener(v -> {
                HapticUtils.tap(v);
                delete(holder.getBindingAdapterPosition());
            });
        }

        @Override
        public int getItemCount() {
            return customResponses.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ItemQuickResponseBinding binding;

            Holder(ItemQuickResponseBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
