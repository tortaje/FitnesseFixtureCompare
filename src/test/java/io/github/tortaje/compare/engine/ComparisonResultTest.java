package io.github.tortaje.compare.engine;

import static io.github.tortaje.compare.engine.DifferenceKind.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ComparisonResultTest {

    static Difference qty = new Difference("$.lines[1].qty", VALUE, "1", "3");
    static Difference status = new Difference("$.status", MISSING, null, null);

    @Test void matchingIsOk() {
        ComparisonResult r = ComparisonResult.of(List.of());
        assertTrue(r.matches()); assertEquals("OK", r.toText()); assertEquals("OK", r.toHtml());
    }
    @Test void textSummary() {
        assertEquals("2 differences: $.lines[1].qty: expected 1 but was 3; $.status: missing",
                ComparisonResult.of(List.of(qty, status)).toText());
    }
    @Test void singular() {
        assertEquals("1 difference: $.status: missing", ComparisonResult.of(List.of(status)).toText());
    }
    @Test void capAtTenThenAndMore() {
        String t = ComparisonResult.of(Collections.nCopies(13, status)).toText();
        assertTrue(t.startsWith("13 differences: "));
        assertEquals(10, t.split("missing", -1).length - 1);
        assertTrue(t.endsWith("; … and 3 more"));
    }
    @Test void longValuesTruncated() {
        String t = new Difference("$.a", VALUE, "x".repeat(100), "y").message();
        assertEquals("expected " + "x".repeat(80) + "… but was y", t);
    }
    @Test void html() {
        assertEquals("<div><b>2 differences:</b><br>$.lines[1].qty: expected <code>1</code> but was <code>3</code>"
                + "<br>$.status: missing</div>", ComparisonResult.of(List.of(qty, status)).toHtml());
    }
    @Test void htmlEscapesPathsAndValues() {
        String h = ComparisonResult.of(List.of(new Difference("/a/@b", VALUE, "<x&y>", "\"q'"))).toHtml();
        assertTrue(h.contains("<code>&lt;x&amp;y&gt;</code> but was <code>&quot;q&#39;</code>"));
    }
    @Test void htmlAndMore() {
        assertTrue(ComparisonResult.of(Collections.nCopies(12, status)).toHtml().endsWith("<br>… and 2 more</div>"));
    }
    @Test void kindsMessages() {
        assertEquals("unexpected \"x\"", new Difference("$.x", UNEXPECTED, null, "\"x\"").message());
        assertEquals("expected to match /\\d+/ but was \"a\"", new Difference("$.x", PATTERN, "\\d+", "\"a\"").message());
    }
    @Test void recordLevelMissingShowsTheRecord() {
        assertEquals("1 difference: expected row[2]: missing {id=7}", ComparisonResult.of(
                List.of(new Difference("expected row[2]", MISSING, "{id=7}", null))).toText());
        assertEquals("1 difference: $.a: missing", Comparisons.compareJson("{\"a\":1}", "{}").toText());
    }
}
