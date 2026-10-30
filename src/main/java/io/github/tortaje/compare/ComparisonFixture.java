package io.github.tortaje.compare;

import io.github.tortaje.compare.engine.CompareOptions;
import io.github.tortaje.compare.engine.ComparisonException;
import io.github.tortaje.compare.engine.ComparisonResult;
import io.github.tortaje.compare.engine.Comparisons;
import io.github.tortaje.compare.engine.Format;
import io.github.tortaje.compare.engine.internal.TextFiles;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Base class of the Slim comparison fixtures: each method returns {@code OK} or an HTML list of
 * differences, to be asserted with a {@code check} row.
 */
public abstract class ComparisonFixture {

    private final Format format;

    /**
     * Creates a fixture for one document format.
     *
     * @param format the format used for every comparison, regardless of file extensions
     */
    protected ComparisonFixture(Format format) {
        this.format = format;
    }

    /**
     * Compares two files. Slim row: {@code |check|compare file|e|to file|a|OK|}.
     *
     * @param expectedPath path of the expected file, relative to the working directory
     * @param actualPath   path of the actual file, relative to the working directory
     * @return {@code OK} or the differences as HTML
     * @throws CompareFixtureException if a path is invalid, a file is missing or a document cannot be parsed
     */
    public String compareFileToFile(String expectedPath, String actualPath) {
        return html(() -> {
            Path expectedFile = TextFiles.path(expectedPath, "expected");
            Path actualFile = TextFiles.path(actualPath, "actual");
            String expected = TextFiles.read(expectedFile, "expected");
            String actual = TextFiles.read(actualFile, "actual");
            return Comparisons.compare(format, expected, actual, options(expectedPath));
        });
    }

    /**
     * Compares an expected file to inline content. Slim row: {@code |check|compare file|e|to|actual|OK|}.
     *
     * @param expectedPath path of the expected file, relative to the working directory
     * @param actual       the actual document text (a {@code <pre>} cell is unwrapped)
     * @return {@code OK} or the differences as HTML
     * @throws CompareFixtureException if the path is invalid, the file is missing or a document cannot be parsed
     */
    public String compareFileTo(String expectedPath, String actual) {
        return html(() -> {
            String expected = TextFiles.read(TextFiles.path(expectedPath, "expected"), "expected");
            return Comparisons.compare(format, expected, CellText.normalize(actual, format),
                    options(expectedPath));
        });
    }

    /**
     * Compares two inline documents. Slim row: {@code |check|compare;|expected|actual|OK|}.
     *
     * @param expected the expected document text (a {@code <pre>} cell is unwrapped)
     * @param actual   the actual document text (a {@code <pre>} cell is unwrapped)
     * @return {@code OK} or the differences as HTML
     * @throws CompareFixtureException if a document cannot be parsed
     */
    public String compare(String expected, String actual) {
        return html(() -> Comparisons.compare(format, CellText.normalize(expected, format),
                CellText.normalize(actual, format), options(null)));
    }

    /**
     * Supplies the comparison options of the next row. The default is {@code null}, the 1.0 behaviour.
     *
     * @param expectedPath the expected file path of the row, {@code null} for an inline {@code compare;} row
     * @return the options, or {@code null} for none
     */
    protected CompareOptions options(String expectedPath) {
        return null;
    }

    private static String html(Supplier<ComparisonResult> comparison) {
        try {
            return comparison.get().toHtml();
        } catch (ComparisonException e) {
            throw new CompareFixtureException(e.getMessage(), e);
        }
    }
}
