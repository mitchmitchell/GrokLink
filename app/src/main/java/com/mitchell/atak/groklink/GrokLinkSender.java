package com.mitchell.atak.groklink;

import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.UUID;

/**
 * Wraps one GrokLink JSON message in a CoT event and sends it to one contact.
 *
 * The plugin has no network code: it hands the event to ATAK's external
 * dispatcher, which delivers it over ATAK's existing TAK Server connection.
 *
 * Event shape (chosen for routing, not display):
 * <pre>
 * &lt;event uid="GrokLink.&lt;my uid&gt;.&lt;random&gt;" type="t-x-groklink" how="h-e" ...&gt;
 *   &lt;point .../&gt;                       my position, or 0,0 with unknown accuracy
 *   &lt;detail&gt;
 *     &lt;groklink&gt;{"v":1,...}&lt;/groklink&gt;
 *     &lt;link uid="&lt;my uid&gt;" relation="p-p" type="&lt;my type&gt;"/&gt;
 *   &lt;/detail&gt;
 * &lt;/event&gt;
 * </pre>
 * Why: a "t-" (tasking) type is not an atom ("a-") or drawing, so a TAK
 * client that did receive it would not place it on the map; the TAK Server
 * routes events by destination, not by type. A fresh uid per message stops
 * one message replacing another, and the link element names the sender, as
 * GeoChat messages do (docs/TakChatSpec.odt, section 4.1).
 */
final class GrokLinkSender {

    static final String EVENT_TYPE = "t-x-groklink";
    static final String DETAIL_NAME = "groklink";

    /** Unknown accuracy, as CoT uses for "no position". */
    private static final double UNKNOWN = 9999999.0;

    /** Thrown with a message the panel can show as is. */
    static final class SendException extends Exception {
        SendException(String message) {
            super(message);
        }
    }

    private GrokLinkSender() {
    }

    /**
     * Finds the bot in ATAK's contact list.
     * Pattern: Contacts.getInstance() as in samples/helloworld
     * (HelloWorldContactHandler and HelloWorldDropDownReceiver).
     */
    static Contact findContact(String callsign) {
        if (callsign == null)
            return null;
        return Contacts.getInstance().getFirstContactWithCallsign(callsign);
    }

    /** True when the contact exists and ATAK has heard from it recently. */
    static boolean isOnline(Contact c) {
        return c != null && c.getUpdateStatus() == Contact.UpdateStatus.CURRENT;
    }

    /** Builds the event for one JSON message. */
    static CotEvent buildEvent(String json) {
        MapView mv = MapView.getMapView();
        String myUid = MapView.getDeviceUid();

        CotPoint point = new CotPoint(0, 0, UNKNOWN, UNKNOWN, UNKNOWN);
        String myType = "a-f-G-U-C";
        Marker self = mv == null ? null : mv.getSelfMarker();
        if (self != null && self.getPoint() != null) {
            point = new CotPoint(self.getPoint());
            // The self marker's own type is the literal "self" (seen in a
            // captured request); only a real CoT atom type belongs in <link>.
            String t = self.getType();
            if (t != null && t.startsWith("a-"))
                myType = t;
        }

        CoordinatedTime now = new CoordinatedTime();
        CotEvent event = new CotEvent();
        event.setVersion("2.0");
        event.setUID("GrokLink." + myUid + "." + UUID.randomUUID());
        event.setType(EVENT_TYPE);
        event.setHow("h-e");
        event.setPoint(point);
        event.setTime(now);
        event.setStart(now);
        event.setStale(now.addMinutes(2));

        CotDetail detail = new CotDetail("detail");
        CotDetail groklink = new CotDetail(DETAIL_NAME);
        // CotDetail escapes inner text when the event is serialized.
        groklink.setInnerText(json);
        detail.addChild(groklink);

        CotDetail link = new CotDetail("link");
        link.setAttribute("uid", myUid);
        link.setAttribute("relation", "p-p");
        link.setAttribute("type", myType);
        detail.addChild(link);

        event.setDetail(detail);
        return event;
    }

    /**
     * Sends one JSON message to the bot contact only, never to a group.
     * Pattern: CotMapComponent.getExternalDispatcher() as in samples/helloworld
     * (which uses dispatchToBroadcast; GrokLink uses dispatchToContact).
     */
    static void send(String botCallsign, String json) throws SendException {
        Contact bot = findContact(botCallsign);
        if (bot == null)
            throw new SendException(botCallsign + " is not in your contacts. "
                    + "Check that the bridge is running and you are connected to the TAK Server.");
        CotEvent event = buildEvent(json);
        if (!event.isValid())
            throw new SendException("ATAK rejected the message as an invalid CoT event.");
        CotMapComponent.getExternalDispatcher().dispatchToContact(event, bot);
    }
}
