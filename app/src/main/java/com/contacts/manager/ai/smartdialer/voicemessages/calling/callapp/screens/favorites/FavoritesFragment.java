package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.favorites;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LayoutAnimationController;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.AppBottomSheet;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.PermissionRequester;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.PersonRowAdapter;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.FragmentFavoritesBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.SheetPickFavoriteBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.contactdetails.ContactDetailsActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.main.MainActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PermissionManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.IntentUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels.FavoritesViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FavoritesFragment extends Fragment implements MainActivity.Tab, FavoritesAdapter.Listener {

    private FragmentFavoritesBinding binding;
    private FavoritesViewModel viewModel;
    private FavoritesAdapter adapter;
    private PermissionRequester permissionRequester;
    private List<ContactModel> latest;
    private boolean animatedIn;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionRequester = new PermissionRequester(this, this::refreshState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
        adapter = new FavoritesAdapter(this);
        int spanCount = getResources().getConfiguration().screenWidthDp >= 600 ? 5 : 3;
        binding.favoritesList.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
        binding.favoritesList.setAdapter(adapter);
        binding.favoritesList.setLayoutAnimation(createLayoutAnimation());
        animatedIn = savedInstanceState != null;
        viewModel.getFavorites().observe(getViewLifecycleOwner(), favorites -> {
            latest = favorites;
            render();
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progress.setVisibility(Boolean.TRUE.equals(loading) && latest == null
                        ? View.VISIBLE : View.GONE));
    }

    private LayoutAnimationController createLayoutAnimation() {
        AnimationSet set = new AnimationSet(true);
        set.addAnimation(new AlphaAnimation(0f, 1f));
        set.addAnimation(new ScaleAnimation(0.88f, 1f, 0.88f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f));
        set.setDuration(280);
        set.setInterpolator(new DecelerateInterpolator(1.6f));
        return new LayoutAnimationController(set, 0.12f);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void refreshState() {
        if (binding == null) {
            return;
        }
        if (ContactsService.canRead(requireContext())) {
            viewModel.loadIfNeeded();
        }
        render();
    }

    private void render() {
        if (binding == null) {
            return;
        }
        if (!ContactsService.canRead(requireContext())) {
            adapter.submitList(null);
            binding.progress.setVisibility(View.GONE);
            binding.emptyState.setContent(R.drawable.ic_star, R.string.perm_contacts_title, R.string.empty_contacts_permission);
            binding.emptyState.setAction(R.string.allow_access, v -> permissionRequester.request(requireActivity(),
                    PermissionManager.Group.CONTACTS));
            binding.emptyState.show(true);
            return;
        }
        if (latest == null) {
            binding.emptyState.show(false);
            return;
        }
        if (latest.isEmpty()) {
            adapter.submitList(null);
            binding.emptyState.setContent(R.drawable.ic_star, R.string.empty_favorites_title, R.string.empty_favorites_body);
            binding.emptyState.setAction(R.string.add_favorite, v -> onAddFavorite());
            binding.emptyState.show(true);
            return;
        }
        List<FavoritesAdapter.Item> items = new ArrayList<>();
        for (ContactModel contact : latest) {
            items.add(FavoritesAdapter.Item.contact(contact));
        }
        items.add(FavoritesAdapter.Item.add());
        boolean animate = !animatedIn;
        animatedIn = true;
        adapter.submitList(items, () -> {
            if (animate && binding != null) {
                binding.favoritesList.scheduleLayoutAnimation();
            }
        });
        binding.emptyState.show(false);
    }

    @Override
    public void scrollToTop() {
        if (binding != null) {
            binding.favoritesList.smoothScrollToPosition(0);
        }
    }

    @Override
    public void onOpen(ContactModel contact) {
        startActivity(ContactDetailsActivity.intent(requireContext(), contact.id, contact.lookupKey));
    }

    @Override
    public void onCall(ContactModel contact) {
        HapticUtils.confirm(binding.favoritesList);
        PhoneService.call(requireActivity(), contact.primaryNumber);
    }

    @Override
    public void onAddFavorite() {
        Context app = requireContext().getApplicationContext();
        AppExecutors.io(() -> {
            List<ContactModel> candidates = new ArrayList<>();
            for (ContactModel contact : ContactsService.getContacts(app)) {
                if (!contact.starred) {
                    candidates.add(contact);
                }
            }
            AppExecutors.main(() -> {
                if (binding == null || !isAdded()) {
                    return;
                }
                if (candidates.isEmpty()) {
                    ((MainActivity) requireActivity()).selectTab(MainActivity.TAB_CONTACTS);
                    return;
                }
                showPicker(candidates);
            });
        });
    }

    private void showPicker(List<ContactModel> candidates) {
        Context context = requireContext();
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        SheetPickFavoriteBinding sheet = SheetPickFavoriteBinding.inflate(LayoutInflater.from(context));
        PersonRowAdapter pickAdapter = new PersonRowAdapter(item -> {
            dialog.dismiss();
            setStarred(item.contact, true);
        });
        List<PersonRowAdapter.Item> rows = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<String> numbers = new ArrayList<>();
        for (ContactModel contact : candidates) {
            String formatted = PhoneUtils.isValidNumber(contact.primaryNumber)
                    ? PhoneUtils.format(context, contact.primaryNumber) : null;
            boolean named = contact.name != null && contact.name.matches(".*\\p{L}.*");
            String title = named || formatted == null ? contact.getDisplayName() : formatted;
            rows.add(PersonRowAdapter.Item.number("c:" + contact.id, title, named ? formatted : null,
                    contact.photoUri, contact.primaryNumber, contact));
            names.add(fold(contact.getDisplayName()));
            numbers.add(digitsOf(contact.primaryNumber));
        }
        sheet.pickList.setLayoutManager(new LinearLayoutManager(context));
        sheet.pickList.setAdapter(pickAdapter);
        pickAdapter.submitList(rows);

        int rowHeight = getResources().getDimensionPixelSize(R.dimen.row_min_height);
        int maxHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.55f);
        ViewGroup.LayoutParams containerParams = sheet.pickListContainer.getLayoutParams();
        containerParams.height = Math.min(Math.max(rows.size(), 3) * rowHeight, maxHeight);
        sheet.pickListContainer.setLayoutParams(containerParams);
        ViewGroup.LayoutParams listParams = sheet.pickList.getLayoutParams();
        listParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        sheet.pickList.setLayoutParams(listParams);

        sheet.pickSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                sheet.pickSearchClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                String folded = fold(query);
                String digits = digitsOf(query);
                boolean numeric = !digits.isEmpty() && query.matches("[+\\d\\s()\\-]+");
                List<PersonRowAdapter.Item> filtered = new ArrayList<>();
                for (int i = 0; i < rows.size(); i++) {
                    if (folded.isEmpty() || names.get(i).contains(folded)
                            || (numeric && numbers.get(i).contains(digits))) {
                        filtered.add(rows.get(i));
                    }
                }
                pickAdapter.submitList(filtered, () -> sheet.pickList.scrollToPosition(0));
                sheet.pickEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
        sheet.pickSearchField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard(v);
                return true;
            }
            return false;
        });
        sheet.pickSearchClear.setOnClickListener(v -> sheet.pickSearchField.setText(null));
        sheet.pickList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideKeyboard(recyclerView);
                }
            }
        });

        dialog.setContentView(sheet.getRoot());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setSkipCollapsed(true);
        dialog.show();
    }

    private static String fold(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.getDefault()).trim();
    }

    private static String digitsOf(@Nullable String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private static void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void setStarred(ContactModel contact, boolean starred) {
        Context app = requireContext().getApplicationContext();
        AppExecutors.io(() -> {
            boolean ok = ContactsService.setStarred(app, contact.id, starred);
            AppExecutors.main(() -> {
                Toast.makeText(app, ok ? (starred ? R.string.added_favorite : R.string.removed_favorite)
                        : R.string.action_failed, Toast.LENGTH_SHORT).show();
                if (ok && binding != null) {
                    viewModel.reload();
                }
            });
        });
    }

    @Override
    public void onLongClick(ContactModel contact, View view) {
        HapticUtils.longPress(view);
        Context context = requireContext();
        List<AppBottomSheet.Option> options = new ArrayList<>();
        if (PhoneUtils.isValidNumber(contact.primaryNumber)) {
            options.add(new AppBottomSheet.Option(R.drawable.ic_call, getString(R.string.call),
                    () -> PhoneService.call(requireActivity(), contact.primaryNumber)));
            options.add(new AppBottomSheet.Option(R.drawable.ic_chat, getString(R.string.message),
                    () -> IntentUtils.openSms(context, contact.primaryNumber)));
        }
        options.add(new AppBottomSheet.Option(R.drawable.ic_info, getString(R.string.contact_info),
                () -> onOpen(contact)));
        if (ContactsService.canWrite(context)) {
            options.add(new AppBottomSheet.Option(R.drawable.ic_star, getString(R.string.remove_favorite),
                    () -> setStarred(contact, false)));
        }
        AppBottomSheet.showOptions(context, contact.getDisplayName(), options);
    }
}
