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

        // tiny_dusts <-> small_dusts, rubbers <-> rubber, coal_coke <-> fuels/coal_coke,
        // tools/wrench <-> wrenches, vanilla/loader diff tags, and minecraft:*_ores <-> ores/*
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "forge:small_dusts/tin", "c:tiny_dusts/tin"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "c:tiny_dusts/lead", "forge:small_dusts/lead"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("forge:rubbers", "c:rubber"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("c:rubber", "forge:rubbers"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias("forge:coal_coke", "c:fuels/coal_coke"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("forge:tools/wrench", "c:wrenches"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("forge:obsidian", "c:obsidians"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("forge:glass", "c:glass_blocks"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("forge:sand", "c:sands"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("forge:stone", "c:stones"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias("forge:cobblestone", "c:cobblestones"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "minecraft:copper_ores", "c:ores/copper"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "forge:ores/iron", "minecraft:iron_ores"::equals));

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
        assertTrue(allMacerator.stream().anyMatch(r -> r.input().equals("#forge:ingots/copper")));
        assertTrue(allMacerator.stream().anyMatch(r -> r.input().equals("#forge:ores/tin")));
        assertTrue(allMacerator.stream().anyMatch(r -> r.input().equals("#minecraft:copper_ores")));

        // Weighted ore-block maceration resolves both built-in ores and third-party c:/forge: ores.
        var maceratedTinOre = ProcessingMachine.recipe("macerator", "ic2:tin_ore", tag -> false);
        assertNotNull(maceratedTinOre);
        assertEquals("ic2:raw_tin", maceratedTinOre.output());
        assertEquals(2, maceratedTinOre.outputCount());
        assertEquals(4, maceratedTinOre.maxOutputCount());
        assertEquals(2, maceratedTinOre.rollOutputCount(1, bound -> 0));
        assertEquals(3, maceratedTinOre.rollOutputCount(1, bound -> 6));
        assertEquals(4, maceratedTinOre.rollOutputCount(1, bound -> 9));

        var externalTinOre =
                ProcessingMachine.recipe("macerator", "othermod:tin_ore", "c:ores/tin"::equals);
        assertNotNull(externalTinOre);
        assertEquals("ic2:raw_tin", externalTinOre.output());

        var externalCopperOre =
                ProcessingMachine.recipe(
                        "macerator", "othermod:copper_ore", "c:ores/copper"::equals);
        assertNotNull(externalCopperOre);
        assertEquals("minecraft:raw_copper", externalCopperOre.output());
        assertEquals(10, externalCopperOre.maxOutputCount());

        var externalCopperIngot =
                ProcessingMachine.recipe(
                        "macerator", "othermod:copper_ingot", "c:ingots/copper"::equals);
        assertNotNull(externalCopperIngot);
        assertEquals("ic2:copper_dust", externalCopperIngot.output());

        var compressedTinyTin =
                ProcessingMachine.recipe(
                        "compressor", "othermod:tiny_tin_dust", "c:tiny_dusts/tin"::equals);
        assertNotNull(compressedTinyTin);
        assertEquals("ic2:tin_dust", compressedTinyTin.output());
        assertEquals(9, compressedTinyTin.inputCount());
    }

    @Test
    void multiCandidateTagAliasesCoverWrenchesShearsVanillaOresAndNeoForgeRenames() {
        var wrenchTags =
                List.of(
                        "c:tools/wrench",
                        "c:tools/wrenches",
                        "c:wrenches",
                        "forge:tools/wrench",
                        "forge:tools/wrenches",
                        "forge:wrenches");
        for (String a : wrenchTags) {
            for (String b : wrenchTags) {
                assertTrue(
                        ProcessingMachine.matchesTagAlias(a, b::equals), a + " must alias to " + b);
            }
        }
        assertTrue(ProcessingMachine.tagAliasCandidates("c:tools/wrench").containsAll(wrenchTags));

        var shearTags =
                List.of(
                        "c:shears",
                        "c:tools/shear",
                        "c:tools/shears",
                        "forge:shears",
                        "forge:tools/shear",
                        "forge:tools/shears");
        for (String a : shearTags) {
            for (String b : shearTags) {
                assertTrue(
                        ProcessingMachine.matchesTagAlias(a, b::equals), a + " must alias to " + b);
            }
        }

        for (String ore :
                List.of(
                        "coal",
                        "copper",
                        "iron",
                        "gold",
                        "lapis",
                        "redstone",
                        "diamond",
                        "emerald")) {
            assertTrue(
                    ProcessingMachine.matchesTagAlias(
                            "minecraft:" + ore + "_ores", ("c:ores/" + ore)::equals));
            assertTrue(
                    ProcessingMachine.matchesTagAlias(
                            "minecraft:" + ore + "_ores", ("forge:ores/" + ore)::equals));
            assertTrue(
                    ProcessingMachine.matchesTagAlias(
                            "c:ores/" + ore, ("minecraft:" + ore + "_ores")::equals));
            assertTrue(
                    ProcessingMachine.matchesTagAlias(
                            "forge:ores/" + ore, ("minecraft:" + ore + "_ores")::equals));
        }

        assertTrue(ProcessingMachine.matchesTagAlias("forge:gravel", "c:gravels"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("c:gravels", "forge:gravel"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("forge:slimeballs", "c:slime_balls"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("c:slime_balls", "forge:slimeball"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("forge:netherrack", "c:netherracks"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias("forge:sandstone", "c:sandstone/blocks"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias("c:sandstone/blocks", "forge:sandstone"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("forge:leather", "c:leathers"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("forge:string", "c:strings"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("forge:bone", "c:bones"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("forge:feather", "c:feathers"::equals));
        assertTrue(ProcessingMachine.matchesTagAlias("forge:gunpowder", "c:gunpowders"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "forge:dense_plates/iron", "c:plates/dense/iron"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "c:plates/dense/copper", "forge:dense_plates/copper"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "forge:sand/colorless", "c:sands/colorless"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias("forge:ingots/brick", "c:bricks/normal"::equals));
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "c:bricks/nether", "forge:ingots/nether_brick"::equals));
    }

    @Test
    void tagPriorityPreventsBroadStoneAndCobblestoneTagsFromShadowingSpecificOres() {
        var silverCobbleOre =
                ProcessingMachine.recipe(
                        "macerator",
                        "othermod:cobbled_silver_ore",
                        tag -> tag.equals("c:cobblestones") || tag.equals("c:ores/silver"));
        assertNotNull(silverCobbleOre);
        assertEquals("ic2:crushed_silver", silverCobbleOre.output());
        assertEquals(2, silverCobbleOre.outputCount());

        var tinStoneOre =
                ProcessingMachine.recipe(
                        "macerator",
                        "othermod:stone_tin_ore",
                        tag -> tag.equals("c:stones") || tag.equals("c:ores/tin"));
        assertNotNull(tinStoneOre);
        assertEquals("ic2:raw_tin", tinStoneOre.output());

        var plainCobble =
                ProcessingMachine.recipe(
                        "macerator", "othermod:cobbled_slate", "c:cobblestones"::equals);
        assertNotNull(plainCobble);
        assertEquals("minecraft:sand", plainCobble.output());

        var plainStone =
                ProcessingMachine.recipe("macerator", "othermod:limestone", "c:stones"::equals);
        assertNotNull(plainStone);
        assertEquals("minecraft:cobblestone", plainStone.output());
    }

    @Test
    void expandedTagRecipesCoverCommonMaterialsOresCropsAndStorageBlocks() {
        var gravel = ProcessingMachine.recipe("macerator", "othermod:gravel", "c:gravels"::equals);
        assertNotNull(gravel);
        assertEquals("minecraft:flint", gravel.output());

        var netherrack =
                ProcessingMachine.recipe(
                        "macerator", "othermod:netherrack", "c:netherracks"::equals);
        assertNotNull(netherrack);
        assertEquals("ic2:netherrack_dust", netherrack.output());

        var sandstone =
                ProcessingMachine.recipe(
                        "macerator", "othermod:sandstone", "c:sandstone/blocks"::equals);
        assertNotNull(sandstone);
        assertEquals("minecraft:sand", sandstone.output());

        var bone = ProcessingMachine.recipe("macerator", "othermod:bone", "c:bones"::equals);
        assertNotNull(bone);
        assertEquals("minecraft:white_dye", bone.output());
        assertEquals(4, bone.outputCount());

        var carrot =
                ProcessingMachine.recipe("macerator", "othermod:carrot", "c:crops/carrot"::equals);
        assertNotNull(carrot);
        assertEquals("ic2:bio_chaff", carrot.output());
        assertEquals(8, carrot.inputCount());

        var beetroot =
                ProcessingMachine.recipe(
                        "macerator", "othermod:beetroot", "c:crops/beetroot"::equals);
        assertNotNull(beetroot);
        assertEquals("ic2:bio_chaff", beetroot.output());
        assertEquals(8, beetroot.inputCount());

        var beetrootSeeds =
                ProcessingMachine.recipe(
                        "macerator", "othermod:beetroot_seeds", "c:seeds/beetroot"::equals);
        assertNotNull(beetrootSeeds);
        assertEquals("ic2:bio_chaff", beetrootSeeds.output());
        assertEquals(16, beetrootSeeds.inputCount());

        var rawSilver =
                ProcessingMachine.recipe(
                        "macerator", "othermod:raw_silver", "c:raw_materials/silver"::equals);
        assertNotNull(rawSilver);
        assertEquals("ic2:crushed_silver", rawSilver.output());
        assertEquals(2, rawSilver.outputCount());

        var coalOre =
                ProcessingMachine.recipe("macerator", "othermod:coal_ore", "c:ores/coal"::equals);
        assertNotNull(coalOre);
        assertEquals("minecraft:coal", coalOre.output());
        assertEquals(2, coalOre.outputCount());

        var diamondOre =
                ProcessingMachine.recipe(
                        "macerator", "othermod:diamond_ore", "c:ores/diamond"::equals);
        assertNotNull(diamondOre);
        assertEquals("minecraft:diamond", diamondOre.output());
        assertEquals(2, diamondOre.outputCount());

        var emeraldOre =
                ProcessingMachine.recipe(
                        "macerator", "othermod:emerald_ore", "c:ores/emerald"::equals);
        assertNotNull(emeraldOre);
        assertEquals("minecraft:emerald", emeraldOre.output());
        assertEquals(2, emeraldOre.outputCount());

        var lapisOre =
                ProcessingMachine.recipe("macerator", "othermod:lapis_ore", "c:ores/lapis"::equals);
        assertNotNull(lapisOre);
        assertEquals("minecraft:lapis_lazuli", lapisOre.output());
        assertEquals(10, lapisOre.outputCount());

        var redstoneOre =
                ProcessingMachine.recipe(
                        "macerator", "othermod:redstone_ore", "c:ores/redstone"::equals);
        assertNotNull(redstoneOre);
        assertEquals("minecraft:redstone", redstoneOre.output());
        assertEquals(10, redstoneOre.outputCount());

        var quartzOre =
                ProcessingMachine.recipe(
                        "macerator", "othermod:quartz_ore", "c:ores/quartz"::equals);
        assertNotNull(quartzOre);
        assertEquals("minecraft:quartz", quartzOre.output());
        assertEquals(2, quartzOre.outputCount());

        var expectedBlocks =
                java.util.Map.of(
                        "bronze", "ic2:bronze_ingot",
                        "copper", "minecraft:copper_ingot",
                        "gold", "minecraft:gold_ingot",
                        "iron", "minecraft:iron_ingot",
                        "silver", "ic2:silver_ingot",
                        "steel", "ic2:steel_ingot");
        for (var entry : expectedBlocks.entrySet()) {
            var block =
                    ProcessingMachine.recipe(
                            "macerator",
                            "othermod:" + entry.getKey() + "_block",
                            ("c:storage_blocks/" + entry.getKey())::equals);
            assertNotNull(block, "Missing macerator storage block recipe for " + entry.getKey());
            assertEquals(entry.getValue(), block.output());
            assertEquals(9, block.outputCount());
        }

        var gunpowder =
                ProcessingMachine.recipe("extractor", "othermod:gunpowder", "c:gunpowders"::equals);
        assertNotNull(gunpowder);
        assertEquals("ic2:sulfur_dust", gunpowder.output());

        var extNetherrack =
                ProcessingMachine.recipe(
                        "extractor", "othermod:netherrack", "c:netherracks"::equals);
        assertNotNull(extNetherrack);
        assertEquals("ic2:small_sulfur_dust", extNetherrack.output());
    }

    @Test
    void recyclerRulesApplyActiveConfigBlacklistWhitelistAndTagPrecedence() {
        assertEquals("balance/recyclerBlacklist", RecyclerRules.BLACKLIST.name());
        assertEquals("balance/recyclerWhitelist", RecyclerRules.WHITELIST.name());

        for (String blocked : RecyclerRules.DEFAULT_BLACKLIST) {
            assertTrue(
                    RecyclerRules.isExcluded(
                            blocked, tag -> false, RecyclerRules.DEFAULT_BLACKLIST, List.of()),
                    blocked + " must be excluded by default blacklist");
        }
        assertFalse(
                RecyclerRules.isExcluded(
                        "minecraft:cobblestone",
                        tag -> false,
                        RecyclerRules.DEFAULT_BLACKLIST,
                        List.of()));

        // Built-in tags exclude third-party panes, wooden rods, and custom ic2:recycler_blacklist.
        assertTrue(
                RecyclerRules.isExcluded(
                        "othermod:stained_pane",
                        "forge:glass_panes"::equals,
                        List.of(),
                        List.of()));
        assertTrue(
                RecyclerRules.isExcluded(
                        "othermod:oak_rod", "c:rods/wooden"::equals, List.of(), List.of()));
        assertTrue(
                RecyclerRules.isExcluded(
                        "othermod:junk", "ic2:recycler_blacklist"::equals, List.of(), List.of()));

        // Configured Tag: entries expand across c:/forge: aliases; Fluid: entries do not match.
        assertTrue(
                RecyclerRules.isExcluded(
                        "othermod:cobbled_basalt",
                        "forge:cobblestone"::equals,
                        List.of("Tag:c:cobblestones", "Fluid:minecraft:water"),
                        List.of()));
        assertFalse(
                RecyclerRules.matchesEntry(
                        "minecraft:water_bucket", tag -> true, "Fluid:minecraft:water"));

        // Non-empty whitelist takes strict precedence over blacklist and built-in tag exclusions.
        var whitelist = List.of("Tag:c:rods/wooden", "minecraft:stick");
        assertFalse(
                RecyclerRules.isExcluded(
                        "minecraft:stick",
                        "c:rods/wooden"::equals,
                        RecyclerRules.DEFAULT_BLACKLIST,
                        whitelist));
        assertFalse(
                RecyclerRules.isExcluded(
                        "othermod:bamboo_rod",
                        "forge:rods/wooden"::equals,
                        RecyclerRules.DEFAULT_BLACKLIST,
                        whitelist));
        assertTrue(
                RecyclerRules.isExcluded(
                        "minecraft:cobblestone",
                        tag -> false,
                        RecyclerRules.DEFAULT_BLACKLIST,
                        whitelist));
    }

    @Test
    void uuValuesResolveExactAndCommonTagEquivalentsForScannerAndAssemblyBench() {
        assertEquals(40_000, UuValues.scanCostMicroliters("minecraft:diamond", tag -> false));
        assertEquals(2_000, UuValues.scanCostMicroliters("ic2:tin_ore", tag -> false));
        assertEquals(120_000, UuValues.scanCostMicroliters("ic2:iridium_ore", tag -> false));

        assertEquals(2_000, UuValues.scanCostMicroliters("othermod:tin_ore", "c:ores/tin"::equals));
        assertEquals(
                3_000,
                UuValues.scanCostMicroliters("othermod:raw_lead", "forge:raw_ores/lead"::equals));
        assertEquals(
                6_000,
                UuValues.scanCostMicroliters("othermod:silver_ingot", "c:ingots/silver"::equals));
        assertEquals(
                15_000,
                UuValues.scanCostMicroliters("othermod:uranium_ingot", "c:ingots/uranium"::equals));
        assertEquals(
                8_000,
                UuValues.scanCostMicroliters(
                        "othermod:gold_stone_ore",
                        tag -> tag.equals("c:stones") || tag.equals("c:ores/gold")));
        assertEquals(0, UuValues.scanCostMicroliters("othermod:unknown", tag -> false));

        var canonical = UuValues.assemblyRecipe("ic2:uu_matter", tag -> false);
        assertNotNull(canonical);
        assertEquals(1, canonical.uuCost());
        assertEquals("minecraft:oak_log", canonical.outputId());
        assertEquals(8, canonical.outputCount());

        var moddedTin = UuValues.assemblyRecipe("othermod:tin_ore", "c:ores/tin"::equals);
        assertNotNull(moddedTin);
        assertEquals(2, moddedTin.uuCost());
        assertEquals("othermod:tin_ore", moddedTin.outputId());
        assertEquals(5, moddedTin.outputCount());

        assertNull(UuValues.assemblyRecipe("othermod:unknown", tag -> false));
        assertEquals(19, UuValues.allAssemblyRecipes().size());
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
                        "data/c/tags/fluid/creosote.json",
                        "data/forge/tags/fluid/steam.json",
                        "data/forge/tags/fluid/biogas.json",
                        "data/forge/tags/fluid/creosote.json",
                        "data/c/tags/item/small_dusts.json",
                        "data/c/tags/item/dense_plates.json",
                        "data/c/tags/item/gems.json",
                        "data/c/tags/item/nuggets.json",
                        "data/c/tags/item/raw_ores.json",
                        "data/c/tags/item/wrenches.json",
                        "data/ic2/tags/item/alloys.json",
                        "data/ic2/tags/item/recycler_blacklist.json");
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
                        "data/forge/tags/items/blocks.json",
                        "data/forge/tags/items/small_dusts.json",
                        "data/forge/tags/items/dense_plates.json",
                        "data/forge/tags/items/gems.json",
                        "data/forge/tags/items/nuggets.json",
                        "data/forge/tags/items/wrenches.json",
                        "data/ic2/tags/items/alloys.json",
                        "data/ic2/tags/items/recycler_blacklist.json",
                        "data/forge/tags/fluids/steam.json",
                        "data/forge/tags/fluids/biogas.json",
                        "data/forge/tags/fluids/creosote.json",
                        "data/c/tags/fluids/steam.json",
                        "data/c/tags/fluids/biogas.json",
                        "data/c/tags/fluids/creosote.json");
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
                String commonNs = forge ? "forge" : "c";
                String bronzeBoots = readEntry(jar, recipeDir + "bronze_boots.json");
                assertTrue(
                        bronzeBoots.contains(commonNs + ":ingots/bronze"),
                        target + ": bronze_boots must use common bronze ingot tag");
                String insulatedCopper = readEntry(jar, recipeDir + "insulated_copper_cable.json");
                assertTrue(
                        insulatedCopper.contains(commonNs + ":rubbers"),
                        target + ": insulated_copper_cable must use common rubbers tag");
                String smeltTin = readEntry(jar, recipeDir + "tin_ingot_from_raw_tin.json");
                assertTrue(
                        smeltTin.contains(commonNs + ":raw_materials/tin"),
                        target + ": tin smelting must use common raw_materials/tin tag");

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
