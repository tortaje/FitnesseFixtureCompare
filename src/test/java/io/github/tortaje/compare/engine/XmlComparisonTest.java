package io.github.tortaje.compare.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class XmlComparisonTest {

    private static String d(String e, String a) {
        return Comparisons.compareXml(e, a).toText();
    }

    @Test void formattingCommentsPiCdataNamespaces() {
        assertEquals("OK", d("<o:order xmlns:o=\"urn:a\"><id>1</id><n>a b</n></o:order>",
                "<?xml version=\"1.0\"?><p:order xmlns:p=\"urn:b\">\n  <!-- c --><id> 1 </id>\n<?pi x?><n><![CDATA[a b]]></n></p:order>"));
    }
    @Test void crlfEqualsLf() { assertEquals("OK", d("<a>x\r\ny</a>", "<a>x\ny</a>")); }
    @Test void attributesAsSet() {
        assertEquals("OK", d("<a x=\"1\" y=\"2\"/>", "<a y=\"2\" x=\"1\"/>"));
        assertEquals("2 differences: /a/@x: missing; /a/@z: unexpected 3", d("<a x=\"1\"/>", "<a z=\"3\"/>"));
    }
    @Test void attributesSharingALocalNameKeepTheirPrefix() {
        assertEquals("1 difference: /a/@x:id: unexpected 9", d("<a id=\"1\"/>", "<a id=\"1\" x:id=\"9\"/>"));
        assertEquals("2 differences: /a/@id: expected 1 but was 2; /a/@x:id: unexpected 1",
                d("<a id=\"1\"/>", "<a x:id=\"1\" id=\"2\"/>"));
        assertEquals("OK", d("<a x:id=\"1\" id=\"2\"/>", "<a id=\"2\" x:id=\"1\"/>"));
        assertEquals("OK", d("<a id=\"1\"/>", "<p:a xmlns:p=\"urn:p\" p:id=\"1\"/>"));
    }
    @Test void childrenInOrderWithIndexes() {
        assertEquals("1 difference: /order/lines/line[2]/@qty: expected 1 but was 3",
                d("<order><lines><line qty=\"1\"/><line qty=\"1\"/></lines></order>",
                  "<order><lines><line qty=\"1\"/><line qty=\"3\"/></lines></order>"));
    }
    @Test void textDifferenceAndPath() {
        assertEquals("1 difference: /order/status/text(): expected OK but was KO",
                d("<order><status>OK</status></order>", "<order><status>KO</status></order>"));
    }
    @Test void nameMismatchNoResync() {
        assertEquals("2 differences: /a/line: expected element <line> but was element <item>; /a/x: unexpected element <x>",
                d("<a><line/></a>", "<a><item/><x/></a>"));
    }
    @Test void missingAndUnexpectedChildren() {
        assertEquals("1 difference: /a/c: missing", d("<a><b/><c/></a>", "<a><b/></a>"));
    }
    @Test void rootMismatchStops() {
        assertEquals("1 difference: /: expected element <a> but was element <b>", d("<a><x/></a>", "<b><y/></b>"));
    }
    @Test void ignoreElementOptional() {
        String e = "<a><token>${ignore}</token><id>1</id></a>";
        assertEquals("OK", d(e, "<a><id>1</id></a>"));
        assertEquals("OK", d(e, "<a><token><deep/></token><id>1</id></a>"));
    }
    @Test void anyElementAcceptsChildrenButMustExist() {
        assertEquals("OK", d("<a><p>${any}</p></a>", "<a><p><q/></p></a>"));
        assertEquals("1 difference: /a/p: missing", d("<a><p>${any}</p></a>", "<a/>"));
    }
    @Test void textWildcards() {
        assertEquals("OK", d("<a n=\"${regex:\\d+}\"><id>${any-number}</id><s>${any-string}</s></a>",
                "<a n=\"42\"><id>4.2</id><s>x</s></a>"));
        assertEquals("1 difference: /a/id: expected number but was text", d("<a><id>${any-number}</id></a>", "<a><id>x</id></a>"));
        assertEquals("OK", d("<a k=\"${ignore}\"/>", "<a/>"));
    }
    @Test void textVersusElementContent() {
        assertEquals("1 difference: /a: expected text but was element content", d("<a>x</a>", "<a><b/></a>"));
    }
    @Test void mixedContent() {
        assertEquals("1 difference: /p/text()[2]: expected world but was there",
                d("<p>hello <b>big</b> world</p>", "<p>hello <b>big</b> there</p>"));
    }
    @Test void unexpectedTextQuotedAndRegexOnElementContent() {
        assertEquals("1 difference: /a/text(): unexpected \"tail\"", d("<a><b/></a>", "<a><b/> tail </a>"));
        assertEquals("1 difference: /a: expected to match /\\d+/ but was element content",
                d("<a>${regex:\\d+}</a>", "<a><b/></a>"));
        assertEquals("1 difference: /a/text(): expected ${x} but was y", d("<a>$${x}</a>", "<a>y</a>"));
    }

    @Test void depthLimit() {
        String ok = "<a>".repeat(512) + "</a>".repeat(512);
        assertEquals("OK", d(ok, ok));
        String deep = "<a>".repeat(513) + "</a>".repeat(513);
        assertTrue(assertThrows(ComparisonException.class, () -> Comparisons.compareXml(deep, ok))
                .getMessage().startsWith("expected: nesting deeper than 512"));
        String veryDeep = "<a>".repeat(5000) + "</a>".repeat(5000);
        assertTrue(assertThrows(ComparisonException.class, () -> Comparisons.compareXml(ok, veryDeep))
                .getMessage().startsWith("actual: nesting deeper than 512"));
    }
    @Test void manyAttributes() {
        StringBuilder sb = new StringBuilder("<a");
        for (int i = 0; i < 300; i++) {
            sb.append(" x").append(i).append("=\"").append(i).append('"');
        }
        String xml = sb.append("/>").toString();
        assertEquals("OK", d(xml, xml));
    }
    @Test void regexTooComplex() {
        String value = "ab".repeat(5000);
        assertEquals("Regex '${regex:(a|b)*}' at /a is too complex for a value of 10000 characters",
                assertThrows(ComparisonException.class,
                        () -> Comparisons.compareXml("<a>${regex:(a|b)*}</a>", "<a>" + value + "</a>")).getMessage());
        assertEquals("Regex '${regex:(a|b)*}' at /a/@n is too complex for a value of 10000 characters",
                assertThrows(ComparisonException.class,
                        () -> Comparisons.compareXml("<a n=\"${regex:(a|b)*}\"/>", "<a n=\"" + value + "\"/>"))
                        .getMessage());
    }
}
