package io.github.tortaje.compare.engine;

/** Thrown when a comparison cannot be performed (bad input, unreadable document, unknown format). */
public class ComparisonException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ComparisonException(String message) {
        super(message);
    }

    public ComparisonException(String message, Throwable cause) {
        super(message, cause);
    }
}
