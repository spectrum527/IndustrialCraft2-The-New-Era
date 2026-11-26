package com.industrialage.ia2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class BuildContractTest {
    private static final Pattern DEPENDENCY_FIELD =
            Pattern.compile("\\s*(modId|versionRange|type)\\s*=\\s*\"([^\"]*)\"\\s*(?:#.*)?");

    @Test
    void jarsContainEntrypointMetadataAndCorrectBytecode() throws Exception {
        for (var platform : platformJars()) {
            try (var jar = new JarFile(platform.path().toFile())) {
                var entry = jar.getJarEntry("com/industrialage/ia2/IndustrialAge2.class");
                assertNotNull(entry, platform.target());
                try (var bytes = new DataInputStream(jar.getInputStream(entry))) {
                    assertEquals(0xCAFEBABE, bytes.readInt(), platform.target());
                    bytes.readUnsignedShort();
                    assertEquals(
                            platform.bytecodeVersion(),
                            bytes.readUnsignedShort(),
                            platform.target());
                }
                var metadata = jar.getJarEntry("META-INF/neoforge.mods.toml");
                assertNotNull(metadata, platform.target());
                String text;
                try (var stream = jar.getInputStream(metadata)) {
                    text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                }
                assertTrue(text.contains("modId=\"ic2\""));
                assertTrue(text.contains("version=\"" + requiredProperty("ic2.modVersion") + "\""));
                assertTrue(text.contains("displayName=\"IndustrialCraft²: The New Era\""));
                assertTrue(
                        text.contains(
                                "displayURL=\"https://github.com/spectrum527/IndustrialCraft2-The-New-Era\""));
                assertTrue(text.contains("[[dependencies.ic2]]"));
                assertFalse(text.contains("modId=\"ia2\""));
                var dependencies = dependencies(text, platform.target());
                assertDependency(
                        dependencies,
                        "minecraft",
                        "[" + platform.target() + "]",
                        platform.target());
                assertDependency(
                        dependencies,
                        "neoforge",
                        "[" + platform.neoForgeVersion() + ",)",
                        platform.target());
                var mixins = jar.getJarEntry("ic2.mixins.json");
                assertNotNull(mixins, platform.target() + ": ic2.mixins.json");
                try (var stream = jar.getInputStream(mixins)) {
                    var mixinText = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    assertTrue(
                            mixinText.contains("\"minVersion\""),
                            platform.target() + ": ic2.mixins.json must declare minVersion");
                }
                assertNull(
                        jar.getJarEntry(
                                "data/ic2/recipe/industry/shaped/heat_exchanger_from_gold.json"),
                        platform.target() + ": conflicting heat_exchanger_from_gold recipe");
                assertNotNull(
                        jar.getJarEntry(
                                "data/ic2/recipe/industry/shaped/component_heat_exchanger.json"),
                        platform.target() + ": component_heat_exchanger recipe");
                assertFalse(text.contains("${"));
                assertNotNull(jar.getJarEntry("LICENSE"));
                assertNotNull(jar.getJarEntry("COPYING"));
                assertNotNull(jar.getJarEntry("LICENSE-ASSETS"));
                for (var language : List.of("en_us", "zh_cn")) {
                    var translation = jar.getJarEntry("assets/ic2/lang/" + language + ".json");
                    assertNotNull(translation, platform.target() + ": " + language);
                    try (var stream = jar.getInputStream(translation)) {
                        var locale = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                        assertTrue(
                                locale.contains(
                                        "\"itemGroup.ic2\": \"IndustrialCraft²: The New Era\""),
                                platform.target() + ": " + language);
                        String accessName = language.equals("zh_cn") ? "六面权限" : "Side access";
                        assertTrue(
                                locale.contains(
                                        "\"gui.ic2.access.button\": \"" + accessName + "\""),
                                language);
                    }
                }
                var notice = jar.getJarEntry("NOTICE.md");
                assertNotNull(notice);
                try (var stream = jar.getInputStream(notice)) {
                    assertArrayEquals(
                            Files.readAllBytes(Path.of(requiredProperty("ic2.notice"))),
                            stream.readAllBytes(),
                            platform.target());
                }
            }
        }
    }

    @Test
    void sideAccessGearHasATransparentOutlineAndCenter() throws IOException {
        for (var platform : platformJars()) {
            try (var jar = new JarFile(platform.path().toFile())) {
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
        for (var platform : platformJars()) {
            try (var jar = new JarFile(platform.path().toFile())) {
                var unexpectedEntries =
                        jar.stream()
                                .map(JarEntry::getName)
                                .filter(
                                        name ->
                                                name.startsWith("com/industrialage/ia2/gametest/")
                                                        || name.startsWith("data/ic2_gametest/")
                                                        || name.startsWith("data/ia2_gametest/")
                                                        || name.startsWith("assets/ia2/")
                                                        || name.startsWith("assets/brand/")
                                                        || name.startsWith("data/ia2/")
                                                        || compiledTests.contains(name)
                                                        || name.startsWith("org/junit/")
                                                        || name.startsWith("org/opentest4j/")
                                                        || name.endsWith(".csv")
                                                        || name.startsWith("org/apiguardian/"))
                                .toList();
                assertTrue(
                        unexpectedEntries.isEmpty(),
                        () ->
                                platform.path()
                                        + " contains unexpected entries: "
                                        + unexpectedEntries);
                var metadata = jar.getJarEntry("META-INF/neoforge.mods.toml");
                assertNotNull(metadata);
                try (var stream = jar.getInputStream(metadata)) {
                    assertFalse(
                            new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                                    .contains("ic2_gametest"),
                            platform.path().toString());
                }
            }
        }
    }

    private static List<PlatformJar> platformJars() {
        var names = List.of(requiredProperty("ia2.targets").split(",", -1));
        assertEquals(names.size(), new HashSet<>(names).size(), "Duplicate NeoForge test targets");
        return names.stream()
                .map(
                        name -> {
                            var bytecodeVersion =
                                    switch (name) {
                                        case "1.21.1" -> 65;
                                        case "26.1.2", "26.2" -> 69;
                                        default ->
                                                throw new AssertionError(
                                                        "Unexpected target: " + name);
                                    };
                            var prefix = "ia2.target." + name + ".";
                            return new PlatformJar(
                                    name,
                                    Path.of(requiredProperty(prefix + "jar")),
                                    requiredProperty(prefix + "neoForgeVersion"),
                                    bytecodeVersion);
                        })
                .toList();
    }

    private static String requiredProperty(String key) {
        var value = System.getProperty(key);
        assertNotNull(value, "Gradle did not provide " + key);
        assertFalse(value.isBlank(), "Gradle provided an empty " + key);
        return value;
    }

    private static List<Map<String, String>> dependencies(String text, String target) {
        var result = new ArrayList<Map<String, String>>();
        Map<String, String> current = null;
        for (var line : text.split("\\R")) {
            var trimmed = line.strip();
            if (trimmed.startsWith("[")) {
                if (current != null) {
                    result.add(current);
                }
                current = trimmed.equals("[[dependencies.ic2]]") ? new HashMap<>() : null;
            } else if (current != null) {
                var field = DEPENDENCY_FIELD.matcher(line);
                if (field.matches()) {
                    assertNull(
                            current.put(field.group(1), field.group(2)),
                            target + ": duplicate dependency field");
                }
            }
        }
        if (current != null) {
            result.add(current);
        }
        assertTrue(result.stream().allMatch(entry -> entry.containsKey("modId")), target);
        return result;
    }

    private static void assertDependency(
            List<Map<String, String>> dependencies,
            String modId,
            String expectedRange,
            String target) {
        var matches =
                dependencies.stream().filter(entry -> modId.equals(entry.get("modId"))).toList();
        assertEquals(1, matches.size(), target + ": expected exactly one " + modId + " dependency");
        assertEquals("required", matches.getFirst().get("type"), target + ": " + modId);
        assertEquals(expectedRange, matches.getFirst().get("versionRange"), target + ": " + modId);
    }

    private record PlatformJar(
            String target, Path path, String neoForgeVersion, int bytecodeVersion) {}

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
