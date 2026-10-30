package io.github.tortaje.compare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XmlComparisonFixtureTest {
    @Test void inlineMatch() { assertEquals("OK", new XmlComparison().compare("<a>${any}</a>", "<a>x</a>")); }
    @Test void differencesAsHtml() {
        assertTrue(new XmlComparison().compare("<a>1</a>", "<a>2</a>")
                .startsWith("<div><b>1 difference:</b><br>/a/text()"));
    }
    @Test void documentWithAPreRoot() {
        assertEquals("OK", new XmlComparison().compare("<pre>a &amp; b</pre>", "<pre>a &amp; b</pre>"));
    }
    @Test void malformedActual() {
        CompareFixtureException ex = assertThrows(CompareFixtureException.class,
                () -> new XmlComparison().compare("<a/>", "<a>"));
        assertTrue(ex.getMessage().startsWith("message:<<actual: "), ex.getMessage());
    }
    @Test void formatFixedByFixtureNotExtension(@TempDir Path dir) throws IOException {
        Path e = Files.writeString(dir.resolve("e.txt"), "<a/>");
        assertEquals("OK", new XmlComparison().compareFileTo(e.toString(), "<a/>"));
    }
}
