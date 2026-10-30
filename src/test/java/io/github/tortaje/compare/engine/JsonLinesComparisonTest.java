package io.github.tortaje.compare.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JsonLinesComparisonTest {

    private static String d(String e, String a) {
        return Comparisons.compareJsonLines(e, a, null).toText();
    }

    private static String d(String e, String a, CompareOptions o) {
        return Comparisons.compareJsonLines(e, a, o).toText();
    }

    @Test void linesComparedInOrderWithJsonRules() {
        assertEquals("OK", d("{\"id\":1,\"t\":\"${any}\"}\n{\"id\":2}\n", "{\"t\":9,\"id\":1.0}\r\n\r\n{\"id\":2}"));
        assertEquals("1 difference: line[3]$.id: expected 2 but was 3",
                d("{\"id\":1}\n{\"id\":2}", "{\"id\":1}\n\n{\"id\":3}"));
    }

    @Test void missingAndUnexpectedLines() {
        assertEquals("1 difference: expected line[2]: missing {\"id\":2}",
                d("{\"id\":1}\n{\"id\":2}", "{\"id\":1}"));
        assertEquals("1 difference: line[2]: unexpected {\"id\":9}", d("{\"id\":1}", "{\"id\":1}\n{\"id\":9}"));
        assertEquals("1 difference: expected line[1]: missing {\"id\":1}", d("{\"id\":1}", ""));
    }

    @Test void trailingNewlineBomAndCrlfGiveSameRecords() {
        assertEquals("OK", d("{\"a\":1}\n{\"a\":2}", "\uFEFF{\"a\":1}\r\n{\"a\":2}\r\n"));
    }

    @Test void unorderedByKeyPath() {
        CompareOptions k = CompareOptions.defaults().key("order.id");
        assertEquals("OK", d("{\"order\":{\"id\":1}}\n{\"order\":{\"id\":2}}",
                "{\"order\":{\"id\":2}}\n{\"order\":{\"id\":1}}", k));
        assertEquals("key 'order.id' cannot be a wildcard in expected line 1",
                assertThrows(ComparisonException.class,
                        () -> d("{\"order\":{\"id\":\"${any}\"}}", "{}", k)).getMessage());
        assertEquals("OK", d("{\"a\":1}\n{\"a\":2}", "{\"a\":2}\n{\"a\":1}", CompareOptions.defaults().unordered()));
    }

    @Test void wildcardErrorNamesTheLine() {
        assertEquals("Unknown wildcard '${nope}' at line[2]$.a", assertThrows(ComparisonException.class,
                () -> d("{}\n{\"a\":\"${nope}\"}", "{}\n{\"a\":1}")).getMessage());
    }

    @Test void keyValueHoldingAWildcardAnywhereIsRefused() {
        CompareOptions k = CompareOptions.defaults().key("o");
        assertEquals("key 'o' cannot be a wildcard in expected line 1", assertThrows(ComparisonException.class,
                () -> d("{\"o\":{\"x\":\"${any}\"}}", "{}", k)).getMessage());
        assertEquals("key 'o' cannot be a wildcard in expected line 1", assertThrows(ComparisonException.class,
                () -> d("{\"o\":[1,\"${any}\"]}", "{}", k)).getMessage());
    }

    @Test void keyDistinguishesStringFromNumber() {
        CompareOptions k = CompareOptions.defaults().key("id");
        String r = d("{\"id\":7}", "{\"id\":\"7\"}", k);
        assertEquals("2 differences", r.substring(0, 13));
    }

    @Test void parseErrorNamesTheLine() {
        assertEquals("actual: line 3: unexpected character 'x' at column 6",
                assertThrows(ComparisonException.class, () -> d("{}", "{}\n\n{\"a\":x}")).getMessage());
    }

    @Test void separatorRejected() {
        assertEquals("Option 'separator' does not apply to JSON_LINES", assertThrows(ComparisonException.class,
                () -> d("{}", "{}", CompareOptions.defaults().separator(';'))).getMessage());
    }
}
