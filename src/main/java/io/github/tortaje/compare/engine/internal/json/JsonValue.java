package io.github.tortaje.compare.engine.internal.json;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Immutable JSON document model produced by {@link JsonParser}. */
public sealed interface JsonValue {

    /** Returns the JSON type name: object, array, string, number, boolean or null. */
    String typeName();

    /** Returns this value as compact JSON text. */
    String toJson();

    /** JSON object; members keep source order. */
    record JsonObject(Map<String, JsonValue> members) implements JsonValue {
        @Override public String typeName() { return "object"; }

        @Override public String toJson() {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, JsonValue> e : members.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                quote(sb, e.getKey());
                sb.append(':').append(e.getValue().toJson());
            }
            return sb.append('}').toString();
        }
    }

    /** JSON array. */
    record JsonArray(List<JsonValue> items) implements JsonValue {
        @Override public String typeName() { return "array"; }

        @Override public String toJson() {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(items.get(i).toJson());
            }
            return sb.append(']').toString();
        }
    }

    /** JSON string (decoded). */
    record JsonString(String value) implements JsonValue {
        @Override public String typeName() { return "string"; }

        @Override public String toJson() {
            StringBuilder sb = new StringBuilder();
            quote(sb, value);
            return sb.toString();
        }
    }

    /** JSON number with its numeric value and original source text. */
    record JsonNumber(BigDecimal value, String source) implements JsonValue {
        @Override public String typeName() { return "number"; }

        @Override public String toJson() { return source; }
    }

    /** JSON boolean. */
    record JsonBoolean(boolean value) implements JsonValue {
        @Override public String typeName() { return "boolean"; }

        @Override public String toJson() { return Boolean.toString(value); }
    }

    /** JSON null. */
    record JsonNull() implements JsonValue {
        @Override public String typeName() { return "null"; }

        @Override public String toJson() { return "null"; }
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
