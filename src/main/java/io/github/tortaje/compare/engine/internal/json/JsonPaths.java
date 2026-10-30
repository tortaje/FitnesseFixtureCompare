package io.github.tortaje.compare.engine.internal.json;

import java.util.regex.Pattern;

/** Builds JSONPath-style location strings. */
public final class JsonPaths {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

    private JsonPaths() { }

    /**
     * Appends an object key to a path.
     *
     * @param parent the parent path (root is {@code $})
     * @param key    the member name
     * @return {@code parent.key}, or {@code parent['key']} when the key is not an identifier
     */
    public static String key(String parent, String key) {
        if (IDENTIFIER.matcher(key).matches()) {
            return parent + "." + key;
        }
        return parent + "['" + key.replace("\\", "\\\\").replace("'", "\\'") + "']";
    }

    /**
     * Appends an array index to a path.
     *
     * @param parent the parent path
     * @param i      the zero-based index
     * @return {@code parent[i]}
     */
    public static String index(String parent, int i) {
        return parent + "[" + i + "]";
    }
}
