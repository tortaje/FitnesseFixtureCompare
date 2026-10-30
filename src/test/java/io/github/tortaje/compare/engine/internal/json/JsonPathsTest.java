package io.github.tortaje.compare.engine.internal.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JsonPathsTest {
    @Test void paths() {
        assertEquals("$.lines[1].qty", JsonPaths.key(JsonPaths.index(JsonPaths.key("$", "lines"), 1), "qty"));
        assertEquals("$['first name']", JsonPaths.key("$", "first name"));
        assertEquals("$['it\\'s']", JsonPaths.key("$", "it's"));
    }
}
