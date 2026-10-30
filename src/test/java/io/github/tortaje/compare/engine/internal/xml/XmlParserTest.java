package io.github.tortaje.compare.engine.internal.xml;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tortaje.compare.engine.ComparisonException;
import org.junit.jupiter.api.Test;

class XmlParserTest {

    @Test void usesTheJdkParserWhateverFactoryIsConfigured() throws Exception {
        String key = "javax.xml.parsers.DocumentBuilderFactory";
        String previous = System.getProperty(key);
        System.setProperty(key, RejectingDocumentBuilderFactory.class.getName());
        try {
            Object[] result = new Object[1];
            Thread t = new Thread(() -> { // a fresh thread: the parser is created per thread
                try {
                    result[0] = XmlParser.parse("<a/>", "actual");
                } catch (Throwable e) {
                    result[0] = e;
                }
            });
            t.start();
            t.join();
            assertTrue(result[0] instanceof org.w3c.dom.Document, String.valueOf(result[0]));
        } finally {
            if (previous == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, previous);
            }
        }
    }
    @Test void doctypeRejected() {
        String xxe = "<!DOCTYPE a [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><a>&x;</a>";
        assertTrue(assertThrows(ComparisonException.class, () -> XmlParser.parse(xxe, "actual"))
                .getMessage().startsWith("actual: "));
    }
    @Test void malformedHasPosition() {
        assertTrue(assertThrows(ComparisonException.class, () -> XmlParser.parse("<a>\n<b></a>", "expected"))
                .getMessage().matches("expected: .* at line 2, column \\d+"));
    }
    @Test void bomStripped() { assertDoesNotThrow(() -> XmlParser.parse("\uFEFF<a/>", "expected")); }
    @Test void depthLimit() {
        assertDoesNotThrow(() -> XmlParser.parse(nested(512), "expected"));
        assertEquals("expected: nesting deeper than 512",
                assertThrows(ComparisonException.class, () -> XmlParser.parse(nested(513), "expected")).getMessage());
        assertEquals("actual: nesting deeper than 512",
                assertThrows(ComparisonException.class, () -> XmlParser.parse(nested(5000), "actual")).getMessage());
    }
    @Test void manyAttributesAccepted() {
        StringBuilder sb = new StringBuilder("<a");
        for (int i = 0; i < 300; i++) {
            sb.append(" x").append(i).append("=\"").append(i).append('"');
        }
        String xml = sb.append("/>").toString();
        assertEquals(300, XmlParser.parse(xml, "expected").getDocumentElement().getAttributes().getLength());
    }
    @Test void empty() { assertEquals("actual: content is empty",
            assertThrows(ComparisonException.class, () -> XmlParser.parse(" ", "actual")).getMessage()); }

    static String nested(int depth) {
        return "<a>".repeat(depth) + "</a>".repeat(depth);
    }
}
