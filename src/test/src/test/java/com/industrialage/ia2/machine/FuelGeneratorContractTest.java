package com.industrialage.ia2.machine;

import static org.junit.jupiter.api.Assertions.*;

import com.industrialage.ia2.machine.FuelGenerator.Snapshot;
import java.util.function.IntSupplier;
import org.junit.jupiter.api.Test;

/** Generator contracts from energy/devices.md sections 2 and 5. */
class FuelGeneratorContractTest {
    private static final class Fuel implements IntSupplier, Runnable {
        private final int burnTime;
        private int count;
        private int consumed;

        private Fuel(int burnTime, int count) {
            this.burnTime = burnTime;
            this.count = count;
        }

        public int getAsInt() {
            return count == 0 ? 0 : burnTime;
        }

        public void run() {
            assertTrue(count > 0, "fuel was consumed more than once");
            count--;
            consumed++;
        }
    }

    private static FuelGenerator generator(Fuel fuel) {
        return new FuelGenerator(FuelGenerator.DEFAULT_ENERGY_PER_TICK, fuel, fuel);
    }

    private static void ticks(BlockEntityBase block, int count) {
        for (int i = 0; i < count; i++) {
            block.serverTick();
        }
    }

    @Test
    void real1600TickFuelBurnsFor400TicksAndProduces4000Eu() {
        assertEquals(10, FuelGenerator.DEFAULT_ENERGY_PER_TICK);
        assertEquals(4000, FuelGenerator.CAPACITY);
        assertEquals(32, FuelGenerator.OUTPUT_PACKET_LIMIT);
        var fuel = new Fuel(1600, 1);
        var generator = generator(fuel);
        var block = new BlockEntityBase(generator);
        block.onLoad();
        assertFalse(generator.isActive());
        ticks(block, 399);
        assertEquals(new Snapshot(1, 3990), generator.snapshot());
        assertEquals(1, fuel.consumed);
        assertTrue(generator.isActive());
        block.serverTick();
        assertEquals(new Snapshot(0, 4000), generator.snapshot());
        assertTrue(generator.isActive());
        block.serverTick();
        assertEquals(new Snapshot(0, 4000), generator.snapshot());
        assertFalse(generator.isActive());
        assertEquals(1, fuel.consumed);
    }

    @Test
    void newFuelNeedsOneTickOfSpaceButExistingFuelBurnsEvenWhenFull() {
        var fuel = new Fuel(1600, 1);
        var generator = generator(fuel);
        var block = new BlockEntityBase(generator);
        generator.restore(new Snapshot(0, 3991));
        block.onLoad();
        assertFalse(block.serverTick());
        assertEquals(new Snapshot(0, 3991), generator.snapshot());
        assertEquals(0, fuel.consumed);
        assertFalse(generator.isActive());
        generator.restore(new Snapshot(0, 3990));
        assertTrue(block.serverTick());
        assertEquals(new Snapshot(399, 4000), generator.snapshot());
        assertEquals(1, fuel.consumed);
        assertTrue(block.serverTick());
        assertEquals(new Snapshot(398, 4000), generator.snapshot());
        assertTrue(generator.isActive());

        generator.restore(new Snapshot(10, 4000));
        ticks(block, 10);
        assertEquals(new Snapshot(0, 4000), generator.snapshot());
        assertTrue(generator.isActive());
        assertFalse(block.serverTick());
        assertFalse(generator.isActive());
        assertEquals(1, fuel.consumed);
    }

    @Test
    void zeroOrTooShortFuelIsNotConsumedButSevenTicksProduceOneWorkTick() {
        for (int burnTime : new int[] {0, 3}) {
            var fuel = new Fuel(burnTime, 1);
            var generator = generator(fuel);
            var block = new BlockEntityBase(generator);
            block.onLoad();
            block.serverTick();
            assertEquals(new Snapshot(0, 0), generator.snapshot());
            assertEquals(0, fuel.consumed);
            assertFalse(generator.isActive());
        }
        var fuel = new Fuel(7, 1);
        var generator = generator(fuel);
        var block = new BlockEntityBase(generator);
        block.onLoad();
        block.serverTick();
        assertEquals(new Snapshot(0, 10), generator.snapshot());
        assertEquals(1, fuel.consumed);
        assertTrue(generator.isActive());
        block.serverTick();
        assertFalse(generator.isActive());
    }

    @Test
    void snapshotRestoresFuelAndEnergyWithoutOfflineProgressOrSharedState() {
        var first = generator(new Fuel(1600, 1));
        var firstBlock = new BlockEntityBase(first);
        firstBlock.onLoad();
        ticks(firstBlock, 25);
        var saved = first.snapshot();
        assertEquals(new Snapshot(375, 250), saved);
        firstBlock.onUnload();
        assertFalse(first.isActive());
        ticks(firstBlock, 1000);
        assertEquals(saved, first.snapshot());

        var second = generator(new Fuel(1600, 0));
        second.restore(saved);
        var secondBlock = new BlockEntityBase(second);
        assertFalse(second.isActive());
        secondBlock.onLoad();
        ticks(secondBlock, 17);
        assertEquals(new Snapshot(358, 420), second.snapshot());
        assertEquals(saved, first.snapshot());
        firstBlock.onLoad();
        firstBlock.serverTick();
        assertEquals(new Snapshot(374, 260), first.snapshot());
        assertEquals(new Snapshot(358, 420), second.snapshot());
    }

    @Test
    void losslessExtractionUsesAtMostOneTierOnePacketAndAllowsPartialPackets() {
        var generator = generator(new Fuel(0, 0));
        var block = new BlockEntityBase(generator);
        generator.restore(new Snapshot(0, 100));
        block.onLoad();
        assertEquals(32.0, generator.extractEnergy(100), 0.0);
        assertEquals(new Snapshot(0, 68), generator.snapshot());
        assertEquals(10.0, generator.extractEnergy(10), 0.0);
        assertEquals(new Snapshot(0, 58), generator.snapshot());
        generator.restore(new Snapshot(0, 10));
        assertEquals(10.0, generator.extractEnergy(100), 0.0);
        assertEquals(0.0, generator.extractEnergy(100), 0.0);
        assertEquals(new Snapshot(0, 0), generator.snapshot());
    }
}
