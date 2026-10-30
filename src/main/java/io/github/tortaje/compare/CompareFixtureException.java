package io.github.tortaje.compare;

/** Failure reported to FitNesse as a Slim exception: the message is wrapped as {@code message:<<text>>}. */
public class CompareFixtureException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String text;

    /**
     * Creates the exception.
     *
     * @param text the message without the Slim wrapper
     */
    public CompareFixtureException(String text) {
        super("message:<<" + text + ">>");
        this.text = text;
    }

    /**
     * Creates the exception with a cause.
     *
     * @param text  the message without the Slim wrapper
     * @param cause the underlying failure
     */
    public CompareFixtureException(String text, Throwable cause) {
        super("message:<<" + text + ">>", cause);
        this.text = text;
    }

    /**
     * Returns the message text.
     *
     * @return the message without the Slim wrapper
     */
    public String text() {
        return text;
    }
}
