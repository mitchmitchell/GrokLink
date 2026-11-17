package com.mitchell.atak.groklink.core;

/**
 * Decides whether an incoming chat message is a reply from the bot.
 *
 * A message counts when its sender uid is the bot contact's uid, or, if
 * ATAK has not resolved the contact yet, when the sender callsign equals the
 * bot callsign. Callsigns are compared trimmed, because ATAK can send
 * trailing spaces (seen in captured traffic). Pure Java.
 */
public final class ReplyFilter {

    private ReplyFilter() {
    }

    public static boolean isFromBot(String botCallsign, String botUid,
            String senderCallsign, String senderUid) {
        if (botUid != null && senderUid != null && !botUid.isEmpty()
                && botUid.equals(senderUid))
            return true;
        if (botCallsign == null || senderCallsign == null)
            return false;
        String b = botCallsign.trim();
        return !b.isEmpty() && b.equals(senderCallsign.trim());
    }
}
