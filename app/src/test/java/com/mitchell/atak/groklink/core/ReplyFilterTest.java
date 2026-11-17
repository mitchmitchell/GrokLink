package com.mitchell.atak.groklink.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReplyFilterTest {

    @Test
    public void matchesOnUid() {
        assertTrue(ReplyFilter.isFromBot("GROK-1", "takbridge-GROK-1", "anything",
                "takbridge-GROK-1"));
    }

    @Test
    public void matchesOnTrimmedCallsignWhenUidUnknown() {
        assertTrue(ReplyFilter.isFromBot("GROK-1", null, "GROK-1 ", "x"));
        assertTrue(ReplyFilter.isFromBot(" GROK-1", "", "GROK-1", null));
    }

    @Test
    public void ignoresOthers() {
        assertFalse(ReplyFilter.isFromBot("GROK-1", "takbridge-GROK-1", "AE5HO/TAK-FOLD",
                "ANDROID-phone"));
        assertFalse(ReplyFilter.isFromBot("GROK-1", null, "GROK-10", null));
        assertFalse(ReplyFilter.isFromBot("", null, "", null));
        assertFalse(ReplyFilter.isFromBot(null, null, null, null));
    }
}
