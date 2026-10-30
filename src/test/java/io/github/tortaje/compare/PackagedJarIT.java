package io.github.tortaje.compare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class PackagedJarIT {

    private static Path packagedJar() throws IOException {
        try (Stream<Path> files = Files.list(Paths.get("target"))) {
            List<Path> jars = files
                    .filter(p -> p.getFileName().toString().matches("fitnesse-compare-.*\\.jar"))
                    .filter(p -> !p.getFileName().toString().endsWith("-sources.jar"))
                    .filter(p -> !p.getFileName().toString().endsWith("-javadoc.jar"))
                    .collect(Collectors.toList());
            assertEquals(1, jars.size(), "expected exactly one packaged jar but found " + jars);
            return jars.get(0);
        }
    }

    @Test
    void onlyOwnClasses() throws IOException {
        List<String> names = new ArrayList<>();
        try (JarFile jar = new JarFile(packagedJar().toFile())) {
            jar.stream().forEach(e -> names.add(e.getName()));
        }
        for (String name : names) {
            assertTrue(name.endsWith("/") || name.startsWith("io/github/tortaje/compare/")
                    || name.startsWith("META-INF/"), "unexpected jar entry: " + name);
        }
        assertTrue(names.contains("io/github/tortaje/compare/JsonComparison.class"), "JsonComparison.class missing");
        assertTrue(names.contains("io/github/tortaje/compare/engine/Comparisons.class"), "Comparisons.class missing");
    }

    @Test
    void automaticModuleName() throws IOException {
        try (JarFile jar = new JarFile(packagedJar().toFile())) {
            assertEquals("io.github.tortaje.compare",
                    jar.getManifest().getMainAttributes().getValue("Automatic-Module-Name"));
        }
    }

    @Test
    void noRuntimeDependencies() throws Exception {
        Element project = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(Paths.get("pom.xml").toFile()).getDocumentElement();
        int checked = 0;
        NodeList children = project.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!(child instanceof Element) || !"dependencies".equals(child.getNodeName())) {
                continue;
            }
            NodeList dependencies = child.getChildNodes();
            for (int j = 0; j < dependencies.getLength(); j++) {
                if (!(dependencies.item(j) instanceof Element dependency)) {
                    continue;
                }
                NodeList scopes = dependency.getElementsByTagName("scope");
                String scope = scopes.getLength() == 0 ? "compile" : scopes.item(0).getTextContent().trim();
                String artifact = dependency.getElementsByTagName("artifactId").item(0).getTextContent();
                assertTrue(scope.equals("provided") || scope.equals("test"),
                        artifact + " has scope " + scope + ": no runtime dependency allowed");
                checked++;
            }
        }
        assertTrue(checked > 0, "no dependency found in pom.xml");
    }

    @Test
    void jarWorksOnItsOwn() throws Exception {
        URL url = packagedJar().toUri().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[] {url}, ClassLoader.getPlatformClassLoader())) {
            Class<?> comparisons = loader.loadClass("io.github.tortaje.compare.engine.Comparisons");
            Object result = comparisons.getMethod("compareJson", String.class, String.class)
                    .invoke(null, "{\"a\":\"${any}\"}", "{\"a\":1}");
            Object text = result.getClass().getMethod("toText").invoke(result);
            assertEquals("OK", text);
        }
    }
}
