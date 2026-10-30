package io.github.tortaje.compare.engine.internal.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/** Item extraction (child elements and significant text runs), attributes and path building for XML elements. */
final class XmlItems {

    /** One item of an element: a child element ({@code element != null}) or a trimmed, non-empty text run. */
    record Item(Element element, String text) {

        boolean isElement() {
            return element != null;
        }

        String name() {
            return localName(element.getNodeName());
        }

        /** Display string: {@code element <name>} or the text. */
        String display() {
            return isElement() ? "element <" + name() + ">" : text;
        }
    }

    private static final String TEXT = "text()";

    private XmlItems() { }

    /** Child elements and significant text runs; comments and PIs are skipped without breaking a text run. */
    static List<Item> items(Element element) {
        List<Item> out = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling()) {
            switch (n.getNodeType()) {
                case Node.ELEMENT_NODE -> {
                    flush(run, out);
                    out.add(new Item((Element) n, null));
                }
                case Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> run.append(n.getNodeValue());
                default -> { }
            }
        }
        flush(run, out);
        return out;
    }

    private static void flush(StringBuilder run, List<Item> out) {
        String t = run.toString().trim();
        if (!t.isEmpty()) {
            out.add(new Item(null, t));
        }
        run.setLength(0);
    }

    /** True when the element has at least one child element item. */
    static boolean hasElements(List<Item> items) {
        return items.stream().anyMatch(Item::isElement);
    }

    /**
     * Attributes by local name (by qualified name when several share it), sorted, {@code xmlns} / {@code xmlns:*}
     * dropped.
     */
    static Map<String, String> attributes(Element element) {
        NamedNodeMap attrs = element.getAttributes();
        Map<String, Integer> localNames = new HashMap<>();
        for (int i = 0; i < attrs.getLength(); i++) {
            String qname = attrs.item(i).getNodeName();
            if (!isNamespaceDeclaration(qname)) {
                localNames.merge(localName(qname), 1, Integer::sum);
            }
        }
        Map<String, String> out = new TreeMap<>();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node a = attrs.item(i);
            String qname = a.getNodeName();
            if (!isNamespaceDeclaration(qname)) {
                // a local name shared by several attributes (id, x:id) keeps the prefixes, so none hides another
                String local = localName(qname);
                out.put(localNames.get(local) > 1 ? qname : local, a.getNodeValue());
            }
        }
        return out;
    }

    /** Paths of the items of a list under {@code parent}, with a 1-based index when a name repeats in the list. */
    static List<String> paths(String parent, List<Item> items) {
        Map<String, Integer> counts = new HashMap<>();
        for (Item item : items) {
            counts.merge(key(item), 1, Integer::sum);
        }
        Map<String, Integer> seen = new HashMap<>();
        List<String> out = new ArrayList<>(items.size());
        for (Item item : items) {
            String key = key(item);
            int k = seen.merge(key, 1, Integer::sum);
            out.add(parent + "/" + key + (counts.get(key) > 1 ? "[" + k + "]" : ""));
        }
        return out;
    }

    private static boolean isNamespaceDeclaration(String qname) {
        return qname.equals("xmlns") || qname.startsWith("xmlns:");
    }

    static String attributePath(String parent, String name) {
        return parent + "/@" + name;
    }

    static String localName(String qname) {
        return qname.substring(qname.lastIndexOf(':') + 1);
    }

    private static String key(Item item) {
        return item.isElement() ? item.name() : TEXT;
    }
}
