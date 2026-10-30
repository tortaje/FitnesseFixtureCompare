package io.github.tortaje.compare.engine.internal.json;

import io.github.tortaje.compare.engine.Difference;
import io.github.tortaje.compare.engine.DifferenceKind;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonArray;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonBoolean;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonNumber;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonObject;
import io.github.tortaje.compare.engine.internal.json.JsonValue.JsonString;
import io.github.tortaje.compare.engine.internal.wildcard.Wildcard;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Structural JSON comparison of an expected template against an actual document. */
public final class JsonComparator {

    private JsonComparator() { }

    /**
     * Compares two documents.
     *
     * @param expected the expected template (wildcards allowed in string values)
     * @param actual   the actual document
     * @return the differences, depth-first; empty when they match
     */
    public static List<Difference> compare(JsonValue expected, JsonValue actual) {
        return compare("$", expected, actual);
    }

    /**
     * Compares two documents, naming the root of the paths.
     *
     * @param root     the root path, e.g. {@code line[2]$}
     * @param expected the expected template (wildcards allowed in string values)
     * @param actual   the actual document
     * @return the differences, depth-first; empty when they match
     */
    public static List<Difference> compare(String root, JsonValue expected, JsonValue actual) {
        List<Difference> out = new ArrayList<>();
        compare(root, expected, actual, out);
        return out;
    }

    /**
     * Compares two documents, adding the differences to a caller-supplied list.
     *
     * @param path     the path of the compared values, e.g. {@code $}
     * @param expected the expected template
     * @param actual   the actual document
     * @param out      receives the differences
     */
    public static void compare(String path, JsonValue expected, JsonValue actual, List<Difference> out) {
        if (expected instanceof JsonString es) {
            Optional<Wildcard> wc = Wildcard.parse(es.value(), path);
            if (wc.isPresent()) {
                wildcard(path, wc.get(), actual, out);
                return;
            }
            if (actual instanceof JsonString as) {
                if (!Wildcard.literal(es.value()).equals(as.value())) {
                    out.add(new Difference(path, DifferenceKind.VALUE,
                            new JsonString(Wildcard.literal(es.value())).toJson(), actual.toJson()));
                }
                return;
            }
            typeMismatch(path, expected, actual, out);
            return;
        }
        if (!expected.typeName().equals(actual.typeName())) {
            typeMismatch(path, expected, actual, out);
            return;
        }
        if (expected instanceof JsonObject eo) {
            object(path, eo, (JsonObject) actual, out);
        } else if (expected instanceof JsonArray ea) {
            array(path, ea, (JsonArray) actual, out);
        } else if (expected instanceof JsonNumber en) {
            if (en.value().compareTo(((JsonNumber) actual).value()) != 0) {
                out.add(new Difference(path, DifferenceKind.VALUE, en.source(), ((JsonNumber) actual).source()));
            }
        } else if (expected instanceof JsonBoolean eb) {
            if (eb.value() != ((JsonBoolean) actual).value()) {
                out.add(new Difference(path, DifferenceKind.VALUE, expected.toJson(), actual.toJson()));
            }
        }
        // null equals null
    }

    private static void typeMismatch(String path, JsonValue expected, JsonValue actual, List<Difference> out) {
        out.add(new Difference(path, DifferenceKind.TYPE, expected.typeName(), actual.typeName()));
    }

    private static void wildcard(String path, Wildcard wc, JsonValue actual, List<Difference> out) {
        switch (wc.kind()) {
            case ANY, IGNORE -> { }
            case ANY_STRING -> {
                if (!(actual instanceof JsonString)) {
                    out.add(new Difference(path, DifferenceKind.TYPE, "string", actual.typeName()));
                }
            }
            case ANY_NUMBER -> {
                if (!(actual instanceof JsonNumber)) {
                    out.add(new Difference(path, DifferenceKind.TYPE, "number", actual.typeName()));
                }
            }
            case REGEX -> {
                String text = null;
                if (actual instanceof JsonString s) {
                    text = s.value();
                } else if (actual instanceof JsonNumber n) {
                    text = n.source();
                } else if (actual instanceof JsonBoolean b) {
                    text = Boolean.toString(b.value());
                }
                if (text == null || !wc.matches(text, path)) {
                    out.add(new Difference(path, DifferenceKind.PATTERN, wc.pattern().pattern(), actual.toJson()));
                }
            }
        }
    }

    private static void object(String path, JsonObject expected, JsonObject actual, List<Difference> out) {
        for (Map.Entry<String, JsonValue> e : expected.members().entrySet()) {
            String child = JsonPaths.key(path, e.getKey());
            JsonValue a = actual.members().get(e.getKey());
            if (a == null) {
                if (!(e.getValue() instanceof JsonString s && Wildcard.isIgnore(s.value()))) {
                    out.add(new Difference(child, DifferenceKind.MISSING, e.getValue().toJson(), null));
                }
            } else {
                compare(child, e.getValue(), a, out);
            }
        }
        for (Map.Entry<String, JsonValue> e : actual.members().entrySet()) {
            if (!expected.members().containsKey(e.getKey())) {
                out.add(new Difference(JsonPaths.key(path, e.getKey()), DifferenceKind.UNEXPECTED, null,
                        e.getValue().toJson()));
            }
        }
    }

    private static void array(String path, JsonArray expected, JsonArray actual, List<Difference> out) {
        int common = Math.min(expected.items().size(), actual.items().size());
        for (int i = 0; i < common; i++) {
            compare(JsonPaths.index(path, i), expected.items().get(i), actual.items().get(i), out);
        }
        for (int i = common; i < expected.items().size(); i++) {
            out.add(new Difference(JsonPaths.index(path, i), DifferenceKind.MISSING,
                    expected.items().get(i).toJson(), null));
        }
        for (int i = common; i < actual.items().size(); i++) {
            out.add(new Difference(JsonPaths.index(path, i), DifferenceKind.UNEXPECTED, null,
                    actual.items().get(i).toJson()));
        }
    }
}
