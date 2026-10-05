package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.contacts;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.AppBottomSheet;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.AppDialogs;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.PermissionRequester;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.FragmentContactsBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.contactdetails.ContactDetailsActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.editcontact.EditContactActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.main.MainActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PermissionManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.IntentUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels.ContactsViewModel;

import java.util.ArrayList;
import java.util.List;

public class ContactsFragment extends Fragment implements MainActivity.Tab, ContactsAdapter.Listener {

    private FragmentContactsBinding binding;
    private ContactsViewModel viewModel;
    private ContactsAdapter adapter;
    private LinearLayoutManager layoutManager;
    private PermissionRequester permissionRequester;
    private List<ContactModel> latest;
    private final Runnable hideBubble = () -> {
        if (binding != null) {
            binding.letterBubble.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(150)
                    .withEndAction(() -> {
                        if (binding != null) {
                            binding.letterBubble.setVisibility(View.GONE);
                        }
                    }).start();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionRequester = new PermissionRequester(this, this::refreshState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentContactsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(ContactsViewModel.class);
        adapter = new ContactsAdapter(this);
        layoutManager = new LinearLayoutManager(requireContext());
        binding.contactsList.setLayoutManager(layoutManager);
        binding.contactsList.setAdapter(adapter);
        binding.contactsList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int first = layoutManager.findFirstVisibleItemPosition();
                if (first >= 0) {
                    binding.alphabetIndex.setSelected(adapter.sectionAt(first));
                }
            }
        });
        binding.alphabetIndex.setOnLetterListener(this::jumpTo);

