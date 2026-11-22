package com.industrialage.ia2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import javax.imageio.ImageIO;
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
                    assertEquals(61, bytes.readUnsignedShort());
                }
                var metadata = jar.getJarEntry("META-INF/mods.toml");
                assertNotNull(metadata);
                var text =
                        new String(
                                jar.getInputStream(metadata).readAllBytes(),
                                StandardCharsets.UTF_8);
                assertTrue(text.contains("modId=\"ic2\""));
                var version = System.getProperty("ic2.modVersion");
                assertNotNull(version);
                assertFalse(version.isBlank());
                assertTrue(text.contains("version=\"" + version + "\""));
                assertTrue(text.contains("displayName=\"IndustrialCraft²: The New Era\""));
                assertTrue(
                        text.contains(
                                "displayURL=\"https://github.com/spectrum527/IndustrialCraft2-The-New-Era\""));
                assertTrue(text.contains("[[dependencies.ic2]]"));
                assertFalse(text.contains("modId=\"ia2\""));
                assertTrue(text.contains("modLoader=\"javafml\""));
                assertTrue(text.contains("versionRange=\"[1.19.2]\""));
                assertTrue(text.contains("versionRange=\"[43.5.0,44)\""));
                assertFalse(text.contains("${"));
                assertNotNull(jar.getJarEntry("com/industrialage/ia2/config/active-keys.tsv"));
                assertNotNull(jar.getJarEntry("assets/ic2/status/client.json"));
                for (String type :
                        Set.of(
                                "energy/graph/EnergyGraph", "energy/graph/PacketBudget",
                                "machine/BlockEntityBase", "machine/FuelGenerator",
                                "machine/ForgeMachineBlockEntity",
                                        "machine/FuelGeneratorBlockEntity")) {
                    assertNotNull(
                            jar.getJarEntry("com/industrialage/ia2/" + type + ".class"), type);
                }
                assertNotNull(jar.getJarEntry("LICENSE"));
                assertNotNull(jar.getJarEntry("COPYING"));
                assertNotNull(jar.getJarEntry("LICENSE-ASSETS"));
                for (var language : Set.of("en_us", "zh_cn")) {
                    var translation = jar.getJarEntry("assets/ic2/lang/" + language + ".json");
                    assertNotNull(translation, language);
                    try (var stream = jar.getInputStream(translation)) {
                        var locale = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                        assertTrue(
                                locale.contains(
                                        "\"itemGroup.ic2\": \"IndustrialCraft²: The New Era\""),
                                language);
                        String accessName = language.equals("zh_cn") ? "六面权限" : "Side access";
                        assertTrue(
                                locale.contains(
                                        "\"gui.ic2.access.button\": \"" + accessName + "\""),
                                language);
                    }
                }
                var notice = jar.getJarEntry("NOTICE.md");
                assertNotNull(notice);
                var noticePath = System.getProperty("ic2.notice");
                assertNotNull(noticePath);
                try (var stream = jar.getInputStream(notice)) {
                    assertArrayEquals(
                            Files.readAllBytes(Path.of(noticePath)), stream.readAllBytes());
                }
            }
        }
    }

    @Test
    void sideAccessGearHasATransparentOutlineAndCenter() throws IOException {
        for (String path : System.getProperty("ia2.jars").split(File.pathSeparator)) {
            try (var jar = new JarFile(path)) {
                var entry = jar.getJarEntry("assets/ic2/textures/gui/icons/side_access_gear.png");
                assertNotNull(entry);
                try (var stream = jar.getInputStream(entry)) {
                    var icon = ImageIO.read(stream);
                    assertNotNull(icon);
                    assertEquals(16, icon.getWidth());
                    assertEquals(16, icon.getHeight());
                    assertTrue(icon.getColorModel().hasAlpha());
                    assertEquals(0, icon.getRGB(0, 0) >>> 24, "transparent margin");
                    assertEquals(0, icon.getRGB(8, 8) >>> 24, "open center");
                    assertTrue((icon.getRGB(8, 2) >>> 24) > 0, "visible upper tooth");
                }
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
                                                        || name.startsWith(
                                                                "com/industrialage/ia2/forge/qa/")
                                                        || name.startsWith("data/ic2_qa/")
                                                        || name.startsWith("assets/ic2_qa/")
                                                        || name.startsWith("data/ic2_gametest/")
                                                        || name.startsWith("data/ia2_gametest/")
                                                        || name.startsWith("assets/ia2/")
                                                        || name.startsWith("assets/brand/")
                                                        || name.startsWith("data/ia2/")
                                                        || compiledTests.contains(name)
                                                        || name.startsWith("org/junit/")
                                                        || name.startsWith("org/opentest4j/")
                                                        || name.startsWith("org/apiguardian/"))
                                .toList();
                assertTrue(
                        unexpectedEntries.isEmpty(),
                        () -> path + " contains unexpected entries: " + unexpectedEntries);
                var metadata = jar.getJarEntry("META-INF/mods.toml");
                assertNotNull(metadata);
                try (var stream = jar.getInputStream(metadata)) {
                    assertFalse(
                            new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                                    .contains("ic2_qa"),
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
