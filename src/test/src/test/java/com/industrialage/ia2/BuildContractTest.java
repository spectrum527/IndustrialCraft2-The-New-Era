package com.industrialage.ia2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

class BuildContractTest {
    @Test
    void jarsContainEntrypointMetadataAndCorrectBytecode() throws Exception {
        for (String path : System.getProperty("ia2.jars").split(File.pathSeparator)) {
            try (var jar = new JarFile(path)) {
                var entry = jar.getJarEntry("com/industrialage/ia2/IndustrialAge2.class");
                assertNotNull(entry);
                try (var bytes = new DataInputStream(jar.getInputStream(entry))) {
                    assertEquals(0xCAFEBABE, bytes.readInt());
                    bytes.readUnsignedShort();
                    assertEquals(path.contains("1.21.1") ? 65 : 69, bytes.readUnsignedShort());
                }
                var metadata = jar.getJarEntry("META-INF/neoforge.mods.toml");
                assertNotNull(metadata);
                var text =
                        new String(
                                jar.getInputStream(metadata).readAllBytes(),
                                StandardCharsets.UTF_8);
                assertTrue(text.contains("modId=\"ic2\""));
                assertTrue(text.contains("displayName=\"IndustrialCraft 2\""));
                assertTrue(text.contains("[[dependencies.ic2]]"));
                assertFalse(text.contains("modId=\"ia2\""));
                assertTrue(
                        text.contains(
                                "versionRange=\"["
                                        + (path.contains("1.21.1") ? "1.21.1" : "26.1")
                                        + "]\""));
                assertTrue(!text.contains("${"));
                assertNotNull(jar.getJarEntry("LICENSE"));
                assertNotNull(jar.getJarEntry("COPYING"));
                assertNotNull(jar.getJarEntry("LICENSE-ASSETS"));
            }
        }
    }

    @Test
    void productionJarsExcludeFixturesAndOldResources() throws Exception {
        var compiledTests = compiledTestEntries();
        for (String path : System.getProperty("ia2.jars").split(File.pathSeparator)) {
            try (var jar = new JarFile(path)) {
                var unexpectedEntries =
                        jar.stream()
                                .map(JarEntry::getName)
                                .filter(
                                        name ->
                                                name.startsWith("com/industrialage/ia2/gametest/")
                                                        || name.startsWith("data/ic2_gametest/")
                                                        || name.startsWith("data/ia2_gametest/")
                                                        || name.startsWith("assets/ia2/")
                                                        || name.startsWith("data/ia2/")
                                                        || compiledTests.contains(name)
                                                        || name.startsWith("org/junit/")
                                                        || name.startsWith("org/opentest4j/")
                                                        || name.startsWith("org/apiguardian/"))
                                .toList();
                assertTrue(
                        unexpectedEntries.isEmpty(),
                        () -> path + " contains unexpected entries: " + unexpectedEntries);
                var metadata = jar.getJarEntry("META-INF/neoforge.mods.toml");
                assertNotNull(metadata);
                try (var stream = jar.getInputStream(metadata)) {
                    assertFalse(
                            new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                                    .contains("ic2_gametest"),
                            path);
                }
            }
        }
    }

    private static Set<String> compiledTestEntries() throws IOException {
        var classDirectories = System.getProperty("ia2.testClassesDirs");
        assertNotNull(classDirectories, "Gradle did not provide the JUnit class directories");
        var entries = new HashSet<String>();
        for (String directory : classDirectories.split(File.pathSeparator)) {
            var root = Path.of(directory);
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (var paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".class"))
                        .map(root::relativize)
                        .map(path -> path.toString().replace(File.separatorChar, '/'))
                        .forEach(entries::add);
            }
        }
        assertFalse(
                entries.isEmpty(), "No compiled JUnit classes were available for isolation checks");
        return entries;
    }
}
