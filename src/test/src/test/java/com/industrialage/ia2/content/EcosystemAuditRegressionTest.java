package com.industrialage.ia2.content;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Regression tests covering copper_ingot removal, biogas_cell addition, dynamic c:/forge: tag
 * matching, transparent cutout pipe assets, standard common tags, and optional compat plugins.
 */
class EcosystemAuditRegressionTest {
    @Test
    void copperIngotRemovedAndBiogasCellRegisteredInMaterialsAndCreativeCatalog() {
        assertFalse(
                IndustrialMaterials.ADDITIONAL.contains("copper_ingot"),
                "ic2:copper_ingot must be removed from industrial-materials.tsv");
        assertThrows(
                IllegalArgumentException.class,
                () -> CreativeCategory.forItem("copper_ingot"),
                "ic2:copper_ingot must not belong to any creative category");
        for (var category : CreativeCategory.values()) {
            assertFalse(
                    category.entries().contains("copper_ingot"),
                    "copper_ingot must be absent from " + category);
        }
        assertEquals(
                CreativeCategory.FLUIDS,
                CreativeCategory.forItem("biogas_cell"),
                "ic2:biogas_cell must be listed under FLUIDS");
        assertTrue(CreativeCategory.FLUIDS.entries().contains("biogas_cell"));
    }

