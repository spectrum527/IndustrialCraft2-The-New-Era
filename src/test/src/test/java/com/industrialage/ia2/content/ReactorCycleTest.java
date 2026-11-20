package com.industrialage.ia2.content;

import static org.junit.jupiter.api.Assertions.*;

import com.industrialage.ia2.reactor.ElectricReactor;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Measured 1.2.4 low-temperature reactor and component-life boundaries. */
class ReactorCycleTest {
    static final class Grid implements ElectricReactor.Grid {
        final String[] slots = new String[54];
        final int[] use = new int[54];

        Grid() {
            Arrays.fill(slots, "");
        }

        public String part(int s) {
            return slots[s];
        }

        public int wear(int s) {
            return use[s];
        }

        public void wear(int s, int n) {
            use[s] = n;
        }

        public void replace(int s, String part) {
            slots[s] = part;
            use[s] = 0;
        }
    }

    @Test
    void chamberWidthSelectsColumnsWithoutJoiningDifferentRows() {
        assertTrue(ElectricReactor.exposed(2, 0));
        assertFalse(ElectricReactor.exposed(3, 0));
        assertTrue(ElectricReactor.exposed(53, 6));
        var machine = new ElectricReactor();
        var grid = new Grid();
        grid.slots[2] = grid.slots[9] = "uranium_fuel_rod";
        machine.cycle(grid, 0, true, 1);
        assertEquals(10, machine.output());
        assertEquals(8, machine.heat());
        grid.slots[2] = "";
        grid.slots[0] = "uranium_fuel_rod";
        machine.cycle(grid, 0, true, 1);
        assertEquals(20, machine.output());
        assertEquals(32, machine.heat());
        machine.cycle(grid, 0, false, 1);
        assertEquals(0, machine.output());
        assertEquals(32, machine.heat());
    }

    @Test
    void expiredReflectorStillHandlesTheLastHeatPulseAndSpentRodProducesItsLastPacket() {
        var machine = new ElectricReactor();
        var grid = new Grid();
        grid.slots[0] = "uranium_fuel_rod";
        grid.slots[1] = "neutron_reflector";
        grid.use[1] = 29_998;
        grid.use[0] = 19_998;
        machine.cycle(grid, 0, true, 1);
        assertEquals(12, machine.heat());
        assertEquals(10, machine.output());
        assertEquals(29_999, grid.wear(1));
        machine.cycle(grid, 0, true, 1);
        assertEquals(24, machine.heat());
        assertEquals(5, machine.output());
        assertEquals("", grid.part(1));
        assertEquals("depleted_uranium_fuel_rod", grid.part(0));
        machine.cycle(grid, 0, true, 1);
        assertEquals(0, machine.output());
    }

    @Test
    void reactorAndOverclockedVentsCoolUnpoweredHullAtTheirMeasuredRates() {
        var machine = new ElectricReactor();
        var grid = new Grid();
        machine.restore(100, 0);
        grid.slots[0] = "reactor_heat_vent";
        machine.cycle(grid, 0, false, 1);
        assertEquals(95, machine.heat());
        machine.restore(100, 0);
        grid.slots[0] = "overclocked_heat_vent";
        machine.cycle(grid, 0, false, 1);
        assertEquals(64, machine.heat());
        assertEquals(16, grid.wear(0));
        machine.cycle(grid, 0, false, 1);
        assertEquals(28, machine.heat());
        assertEquals(32, grid.wear(0));
    }
}
