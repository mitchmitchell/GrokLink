GrokLink
========

An example ATAK-CIV plugin: a panel that sends a typed request to an AI bot
contact on your TAK Server and shows the bot's chat replies. It is a
reference for building other plugins, not a finished product.

_________________________________________________________________
PURPOSE AND CAPABILITIES

- Toolbar button opens a GrokLink panel.
- Shows whether the configured bot contact (default GROK-1) is online.
- Sends a typed request to that contact only, as a CoT event of type
  t-x-groklink carrying a <groklink> detail whose text is JSON:
  {"v":1,"msg":"request","text":"..."}
- Shows the bot's chat replies in the panel (they also stay in ATAK chat).

The plugin has no network code. It hands events to ATAK, which delivers
them over its existing TAK Server connection (CotDispatcher.dispatchToContact).
The server side is tak-bridge: https://github.com/mitchmitchell/GrokBridge

_________________________________________________________________
STATUS

Frozen at phase P3 (2026-11-17) as a working example. Day-to-day use goes
through ordinary ATAK chat and native sharing, which the bridge supports on
every TAK client. Planned but not built: P4 map summary, P5 full item
detail, P6 map picture, P7 hardening.

Checked on a Galaxy Tab S12 Ultra with developer ATAK-CIV 5.8.0.10 against
TAK Server 5.8-RELEASE-103: a panel request reached the bot and its reply
appeared in the panel and in ATAK chat.

_________________________________________________________________
POINT OF CONTACTS

Mitch Mitchell (repository owner).

_________________________________________________________________
PORTS REQUIRED

None of its own. Uses ATAK's existing TAK Server connection.

_________________________________________________________________
EQUIPMENT REQUIRED

ATAK-CIV 5.8.0 developer build (the atak.apk inside the ATAK-CIV 5.8.0.10
SDK). Debug-signed plugins do not load in the Play Store ATAK.

_________________________________________________________________
EQUIPMENT SUPPORTED

Android devices running the ATAK-CIV 5.8.0 developer build.

_________________________________________________________________
COMPILATION

Prerequisites: ATAK-CIV 5.8.0.10 SDK unzipped, Android SDK (platform 36,
build-tools 35), JDK 17 (Adoptium), Gradle 8.14.3 via the wrapper.

local.properties (not committed):

    sdk.dir=<Android SDK path>
    sdk.path=<ATAK-CIV SDK path>
    takdev.plugin=<ATAK-CIV SDK path>/atak-gradle-takdev.jar

Build and test:

    ./gradlew assembleCivDebug testCivDebugUnitTest

The APK is in app/build/outputs/apk/civ/debug/.

Publish to an ATAK update repository served by TAK Server (ATAK: Settings >
TAK Package Mgmt Preferences > Update Server):

    python3 tools/publish_update.py /opt/tak/webcontent/update GrokLink.apk \
      --package com.mitchell.atak.groklink --name GrokLink --version "<versionName>" \
      --revision <versionCode> --icon icons/groklink.png --description "..." \
      --min-sdk 21 --tak-req com.atakmap.app@5.8.0.CIV

_________________________________________________________________
DEVELOPER NOTES

Code tour:

- GrokLinkPlugin: entry point named in assets/plugin.xml; adds the toolbar
  button, builds the pane, cleans up on unload (from samples/plugintemplate).
- GrokLinkPanel: the panel's views, settings (bot callsign) and sending on a
  background thread.
- GrokLinkSender: builds the CoT event and sends it to one contact.
- GrokLinkReplies: ChatManagerMapComponent.ChatMessageListener that shows
  the bot's replies.
- core/: pure Java with JVM unit tests (JSON writer, request message, bot
  setting, reply filter). No Android or ATAK classes, so tests run on the JVM.

Lessons recorded while building it:

- The self marker's getType() returns "self", not a CoT type.
- Use anonymous classes, not lambdas: the SDK README warns lambdas can fail
  in release builds after proguard.
- The plugin must be rebuilt for each ATAK version it should load in.
