package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.VoicemailContract.Voicemails;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.AppDialogs;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.TileRows;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityListScreenBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemTileRowBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.DateUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.util.ArrayList;
import java.util.List;

public class VoicemailActivity extends BaseActivity {

    private static class Voicemail {
        long id;
        String number;
        long date;
        long duration;
        boolean read;
        String transcription;
        String name;
        String photo;
    }

    private ActivityListScreenBinding binding;
    private MediaPlayer player;
    private long playingId = -1;
    private ImageButton playingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        binding.topBar.topTitle.setText(R.string.voicemail_messages);
        setupBack(binding.topBar.backButton);
        setupBackAd(AdScreens.VOICEMAIL_BACK);
        binding.info.infoIcon.setImageResource(R.drawable.ic_voicemail);
        binding.bottomBar.setVisibility(View.VISIBLE);
        binding.bottomButton.setText(R.string.voicemail_call);
        binding.bottomButton.setIconResource(R.drawable.ic_call);
        binding.bottomButton.setOnClickListener(v -> PhoneService.callVoicemail(this));
        showNativeBig(binding.ads.nativeBigContainer, AdScreens.VOICEMAIL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    @Override
    protected void onStop() {
        stopPlayback();
        super.onStop();
    }

    private void load() {
        boolean isDefault = PhoneService.isDefaultDialer(this);
        binding.info.getRoot().setVisibility(isDefault ? View.GONE : View.VISIBLE);
        binding.info.infoText.setText(R.string.voicemail_needs_default);
        binding.progress.setVisibility(View.VISIBLE);
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            List<Voicemail> items = query(app);
            AppExecutors.main(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    render(items);
                }
            });
        });
    }

    private static List<Voicemail> query(Context context) {
        List<Voicemail> items = new ArrayList<>();
        String[] projection = {Voicemails._ID, Voicemails.NUMBER, Voicemails.DATE, Voicemails.DURATION,
                Voicemails.IS_READ, Voicemails.TRANSCRIPTION};
        try (Cursor cursor = context.getContentResolver().query(Voicemails.CONTENT_URI, projection,
                Voicemails.DELETED + " = 0", null, Voicemails.DATE + " DESC")) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    Voicemail item = new Voicemail();
                    item.id = cursor.getLong(0);
                    item.number = cursor.getString(1);
                    item.date = cursor.getLong(2);
                    item.duration = cursor.getLong(3);
                    item.read = cursor.getInt(4) == 1;
                    item.transcription = cursor.getString(5);
                    ContactModel contact = ContactsService.canRead(context)
                            ? ContactsService.lookupNumber(context, item.number) : null;
                    if (contact != null) {
                        item.name = contact.name;
                        item.photo = contact.photoUri;
                    }
                    items.add(item);
                }
            }
        } catch (Exception ignored) {
        }
        return items;
    }

    private void render(List<Voicemail> items) {
        binding.progress.setVisibility(View.GONE);
        binding.listGroup.removeAllViews();
        binding.listGroup.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        if (items.isEmpty()) {
            binding.emptyState.setContent(R.drawable.ic_voicemail, R.string.voicemail_empty_title,
                    R.string.voicemail_empty_body);
            binding.emptyState.show(true);
            return;
        }
        binding.emptyState.show(false);
        for (Voicemail item : items) {
            boolean known = !TextUtils.isEmpty(item.name);
            String number = PhoneUtils.isPrivate(item.number) ? getString(R.string.private_number)
                    : PhoneUtils.format(this, item.number);
            StringBuilder subtitle = new StringBuilder(DateUtils.rowTime(this, item.date));
            if (item.duration > 0) {
                subtitle.append("  •  ").append(DateUtils.timer(item.duration));
            }
            if (!item.read) {
                subtitle.append("  •  ").append(getString(R.string.voicemail_new));
            }
            if (!TextUtils.isEmpty(item.transcription)) {
                subtitle.append('\n').append(item.transcription);
            }
            ItemTileRowBinding row = TileRows.add(binding.listGroup, known ? item.name : number, subtitle);
            TileRows.avatar(row, item.name, item.photo);
            ImageButton play = TileRows.action(row, R.drawable.ic_play, getString(R.string.voicemail_messages),
                    R.color.primary, null);
            play.setOnClickListener(v -> togglePlayback(item, play));
            if (!PhoneUtils.isPrivate(item.number)) {
                TileRows.action(row, R.drawable.ic_call, getString(R.string.call), R.color.primary,
                        v -> PhoneService.call(this, item.number));
            }
            TileRows.action(row, R.drawable.ic_delete, getString(R.string.delete), R.color.text_secondary,
                    v -> AppDialogs.confirm(this, getString(R.string.voicemail_delete_title), null,
                            R.string.delete, true, () -> delete(item)));
            row.tileRow.setOnClickListener(v -> togglePlayback(item, play));
        }
    }

    private void togglePlayback(Voicemail item, ImageButton button) {
        if (playingId == item.id) {
            stopPlayback();
            return;
        }
        stopPlayback();
        Uri uri = ContentUris.withAppendedId(Voicemails.CONTENT_URI, item.id);
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            player.setDataSource(this, uri);
            player.setOnCompletionListener(mp -> stopPlayback());
            player.setOnPreparedListener(MediaPlayer::start);
            player.prepareAsync();
            playingId = item.id;
            playingButton = button;
            button.setImageResource(R.drawable.ic_pause);
            markRead(item);
        } catch (Exception e) {
            stopPlayback();
            Toast.makeText(this, R.string.voicemail_play_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPlayback() {
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
            }
            player.release();
            player = null;
        }
        if (playingButton != null) {
            playingButton.setImageResource(R.drawable.ic_play);
            playingButton = null;
        }
        playingId = -1;
    }

    private void markRead(Voicemail item) {
        if (item.read) {
            return;
        }
        item.read = true;
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            ContentValues values = new ContentValues();
            values.put(Voicemails.IS_READ, 1);
            try {
                app.getContentResolver().update(ContentUris.withAppendedId(Voicemails.CONTENT_URI, item.id),
                        values, null, null);
            } catch (Exception ignored) {
            }
        });
    }

    private void delete(Voicemail item) {
        if (playingId == item.id) {
            stopPlayback();
        }
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            boolean ok;
            try {
                ok = app.getContentResolver().delete(ContentUris.withAppendedId(Voicemails.CONTENT_URI, item.id),
                        null, null) > 0;
            } catch (Exception e) {
                ok = false;
            }
            boolean deleted = ok;
            AppExecutors.main(() -> {
                if (!deleted) {
                    Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
                }
                load();
            });
        });
    }
}
