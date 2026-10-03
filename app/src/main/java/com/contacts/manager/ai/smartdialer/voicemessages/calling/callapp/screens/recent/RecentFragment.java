package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.recent;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.AppBottomSheet;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.AppDialogs;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.PermissionRequester;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.FragmentRecentBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.CallGroupModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.CallModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.callhistory.CallHistoryActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.contactdetails.ContactDetailsActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.editcontact.EditContactActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.main.MainActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.BlockService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.CallLogService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.NotificationService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PermissionManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.WhatsAppService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.DateUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.IntentUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels.RecentViewModel;

import java.util.ArrayList;
import java.util.List;

public class RecentFragment extends Fragment implements MainActivity.Tab, RecentAdapter.Listener {

    private static final String STATE_MISSED = "missed_only";

    private FragmentRecentBinding binding;
    private RecentViewModel viewModel;
    private RecentAdapter adapter;
    private PermissionRequester permissionRequester;
    private boolean missedOnly;
    private List<CallModel> latestCalls;

    private final ActivityResultLauncher<Intent> roleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> refreshState());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionRequester = new PermissionRequester(this, this::refreshState);
        if (savedInstanceState != null) {
            missedOnly = savedInstanceState.getBoolean(STATE_MISSED);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(RecentViewModel.class);
        adapter = new RecentAdapter(this);
        adapter.setVideoSupported(WhatsAppService.isInstalled(requireContext()));
        binding.recentList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recentList.setAdapter(adapter);

        binding.segmented.segmentFirst.setText(R.string.filter_all);
        binding.segmented.segmentSecond.setText(R.string.filter_missed);
        binding.segmented.segmentFirst.setOnClickListener(v -> setFilter(false, true));
        binding.segmented.segmentSecond.setOnClickListener(v -> setFilter(true, true));
        binding.segmented.getRoot().post(() -> setFilter(missedOnly, false));

        viewModel.getCalls().observe(getViewLifecycleOwner(), calls -> {
            latestCalls = calls;
            render();
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progress.setVisibility(Boolean.TRUE.equals(loading) && latestCalls == null
                        ? View.VISIBLE : View.GONE));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_MISSED, missedOnly);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshState();
        if (!isHidden()) {
            clearMissedNotifications();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshState();
            clearMissedNotifications();
        } else if (adapter != null) {
            adapter.collapse();
        }
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
        Context context = requireContext();
        updateBanner();
        if (CallLogService.canRead(context)) {
            viewModel.reload();
        } else {
            latestCalls = null;
            render();
        }
    }

    private void clearMissedNotifications() {
        Context context = requireContext().getApplicationContext();
        if (!PhoneService.isDefaultDialer(context)) {
            return;
        }
        AppExecutors.io(() -> {
            CallLogService.markMissedCallsRead(context);
            PhoneService.cancelMissedCallsNotification(context);
            NotificationService.cancelMissedCallNotifications(context);
        });
    }

    private void setFilter(boolean missed, boolean animate) {
        if (binding == null) {
            return;
        }
        if (animate && missed != missedOnly) {
            HapticUtils.tick(binding.segmented.getRoot());
        }
        missedOnly = missed;
        View thumb = binding.segmented.segmentThumb;
        View track = binding.segmented.getRoot();
        int innerWidth = track.getWidth() - track.getPaddingLeft() - track.getPaddingRight();
        if (innerWidth > 0) {
            ViewGroup.LayoutParams params = thumb.getLayoutParams();
            if (params.width != innerWidth / 2) {
                params.width = innerWidth / 2;
                thumb.setLayoutParams(params);
            }
            boolean rtl = track.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            float target = missed ? (rtl ? -innerWidth / 2f : innerWidth / 2f) : 0f;
            if (animate) {
                thumb.animate().translationX(target).setDuration(220).start();
            } else {
                thumb.setTranslationX(target);
            }
        }
        binding.segmented.segmentFirst.setSelected(!missed);
        binding.segmented.segmentSecond.setSelected(missed);
        if (animate) {
            adapter.collapse();
            render(() -> {
                if (binding != null) {
                    binding.recentList.scrollToPosition(0);
                }
            });
        }
    }

    private void updateBanner() {
        Context context = requireContext();
        View banner = binding.banner.bannerRoot;
        if (!PhoneService.isDefaultDialer(context) && !StorageService.isDefaultBannerDismissed()
                && CallLogService.canRead(context)) {
            binding.banner.bannerIcon.setImageResource(R.drawable.ic_call);
            binding.banner.bannerTitle.setText(R.string.banner_default_title);
            binding.banner.bannerBody.setText(R.string.banner_default_body);
            binding.banner.bannerAction.setText(R.string.set_as_default);
            binding.banner.bannerClose.setVisibility(View.VISIBLE);
            binding.banner.bannerClose.setOnClickListener(v -> {
                StorageService.setDefaultBannerDismissed(true);
                banner.setVisibility(View.GONE);
            });
            banner.setOnClickListener(v -> {
                Intent intent = PhoneService.createDefaultDialerIntent(context);
                if (intent != null) {
                    try {
                        roleLauncher.launch(intent);
                    } catch (Exception ignored) {
                    }
                }
            });
            banner.setVisibility(View.VISIBLE);
        } else if (CallLogService.canRead(context) && !PermissionManager.isGranted(context, PermissionManager.Group.CONTACTS)) {
            binding.banner.bannerIcon.setImageResource(R.drawable.ic_person);
            binding.banner.bannerTitle.setText(R.string.banner_contacts_title);
            binding.banner.bannerBody.setText(R.string.banner_contacts_body);
            binding.banner.bannerAction.setText(R.string.allow);
            binding.banner.bannerClose.setVisibility(View.GONE);
            banner.setOnClickListener(v -> permissionRequester.request(requireActivity(),
                    PermissionManager.Group.CONTACTS));
            banner.setVisibility(View.VISIBLE);
        } else {
            banner.setVisibility(View.GONE);
        }
    }

    private void render() {
        render(null);
    }

    private void render(@Nullable Runnable onCommitted) {
        if (binding == null) {
            return;
        }
        Context context = requireContext();
        if (!CallLogService.canRead(context)) {
            adapter.submitList(null);
            binding.progress.setVisibility(View.GONE);
            binding.emptyState.setContent(R.drawable.ic_history, R.string.perm_call_log_title, R.string.empty_recent_permission);
            binding.emptyState.setAction(R.string.allow_access, v -> permissionRequester.request(requireActivity(),
                    PermissionManager.Group.CALL_LOG, PermissionManager.Group.CONTACTS));
            binding.emptyState.show(true);
            return;
        }
        if (latestCalls == null) {
            binding.emptyState.show(false);
            return;
        }
        List<CallGroupModel> groups = CallLogService.group(latestCalls, missedOnly);
        List<RecentAdapter.Item> items = new ArrayList<>();
        String lastSection = null;
        for (CallGroupModel group : groups) {
            String section = DateUtils.sectionTitle(context, group.getLatest().date);
            if (!section.equals(lastSection)) {
                items.add(RecentAdapter.Item.header(section));
                lastSection = section;
            }
            items.add(RecentAdapter.Item.group(group));
        }
        adapter.submitList(items, onCommitted);
        if (items.isEmpty()) {
            if (missedOnly) {
                binding.emptyState.setContent(R.drawable.ic_call_missed, R.string.empty_missed_title, R.string.empty_missed_body);
            } else {
                binding.emptyState.setContent(R.drawable.ic_history, R.string.empty_recent_title, R.string.empty_recent_body);
                binding.emptyState.setAction(R.string.open_dialpad, v -> requireActivity().findViewById(R.id.fab).performClick());
            }
        }
        binding.emptyState.show(items.isEmpty());
    }

    @Override
    public void scrollToTop() {
        if (binding != null) {
            binding.recentList.smoothScrollToPosition(0);
        }
    }

    @Override
    public void onExpand(int position) {
        TransitionManager.beginDelayedTransition(binding.recentList, new AutoTransition().setDuration(180));
        adapter.toggleExpanded(position);
    }

    @Override
    public void onCall(CallGroupModel group) {
        PhoneService.call(requireActivity(), group.getNumber());
    }

    @Override
    public void onAction(CallGroupModel group, RecentAdapter.Action action) {
        CallModel latest = group.getLatest();
        switch (action) {
            case ADD_CONTACT:
                startActivity(EditContactActivity.createIntent(requireContext(), latest.number));
                break;
            case MESSAGE:
                IntentUtils.openSms(requireContext(), latest.number);
                break;
            case VIDEO:
                WhatsAppService.videoCall(requireActivity(), latest.number);
                break;
            case DETAILS:
                startActivity(ContactDetailsActivity.intent(requireContext(), latest.contactId, latest.lookupKey));
                break;
            default:
                startActivity(CallHistoryActivity.intent(requireContext(), latest.number));
                break;
        }
    }

    @Override
    public void onLongClick(CallGroupModel group, View anchor) {
        HapticUtils.longPress(anchor);
        Context context = requireContext();
        CallModel latest = group.getLatest();
        List<AppBottomSheet.Option> options = new ArrayList<>();
        boolean isPrivate = PhoneUtils.isPrivate(latest.number);
        if (!isPrivate) {
            options.add(new AppBottomSheet.Option(R.drawable.ic_content_copy, getString(R.string.copy_number),
                    () -> copyNumber(latest.number)));
            if (BlockService.canBlock(context)) {
                options.add(new AppBottomSheet.Option(R.drawable.ic_block, getString(R.string.block_number),
                        () -> confirmBlock(latest.number)));
            }
        }
        options.add(new AppBottomSheet.Option(R.drawable.ic_history, getString(R.string.call_history),
                () -> startActivity(CallHistoryActivity.intent(context, latest.number))));
        if (CallLogService.canWrite(context)) {
            options.add(new AppBottomSheet.Option(R.drawable.ic_delete, getString(R.string.delete_from_history),
                    () -> deleteGroup(group)));
        }
        String title = isPrivate ? getString(R.string.private_number) : group.getDisplayName();
        AppBottomSheet.showOptions(context, title, options);
    }

    private void copyNumber(String number) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.phone), number));
            Toast.makeText(requireContext(), R.string.number_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmBlock(String number) {
        Context context = requireContext().getApplicationContext();
        AppDialogs.confirm(requireContext(), getString(R.string.block_number_title, number),
                getString(R.string.block_number_body), R.string.block, true, () -> AppExecutors.io(() -> {
                    boolean blocked = BlockService.block(context, number);
                    AppExecutors.main(() -> Toast.makeText(context,
                            blocked ? R.string.number_blocked : R.string.block_failed, Toast.LENGTH_SHORT).show());
                }));
    }

    private void deleteGroup(CallGroupModel group) {
        Context context = requireContext().getApplicationContext();
        List<CallModel> calls = new ArrayList<>(group.calls);
        AppDialogs.confirm(requireContext(), getString(R.string.delete_history_title),
                getResources().getQuantityString(R.plurals.delete_calls_body, calls.size(), calls.size()),
                R.string.delete, true, () -> AppExecutors.io(() -> {
                    boolean deleted = CallLogService.deleteCalls(context, calls);
                    AppExecutors.main(() -> {
                        if (!deleted) {
                            Toast.makeText(context, R.string.delete_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                }));
    }
}