        viewModel.getContacts().observe(getViewLifecycleOwner(), contacts -> {
            latest = contacts;
            render();
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progress.setVisibility(Boolean.TRUE.equals(loading) && latest == null
                        ? View.VISIBLE : View.GONE));
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.letterBubble.removeCallbacks(hideBubble);
        }
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
        Context context = requireContext();
        if (!ContactsService.canRead(context)) {
            adapter.submitList(null);
            binding.alphabetIndex.setVisibility(View.GONE);
            binding.progress.setVisibility(View.GONE);
            binding.emptyState.setContent(R.drawable.ic_contacts, R.string.perm_contacts_title, R.string.empty_contacts_permission);
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
            binding.alphabetIndex.setVisibility(View.GONE);
            binding.emptyState.setContent(R.drawable.ic_contacts, R.string.empty_contacts_title, R.string.empty_contacts_body);
            binding.emptyState.setAction(R.string.create_contact, v -> onCreateContact());
            binding.emptyState.show(true);
            return;
        }
        List<ContactsAdapter.Item> items = new ArrayList<>();
        items.add(ContactsAdapter.Item.create());
        String section = null;
        for (ContactModel contact : latest) {
            String letter = contact.getSectionLetter();
            if (!letter.equals(section)) {
                items.add(ContactsAdapter.Item.header(letter));
                section = letter;
            }
            items.add(ContactsAdapter.Item.contact(contact));
        }
        items.add(ContactsAdapter.Item.footer(
                getResources().getQuantityString(R.plurals.contacts_count, latest.size(), latest.size())));
        adapter.submitList(items);
        binding.alphabetIndex.setVisibility(latest.size() > 12 ? View.VISIBLE : View.GONE);
        binding.emptyState.show(false);
    }

    private void jumpTo(String letter) {
        List<String> letters = binding.alphabetIndex.getLetters();
        int start = letters.indexOf(letter);
        int position = -1;
        for (int i = start; i >= 0 && i < letters.size() && position < 0; i++) {
            position = adapter.findHeaderPosition(letters.get(i));
        }
        if (position < 0) {
            for (int i = start - 1; i >= 0 && position < 0; i--) {
                position = adapter.findHeaderPosition(letters.get(i));
            }
        }
        if (position >= 0) {
            layoutManager.scrollToPositionWithOffset(position, 0);
        }
        binding.letterBubble.setText(letter);
        binding.letterBubble.removeCallbacks(hideBubble);
        if (binding.letterBubble.getVisibility() != View.VISIBLE) {
            binding.letterBubble.setAlpha(0f);
            binding.letterBubble.setScaleX(0.8f);
            binding.letterBubble.setScaleY(0.8f);
            binding.letterBubble.setVisibility(View.VISIBLE);
        }
        binding.letterBubble.animate().cancel();
        binding.letterBubble.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(120).start();
        binding.letterBubble.postDelayed(hideBubble, 700);
    }

    @Override
    public void scrollToTop() {
        if (binding != null) {
            binding.contactsList.smoothScrollToPosition(0);
        }
    }

    @Override
    public void onCreateContact() {
        showFullscreen(AdScreens.CONTACTS_CREATE, () -> startActivity(EditContactActivity.createIntent(requireContext(), null)));
    }

    private void openInfo(ContactModel contact) {
        showFullscreen(AdScreens.CONTACTS_INFO,
                () -> startActivity(ContactDetailsActivity.intent(requireContext(), contact.id, contact.lookupKey)));
    }

    private void showFullscreen(String screenKey, Runnable onDone) {
        AdsManager.showFullscreen(requireActivity(), screenKey, () -> {
            if (isAdded()) {
                onDone.run();
            }
        });
    }

    @Override
    public void onCall(ContactModel contact) {
        PhoneService.call(requireActivity(), contact.primaryNumber);
    }

    @Override
    public void onOpen(ContactModel contact) {
        openInfo(contact);
    }

    @Override
    public void onLongClick(ContactModel contact, View anchor) {
        HapticUtils.longPress(anchor);
        Context context = requireContext();
        List<AppBottomSheet.Option> options = new ArrayList<>();
        if (PhoneUtils.isValidNumber(contact.primaryNumber)) {
            options.add(new AppBottomSheet.Option(R.drawable.ic_call, getString(R.string.call),
                    () -> PhoneService.call(requireActivity(), contact.primaryNumber)));
            options.add(new AppBottomSheet.Option(R.drawable.ic_chat, getString(R.string.message),
                    () -> IntentUtils.openSms(context, contact.primaryNumber)));
        }
        options.add(new AppBottomSheet.Option(R.drawable.ic_info, getString(R.string.contact_info),
                () -> openInfo(contact)));
        if (ContactsService.canWrite(context)) {
            options.add(new AppBottomSheet.Option(contact.starred ? R.drawable.ic_star_filled : R.drawable.ic_star,
                    getString(contact.starred ? R.string.remove_favorite : R.string.add_favorite),
                    () -> toggleFavorite(contact)));
            options.add(new AppBottomSheet.Option(R.drawable.ic_edit, getString(R.string.edit),
                    () -> startActivity(EditContactActivity.editIntent(context, contact.id))));
            options.add(new AppBottomSheet.Option(R.drawable.ic_delete, getString(R.string.delete),
                    () -> confirmDelete(contact)));
        }
        AppBottomSheet.showOptions(context, contact.getDisplayName(), options);
    }

    private void toggleFavorite(ContactModel contact) {
        Context context = requireContext().getApplicationContext();
        boolean target = !contact.starred;
        AppExecutors.io(() -> {
            boolean ok = ContactsService.setStarred(context, contact.id, target);
            AppExecutors.main(() -> Toast.makeText(context, ok
                    ? (target ? R.string.added_favorite : R.string.removed_favorite)
                    : R.string.action_failed, Toast.LENGTH_SHORT).show());
        });
    }

    private void confirmDelete(ContactModel contact) {
        Context context = requireContext().getApplicationContext();
        AppDialogs.confirm(requireContext(), getString(R.string.delete_contact_title),
                getString(R.string.delete_contact_body, contact.getDisplayName()), R.string.delete, true,
                () -> AppExecutors.io(() -> {
                    boolean ok = ContactsService.deleteContact(context, contact.id);
                    AppExecutors.main(() -> Toast.makeText(context,
                            ok ? R.string.contact_deleted : R.string.action_failed, Toast.LENGTH_SHORT).show());
                }));
    }
}
