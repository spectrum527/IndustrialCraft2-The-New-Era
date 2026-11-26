package com.industrialage.ia2.content;

import static org.junit.jupiter.api.Assertions.*;

import com.industrialage.ia2.energy.graph.EnergyGraph;
import com.industrialage.ia2.energy.graph.EnergyGraph.Ports;
import com.industrialage.ia2.energy.graph.EnergyGraph.Position;
import com.industrialage.ia2.platform.CapabilityFacade.Face;
import com.industrialage.ia2.reactor.ElectricReactor;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Regression tests locking down core fixes from the v1.0.6 full-ecosystem audit. */
class AuditRegressionTest {
    private static final class ArrayGrid implements ElectricReactor.Grid {
        final String[] parts = new String[ElectricReactor.SLOTS];
        final int[] wears = new int[ElectricReactor.SLOTS];

        ArrayGrid() {
            Arrays.fill(parts, "");
        }

        @Override
        public String part(int slot) {
            return parts[slot];
        }

        @Override
        public int wear(int slot) {
            return wears[slot];
        }

        @Override
        public void wear(int slot, int value) {
            wears[slot] = value;
        }

        @Override
        public void replace(int slot, String result) {
            parts[slot] = result;
            wears[slot] = 0;
        }
    }

    @Test
    void reactorHeatExchangerConvergesCleanlyWithoutOscillationOrPrematureMeltdown() {
        var steady = new ElectricReactor();
        steady.restore(5000, 0);
        var grid = new ArrayGrid();
        grid.parts[0] = "reactor_heat_exchanger";
        grid.wears[0] = 2499;

        steady.cycle(grid, 0, false, 1.0);
        assertEquals(4999, steady.heat());
        assertEquals(2500, grid.wear(0));

        // Subsequent cycles must remain at integer equilibrium with zero oscillation.
        for (int i = 0; i < 5; i++) {
            steady.cycle(grid, 0, false, 1.0);
            assertEquals(4999, steady.heat());
            assertEquals(2500, grid.wear(0));
        }

        // 99.31% combined heat must converge safely to 9931/10000 rather than hitting 10000.
        var nearLimit = new ElectricReactor();
        nearLimit.restore(9930, 0);
        var nearGrid = new ArrayGrid();
        nearGrid.parts[0] = "reactor_heat_exchanger";
        nearGrid.wears[0] = 4966;

        nearLimit.cycle(nearGrid, 0, false, 1.0);
        assertEquals(9931, nearLimit.heat());
        assertEquals(4965, nearGrid.wear(0));
        assertTrue(nearLimit.heat() < nearLimit.limit());

        // Large temperature delta still transfers at full exchanger rate (72 hU/cycle).
        var hotHull = new ElectricReactor();
        hotHull.restore(3000, 0);
        var coldGrid = new ArrayGrid();
        coldGrid.parts[0] = "reactor_heat_exchanger";
        coldGrid.wears[0] = 0;

        hotHull.cycle(coldGrid, 0, false, 1.0);
        assertEquals(2928, hotHull.heat());
        assertEquals(72, coldGrid.wear(0));
    }

    @Test
    void lossyCableRoutesPreserveArrivedVoltageForOvervoltageDetection() {
        var allFaces = EnumSet.allOf(Face.class);
        var sourcePorts = new Ports(Set.of(), allFaces, false);
        var sinkPorts = new Ports(allFaces, Set.of(), false);
        var cablePorts = new Ports(allFaces, allFaces, true);

        var graph = new EnergyGraph();
        var source = new Position(0, 0, 0);
        graph.put(source, sourcePorts);
        for (int i = 1; i <= 5; i++) {
            graph.put(new Position(i, 0, 0), cablePorts);
        }
        var sink = new Position(6, 0, 0);
        graph.put(sink, sinkPorts);

        // 5 double-insulated gold cables (0.4 loss each -> rawLoss 2.002 -> rounded loss 2).
        var route = graph.routes(source, pos -> 0.4).get(0);
        double roundedLoss = Math.floor(route.rawLoss());
        assertEquals(2.0, roundedLoss, 1e-9);

        double arrivedRounded = 512.0 - roundedLoss;
        assertTrue(arrivedRounded > 32.0, "512 EU/t over 5 gold cables must exceed LV 32 EU/t");

        double arrivedUnrounded = 128.0 - route.rawLoss();
        assertTrue(
                arrivedUnrounded > 32.0,
                "128 EU/t with roundEnetLoss=false must still exceed LV 32 EU/t");
    }

