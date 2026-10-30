package io.github.tortaje.compare.engine.internal.xml;

import io.github.tortaje.compare.engine.ComparisonException;
import java.io.StringReader;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * Hardened, namespace-unaware DOM parser (DTDs, external entities and XInclude disabled; CDATA coalesced).
 *
 * <p>Limits are the same on every JDK: element nesting up to {@value #MAX_DEPTH} levels (checked by this class,
 * the JDK's own depth limit is disabled) and up to {@value #MAX_ATTRIBUTES} attributes per element.
 */
public final class XmlParser {

    /** Maximum element nesting depth (the root element is depth 1), as for JSON. */
    public static final int MAX_DEPTH = 512;

    /** Maximum number of attributes on one element (overrides the JDK default, 200 on JDK 24+). */
    public static final int MAX_ATTRIBUTES = 10_000;

    private static final String JAXP = "http://www.oracle.com/xml/jaxp/properties/";

    /** Builders are not thread-safe and factories are costly to look up: one builder per thread. */
    private static final ThreadLocal<DocumentBuilder> BUILDER = ThreadLocal.withInitial(XmlParser::newBuilder);

    private XmlParser() { }

    /**
     * Parses an XML document.
     *
     * @param text the XML text (a leading BOM is ignored)
     * @param side {@code "expected"} or {@code "actual"}, used as error prefix
     * @return the parsed document
     * @throws ComparisonException if the content is empty, not well-formed XML, declares a DOCTYPE or nests
     *                             elements deeper than {@value #MAX_DEPTH}
     */
    public static Document parse(String text, String side) {
        String t = text != null && !text.isEmpty() && text.charAt(0) == '\uFEFF' ? text.substring(1) : text;
        if (t == null || t.isBlank()) {
            throw new ComparisonException(side + ": content is empty");
        }
        DocumentBuilder builder = BUILDER.get();
        builder.reset();
        builder.setErrorHandler(null); // default handler prints to stderr; parse errors still throw
        Document document;
        try {
            document = builder.parse(new InputSource(new StringReader(t)));
        } catch (SAXParseException e) {
            throw new ComparisonException(side + ": " + e.getMessage() + " at line " + e.getLineNumber()
                    + ", column " + e.getColumnNumber(), e);
        } catch (Exception e) {
            throw new ComparisonException(side + ": not valid XML: " + e.getMessage(), e);
        }
        checkDepth(document, side);
        return document;
    }

    /** Iterative (no recursion) walk over the elements, so that any depth is measured without stack risk. */
    private static void checkDepth(Document document, String side) {
        Node root = document.getDocumentElement();
        Node node = root;
        int depth = 1;
        while (node != null) {
            if (depth > MAX_DEPTH) {
                throw new ComparisonException(side + ": nesting deeper than " + MAX_DEPTH);
            }
            Node child = firstElement(node.getFirstChild());
            if (child != null) {
                node = child;
                depth++;
                continue;
            }
            while (node != root && nextElement(node) == null) {
                node = node.getParentNode();
                depth--;
            }
            node = node == root ? null : nextElement(node);
        }
    }

    private static Node nextElement(Node node) {
        return firstElement(node.getNextSibling());
    }

    private static Node firstElement(Node node) {
        Node n = node;
        while (n != null && n.getNodeType() != Node.ELEMENT_NODE) {
            n = n.getNextSibling();
        }
        return n;
    }

    private static DocumentBuilder newBuilder() {
        try {
            // the JDK parser, never a factory found on the class path (such as Apache Xerces, which rejects the
            // JDK-specific limits below): the hardening and the limits are only verified on this one
            DocumentBuilderFactory f = DocumentBuilderFactory.newDefaultInstance();
            f.setNamespaceAware(false);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            f.setCoalescing(true);
            f.setAttribute(JAXP + "maxElementDepth", "0"); // 0 = no JDK limit: MAX_DEPTH is checked after parse
            f.setAttribute(JAXP + "elementAttributeLimit", Integer.toString(MAX_ATTRIBUTES));
            return f.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("XML parser configuration failed", e);
        }
    }
}
