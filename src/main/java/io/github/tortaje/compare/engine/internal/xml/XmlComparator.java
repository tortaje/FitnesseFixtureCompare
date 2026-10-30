package io.github.tortaje.compare.engine.internal.xml;

import io.github.tortaje.compare.engine.Difference;
import io.github.tortaje.compare.engine.DifferenceKind;
import io.github.tortaje.compare.engine.internal.wildcard.Wildcard;
import io.github.tortaje.compare.engine.internal.xml.XmlItems.Item;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Structural XML comparison of an expected template against an actual document (spec §6). */
public final class XmlComparator {

    private static final String TEXT = "text";
    private static final String ELEMENT_CONTENT = "element content";

    private XmlComparator() { }

    /**
     * Compares two documents.
     *
     * @param expected the expected template (wildcards allowed in texts and attribute values)
     * @param actual   the actual document
     * @return the differences, depth-first; empty when they match
     */
    public static List<Difference> compare(Document expected, Document actual) {
        Element e = expected.getDocumentElement();
        Element a = actual.getDocumentElement();
        String en = XmlItems.localName(e.getNodeName());
        String an = XmlItems.localName(a.getNodeName());
        List<Difference> out = new ArrayList<>();
        if (!en.equals(an)) {
            out.add(new Difference("/", DifferenceKind.VALUE, "element <" + en + ">", "element <" + an + ">"));
            return out;
        }
        element("/" + en, e, a, out);
        return out;
    }

    private static void element(String path, Element expected, Element actual, List<Difference> out) {
        List<Item> e = XmlItems.items(expected);
        List<Item> a = XmlItems.items(actual);
        attributes(path, expected, actual, out);
        boolean actualHasElements = XmlItems.hasElements(a);
        if (e.size() == 1 && !e.get(0).isElement()) {
            Optional<Wildcard> wc = Wildcard.parse(e.get(0).text(), path);
            if (wc.isPresent()) {
                elementWildcard(path, wc.get(), a, actualHasElements, out);
                return;
            }
            if (actualHasElements) {
                out.add(new Difference(path, DifferenceKind.TYPE, TEXT, ELEMENT_CONTENT));
                return;
            }
        } else if (XmlItems.hasElements(e) && !a.isEmpty() && !actualHasElements) {
            out.add(new Difference(path, DifferenceKind.TYPE, ELEMENT_CONTENT, TEXT));
            return;
        }
        sequence(path, e, a, out);
    }

    private static void elementWildcard(String path, Wildcard wc, List<Item> actual, boolean actualHasElements,
                                        List<Difference> out) {
        String text = actualHasElements ? null : actual.isEmpty() ? "" : actual.get(0).text();
        switch (wc.kind()) {
            case ANY, IGNORE -> { }
            case ANY_STRING -> {
                if (actualHasElements) {
                    out.add(new Difference(path, DifferenceKind.TYPE, TEXT, ELEMENT_CONTENT));
                }
            }
            case ANY_NUMBER -> {
                if (text == null || !isNumber(text)) {
                    out.add(new Difference(path, DifferenceKind.TYPE, "number",
                            actualHasElements ? ELEMENT_CONTENT : TEXT));
                }
            }
            case REGEX -> {
                if (text == null || !wc.matches(text, path)) {
                    out.add(new Difference(path, DifferenceKind.PATTERN, wc.pattern().pattern(),
                            actualHasElements ? ELEMENT_CONTENT : text));
                }
            }
        }
    }

    private static void sequence(String path, List<Item> expected, List<Item> actual, List<Difference> out) {
        List<String> ePaths = XmlItems.paths(path, expected);
        List<String> aPaths = XmlItems.paths(path, actual);
        int i = 0;
        int j = 0;
        while (i < expected.size()) {
            Item e = expected.get(i);
            if (isIgnoreElement(e)
                    && (j >= actual.size() || !actual.get(j).isElement() || !actual.get(j).name().equals(e.name()))) {
                i++;
                continue;
            }
            if (j >= actual.size()) {
                out.add(new Difference(ePaths.get(i), DifferenceKind.MISSING, e.display(), null));
                i++;
                continue;
            }
            item(ePaths.get(i), e, actual.get(j), out);
            i++;
            j++;
        }
        for (; j < actual.size(); j++) {
            Item a = actual.get(j);
            out.add(new Difference(aPaths.get(j), DifferenceKind.UNEXPECTED, null,
                    a.isElement() ? a.display() : "\"" + a.text() + "\""));
        }
    }

    private static boolean isIgnoreElement(Item item) {
        if (!item.isElement()) {
            return false;
        }
        List<Item> items = XmlItems.items(item.element());
        return items.size() == 1 && !items.get(0).isElement() && Wildcard.isIgnore(items.get(0).text());
    }

    private static void item(String path, Item expected, Item actual, List<Difference> out) {
        if (expected.isElement() && actual.isElement()) {
            if (expected.name().equals(actual.name())) {
                element(path, expected.element(), actual.element(), out);
            } else {
                out.add(new Difference(path, DifferenceKind.VALUE, expected.display(), actual.display()));
            }
        } else if (expected.isElement()) {
            out.add(new Difference(path, DifferenceKind.TYPE, expected.display(), TEXT));
        } else if (actual.isElement()) {
            out.add(new Difference(path, DifferenceKind.TYPE, TEXT, actual.display()));
        } else {
            value(path, expected.text(), actual.text(), out);
        }
    }

    private static void attributes(String path, Element expected, Element actual, List<Difference> out) {
        Map<String, String> e = XmlItems.attributes(expected);
        Map<String, String> a = XmlItems.attributes(actual);
        for (Map.Entry<String, String> entry : e.entrySet()) {
            String child = XmlItems.attributePath(path, entry.getKey());
            String value = a.get(entry.getKey());
            if (value == null) {
                if (!Wildcard.isIgnore(entry.getValue())) {
                    out.add(new Difference(child, DifferenceKind.MISSING, entry.getValue(), null));
                }
            } else {
                value(child, entry.getValue(), value, out);
            }
        }
        for (Map.Entry<String, String> entry : a.entrySet()) {
            if (!e.containsKey(entry.getKey())) {
                out.add(new Difference(XmlItems.attributePath(path, entry.getKey()), DifferenceKind.UNEXPECTED,
                        null, entry.getValue()));
            }
        }
    }

    /** Compares a text or attribute value: wildcard rules, else literal equality. */
    private static void value(String path, String expected, String actual, List<Difference> out) {
        Optional<Wildcard> wc = Wildcard.parse(expected, path);
        if (wc.isEmpty()) {
            String literal = Wildcard.literal(expected);
            if (!literal.equals(actual)) {
                out.add(new Difference(path, DifferenceKind.VALUE, literal, actual));
            }
            return;
        }
        switch (wc.get().kind()) {
            case ANY, IGNORE, ANY_STRING -> { }
            case ANY_NUMBER -> {
                if (!isNumber(actual)) {
                    out.add(new Difference(path, DifferenceKind.TYPE, "number", TEXT));
                }
            }
            case REGEX -> {
                if (!wc.get().matches(actual, path)) {
                    out.add(new Difference(path, DifferenceKind.PATTERN, wc.get().pattern().pattern(), actual));
                }
            }
        }
    }

    private static boolean isNumber(String text) {
        try {
            new BigDecimal(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
