package com.mitchell.atak.groklink;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.atakmap.android.contact.Contact;
import com.atakmap.android.maps.MapView;
import com.mitchell.atak.groklink.core.BotSetting;
import com.mitchell.atak.groklink.core.RequestMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The GrokLink panel: bot status, request box, Send, conversation view and
 * the bot callsign setting.
 *
 * Contexts (rule from samples/plugintemplate): the view is inflated with the
 * plugin context; dialogs use the ATAK MapView context.
 */
final class GrokLinkPanel {

    static final String PREF_BOTS = "groklink.bots";

    private final Context pluginContext;
    private final View root;
    private final TextView botStatus;
    private final EditText requestBox;
    private final Button sendButton;
    private final TextView conversation;
    private final ScrollView conversationScroll;

    /** One thread for sending, so the UI thread never waits on ATAK comms. */
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();

    /** Shows the bot's chat replies in the conversation view (P3). */
    private final GrokLinkReplies replies = new GrokLinkReplies(new GrokLinkReplies.Sink() {
        @Override
        public String botCallsign() {
            return GrokLinkPanel.this.botCallsign();
        }

        @Override
        public void onReply(String bot, String text) {
            // The listener may run off the UI thread.
            postLine(pluginContext.getString(R.string.line_bot, bot, text));
        }
    });

    GrokLinkPanel(Context pluginContext, View root) {
        this.pluginContext = pluginContext;
        this.root = root;
        botStatus = root.findViewById(R.id.bot_status);
        requestBox = root.findViewById(R.id.request_box);
        sendButton = root.findViewById(R.id.send_button);
        conversation = root.findViewById(R.id.conversation);
        conversationScroll = root.findViewById(R.id.conversation_scroll);

        // Anonymous classes rather than lambdas: the SDK README notes lambdas
        // can fail in release builds after proguard.
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSend();
            }
        });
        root.findViewById(R.id.settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettings();
            }
        });
        root.findViewById(R.id.refresh_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshStatus();
            }
        });
        requestBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSendEnabled();
            }
        });
        refreshStatus();
        // Listen from when the panel is first opened until the plugin
        // unloads, so replies that arrive while the pane is closed are still
        // in the conversation when it is reopened.
        replies.register();
    }

    View getView() {
        return root;
    }

    /** Called when the pane is shown. */
    void onShown() {
        replies.register();  // no-op if already registered
        refreshStatus();
    }

    /** Called when the plugin unloads. */
    void dispose() {
        replies.unregister();
        sendExecutor.shutdownNow();
    }

    // ---- settings ----------------------------------------------------------

    private SharedPreferences prefs() {
        // ATAK's default preferences, under a groklink. prefix.
        return PreferenceManager.getDefaultSharedPreferences(
                MapView.getMapView().getContext());
    }

    String botCallsign() {
        return BotSetting.primary(prefs().getString(PREF_BOTS, BotSetting.DEFAULT));
    }

    private void showSettings() {
        final Context ctx = MapView.getMapView().getContext();
        final EditText input = new EditText(ctx);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setSingleLine(true);
        input.setText(prefs().getString(PREF_BOTS, BotSetting.DEFAULT));

        new AlertDialog.Builder(ctx)
                .setTitle(pluginContext.getString(R.string.settings_title))
                .setMessage(pluginContext.getString(R.string.settings_message))
                .setView(input)
                .setPositiveButton(android.R.string.ok,
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int w) {
                                String value = input.getText().toString().trim();
                                if (BotSetting.primary(value) == null) {
                                    appendLine(pluginContext.getString(R.string.settings_invalid));
                                    return;
                                }
                                prefs().edit().putString(PREF_BOTS, value).apply();
                                refreshStatus();
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ---- status ------------------------------------------------------------

    private void refreshStatus() {
        String bot = botCallsign();
        if (bot == null) {
            botStatus.setText(R.string.status_no_bot);
        } else {
            Contact c = GrokLinkSender.findContact(bot);
            int fmt = c == null ? R.string.status_missing
                    : GrokLinkSender.isOnline(c) ? R.string.status_online
                            : R.string.status_offline;
            botStatus.setText(pluginContext.getString(fmt, bot));
        }
        updateSendEnabled();
    }

    private void updateSendEnabled() {
        boolean hasText = requestBox.getText().toString().trim().length() > 0;
        sendButton.setEnabled(hasText && botCallsign() != null);
    }

    // ---- sending -----------------------------------------------------------

    private void onSend() {
        final String bot = botCallsign();
        final String text = requestBox.getText().toString();
        final String json;
        try {
            json = RequestMessage.build(text, null, null);
        } catch (IllegalArgumentException e) {
            appendLine(pluginContext.getString(R.string.send_failed, e.getMessage()));
            return;
        }
        appendLine(pluginContext.getString(R.string.line_me, text.trim()));
        requestBox.setText("");

        sendExecutor.execute(new Runnable() {
            @Override
            public void run() {
                String result;
                try {
                    GrokLinkSender.send(bot, json);
                    result = pluginContext.getString(R.string.send_ok, bot);
                } catch (GrokLinkSender.SendException e) {
                    result = pluginContext.getString(R.string.send_failed, e.getMessage());
                } catch (RuntimeException e) {
                    result = pluginContext.getString(R.string.send_failed, e.toString());
                }
                postLine(result);
            }
        });
    }

    // ---- conversation view -------------------------------------------------

    /** Appends a line from any thread. */
    void postLine(final String line) {
        root.post(new Runnable() {
            @Override
            public void run() {
                appendLine(line);
            }
        });
    }

    /** Appends a line; UI thread only. */
    void appendLine(String line) {
        String time = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
        if (conversation.length() > 0)
            conversation.append("\n");
        conversation.append(time + "  " + line);
        conversationScroll.post(new Runnable() {
            @Override
            public void run() {
                conversationScroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
}
