package io.github.tortaje.compare.engine;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable comparison options. Every modifier returns a new instance. Which options a format accepts is
 * decided by {@link #requireApplicableTo(Format)}.
 */
public final class CompareOptions {

    private static final CompareOptions DEFAULTS = new CompareOptions(false, List.of(), null);

    private final boolean unordered;
    private final List<String> keyNames;
    private final Character separator;

    private CompareOptions(boolean unordered, List<String> keyNames, Character separator) {
        this.unordered = unordered;
        this.keyNames = List.copyOf(keyNames);
        this.separator = separator;
    }

    /**
     * Returns the default options: ordered, no key, no separator.
     *
     * @return the defaults
     */
    public static CompareOptions defaults() {
        return DEFAULTS;
    }

    /**
     * Returns options comparing records without regard to their order.
     *
     * @return new options with unordered matching
     */
    public CompareOptions unordered() {
        return new CompareOptions(true, keyNames, separator);
    }

    /**
     * Returns options pairing records by the values of the given fields (implies unordered matching).
     *
     * @param names one or more field names
     * @return new options with the key set
     * @throws ComparisonException if no name is given
     */
    public CompareOptions key(String... names) {
        if (names == null || names.length == 0) {
            throw new ComparisonException("key needs at least one name");
        }
        for (String name : names) {
            Objects.requireNonNull(name, "key name");
        }
        return new CompareOptions(true, List.of(names), separator);
    }

    /**
     * Returns options with the given CSV separator.
     *
     * @param c the separator character
     * @return new options with the separator set
     * @throws ComparisonException if the character is a double quote, a carriage return or a line feed
     */
    public CompareOptions separator(char c) {
        if (c == '"' || c == '\n' || c == '\r') {
            throw new ComparisonException("Invalid CSV separator '" + c + "'");
        }
        return new CompareOptions(unordered, keyNames, c);
    }

    /**
     * Returns options comparing records in order, clearing unordered matching and the key.
     *
     * @return new options with ordered matching
     */
    public CompareOptions ordered() {
        return new CompareOptions(false, List.of(), separator);
    }

    /**
     * Tells whether records are matched without regard to order (explicitly, or because a key is set).
     *
     * @return {@code true} for unordered matching
     */
    public boolean isUnordered() {
        return unordered;
    }

    /**
     * Returns the key field names.
     *
     * @return the names, empty when no key is set
     */
    public List<String> keyNames() {
        return keyNames;
    }

    /**
     * Returns the CSV separator.
     *
     * @return the separator, empty when not set
     */
    public Optional<Character> separator() {
        return Optional.ofNullable(separator);
    }

    /**
     * Checks that every option set here applies to the given format.
     *
     * @param format the document format
     * @throws ComparisonException naming the first option (separator, key, unordered) the format does not accept
     */
    public void requireApplicableTo(Format format) {
        Set<String> allowed = allowedOptions(format);
        if (separator != null && !allowed.contains("separator")) {
            throw notApplicable("separator", format);
        }
        if (!keyNames.isEmpty() && !allowed.contains("key")) {
            throw notApplicable("key", format);
        }
        if (unordered && keyNames.isEmpty() && !allowed.contains("unordered")) {
            throw notApplicable("unordered", format);
        }
    }

    /** The one place deciding which options each format accepts. */
    private static Set<String> allowedOptions(Format format) {
        return switch (format) {
            case JSON, XML -> Set.of();
            case JSON_LINES -> Set.of("unordered", "key");
            case TEXT -> Set.of("unordered");
            case CSV -> Set.of("unordered", "key", "separator");
        };
    }

    private static ComparisonException notApplicable(String option, Format format) {
        return new ComparisonException("Option '" + option + "' does not apply to " + format.name());
    }
}
