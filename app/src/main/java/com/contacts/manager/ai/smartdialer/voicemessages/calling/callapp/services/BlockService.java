package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BlockedNumberContract;
import android.provider.BlockedNumberContract.BlockedNumbers;
import android.text.TextUtils;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.BlockedNumberModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;

import java.util.ArrayList;
import java.util.List;

public final class BlockService {

    private BlockService() {
    }

    public static boolean canBlock(Context context) {
        if (!PhoneService.isDefaultDialer(context)) {
            return false;
        }
        try {
            return BlockedNumberContract.canCurrentUserBlockNumbers(context);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isBlocked(Context context, String number) {
        if (TextUtils.isEmpty(number) || !canBlock(context)) {
            return false;
        }
        try {
            return BlockedNumberContract.isBlocked(context, number);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean block(Context context, String number) {
        if (TextUtils.isEmpty(number) || !canBlock(context)) {
            return false;
        }
        try {
            if (BlockedNumberContract.isBlocked(context, number)) {
                return true;
            }
            ContentValues values = new ContentValues();
            values.put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number);
            Uri uri = context.getContentResolver().insert(BlockedNumbers.CONTENT_URI, values);
            return uri != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean unblock(Context context, String number) {
        if (TextUtils.isEmpty(number) || !canBlock(context)) {
            return false;
        }
        try {
            return BlockedNumberContract.unblock(context, number) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static List<BlockedNumberModel> getBlockedNumbers(Context context) {
        List<BlockedNumberModel> list = new ArrayList<>();
        if (!canBlock(context)) {
            return list;
        }
        String[] projection = {BlockedNumbers.COLUMN_ID, BlockedNumbers.COLUMN_ORIGINAL_NUMBER};
        try (Cursor cursor = context.getContentResolver().query(BlockedNumbers.CONTENT_URI, projection,
                null, null, BlockedNumbers.COLUMN_ID + " DESC")) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    BlockedNumberModel model = new BlockedNumberModel();
                    model.id = cursor.getLong(0);
                    model.number = cursor.getString(1);
                    ContactModel contact = ContactsService.lookupNumber(context, model.number);
                    if (contact != null) {
                        model.name = contact.name;
                    }
                    list.add(model);
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }
}
