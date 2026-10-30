package io.github.tortaje.compare.engine.internal.records;

import io.github.tortaje.compare.engine.Difference;
import io.github.tortaje.compare.engine.DifferenceKind;
import java.util.ArrayList;
import java.util.List;

/** Text records: one record per physical line. Expected records are {@link LinePattern}, actual ones strings. */
public final class TextRecords {

    /**
     * The record format of text. Expected and actual records are both {@link LinePattern}; an actual line is a
     * pattern compiled without wildcard interpretation (see {@link #records}).
     */
    public static final RecordFormat<LinePattern> FORMAT = new RecordFormat<>() {
        @Override public String label() {
            return "line";
        }

        @Override public void compare(String path, LinePattern expected, LinePattern actual, List<Difference> out) {
            if (!expected.matches(actual.text(), path)) {
                out.add(new Difference(path, DifferenceKind.VALUE, quote(expected.text()), quote(actual.text())));
            }
        }

        @Override public boolean matches(LinePattern expected, LinePattern actual) {
            return expected.matches(actual.text(), "line");
        }

        /** Never reached: TEXT rejects the {@code key} option. */
        @Override public String key(LinePattern record, List<String> key, boolean expectedSide, int position) {
            throw new UnsupportedOperationException("text lines have no key");
        }

        @Override public String display(LinePattern record) {
            return quote(record.text());
        }

        @Override public boolean isIgnore(LinePattern expected) {
            return expected.isIgnore();
        }
    };

    private TextRecords() { }

    private static String quote(String s) {
        return "\"" + s + "\"";
    }

    /**
     * Splits text into line records.
     *
     * @param content  the text (leading BOM dropped, CRLF and trailing spaces ignored, final blank lines dropped;
     *                 empty means no record)
     * @param expected whether the lines are expected ones (compiled with wildcards) or actual ones (literal)
     * @return the records, positioned by physical line
     * @throws io.github.tortaje.compare.engine.ComparisonException on an invalid wildcard in an expected line
     */
    public static List<Rec<LinePattern>> records(String content, boolean expected) {
        List<Rec<LinePattern>> out = new ArrayList<>();
        if (content == null) {
            return out;
        }
        String t = !content.isEmpty() && content.charAt(0) == '\uFEFF' ? content.substring(1) : content;
        String[] lines = t.split("\n", -1);
        int last = lines.length;
        while (last > 0 && stripTrailing(lines[last - 1]).isEmpty()) {
            last--;
        }
        for (int i = 0; i < last; i++) {
            String line = stripTrailing(lines[i]);
            out.add(new Rec<>(i + 1, expected
                    ? LinePattern.compile(line, "line[" + (i + 1) + "]")
                    : LinePattern.literal(line)));
        }
        return out;
    }

    private static String stripTrailing(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(0, end);
    }
}
