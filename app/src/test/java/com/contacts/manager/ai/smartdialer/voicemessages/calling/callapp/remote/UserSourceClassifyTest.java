package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote;

import static org.junit.Assert.assertEquals;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.AppConstants;

import org.junit.Test;

public class UserSourceClassifyTest {

    private static void organic(String referrer) {
        assertEquals(referrer, AppConstants.SOURCE_ORGANIC, UserSourceManager.classify(referrer));
    }

    private static void marketing(String referrer) {
        assertEquals(referrer, AppConstants.SOURCE_MARKETING, UserSourceManager.classify(referrer));
    }

    @Test
    public void playStoreDefaultsAreOrganic() {
        organic(null);
        organic("");
        organic("utm_source=google-play&utm_medium=organic");
        organic("utm_source=(not%20set)&utm_medium=(not%20set)");
        organic("utm_source%3Dgoogle-play%26utm_medium%3Dorganic");
    }

    @Test
    public void adClickIdsAreMarketing() {
        marketing("gclid=Cj0KCQ&utm_source=google-play");
        marketing("utm_source=google-play&fbclid=IwAR0");
    }

    @Test
    public void paidMediumOrCampaignSourceIsMarketing() {
        marketing("utm_source=google&utm_medium=cpc");
        marketing("utm_source=newsletter&utm_medium=email");
        marketing("utm_source%3Dfacebook_ads%26utm_medium%3Dpaid");
    }

    @Test
    public void metaInstallReferrerIsMarketing() {
        marketing("utm_source=apps.facebook.com&utm_campaign=fb4a&utm_content=%7B%22app%22%3A123%7D");
    }
}
