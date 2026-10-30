package io.github.tortaje.compare.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CsvComparisonTest {

    private static String d(String e, String a) {
        return Comparisons.compareCsv(e, a, null).toText();
    }

    private static String d(String e, String a, CompareOptions o) {
        return Comparisons.compareCsv(e, a, o).toText();
    }

    @Test void columnsByNameNumbersByValueWildcards() {
        assertEquals("OK", d("id,amount,at\n1,10,${any}\n", "at,id,amount\r\nx,1,10.0\r\n"));
        assertEquals("1 difference: row[1].amount: expected 10 but was 11", d("id,amount\n1,10", "id,amount\n1,11"));
        assertEquals("1 difference: row[1]['unit price']: expected 1,5 but was 1,50",
                d("id;unit price\n1;1,5", "id;unit price\n1;1,50", CompareOptions.defaults().separator(';')));
    }

    @Test void numberAndRegexWildcardKinds() {
        assertEquals("1 difference: row[1].n: expected number but was abc", d("n\n${any-number}", "n\nabc"));
        assertEquals("1 difference: row[1].n: expected to match /\\d+/ but was x1", d("n\n${regex:\\d+}", "n\nx1"));
        assertEquals("OK", d("n\n$${any}", "n\n${any}"));
    }

    @Test void headerMismatchIsOneDifference() {
        assertEquals("1 difference: header: expected [id, amount] but was [id, total]",
                d("id,amount\n1,2", "id,total\n1,2"));
    }

    @Test void missingRowShowsTheRecord() {
        assertEquals("1 difference: expected row[2]: missing {id=7, amount=3}",
                d("id,amount\n1,2\n7,3", "id,amount\n1,2"));
    }

    @Test void keyedUnorderedAndKeyErrors() {
        CompareOptions k = CompareOptions.defaults().key("id");
        assertEquals("OK", d("id,v\n1,a\n2,b", "id,v\n2,b\n1,a", k));
        assertEquals("key column 'idd' not in expected header [id, v]", assertThrows(ComparisonException.class,
                () -> d("id,v\n1,a", "id,v\n1,a", CompareOptions.defaults().key("idd"))).getMessage());
        assertEquals("key 'id' cannot be a wildcard in expected row 1", assertThrows(ComparisonException.class,
                () -> d("id,v\n${any},a", "id,v\n1,a", k)).getMessage());
    }

    @Test void anyNumberUsesTheDecimalShape() {
        assertEquals("1 difference: row[1].n: expected number but was +1.", d("n\n${any-number}", "n\n+1."));
        assertEquals("1 difference: row[1].n: expected number but was .5", d("n\n${any-number}", "n\n.5"));
        assertEquals("OK", d("n\n${any-number}", "n\n-12.5e3"));
        assertEquals("OK", d("n\n+1.", "n\n1"));
    }

    @Test void wildcardKeyErrorNamesTheCell() {
        assertEquals("Unknown wildcard '${nope}' at row[1].a", assertThrows(ComparisonException.class,
                () -> d("a\n${nope}", "a\nx")).getMessage());
        assertEquals("Unknown wildcard '${nope}' at row[1].id", assertThrows(ComparisonException.class,
                () -> d("id,v\n${nope},a", "id,v\n1,a", CompareOptions.defaults().key("id"))).getMessage());
    }

    @Test void tenThousandReversedRowsWithoutKeyStayFast() {
        StringBuilder e = new StringBuilder("a,b,c\n");
        StringBuilder a = new StringBuilder("a,b,c\n");
        int n = 10_000;
        for (int i = 0; i < n; i++) {
            e.append("k").append(i).append(",v").append(i).append(",w").append(i).append('\n');
            int r = n - 1 - i;
            a.append("k").append(r).append(",v").append(r).append(",w").append(r).append('\n');
        }
        assertEquals("OK", org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(
                java.time.Duration.ofSeconds(10),
                () -> d(e.toString(), a.toString(), CompareOptions.defaults().unordered())));
    }

    @Test void unorderedWithoutKey() {
        assertEquals("OK", d("v\na\nb", "v\nb\na", CompareOptions.defaults().unordered()));
    }
}
