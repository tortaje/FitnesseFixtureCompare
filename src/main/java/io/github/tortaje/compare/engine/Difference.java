package io.github.tortaje.compare.engine;

import java.util.function.UnaryOperator;

/**
 * One difference between an expected and an actual document.
 *
 * @param path     location of the differing node
 * @param kind     kind of difference
 * @param expected display string of the expected side (type name for {@link DifferenceKind#TYPE},
 *                 the regex for {@link DifferenceKind#PATTERN}), or {@code null} when unused
 * @param actual   display string of the actual side, or {@code null} when unused
 */
public record Difference(String path, DifferenceKind kind, String expected, String actual) {

    /** Maximum number of characters of a value shown in a message. */
    static final int MAX_VALUE_LENGTH = 80;

    /**
     * Returns the text form of this difference, without the path.
     *
     * @return the message, values truncated to 80 characters
     */
    public String message() {
        return render(UnaryOperator.identity());
    }

    /** Renders the message, passing each (already truncated) value through {@code wrap}. */
    String render(UnaryOperator<String> wrap) {
        return switch (kind) {
            case VALUE, TYPE -> "expected " + v(expected, wrap) + " but was " + v(actual, wrap);
            case MISSING -> path.startsWith("expected ") ? "missing " + v(expected, wrap) : "missing";
            case UNEXPECTED -> "unexpected " + v(actual, wrap);
            case PATTERN -> "expected to match /" + v(expected, wrap) + "/ but was " + v(actual, wrap);
        };
    }

    private static String v(String value, UnaryOperator<String> wrap) {
        String s = String.valueOf(value);
        if (s.length() > MAX_VALUE_LENGTH) {
            s = s.substring(0, MAX_VALUE_LENGTH) + "…";
        }
        return wrap.apply(s);
    }
}
