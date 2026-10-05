package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.contactdetails;

import android.animation.AnimatorInflater;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.ViewModelProvider;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.AppBottomSheet;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.ConfirmDialog;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.IntentKeys;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityContactDetailsBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.IncludeBottomActionBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.IncludeQuickActionBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemCallEntryBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemInfoRowBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemSectionHeaderBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.CallModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.LabeledValue;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.callhistory.CallEntryAdapter;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.callhistory.CallHistoryActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.editcontact.EditContactActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.BlockService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.WhatsAppService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AvatarBitmaps;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.DateUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.IntentUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels.ContactDetailsViewModel;
import java.util.ArrayList;
import java.util.List;

public class ContactDetailsActivity extends BaseActivity {

    private ActivityContactDetailsBinding binding;
    private ContactDetailsViewModel viewModel;
    private ContactDetailsViewModel.State state;
    private boolean busy;
    private boolean titleShown;

    public static Intent intent(Context context, long contactId, String lookupKey) {
        return new Intent(context, ContactDetailsActivity.class)
                .setAction(Intent.ACTION_VIEW)
                .putExtra(IntentKeys.EXTRA_CONTACT_ID, contactId)
                .putExtra(IntentKeys.EXTRA_LOOKUP_KEY, lookupKey);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContactDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        long contactId = getIntent().getLongExtra(IntentKeys.EXTRA_CONTACT_ID, -1);
        String lookupKey = getIntent().getStringExtra(IntentKeys.EXTRA_LOOKUP_KEY);
        viewModel = new ViewModelProvider(this).get(ContactDetailsViewModel.class);
        viewModel.init(contactId, lookupKey);

        setupBack(binding.backButton);
        setupBackAd(AdScreens.CONTACT_DETAILS_BACK);
        binding.editButton.setOnClickListener(v -> {
            if (state != null && state.contact != null) {
                startActivity(EditContactActivity.editIntent(this, state.contact.id));
            }
        });
        binding.moreButton.setOnClickListener(v -> showMoreMenu());
        binding.showMoreButton.setOnClickListener(v -> {
            if (state != null && state.contact != null) {
                openCallHistory(state.contact, AdScreens.CONTACT_DETAILS_SHOW_MORE);
            }
        });
        AdsManager.showNativeBig(this, binding.nativeBigContainer, AdScreens.CONTACT_DETAILS);
        AdsManager.showBanner(this, binding.bannerContainer, AdScreens.CONTACT_DETAILS);
        binding.scrollView.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> updateTitle(scrollY));

        setupQuick(binding.quickCall, R.drawable.ic_call, R.string.call, v -> callPrimary(false));
        setupQuick(binding.quickMessage, R.drawable.ic_chat, R.string.message, v -> messagePrimary());
        setupQuick(binding.quickVideo, R.drawable.ic_videocam, R.string.video, v -> callPrimary(true));

