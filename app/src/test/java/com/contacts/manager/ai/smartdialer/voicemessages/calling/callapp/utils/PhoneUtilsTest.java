package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PhoneUtilsTest {

    @Test
    public void keyKeepsLastTenDigits() {
        assertEquals("8264683525", PhoneUtils.key("+91 82646 83525"));
        assertEquals("5551234", PhoneUtils.key("555-1234"));
        assertEquals("", PhoneUtils.key(null));
    }

    @Test
    public void sameNumberIgnoresFormattingAndCountryCode() {
        assertTrue(PhoneUtils.sameNumber("+91 8264683525", "08264683525"));
        assertTrue(PhoneUtils.sameNumber("(555) 123-4567", "5551234567"));
        assertFalse(PhoneUtils.sameNumber("5551234567", "5551234568"));
        assertFalse(PhoneUtils.sameNumber("", ""));
    }

    @Test
    public void toT9MapsLettersToKeys() {
        assertEquals("92445", PhoneUtils.toT9("Yagik"));
        assertEquals("56 5", PhoneUtils.toT9("Jo-l"));
        assertEquals("7777", PhoneUtils.toT9("pqrs"));
        assertEquals("9999", PhoneUtils.toT9("wxyz"));
        assertEquals("", PhoneUtils.toT9(null));
    }
}
