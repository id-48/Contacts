package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.search;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemSearchActionBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemSearchCarouselBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemSearchChipBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemSearchHeaderBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemSearchHintBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemSearchRowBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SearchAdapter extends ListAdapter<SearchAdapter.Item, RecyclerView.ViewHolder> {

    public interface Listener {
        void onOpen(Item item);

        void onCall(Item item);

        void onQuickAction(Item item);
    }

    static final int TYPE_HEADER = 0;
    static final int TYPE_ROW = 1;
    static final int TYPE_CAROUSEL = 2;
    static final int TYPE_ACTION = 3;
    static final int TYPE_HINT = 4;

    public static final int ACTION_CALL = 1;
    public static final int ACTION_CREATE = 2;

    public static final class Item {
        final int type;
        final String key;
        String title;
        String subtitle;
        int count;
        @DrawableRes
        int typeIcon;
        int typeColor;
        boolean subtitleTinted;
        String avatarName;
        String photoUri;
        String number;
        ContactModel contact;
        String query = "";
        int action;
        List<Item> people = Collections.emptyList();

        private Item(int type, String key) {
            this.type = type;
            this.key = key;
        }

        static Item header(String title, int count) {
            Item item = new Item(TYPE_HEADER, "h:" + title);
            item.title = title;
            item.count = count;
            return item;
        }

        static Item row(String key, String title, String subtitle, @Nullable String avatarName,
                        @Nullable String photoUri, String number, @Nullable ContactModel contact) {
            Item item = new Item(TYPE_ROW, key);
            item.title = title;
            item.subtitle = subtitle;
            item.avatarName = avatarName;
            item.photoUri = photoUri;
            item.number = number;
            item.contact = contact;
            return item;
        }

        static Item carousel(List<Item> people) {
            Item item = new Item(TYPE_CAROUSEL, "carousel");
            item.people = people;
            return item;
        }

        static Item action(int action, String title, String number) {
            Item item = new Item(TYPE_ACTION, "a:" + action);
            item.action = action;
            item.title = title;
            item.number = number;
            return item;
        }

        static Item hint() {
            return new Item(TYPE_HINT, "hint");
        }

        Item withType(@DrawableRes int icon, int color, boolean tintSubtitle) {
            typeIcon = icon;
            typeColor = color;
            subtitleTinted = tintSubtitle;
            return this;
        }

        Item withQuery(String query) {
            this.query = query == null ? "" : query;
            return this;
        }

        boolean sameContent(Item other) {
            return type == other.type
                    && count == other.count
                    && typeIcon == other.typeIcon
                    && typeColor == other.typeColor
                    && subtitleTinted == other.subtitleTinted
                    && action == other.action
                    && Objects.equals(title, other.title)
                    && Objects.equals(subtitle, other.subtitle)
                    && Objects.equals(avatarName, other.avatarName)
                    && Objects.equals(photoUri, other.photoUri)
                    && Objects.equals(number, other.number)
                    && Objects.equals(query, other.query)
                    && samePeople(other.people);
        }

        private boolean samePeople(List<Item> others) {
            if (people.size() != others.size()) {
                return false;
            }
            for (int i = 0; i < people.size(); i++) {
                Item a = people.get(i);
                Item b = others.get(i);
                if (!a.key.equals(b.key) || !a.sameContent(b)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final DiffUtil.ItemCallback<Item> DIFF = new DiffUtil.ItemCallback<Item>() {
        @Override
        public boolean areItemsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.key.equals(newItem.key);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.sameContent(newItem);
        }
    };

    private final Listener listener;

    public SearchAdapter(Listener listener) {
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
        switch (viewType) {
            case TYPE_HEADER:
                return new HeaderHolder(ItemSearchHeaderBinding.inflate(inflater, parent, false));
            case TYPE_CAROUSEL:
                return new CarouselHolder(ItemSearchCarouselBinding.inflate(inflater, parent, false));
            case TYPE_ACTION:
                return new ActionHolder(ItemSearchActionBinding.inflate(inflater, parent, false));
            case TYPE_HINT:
                return new HintHolder(ItemSearchHintBinding.inflate(inflater, parent, false));
            default:
                return new RowHolder(ItemSearchRowBinding.inflate(inflater, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = getItem(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).bind(item);
        } else if (holder instanceof RowHolder) {
            ((RowHolder) holder).bind(item);
        } else if (holder instanceof CarouselHolder) {
            ((CarouselHolder) holder).bind(item);
        } else if (holder instanceof ActionHolder) {
            ((ActionHolder) holder).bind(item);
        }
    }

    static CharSequence highlightName(Context context, String text, String query) {
        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(query)) {
            return text;
        }
        String q = fold(query.trim());
        String folded = fold(text);
        if (q.isEmpty() || folded.length() != text.length()) {
            folded = text.toLowerCase(Locale.getDefault());
            q = query.trim().toLowerCase(Locale.getDefault());
        }
        int start = -1;
        if (folded.startsWith(q)) {
            start = 0;
        } else {
            int word = folded.indexOf(" " + q);
            if (word >= 0) {
                start = word + 1;
            } else if (q.length() > 1) {
                start = folded.indexOf(q);
            }
        }
        if (start < 0) {
            return highlightDigits(context, text, query);
        }
        return span(context, text, start, start + q.length());
    }

    static CharSequence highlightDigits(Context context, String text, String query) {
        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(query)) {
            return text;
        }
        String digits = query.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return text;
        }
        StringBuilder textDigits = new StringBuilder();
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                textDigits.append(c);
                positions.add(i);
            }
        }
        int index = textDigits.indexOf(digits);
        if (index < 0) {
            return text;
        }
        return span(context, text, positions.get(index), positions.get(index + digits.length() - 1) + 1);
    }

    private static CharSequence span(Context context, String text, int start, int end) {
        if (start < 0 || end > text.length() || start >= end) {
            return text;
        }
        SpannableString spannable = new SpannableString(text);
        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.primary)),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    private static String fold(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase(Locale.getDefault());
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final ItemSearchHeaderBinding binding;

        HeaderHolder(ItemSearchHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Item item) {
            binding.headerTitle.setText(item.title);
            binding.headerCount.setVisibility(item.count > 0 ? View.VISIBLE : View.GONE);
            binding.headerCount.setText(String.valueOf(item.count));
        }
    }

    class RowHolder extends RecyclerView.ViewHolder {
        final ItemSearchRowBinding binding;

        RowHolder(ItemSearchRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Item item) {
            Context context = itemView.getContext();
            binding.rowAvatar.bind(item.avatarName, item.photoUri);
            binding.rowName.setText(highlightName(context, item.title, item.query));
            binding.rowSubtitle.setText(highlightDigits(context, item.subtitle, item.query));

            if (item.typeIcon != 0) {
                int color = ContextCompat.getColor(context, item.typeColor);
                binding.rowTypeIcon.setVisibility(View.VISIBLE);
                binding.rowTypeIcon.setImageResource(item.typeIcon);
                binding.rowTypeIcon.setImageTintList(ColorStateList.valueOf(color));
                binding.rowSubtitle.setTextColor(item.subtitleTinted ? color
                        : ContextCompat.getColor(context, R.color.text_muted));
            } else {
                binding.rowTypeIcon.setVisibility(View.GONE);
                binding.rowSubtitle.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
            }

            boolean canCall = !TextUtils.isEmpty(item.number);
            binding.rowCall.setVisibility(canCall ? View.VISIBLE : View.INVISIBLE);
            binding.rowCall.setContentDescription(context.getString(R.string.call_name, item.title));
            binding.rowCall.setOnClickListener(v -> listener.onCall(item));
            binding.searchRow.setContentDescription(item.title + ", " + item.subtitle);
            binding.searchRow.setOnClickListener(v -> listener.onOpen(item));
        }
    }

    class CarouselHolder extends RecyclerView.ViewHolder {
        final ChipAdapter chips = new ChipAdapter();

        CarouselHolder(ItemSearchCarouselBinding binding) {
            super(binding.getRoot());
            binding.carouselList.setLayoutManager(
                    new LinearLayoutManager(binding.getRoot().getContext(), RecyclerView.HORIZONTAL, false));
            binding.carouselList.setAdapter(chips);
            binding.carouselList.setItemAnimator(null);
        }

        void bind(Item item) {
            chips.setPeople(item.people);
        }
    }

    class ChipAdapter extends RecyclerView.Adapter<ChipHolder> {
        private List<Item> people = Collections.emptyList();

        void setPeople(List<Item> people) {
            this.people = people;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ChipHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ChipHolder(ItemSearchChipBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ChipHolder holder, int position) {
            holder.bind(people.get(position));
        }

        @Override
        public int getItemCount() {
            return people.size();
        }
    }

    class ChipHolder extends RecyclerView.ViewHolder {
        final ItemSearchChipBinding binding;

        ChipHolder(ItemSearchChipBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Item item) {
            binding.chipAvatar.bind(item.avatarName, item.photoUri);
            String label = item.avatarName != null ? firstName(item.avatarName) : item.title;
            binding.chipName.setText(label);
            itemView.setContentDescription(item.title);
            itemView.setOnClickListener(v -> listener.onOpen(item));
            itemView.setOnLongClickListener(v -> {
                if (TextUtils.isEmpty(item.number)) {
                    return false;
                }
                listener.onCall(item);
                return true;
            });
        }

        private String firstName(String name) {
            String trimmed = name.trim();
            int space = trimmed.indexOf(' ');
            return space > 0 ? trimmed.substring(0, space) : trimmed;
        }
    }

    class ActionHolder extends RecyclerView.ViewHolder {
        final ItemSearchActionBinding binding;

        ActionHolder(ItemSearchActionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Item item) {
            Context context = itemView.getContext();
            boolean call = item.action == ACTION_CALL;
            binding.actionIcon.setBackgroundResource(call ? R.drawable.bg_circle_primary : R.drawable.bg_search_count);
            binding.actionIcon.setImageResource(call ? R.drawable.ic_call_filled : R.drawable.ic_person_add);
            binding.actionIcon.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, call ? R.color.on_primary : R.color.primary)));
            binding.actionTitle.setText(item.title);
            binding.actionTitle.setTextColor(ContextCompat.getColor(context, call ? R.color.primary : R.color.text_primary));
            binding.actionRow.setOnClickListener(v -> listener.onQuickAction(item));
        }
    }

    static class HintHolder extends RecyclerView.ViewHolder {
        HintHolder(ItemSearchHintBinding binding) {
            super(binding.getRoot());
        }
    }
}
