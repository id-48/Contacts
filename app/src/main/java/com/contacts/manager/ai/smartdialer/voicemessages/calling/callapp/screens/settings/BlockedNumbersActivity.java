package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.ConfirmDialog;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BlockNumberDialog;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityBlockedNumbersBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemBlockedNumberBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.BlockedNumberModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.BlockService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.util.List;
import java.util.Objects;

public class BlockedNumbersActivity extends BaseActivity {

    private ActivityBlockedNumbersBinding binding;
    private final BlockedAdapter adapter = new BlockedAdapter();
    private boolean pendingBlockUnknown;
    private String pendingNumber;

    private final ActivityResultLauncher<Intent> roleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (pendingBlockUnknown && (PhoneService.isDefaultDialer(this) || PhoneService.isScreeningRoleHeld(this))) {
                    StorageService.setBlockUnknownEnabled(true);
                }
                pendingBlockUnknown = false;
                String number = pendingNumber;
                pendingNumber = null;
                refresh();
                if (number != null && BlockService.canBlock(this)) {
                    addNumber(number);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBlockedNumbersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        setupBack(binding.backButton);
        setupBackAd(AdScreens.BLOCKED_NUMBERS_BACK);
        AdsManager.showNativeSmall(this, binding.nativeSmallContainer, AdScreens.BLOCKED_NUMBERS);
        if (savedInstanceState != null) {
            pendingNumber = savedInstanceState.getString("pending_number");
        }

        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);
        binding.listCard.setClipToOutline(true);
        binding.emptyState.setContent(R.drawable.ic_block, R.string.empty_blocked_title, R.string.empty_blocked_body);

        binding.blockUnknownRow.setOnCheckedListener(this::onBlockUnknown);
        binding.addButton.setOnClickListener(v -> showFullscreen(AdScreens.BLOCKED_NUMBERS_ADD,
                () -> BlockNumberDialog.show(this,
                        number -> showFullscreen(AdScreens.BLOCKED_NUMBERS_BLOCK, () -> onNumberEntered(number)))));

        binding.banner.bannerIcon.setImageResource(R.drawable.ic_phone_in_talk);
        binding.banner.bannerTitle.setText(R.string.blocking_requires_default_title);
        binding.banner.bannerBody.setText(R.string.blocking_requires_default_body);
        binding.banner.bannerAction.setText(R.string.set_as_default);
        binding.banner.bannerClose.setVisibility(View.GONE);
        binding.banner.getRoot().setOnClickListener(v -> requestDefault(false));

        if (savedInstanceState == null) {
            animateIn();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("pending_number", pendingNumber);
    }

    private void animateIn() {
        float offset = getResources().getDimension(R.dimen.space_24);
        DecelerateInterpolator interpolator = new DecelerateInterpolator(1.6f);
        View[] views = {binding.headerBlock, binding.listCard};
        for (int i = 0; i < views.length; i++) {
            View view = views[i];
            view.setAlpha(0f);
            view.setTranslationY(offset);
            view.animate().alpha(1f).translationY(0f).setStartDelay(40 + i * 90L).setDuration(320)
                    .setInterpolator(interpolator).start();
        }
    }

