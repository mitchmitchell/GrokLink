package com.mitchell.atak.groklink.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The bot callsign setting. Stored as one comma-separated string so a second
 * bot only needs a picker later; this build uses the first entry. Pure Java.
 */
public final class BotSetting {

    public static final String DEFAULT = "GROK-1";

    /** TAK callsigns: letters, digits, space, dash, underscore, dot; 1 to 40. */
    private static final String VALID = "[A-Za-z0-9 _.\\-]{1,40}";

    private BotSetting() {
    }

    /** Splits, trims and drops empty or invalid entries, keeping order. */
    public static List<String> parse(String stored) {
        if (stored == null)
            return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (String part : stored.split(",")) {
            String p = part.trim();
            if (isValid(p) && !out.contains(p))
                out.add(p);
        }
        return out;
    }

    public static boolean isValid(String callsign) {
        return callsign != null && callsign.trim().matches(VALID)
                && !callsign.trim().isEmpty();
    }

    /** The bot this build talks to, or null when none is configured. */
    public static String primary(String stored) {
        List<String> all = parse(stored);
        return all.isEmpty() ? null : all.get(0);
    }
}
