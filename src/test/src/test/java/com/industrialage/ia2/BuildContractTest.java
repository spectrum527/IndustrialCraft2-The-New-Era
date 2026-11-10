package com.industrialage.ia2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
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
                assertNotNull(jar.getJarEntry("LICENSE-ASSETS"));
            }
        }
    }
}
