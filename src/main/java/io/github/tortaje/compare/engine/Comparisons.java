package io.github.tortaje.compare.engine;

import io.github.tortaje.compare.engine.internal.TextFiles;
import io.github.tortaje.compare.engine.internal.json.JsonComparator;
import io.github.tortaje.compare.engine.internal.json.JsonParser;
import io.github.tortaje.compare.engine.internal.json.JsonValue;
import io.github.tortaje.compare.engine.internal.records.CsvParser;
import io.github.tortaje.compare.engine.internal.records.CsvRecords;
import io.github.tortaje.compare.engine.internal.records.JsonLinesRecords;
import io.github.tortaje.compare.engine.internal.records.RecordSequence;
import io.github.tortaje.compare.engine.internal.records.TextRecords;
import io.github.tortaje.compare.engine.internal.xml.XmlComparator;
import io.github.tortaje.compare.engine.internal.xml.XmlParser;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import org.w3c.dom.Document;

/** Static facade of the comparison engine. */
public final class Comparisons {

    private Comparisons() { }

    /**
     * Compares an actual JSON document against an expected template.
     *
     * @param expected the expected JSON template (wildcards allowed)
     * @param actual   the actual JSON document
     * @return the comparison result
     * @throws ComparisonException on a syntax error (naming the side), empty content or an invalid wildcard
     */
    public static ComparisonResult compareJson(String expected, String actual) {
        JsonValue e = JsonParser.parse(expected, "expected");
        JsonValue a = JsonParser.parse(actual, "actual");
        return ComparisonResult.of(JsonComparator.compare(e, a));
    }

    /**
     * Compares an actual XML document against an expected template.
     *
     * @param expected the expected XML template (wildcards allowed in texts and attribute values)
     * @param actual   the actual XML document
     * @return the comparison result
     * @throws ComparisonException on a syntax error (naming the side), empty content or an invalid wildcard
     */
    public static ComparisonResult compareXml(String expected, String actual) {
        Document e = XmlParser.parse(expected, "expected");
        Document a = XmlParser.parse(actual, "actual");
        return ComparisonResult.of(XmlComparator.compare(e, a));
    }

    /**
     * Compares JSON Lines content line by line (blank lines are skipped, wildcards allowed).
     *
     * @param expected the expected JSON Lines template
     * @param actual   the actual JSON Lines content
     * @param options  comparison options ({@code unordered}, {@code key}), or {@code null} for the defaults
     * @return the comparison result
     * @throws ComparisonException on a syntax error (naming the side and line), a wildcard key or an invalid option
     */
    public static ComparisonResult compareJsonLines(String expected, String actual, CompareOptions options) {
        CompareOptions o = options == null ? CompareOptions.defaults() : options;
        o.requireApplicableTo(Format.JSON_LINES);
        return ComparisonResult.of(RecordSequence.compare(JsonLinesRecords.FORMAT,
                JsonLinesRecords.records(expected, "expected"), JsonLinesRecords.records(actual, "actual"), o));
    }

    /**
     * Compares text line by line (CRLF, trailing spaces and final blank lines ignored; inline wildcards allowed).
     *
     * @param expected the expected text template
     * @param actual   the actual text
     * @param options  comparison options (only {@code unordered}), or {@code null} for the defaults
     * @return the comparison result
     * @throws ComparisonException on an invalid wildcard or option
     */
    public static ComparisonResult compareText(String expected, String actual, CompareOptions options) {
        CompareOptions o = options == null ? CompareOptions.defaults() : options;
        o.requireApplicableTo(Format.TEXT);
        return ComparisonResult.of(RecordSequence.compare(TextRecords.FORMAT,
                TextRecords.records(expected, true), TextRecords.records(actual, false), o));
    }

    /**
     * Compares CSV content by header column name (columns in any order, numbers by value, wildcards allowed).
     *
     * @param expected the expected CSV template, header first
     * @param actual   the actual CSV content
     * @param options  comparison options ({@code unordered}, {@code key}, {@code separator}), or {@code null}
     * @return the comparison result
     * @throws ComparisonException on a syntax error (naming the side and line), a bad key or an invalid option
     */
    public static ComparisonResult compareCsv(String expected, String actual, CompareOptions options) {
        CompareOptions o = options == null ? CompareOptions.defaults() : options;
        o.requireApplicableTo(Format.CSV);
        char sep = o.separator().orElse(',');
        return ComparisonResult.of(CsvRecords.compare(CsvParser.parse(expected, sep, "expected"),
                CsvParser.parse(actual, sep, "actual"), o));
    }

    /**
     * Compares an actual document against an expected template in the given format.
     *
     * @param format   the document format
     * @param expected the expected template (wildcards allowed)
     * @param actual   the actual document
     * @param options  comparison options, or {@code null} for the defaults
     * @return the comparison result
     * @throws ComparisonException on a syntax error (naming the side), empty content or an invalid wildcard
     */
    public static ComparisonResult compare(Format format, String expected, String actual, CompareOptions options) {
        Objects.requireNonNull(format, "format");
        if (options != null) {
            options.requireApplicableTo(format);
        }
        return switch (format) {
            case JSON -> compareJson(expected, actual);
            case XML -> compareXml(expected, actual);
            case JSON_LINES -> compareJsonLines(expected, actual, options);
            case TEXT -> compareText(expected, actual, options);
            case CSV -> compareCsv(expected, actual, options);
        };
    }

    /**
     * Compares an actual document against an expected template stored in a UTF-8 file; the format is
     * taken from the file extension ({@code .json}, {@code .xml}, {@code .jsonl}, {@code .ndjson}, {@code .txt},
     * {@code .log}, {@code .csv} or {@code .tsv}).
     *
     * @param expected path of the expected template file
     * @param actual   the actual document
     * @return the comparison result
     * @throws ComparisonException if the extension is unknown, the file is missing or unreadable, or on
     *                             any error reported by {@link #compare}
     */
    public static ComparisonResult compareToFile(Path expected, String actual) {
        return compareToFile(expected, actual, null);
    }

    /**
     * Compares an actual document against an expected template stored in a UTF-8 file, with options; the
     * format is taken from the file extension.
     *
     * @param expected path of the expected template file
     * @param actual   the actual document
     * @param options  comparison options, or {@code null} for the defaults
     * @return the comparison result
     * @throws ComparisonException if the extension is unknown, the file is missing or unreadable, or on
     *                             any error reported by {@link #compare}
     */
    public static ComparisonResult compareToFile(Path expected, String actual, CompareOptions options) {
        Path name = expected.getFileName(); // null for a root such as "/"
        String fileName = name == null ? expected.toString() : name.toString();
        Format format = Format.fromFileName(fileName);
        CompareOptions o = options;
        if (format == Format.CSV && fileName.toLowerCase(Locale.ROOT).endsWith(".tsv")
                && (o == null || o.separator().isEmpty())) {
            o = (o == null ? CompareOptions.defaults() : o).separator('\t');
        }
        return compare(format, TextFiles.read(expected, "expected"), actual, o);
    }
}
