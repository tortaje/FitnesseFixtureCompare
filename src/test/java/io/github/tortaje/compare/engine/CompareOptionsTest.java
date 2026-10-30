package io.github.tortaje.compare.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CompareOptionsTest {

    @Test
    void defaultsAreOrderedWithoutKeyOrSeparator() {
        CompareOptions o = CompareOptions.defaults();
        assertFalse(o.isUnordered());
        assertEquals(List.of(), o.keyNames());
        assertEquals(Optional.empty(), o.separator());
    }

    @Test
    void chainingReturnsNewInstances() {
        CompareOptions d = CompareOptions.defaults();
        CompareOptions o = d.key("id", "line").separator(';');
        assertTrue(o.isUnordered());
        assertEquals(List.of("id", "line"), o.keyNames());
        assertEquals(Optional.of(';'), o.separator());
        assertFalse(d.isUnordered());
        assertFalse(o.ordered().isUnordered());
        assertEquals(List.of(), o.ordered().keyNames());
        assertEquals(Optional.of(';'), o.ordered().separator());
    }

    @Test
    void invalidSeparatorOrKey() {
        assertEquals("Invalid CSV separator '\"'",
                assertThrows(ComparisonException.class, () -> CompareOptions.defaults().separator('"')).getMessage());
        assertThrows(ComparisonException.class, () -> CompareOptions.defaults().separator('\n'));
        assertThrows(ComparisonException.class, () -> CompareOptions.defaults().separator('\r'));
        assertEquals("key needs at least one name",
                assertThrows(ComparisonException.class, () -> CompareOptions.defaults().key(new String[0]))
                        .getMessage());
    }

    @Test
    void optionsNotApplicableToJsonOrXml() {
        assertEquals("Option 'separator' does not apply to JSON", assertThrows(ComparisonException.class,
                () -> Comparisons.compare(Format.JSON, "{}", "{}", CompareOptions.defaults().separator(';')))
                .getMessage());
        assertEquals("Option 'unordered' does not apply to XML", assertThrows(ComparisonException.class,
                () -> Comparisons.compare(Format.XML, "<a/>", "<a/>", CompareOptions.defaults().unordered()))
                .getMessage());
        assertEquals("Option 'key' does not apply to JSON", assertThrows(ComparisonException.class,
                () -> Comparisons.compare(Format.JSON, "{}", "{}", CompareOptions.defaults().key("id")))
                .getMessage());
        assertTrue(Comparisons.compare(Format.JSON, "{}", "{}", CompareOptions.defaults()).matches());
        assertTrue(Comparisons.compare(Format.JSON, "{}", "{}", null).matches());
    }

    @Test
    void compareToFileWithOptions(@TempDir Path dir) throws IOException {
        Path f = Files.writeString(dir.resolve("a.json"), "{\"a\":1}");
        assertTrue(Comparisons.compareToFile(f, "{\"a\":1}", CompareOptions.defaults()).matches());
    }
}
