package io.github.tortaje.compare.engine;

import java.util.List;
import java.util.function.UnaryOperator;

/** The outcome of a comparison: a list of differences, renderable as text or HTML. */
public final class ComparisonResult {

    private static final int MAX_SHOWN = 10;

    private final List<Difference> differences;

    private ComparisonResult(List<Difference> differences) {
        this.differences = List.copyOf(differences);
    }

    /**
     * Creates a result.
     *
     * @param differences the differences found, empty when the documents match
     * @return the result
     */
    public static ComparisonResult of(List<Difference> differences) {
        return new ComparisonResult(differences);
    }

    /**
     * Tells whether the documents match.
     *
     * @return {@code true} when there is no difference
     */
    public boolean matches() {
        return differences.isEmpty();
    }

    /**
     * Returns the differences.
     *
     * @return an unmodifiable list
     */
    public List<Difference> differences() {
        return differences;
    }

    /**
     * Returns the number of differences.
     *
     * @return the total count
     */
    public int total() {
        return differences.size();
    }

    /**
     * Renders the result as plain text; at most 10 differences are shown.
     *
     * @return {@code OK} or a one-line summary
     */
    public String toText() {
        return render(UnaryOperator.identity(), UnaryOperator.identity(), "", "; ", "; … and ", "");
    }

    /**
     * Renders the result as HTML with escaped paths and values in {@code <code>}; at most 10 differences are shown.
     *
     * @return {@code OK} or an HTML fragment
     */
    public String toHtml() {
        return render(ComparisonResult::escape, v -> "<code>" + escape(v) + "</code>",
                "<div><b>", "<br>", "<br>… and ", "</div>");
    }

    private String render(UnaryOperator<String> path, UnaryOperator<String> value,
                          String open, String sep, String moreSep, String close) {
        if (matches()) {
            return "OK";
        }
        int n = total();
        StringBuilder sb = new StringBuilder();
        boolean html = !open.isEmpty();
        sb.append(open).append(n).append(n == 1 ? " difference:" : " differences:");
        if (html) {
            sb.append("</b>");
        } else {
            sb.append(' ');
        }
        for (int i = 0; i < Math.min(n, MAX_SHOWN); i++) {
            Difference d = differences.get(i);
            if (i > 0 || html) {
                sb.append(sep);
            }
            sb.append(path.apply(d.path())).append(": ").append(d.render(value));
        }
        if (n > MAX_SHOWN) {
            sb.append(moreSep).append(n - MAX_SHOWN).append(" more");
        }
        return sb.append(close).toString();
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
