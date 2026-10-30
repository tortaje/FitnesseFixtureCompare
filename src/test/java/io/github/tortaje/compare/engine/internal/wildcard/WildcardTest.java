package io.github.tortaje.compare.engine.internal.wildcard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tortaje.compare.engine.ComparisonException;
import io.github.tortaje.compare.engine.internal.wildcard.Wildcard.Kind;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class WildcardTest {

    @Test void markers() {
        assertEquals(Kind.ANY, Wildcard.parse("${any}", "$.a").orElseThrow().kind());
        assertEquals(Kind.ANY_STRING, Wildcard.parse("${any-string}", "$.a").orElseThrow().kind());
        assertEquals(Kind.ANY_NUMBER, Wildcard.parse("${any-number}", "$.a").orElseThrow().kind());
        assertEquals(Kind.IGNORE, Wildcard.parse("${ignore}", "$.a").orElseThrow().kind());
    }

    @Test void regexKeepsBraces() {
        Wildcard w = Wildcard.parse("${regex:^\\d{3}$}", "$.a").orElseThrow();
        assertEquals(Kind.REGEX, w.kind());
        assertTrue(w.pattern().matcher("123").matches());
    }

    @Test void emptyRegexMatchesOnlyEmpty() {
        Wildcard w = Wildcard.parse("${regex:}", "$.a").orElseThrow();
        assertTrue(w.pattern().matcher("").matches());
        assertFalse(w.pattern().matcher("x").matches());
    }

    @Test void plainValuesAreNotMarkers() {
        assertTrue(Wildcard.parse("any", "$.a").isEmpty());
        assertTrue(Wildcard.parse("Order ${any} created", "$.a").isEmpty());
    }

    @Test void escapedLiteral() {
        assertTrue(Wildcard.parse("$${any}", "$.a").isEmpty());
        assertEquals("${any}", Wildcard.literal("$${any}"));
        assertEquals("x", Wildcard.literal("x"));
    }

    @Test void unknownMarkerFails() {
        ComparisonException e = assertThrows(ComparisonException.class, () -> Wildcard.parse("${anything}", "$.id"));
        assertEquals("Unknown wildcard '${anything}' at $.id", e.getMessage());
    }

    @Test void invalidRegexFails() {
        ComparisonException e = assertThrows(ComparisonException.class, () -> Wildcard.parse("${regex:(}", "$.id"));
        assertTrue(e.getMessage().startsWith("Invalid regex in '${regex:(}' at $.id: "));
    }

    @Test void ignoreCheckNeverThrows() {
        assertTrue(Wildcard.isIgnore("${ignore}"));
        assertFalse(Wildcard.isIgnore("${anything}"));
    }

    @Test void matchesHelper() {
        Wildcard w = Wildcard.parse("${regex:(a|b)*}", "$.a").orElseThrow();
        assertTrue(w.matches("abab", "$.a"));
        assertFalse(w.matches("abc", "$.a"));
        assertEquals("Regex '${regex:(a|b)*}' at $.a is too complex for a value of 10000 characters",
                assertThrows(ComparisonException.class, () -> w.matches("ab".repeat(5000), "$.a")).getMessage());
    }

    @Test void catastrophicBacktrackingStopsAfterTheTimeLimit() { // minutes of backtracking without the limit
        Wildcard w = Wildcard.parse("${regex:(.*a){12}}", "$.a").orElseThrow();
        String value = "a".repeat(40) + "!";
        ComparisonException e = assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> assertThrows(ComparisonException.class, () -> w.matches(value, "$.a")));
        assertEquals("Regex '${regex:(.*a){12}}' at $.a took longer than 1000 ms on a value of 41 characters",
                e.getMessage());
    }
}
