package io.github.tortaje.compare.engine.internal.records;

import io.github.tortaje.compare.engine.ComparisonException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** RFC 4180 CSV reader with a configurable separator. */
public final class CsvParser {

    /**
     * A parsed CSV document.
     *
     * @param header the column names
     * @param rows   the data rows
     */
    public record Csv(List<String> header, List<CsvRow> rows) { }

    /**
     * A data row.
     *
     * @param line   physical line (1-based) where the record starts
     * @param fields the cells, as many as the header has columns
     */
    public record CsvRow(int line, List<String> fields) { }

    private CsvParser() { }

    /**
     * Parses CSV text. The first record is the header; empty unquoted records are skipped.
     *
     * @param text      the content (a leading BOM is ignored; CRLF and LF both end a record)
     * @param separator the field separator
     * @param side      {@code "expected"} or {@code "actual"}, used in error messages
     * @return the header and data rows
     * @throws ComparisonException on empty content, an unterminated quote, a duplicate column or a row whose
     *                             field count differs from the header
     */
    public static Csv parse(String text, char separator, String side) {
        String t = text == null ? "" : text;
        if (!t.isEmpty() && t.charAt(0) == '\uFEFF') {
            t = t.substring(1);
        }
        if (t.isBlank()) {
            throw new ComparisonException(side + ": content is empty");
        }
        List<String> header = null;
        List<CsvRow> rows = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quotedCell = false;
        boolean anyQuoted = false;
        boolean started = false;
        int line = 1;
        int recordLine = 1;
        int n = t.length();
        int i = 0;
        while (i <= n) {
            boolean eof = i == n;
            char c = eof ? '\n' : t.charAt(i);
            boolean crlf = !eof && c == '\r' && i + 1 < n && t.charAt(i + 1) == '\n';
            if (!eof && c == '"' && cell.length() == 0 && !quotedCell) {
                int[] end = quoted(t, i + 1, cell, side, recordLine);
                line += end[1];
                i = end[0];
                quotedCell = true;
                anyQuoted = true;
                started = true;
            } else if (eof || c == '\n' || crlf) {
                if (started) {
                    fields.add(cell.toString());
                    boolean blank = fields.size() == 1 && fields.get(0).isEmpty() && !anyQuoted;
                    if (!blank) {
                        if (header == null) {
                            header = header(fields, side);
                        } else {
                            if (fields.size() != header.size()) {
                                throw new ComparisonException(side + ": line " + recordLine + ": " + fields.size()
                                        + " fields, header has " + header.size());
                            }
                            rows.add(new CsvRow(recordLine, fields));
                        }
                    }
                }
                fields = new ArrayList<>();
                cell.setLength(0);
                quotedCell = false;
                anyQuoted = false;
                started = false;
                i += crlf ? 2 : 1;
                line++;
                recordLine = line;
            } else if (c == separator) {
                fields.add(cell.toString());
                cell.setLength(0);
                quotedCell = false;
                started = true;
                i++;
            } else {
                cell.append(c);
                started = true;
                i++;
            }
        }
        return new Csv(header, rows);
    }

    private static List<String> header(List<String> fields, String side) {
        Set<String> seen = new HashSet<>();
        for (String name : fields) {
            if (!seen.add(name)) {
                throw new ComparisonException(side + ": duplicate column '" + name + "'");
            }
        }
        return fields;
    }

    /** Reads a quoted cell body from {@code from}; returns {index after the closing quote, line breaks read}. */
    private static int[] quoted(String t, int from, StringBuilder cell, String side, int recordLine) {
        int breaks = 0;
        int i = from;
        while (i < t.length()) {
            char c = t.charAt(i);
            if (c == '"') {
                if (i + 1 < t.length() && t.charAt(i + 1) == '"') {
                    cell.append('"');
                    i += 2;
                    continue;
                }
                return new int[] {i + 1, breaks};
            }
            if (c == '\n') {
                breaks++;
            }
            cell.append(c);
            i++;
        }
        throw new ComparisonException(side + ": line " + recordLine + ": unterminated quoted field");
    }
}
