package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.contacts;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.IncludeRowActionBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemContactBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemContactCreateBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemContactFooterBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemRecentHeaderBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.util.List;
import java.util.Objects;

public class ContactsAdapter extends ListAdapter<ContactsAdapter.Item, RecyclerView.ViewHolder> {

    public enum Action {
        MESSAGE, VIDEO, EDIT, DETAILS
    }

    public interface Listener {
        void onCreateContact();

        void onCall(ContactModel contact);

        void onAction(ContactModel contact, Action action);

        void onLongClick(ContactModel contact, View anchor);

        void onExpand(int position);
    }

    private static final int TYPE_CREATE = 0;
    private static final int TYPE_HEADER = 1;
    private static final int TYPE_CONTACT = 2;
    private static final int TYPE_FOOTER = 3;
    private static final Object PAYLOAD_EXPAND = new Object();

    public static class Item {
        final int type;
        final String text;
        final ContactModel contact;
        final long id;

        private Item(int type, String text, ContactModel contact, long id) {
            this.type = type;
            this.text = text;
            this.contact = contact;
            this.id = id;
        }

        public static Item create() {
            return new Item(TYPE_CREATE, null, null, -1L);
        }

        public static Item header(String letter) {
            return new Item(TYPE_HEADER, letter, null, -10L - Math.abs((long) letter.hashCode()));
        }

        public static Item contact(ContactModel contact) {
            return new Item(TYPE_CONTACT, null, contact, contact.id);
        }

        public static Item footer(String text) {
            return new Item(TYPE_FOOTER, text, null, -2L);
        }
    }

