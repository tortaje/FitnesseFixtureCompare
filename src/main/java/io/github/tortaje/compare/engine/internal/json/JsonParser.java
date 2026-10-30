package io.github.tortaje.compare.engine.internal.json;

import io.github.tortaje.compare.engine.ComparisonException;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonArray;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonBoolean;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonNull;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonNumber;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonObject;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonString;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Strict RFC 8259 recursive-descent JSON parser with line/column error reporting. */
public final class JsonParser {

    private static final int MAX_DEPTH = 512;

    private final String text;
    private final String side;
    private final int lineNumber; // 0 = whole document, otherwise the JSON Lines line being parsed
    private int pos;

    private JsonParser(String text, String side, int lineNumber) {
        this.text = text;
        this.side = side;
        this.lineNumber = lineNumber;
    }

    /**
     * Parses a JSON document.
     *
     * @param text the JSON text (a leading BOM is ignored)
     * @param side {@code "expected"} or {@code "actual"}, used as error prefix
     * @return the parsed value
     * @throws ComparisonException if the content is empty or not strict JSON
     */
    public static JsonValue parse(String text, String side) {
        if (text == null || text.isBlank()) {
            throw new ComparisonException(side + ": content is empty");
        }
        String t = text.charAt(0) == '\uFEFF' ? text.substring(1) : text;
        JsonParser p = new JsonParser(t, side, 0);
        p.skipWs();
        if (p.pos >= t.length()) {
            throw new ComparisonException(side + ": content is empty");
        }
        JsonValue v = p.value(0);
        p.skipWs();
        if (p.pos < t.length()) {
            throw p.unexpected();
        }
        return v;
    }

    /**
     * Parses one non-blank JSON Lines line; errors read {@code <side>: line <n>: <reason> at column <c>}.
     *
     * @param text the line, without its line terminator
     * @param side {@code "expected"} or {@code "actual"}, used as error prefix
     * @param line physical line number (1-based) of the line in its content
     * @return the parsed value
     * @throws ComparisonException if the line is not strict JSON
     */
    public static JsonValue parseLine(String text, String side, int line) {
        JsonParser p = new JsonParser(text, side, line);
        p.skipWs();
        JsonValue v = p.value(0);
        p.skipWs();
        if (p.pos < text.length()) {
            throw p.unexpected();
        }
        return v;
    }

    private JsonValue value(int depth) {
        if (pos >= text.length()) {
            throw unexpected();
        }
        char c = text.charAt(pos);
        switch (c) {
            case '{':
                return object(depth + 1);
            case '[':
                return array(depth + 1);
            case '"':
                return new JsonString(string());
            case 't':
                literal("true");
                return new JsonBoolean(true);
            case 'f':
                literal("false");
                return new JsonBoolean(false);
            case 'n':
                literal("null");
                return new JsonNull();
            default:
                if (c == '-' || (c >= '0' && c <= '9')) {
                    return number();
                }
                throw unexpected();
        }
    }

    private JsonValue object(int depth) {
        checkDepth(depth);
        pos++;
        Map<String, JsonValue> members = new LinkedHashMap<>();
        skipWs();
        if (peek() == '}') {
            pos++;
            return new JsonObject(members);
        }
        while (true) {
            skipWs();
            if (peek() != '"') {
                throw unexpected();
            }
            int keyStart = pos;
            String key = string();
            if (members.containsKey(key)) {
                throw error("duplicate key '" + key + "'", keyStart);
            }
            skipWs();
            expect(':');
            skipWs();
            members.put(key, value(depth));
            skipWs();
            char c = peek();
            if (c == ',') {
                pos++;
            } else if (c == '}') {
                pos++;
                return new JsonObject(members);
            } else {
                throw unexpected();
            }
        }
    }

    private JsonValue array(int depth) {
        checkDepth(depth);
        pos++;
        List<JsonValue> items = new ArrayList<>();
        skipWs();
        if (peek() == ']') {
            pos++;
            return new JsonArray(items);
        }
        while (true) {
            skipWs();
            items.add(value(depth));
            skipWs();
            char c = peek();
            if (c == ',') {
                pos++;
            } else if (c == ']') {
                pos++;
                return new JsonArray(items);
            } else {
                throw unexpected();
            }
        }
    }

    private String string() {
        pos++; // opening quote
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= text.length()) {
                throw unexpected();
            }
            char c = text.charAt(pos);
            if (c == '"') {
                pos++;
                return sb.toString();
            }
            if (c < 0x20) {
                throw unexpected();
            }
            if (c != '\\') {
                sb.append(c);
                pos++;
                continue;
            }
            pos++;
            if (pos >= text.length()) {
                throw unexpected();
            }
            char e = text.charAt(pos);
            switch (e) {
                case '"', '\\', '/' -> sb.append(e);
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    int code = 0;
                    for (int i = 1; i <= 4; i++) {
                        int idx = pos + i;
                        if (idx >= text.length() || Character.digit(text.charAt(idx), 16) < 0) {
                            pos = idx;
                            throw unexpected();
                        }
                        code = code * 16 + Character.digit(text.charAt(idx), 16);
                    }
                    sb.append((char) code);
                    pos += 4;
                }
                default -> throw unexpected();
            }
            pos++;
        }
    }

    private JsonValue number() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        char c = peek();
        if (c == '0') {
            pos++;
        } else if (c >= '1' && c <= '9') {
            digits();
        } else {
            throw unexpected();
        }
        if (peek() == '.') {
            pos++;
            requireDigit();
            digits();
        }
        if (peek() == 'e' || peek() == 'E') {
            pos++;
            if (peek() == '+' || peek() == '-') {
                pos++;
            }
            requireDigit();
            digits();
        }
        String source = text.substring(start, pos);
        try {
            return new JsonNumber(new BigDecimal(source), source);
        } catch (NumberFormatException e) {
            throw error("number out of range", start);
        }
    }

    private void requireDigit() {
        char c = peek();
        if (c < '0' || c > '9') {
            throw unexpected();
        }
    }

    private void digits() {
        while (pos < text.length() && text.charAt(pos) >= '0' && text.charAt(pos) <= '9') {
            pos++;
        }
    }

    private void literal(String word) {
        for (int i = 0; i < word.length(); i++) {
            if (pos >= text.length() || text.charAt(pos) != word.charAt(i)) {
                throw unexpected();
            }
            pos++;
        }
    }

    private void expect(char c) {
        if (peek() != c) {
            throw unexpected();
        }
        pos++;
    }

    /** Current char, or NUL at end of content (never matches a structural token). */
    private char peek() {
        return pos < text.length() ? text.charAt(pos) : '\0';
    }

    private void skipWs() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }

    private void checkDepth(int depth) {
        if (depth > MAX_DEPTH) {
            throw error("nesting deeper than " + MAX_DEPTH, pos);
        }
    }

    private ComparisonException unexpected() {
        if (pos >= text.length()) {
            return error("unexpected end of content", text.length());
        }
        char c = text.charAt(pos);
        String shown = c < 0x20 ? String.format("\\u%04x", (int) c) : String.valueOf(c);
        return error("unexpected character '" + shown + "'", pos);
    }

    private ComparisonException error(String reason, int at) {
        if (lineNumber > 0) {
            return new ComparisonException(side + ": line " + lineNumber + ": " + reason + " at column " + (at + 1));
        }
        int line = 1;
        int col = 1;
        for (int i = 0; i < at; i++) {
            if (text.charAt(i) == '\n') {
                line++;
                col = 1;
            } else {
                col++;
            }
        }
        return new ComparisonException(side + ": " + reason + " at line " + line + ", column " + col);
    }
}
