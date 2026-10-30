package io.github.tortaje.compare;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import fitnesse.junit.FitNesseRunner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

/**
 * Runs CompareErrorSuite, whose rows must all raise, and checks that FitNesse reports the page as failed with
 * exceptions and renders the fixture messages.
 */
public class ErrorCellsTest {

    private static final Path OUTPUT_DIR = Path.of("target/fitnesse-results");

    /** Configuration of the error suite run; not a test class itself. */
    @FitNesseRunner.Suite("CompareErrorSuite")
    @FitNesseRunner.FitnesseDir("src/test/fitnesse")
    @FitNesseRunner.OutputDir("target/fitnesse-results")
    @FitNesseRunner.PreventSystemExit(false)
    public static class ErrorSuite {
    }

    @Test
    public void errorCellsRaiseRenderedExceptions() throws Exception {
        deletePreviousResults();
        List<Failure> failures = new ArrayList<>();
        RunNotifier notifier = new RunNotifier();
        notifier.addListener(new RunListener() {
            @Override
            public void testFailure(Failure failure) {
                failures.add(failure);
            }
        });

        new QuietFitNesseRunner(ErrorSuite.class).run(notifier);

        assertEquals(failures.toString(), 1, failures.size());
        Failure failure = failures.get(0);
        assertTrue(failure.getDescription().toString(), failure.getDescription().toString().contains("ErrorCells"));
        assertFalse("an exception, not a wrong check", failure.getException() instanceof AssertionError);
        assertTrue(failure.getMessage(), failure.getMessage().contains("Expected file"));

        String html = Files.readString(resultFile("ErrorCells"), StandardCharsets.UTF_8);
        for (String message : List.of(
                "Expected file 'src/test/resources/e2e/expected/missing.json' not found",
                "Actual file 'src/test/resources/e2e/actual/missing.json' not found",
                "actual: unexpected end of content at line 1, column 6",
                "expected: duplicate key 'a' at line 1, column 8",
                "actual: content is empty",
                "Unknown wildcard '${nope}' at $.a",
                "Invalid regex in '${regex:[0-9}' at $.a",
                // XML parser messages are localized: only the position and the DOCTYPE keyword are fixed
                "at line 1, column 9",
                "DOCTYPE",
                "at line 1, column 10",
                "Unknown wildcard '${nope}' at /a",
                "actual: line 2: unexpected character 'x' at column 6",
                "key 'id' cannot be a wildcard in expected line 1",
                "actual: line 2: 3 fields, header has 2",
                "expected: line 2: unterminated quoted field",
                "actual: duplicate column 'id'",
                "key column 'idd' not in expected header [id, v]",
                "Invalid CSV separator ';;'",
                "Option 'key' does not apply to TEXT")) {
            assertTrue(message, html.contains(message) || html.contains(message.replace("'", "&#39;")));
        }
        assertEquals(18, count(html, "<span class=\"error\">"));
        assertEquals("only the import and the five fixtures pass", 6, count(html, "<span class=\"pass\">"));
    }

    private static void deletePreviousResults() throws IOException {
        if (!Files.isDirectory(OUTPUT_DIR)) {
            return;
        }
        try (Stream<Path> files = Files.list(OUTPUT_DIR)) {
            for (Path f : (Iterable<Path>) files::iterator) {
                if (f.getFileName().toString().startsWith("CompareErrorSuite")) {
                    Files.delete(f);
                }
            }
        }
    }

    private static Path resultFile(String page) throws IOException {
        try (Stream<Path> files = Files.list(OUTPUT_DIR)) {
            return files.filter(f -> f.getFileName().toString().startsWith("CompareErrorSuite.")
                            && f.getFileName().toString().contains(page) && f.toString().endsWith(".html"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no " + page + " result in " + OUTPUT_DIR));
        }
    }

    private static int count(String text, String token) {
        Matcher m = Pattern.compile(Pattern.quote(token)).matcher(text);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }
}
