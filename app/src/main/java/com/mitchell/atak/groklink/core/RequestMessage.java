package com.mitchell.atak.groklink.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the "request" message the bridge reads from the groklink element:
 *
 * <pre>
 * {"v": 1, "msg": "request", "snapshot_id": "...", "text": "...", "summary": {...}}
 * </pre>
 *
 * At share level Text only, snapshot_id and summary are left out. Pure Java.
 */
public final class RequestMessage {

    public static final int VERSION = 1;

    /** Longest request text accepted; matches the bridge's chat limit. */
    public static final int MAX_TEXT_CHARS = 2000;

    private RequestMessage() {
    }

    /**
     * @param text       what the user typed; trimmed, must not be empty
     * @param snapshotId the snapshot id, or null at share level Text only
     * @param summary    the summary object, or null at share level Text only
     */
    public static String build(String text, String snapshotId, Map<String, Object> summary) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty())
            throw new IllegalArgumentException("request text is empty");
        if (t.length() > MAX_TEXT_CHARS)
            throw new IllegalArgumentException(
                    "request text is over " + MAX_TEXT_CHARS + " characters");

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("v", VERSION);
        m.put("msg", "request");
        if (snapshotId != null)
            m.put("snapshot_id", snapshotId);
        m.put("text", t);
        if (summary != null)
            m.put("summary", summary);
        return Json.write(m);
    }
}
