package io.github.tortaje.compare;

import io.github.tortaje.compare.engine.CompareOptions;
import io.github.tortaje.compare.engine.Format;
import java.util.Locale;

/**
 * Slim fixture comparing CSV documents. Slim rows: {@code compare file to file}, {@code compare file to},
 * {@code compare;}, {@code key}, {@code unordered}, {@code ordered} and {@code separator}. An expected file named
 * {@code *.tsv} is read with a tab separator unless a separator was set.
 */
public class CsvComparison extends RecordComparisonFixture {

    /** Creates the CSV comparison fixture. */
    public CsvComparison() {
        super(Format.CSV);
    }

    /**
     * Sets the field separator. Slim row: {@code |separator|;|}.
     *
     * @param value {@code tab} (a tab cannot be typed in a wiki cell) or exactly one character
     * @throws CompareFixtureException if the value is not valid
     */
    public void separator(String value) {
        if ("tab".equals(value)) {
            update(o -> o.separator('\t'));
        } else if (value != null && value.length() == 1) {
            update(o -> o.separator(value.charAt(0)));
        } else {
            throw new CompareFixtureException("Invalid CSV separator '" + value + "'");
        }
    }

    @Override
    protected CompareOptions options(String expectedPath) {
        CompareOptions o = current();
        if (o.separator().isEmpty() && expectedPath != null
                && expectedPath.toLowerCase(Locale.ROOT).endsWith(".tsv")) {
            return o.separator('\t');
        }
        return o;
    }
}
