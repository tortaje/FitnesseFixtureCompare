package io.github.tortaje.compare.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TextComparisonTest {

    private static String d(String e, String a) {
        return Comparisons.compareText(e, a, null).toText();
    }

    private static String d(String e, String a, CompareOptions o) {
        return Comparisons.compareText(e, a, o).toText();
    }

    @Test void crlfTrailingSpacesAndFinalBlankLinesIgnored() {
        assertEquals("OK", d("a\nb\n", "a  \r\nb\r\n\r\n"));
        assertEquals("OK", d("\uFEFFa", "a"));
        assertEquals("1 difference: line[2]: expected \"\" but was \"b\"", d("a\n\nb", "a\nb\nb"));
    }

    @Test void differencesShowThePatternAsWritten() {
        assertEquals("1 difference: line[1]: expected \"${any} INFO Order ${any} created\" but was \"x WARN y\"",
                d("${any} INFO Order ${any} created", "x WARN y"));
        assertEquals("1 difference: expected line[1]: missing \"a\"", d("a", ""));
        assertEquals("OK", d("", ""));
    }

    @Test void optionalIgnoreLineAndUnordered() {
        assertEquals("OK", d("start\n${ignore}\nend", "start\nend"));
        assertEquals("OK", d("start\n${ignore}\nend", "start\nnoise\nend"));
        assertEquals("OK", d("b\na", "a\nb", CompareOptions.defaults().unordered()));
        assertEquals("Option 'key' does not apply to TEXT", assertThrows(ComparisonException.class,
                () -> d("a", "a", CompareOptions.defaults().key("k"))).getMessage());
    }
}
