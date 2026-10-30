package io.github.tortaje.compare;

import io.github.tortaje.compare.engine.Format;

/** Normalizes FitNesse wiki cell text. */
final class CellText {

    private CellText() { }

    /**
     * Unwraps the {@code <pre>...</pre>} block FitNesse makes of a {@code {{{ }}}} cell and decodes the HTML entities
     * it applies to the content. FitNesse escapes every {@code <} of the content, so a block holding a raw {@code <}
     * is real markup; for XML, a block whose decoded content does not start with {@code <} is a document whose root
     * element is {@code <pre>} (such as {@code <pre>a &amp; b</pre>}) and is kept as is.
     *
     * @param cell   the raw cell text, may be null
     * @param format the format of the document in the cell
     * @return the decoded inner text, or the cell unchanged when it is not a FitNesse {@code <pre>} block
     */
    static String normalize(String cell, Format format) {
        if (cell == null) {
            return null;
        }
        String t = cell.trim();
        if (!t.startsWith("<pre>") || !t.endsWith("</pre>") || t.length() < "<pre></pre>".length()) {
            return cell;
        }
        String inner = t.substring(5, t.length() - 6);
        if (inner.indexOf('<') >= 0) {
            return cell;
        }
        String decoded = inner.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&nbsp;", " ").replace("&amp;", "&");
        if (format == Format.XML && !decoded.strip().startsWith("<")) {
            return cell;
        }
        return decoded;
    }
}
