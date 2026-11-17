package com.mitchell.atak.groklink.core;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A minimal JSON writer: maps, lists, strings, numbers, booleans and null.
 *
 * GrokLink adds no third-party libraries (a plugin shares ATAK's process),
 * and Android's org.json is only a stub in JVM unit tests, so the message
 * builders use this instead. Pure Java, no Android or ATAK classes.
 *
 * Maps keep their iteration order, so pass a LinkedHashMap for a stable
 * field order.
 */
public final class Json {

    private Json() {
    }

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        append(sb, value);
        return sb.toString();
    }

    /** Size in bytes of the UTF-8 encoding of {@code s}. */
    public static int utf8Length(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x80) {
                n += 1;
            } else if (c < 0x800) {
                n += 2;
            } else if (Character.isHighSurrogate(c) && i + 1 < s.length()
                    && Character.isLowSurrogate(s.charAt(i + 1))) {
                n += 4;
                i++;
            } else {
                n += 3;
            }
        }
        return n;
    }

    private static void append(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String) {
            appendString(sb, (String) v);
        } else if (v instanceof Boolean) {
            sb.append(((Boolean) v) ? "true" : "false");
        } else if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                sb.append("null");
            } else if (d == Math.rint(d) && Math.abs(d) < 1e15) {
                sb.append((long) d);
            } else {
                sb.append(d);
            }
        } else if (v instanceof Number) {
            sb.append(((Number) v).longValue());
        } else if (v instanceof Map) {
            sb.append('{');
            Iterator<? extends Map.Entry<?, ?>> it = ((Map<?, ?>) v).entrySet().iterator();
            boolean first = true;
            while (it.hasNext()) {
                Map.Entry<?, ?> e = it.next();
                if (!first)
                    sb.append(',');
                first = false;
                appendString(sb, String.valueOf(e.getKey()));
                sb.append(':');
                append(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof List) {
            sb.append('[');
            boolean first = true;
            for (Object o : (List<?>) v) {
                if (!first)
                    sb.append(',');
                first = false;
                append(sb, o);
            }
            sb.append(']');
        } else if (v instanceof double[]) {
            double[] a = (double[]) v;
            sb.append('[');
            for (int i = 0; i < a.length; i++) {
                if (i > 0)
                    sb.append(',');
                append(sb, a[i]);
            }
            sb.append(']');
        } else {
            appendString(sb, v.toString());
        }
    }

    private static void appendString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    // Escape control characters, and < > & so the JSON stays
                    // inert even if a parser on the far side mishandles the
                    // XML text it is wrapped in.
                    if (c < 0x20 || c == '<' || c == '>' || c == '&'
                            || c == ' ' || c == ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }
}
