package io.github.tortaje.compare.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tortaje.compare.engine.internal.TextFiles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ComparisonsTest {

    @Test void compareToFileWithARootPath() {
        ComparisonException e = assertThrows(ComparisonException.class,
                () -> Comparisons.compareToFile(Path.of("/"), "{}"));
        assertEquals("Cannot tell the format of '/': use a .json, .xml, .jsonl, .ndjson, .txt, .log, .csv or .tsv file",
                e.getMessage());
    }
    @Test void compareToFileByExtension(@TempDir Path dir) throws IOException {
        Path f = Files.writeString(dir.resolve("order.json"), "{\"id\":\"${any}\",\"name\":\"Café ☕\"}");
        assertTrue(Comparisons.compareToFile(f, "{\"id\":7,\"name\":\"Café ☕\"}").matches());
        Path x = Files.writeString(dir.resolve("order.xml"), "<a>1</a>");
        assertEquals("OK", Comparisons.compareToFile(x, "<a>1</a>").toText());
    }

    @Test void missingFile(@TempDir Path dir) {
        Path f = dir.resolve("none.json");
        assertEquals("Expected file '" + f + "' not found",
                assertThrows(ComparisonException.class, () -> Comparisons.compareToFile(f, "{}")).getMessage());
    }

    @Test void directoryIsNotFound(@TempDir Path dir) {
        assertEquals("Actual file '" + dir + "' not found",
                assertThrows(ComparisonException.class, () -> TextFiles.read(dir, "actual")).getMessage());
    }

    @Test void unknownExtension(@TempDir Path dir) {
        assertEquals("Cannot tell the format of 'a.docx': use a .json, .xml, .jsonl, .ndjson, .txt, .log, .csv or "
                        + ".tsv file",
                assertThrows(ComparisonException.class,
                        () -> Comparisons.compareToFile(dir.resolve("a.docx"), "{}")).getMessage());
    }

    @Test void compareDispatchesOnFormat() {
        assertTrue(Comparisons.compare(Format.XML, "<a/>", "<a/>", null).matches());
        assertFalse(Comparisons.compare(Format.JSON, "1", "2", CompareOptions.defaults()).matches());
    }

    @Test void tsvFileUsesTabByDefault(@TempDir Path dir) throws IOException {
        Path f = Files.writeString(dir.resolve("t.tsv"), "id\tv\n1\ta");
        assertEquals("OK", Comparisons.compareToFile(f, "id\tv\n1\ta").toText());
        assertTrue(Comparisons.compareToFile(f, "id,v\n1,a", CompareOptions.defaults().separator(',')).toText()
                .startsWith("1 difference: header:"));
    }
}
