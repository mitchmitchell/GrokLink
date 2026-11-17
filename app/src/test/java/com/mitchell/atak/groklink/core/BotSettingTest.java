package com.mitchell.atak.groklink.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class BotSettingTest {

    @Test
    public void parsesTrimsAndDropsDuplicatesAndInvalid() {
        assertEquals(Arrays.asList("GROK-1", "GROK-2"),
                BotSetting.parse(" GROK-1 , ,GROK-2,GROK-1, bad;name "));
    }

    @Test
    public void primaryIsFirstValidEntry() {
        assertEquals("GROK-1", BotSetting.primary("GROK-1,GROK-2"));
        assertNull(BotSetting.primary(""));
        assertNull(BotSetting.primary(null));
        assertNull(BotSetting.primary("<script>"));
    }

    @Test
    public void validatesCallsigns() {
        assertTrue(BotSetting.isValid("GROK-1"));
        assertTrue(BotSetting.isValid("Bot one.2_a"));
        assertFalse(BotSetting.isValid(""));
        assertFalse(BotSetting.isValid("a\"b"));
        assertFalse(BotSetting.isValid("12345678901234567890123456789012345678901"));
    }
}
