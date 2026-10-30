package io.github.tortaje.compare.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FormatTest {

    @Test
    void jsonAndXmlFromExtensionIgnoringCase() {
        assertEquals(Format.JSON, Format.fromFileName("expected/order.json"));
        assertEquals(Format.XML, Format.fromFileName("ORDER.XML"));
    }

    @Test
    void unknownExtensionIsRejected() {
        ComparisonException e = assertThrows(ComparisonException.class, () -> Format.fromFileName("x.docx"));
        assertEquals("Cannot tell the format of 'x.docx': use a .json, .xml, .jsonl, .ndjson, .txt, .log, .csv or "
                + ".tsv file", e.getMessage());
    }

    @Test
    void jsonLinesExtensions() {
        assertEquals(Format.JSON_LINES, Format.fromFileName("a.jsonl"));
        assertEquals(Format.JSON_LINES, Format.fromFileName("a.NDJSON"));
    }

    @Test
    void textExtensions() {
        assertEquals(Format.TEXT, Format.fromFileName("a.txt"));
        assertEquals(Format.TEXT, Format.fromFileName("a.LOG"));
    }

    @Test
    void csvExtensions() {
        assertEquals(Format.CSV, Format.fromFileName("a.csv"));
        assertEquals(Format.CSV, Format.fromFileName("a.TSV"));
    }
}
