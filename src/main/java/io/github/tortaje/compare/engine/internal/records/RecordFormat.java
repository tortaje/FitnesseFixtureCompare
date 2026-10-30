package io.github.tortaje.compare.engine.internal.records;

import io.github.tortaje.compare.engine.Difference;
import java.util.ArrayList;
import java.util.List;

/**
 * What {@link RecordSequence} needs to know about a record format.
 *
 * @param <T> record type
 */
public interface RecordFormat<T> {

    /**
     * Returns the position label.
     *
     * @return {@code "line"} or {@code "row"}
     */
    String label();

    /**
     * Compares two paired records.
     *
     * @param path     path of the record, e.g. {@code row[4]}; the format appends its own suffix
     * @param expected expected record
     * @param actual   actual record
     * @param out      receives the differences
     */
    void compare(String path, T expected, T actual, List<Difference> out);

    /**
     * Tells whether two records match, i.e. whether {@link #compare} would find no difference. Formats override it
     * to stop at the first difference.
     *
     * @param expected expected record
     * @param actual   actual record
     * @return true when they match
     */
    default boolean matches(T expected, T actual) {
        List<Difference> scratch = new ArrayList<>();
        compare(label(), expected, actual, scratch);
        return scratch.isEmpty();
    }

    /**
     * Returns the matching key of a record.
     *
     * @param record       the record
     * @param key          key names
     * @param expectedSide whether the record is an expected one (wildcards are then refused)
     * @param position     position of the record, for error messages
     * @return the key, or {@code null} when the record has no value for it
     */
    String key(T record, List<String> key, boolean expectedSide, int position);

    /**
     * Returns the text shown for a record in {@code missing} and {@code unexpected}.
     *
     * @param record the record
     * @return its display value
     */
    String display(T record);

    /**
     * Tells whether an expected record is an optional {@code ${ignore}} line.
     *
     * @param expected the expected record
     * @return {@code true} for an ignore line
     */
    default boolean isIgnore(T expected) {
        return false;
    }
}
