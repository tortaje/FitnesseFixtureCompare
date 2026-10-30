package io.github.tortaje.compare;

import io.github.tortaje.compare.engine.Format;

/** Slim fixture comparing XML documents, with the format fixed regardless of file extensions. */
public class XmlComparison extends ComparisonFixture {

    /** Creates the XML comparison fixture. */
    public XmlComparison() {
        super(Format.XML);
    }
}
