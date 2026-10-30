package io.github.tortaje.compare;

import io.github.tortaje.compare.engine.Format;

/**
 * Slim fixture comparing JSON Lines documents. Slim rows: {@code compare file to file}, {@code compare file to},
 * {@code compare;}, {@code key}, {@code unordered} and {@code ordered}.
 */
public class JsonLinesComparison extends RecordComparisonFixture {

    /** Creates the JSON Lines comparison fixture. */
    public JsonLinesComparison() {
        super(Format.JSON_LINES);
    }
}
