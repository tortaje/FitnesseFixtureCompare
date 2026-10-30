package io.github.tortaje.compare.engine.internal.xml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/** A third-party-like factory that rejects every JDK-specific setting, as Apache Xerces does. */
public class RejectingDocumentBuilderFactory extends DocumentBuilderFactory {

    @Override public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        throw new ParserConfigurationException("not the JDK parser");
    }
    @Override public void setAttribute(String name, Object value) {
        throw new IllegalArgumentException("Property '" + name + "' is not recognized.");
    }
    @Override public Object getAttribute(String name) {
        throw new IllegalArgumentException(name);
    }
    @Override public void setFeature(String name, boolean value) throws ParserConfigurationException {
        throw new ParserConfigurationException(name);
    }
    @Override public boolean getFeature(String name) throws ParserConfigurationException {
        throw new ParserConfigurationException(name);
    }
}
