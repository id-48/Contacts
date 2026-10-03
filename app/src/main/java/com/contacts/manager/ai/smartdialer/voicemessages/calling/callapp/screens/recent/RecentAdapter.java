package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.recent;

import android.content.Context;
import android.content.res.ColorStateList;
import android.provider.CallLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.IncludeRowActionBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemRecentBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemRecentHeaderBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.CallGroupModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.CallModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.CallLogService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.DateUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.util.Objects;

public class RecentAdapter extends ListAdapter<RecentAdapter.Item, RecyclerView.ViewHolder> {

    public enum Action {
        ADD_CONTACT, MESSAGE, HISTORY, VIDEO, DETAILS
    }

    public interface Listener {
        void onCall(CallGroupModel group);

        void onAction(CallGroupModel group, Action action);

        void onLongClick(CallGroupModel group, View anchor);

        void onExpand(int position);
    }

    public static class Item {
        final String header;
        final CallGroupModel group;
        final long id;

        private Item(String header, CallGroupModel group) {
            this.header = header;
            this.group = group;
            this.id = group != null ? group.getStableId() : -1L - Math.abs((long) header.hashCode());
        }

        public static Item header(String title) {
            return new Item(title, null);
        }

        public static Item group(CallGroupModel group) {
            return new Item(null, group);
        }
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_GROUP = 1;
    private static final Object PAYLOAD_EXPAND = new Object();

    private static final DiffUtil.ItemCallback<Item> DIFF = new DiffUtil.ItemCallback<Item>() {
        @Override
        public boolean areItemsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            if (oldItem.group == null || newItem.group == null) {
                return Objects.equals(oldItem.header, newItem.header);
            }
            CallModel a = oldItem.group.getLatest();
            CallModel b = newItem.group.getLatest();
            return a.id == b.id && oldItem.group.getCount() == newItem.group.getCount()
                    && Objects.equals(a.name, b.name) && Objects.equals(a.photoUri, b.photoUri);
        }
    };

    private final Listener listener;
    private boolean videoSupported;
    private long expandedId = -1;

