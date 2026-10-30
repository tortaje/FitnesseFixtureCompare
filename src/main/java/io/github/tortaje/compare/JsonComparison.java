package io.github.tortaje.compare;

import io.github.tortaje.compare.engine.Format;

/** Slim fixture comparing JSON documents, with the format fixed regardless of file extensions. */
public class JsonComparison extends ComparisonFixture {

    /** Creates the JSON comparison fixture. */
    public JsonComparison() {
        super(Format.JSON);
    }
}
