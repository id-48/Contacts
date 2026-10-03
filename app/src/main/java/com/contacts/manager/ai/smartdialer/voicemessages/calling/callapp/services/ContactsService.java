package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.LabeledValue;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ContactsService {

    public static class AccountOption {
        public final String type;
        public final String name;
        public final String label;

        public AccountOption(String type, String name, String label) {
            this.type = type;
            this.name = name;
            this.label = label;
        }
    }

    private static final String[] LOCAL_ACCOUNT_TYPES = {
            "vnd.sec.contact.phone", "com.android.localphone", "com.oneplus.account", "com.xiaomi", "local"};
    private static final String[] SIM_ACCOUNT_TYPES = {
            "vnd.sec.contact.sim", "com.android.contacts.sim", "sim", "com.anddroid.contacts.sim"};

    private ContactsService() {
    }

    public static boolean canRead(Context context) {
        return PermissionManager.hasPermission(context, Manifest.permission.READ_CONTACTS);
    }

    public static boolean canWrite(Context context) {
        return PermissionManager.hasPermission(context, Manifest.permission.WRITE_CONTACTS);
    }

    public static List<ContactModel> getContacts(Context context) {
        List<ContactModel> contacts = new ArrayList<>();
        if (!canRead(context)) {
            return contacts;
        }
        ContentResolver resolver = context.getContentResolver();
        Map<Long, String> numbers = loadPrimaryNumbers(resolver);
        String[] projection = {Contacts._ID, Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME_PRIMARY,
                Contacts.PHOTO_THUMBNAIL_URI, Contacts.STARRED};
        try (Cursor cursor = resolver.query(Contacts.CONTENT_URI, projection, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    ContactModel contact = new ContactModel();
                    contact.id = cursor.getLong(0);
                    contact.lookupKey = cursor.getString(1);
                    contact.name = cursor.getString(2);
                    contact.photoUri = cursor.getString(3);
                    contact.starred = cursor.getInt(4) == 1;
                    contact.primaryNumber = numbers.get(contact.id);
                    if (!TextUtils.isEmpty(contact.name) || !TextUtils.isEmpty(contact.primaryNumber)) {
                        contacts.add(contact);
                    }
                }
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
        }
        sortByName(contacts);
        return contacts;
    }

    public static List<ContactModel> getFavorites(Context context) {
        List<ContactModel> favorites = new ArrayList<>();
        for (ContactModel contact : getContacts(context)) {
            if (contact.starred) {
                favorites.add(contact);
            }
        }
        return favorites;
    }

    private static Map<Long, String> loadPrimaryNumbers(ContentResolver resolver) {
        Map<Long, String> numbers = new HashMap<>();
        String[] projection = {Phone.CONTACT_ID, Phone.NUMBER, Phone.IS_SUPER_PRIMARY};
        try (Cursor cursor = resolver.query(Phone.CONTENT_URI, projection, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    String number = cursor.getString(1);
                    boolean primary = cursor.getInt(2) == 1;
                    if (primary || !numbers.containsKey(id)) {
                        numbers.put(id, number);
                    }
                }
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
        }
        return numbers;
    }

    private static void sortByName(List<ContactModel> contacts) {
        Collator collator = Collator.getInstance(Locale.getDefault());
        collator.setStrength(Collator.PRIMARY);
        Collections.sort(contacts, (a, b) -> {
            boolean aSymbol = "#".equals(a.getSectionLetter());
            boolean bSymbol = "#".equals(b.getSectionLetter());
            if (aSymbol != bSymbol) {
                return aSymbol ? -1 : 1;
            }
            return collator.compare(a.getDisplayName(), b.getDisplayName());
        });
    }

    public static long resolveContactId(Context context, long contactId, String lookupKey) {
        if (!TextUtils.isEmpty(lookupKey) && canRead(context)) {
            try {
                Uri lookupUri = Contacts.getLookupUri(contactId, lookupKey);
                Uri contactUri = Contacts.lookupContact(context.getContentResolver(), lookupUri);
                if (contactUri != null) {
                    return ContentUris.parseId(contactUri);
                }
            } catch (Exception ignored) {
            }
        }
        return contactId;
    }

    public static ContactModel getContactDetails(Context context, long contactId) {
        if (!canRead(context) || contactId <= 0) {
            return null;
        }
        ContentResolver resolver = context.getContentResolver();
        ContactModel contact = null;
        String[] projection = {Contacts._ID, Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME_PRIMARY,
                Contacts.PHOTO_THUMBNAIL_URI, Contacts.PHOTO_URI, Contacts.STARRED};
        try (Cursor cursor = resolver.query(ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                contact = new ContactModel();
                contact.id = cursor.getLong(0);
                contact.lookupKey = cursor.getString(1);
                contact.name = cursor.getString(2);
                contact.photoUri = cursor.getString(3);
                contact.fullPhotoUri = cursor.getString(4);
                contact.starred = cursor.getInt(5) == 1;
            }
        } catch (SecurityException | IllegalArgumentException e) {
            return null;
        }
        if (contact == null) {
            return null;
        }
        loadData(resolver, contact, Data.CONTACT_ID + "=?", new String[]{String.valueOf(contactId)}, false);
        if (!contact.phones.isEmpty()) {
            contact.primaryNumber = contact.phones.get(0).value;
        }
        return contact;
    }

    public static ContactModel getContactForEdit(Context context, long contactId) {
        ContactModel contact = getContactDetails(context, contactId);
        if (contact == null) {
            return null;
        }
        long rawId = findEditableRawContact(context.getContentResolver(), contactId);
        contact.rawContactId = rawId;
        contact.phones.clear();
        contact.emails.clear();
        contact.company = null;
        contact.jobTitle = null;
        contact.address = null;
        contact.birthday = null;
        contact.nickname = null;
        contact.note = null;
        if (rawId > 0) {
            loadData(context.getContentResolver(), contact, Data.RAW_CONTACT_ID + "=?",
                    new String[]{String.valueOf(rawId)}, true);
        }
        return contact;
    }

    private static long findEditableRawContact(ContentResolver resolver, long contactId) {
        String[] projection = {RawContacts._ID, RawContacts.ACCOUNT_TYPE, RawContacts.ACCOUNT_NAME};
        long fallback = -1;
        try (Cursor cursor = resolver.query(RawContacts.CONTENT_URI, projection,
                RawContacts.CONTACT_ID + "=? AND " + RawContacts.DELETED + "=0",
                new String[]{String.valueOf(contactId)}, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    String type = cursor.getString(1);
                    if (fallback < 0) {
                        fallback = id;
                    }
                    if (type == null || isLocalAccount(type) || "com.google".equals(type)) {
                        return id;
                    }
                }
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
        }
        return fallback;
    }

    private static void loadData(ContentResolver resolver, ContactModel contact, String selection,
                                 String[] args, boolean collectIds) {
        String[] projection = {Data._ID, Data.MIMETYPE, Data.DATA1, Data.DATA2, Data.DATA3, Data.DATA4,
                Data.DATA5, Data.DATA6, Data.RAW_CONTACT_ID, Data.IS_SUPER_PRIMARY};
        LinkedHashMap<String, LabeledValue> phones = new LinkedHashMap<>();
        try (Cursor cursor = resolver.query(Data.CONTENT_URI, projection, selection, args,
                Data.IS_SUPER_PRIMARY + " DESC")) {
            if (cursor == null) {
                return;
            }
            while (cursor.moveToNext()) {
                long dataId = cursor.getLong(0);
                String mime = cursor.getString(1);
                String data1 = cursor.getString(2);
                if (mime == null) {
                    continue;
                }
                boolean editable = true;
                switch (mime) {
                    case StructuredName.CONTENT_ITEM_TYPE:
                        contact.firstName = cursor.getString(3);
                        contact.lastName = cursor.getString(4);
                        contact.prefix = cursor.getString(5);
                        contact.middleName = cursor.getString(6);
                        contact.suffix = cursor.getString(7);
                        if (collectIds && contact.rawContactId <= 0) {
                            contact.rawContactId = cursor.getLong(8);
                        }
                        break;
                    case Phone.CONTENT_ITEM_TYPE:
                        if (!TextUtils.isEmpty(data1)) {
                            String key = PhoneUtils.key(data1);
                            if (collectIds || !phones.containsKey(key)) {
                                phones.put(collectIds ? String.valueOf(dataId) : key,
                                        new LabeledValue(data1, cursor.getInt(3), cursor.getString(4)));
                            }
                        }
                        break;
                    case Email.CONTENT_ITEM_TYPE:
                        if (!TextUtils.isEmpty(data1)) {
                            contact.emails.add(new LabeledValue(data1, cursor.getInt(3), cursor.getString(4)));
                        }
                        break;
                    case Organization.CONTENT_ITEM_TYPE:
                        if (contact.company == null) {
                            contact.company = data1;
                            contact.jobTitle = cursor.getString(5);
                        } else {
                            editable = false;
                        }
                        break;
                    case StructuredPostal.CONTENT_ITEM_TYPE:
                        if (contact.address == null) {
                            contact.address = data1;
                        } else {
                            editable = false;
                        }
                        break;
                    case Event.CONTENT_ITEM_TYPE:
                        if (cursor.getInt(3) == Event.TYPE_BIRTHDAY && contact.birthday == null) {
                            contact.birthday = data1;
                        } else {
                            editable = false;
                        }
                        break;
                    case Nickname.CONTENT_ITEM_TYPE:
                        if (contact.nickname == null) {
                            contact.nickname = data1;
                        } else {
                            editable = false;
                        }
                        break;
                    case Note.CONTENT_ITEM_TYPE:
                        if (contact.note == null && !TextUtils.isEmpty(data1)) {
                            contact.note = data1;
                        } else {
                            editable = false;
                        }
                        break;
                    default:
                        editable = false;
                        break;
                }
                if (collectIds && editable) {
                    contact.editableDataIds.add(dataId);
                }
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
        }
        contact.phones.addAll(phones.values());
    }

    public static ContactModel lookupNumber(Context context, String number) {
        if (!canRead(context) || TextUtils.isEmpty(number)) {
            return null;
        }
        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String[] projection = {PhoneLookup._ID, PhoneLookup.LOOKUP_KEY, PhoneLookup.DISPLAY_NAME,
                PhoneLookup.PHOTO_THUMBNAIL_URI, PhoneLookup.STARRED};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                ContactModel contact = new ContactModel();
                contact.id = cursor.getLong(0);
                contact.lookupKey = cursor.getString(1);
                contact.name = cursor.getString(2);
                contact.photoUri = cursor.getString(3);
                contact.starred = cursor.getInt(4) == 1;
                contact.primaryNumber = number;
                return contact;
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
        }
        return null;
    }

    public static Map<String, ContactModel> getNumberIndex(Context context) {
        Map<String, ContactModel> index = new HashMap<>();
        if (!canRead(context)) {
            return index;
        }
        String[] projection = {Phone.CONTACT_ID, Phone.LOOKUP_KEY, Phone.DISPLAY_NAME_PRIMARY,
                Phone.PHOTO_THUMBNAIL_URI, Phone.STARRED, Phone.NUMBER};
        try (Cursor cursor = context.getContentResolver().query(Phone.CONTENT_URI, projection, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String number = cursor.getString(5);
                    String key = PhoneUtils.key(number);
                    if (key.length() < 3 || index.containsKey(key)) {
                        continue;
                    }
                    ContactModel contact = new ContactModel();
                    contact.id = cursor.getLong(0);
                    contact.lookupKey = cursor.getString(1);
                    contact.name = cursor.getString(2);
                    contact.photoUri = cursor.getString(3);
                    contact.starred = cursor.getInt(4) == 1;
                    contact.primaryNumber = number;
                    index.put(key, contact);
                }
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
        }
        return index;
    }

    public static boolean setStarred(Context context, long contactId, boolean starred) {
        if (!canWrite(context)) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(Contacts.STARRED, starred ? 1 : 0);
        try {
            return context.getContentResolver().update(ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                    values, null, null) > 0;
        } catch (SecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean deleteContact(Context context, long contactId) {
        if (!canWrite(context)) {
            return false;
        }
        try {
            return context.getContentResolver().delete(
                    ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId), null, null) > 0;
        } catch (SecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    public static List<AccountOption> getAccounts(Context context) {
        List<AccountOption> options = new ArrayList<>();
        options.add(new AccountOption(null, null, context.getString(R.string.device_account)));
        Map<String, AccountOption> found = new LinkedHashMap<>();
        if (canRead(context)) {
            String[] projection = {RawContacts.ACCOUNT_TYPE, RawContacts.ACCOUNT_NAME};
            try (Cursor cursor = context.getContentResolver().query(RawContacts.CONTENT_URI, projection,
                    RawContacts.DELETED + "=0", null, null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String type = cursor.getString(0);
                        String name = cursor.getString(1);
                        addAccountOption(found, type, name);
                    }
                }
            } catch (SecurityException | IllegalArgumentException ignored) {
            }
        }
        try {
            for (Account account : AccountManager.get(context).getAccountsByType("com.google")) {
                addAccountOption(found, account.type, account.name);
            }
        } catch (SecurityException ignored) {
        }
        options.addAll(found.values());
        return options;
    }

    private static void addAccountOption(Map<String, AccountOption> found, String type, String name) {
        if (type == null || TextUtils.isEmpty(name) || isLocalAccount(type) || isSimAccount(type)) {
            return;
        }
        if (!"com.google".equals(type) && !type.contains("exchange") && !type.contains("eas")) {
            return;
        }
        String key = type + "/" + name;
        if (!found.containsKey(key)) {
            found.put(key, new AccountOption(type, name, name));
        }
    }

    private static boolean isLocalAccount(String type) {
        for (String local : LOCAL_ACCOUNT_TYPES) {
            if (local.equals(type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSimAccount(String type) {
        for (String sim : SIM_ACCOUNT_TYPES) {
            if (sim.equals(type)) {
                return true;
            }
        }
        return false;
    }

    public static long saveContact(Context context, ContactModel model) {
        if (!canWrite(context)) {
            return -1;
        }
        ContentResolver resolver = context.getContentResolver();
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        boolean isNew = model.rawContactId <= 0;
        if (isNew) {
            ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                    .withValue(RawContacts.ACCOUNT_TYPE, model.accountType)
                    .withValue(RawContacts.ACCOUNT_NAME, model.accountName)
                    .build());
        } else if (!model.editableDataIds.isEmpty()) {
            StringBuilder placeholders = new StringBuilder();
            String[] args = new String[model.editableDataIds.size()];
            for (int i = 0; i < args.length; i++) {
                placeholders.append(i == 0 ? "?" : ",?");
                args[i] = String.valueOf(model.editableDataIds.get(i));
            }
            ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
                    .withSelection(Data._ID + " IN (" + placeholders + ")", args)
                    .build());
        }

        ContentValues name = new ContentValues();
        name.put(StructuredName.GIVEN_NAME, emptyToNull(model.firstName));
        name.put(StructuredName.MIDDLE_NAME, emptyToNull(model.middleName));
        name.put(StructuredName.FAMILY_NAME, emptyToNull(model.lastName));
        name.put(StructuredName.PREFIX, emptyToNull(model.prefix));
        name.put(StructuredName.SUFFIX, emptyToNull(model.suffix));
        addData(ops, isNew, model.rawContactId, StructuredName.CONTENT_ITEM_TYPE, name);

        for (LabeledValue phone : model.phones) {
            if (TextUtils.isEmpty(phone.value)) {
                continue;
            }
            ContentValues values = new ContentValues();
            values.put(Phone.NUMBER, phone.value.trim());
            values.put(Phone.TYPE, phone.type);
            if (phone.type == Phone.TYPE_CUSTOM) {
                values.put(Phone.LABEL, phone.label);
            }
            addData(ops, isNew, model.rawContactId, Phone.CONTENT_ITEM_TYPE, values);
        }
        for (LabeledValue email : model.emails) {
            if (TextUtils.isEmpty(email.value)) {
                continue;
            }
            ContentValues values = new ContentValues();
            values.put(Email.ADDRESS, email.value.trim());
            values.put(Email.TYPE, email.type);
            if (email.type == Email.TYPE_CUSTOM) {
                values.put(Email.LABEL, email.label);
            }
            addData(ops, isNew, model.rawContactId, Email.CONTENT_ITEM_TYPE, values);
        }
        if (!TextUtils.isEmpty(model.company) || !TextUtils.isEmpty(model.jobTitle)) {
            ContentValues values = new ContentValues();
            values.put(Organization.COMPANY, emptyToNull(model.company));
            values.put(Organization.TITLE, emptyToNull(model.jobTitle));
            values.put(Organization.TYPE, Organization.TYPE_WORK);
            addData(ops, isNew, model.rawContactId, Organization.CONTENT_ITEM_TYPE, values);
        }
        if (!TextUtils.isEmpty(model.address)) {
            ContentValues values = new ContentValues();
            values.put(StructuredPostal.FORMATTED_ADDRESS, model.address.trim());
            values.put(StructuredPostal.TYPE, StructuredPostal.TYPE_HOME);
            addData(ops, isNew, model.rawContactId, StructuredPostal.CONTENT_ITEM_TYPE, values);
        }
        if (!TextUtils.isEmpty(model.birthday)) {
            ContentValues values = new ContentValues();
            values.put(Event.START_DATE, model.birthday);
            values.put(Event.TYPE, Event.TYPE_BIRTHDAY);
            addData(ops, isNew, model.rawContactId, Event.CONTENT_ITEM_TYPE, values);
        }
        if (!TextUtils.isEmpty(model.nickname)) {
            ContentValues values = new ContentValues();
            values.put(Nickname.NAME, model.nickname.trim());
            addData(ops, isNew, model.rawContactId, Nickname.CONTENT_ITEM_TYPE, values);
        }
        if (!TextUtils.isEmpty(model.note)) {
            ContentValues values = new ContentValues();
            values.put(Note.NOTE, model.note.trim());
            addData(ops, isNew, model.rawContactId, Note.CONTENT_ITEM_TYPE, values);
        }
        if (!isNew && (model.photoBytes != null || model.removePhoto)) {
            ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
                    .withSelection(Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?",
                            new String[]{String.valueOf(model.rawContactId), Photo.CONTENT_ITEM_TYPE})
                    .build());
        }
        if (model.photoBytes != null) {
            ContentValues values = new ContentValues();
            values.put(Photo.PHOTO, model.photoBytes);
            values.put(Data.IS_SUPER_PRIMARY, 1);
            addData(ops, isNew, model.rawContactId, Photo.CONTENT_ITEM_TYPE, values);
        }

        try {
            ContentProviderResult[] results = resolver.applyBatch(ContactsContract.AUTHORITY, ops);
            if (!isNew) {
                return model.id;
            }
            if (results.length == 0 || results[0].uri == null) {
                return -1;
            }
            long rawId = ContentUris.parseId(results[0].uri);
            return contactIdForRaw(resolver, rawId);
        } catch (Exception e) {
            return -1;
        }
    }

    private static long contactIdForRaw(ContentResolver resolver, long rawId) {
        try (Cursor cursor = resolver.query(ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawId),
                new String[]{RawContacts.CONTACT_ID}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
        }
        return -1;
    }

    private static void addData(List<ContentProviderOperation> ops, boolean isNew, long rawId,
                                String mime, ContentValues values) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
        if (isNew) {
            builder.withValueBackReference(Data.RAW_CONTACT_ID, 0);
        } else {
            builder.withValue(Data.RAW_CONTACT_ID, rawId);
        }
        builder.withValue(Data.MIMETYPE, mime);
        builder.withValues(values);
        ops.add(builder.build());
    }

    private static String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String phoneTypeLabel(Context context, LabeledValue value) {
        if (value.type == Phone.TYPE_CUSTOM && !TextUtils.isEmpty(value.label)) {
            return value.label;
        }
        switch (value.type) {
            case Phone.TYPE_HOME:
                return context.getString(R.string.type_home);
            case Phone.TYPE_WORK:
                return context.getString(R.string.type_work);
            case Phone.TYPE_MAIN:
                return context.getString(R.string.type_main);
            case Phone.TYPE_MOBILE:
                return context.getString(R.string.type_mobile);
            default:
                return context.getString(R.string.type_other);
        }
    }

    public static String emailTypeLabel(Context context, LabeledValue value) {
        if (value.type == Email.TYPE_CUSTOM && !TextUtils.isEmpty(value.label)) {
            return value.label;
        }
        switch (value.type) {
            case Email.TYPE_HOME:
                return context.getString(R.string.type_home);
            case Email.TYPE_WORK:
                return context.getString(R.string.type_work);
            case Email.TYPE_MOBILE:
                return context.getString(R.string.type_mobile);
            default:
                return context.getString(R.string.type_other);
        }
    }
}
