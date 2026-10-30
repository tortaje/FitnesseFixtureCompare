package io.github.tortaje.compare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonComparisonFixtureTest {
    @Test void inlineMatch() { assertEquals("OK", new JsonComparison().compare("{\"id\":\"${any}\"}", "{\"id\":3}")); }
    @Test void invalidPathIsAFixtureError() {
        CompareFixtureException ex = assertThrows(CompareFixtureException.class,
                () -> new JsonComparison().compareFileTo("bad\u0000.json", "{}"));
        assertTrue(ex.getMessage().startsWith("message:<<Invalid expected file path 'bad"), ex.getMessage());
        ex = assertThrows(CompareFixtureException.class,
                () -> new JsonComparison().compareFileToFile("e.json", " bad\u0000.json "));
        assertTrue(ex.getMessage().startsWith("message:<<Invalid actual file path 'bad"), ex.getMessage());
    }
    @Test void differencesAsHtml() {
        assertTrue(new JsonComparison().compare("{\"a\":1}", "{\"a\":2}").startsWith("<div><b>1 difference:</b><br>$.a"));
    }
    @Test void filesUtf8(@TempDir Path dir) throws IOException {
        Path e = Files.writeString(dir.resolve("e.json"), "{\"n\":\"Café ☕\"}");
        Path a = Files.writeString(dir.resolve("a.json"), "{\"n\":\"Cafe\"}");
        assertTrue(new JsonComparison().compareFileToFile(e.toString(), a.toString())
                .contains("expected <code>&quot;Café ☕&quot;</code>"));
        assertEquals("OK", new JsonComparison().compareFileTo(e.toString(), "{\"n\":\"Café ☕\"}"));
    }
    @Test void errorsWrappedForSlim() {
        CompareFixtureException ex = assertThrows(CompareFixtureException.class,
                () -> new JsonComparison().compareFileTo("missing.json", "{}"));
        assertEquals("message:<<Expected file 'missing.json' not found>>", ex.getMessage());
        assertEquals("message:<<Actual file 'none.json' not found>>", assertThrows(CompareFixtureException.class,
                () -> new JsonComparison().compareFileToFile("pom.xml", "none.json")).getMessage());
    }
}