    private void onNumberEntered(String number) {
        if (BlockService.canBlock(this)) {
            addNumber(number);
            return;
        }
        pendingNumber = number;
        requestDefault(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean canBlock = BlockService.canBlock(this);
        binding.banner.getRoot().setVisibility(canBlock ? View.GONE : View.VISIBLE);
        boolean screening = PhoneService.isDefaultDialer(this) || PhoneService.isScreeningRoleHeld(this);
        binding.blockUnknownRow.setChecked(StorageService.isBlockUnknownEnabled() && screening);
        load();
    }

    private void load() {
        Context app = getApplicationContext();
        if (adapter.getItemCount() == 0) {
            binding.progress.setVisibility(View.VISIBLE);
        }
        AppExecutors.io(() -> {
            List<BlockedNumberModel> numbers = BlockService.getBlockedNumbers(app);
            AppExecutors.main(() -> {
                if (isDestroyed()) {
                    return;
                }
                binding.progress.setVisibility(View.GONE);
                adapter.submitList(numbers);
                boolean empty = numbers.isEmpty();
                binding.emptyState.show(empty);
                binding.listCard.setVisibility(empty ? View.GONE : View.VISIBLE);
                binding.listHeader.setVisibility(empty ? View.GONE : View.VISIBLE);
                binding.listCount.setText(String.valueOf(numbers.size()));
            });
        });
    }

    private void onBlockUnknown(boolean enabled) {
        if (!enabled) {
            StorageService.setBlockUnknownEnabled(false);
            return;
        }
        if (PhoneService.isDefaultDialer(this) || PhoneService.isScreeningRoleHeld(this)) {
            StorageService.setBlockUnknownEnabled(true);
            return;
        }
        binding.blockUnknownRow.setChecked(false);
        requestDefault(true);
    }

    private void requestDefault(boolean forBlockUnknown) {
        Intent intent = PhoneService.isScreeningRoleAvailable(this) && forBlockUnknown
                ? PhoneService.createScreeningRoleIntent(this)
                : PhoneService.createDefaultDialerIntent(this);
        if (intent == null) {
            Toast.makeText(this, R.string.default_phone_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        pendingBlockUnknown = forBlockUnknown;
        try {
            roleLauncher.launch(intent);
        } catch (Exception e) {
            pendingBlockUnknown = false;
            pendingNumber = null;
            Toast.makeText(this, R.string.default_phone_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void addNumber(String value) {
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            boolean ok = BlockService.block(app, value);
            AppExecutors.main(() -> {
                Toast.makeText(app, ok ? R.string.number_blocked : R.string.block_failed, Toast.LENGTH_SHORT).show();
                if (!isDestroyed() && ok) {
                    load();
                }
            });
        });
    }

    private void confirmUnblock(BlockedNumberModel model) {
        ConfirmDialog.show(this, getString(R.string.unblock_number_title), getString(R.string.unblock_number_body),
                R.string.unblock, () -> {
                    Context app = getApplicationContext();
                    AppExecutors.io(() -> {
                        boolean ok = BlockService.unblock(app, model.number);
                        AppExecutors.main(() -> {
                            Toast.makeText(app, ok ? R.string.number_unblocked : R.string.unblock_failed,
                                    Toast.LENGTH_SHORT).show();
                            if (!isDestroyed() && ok) {
                                load();
                            }
                        });
                    });
                });
    }

    private class BlockedAdapter extends ListAdapter<BlockedNumberModel, BlockedAdapter.Holder> {

        BlockedAdapter() {
            super(new DiffUtil.ItemCallback<BlockedNumberModel>() {
                @Override
                public boolean areItemsTheSame(@NonNull BlockedNumberModel a, @NonNull BlockedNumberModel b) {
                    return a.id == b.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull BlockedNumberModel a, @NonNull BlockedNumberModel b) {
                    return Objects.equals(a.number, b.number) && Objects.equals(a.name, b.name);
                }
            });
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemBlockedNumberBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            BlockedNumberModel model = getItem(position);
            Context context = holder.itemView.getContext();
            String formatted = PhoneUtils.format(context, model.number);
            boolean named = !TextUtils.isEmpty(model.name);
            String title = named ? model.name : formatted;
            holder.binding.blockedAvatar.bindBlocked();
            holder.binding.blockedName.setText(title);
            holder.binding.blockedSubtitle.setText(named ? formatted : context.getString(R.string.call_blocked));
            holder.binding.unblockButton.setContentDescription(context.getString(R.string.unblock_named, title));
            holder.binding.unblockButton.setOnClickListener(v -> confirmUnblock(model));
        }

        class Holder extends RecyclerView.ViewHolder {
            final ItemBlockedNumberBinding binding;

            Holder(ItemBlockedNumberBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
