package io.github.tortaje.compare.engine.internal.records;

import io.github.tortaje.compare.engine.ComparisonException;
import io.github.tortaje.compare.engine.CompareOptions;
import io.github.tortaje.compare.engine.Difference;
import io.github.tortaje.compare.engine.DifferenceKind;
import io.github.tortaje.compare.engine.internal.json.JsonPaths;
import io.github.tortaje.compare.engine.internal.wildcard.Wildcard;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** CSV records: one row per data line, cells addressed by header name. */
public final class CsvRecords {

    private CsvRecords() { }

    /** An expected cell, analysed once: wildcard or literal, and the literal's numeric value. */
    private record Cell(Wildcard wildcard, String literal, BigDecimal number) { }

    /** The record format of CSV rows (a record maps column name to cell, in header order); one per comparison. */
    private static final class CsvFormat implements RecordFormat<Map<String, String>> {

        private final Map<String, Cell> cells = new HashMap<>();

        @Override public String label() {
            return "row";
        }

        @Override public void compare(String path, Map<String, String> expected, Map<String, String> actual,
                                      List<Difference> out) {
            for (Map.Entry<String, String> e : expected.entrySet()) {
                Difference d = cell(path, e.getKey(), e.getValue(), actual.get(e.getKey()));
                if (d != null) {
                    out.add(d);
                }
            }
        }

        @Override public boolean matches(Map<String, String> expected, Map<String, String> actual) {
            for (Map.Entry<String, String> e : expected.entrySet()) {
                if (cell("row", e.getKey(), e.getValue(), actual.get(e.getKey())) != null) {
                    return false;
                }
            }
            return true;
        }

        @Override public String key(Map<String, String> record, List<String> key, boolean expectedSide,
                                    int position) {
            List<String> parts = new ArrayList<>();
            for (String name : key) {
                String v = record.get(name);
                if (v == null) {
                    return null;
                }
                if (expectedSide) {
                    if (Wildcard.parse(v, JsonPaths.key("row[" + position + "]", name)).isPresent()) {
                        throw new ComparisonException(
                                "key '" + name + "' cannot be a wildcard in expected row " + position);
                    }
                    v = Wildcard.literal(v);
                }
                parts.add(v);
            }
            return String.join("\u0000", parts);
        }

        @Override public String display(Map<String, String> record) {
            StringBuilder sb = new StringBuilder("{");
            for (Map.Entry<String, String> e : record.entrySet()) {
                if (sb.length() > 1) {
                    sb.append(", ");
                }
                sb.append(e.getKey()).append('=').append(e.getValue());
            }
            return sb.append('}').toString();
        }

        /** Compares one cell; the path is only built when needed. Returns null on a match. */
        private Difference cell(String row, String name, String expected, String actual) {
            Cell c = cells.get(expected);
            if (c == null) {
                c = analyse(JsonPaths.key(row, name), expected);
                cells.put(expected, c);
            }
            if (c.wildcard() != null) {
                Wildcard w = c.wildcard();
                switch (w.kind()) {
                    case ANY_NUMBER -> {
                        if (!Wildcard.isNumberShape(actual)) {
                            return new Difference(JsonPaths.key(row, name), DifferenceKind.TYPE, "number", actual);
                        }
                    }
                    case REGEX -> {
                        String path = JsonPaths.key(row, name);
                        if (!w.matches(actual, path)) {
                            return new Difference(path, DifferenceKind.PATTERN, w.pattern().pattern(), actual);
                        }
                    }
                    default -> { }
                }
                return null;
            }
            BigDecimal y = c.number() == null ? null : number(actual);
            boolean same = y != null ? c.number().compareTo(y) == 0 : c.literal().equals(actual);
            return same ? null : new Difference(JsonPaths.key(row, name), DifferenceKind.VALUE, c.literal(), actual);
        }
    }

    private static Cell analyse(String path, String expected) {
        Optional<Wildcard> wc = Wildcard.parse(expected, path);
        if (wc.isPresent()) {
            return new Cell(wc.get(), null, null);
        }
        String lit = Wildcard.literal(expected);
        return new Cell(null, lit, number(lit));
    }

    /**
     * Compares two parsed CSV documents.
     *
     * @param expected the expected document
     * @param actual   the actual document
     * @param options  comparison options, {@code null} for the defaults
     * @return the differences: a single {@code header} difference when the column sets differ, else the rows'
     * @throws ComparisonException on a key column missing from the expected header or a wildcard key
     */
    public static List<Difference> compare(CsvParser.Csv expected, CsvParser.Csv actual, CompareOptions options) {
        if (options != null) {
            for (String name : options.keyNames()) {
                if (!expected.header().contains(name)) {
                    throw new ComparisonException(
                            "key column '" + name + "' not in expected header " + expected.header());
                }
            }
        }
        if (!new HashSet<>(expected.header()).equals(new HashSet<>(actual.header()))) {
            return List.of(new Difference("header", DifferenceKind.VALUE,
                    expected.header().toString(), actual.header().toString()));
        }
        return RecordSequence.compare(new CsvFormat(), records(expected), records(actual), options);
    }

    private static List<Rec<Map<String, String>>> records(CsvParser.Csv csv) {
        List<Rec<Map<String, String>>> out = new ArrayList<>();
        int rank = 1;
        for (CsvParser.CsvRow row : csv.rows()) {
            Map<String, String> m = new LinkedHashMap<>();
            for (int i = 0; i < csv.header().size(); i++) {
                m.put(csv.header().get(i), row.fields().get(i));
            }
            out.add(new Rec<>(rank++, m));
        }
        return out;
    }

    /** Parses a cell with {@code new BigDecimal}; text that cannot possibly parse skips the exception. */
    private static BigDecimal number(String cell) {
        if (cell == null || cell.isEmpty()) {
            return null;
        }
        for (int i = 0; i < cell.length(); i++) {
            if ("0123456789+-.eE".indexOf(cell.charAt(i)) < 0) {
                return null;
            }
        }
        try {
            return new BigDecimal(cell);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
