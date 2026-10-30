package io.github.tortaje.compare;

import io.github.tortaje.compare.engine.Format;

/**
 * Slim fixture comparing text documents line by line. Slim rows: {@code compare file to file},
 * {@code compare file to}, {@code compare;}, {@code unordered} and {@code ordered}. A {@code key} row is accepted
 * but fails at the next compare row, since text has no fields.
 */
public class TextComparison extends RecordComparisonFixture {

    /** Creates the text comparison fixture. */
    public TextComparison() {
        super(Format.TEXT);
    }
}
