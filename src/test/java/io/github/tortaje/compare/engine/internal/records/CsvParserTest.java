package io.github.tortaje.compare.engine.internal.records;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.tortaje.compare.engine.ComparisonException;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvParserTest {

    @Test void rfc4180QuotesSeparatorAndLineBreak() {
        CsvParser.Csv c = CsvParser.parse("id;note;n\r\n1;\"a;b\nc\";\"say \"\"hi\"\"\"\n\n2;x;3\n", ';', "actual");
        assertEquals(List.of("id", "note", "n"), c.header());
        assertEquals(List.of("1", "a;b\nc", "say \"hi\""), c.rows().get(0).fields());
        assertEquals(2, c.rows().size());
        assertEquals(5, c.rows().get(1).line());
    }

    @Test void bomAndNoFinalNewline() {
        CsvParser.Csv c = CsvParser.parse("\uFEFFa\n1", ',', "e");
        assertEquals(List.of("a"), c.header());
        assertEquals(List.of("1"), c.rows().get(0).fields());
    }

    @Test void errors() {
        assertEquals("actual: line 3: 3 fields, header has 2",
                assertThrows(ComparisonException.class, () -> CsvParser.parse("a,b\n1,2\n1,2,3", ',', "actual"))
                        .getMessage());
        assertEquals("expected: line 2: unterminated quoted field",
                assertThrows(ComparisonException.class, () -> CsvParser.parse("a\n\"x", ',', "expected"))
                        .getMessage());
        assertEquals("actual: duplicate column 'id'",
                assertThrows(ComparisonException.class, () -> CsvParser.parse("id,id\n1,2", ',', "actual"))
                        .getMessage());
        assertEquals("expected: content is empty",
                assertThrows(ComparisonException.class, () -> CsvParser.parse(" \n", ',', "expected"))
                        .getMessage());
    }

    @Test void errorLineAfterMultiLineRecordIsPhysical() {
        assertEquals("actual: line 4: 3 fields, header has 2",
                assertThrows(ComparisonException.class,
                        () -> CsvParser.parse("a,b\n\"x\ny\",1\n1,2,3", ',', "actual")).getMessage());
    }
}
