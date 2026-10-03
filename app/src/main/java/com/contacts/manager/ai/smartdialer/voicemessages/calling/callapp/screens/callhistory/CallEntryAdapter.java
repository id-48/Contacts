package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.callhistory;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemCallEntryBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemSectionHeaderBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.CallModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.CallLogService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.DateUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CallEntryAdapter extends ListAdapter<CallEntryAdapter.Item, RecyclerView.ViewHolder> {

    public interface Listener {
        void onLongClick(CallModel call, View view);
    }

    public static class Item {
        final String header;
        final CallModel call;
        int position;

        Item(String header, CallModel call) {
            this.header = header;
            this.call = call;
        }
    }

    private static final int POSITION_SINGLE = 0;
    private static final int POSITION_TOP = 1;
    private static final int POSITION_MIDDLE = 2;
    private static final int POSITION_BOTTOM = 3;

    private static final DiffUtil.ItemCallback<Item> DIFF = new DiffUtil.ItemCallback<Item>() {
        @Override
        public boolean areItemsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            if (oldItem.call == null || newItem.call == null) {
                return Objects.equals(oldItem.header, newItem.header);
            }
            return oldItem.call.id == newItem.call.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.position == newItem.position && Objects.equals(oldItem.header, newItem.header);
        }
    };

    private final Listener listener;
    private boolean showNumber;

    public CallEntryAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void setShowNumber(boolean showNumber) {
        this.showNumber = showNumber;
    }

    public static List<Item> build(Context context, List<CallModel> calls) {
        List<Item> items = new ArrayList<>();
        String section = null;
        Item previous = null;
        for (CallModel call : calls) {
            String title = DateUtils.sectionTitle(context, call.date);
            boolean newSection = !title.equals(section);
            if (newSection) {
                if (previous != null) {
                    previous.position = previous.position == POSITION_TOP ? POSITION_SINGLE : POSITION_BOTTOM;
                }
                items.add(new Item(title, null));
                section = title;
            }
            Item item = new Item(null, call);
            item.position = newSection ? POSITION_TOP : POSITION_MIDDLE;
            items.add(item);
            previous = item;
        }
        if (previous != null) {
            previous.position = previous.position == POSITION_TOP ? POSITION_SINGLE : POSITION_BOTTOM;
        }
        return items;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).call == null ? 0 : 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == 0) {
            return new HeaderHolder(ItemSectionHeaderBinding.inflate(inflater, parent, false));
        }
        return new EntryHolder(ItemCallEntryBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = getItem(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).binding.sectionTitle.setText(item.header);
            return;
        }
        ItemCallEntryBinding binding = ((EntryHolder) holder).binding;
        bindEntry(binding, item.call, showNumber);
        int background;
        switch (item.position) {
            case POSITION_SINGLE:
                background = R.drawable.bg_card;
                break;
            case POSITION_TOP:
                background = R.drawable.bg_card_top;
                break;
            case POSITION_BOTTOM:
                background = R.drawable.bg_card_bottom;
                break;
            default:
                background = R.drawable.bg_card_middle;
                break;
        }
        binding.entryRoot.setBackgroundResource(background);
        binding.entryRoot.setOnLongClickListener(listener == null ? null : v -> {
            listener.onLongClick(item.call, v);
            return true;
        });
    }

    public static void bindEntry(ItemCallEntryBinding binding, CallModel call, boolean showNumber) {
        Context context = binding.getRoot().getContext();
        int color = ContextCompat.getColor(context, CallLogService.typeColor(call.type));
        binding.entryIcon.setImageResource(CallLogService.typeIcon(call.type));
        binding.entryIcon.setImageTintList(ColorStateList.valueOf(color));
        String label = context.getString(CallLogService.typeLabel(call.type));
        binding.entryTitle.setText(label);
        String time = DateUtils.isToday(call.date) ? DateUtils.rowTime(context, call.date)
                : DateUtils.clockTime(context, call.date);
        String subtitle = context.getString(R.string.call_type_time, time, DateUtils.duration(context, call.duration));
        binding.entrySubtitle.setText(subtitle);
        binding.entrySubtitle.setTextColor(call.isMissed() ? color
                : ContextCompat.getColor(context, R.color.text_secondary));
        if (showNumber && !PhoneUtils.isPrivate(call.number)) {
            binding.entryTrailing.setVisibility(View.VISIBLE);
            binding.entryTrailing.setText(PhoneUtils.format(context, call.number));
        } else {
            binding.entryTrailing.setVisibility(View.GONE);
        }
        binding.getRoot().setContentDescription(label + ", " + subtitle);
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final ItemSectionHeaderBinding binding;

        HeaderHolder(ItemSectionHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class EntryHolder extends RecyclerView.ViewHolder {
        final ItemCallEntryBinding binding;

        EntryHolder(ItemCallEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
