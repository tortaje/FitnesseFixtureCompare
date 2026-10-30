package io.github.tortaje.compare.engine.internal.wildcard;

import io.github.tortaje.compare.engine.ComparisonException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A wildcard marker found in expected content ({@code ${any}}, {@code ${any-string}}, {@code ${any-number}},
 * {@code ${ignore}}, {@code ${regex:PATTERN}}).
 *
 * @param kind    the marker kind
 * @param pattern the compiled pattern for {@link Kind#REGEX}, otherwise {@code null}
 */
public record Wildcard(Kind kind, Pattern pattern) {

    /** Wildcard marker kinds. */
    public enum Kind { ANY, ANY_STRING, ANY_NUMBER, IGNORE, REGEX }

    /** Regex source of the decimal number shape: {@code -?\d+(\.\d+)?([eE][+-]?\d+)?}. */
    public static final String NUMBER_SHAPE = "-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?";

    private static final Pattern NUMBER = Pattern.compile(NUMBER_SHAPE);

    private static final String IGNORE_MARKER = "${ignore}";

    /** Time budget of one regex match: catastrophic backtracking fails the comparison instead of hanging it. */
    static final long REGEX_TIME_LIMIT_MILLIS = 1000;
    private static final long REGEX_TIME_LIMIT_NANOS = TimeUnit.MILLISECONDS.toNanos(REGEX_TIME_LIMIT_MILLIS);

    /**
     * Parses a whole expected value as a wildcard marker.
     *
     * @param raw  the expected value
     * @param path the location, used in error messages
     * @return the wildcard, or empty when the value is not a marker (including the escaped {@code $${...}} form)
     * @throws ComparisonException for an unknown marker or an invalid regex
     */
    public static Optional<Wildcard> parse(String raw, String path) {
        if (!isMarkerShape(raw)) {
            return Optional.empty();
        }
        String inside = raw.substring(2, raw.length() - 1);
        int colon = inside.indexOf(':');
        String name = colon < 0 ? inside : inside.substring(0, colon);
        switch (name) {
            case "any" -> {
                if (colon < 0) {
                    return Optional.of(new Wildcard(Kind.ANY, null));
                }
            }
            case "any-string" -> {
                if (colon < 0) {
                    return Optional.of(new Wildcard(Kind.ANY_STRING, null));
                }
            }
            case "any-number" -> {
                if (colon < 0) {
                    return Optional.of(new Wildcard(Kind.ANY_NUMBER, null));
                }
            }
            case "ignore" -> {
                if (colon < 0) {
                    return Optional.of(new Wildcard(Kind.IGNORE, null));
                }
            }
            case "regex" -> {
                if (colon >= 0) {
                    try {
                        return Optional.of(new Wildcard(Kind.REGEX, Pattern.compile(inside.substring(colon + 1))));
                    } catch (PatternSyntaxException e) {
                        throw new ComparisonException(
                                "Invalid regex in '" + raw + "' at " + path + ": " + e.getDescription(), e);
                    }
                }
            }
            default -> { }
        }
        throw new ComparisonException("Unknown wildcard '" + raw + "' at " + path);
    }

    /**
     * Tells whether a value matches this {@link Kind#REGEX} wildcard as a whole.
     *
     * @param value the actual value
     * @param path  the location, used in error messages
     * @return true when the whole value matches the pattern
     * @throws ComparisonException when the regex engine runs out of stack or of time on this value
     */
    public boolean matches(String value, String path) {
        return matches(pattern, "${regex:" + pattern.pattern() + "}", value, path);
    }

    /**
     * Tells whether a value matches a pattern as a whole, within the 1 second deadline.
     *
     * @param pattern the compiled pattern
     * @param shown   how the pattern is named in error messages
     * @param value   the actual value
     * @param path    the location, used in error messages
     * @return true when the whole value matches the pattern
     * @throws ComparisonException when the regex engine runs out of stack or of time on this value
     */
    public static boolean matches(Pattern pattern, String shown, String value, String path) {
        try {
            return pattern.matcher(new DeadlineChars(value, System.nanoTime() + REGEX_TIME_LIMIT_NANOS)).matches();
        } catch (StackOverflowError e) {
            throw new ComparisonException("Regex '" + shown + "' at " + path
                    + " is too complex for a value of " + value.length() + " characters");
        } catch (DeadlineChars.Expired e) {
            throw new ComparisonException("Regex '" + shown + "' at " + path
                    + " took longer than " + REGEX_TIME_LIMIT_MILLIS + " ms on a value of " + value.length()
                    + " characters");
        }
    }

    /**
     * Tells whether a text has the decimal number shape used by {@code ${any-number}} on text-like formats.
     *
     * @param text the text
     * @return true for e.g. {@code -12.5e3}, false for {@code +1.} or {@code .5}
     */
    public static boolean isNumberShape(String text) {
        return text != null && NUMBER.matcher(text).matches();
    }

    /**
     * Returns the literal value of an expected string: {@code $${...}} loses one leading {@code $}; any other
     * value is returned unchanged.
     *
     * @param raw the expected value
     * @return the literal value
     */
    public static String literal(String raw) {
        return isEscaped(raw) ? raw.substring(1) : raw;
    }

    /**
     * Tells whether the value is exactly {@code ${ignore}}; never throws.
     *
     * @param raw the expected value
     * @return true for the ignore marker
     */
    public static boolean isIgnore(String raw) {
        return IGNORE_MARKER.equals(raw);
    }

    private static boolean isMarkerShape(String raw) {
        return raw.length() >= 3 && raw.startsWith("${") && raw.endsWith("}");
    }

    private static boolean isEscaped(String raw) {
        return raw.length() >= 4 && raw.startsWith("$${") && raw.endsWith("}");
    }

    /** The matched value, checking the deadline every few thousand reads: {@link Pattern} has no timeout. */
    private record DeadlineChars(String value, long deadline, int[] reads) implements CharSequence {

        private static final int CHECK_EVERY = 4096;

        DeadlineChars(String value, long deadline) {
            this(value, deadline, new int[1]);
        }

        @Override public char charAt(int index) {
            if (++reads[0] % CHECK_EVERY == 0 && System.nanoTime() - deadline > 0) {
                throw new Expired();
            }
            return value.charAt(index);
        }

        @Override public int length() {
            return value.length();
        }

        @Override public CharSequence subSequence(int start, int end) {
            return new DeadlineChars(value.substring(start, end), deadline, reads);
        }

        @Override public String toString() {
            return value;
        }

        /** Thrown out of the regex engine when the deadline has passed. */
        private static final class Expired extends RuntimeException {
            private static final long serialVersionUID = 1L;

            Expired() {
                super(null, null, false, false);
            }
        }
    }
}
