package com.mitchell.atak.groklink.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public class RequestMessageTest {

    @Test
    public void textOnlyOmitsSnapshotAndSummary() {
        assertEquals("{\"v\":1,\"msg\":\"request\",\"text\":\"Where is the nearest river crossing?\"}",
                RequestMessage.build("  Where is the nearest river crossing?\n", null, null));
    }

    @Test
    public void includesSnapshotAndSummaryWhenGiven() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("detail", "none");
        assertEquals("{\"v\":1,\"msg\":\"request\",\"snapshot_id\":\"s1\",\"text\":\"hi\","
                + "\"summary\":{\"detail\":\"none\"}}",
                RequestMessage.build("hi", "s1", summary));
    }

    @Test
    public void escapesQuotesAndMarkup() {
        assertEquals("{\"v\":1,\"msg\":\"request\",\"text\":\"say \\\"\\u003cb\\u003e\\\" \\u0026 go\"}",
                RequestMessage.build("say \"<b>\" & go", null, null));
    }

    @Test
    public void rejectsEmptyAndOverLongText() {
        try {
            RequestMessage.build("   ", null, null);
            fail("empty text accepted");
        } catch (IllegalArgumentException expected) {
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= RequestMessage.MAX_TEXT_CHARS; i++)
            sb.append('x');
        try {
            RequestMessage.build(sb.toString(), null, null);
            fail("over-long text accepted");
        } catch (IllegalArgumentException expected) {
        }
    }
}
