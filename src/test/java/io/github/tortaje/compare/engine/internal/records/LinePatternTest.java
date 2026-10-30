package io.github.tortaje.compare.engine.internal.records;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tortaje.compare.engine.ComparisonException;
import org.junit.jupiter.api.Test;

class LinePatternTest {

    @Test void literalLineIncludingMetacharacters() {
        LinePattern p = LinePattern.compile("price: $5 (approx.) {x} [y]*", "line[1]");
        assertTrue(p.matches("price: $5 (approx.) {x} [y]*", "line[1]"));
        assertFalse(p.matches("price: $5 (approx) {x} [y]*", "line[1]"));
    }

    @Test void inlineMarkers() {
        assertTrue(LinePattern.compile("${any} INFO Order ${any} created", "l")
                .matches("2026-10-31 14:05 INFO Order A-7 created", "l"));
        assertTrue(LinePattern.compile("x${any}y", "l").matches("xy", "l"));
        assertFalse(LinePattern.compile("x${any-string}y", "l").matches("xy", "l"));
        assertTrue(LinePattern.compile("total=${any-number} EUR", "l").matches("total=-12.50 EUR", "l"));
        assertFalse(LinePattern.compile("total=${any-number} EUR", "l").matches("total=abc EUR", "l"));
        assertTrue(LinePattern.compile("at ${regex:[0-9]{2}:[0-9]{2}} ok", "l").matches("at 14:05 ok", "l"));
        assertTrue(LinePattern.compile("a ${ignore} b", "l").matches("a anything b", "l"));
    }

    @Test void escapeAndWholeLineIgnore() {
        assertTrue(LinePattern.compile("home=$${HOME}", "l").matches("home=${HOME}", "l"));
        assertTrue(LinePattern.compile("${ignore}", "l").isIgnore());
        assertFalse(LinePattern.compile("a ${ignore}", "l").isIgnore());
    }

    @Test void invalidCombinedRegexIsAComparisonException() {
        for (String line : new String[] {"${regex:(?<x>a)}-${regex:(?<x>b)}", "${regex:\\Qa}x"}) {
            String m = assertThrows(ComparisonException.class, () -> LinePattern.compile(line, "line[2]"))
                    .getMessage();
            assertTrue(m.startsWith("Invalid regex in '" + line + "' at line[2]: "), m);
        }
    }

    @Test void backreferencesCountGroupsAcrossTheWholeLine() {
        LinePattern p = LinePattern.compile("${regex:(z)}-${regex:(a)\\1}", "l");
        assertFalse(p.matches("z-aa", "l"));
        assertTrue(p.matches("z-az", "l"));
    }

    @Test void unbalancedBraceInMarkerFallsBackToLiteralText() {
        LinePattern p = LinePattern.compile("${regex:\\{}", "l");
        assertTrue(p.matches("${regex:\\{}", "l"));
        assertFalse(p.matches("{", "l"));
    }

    @Test void unknownMarkerAndSlowRegex() {
        assertEquals("Unknown wildcard '${nope}' at line[3]",
                assertThrows(ComparisonException.class, () -> LinePattern.compile("a ${nope} b", "line[3]"))
                        .getMessage());
        LinePattern slow = LinePattern.compile("${regex:(.*a){12}}", "line[1]");
        assertTrue(assertThrows(ComparisonException.class, () -> slow.matches("a".repeat(40) + "!", "line[1]"))
                .getMessage().contains("took longer than 1000 ms"));
    }
}