        viewModel.getState().observe(this, this::render);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.reload();
    }

    private void setupQuick(IncludeQuickActionBinding quick, int icon, int label, View.OnClickListener listener) {
        quick.quickIcon.setImageResource(icon);
        quick.quickIcon.setImageTintList(ColorStateList.valueOf(getColor(R.color.primary)));
        quick.quickLabel.setText(label);
        quick.quickRoot.setContentDescription(getString(label));
        quick.quickRoot.setOnClickListener(v -> {
            HapticUtils.tap(v);
            listener.onClick(v);
        });
    }

    private void animateIn() {
        View[] views = {binding.detailsAvatar, binding.detailsName, binding.detailsSubtitle, binding.quickActions,
                binding.infoTitle, binding.infoCard, binding.activityHeader, binding.activityCard};
        float offset = getResources().getDimension(R.dimen.space_16);
        binding.detailsAvatar.setScaleX(0.86f);
        binding.detailsAvatar.setScaleY(0.86f);
        int order = 0;
        for (View view : views) {
            if (view.getVisibility() != View.VISIBLE) {
                continue;
            }
            view.setAlpha(0f);
            view.setTranslationY(offset);
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(order++ * 40L)
                    .setDuration(340)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .start();
        }

        binding.bottomBar.setTranslationY(getResources().getDimension(R.dimen.space_24));
        binding.bottomBar.setAlpha(0f);
        binding.bottomBar.animate()
                .translationY(0f)
                .alpha(1f)
                .setStartDelay(160)
                .setDuration(320)
                .setInterpolator(new DecelerateInterpolator(2f))
                .start();
    }

    private static void bounce(View view) {
        view.animate().cancel();
        view.setScaleX(0.6f);
        view.setScaleY(0.6f);
        view.animate().scaleX(1f).scaleY(1f).setStartDelay(0).setDuration(320)
                .setInterpolator(new OvershootInterpolator(3f)).start();
    }

    private void updateTitle(int scrollY) {
        if (state == null || state.contact == null) {
            return;
        }
        boolean show = scrollY > binding.detailsName.getBottom();
        if (show == titleShown) {
            return;
        }
        titleShown = show;
        binding.topTitle.animate().alpha(0f).setDuration(90).withEndAction(() -> {
            binding.topTitle.setText(show ? state.contact.getDisplayName() : getString(R.string.details));
            binding.topTitle.animate().alpha(1f).setDuration(140).start();
        }).start();
    }

    private void render(ContactDetailsViewModel.State newState) {
        state = newState;
        binding.progress.setVisibility(View.GONE);
        if (newState.contact == null) {
            binding.scrollView.setVisibility(View.GONE);
            binding.bottomBar.setVisibility(View.GONE);
            binding.editButton.setVisibility(View.GONE);
            binding.moreButton.setVisibility(View.GONE);
            binding.emptyState.setContent(R.drawable.ic_person, R.string.contact_not_found_title, R.string.contact_not_found_body);
            binding.emptyState.show(true);
            return;
        }
        boolean firstRender = binding.scrollView.getVisibility() != View.VISIBLE;
        ContactModel contact = newState.contact;
        binding.emptyState.show(false);
        binding.scrollView.setVisibility(View.VISIBLE);
        binding.bottomBar.setVisibility(View.VISIBLE);
        binding.editButton.setVisibility(ContactsService.canWrite(this) ? View.VISIBLE : View.GONE);

        binding.detailsAvatar.bind(contact.name, contact.fullPhotoUri != null ? contact.fullPhotoUri : contact.photoUri);
        binding.detailsName.setText(contact.getDisplayName());
        String subtitle = joinNonEmpty(contact.jobTitle, contact.company);
        binding.detailsSubtitle.setText(subtitle);
        binding.detailsSubtitle.setVisibility(TextUtils.isEmpty(subtitle) ? View.GONE : View.VISIBLE);

        boolean hasPhone = !contact.phones.isEmpty();
        binding.quickCall.quickRoot.setEnabled(hasPhone);
        binding.quickCall.quickRoot.setAlpha(hasPhone ? 1f : 0.4f);
        binding.quickMessage.quickRoot.setEnabled(hasPhone);
        binding.quickMessage.quickRoot.setAlpha(hasPhone ? 1f : 0.4f);
        boolean video = hasPhone && WhatsAppService.isInstalled(this);
        binding.quickVideo.quickRoot.setVisibility(video ? View.VISIBLE : View.GONE);

        renderInfo(contact);
        renderActivity(newState.recentCalls);
        renderBottomBar(newState);

        if (firstRender) {
            animateIn();
        }
    }

    private void renderInfo(ContactModel contact) {
        binding.infoCard.removeAllViews();
        boolean video = WhatsAppService.isInstalled(this);
        for (LabeledValue phone : contact.phones) {
            ItemInfoRowBinding row = addInfoRow(R.drawable.ic_call, PhoneUtils.format(this, phone.value),
                    ContactsService.phoneTypeLabel(this, phone));
            row.infoIcon.setImageTintList(ColorStateList.valueOf(getColor(R.color.call_incoming)));
            row.infoRoot.setOnClickListener(v -> {
                HapticUtils.tap(v);
                PhoneService.call(this, phone.value);
            });
            row.infoRoot.setOnLongClickListener(v -> {
                HapticUtils.longPress(v);
                copy(phone.value);
                return true;
            });
            ColorStateList accent = ColorStateList.valueOf(getColor(R.color.primary));
            row.infoActionFirst.setVisibility(View.VISIBLE);
            row.infoActionFirst.setImageResource(R.drawable.ic_chat);
            row.infoActionFirst.setImageTintList(accent);
            row.infoActionFirst.setContentDescription(getString(R.string.message));
            row.infoActionFirst.setOnClickListener(v -> {
                HapticUtils.tap(v);
                IntentUtils.openSms(this, phone.value);
            });
            if (video) {
                row.infoActionSecond.setVisibility(View.VISIBLE);
                row.infoActionSecond.setImageResource(R.drawable.ic_videocam);
                row.infoActionSecond.setImageTintList(accent);
                row.infoActionSecond.setContentDescription(getString(R.string.video_call));
                row.infoActionSecond.setOnClickListener(v -> {
                    HapticUtils.tap(v);
                    WhatsAppService.videoCall(this, phone.value);
                });
            }
        }
        for (LabeledValue email : contact.emails) {
            ItemInfoRowBinding row = addInfoRow(R.drawable.ic_mail, email.value,
                    ContactsService.emailTypeLabel(this, email));
            row.infoRoot.setOnClickListener(v -> {
                HapticUtils.tap(v);
                IntentUtils.openEmail(this, email.value);
            });
            row.infoRoot.setOnLongClickListener(v -> {
                HapticUtils.longPress(v);
                copy(email.value);
                return true;
            });
        }
        if (!TextUtils.isEmpty(contact.address)) {
            ItemInfoRowBinding row = addInfoRow(R.drawable.ic_location_on, contact.address, getString(R.string.address));
            row.infoRoot.setOnClickListener(v -> {
                HapticUtils.tap(v);
                IntentUtils.openMap(this, contact.address);
            });
        }
        if (!TextUtils.isEmpty(contact.birthday)) {
            addInfoRow(R.drawable.ic_cake, DateUtils.birthdayLabel(contact.birthday), getString(R.string.birthday));
        }
        if (!TextUtils.isEmpty(contact.nickname)) {
            addInfoRow(R.drawable.ic_badge, contact.nickname, getString(R.string.nickname));
        }
        if (!TextUtils.isEmpty(contact.note)) {
            addInfoRow(R.drawable.ic_sticky_note_2, contact.note, getString(R.string.notes));
        }
        boolean empty = binding.infoCard.getChildCount() == 0;
        binding.infoCard.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.infoTitle.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private ItemInfoRowBinding addInfoRow(int icon, String value, String label) {
        ItemInfoRowBinding row = ItemInfoRowBinding.inflate(LayoutInflater.from(this), binding.infoCard, false);
        row.infoIcon.setImageResource(icon);
        row.infoValue.setText(value);
        row.infoLabel.setText(label);
        row.infoLabel.setVisibility(TextUtils.isEmpty(label) ? View.GONE : View.VISIBLE);
        row.infoRoot.setContentDescription(TextUtils.isEmpty(label) ? value : label + ", " + value);
        binding.infoCard.addView(row.getRoot());
        return row;
    }

    private void renderActivity(List<CallModel> calls) {
        binding.activityCard.removeAllViews();
        boolean hasCalls = !calls.isEmpty();
        binding.activityHeader.setVisibility(hasCalls ? View.VISIBLE : View.GONE);
        binding.activityCard.setVisibility(hasCalls ? View.VISIBLE : View.GONE);
        if (!hasCalls) {
            return;
        }
        String lastSection = null;
        for (int i = 0; i < calls.size(); i++) {
            CallModel call = calls.get(i);
            String section = DateUtils.sectionTitle(this, call.date);
            if (!section.equals(lastSection)) {
                ItemSectionHeaderBinding header = ItemSectionHeaderBinding.inflate(getLayoutInflater(),
                        binding.activityCard, false);
                header.sectionTitle.setText(section);
                binding.activityCard.addView(header.getRoot());
                lastSection = section;
            }
            boolean first = i == 0 || !DateUtils.isSameDay(calls.get(i - 1).date, call.date);
            boolean last = i == calls.size() - 1 || !DateUtils.isSameDay(calls.get(i + 1).date, call.date);
            ItemCallEntryBinding entry = ItemCallEntryBinding.inflate(getLayoutInflater(), binding.activityCard, false);
            CallEntryAdapter.bindEntry(entry, call, state.contact.phones.size() > 1);
            entry.entryRoot.setBackgroundResource(first && last ? R.drawable.bg_card
                    : first ? R.drawable.bg_card_top : last ? R.drawable.bg_card_bottom : R.drawable.bg_card_middle);
            binding.activityCard.addView(entry.getRoot());
        }
    }

    private void renderBottomBar(ContactDetailsViewModel.State s) {
        boolean starred = s.contact.starred;
        bindBottom(binding.actionFavorite, starred ? R.drawable.ic_star_filled : R.drawable.ic_star,
                starred ? R.string.unfavorite : R.string.favorite, starred ? R.color.primary : R.color.text_primary,
                v -> toggleFavorite());
        binding.actionFavorite.bottomActionRoot.setVisibility(ContactsService.canWrite(this) ? View.VISIBLE : View.GONE);

        boolean blockAvailable = s.canBlock && !s.contact.phones.isEmpty();
        bindBottom(binding.actionBlock, R.drawable.ic_block, s.blocked ? R.string.unblock : R.string.block,
                s.blocked ? R.color.error : R.color.text_primary, v -> toggleBlock());
        binding.actionBlock.bottomActionRoot.setVisibility(blockAvailable ? View.VISIBLE : View.GONE);

        bindBottom(binding.actionDelete, R.drawable.ic_delete, R.string.delete, R.color.error, v -> confirmDelete());
        binding.actionDelete.bottomActionRoot.setVisibility(ContactsService.canWrite(this) ? View.VISIBLE : View.GONE);
    }

    private void bindBottom(IncludeBottomActionBinding action, int icon, int label, int colorRes,
                            View.OnClickListener listener) {
        int color = getColor(colorRes);
        action.bottomActionIcon.setImageResource(icon);
        action.bottomActionIcon.setImageTintList(ColorStateList.valueOf(color));
        action.bottomActionLabel.setText(label);
        action.bottomActionLabel.setTextColor(color);
        action.bottomActionRoot.setContentDescription(getString(label));
        action.bottomActionRoot.setOnClickListener(listener);
        if (action.bottomActionRoot.getStateListAnimator() == null) {
            action.bottomActionRoot.setStateListAnimator(
                    AnimatorInflater.loadStateListAnimator(this, R.animator.press_scale));
        }
    }

    private void callPrimary(boolean video) {
        if (state == null || state.contact == null || state.contact.phones.isEmpty()) {
            return;
        }
        chooseNumber(number -> {
            if (video) {
                WhatsAppService.videoCall(this, number);
            } else {
                PhoneService.call(this, number);
            }
        });
    }

    private void messagePrimary() {
        if (state == null || state.contact == null || state.contact.phones.isEmpty()) {
            return;
        }
        chooseNumber(number -> IntentUtils.openSms(this, number));
    }

    private interface NumberAction {
        void run(String number);
    }

    private void chooseNumber(NumberAction action) {
        List<LabeledValue> phones = state.contact.phones;
        if (phones.size() == 1) {
            action.run(phones.get(0).value);
            return;
        }
        List<AppBottomSheet.Option> options = new ArrayList<>();
        for (LabeledValue phone : phones) {
            options.add(new AppBottomSheet.Option(R.drawable.ic_call,
                    PhoneUtils.format(this, phone.value) + "  ·  " + ContactsService.phoneTypeLabel(this, phone),
                    () -> action.run(phone.value)));
        }
        AppBottomSheet.showOptions(this, getString(R.string.choose_number), options);
    }

    private void toggleFavorite() {
        if (busy || state == null || state.contact == null) {
            return;
        }
        busy = true;
        HapticUtils.confirm(binding.actionFavorite.bottomActionRoot);
        ContactModel contact = state.contact;
        boolean target = !contact.starred;
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            boolean ok = ContactsService.setStarred(app, contact.id, target);
            AppExecutors.main(() -> {
                busy = false;
                if (ok) {
                    contact.starred = target;
                    renderBottomBar(state);
                    bounce(binding.actionFavorite.bottomActionIcon);
                }
                Toast.makeText(app, ok ? (target ? R.string.added_favorite : R.string.removed_favorite)
                        : R.string.action_failed, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void toggleBlock() {
        if (busy || state == null || state.contact == null) {
            return;
        }
        HapticUtils.tap(binding.actionBlock.bottomActionRoot);
        ContactModel contact = state.contact;
        boolean unblock = state.blocked;
        int title = unblock ? R.string.unblock_contact_title : R.string.block_contact_title;
        int body = unblock ? R.string.unblock_contact_body : R.string.block_contact_body;
        ConfirmDialog.show(this, getString(title, contact.getDisplayName()), getString(body),
                unblock ? R.string.unblock : R.string.block, !unblock, R.drawable.ic_block, R.string.cancel,
                () -> showFullscreen(AdScreens.CONTACT_DETAILS_BLOCK, () -> {
                    busy = true;
                    Context app = getApplicationContext();
                    AppExecutors.io(() -> {
                        boolean allOk = true;
                        for (LabeledValue phone : contact.phones) {
                            boolean ok = unblock ? BlockService.unblock(app, phone.value)
                                    : BlockService.block(app, phone.value);
                            allOk &= ok;
                        }
                        boolean result = allOk;
                        AppExecutors.main(() -> {
                            busy = false;
                            if (result) {
                                bounce(binding.actionBlock.bottomActionIcon);
                                Toast.makeText(app, unblock ? R.string.number_unblocked : R.string.number_blocked,
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(app, unblock ? R.string.unblock_failed : R.string.block_failed,
                                        Toast.LENGTH_SHORT).show();
                            }
                            viewModel.reload();
                        });
                    });
                }));
    }

    private void confirmDelete() {
        if (state == null || state.contact == null) {
            return;
        }
        HapticUtils.tap(binding.actionDelete.bottomActionRoot);
        ContactModel contact = state.contact;
        ConfirmDialog.show(this, getString(R.string.delete_contact_title),
                getString(R.string.delete_contact_body, contact.getDisplayName()), R.string.delete, true,
                R.drawable.ic_delete, R.string.cancel,
                () -> showFullscreen(AdScreens.CONTACT_DETAILS_DELETE, () -> {
                    Context app = getApplicationContext();
                    AppExecutors.io(() -> {
                        boolean ok = ContactsService.deleteContact(app, contact.id);
                        AppExecutors.main(() -> {
                            Toast.makeText(app, ok ? R.string.contact_deleted : R.string.action_failed,
                                    Toast.LENGTH_SHORT).show();
                            if (ok) {
                                finish();
                            }
                        });
                    });
                }),
                () -> showFullscreen(AdScreens.CONTACT_DETAILS_DELETE_CANCEL, () -> {
                }));
    }

    private void openCallHistory(ContactModel contact, String screenKey) {
        showFullscreen(screenKey, () -> startActivity(CallHistoryActivity.contactIntent(this, contact.id, contact.lookupKey)));
    }

    private void showMoreMenu() {
        if (state == null || state.contact == null) {
            return;
        }
        List<AppBottomSheet.Option> options = new ArrayList<>();
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
            options.add(new AppBottomSheet.Option(R.drawable.ic_app_shortcut, getString(R.string.create_shortcut),
                    this::createShortcut));
        }
        if (!TextUtils.isEmpty(state.contact.lookupKey)) {
            options.add(new AppBottomSheet.Option(R.drawable.ic_share, getString(R.string.share_contact),
                    this::shareContact));
        }
        ContactModel contact = state.contact;
        options.add(new AppBottomSheet.Option(R.drawable.ic_history, getString(R.string.call_history),
                () -> openCallHistory(contact, AdScreens.CONTACT_DETAILS_CALL_HISTORY)));
        AppBottomSheet.showOptions(this, state.contact.getDisplayName(), options);
    }

    private void createShortcut() {
        ContactModel contact = state.contact;
        Context app = getApplicationContext();
        int size = getResources().getDimensionPixelSize(R.dimen.avatar_large);
        AppExecutors.io(() -> {
            Bitmap bitmap = AvatarBitmaps.create(app, contact.name, contact.photoUri, size);
            AppExecutors.main(() -> {
                Intent shortcutIntent = intent(app, contact.id, contact.lookupKey)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(app, "contact_" + contact.id)
                        .setShortLabel(contact.getDisplayName())
                        .setLongLabel(contact.getDisplayName())
                        .setIcon(IconCompat.createWithBitmap(bitmap))
                        .setIntent(shortcutIntent)
                        .build();
                boolean requested = ShortcutManagerCompat.requestPinShortcut(app, shortcut, null);
                if (!requested) {
                    Toast.makeText(app, R.string.shortcut_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void shareContact() {
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, state.contact.lookupKey);
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType(ContactsContract.Contacts.CONTENT_VCARD_TYPE)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_SUBJECT, state.contact.getDisplayName())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri(state.contact.getDisplayName(), uri));
        IntentUtils.safeStart(this, Intent.createChooser(intent, getString(R.string.share_contact)));
    }

    private void copy(String value) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), value));
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
        }
    }

    private static String joinNonEmpty(String first, String second) {
        boolean a = !TextUtils.isEmpty(first);
        boolean b = !TextUtils.isEmpty(second);
        if (a && b) {
            return first + " · " + second;
        }
        return a ? first : b ? second : null;
    }
}
