package io.github.tortaje.compare.engine;

import java.util.Locale;

/** Document format handled by the comparison engine. */
public enum Format {
    JSON,
    XML,
    /** JSON Lines: one JSON value per line ({@code .jsonl}, {@code .ndjson}). */
    JSON_LINES,
    /** Line-by-line text ({@code .txt}, {@code .log}). */
    TEXT,
    /** CSV with a header row ({@code .csv}, {@code .tsv}). */
    CSV;

    /**
     * Determines the format from a file name extension, ignoring case.
     *
     * @param name file name or path with a known extension ({@code .json}, {@code .xml}, {@code .jsonl},
     *             {@code .ndjson}, {@code .txt}, {@code .log}, {@code .csv}, {@code .tsv})
     * @return the matching format
     * @throws ComparisonException if the extension is not one of the above
     */
    public static Format fromFileName(String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".json")) {
            return JSON;
        }
        if (lower.endsWith(".xml")) {
            return XML;
        }
        if (lower.endsWith(".jsonl") || lower.endsWith(".ndjson")) {
            return JSON_LINES;
        }
        if (lower.endsWith(".txt") || lower.endsWith(".log")) {
            return TEXT;
        }
        if (lower.endsWith(".csv") || lower.endsWith(".tsv")) {
            return CSV;
        }
        throw new ComparisonException("Cannot tell the format of '" + name
                + "': use a .json, .xml, .jsonl, .ndjson, .txt, .log, .csv or .tsv file");
    }
}
