package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.AppBottomSheet;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.IntentUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.util.ArrayList;
import java.util.List;

public final class PhoneService {

    private static final int REQUEST_CALL_PERMISSION = 7001;

    private PhoneService() {
    }

    private static TelecomManager telecom(Context context) {
        return (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
    }

    public static boolean isDefaultDialer(Context context) {
        TelecomManager telecom = telecom(context);
        return telecom != null && context.getPackageName().equals(telecom.getDefaultDialerPackage());
    }

    public static Intent createDefaultDialerIntent(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = context.getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER);
            }
            return null;
        }
        Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
        intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.getPackageName());
        return intent;
    }

    public static boolean isScreeningRoleAvailable(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false;
        }
        RoleManager roleManager = context.getSystemService(RoleManager.class);
        return roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING);
    }

    public static boolean isScreeningRoleHeld(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false;
        }
        RoleManager roleManager = context.getSystemService(RoleManager.class);
        return roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
    }

    public static Intent createScreeningRoleIntent(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !isScreeningRoleAvailable(context)) {
            return null;
        }
        RoleManager roleManager = context.getSystemService(RoleManager.class);
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
    }

    public static boolean canPlaceCalls(Context context) {
        return PermissionManager.hasPermission(context, Manifest.permission.CALL_PHONE);
    }

    public static void call(Activity activity, String number) {
        call(activity, number, false);
    }

    public static void call(Activity activity, String number, boolean video) {
        if (!PhoneUtils.isValidNumber(number)) {
            Toast.makeText(activity, R.string.invalid_number, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!canPlaceCalls(activity)) {
            requestCallPermission(activity);
            return;
        }
        List<PhoneAccountHandle> accounts = getCallAccounts(activity);
        PhoneAccountHandle defaultAccount = getDefaultAccount(activity);
        if (accounts.size() > 1 && defaultAccount == null) {
            showSimChooser(activity, number, accounts, video);
        } else {
            placeCall(activity, number, null, video);
        }
    }

    private static void requestCallPermission(Activity activity) {
        PermissionManager.Status status = PermissionManager.getStatus(activity, PermissionManager.Group.PHONE);
        if (status == PermissionManager.Status.PERMANENTLY_DENIED) {
            Toast.makeText(activity, R.string.perm_denied_forever, Toast.LENGTH_LONG).show();
            PermissionManager.openAppSettings(activity);
            return;
        }
        PermissionManager.markRequested(PermissionManager.Group.PHONE);
        ActivityCompat.requestPermissions(activity, PermissionManager.Group.PHONE.permissions, REQUEST_CALL_PERMISSION);
    }

    private static void showSimChooser(Activity activity, String number, List<PhoneAccountHandle> accounts,
                                       boolean video) {
        TelecomManager telecom = telecom(activity);
        List<AppBottomSheet.Option> options = new ArrayList<>();
        for (PhoneAccountHandle handle : accounts) {
            CharSequence label = handle.getId();
            try {
                PhoneAccount account = telecom.getPhoneAccount(handle);
                if (account != null && !TextUtils.isEmpty(account.getLabel())) {
                    label = account.getLabel();
                }
            } catch (SecurityException ignored) {
            }
            options.add(new AppBottomSheet.Option(R.drawable.ic_sim_card, label,
                    () -> placeCall(activity, number, handle, video)));
        }
        AppBottomSheet.showOptions(activity, activity.getString(R.string.sim_choose), options);
    }

    public static List<PhoneAccountHandle> getCallAccounts(Context context) {
        TelecomManager telecom = telecom(context);
        if (telecom == null || !PermissionManager.hasPermission(context, Manifest.permission.READ_PHONE_STATE)) {
            return new ArrayList<>();
        }
        try {
            List<PhoneAccountHandle> accounts = telecom.getCallCapablePhoneAccounts();
            return accounts == null ? new ArrayList<>() : accounts;
        } catch (SecurityException e) {
            return new ArrayList<>();
        }
    }

    private static PhoneAccountHandle getDefaultAccount(Context context) {
        TelecomManager telecom = telecom(context);
        if (telecom == null || !PermissionManager.hasPermission(context, Manifest.permission.READ_PHONE_STATE)) {
            return null;
        }
        try {
            return telecom.getDefaultOutgoingPhoneAccount(PhoneAccount.SCHEME_TEL);
        } catch (SecurityException e) {
            return null;
        }
    }

    public static boolean supportsVideo(Context context) {
        TelecomManager telecom = telecom(context);
        if (telecom == null) {
            return false;
        }
        for (PhoneAccountHandle handle : getCallAccounts(context)) {
            try {
                PhoneAccount account = telecom.getPhoneAccount(handle);
                if (account != null && account.hasCapabilities(PhoneAccount.CAPABILITY_VIDEO_CALLING)) {
                    return true;
                }
            } catch (SecurityException ignored) {
            }
        }
        return false;
    }

    public static void placeCall(Context context, String number, PhoneAccountHandle handle, boolean video) {
        Uri uri = Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null);
        Bundle extras = new Bundle();
        if (handle != null) {
            extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle);
        }
        if (video) {
            extras.putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_BIDIRECTIONAL);
        }
        TelecomManager telecom = telecom(context);
        try {
            if (telecom == null) {
                throw new SecurityException();
            }
            telecom.placeCall(uri, extras);
        } catch (SecurityException e) {
            Intent intent = new Intent(Intent.ACTION_CALL, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (!IntentUtils.safeStart(context, intent)) {
                Toast.makeText(context, R.string.call_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    public static void cancelMissedCallsNotification(Context context) {
        TelecomManager telecom = telecom(context);
        if (telecom == null || !isDefaultDialer(context)) {
            return;
        }
        try {
            telecom.cancelMissedCallsNotification();
        } catch (SecurityException ignored) {
        }
    }
}
