package io.github.tortaje.compare.engine.internal.json;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tortaje.compare.engine.ComparisonException;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonArray;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonBoolean;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonNumber;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonObject;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonString;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonParserTest {

    @Test void parsesAllTypes() {
        JsonValue v = JsonParser.parse("{\"a\":[1,-2.50e1,\"x\\u00e9\",true,false,null],\"b\":{}}", "expected");
        JsonObject o = (JsonObject) v;
        assertEquals(List.of("a", "b"), List.copyOf(o.members().keySet()));
        JsonArray a = (JsonArray) o.members().get("a");
        assertEquals(new BigDecimal("-25.0"), ((JsonNumber) a.items().get(1)).value());
        assertEquals("-2.50e1", ((JsonNumber) a.items().get(1)).source());
        assertEquals("xé", ((JsonString) a.items().get(2)).value());
        assertEquals("number", a.items().get(0).typeName());
        assertEquals("object", v.typeName());
        assertEquals("array", a.typeName());
        assertEquals("boolean", a.items().get(3).typeName());
        assertEquals("null", a.items().get(5).typeName());
        assertEquals("string", a.items().get(2).typeName());
    }

    @Test void topLevelScalar() { assertEquals(new JsonString("abc"), JsonParser.parse(" \"abc\" ", "actual")); }

    @Test void bomStripped() { assertEquals(new JsonBoolean(true), JsonParser.parse("\uFEFFtrue", "expected")); }

    @Test void emptyContent() { assertError("   ", "actual", "actual: content is empty"); }

    @Test void nullContent() { assertError(null, "expected", "expected: content is empty"); }

    @Test void duplicateKey() {
        assertError("{\n  \"id\": 1,\n    \"id\": 2}", "expected", "expected: duplicate key 'id' at line 3, column 5");
    }

    @Test void trailingComma() {
        assertError("[1,]", "expected", "expected: unexpected character ']' at line 1, column 4");
    }

    @Test void leadingZero() {
        assertError("01", "expected", "expected: unexpected character '1' at line 1, column 2");
    }

    @Test void truncated() {
        assertError("{\"a\":", "expected", "expected: unexpected end of content at line 1, column 6");
    }

    @Test void trailingGarbage() {
        assertError("{} x", "actual", "actual: unexpected character 'x' at line 1, column 4");
    }

    @Test void controlCharacterInString() {
        assertError("\"a\tb\"", "expected", "expected: unexpected character '\\u0009' at line 1, column 3");
    }

    @Test void numberOutOfRange() {
        assertError("[1e99999999999]", "expected", "expected: number out of range at line 1, column 2");
    }

    @Test void tooDeep() {
        assertTrue(message("[".repeat(513) + "]".repeat(513)).startsWith("expected: nesting deeper than 512 at "));
        assertDoesNotThrow(() -> JsonParser.parse("[".repeat(512) + "]".repeat(512), "expected"));
    }

    @Test void toJsonIsCompact() {
        assertEquals("{\"a\":[1,\"q\\\"\"],\"b\":null}",
                JsonParser.parse("{ \"a\" : [ 1 , \"q\\\"\" ], \"b\": null }", "expected").toJson());
    }

    @Test void toJsonEscapesControlCharacters() {
        assertEquals("\"a\\n\\u0001\\\\\"", new JsonString("a\n\u0001\\").toJson());
    }

    private static void assertError(String text, String side, String msg) {
        ComparisonException e = assertThrows(ComparisonException.class, () -> JsonParser.parse(text, side));
        assertEquals(msg, e.getMessage());
    }

    private static String message(String text) {
        return assertThrows(ComparisonException.class, () -> JsonParser.parse(text, "expected")).getMessage();
    }
}
