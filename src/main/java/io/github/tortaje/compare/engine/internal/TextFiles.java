package io.github.tortaje.compare.engine.internal;

import io.github.tortaje.compare.engine.ComparisonException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/** Reads UTF-8 text files for the comparison facade. */
public final class TextFiles {

    private TextFiles() { }

    /**
     * Reads a whole file as UTF-8.
     *
     * @param file the file to read
     * @param role {@code "expected"} or {@code "actual"}, used in error messages
     * @return the file content
     * @throws ComparisonException if the file is missing, not a regular file or unreadable
     */
    public static String read(Path file, String role) {
        if (!Files.isRegularFile(file)) {
            throw new ComparisonException(capitalize(role) + " file '" + file + "' not found");
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ComparisonException("Cannot read " + role + " file '" + file + "': " + e.getMessage(), e);
        }
    }

    /**
     * Turns a path written in a wiki cell into a {@link Path}.
     *
     * @param path the path text, surrounding blanks ignored
     * @param role {@code "expected"} or {@code "actual"}, used in error messages
     * @return the path
     * @throws ComparisonException when the text is not a valid path on this platform
     */
    public static Path path(String path, String role) {
        try {
            return Path.of(path.trim());
        } catch (InvalidPathException e) {
            throw new ComparisonException("Invalid " + role + " file path '" + path.trim() + "': " + e.getReason(), e);
        }
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
