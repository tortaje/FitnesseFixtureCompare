package io.github.tortaje.compare.engine.internal.records;

/**
 * A record of a sequence, with its position.
 *
 * @param position physical line (JSON Lines, text) or data row rank (CSV), 1-based
 * @param value    the parsed record
 * @param <T>      record type of the format
 */
public record Rec<T>(int position, T value) {
}
