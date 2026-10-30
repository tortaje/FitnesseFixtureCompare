package io.github.tortaje.compare.engine.internal.records;

import static io.github.tortaje.compare.engine.DifferenceKind.VALUE;
import static org.junit.jupiter.api.Assertions.*;

import io.github.tortaje.compare.engine.CompareOptions;
import io.github.tortaje.compare.engine.ComparisonException;
import io.github.tortaje.compare.engine.Difference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecordSequenceTest {

    static final RecordFormat<String> LINES = new RecordFormat<>() {
        @Override public String label() { return "line"; }
        @Override public void compare(String path, String expected, String actual, List<Difference> out) {
            if (!expected.equals(actual)) { out.add(new Difference(path, VALUE, expected, actual)); }
        }
        @Override public String key(String record, List<String> key, boolean expectedSide, int position) {
            int i = record.indexOf(':');
            return i < 0 ? null : record.substring(0, i);
        }
        @Override public String display(String record) { return record; }
        @Override public boolean isIgnore(String expected) { return expected.equals("*"); }
    };

    static List<Rec<String>> recs(String... values) {
        List<Rec<String>> r = new ArrayList<>();
        for (int i = 0; i < values.length; i++) { r.add(new Rec<>(i + 1, values[i])); }
        return r;
    }

    static List<String> texts(List<Difference> diffs) {
        return diffs.stream().map(d -> d.path() + ": " + d.message()).toList();
    }

    @Test void orderedPairsByPositionThenMissingAndUnexpected() {
        assertEquals(List.of("line[2]: expected b but was x", "expected line[3]: missing c"),
                texts(RecordSequence.compare(LINES, recs("a", "b", "c"), recs("a", "x"), null)));
        assertEquals(List.of("line[2]: unexpected z"),
                texts(RecordSequence.compare(LINES, recs("a"), recs("a", "z"), null)));
    }
    @Test void orderedIgnoreIsOptional() {
        assertEquals(List.of(), texts(RecordSequence.compare(LINES, recs("a", "*", "c"), recs("a", "c"), null)));
        assertEquals(List.of(), texts(RecordSequence.compare(LINES, recs("a", "*", "c"), recs("a", "q", "c"), null)));
        assertEquals(List.of(), texts(RecordSequence.compare(LINES, recs("a", "*"), recs("a"), null)));
    }
    @Test void unorderedByKeyInOrderOfAppearance() {
        CompareOptions k = CompareOptions.defaults().key("k");
        assertEquals(List.of("line[3]: expected 1:b but was 1:x", "expected line[3]: missing 2:c",
                "line[1]: unexpected 3:z"),
                texts(RecordSequence.compare(LINES, recs("1:a", "1:b", "2:c"), recs("3:z", "1:a", "1:x"), k)));
    }
    @Test void nullKeyNeverMatches() {
        CompareOptions k = CompareOptions.defaults().key("k");
        assertEquals(List.of("expected line[1]: missing nokey", "line[1]: unexpected nokey"),
                texts(RecordSequence.compare(LINES, recs("nokey"), recs("nokey"), k)));
    }
    @Test void unorderedWithoutKeyByEquality() {
        CompareOptions u = CompareOptions.defaults().unordered();
        assertEquals(List.of(), texts(RecordSequence.compare(LINES, recs("a", "b"), recs("b", "a"), u)));
        assertEquals(List.of("expected line[2]: missing b", "line[2]: unexpected x"),
                texts(RecordSequence.compare(LINES, recs("a", "b"), recs("a", "x"), u)));
        assertEquals(List.of(), texts(RecordSequence.compare(LINES, recs("a", "*"), recs("a"), u)));
    }
    @Test void unorderedWithoutKeyIsBounded() {
        List<Rec<String>> many = new ArrayList<>();
        for (int i = 1; i <= RecordSequence.MAX_UNKEYED + 1; i++) { many.add(new Rec<>(i, "v" + i)); }
        assertEquals("unordered comparison without key is limited to 10000 records: use key",
                assertThrows(ComparisonException.class, () -> RecordSequence.compare(LINES, many, recs("a"),
                        CompareOptions.defaults().unordered())).getMessage());
    }
    @Test void keyedMatchingIsLinear() {
        List<Rec<String>> e = new ArrayList<>(), a = new ArrayList<>();
        for (int i = 1; i <= 100_000; i++) { e.add(new Rec<>(i, i + ":v")); a.add(new Rec<>(i, (100_001 - i) + ":v")); }
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> assertEquals(List.of(),
                texts(RecordSequence.compare(LINES, e, a, CompareOptions.defaults().key("k")))));
    }
}
