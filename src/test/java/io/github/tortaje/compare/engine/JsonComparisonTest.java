package io.github.tortaje.compare.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JsonComparisonTest {

    private static String d(String e, String a) {
        return Comparisons.compareJson(e, a).toText();
    }

    @Test void briefExample() {
        assertEquals("2 differences: $.lines[1].qty: expected 1 but was 3; $.status: missing",
                d("{\"lines\":[{\"qty\":1},{\"qty\":1}],\"status\":\"OK\"}", "{\"lines\":[{\"qty\":1},{\"qty\":3}]}"));
    }
    @Test void keyOrderAndFormattingIgnored() { assertEquals("OK", d("{\"a\":1,\"b\":2}", "{ \"b\" : 2,\n\"a\":1 }")); }
    @Test void numbersByValue() {
        assertEquals("OK", d("[1, -0, 100, 12345678901234567890.1]", "[1.0, 0, 1E+2, 12345678901234567890.10]"));
    }
    @Test void typeMismatch() { assertEquals("1 difference: $.id: expected number but was string", d("{\"id\":1}", "{\"id\":\"1\"}")); }
    @Test void stringValueQuoted() { assertEquals("1 difference: $: expected \"A\" but was \"B\"", d("\"A\"", "\"B\"")); }
    @Test void unexpectedKeyAndItems() {
        assertEquals("2 differences: $.c[1]: unexpected {\"x\":1}; $.b: unexpected true",
                d("{\"c\":[0]}", "{\"c\":[0,{\"x\":1}],\"b\":true}"));
    }
    @Test void missingItems() { assertEquals("1 difference: $[1]: missing", d("[1,2]", "[1]")); }
    @Test void wildcards() {
        assertEquals("OK", d("{\"id\":\"${any}\",\"n\":\"${any-number}\",\"s\":\"${any-string}\",\"t\":\"${ignore}\"}",
                "{\"id\":{\"k\":[1]},\"n\":4.2,\"s\":\"x\"}"));
        assertEquals("1 difference: $.n: expected number but was string", d("{\"n\":\"${any-number}\"}", "{\"n\":\"4\"}"));
        assertEquals("1 difference: $.id: missing", d("{\"id\":\"${any}\"}", "{}"));
    }
    @Test void ignoreInArrayRequiresItem() { assertEquals("1 difference: $[1]: missing", d("[1,\"${ignore}\"]", "[1]")); }
    @Test void regex() {
        assertEquals("OK", d("[\"${regex:^O-\\\\d+$}\",\"${regex:\\\\d+\\\\.50}\",\"${regex:true}\"]", "[\"O-42\",1.50,true]"));
        assertEquals("1 difference: $: expected to match /^O-\\d+$/ but was \"X\"", d("\"${regex:^O-\\\\d+$}\"", "\"X\""));
        assertEquals("1 difference: $: expected to match /a/ but was null", d("\"${regex:a}\"", "null"));
    }
    @Test void escapedMarkerIsLiteral() {
        assertEquals("OK", d("\"$${any}\"", "\"${any}\""));
    }
    @Test void actualSideWildcardNotInterpreted() {
        assertEquals("1 difference: $: expected \"x\" but was \"${any}\"", d("\"x\"", "\"${any}\""));
    }
    @Test void unknownWildcardFails() {
        assertThrows(ComparisonException.class, () -> Comparisons.compareJson("{\"a\":\"${nope}\"}", "{\"a\":1}"));
    }
    @Test void parseErrorsNameTheSide() {
        assertTrue(assertThrows(ComparisonException.class, () -> Comparisons.compareJson("{}", "{"))
                .getMessage().startsWith("actual: "));
    }

    @Test void regexTooComplex() {
        String value = "ab".repeat(5000);
        assertEquals("Regex '${regex:(a|b)*}' at $.k is too complex for a value of 10000 characters",
                assertThrows(ComparisonException.class,
                        () -> Comparisons.compareJson("{\"k\":\"${regex:(a|b)*}\"}", "{\"k\":\"" + value + "\"}"))
                        .getMessage());
    }
}