    private static final DiffUtil.ItemCallback<Item> DIFF = new DiffUtil.ItemCallback<Item>() {
        @Override
        public boolean areItemsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.type == newItem.type && oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            if (oldItem.contact == null || newItem.contact == null) {
                return Objects.equals(oldItem.text, newItem.text);
            }
            ContactModel a = oldItem.contact;
            ContactModel b = newItem.contact;
            return Objects.equals(a.name, b.name) && Objects.equals(a.photoUri, b.photoUri)
                    && Objects.equals(a.primaryNumber, b.primaryNumber) && a.starred == b.starred;
        }
    };

    private final Listener listener;
    private boolean videoSupported;
    private long expandedId = Long.MIN_VALUE;

    public ContactsAdapter(Listener listener) {
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
        return getItem(position).type;
    }

    public void toggleExpanded(int position) {
        if (position < 0 || position >= getItemCount() || getItem(position).type != TYPE_CONTACT) {
            return;
        }
        long id = getItem(position).id;
        int previous = positionOf(expandedId);
        expandedId = expandedId == id ? Long.MIN_VALUE : id;
        if (previous >= 0) {
            notifyItemChanged(previous, PAYLOAD_EXPAND);
        }
        if (previous != position) {
            notifyItemChanged(position, PAYLOAD_EXPAND);
        }
    }

    public void collapse() {
        int previous = positionOf(expandedId);
        expandedId = Long.MIN_VALUE;
        if (previous >= 0) {
            notifyItemChanged(previous, PAYLOAD_EXPAND);
        }
    }

    private int positionOf(long id) {
        if (id == Long.MIN_VALUE) {
            return -1;
        }
        for (int i = 0; i < getItemCount(); i++) {
            Item item = getItem(i);
            if (item.type == TYPE_CONTACT && item.id == id) {
                return i;
            }
        }
        return -1;
    }

    public int findHeaderPosition(String letter) {
        for (int i = 0; i < getItemCount(); i++) {
            Item item = getItem(i);
            if (item.type == TYPE_HEADER && item.text.equals(letter)) {
                return i;
            }
        }
        return -1;
    }

    public String sectionAt(int position) {
        for (int i = Math.min(position, getItemCount() - 1); i >= 0; i--) {
            Item item = getItem(i);
            if (item.type == TYPE_HEADER) {
                return item.text;
            }
        }
        return null;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_CREATE:
                ItemContactCreateBinding create = ItemContactCreateBinding.inflate(inflater, parent, false);
                create.createRow.setOnClickListener(v -> listener.onCreateContact());
                return new SimpleHolder(create.getRoot());
            case TYPE_HEADER:
                return new HeaderHolder(ItemRecentHeaderBinding.inflate(inflater, parent, false));
            case TYPE_FOOTER:
                return new FooterHolder(ItemContactFooterBinding.inflate(inflater, parent, false));
            default:
                return new ContactHolder(ItemContactBinding.inflate(inflater, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = getItem(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).binding.sectionTitle.setText(item.text);
        } else if (holder instanceof FooterHolder) {
            ((FooterHolder) holder).binding.footerText.setText(item.text);
        } else if (holder instanceof ContactHolder) {
            ((ContactHolder) holder).bind(item);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains(PAYLOAD_EXPAND) && holder instanceof ContactHolder) {
            ((ContactHolder) holder).bindExpanded(getItem(position), true);
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    private static boolean hasLetter(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetter(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    static class SimpleHolder extends RecyclerView.ViewHolder {
        SimpleHolder(View view) {
            super(view);
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final ItemRecentHeaderBinding binding;

        HeaderHolder(ItemRecentHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class FooterHolder extends RecyclerView.ViewHolder {
        final ItemContactFooterBinding binding;

        FooterHolder(ItemContactFooterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    class ContactHolder extends RecyclerView.ViewHolder {
        final ItemContactBinding binding;

        ContactHolder(ItemContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Item item) {
            Context context = itemView.getContext();
            ContactModel contact = item.contact;
            boolean hasNumber = PhoneUtils.isValidNumber(contact.primaryNumber);
            boolean named = hasLetter(contact.name);
            String formatted = hasNumber ? PhoneUtils.format(context, contact.primaryNumber) : null;
            String name = named || formatted == null ? contact.getDisplayName() : formatted;

            binding.contactName.setText(name);
            binding.contactName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0,
                    contact.starred ? R.drawable.ic_star_badge : 0, 0);
            binding.contactAvatar.bind(contact.name, contact.photoUri);

            String subtitle = named ? formatted : null;
            binding.contactSubtitle.setText(subtitle);
            binding.contactSubtitle.setVisibility(subtitle == null ? View.GONE : View.VISIBLE);

            binding.contactCall.setVisibility(hasNumber ? View.VISIBLE : View.INVISIBLE);
            binding.contactCall.setContentDescription(context.getString(R.string.call_name, name));
            binding.contactCall.setOnClickListener(v -> listener.onCall(contact));
            binding.contactRow.setContentDescription(subtitle == null ? name : name + ", " + subtitle);
            binding.contactRow.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onExpand(position);
                }
            });
            binding.contactRow.setOnLongClickListener(v -> {
                listener.onLongClick(contact, v);
                return true;
            });

            if (hasNumber) {
                bindAction(binding.actionFirst, contact, Action.MESSAGE);
                bindAction(binding.actionSecond, contact, videoSupported ? Action.VIDEO : Action.EDIT);
                bindAction(binding.actionThird, contact, Action.DETAILS);
            } else {
                bindAction(binding.actionFirst, contact, Action.EDIT);
                bindAction(binding.actionSecond, contact, Action.DETAILS);
                binding.actionThird.actionRoot.setVisibility(View.INVISIBLE);
            }
            bindExpanded(item, false);
        }

        void bindExpanded(Item item, boolean animate) {
            boolean expanded = item.id == expandedId;
            binding.contactCard.setBackgroundResource(expanded ? R.drawable.bg_row_expanded : 0);
            if (!animate) {
                binding.contactActions.setVisibility(expanded ? View.VISIBLE : View.GONE);
                binding.contactActions.setAlpha(1f);
                binding.contactActions.setTranslationY(0f);
                return;
            }
            if (expanded) {
                binding.contactActions.setVisibility(View.VISIBLE);
                binding.contactActions.setAlpha(0f);
                binding.contactActions.setTranslationY(-12f);
                binding.contactActions.animate().alpha(1f).translationY(0f).setDuration(180).start();
            } else {
                binding.contactActions.animate().cancel();
                binding.contactActions.setVisibility(View.GONE);
            }
        }

        private void bindAction(IncludeRowActionBinding action, ContactModel contact, Action type) {
            Context context = itemView.getContext();
            action.actionRoot.setVisibility(View.VISIBLE);
            int icon;
            int label;
            switch (type) {
                case MESSAGE:
                    icon = R.drawable.ic_chat;
                    label = R.string.messages;
                    break;
                case VIDEO:
                    icon = R.drawable.ic_videocam;
                    label = R.string.video_call;
                    break;
                case EDIT:
                    icon = R.drawable.ic_edit;
                    label = R.string.edit;
                    break;
                default:
                    icon = R.drawable.ic_info;
                    label = R.string.contact_info;
                    break;
            }
            action.actionIcon.setImageResource(icon);
            action.actionLabel.setText(label);
            action.actionRoot.setContentDescription(context.getString(label));
            action.actionRoot.setOnClickListener(v -> listener.onAction(contact, type));
        }
    }
}
