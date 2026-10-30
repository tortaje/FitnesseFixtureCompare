package io.github.tortaje.compare.engine;

/** The kind of a single difference found between an expected and an actual document. */
public enum DifferenceKind {
    /** The values differ. */
    VALUE,
    /** The node types differ. */
    TYPE,
    /** An expected node is absent from the actual document. */
    MISSING,
    /** The actual document holds a node that was not expected. */
    UNEXPECTED,
    /** The actual value does not match the expected pattern. */
    PATTERN
}
