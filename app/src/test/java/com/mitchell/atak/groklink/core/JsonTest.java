package com.mitchell.atak.groklink.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonTest {

    @Test
    public void writesScalars() {
        assertEquals("null", Json.write(null));
        assertEquals("true", Json.write(true));
        assertEquals("42", Json.write(42));
        assertEquals("3", Json.write(3.0));
        assertEquals("35.0045", Json.write(35.0045));
        assertEquals("-90.5", Json.write(-90.5));
        assertEquals("null", Json.write(Double.NaN));
    }

    @Test
    public void keepsMapOrderAndNests() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("b", 1);
        m.put("a", Arrays.asList("x", 2, null));
        assertEquals("{\"b\":1,\"a\":[\"x\",2,null]}", Json.write(m));
    }

    @Test
    public void escapesSpecialCharacters() {
        assertEquals("\"q\\\"b\\\\n\\nt\\t\"", Json.write("q\"b\\n\nt\t"));
        // < > & are escaped so the JSON stays inert inside XML text.
        assertEquals("\"\\u003ca\\u003e \\u0026\"", Json.write("<a> &"));
        assertEquals("\"\\u0001\"", Json.write("\u0001"));
        // Non-ASCII passes through unchanged.
        assertEquals("\"caf\u00e9 \u2713\"", Json.write("caf\u00e9 \u2713"));
    }

    @Test
    public void countsUtf8Bytes() {
        assertEquals(3, Json.utf8Length("abc"));
        assertEquals(2, Json.utf8Length("\u00e9"));
        assertEquals(3, Json.utf8Length("\u2713"));
        assertEquals(4, Json.utf8Length("\uD83D\uDE00"));
    }
}
