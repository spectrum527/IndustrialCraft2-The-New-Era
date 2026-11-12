package com.industrialage.ia2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
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
                assertTrue(text.contains("modId=\"ia2\""));
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
    void productionJarsExcludeDevelopmentTests() throws Exception {
        for (String path : System.getProperty("ia2.jars").split(File.pathSeparator)) {
            try (var jar = new JarFile(path)) {
                var testEntries =
                        jar.stream()
                                .map(JarEntry::getName)
                                .filter(
                                        name ->
                                                name.startsWith("com/industrialage/ia2/gametest/")
                                                        || name.startsWith("data/ia2_gametest/")
                                                        || name.equals(
                                                                "com/industrialage/ia2/BuildContractTest.class")
                                                        || name.startsWith("org/junit/")
                                                        || name.startsWith("org/opentest4j/")
                                                        || name.startsWith("org/apiguardian/"))
                                .toList();
                assertTrue(
                        testEntries.isEmpty(),
                        () -> path + " contains test fixtures: " + testEntries);
                var metadata = jar.getJarEntry("META-INF/neoforge.mods.toml");
                assertNotNull(metadata);
                try (var stream = jar.getInputStream(metadata)) {
                    assertFalse(
                            new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                                    .contains("ia2_gametest"),
                            path);
                }
            }
        }
    }
}
