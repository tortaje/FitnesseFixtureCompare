package io.github.tortaje.compare.engine.internal.records;

import io.github.tortaje.compare.engine.ComparisonException;
import io.github.tortaje.compare.engine.Difference;
import io.github.tortaje.compare.engine.internal.json.JsonComparator;
import io.github.tortaje.compare.engine.internal.json.JsonParser;
import io.github.tortaje.compare.engine.internal.json.JsonValue;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonObject;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonString;
import io.github.tortaje.compare.engine.internal.wildcard.Wildcard;
import java.util.ArrayList;
import java.util.List;

/** JSON Lines records: one JSON value per non-blank line. */
public final class JsonLinesRecords {

    /** The record format of JSON Lines. */
    public static final RecordFormat<JsonValue> FORMAT = new RecordFormat<>() {
        @Override public String label() {
            return "line";
        }

        @Override public void compare(String path, JsonValue expected, JsonValue actual, List<Difference> out) {
            out.addAll(JsonComparator.compare(path + "$", expected, actual));
        }

        @Override public boolean matches(JsonValue expected, JsonValue actual) {
            try {
                JsonComparator.compare("$", expected, actual, new ArrayList<>() {
                    private static final long serialVersionUID = 1L;

                    @Override public boolean add(Difference d) {
                        throw FIRST_DIFFERENCE;
                    }
                });
                return true;
            } catch (FirstDifference e) {
                return false;
            }
        }

        @Override public String key(JsonValue record, List<String> key, boolean expectedSide, int position) {
            List<String> parts = new ArrayList<>();
            for (String name : key) {
                JsonValue v = record;
                for (String step : name.split("\\.", -1)) {
                    if (!(v instanceof JsonObject o) || !o.members().containsKey(step)) {
                        return null;
                    }
                    v = o.members().get(step);
                }
                if (expectedSide) {
                    if (hasWildcard(v)) {
                        throw new ComparisonException(
                                "key '" + name + "' cannot be a wildcard in expected line " + position);
                    }
                    if (v instanceof JsonString s) {
                        v = new JsonString(Wildcard.literal(s.value()));
                    }
                }
                parts.add(v.toJson());
            }
            return String.join("\u0000", parts);
        }

        @Override public String display(JsonValue record) {
            return record.toJson();
        }
    };

    private static final FirstDifference FIRST_DIFFERENCE = new FirstDifference();

    /** Stops a comparison at its first difference; carries no stack trace. */
    private static final class FirstDifference extends RuntimeException {
        private static final long serialVersionUID = 1L;

        FirstDifference() {
            super(null, null, false, false);
        }
    }

    private JsonLinesRecords() { }

    private static boolean hasWildcard(JsonValue v) {
        if (v instanceof JsonString s) {
            return Wildcard.parse(s.value(), "$").isPresent();
        }
        if (v instanceof JsonObject o) {
            return o.members().values().stream().anyMatch(JsonLinesRecords::hasWildcard);
        }
        if (v instanceof JsonValue.JsonArray a) {
            return a.items().stream().anyMatch(JsonLinesRecords::hasWildcard);
        }
        return false;
    }

    /**
     * Splits JSON Lines content into records.
     *
     * @param content the content (leading BOM, CRLF and blank lines tolerated; empty means no record)
     * @param side    {@code "expected"} or {@code "actual"}, used in error messages
     * @return the records, positioned by physical line
     * @throws ComparisonException on a line that is not strict JSON
     */
    public static List<Rec<JsonValue>> records(String content, String side) {
        List<Rec<JsonValue>> out = new ArrayList<>();
        if (content == null) {
            return out;
        }
        String t = !content.isEmpty() && content.charAt(0) == '\uFEFF' ? content.substring(1) : content;
        String[] lines = t.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            if (!line.isBlank()) {
                out.add(new Rec<>(i + 1, JsonParser.parseLine(line, side, i + 1)));
            }
        }
        return out;
    }
}
