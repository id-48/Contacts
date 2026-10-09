package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom;

import android.net.Uri;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

public class AppCallScreeningService extends CallScreeningService {

    @Override
    public void onScreenCall(Call.Details details) {
        CallResponse.Builder response = new CallResponse.Builder();
        if (isIncoming(details) && ((StorageService.isBlockUnknownEnabled() && isUnknownCaller(details))
                || (StorageService.isSpamShieldEnabled() && isSpam(details)))) {
            response.setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipNotification(true)
                    .setSkipCallLog(false);
        }
        respondToCall(details, response.build());
    }

    private boolean isIncoming(Call.Details details) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || details.getCallDirection() == Call.Details.DIRECTION_INCOMING;
    }

    private boolean isSpam(Call.Details details) {
        Uri handle = details.getHandle();
        String number = handle == null ? null : handle.getSchemeSpecificPart();
        if (!SpamShield.shouldBlock(number)) {
            return false;
        }
        return PhoneUtils.isPrivate(number) || !ContactsService.canRead(this)
                || ContactsService.lookupNumber(this, number) == null;
    }

    private boolean isUnknownCaller(Call.Details details) {
        Uri handle = details.getHandle();
        String number = handle == null ? null : handle.getSchemeSpecificPart();
        if (PhoneUtils.isPrivate(number)) {
            return true;
        }
        if (!ContactsService.canRead(this)) {
            return false;
        }
        return ContactsService.lookupNumber(this, number) == null;
    }
}
