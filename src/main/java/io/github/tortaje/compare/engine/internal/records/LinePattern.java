package io.github.tortaje.compare.engine.internal.records;

import io.github.tortaje.compare.engine.ComparisonException;
import io.github.tortaje.compare.engine.internal.wildcard.Wildcard;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * An expected text line, possibly holding inline wildcard markers, compiled for matching.
 *
 * <p>The markers are compiled into one regular expression for the whole line, so capturing groups are numbered
 * across the line: in {@code ${regex:(z)}-${regex:(a)\1}} the {@code \1} refers to the group of the first marker.
 * A marker whose braces are not balanced (for instance {@code ${regex:\{}}) never closes, and the line is then
 * compared as literal text.
 */
public final class LinePattern {

    private final String text;
    private final String literal;
    private final Pattern pattern;
    private final boolean ignore;

    private LinePattern(String text, String literal, Pattern pattern, boolean ignore) {
        this.text = text;
        this.literal = literal;
        this.pattern = pattern;
        this.ignore = ignore;
    }

    /**
     * Compiles an expected line.
     *
     * @param expectedLine the line as written
     * @param path         the location, used in error messages
     * @return the compiled line
     * @throws io.github.tortaje.compare.engine.ComparisonException on an unknown marker or an invalid regex
     */
    public static LinePattern compile(String expectedLine, String path) {
        boolean ignore = Wildcard.isIgnore(expectedLine);
        StringBuilder regex = new StringBuilder();
        StringBuilder literal = new StringBuilder();
        boolean marked = false;
        int i = 0;
        int n = expectedLine.length();
        while (i < n) {
            boolean escaped = expectedLine.startsWith("$${", i);
            if (escaped || expectedLine.startsWith("${", i)) {
                int open = i + (escaped ? 1 : 0);
                int end = closing(expectedLine, open + 2);
                if (end > 0) {
                    String marker = expectedLine.substring(open, end + 1);
                    if (escaped) {
                        literal.append(marker);
                    } else {
                        flush(regex, literal);
                        regex.append(translate(marker, path));
                        marked = true;
                    }
                    i = end + 1;
                    continue;
                }
            }
            literal.append(expectedLine.charAt(i));
            i++;
        }
        Pattern p = null;
        if (marked) {
            flush(regex, literal);
            try {
                p = Pattern.compile(regex.toString(), Pattern.DOTALL);
            } catch (PatternSyntaxException e) {
                throw new ComparisonException(
                        "Invalid regex in '" + expectedLine + "' at " + path + ": " + e.getDescription(), e);
            }
        }
        return new LinePattern(expectedLine, p == null ? literal.toString() : null, p, ignore);
    }

    /**
     * Wraps an actual line, which is never interpreted: it only carries its text.
     *
     * @param line the actual line
     * @return a pattern holding the line
     */
    static LinePattern literal(String line) {
        return new LinePattern(line, line, null, false);
    }

    /** Index of the brace closing a marker whose body starts at {@code from}, or -1. */
    private static int closing(String s, int from) {
        int depth = 1;
        for (int j = from; j < s.length(); j++) {
            char c = s.charAt(j);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return j;
            }
        }
        return -1;
    }

    private static void flush(StringBuilder regex, StringBuilder literal) {
        if (literal.length() > 0) {
            regex.append(Pattern.quote(literal.toString()));
            literal.setLength(0);
        }
    }

    private static String translate(String marker, String path) {
        Wildcard w = Wildcard.parse(marker, path).orElseThrow();
        return switch (w.kind()) {
            case ANY, IGNORE -> ".*?";
            case ANY_STRING -> ".+?";
            case ANY_NUMBER -> Wildcard.NUMBER_SHAPE;
            case REGEX -> "(?:" + w.pattern().pattern() + ")";
        };
    }

    /**
     * Tells whether an actual line matches this expected line as a whole.
     *
     * @param actualLine the actual line
     * @param path       the location, used in error messages
     * @return true on a match
     * @throws io.github.tortaje.compare.engine.ComparisonException when matching exceeds the regex time limit
     */
    public boolean matches(String actualLine, String path) {
        return pattern == null ? literal.equals(actualLine) : Wildcard.matches(pattern, text, actualLine, path);
    }

    /**
     * Tells whether the whole line is {@code ${ignore}}.
     *
     * @return true for an optional ignore line
     */
    public boolean isIgnore() {
        return ignore;
    }

    /**
     * Returns the line as written.
     *
     * @return the expected line text
     */
    public String text() {
        return text;
    }
}
