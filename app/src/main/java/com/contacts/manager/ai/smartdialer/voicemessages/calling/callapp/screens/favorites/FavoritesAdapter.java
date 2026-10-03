package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.favorites;

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
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemFavoriteAddBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemFavoriteBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.util.Objects;

public class FavoritesAdapter extends ListAdapter<FavoritesAdapter.Item, RecyclerView.ViewHolder> {

    public interface Listener {
        void onOpen(ContactModel contact);

        void onCall(ContactModel contact);

        void onLongClick(ContactModel contact, View view);

        void onAddFavorite();
    }

    private static final int TYPE_CONTACT = 0;
    private static final int TYPE_ADD = 1;

    public static class Item {
        final ContactModel contact;

        private Item(ContactModel contact) {
            this.contact = contact;
        }

        public static Item contact(ContactModel contact) {
            return new Item(contact);
        }

        public static Item add() {
            return new Item(null);
        }

        long id() {
            return contact == null ? -1L : contact.id;
        }
    }

    private static final DiffUtil.ItemCallback<Item> DIFF = new DiffUtil.ItemCallback<Item>() {
        @Override
        public boolean areItemsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.id() == newItem.id();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            if (oldItem.contact == null || newItem.contact == null) {
                return oldItem.contact == newItem.contact;
            }
            ContactModel a = oldItem.contact;
            ContactModel b = newItem.contact;
            return Objects.equals(a.name, b.name) && Objects.equals(a.photoUri, b.photoUri)
                    && Objects.equals(a.primaryNumber, b.primaryNumber);
        }
    };

    private final Listener listener;

    public FavoritesAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id();
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).contact == null ? TYPE_ADD : TYPE_CONTACT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ADD) {
            ItemFavoriteAddBinding add = ItemFavoriteAddBinding.inflate(inflater, parent, false);
            add.addTile.setOnClickListener(v -> listener.onAddFavorite());
            return new AddHolder(add.getRoot());
        }
        return new Holder(ItemFavoriteBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof Holder) {
            ((Holder) holder).bind(getItem(position).contact);
        }
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

    static class AddHolder extends RecyclerView.ViewHolder {
        AddHolder(View view) {
            super(view);
        }
    }

    class Holder extends RecyclerView.ViewHolder {
        final ItemFavoriteBinding binding;

        Holder(ItemFavoriteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ContactModel contact) {
            Context context = itemView.getContext();
            boolean callable = PhoneUtils.isValidNumber(contact.primaryNumber);
            String formatted = callable ? PhoneUtils.format(context, contact.primaryNumber) : null;
            boolean named = hasLetter(contact.name);
            String name = named || formatted == null ? contact.getDisplayName() : formatted;

            binding.favoriteName.setText(name);
            binding.favoriteNumber.setText(named ? formatted : null);
            binding.favoriteNumber.setVisibility(named && formatted != null ? View.VISIBLE : View.INVISIBLE);
            binding.favoriteAvatar.bind(contact.name, contact.photoUri);
            binding.favoriteCall.setVisibility(callable ? View.VISIBLE : View.INVISIBLE);
            binding.favoriteCall.setContentDescription(context.getString(R.string.call_name, name));
            binding.favoriteCall.setOnClickListener(v -> listener.onCall(contact));
            binding.favoriteTile.setContentDescription(formatted != null && named ? name + ", " + formatted : name);
            binding.favoriteTile.setOnClickListener(v -> listener.onOpen(contact));
            binding.favoriteTile.setOnLongClickListener(v -> {
                listener.onLongClick(contact, v);
                return true;
            });
        }
    }
}