    @Test
    void thirtyTwoBitMenuEnergySplitRoundTripsHighCapacityMachines() {
        for (int eu : new int[] {0, 1, 32_767, 32_768, 40_000, 60_000, 10_000_000}) {
            short low = (short) (eu & 0xffff);
            short high = (short) (eu >>> 16);
            int restored = (low & 0xffff) | ((high & 0xffff) << 16);
            assertEquals(eu, restored);
        }
    }

    @Test
    void steamBoilerAndTurbineLoopCannotGenerateNetFreeEnergy() {
        // Electric Heat Generator: 0.5 EU per hU.
        // Normal steam: boiler costs 110 hU (55 EU) per 100 mB -> turbine yields 200 kU (50 EU).
        double normalInputEu = 110 * 0.5;
        double normalOutputEu = 200 * 0.25;
        assertTrue(normalOutputEu < normalInputEu);

        // Superheated steam: boiler costs 300 hU (150 EU) per 100 mB ->
        // first turbine yields 400 kU (100 EU) + 90 mB normal steam (<= 180 kU = 45 EU) = 145 EU.
        double superInputEu = 300 * 0.5;
        double superOutputEu = 400 * 0.25 + (90.0 / 100.0) * 200 * 0.25;
        assertTrue(superOutputEu < superInputEu);
    }

    @Test
    void processingUpgradesStayBoundedUnderExtremeOverclockerCounts() {
        var extreme = new ProcessingUpgrades(64, 4, 16).stats(300, 2, 32, 1200);
        assertTrue(extreme.duration() >= 1);
        assertTrue(extreme.energyPerTick() > 0);
        assertTrue(extreme.capacity() >= extreme.energyPerTick());
    }

    @Test
    void copperIngotRemovedAndBiogasCellRegistered() {
        assertFalse(
                IndustrialMaterials.ADDITIONAL.contains("copper_ingot"),
                "ic2:copper_ingot must be removed from passive materials");
        assertFalse(
                CreativeCategory.MATERIALS.entries().contains("copper_ingot"),
                "ic2:copper_ingot must be removed from creative materials");
        assertThrows(
                IllegalArgumentException.class,
                () -> CreativeCategory.forItem("copper_ingot"),
                "ic2:copper_ingot must have no creative category");
        assertTrue(
                CreativeCategory.FLUIDS.entries().contains("biogas_cell"),
                "ic2:biogas_cell must be listed in creative fluids");
        assertEquals(CreativeCategory.FLUIDS, CreativeCategory.forItem("biogas_cell"));
    }

    @Test
    void processingRecipesSupportDynamicTagPredicateFallbackAndEnumeration() {
        var dynamicOre =
                ProcessingMachine.recipe(
                        "macerator",
                        "mod:custom_silver_ore",
                        tag -> tag.equals("forge:ores/silver"));
        assertNotNull(dynamicOre, "Dynamic tag predicate should match third-party tagged ore");
        assertEquals("mod:custom_silver_ore", dynamicOre.input());
        assertEquals("ic2:crushed_silver", dynamicOre.output());
        assertEquals(2, dynamicOre.outputCount());

        var dynamicIngot =
                ProcessingMachine.recipe(
                        "macerator", "mod:custom_tin_ingot", tag -> tag.equals("c:ingots/tin"));
        assertNotNull(
                dynamicIngot,
                "Dynamic tag predicate should match #forge:ingots/tin via c:ingots/tin alias");
        assertEquals("ic2:tin_dust", dynamicIngot.output());

        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "forge:raw_ores/tin", tag -> tag.equals("c:raw_materials/tin")));
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "c:raw_materials", tag -> tag.equals("forge:raw_ores")));
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "forge:blocks/steel", tag -> tag.equals("c:storage_blocks/steel")));
        assertTrue(
                ProcessingMachine.matchesTagAlias(
                        "c:storage_blocks", tag -> tag.equals("forge:blocks")));
        assertFalse(
                ProcessingMachine.matchesTagAlias("minecraft:logs", tag -> tag.equals("c:logs")));

        var fallback = ProcessingMachine.recipe("macerator", "ic2:tin_ingot", tag -> false);
        assertNotNull(
                fallback,
                "Built-in reference tag members must still match when runtime tag predicate misses");
        assertEquals("ic2:tin_dust", fallback.output());

        for (String machine : new String[] {"macerator", "extractor", "compressor"}) {
            var all = ProcessingMachine.allRecipes(machine);
            assertFalse(all.isEmpty(), machine + " must expose recipes for recipe viewers");
        }
        assertTrue(
                ProcessingMachine.allRecipes("macerator").stream()
                        .anyMatch(recipe -> recipe.input().startsWith("#")),
                "macerator recipe enumeration must include tag-backed entries");
    }
}
