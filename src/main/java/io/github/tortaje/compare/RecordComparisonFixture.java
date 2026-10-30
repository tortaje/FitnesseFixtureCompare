package io.github.tortaje.compare;

import io.github.tortaje.compare.engine.CompareOptions;
import io.github.tortaje.compare.engine.ComparisonException;
import io.github.tortaje.compare.engine.Format;
import java.util.Arrays;
import java.util.function.UnaryOperator;

/**
 * Base class of the record-oriented fixtures. Settings rows ({@code |key|a, b|}, {@code |unordered|},
 * {@code |ordered|}) apply to every following compare row of the same script table.
 */
public abstract class RecordComparisonFixture extends ComparisonFixture {

    private CompareOptions current = CompareOptions.defaults();

    /**
     * Creates a fixture for one record format.
     *
     * @param format the format used for every comparison
     */
    protected RecordComparisonFixture(Format format) {
        super(format);
    }

    /**
     * Pairs records by the values of the given fields (implies unordered). Slim row: {@code |key|id, name|}.
     * Empty names fail immediately; a key the format does not accept fails at the next compare row.
     *
     * @param names comma-separated field names, trimmed
     * @throws CompareFixtureException if no name is given
     */
    public void key(String names) {
        String[] parts = Arrays.stream(names == null ? new String[0] : names.split(","))
                .map(String::trim).filter(n -> !n.isEmpty()).toArray(String[]::new);
        update(o -> o.key(parts));
    }

    /** Matches records without regard to their order. Slim row: {@code |unordered|}. */
    public void unordered() {
        update(CompareOptions::unordered);
    }

    /** Matches records in order and clears the key; a CSV separator is kept. Slim row: {@code |ordered|}. */
    public void ordered() {
        update(CompareOptions::ordered);
    }

    /**
     * Returns the options accumulated by the settings rows.
     *
     * @return the current options, never null
     */
    protected CompareOptions current() {
        return current;
    }

    /**
     * Applies a change to the current options, turning an engine error into a fixture exception.
     *
     * @param change the modification
     */
    protected void update(UnaryOperator<CompareOptions> change) {
        try {
            current = change.apply(current);
        } catch (ComparisonException e) {
            throw new CompareFixtureException(e.getMessage(), e);
        }
    }

    @Override
    protected CompareOptions options(String expectedPath) {
        return current;
    }
}