    @Test
    void dynamicTagAliasesMatchForgeCommonRawAndStorageBlockVariants() {
        // c: <-> forge:
        assertTrue(ProcessingMachine.matchesTagAlias("forge:ingots/tin", "c:ingots/tin"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("c:ingots/tin", "forge:ingots/tin"::equals));

        // raw_ores <-> raw_materials across c: and forge:
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "forge:raw_ores/tin", "c:raw_materials/tin"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "forge:raw_ores/tin", "forge:raw_materials/tin"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias("forge:raw_ores/tin", "c:raw_ores/tin"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "c:raw_materials/lead", "forge:raw_ores/lead"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("forge:raw_ores", "c:raw_materials"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("c:raw_materials", "forge:raw_ores"::equals));

        // blocks <-> storage_blocks across c: and forge:
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "forge:blocks/bronze", "c:storage_blocks/bronze"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "forge:blocks/bronze", "forge:storage_blocks/bronze"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "forge:blocks/bronze", "c:blocks/bronze"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "c:storage_blocks/steel", "forge:blocks/steel"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("forge:blocks", "c:storage_blocks"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("c:storage_blocks", "forge:blocks"::equals));

        // Non-aliased namespace stays exact
        assertTrue(ProcessingMachine.matchesTagAlias("minecraft:wool", "minecraft:wool"::equals));
        assertFalse(ProcessingMachine.matchesTagAlias("minecraft:wool", "c:wool"::equals));
        assertFalse(ProcessingMachine.matchesTagAlias("forge:ingots/tin", "c:ingots/lead"::equals));
        assertFalse(ProcessingMachine.matchesTagAlias("", tag -> true));
        assertFalse(ProcessingMachine.matchesTagAlias(null, tag -> true));
        assertFalse(ProcessingMachine.matchesTagAlias("c:ingots/tin", null));
    }

    @Test
    void processingRecipesResolveExternalItemsThroughDynamicCommonTagsAndFallback() {
        var maceratedRawTin =
                ProcessingMachine.recipe(
                        "macerator", "othermod:raw_tin", "c:raw_materials/tin"::equals);
        assertNotNull(maceratedRawTin);
        assertEquals("othermod:raw_tin", maceratedRawTin.input());
        assertEquals("ic2:crushed_tin", maceratedRawTin.output());
        assertEquals(2, maceratedRawTin.outputCount());

        var maceratedTinIngot =
                ProcessingMachine.recipe("macerator", "othermod:tin_ingot", "c:ingots/tin"::equals);
        assertNotNull(maceratedTinIngot);
        assertEquals("ic2:tin_dust", maceratedTinIngot.output());
        assertEquals(1, maceratedTinIngot.outputCount());

        var compressedBronze =
                ProcessingMachine.recipe(
                        "compressor", "othermod:bronze_ingot", "c:ingots/bronze"::equals);
        assertNotNull(compressedBronze);
        assertEquals(9, compressedBronze.inputCount());
        assertEquals("ic2:bronze_block", compressedBronze.output());

        var extrudedSteelShaft =
                ProcessingMachine.recipe(
                        "metal_former_extruding",
                        "othermod:steel_block",
                        "c:storage_blocks/steel"::equals);
        assertNotNull(extrudedSteelShaft);
        assertEquals("ic2:steel_shaft", extrudedSteelShaft.output());
        assertEquals(1, extrudedSteelShaft.outputCount());

        // Built-in reference fallback remains active even if the live tag predicate returns false.
        var fallback = ProcessingMachine.recipe("macerator", "ic2:raw_tin", tag -> false);
        assertNotNull(fallback);
        assertEquals("ic2:crushed_tin", fallback.output());
        assertEquals(2, fallback.outputCount());

        assertNull(ProcessingMachine.recipe("macerator", "othermod:unknown", tag -> false));

        // Running the tag-matched recipe through ProcessingMachine completes in 300 ticks.
        var macerator = new ProcessingMachine("macerator");
        macerator.receive(1200);
        for (int tick = 1; tick <= 300; tick++) {
            assertEquals(tick == 300, macerator.tick(maceratedRawTin, 1, 64));
        }
        assertEquals(1, macerator.completedOperations());

        var allMacerator = ProcessingMachine.allRecipes("macerator");
        assertTrue(allMacerator.stream().anyMatch(r -> r.input().equals("#forge:raw_ores/tin")));
        assertTrue(allMacerator.stream().anyMatch(r -> r.input().equals("minecraft:copper_ingot")));
    }

    @Test
    void packagedJarsReflectCopperRemovalBiogasCellCutoutPipesCommonTagsAndCompatPlugins()
            throws IOException {
        var targets = List.of(requiredProperty("ia2.targets").split(",", -1));
        var pipeTextures =
                List.of(
                        "bronze/fluid_pipe_arm.png",
                        "bronze/fluid_pipe_core.png",
                        "bronze/fluid_pipe_end.png",
                        "bronze/item_pipe_arm.png",
                        "bronze/item_pipe_core.png",
                        "bronze/item_pipe_end.png",
                        "bronze/item_pipe_outlet.png",
                        "steel/fluid_pipe_arm.png",
                        "steel/fluid_pipe_core.png",
                        "steel/fluid_pipe_end.png",
                        "steel/item_pipe_arm.png",
                        "steel/item_pipe_core.png",
                        "steel/item_pipe_end.png",
                        "steel/item_pipe_outlet.png");
        var neoCommonTags =
                List.of(
                        "data/c/tags/item/ingots/tin.json",
                        "data/c/tags/item/raw_materials/tin.json",
                        "data/c/tags/item/raw_ores/tin.json",
                        "data/c/tags/item/storage_blocks/bronze.json",
                        "data/c/tags/item/plates/copper.json",
                        "data/c/tags/item/dusts/copper.json",
                        "data/c/tags/fluid/steam.json",
                        "data/c/tags/fluid/biogas.json",
                        "data/forge/tags/fluid/steam.json",
                        "data/forge/tags/fluid/biogas.json");
        var forgeCommonTags =
                List.of(
                        "data/forge/tags/items/ingots/tin.json",
                        "data/forge/tags/items/dusts/copper.json",
                        "data/forge/tags/items/plates/bronze.json",
                        "data/forge/tags/items/raw_ores/tin.json",
                        "data/forge/tags/items/raw_materials/tin.json",
                        "data/forge/tags/blocks/ores/tin.json",
                        "data/forge/tags/blocks/storage_blocks/steel.json",
                        "data/forge/tags/blocks/blocks/steel.json",
                        "data/forge/tags/fluids/steam.json",
                        "data/forge/tags/fluids/biogas.json",
                        "data/c/tags/fluids/steam.json",
                        "data/c/tags/fluids/biogas.json");
        var compatClasses =
                List.of(
                        "com/industrialage/ia2/logistics/ItemPipeRouter.class",
                        "com/industrialage/ia2/compat/ProbeNbt.class",
                        "com/industrialage/ia2/compat/Ic2ProbeData.class",
                        "com/industrialage/ia2/compat/Ic2RecipeCatalog.class",
                        "com/industrialage/ia2/compat/Ic2JeiPlugin.class",
                        "com/industrialage/ia2/compat/Ic2EmiPlugin.class",
                        "com/industrialage/ia2/compat/Ic2JadePlugin.class",
                        "com/industrialage/ia2/compat/Ic2WthitCommonPlugin.class",
                        "com/industrialage/ia2/compat/Ic2WthitClientPlugin.class",
                        "wthit_plugins.json");

        for (String target : targets) {
            boolean forge = target.equals("1.19.2");
            String recipeDir = forge ? "data/ic2/recipes/" : "data/ic2/recipe/";
            Path jarPath = Path.of(requiredProperty("ia2.target." + target + ".jar"));
            try (var jar = new JarFile(jarPath.toFile())) {
                // ic2:copper_ingot removed
                assertNull(jar.getJarEntry("assets/ic2/models/item/copper_ingot.json"), target);
                assertNull(jar.getJarEntry("assets/ic2/items/copper_ingot.json"), target);
                assertNull(jar.getJarEntry(recipeDir + "classic/copper_ingot.json"), target);

                // ic2:biogas_cell present
                assertNotNull(jar.getJarEntry("assets/ic2/models/item/biogas_cell.json"), target);
                assertNotNull(
                        jar.getJarEntry("assets/ic2/textures/items/cell/biogas_cell.png"), target);
                if (!forge && !target.equals("1.21.1")) {
                    assertNotNull(jar.getJarEntry("assets/ic2/items/biogas_cell.json"), target);
                }
                String fillBiogas = readEntry(jar, recipeDir + "cells/fill_biogas.json");
                assertTrue(fillBiogas.contains("\"ic2:biogas_cell\""), target + ": fill_biogas");
                String recoverBiogas = readEntry(jar, recipeDir + "cells/recover_biogas_cell.json");
                assertTrue(
                        recoverBiogas.contains("\"ic2:fluid_cell\""), target + ": recover_biogas");

                // Bilingual lang keys
                for (String lang : Set.of("en_us", "zh_cn")) {
                    String text = readEntry(jar, "assets/ic2/lang/" + lang + ".json");
                    assertFalse(
                            text.contains("\"item.ic2.copper_ingot\""),
                            target + " " + lang + " must not contain item.ic2.copper_ingot");
                    assertTrue(
                            text.contains("\"item.ic2.biogas_cell\""),
                            target + " " + lang + " must contain item.ic2.biogas_cell");
                    assertTrue(text.contains("\"tooltip.ic2.rotor.durability\""), target + lang);
                    assertTrue(text.contains("\"tooltip.ic2.rotor.max_safe_wind\""), target + lang);
                    assertTrue(text.contains("\"jei.ic2.category.macerator\""), target + lang);
                    assertTrue(text.contains("\"emi.category.ic2.macerator\""), target + lang);
                    assertTrue(text.contains("\"config.jade.plugin_ic2.device\""), target + lang);
                    assertTrue(text.contains("\"config.waila.plugin_ic2\""), target + lang);
                    String sortLabel = lang.equals("zh_cn") ? "自动整理" : "Sort";
                    assertTrue(
                            text.contains("\"gui.ic2.box.sort\": \"" + sortLabel + "\""),
                            target + " " + lang);
                }

                // 14 transparent cutout pipe textures + culled cutout pipe models
                for (String rel : pipeTextures) {
                    var entry = jar.getJarEntry("assets/ic2/textures/blocks/transport/" + rel);
                    assertNotNull(entry, target + ": missing " + rel);
                    try (var stream = jar.getInputStream(entry)) {
                        var image = ImageIO.read(stream);
                        assertNotNull(image, target + ": unreadable " + rel);
                        assertTrue(image.getColorModel().hasAlpha(), target + ": alpha " + rel);
                    }
                }
                String pipeModel =
                        readEntry(
                                jar,
                                "assets/ic2/models/block/transport/bronze_item_small_east.json");
                assertTrue(pipeModel.contains("\"minecraft:cutout\""), target + ": cutout model");
                String northArmModel =
                        readEntry(
                                jar,
                                "assets/ic2/models/block/transport/bronze_item_small_north.json");
                assertTrue(
                        northArmModel.contains("\"minecraft:cutout\""),
                        target + ": north arm cutout");
                assertFalse(
                        northArmModel.contains("\"south\""),
                        target + ": north pipe arm must cull its inner south face");
                String coreModel =
                        readEntry(
                                jar,
                                "assets/ic2/models/block/transport/bronze_item_small_core_north.json");
                assertTrue(coreModel.contains("\"minecraft:cutout\""), target + ": core cutout");

                // Waterlogged mining pipe blockstates
                for (String miningPipe : List.of("mining_pipe", "mining_pipe_tip")) {
                    String stateJson =
                            readEntry(jar, "assets/ic2/blockstates/" + miningPipe + ".json");
                    assertTrue(
                            stateJson.contains("\"waterlogged=false\""),
                            target + ": " + miningPipe + " missing waterlogged=false");
                    assertTrue(
                            stateJson.contains("\"waterlogged=true\""),
                            target + ": " + miningPipe + " missing waterlogged=true");
                }

                // Standard c:/forge: tags
                for (String tagPath : (forge ? forgeCommonTags : neoCommonTags)) {
                    assertNotNull(jar.getJarEntry(tagPath), target + ": missing " + tagPath);
                }

                // Compat plugins
                for (String compat : compatClasses) {
                    assertNotNull(jar.getJarEntry(compat), target + ": missing " + compat);
                }
                String wthit = readEntry(jar, "wthit_plugins.json");
                assertTrue(wthit.contains("Ic2WthitCommonPlugin"), target);
                assertTrue(wthit.contains("Ic2WthitClientPlugin"), target);
            }
        }
    }

    private static String readEntry(JarFile jar, String path) throws IOException {
        var entry = jar.getJarEntry(path);
        assertNotNull(entry, "Missing JAR entry " + path);
        try (var stream = jar.getInputStream(entry)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String requiredProperty(String key) {
        String value = System.getProperty(key);
        assertNotNull(value, "Missing system property " + key);
        assertFalse(value.isBlank(), "Blank system property " + key);
        return value;
    }
}
