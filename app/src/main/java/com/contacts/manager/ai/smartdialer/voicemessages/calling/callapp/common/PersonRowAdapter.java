package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemPersonRowBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemSectionHeaderBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;

import java.util.Objects;

public class PersonRowAdapter extends ListAdapter<PersonRowAdapter.Item, RecyclerView.ViewHolder> {

    public interface Listener {
        void onClick(Item item);

        default void onAction(Item item) {
        }

        default boolean onLongClick(Item item, View view) {
            return false;
        }
    }

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ROW = 1;

    public static class Item {
        public final int type;
        public final String key;
        public final String title;
        public final CharSequence subtitle;
        public final String photoUri;
        public final String number;
        public final ContactModel contact;
        public boolean showAction;

        private Item(int type, String key, String title, CharSequence subtitle, String photoUri, String number,
                     ContactModel contact) {
            this.type = type;
            this.key = key;
            this.title = title;
            this.subtitle = subtitle;
            this.photoUri = photoUri;
            this.number = number;
            this.contact = contact;
        }

        public static Item header(String title) {
            return new Item(TYPE_HEADER, "h:" + title, title, null, null, null, null);
        }

        public static Item contact(ContactModel contact, CharSequence subtitle) {
            return new Item(TYPE_ROW, "c:" + contact.id + ":" + contact.primaryNumber, contact.getDisplayName(),
                    subtitle, contact.photoUri, contact.primaryNumber, contact);
        }

        public static Item number(String key, String title, CharSequence subtitle, String photoUri, String number,
                                  ContactModel contact) {
            return new Item(TYPE_ROW, key, title, subtitle, photoUri, number, contact);
        }

        public Item withAction() {
            showAction = true;
            return this;
        }
    }

    private static final DiffUtil.ItemCallback<Item> DIFF = new DiffUtil.ItemCallback<Item>() {
        @Override
        public boolean areItemsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.key.equals(newItem.key);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return Objects.equals(oldItem.title, newItem.title)
                    && TextUtils.equals(oldItem.subtitle, newItem.subtitle)
                    && Objects.equals(oldItem.photoUri, newItem.photoUri)
                    && oldItem.showAction == newItem.showAction;
        }
    };

    private final Listener listener;

    public PersonRowAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(ItemSectionHeaderBinding.inflate(inflater, parent, false));
        }
        return new RowHolder(ItemPersonRowBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = getItem(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).binding.sectionTitle.setText(item.title);
        } else {
            ((RowHolder) holder).bind(item);
        }
    }

    public int findHeaderPosition(String title) {
        for (int i = 0; i < getItemCount(); i++) {
            Item item = getItem(i);
            if (item.type == TYPE_HEADER && item.title.equals(title)) {
                return i;
            }
        }
        return -1;
    }

    public String sectionAt(int position) {
        for (int i = Math.min(position, getItemCount() - 1); i >= 0; i--) {
            Item item = getItem(i);
            if (item.type == TYPE_HEADER) {
                return item.title;
            }
        }
        return null;
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final ItemSectionHeaderBinding binding;

        HeaderHolder(ItemSectionHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    class RowHolder extends RecyclerView.ViewHolder {
        final ItemPersonRowBinding binding;

        RowHolder(ItemPersonRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Item item) {
            binding.personName.setText(item.title);
            binding.personSubtitle.setText(item.subtitle);
            binding.personSubtitle.setVisibility(TextUtils.isEmpty(item.subtitle) ? View.GONE : View.VISIBLE);
            String avatarName = item.contact != null ? item.contact.name : null;
            binding.personAvatar.bind(avatarName, item.photoUri);
            binding.personAction.setVisibility(item.showAction ? View.VISIBLE : View.GONE);
            binding.personAction.setContentDescription(
                    binding.getRoot().getContext().getString(R.string.call_name, item.title));
            binding.personAction.setOnClickListener(v -> listener.onAction(item));
            binding.getRoot().setOnClickListener(v -> listener.onClick(item));
            binding.getRoot().setOnLongClickListener(v -> listener.onLongClick(item, v));
        }
    }
}
