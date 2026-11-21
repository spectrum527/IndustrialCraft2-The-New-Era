package com.industrialage.ia2.content;

import static org.junit.jupiter.api.Assertions.*;

import com.industrialage.ia2.energy.graph.EnergyGraph;
import com.industrialage.ia2.energy.graph.EnergyGraph.Ports;
import com.industrialage.ia2.energy.graph.EnergyGraph.Position;
import com.industrialage.ia2.machine.FuelGenerator;
import com.industrialage.ia2.platform.CapabilityFacade.Face;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Gameplay accounting from the repaired release observations and the approved M09 correction. */
class IndustrialLineTest {
    @Test
    void disposableBatteriesOnlyWasteEnergyInBuffersSmallerThanOneBattery() {
        var cell = PortableEquipment.SINGLE_USE_BATTERY;
        assertEquals(0, cell.disposableCharge(0, 600, 1));
        assertEquals(1, cell.disposableCharge(1, 600, 1));
        assertEquals(600, cell.disposableCharge(600, 600, 1));
        assertEquals(0, cell.disposableCharge(1_199, 1_200, 1));
        assertEquals(1_200, cell.disposableCharge(1_200, 1_200, 1));
        assertEquals(0, cell.disposableCharge(1_199.99, 40_000, 1));
        assertEquals(1_200, cell.disposableCharge(2_400, 40_000, 4));
        assertEquals(0, cell.disposableCharge(1_200, 40_000, 0));
        assertEquals(0, cell.disposableCharge(Double.NaN, 40_000, 1));
        assertEquals(0, PortableEquipment.RE_BATTERY.disposableCharge(1_200, 40_000, 1));
    }

    @Test
    void twoCoalFinishOnTicks300And600WithOneChargePerTick() {
        var mill = new ProcessingMachine("macerator");
        var coal = ProcessingMachine.recipe("macerator", "minecraft:coal");
        assertEquals(1200, mill.receive(1600));
        int input = 2;
        int output = 0;
        for (int tick = 1; tick <= 600; tick++) {
            double before = mill.energy();
            boolean finished = mill.tick(coal, input, 64 - output);
            assertEquals(2, before - mill.energy(), 0.00001);
            assertEquals(tick % 300 == 0, finished);
            if (finished) {
                input -= coal.inputCount();
                output += coal.outputCount();
            }
            assertEquals(tick % 300, mill.progress());
            if (tick == 299) {
                var reloaded = new ProcessingMachine("macerator");
                reloaded.restore(mill.energy(), mill.progress(), mill.recipeKey());
                mill = reloaded;
            }
        }
        assertEquals(0, input);
        assertEquals(2, output);
        assertEquals(0, mill.energy());
        assertFalse(mill.active());
    }

    @Test
    void invalidInputPartialSpacePowerLossAndRecipeChangesAreDistinguished() {
        var mill = new ProcessingMachine("macerator");
        var block = ProcessingMachine.recipe("macerator", "minecraft:coal_block");
        assertEquals(9, block.outputCount());
        mill.receive(200);
        assertFalse(mill.tick(block, 1, 8));
        assertEquals(200, mill.energy());
        for (int i = 0; i < 150; i++) {
            assertFalse(mill.tick(block, 1, 9));
        }
        assertEquals(100, mill.progress());
        assertEquals(0, mill.energy());
        var iron = ProcessingMachine.recipe("macerator", "minecraft:iron_ingot");
        mill.receive(400);
        assertFalse(mill.tick(iron, 1, 1));
        assertEquals(1, mill.progress());
        assertFalse(mill.tick(iron, 0, 1));
        assertEquals(0, mill.progress());
        assertEquals(398, mill.energy());
    }

    @Test
    void measuredCapacitiesAndSuccessfulOperationCostsMatchTheReference() {
        var melt = new ProcessingMachine("electric_furnace");
        var rawIron =
                new ProcessingMachine.Recipe("minecraft:raw_iron", 1, "minecraft:iron_ingot", 1);
        assertEquals(600, melt.receive(1200));
        melt.restore(1200, 0, "");
        assertEquals(0, melt.receive(10));
        for (int i = 1; i <= 100; i++) {
            assertEquals(i == 100, melt.tick(rawIron, 1, 1));
        }
        assertEquals(900, melt.energy());
        for (String path : List.of("compressor", "extractor")) {
            var machine = new ProcessingMachine(path);
            var r =
                    ProcessingMachine.recipe(
                            path, path.equals("extractor") ? "ic2:resin" : "minecraft:snowball");
            assertEquals(1200, machine.receive(2000));
            assertFalse(machine.tick(r, r.inputCount() - 1, 64));
            assertFalse(machine.tick(r, r.inputCount(), r.outputCount() - 1));
            assertEquals(1200, machine.energy());
            for (int i = 1; i <= 300; i++) {
                assertEquals(i == 300, machine.tick(r, r.inputCount(), r.outputCount()));
            }
            assertEquals(600, machine.energy());
        }
    }

    @Test
    void graphTakesLowerLossBranchAndNeverRelaysThroughADevice() {
        var graph = new EnergyGraph();
        Set<Face> all = Set.of(Face.values());
        var wire = new Ports(all, all, true);
        var source = new Position(0, 0, 0);
        var sink = new Position(2, 0, 0);
        graph.put(source, new Ports(Set.of(), all, false));
        graph.put(new Position(1, 0, 0), wire);
        for (int x = 0; x < 3; x++) {
            graph.put(new Position(x, 0, 1), wire);
        }
        graph.put(sink, new Ports(all, all, false));
        graph.put(new Position(3, 0, 0), new Ports(all, Set.of(), false));
        var routes = graph.routes(source, p -> p.z() == 0 ? 0.8 : 0.025);
        assertEquals(1, routes.size());
        assertEquals(sink, routes.get(0).sink());
        assertEquals(0.077, routes.get(0).rawLoss(), 1e-12);
        graph.remove(new Position(1, 0, 1));
        assertEquals(0.802, graph.routes(source, p -> 0.8).get(0).rawLoss(), 1e-12);
        graph.remove(new Position(1, 0, 0));
        assertTrue(graph.routes(source, p -> 0.8).isEmpty());
    }

    @Test
    void sourcePaysOnlyTheAcceptedEnergyAndOneWholePathLoss() {
        var box = PowerGrid.storage("batbox");
        assertEquals(0, box.offer(31.999));
        assertEquals(32, box.offer(100));
        assertEquals(0, PowerGrid.transfer(32, 1.002, true, q -> 0));
        assertEquals(11, PowerGrid.transfer(32, 1.002, true, q -> Math.min(10, q)));
        assertEquals(10.202, PowerGrid.transfer(32, 0.202, false, q -> Math.min(10, q)), 1e-9);
        assertEquals(0, PowerGrid.transfer(1, 1.002, true, q -> fail("no usable energy")));
        var generator = new FuelGenerator(10, () -> 1600, () -> {});
        for (int i = 0; i < 20; i++) {
            generator.serverTick();
        }
        assertEquals(32, generator.extractEnergy(64));
        assertEquals(64, generator.extractLocalEnergy(64));
        assertEquals(104, generator.snapshot().storedEnergy());
    }
}