    public RecentAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
        setHasStableIds(true);
    }

    public void setVideoSupported(boolean videoSupported) {
        this.videoSupported = videoSupported;
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).group == null ? TYPE_HEADER : TYPE_GROUP;
    }

    public void toggleExpanded(int position) {
        if (position < 0 || position >= getItemCount()) {
            return;
        }
        long id = getItem(position).id;
        int previous = positionOf(expandedId);
        expandedId = expandedId == id ? -1 : id;
        if (previous >= 0) {
            notifyItemChanged(previous, PAYLOAD_EXPAND);
        }
        if (previous != position) {
            notifyItemChanged(position, PAYLOAD_EXPAND);
        }
    }

    public void collapse() {
        int previous = positionOf(expandedId);
        expandedId = -1;
        if (previous >= 0) {
            notifyItemChanged(previous, PAYLOAD_EXPAND);
        }
    }

    private int positionOf(long id) {
        if (id < 0) {
            return -1;
        }
        for (int i = 0; i < getItemCount(); i++) {
            if (getItem(i).id == id) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(ItemRecentHeaderBinding.inflate(inflater, parent, false));
        }
        return new GroupHolder(ItemRecentBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = getItem(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).binding.sectionTitle.setText(item.header);
        } else {
            ((GroupHolder) holder).bind(item);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position,
                                 @NonNull java.util.List<Object> payloads) {
        if (payloads.contains(PAYLOAD_EXPAND) && holder instanceof GroupHolder) {
            ((GroupHolder) holder).bindExpanded(getItem(position), true);
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    private static String relativeTime(Context context, long time) {
        long now = System.currentTimeMillis();
        long diff = now - time;
        if (diff >= 0 && diff < android.text.format.DateUtils.MINUTE_IN_MILLIS) {
            return context.getString(R.string.just_now);
        }
        if (diff >= 0 && diff < android.text.format.DateUtils.DAY_IN_MILLIS) {
            return android.text.format.DateUtils.getRelativeTimeSpanString(time, now,
                    android.text.format.DateUtils.MINUTE_IN_MILLIS).toString();
        }
        return DateUtils.clockTime(context, time);
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final ItemRecentHeaderBinding binding;

        HeaderHolder(ItemRecentHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    class GroupHolder extends RecyclerView.ViewHolder {
        final ItemRecentBinding binding;

        GroupHolder(ItemRecentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Item item) {
            Context context = itemView.getContext();
            CallGroupModel group = item.group;
            CallModel latest = group.getLatest();
            boolean isPrivate = PhoneUtils.isPrivate(latest.number);
            String name = isPrivate ? context.getString(R.string.private_number) : group.getDisplayName();
            if (!isPrivate && !latest.isSavedContact()) {
                name = PhoneUtils.format(context, latest.number);
            }
            String title = group.getCount() > 1 ? context.getString(R.string.name_with_count, name, group.getCount()) : name;
            binding.recentName.setText(title);
            binding.recentAvatar.bind(latest.isSavedContact() ? latest.name : null, latest.photoUri);

            int color = ContextCompat.getColor(context, CallLogService.typeColor(latest.type));
            binding.recentTypeIcon.setImageResource(CallLogService.typeArrowIcon(latest.type));
            binding.recentTypeIcon.setImageTintList(ColorStateList.valueOf(color));
            String subtitle = context.getString(R.string.recent_meta,
                    context.getString(CallLogService.typeLabelLong(latest.type)), relativeTime(context, latest.date));
            binding.recentSubtitle.setText(subtitle);
            binding.recentSubtitle.setTextColor(latest.isMissed() ? color
                    : ContextCompat.getColor(context, R.color.text_muted));

            binding.recentCall.setVisibility(isPrivate ? View.INVISIBLE : View.VISIBLE);
            binding.recentCall.setContentDescription(context.getString(R.string.call_name, name));
            binding.recentCall.setOnClickListener(v -> listener.onCall(group));
            binding.recentRow.setContentDescription(title + ", " + subtitle);
            binding.recentRow.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onExpand(position);
                }
            });
            binding.recentRow.setOnLongClickListener(v -> {
                listener.onLongClick(group, v);
                return true;
            });

            if (isPrivate) {
                bindAction(binding.actionFirst, group, Action.HISTORY);
                binding.actionSecond.actionRoot.setVisibility(View.INVISIBLE);
                binding.actionThird.actionRoot.setVisibility(View.INVISIBLE);
            } else {
                if (latest.isSavedContact()) {
                    bindAction(binding.actionFirst, group, videoSupported ? Action.VIDEO : Action.DETAILS);
                } else {
                    bindAction(binding.actionFirst, group, Action.ADD_CONTACT);
                }
                bindAction(binding.actionSecond, group, Action.MESSAGE);
                bindAction(binding.actionThird, group, Action.HISTORY);
            }
            bindExpanded(item, false);
        }

        void bindExpanded(Item item, boolean animate) {
            boolean expanded = item.id == expandedId;
            binding.recentCard.setBackgroundResource(expanded ? R.drawable.bg_row_expanded : 0);
            if (!animate) {
                binding.recentActions.setVisibility(expanded ? View.VISIBLE : View.GONE);
                binding.recentActions.setAlpha(1f);
                return;
            }
            if (expanded) {
                binding.recentActions.setVisibility(View.VISIBLE);
                binding.recentActions.setAlpha(0f);
                binding.recentActions.setTranslationY(-12f);
                binding.recentActions.animate().alpha(1f).translationY(0f).setDuration(180).start();
            } else {
                binding.recentActions.animate().cancel();
                binding.recentActions.setVisibility(View.GONE);
            }
        }

        private void bindAction(IncludeRowActionBinding action, CallGroupModel group, Action type) {
            Context context = itemView.getContext();
            action.actionRoot.setVisibility(View.VISIBLE);
            int icon;
            int label;
            switch (type) {
                case ADD_CONTACT:
                    icon = R.drawable.ic_person_add;
                    label = R.string.add_contact;
                    break;
                case MESSAGE:
                    icon = R.drawable.ic_chat;
                    label = R.string.messages;
                    break;
                case VIDEO:
                    icon = R.drawable.ic_videocam;
                    label = R.string.video_call;
                    break;
                case DETAILS:
                    icon = R.drawable.ic_info;
                    label = R.string.contact_info;
                    break;
                default:
                    icon = R.drawable.ic_history;
                    label = R.string.call_history;
                    break;
            }
            action.actionIcon.setImageResource(icon);
            action.actionLabel.setText(label);
            action.actionRoot.setContentDescription(context.getString(label));
            action.actionRoot.setOnClickListener(v -> listener.onAction(group, type));
        }
    }
}
