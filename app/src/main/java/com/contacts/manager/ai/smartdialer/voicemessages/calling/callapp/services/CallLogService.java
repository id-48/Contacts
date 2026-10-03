package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog.Calls;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.CallGroupModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.CallModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.DateUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CallLogService {

    private static final String[] PROJECTION = {Calls._ID, Calls.NUMBER, Calls.TYPE, Calls.DATE,
            Calls.DURATION, Calls.CACHED_NAME, Calls.CACHED_PHOTO_URI};

    private CallLogService() {
    }

    public static boolean canRead(Context context) {
        return PermissionManager.hasPermission(context, Manifest.permission.READ_CALL_LOG);
    }

    public static boolean canWrite(Context context) {
        return PermissionManager.hasPermission(context, Manifest.permission.WRITE_CALL_LOG);
    }

    public static List<CallModel> getRecentCalls(Context context, int limit) {
        List<CallModel> calls = query(context, null, null, limit);
        resolveContacts(context, calls);
        return calls;
    }

    private static List<CallModel> query(Context context, String selection, String[] args, int limit) {
        List<CallModel> calls = new ArrayList<>();
        if (!canRead(context)) {
            return calls;
        }
        try (Cursor cursor = context.getContentResolver().query(Calls.CONTENT_URI, PROJECTION, selection, args,
                Calls.DATE + " DESC")) {
            if (cursor != null) {
                while (cursor.moveToNext() && calls.size() < limit) {
                    calls.add(read(cursor));
                }
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
        }
        return calls;
    }

    private static List<CallModel> queryForKeys(Context context, List<String> keys, int limit) {
        StringBuilder selection = new StringBuilder();
        List<String> args = new ArrayList<>();
        for (String key : keys) {
            if (key.isEmpty()) {
                continue;
            }
            if (selection.length() > 0) {
                selection.append(" OR ");
            }
            selection.append(Calls.NUMBER).append(" LIKE ?");
            args.add("%" + key.substring(Math.max(0, key.length() - 7)));
        }
        List<CallModel> result = new ArrayList<>();
        if (args.isEmpty()) {
            return result;
        }
        for (CallModel call : query(context, selection.toString(), args.toArray(new String[0]), Integer.MAX_VALUE)) {
            if (keys.contains(PhoneUtils.key(call.number))) {
                result.add(call);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        resolveContacts(context, result);
        return result;
    }

    public static List<CallModel> getCallsForNumber(Context context, String number) {
        String key = PhoneUtils.key(number);
        if (key.isEmpty()) {
            List<CallModel> calls = query(context, Calls.NUMBER + "=?", new String[]{number == null ? "" : number},
                    Integer.MAX_VALUE);
            resolveContacts(context, calls);
            return calls;
        }
        List<String> keys = new ArrayList<>();
        keys.add(key);
        return queryForKeys(context, keys, Integer.MAX_VALUE);
    }

    public static List<CallModel> getCallsForContact(Context context, ContactModel contact, int limit) {
        if (contact == null || contact.phones.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < contact.phones.size(); i++) {
            keys.add(PhoneUtils.key(contact.phones.get(i).value));
        }
        return queryForKeys(context, keys, limit);
    }

    private static CallModel read(Cursor cursor) {
        CallModel call = new CallModel();
        call.id = cursor.getLong(0);
        call.number = cursor.getString(1);
        call.type = cursor.getInt(2);
        call.date = cursor.getLong(3);
        call.duration = cursor.getLong(4);
        call.name = cursor.getString(5);
        call.photoUri = cursor.getString(6);
        return call;
    }

    private static void resolveContacts(Context context, List<CallModel> calls) {
        if (!ContactsService.canRead(context)) {
            return;
        }
        boolean bulk = calls.size() > 40;
        Map<String, ContactModel> index = bulk ? ContactsService.getNumberIndex(context) : null;
        Map<String, ContactModel> cache = new HashMap<>();
        for (CallModel call : calls) {
            if (PhoneUtils.isPrivate(call.number)) {
                continue;
            }
            String key = PhoneUtils.key(call.number);
            ContactModel contact;
            if (bulk) {
                contact = index.get(key);
            } else if (cache.containsKey(key)) {
                contact = cache.get(key);
            } else {
                contact = ContactsService.lookupNumber(context, call.number);
                cache.put(key, contact);
            }
            if (contact != null) {
                call.name = contact.name;
                call.photoUri = contact.photoUri;
                call.contactId = contact.id;
                call.lookupKey = contact.lookupKey;
            } else {
                call.name = null;
                call.photoUri = null;
            }
        }
    }

    public static List<CallGroupModel> group(List<CallModel> calls, boolean missedOnly) {
        List<CallGroupModel> groups = new ArrayList<>();
        CallGroupModel current = null;
        for (CallModel call : calls) {
            if (missedOnly && !call.isMissed()) {
                continue;
            }
            if (current != null
                    && PhoneUtils.sameNumber(current.getNumber(), call.number)
                    && DateUtils.isSameDay(current.getLatest().date, call.date)) {
                current.calls.add(call);
            } else {
                current = new CallGroupModel(call);
                groups.add(current);
            }
        }
        return groups;
    }

    public static boolean deleteCalls(Context context, List<CallModel> calls) {
        if (!canWrite(context) || calls.isEmpty()) {
            return false;
        }
        StringBuilder placeholders = new StringBuilder();
        String[] args = new String[calls.size()];
        for (int i = 0; i < calls.size(); i++) {
            placeholders.append(i == 0 ? "?" : ",?");
            args[i] = String.valueOf(calls.get(i).id);
        }
        try {
            return context.getContentResolver().delete(Calls.CONTENT_URI,
                    Calls._ID + " IN (" + placeholders + ")", args) > 0;
        } catch (SecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    public static List<CallModel> getNewMissedCalls(Context context) {
        List<CallModel> calls = new ArrayList<>();
        if (!canRead(context)) {
            return calls;
        }
        String selection = Calls.TYPE + "=? AND " + Calls.NEW + "=1";
        try (Cursor cursor = context.getContentResolver().query(Calls.CONTENT_URI, PROJECTION, selection,
                new String[]{String.valueOf(Calls.MISSED_TYPE)}, Calls.DATE + " DESC")) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    calls.add(read(cursor));
                }
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
        }
        resolveContacts(context, calls);
        return calls;
    }

    public static void markMissedCallsRead(Context context) {
        if (!canWrite(context)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(Calls.NEW, 0);
        values.put(Calls.IS_READ, 1);
        try {
            context.getContentResolver().update(Calls.CONTENT_URI, values,
                    Calls.TYPE + "=? AND " + Calls.NEW + "=1",
                    new String[]{String.valueOf(Calls.MISSED_TYPE)});
        } catch (SecurityException | IllegalArgumentException ignored) {
        }
    }

    public static int typeLabel(int type) {
        switch (type) {
            case Calls.OUTGOING_TYPE:
                return R.string.call_outgoing;
            case Calls.MISSED_TYPE:
                return R.string.call_missed;
            case Calls.REJECTED_TYPE:
                return R.string.call_rejected;
            case Calls.BLOCKED_TYPE:
                return R.string.call_blocked;
            case Calls.VOICEMAIL_TYPE:
                return R.string.call_voicemail;
            default:
                return R.string.call_incoming;
        }
    }

    public static int typeLabelLong(int type) {
        switch (type) {
            case Calls.OUTGOING_TYPE:
                return R.string.outgoing_call;
            case Calls.MISSED_TYPE:
                return R.string.missed_call;
            case Calls.INCOMING_TYPE:
                return R.string.incoming_call;
            default:
                return typeLabel(type);
        }
    }

    public static int typeArrowIcon(int type) {
        return type == Calls.MISSED_TYPE ? R.drawable.ic_call_missed_arrow : typeIcon(type);
    }

    public static int typeIcon(int type) {
        switch (type) {
            case Calls.OUTGOING_TYPE:
                return R.drawable.ic_call_made;
            case Calls.MISSED_TYPE:
                return R.drawable.ic_call_missed;
            case Calls.REJECTED_TYPE:
                return R.drawable.ic_do_not_disturb_on;
            case Calls.BLOCKED_TYPE:
                return R.drawable.ic_block;
            case Calls.VOICEMAIL_TYPE:
                return R.drawable.ic_voicemail;
            default:
                return R.drawable.ic_call_received;
        }
    }

    public static int typeColor(int type) {
        switch (type) {
            case Calls.OUTGOING_TYPE:
                return R.color.call_outgoing;
            case Calls.MISSED_TYPE:
                return R.color.call_missed;
            case Calls.REJECTED_TYPE:
            case Calls.BLOCKED_TYPE:
                return R.color.call_rejected;
            default:
                return R.color.call_incoming;
        }
    }
}
