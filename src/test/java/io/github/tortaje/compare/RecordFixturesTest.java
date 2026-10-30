package io.github.tortaje.compare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecordFixturesTest {

    @Test
    void csvSettingsHoldUntilChanged(@TempDir Path dir) throws IOException {
        CsvComparison f = new CsvComparison();
        f.separator(";");
        f.key("id");
        assertEquals("OK", f.compare("id;v\n1;a\n2;b", "id;v\n2;b\n1;a"));
        f.ordered();
        assertTrue(f.compare("id;v\n1;a\n2;b", "id;v\n2;b\n1;a").startsWith("<div><b>"));
        Path tsv = Files.writeString(dir.resolve("e.TSV"), "id\tv\n1\ta");
        assertEquals("OK", new CsvComparison().compareFileTo(tsv.toString(), "id\tv\n1\ta"));
        f.separator("tab");
        assertEquals("OK", f.compare("id\tv\n1\ta", "id\tv\n1\ta"));
        assertEquals("message:<<Invalid CSV separator ';;'>>",
                assertThrows(CompareFixtureException.class, () -> f.separator(";;")).getMessage());
        assertEquals("message:<<Invalid CSV separator '\"'>>",
                assertThrows(CompareFixtureException.class, () -> f.separator("\"")).getMessage());
    }

    @Test
    void explicitSeparatorBeatsTsvDefault(@TempDir Path dir) throws IOException {
        Path tsv = Files.writeString(dir.resolve("e.tsv"), "id,v\n1,a");
        CsvComparison f = new CsvComparison();
        f.separator(",");
        assertEquals("OK", f.compareFileTo(tsv.toString(), "id,v\n1,a"));
    }

    @Test
    void emptyKeyFailsImmediately() {
        CsvComparison f = new CsvComparison();
        assertEquals("message:<<key needs at least one name>>",
                assertThrows(CompareFixtureException.class, () -> f.key(" , ")).getMessage());
    }

    @Test
    void jsonLinesAndTextFixtures() {
        JsonLinesComparison j = new JsonLinesComparison();
        j.unordered();
        assertEquals("OK", j.compare("{\"a\":1}\n{\"a\":2}", "{\"a\":2}\n{\"a\":1}"));
        TextComparison t = new TextComparison();
        assertEquals("OK", t.compare("<pre>${any} started</pre>", "<pre>app started</pre>"));
        t.key("k");
        assertEquals("message:<<Option 'key' does not apply to TEXT>>",
                assertThrows(CompareFixtureException.class, () -> t.compare("a", "a")).getMessage());
    }
}
