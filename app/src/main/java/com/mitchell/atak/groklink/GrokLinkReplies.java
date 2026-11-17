package com.mitchell.atak.groklink;

import android.os.Bundle;
import android.util.Log;

import com.atakmap.android.chat.ChatManagerMapComponent;
import com.atakmap.android.contact.Contact;
import com.mitchell.atak.groklink.core.ReplyFilter;

/**
 * Shows the bot's chat replies in the GrokLink panel.
 *
 * ATAK integration point: ChatManagerMapComponent.addChatMessageListener
 * (ATAK-CIV 5.8 SDK, main.jar; its javadoc calls it the replacement for
 * listening to chat intents). ATAK hands each incoming message to the
 * listener as a copy of the chat Bundle, so the message still appears in
 * ATAK's own chat as usual; GrokLink only reads it.
 *
 * Bundle keys used (string constants in ChatManagerMapComponent and
 * GeoChatService): senderUid, senderCallsign, message.
 */
final class GrokLinkReplies implements ChatManagerMapComponent.ChatMessageListener {

    private static final String TAG = "GrokLinkReplies";

    interface Sink {
        String botCallsign();

        void onReply(String botCallsign, String text);
    }

    private final Sink sink;
    private boolean registered;

    GrokLinkReplies(Sink sink) {
        this.sink = sink;
    }

    /** Starts listening. Safe to call more than once. */
    void register() {
        ChatManagerMapComponent chat = ChatManagerMapComponent.getInstance();
        if (chat == null || registered) {
            if (chat == null)
                Log.w(TAG, "chat component not ready; replies will not show");
            return;
        }
        chat.addChatMessageListener(this);
        registered = true;
    }

    /** Stops listening. Called when the plugin unloads. */
    void unregister() {
        ChatManagerMapComponent chat = ChatManagerMapComponent.getInstance();
        if (chat != null && registered)
            chat.removeChatMessageListener(this);
        registered = false;
    }

    @Override
    public void chatMessageReceived(Bundle bundle) {
        if (bundle == null)
            return;
        String bot = sink.botCallsign();
        if (bot == null)
            return;
        Contact c = GrokLinkSender.findContact(bot);
        String botUid = c == null ? null : c.getUID();
        String senderUid = bundle.getString("senderUid");
        String senderCallsign = bundle.getString("senderCallsign");
        if (!ReplyFilter.isFromBot(bot, botUid, senderCallsign, senderUid))
            return;
        String text = bundle.getString("message");
        if (text == null || text.trim().isEmpty())
            return;
        sink.onReply(bot, text.trim());
    }
}
