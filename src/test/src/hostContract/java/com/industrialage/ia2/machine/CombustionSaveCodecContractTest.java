package com.industrialage.ia2.machine;

import static org.junit.jupiter.api.Assertions.*;

import com.industrialage.ia2.machine.FuelGenerator.Snapshot;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

class CombustionSaveCodecContractTest {
    private static CompoundTag state(Tag fuel, Tag energy) {
        var tag = new CompoundTag();
        if (fuel != null) {
            tag.put("fuel_ticks", fuel);
        }
        if (energy != null) {
            tag.put("stored_energy", energy);
        }
        return tag;
    }

    private static Snapshot decode(Tag tag) {
        return FuelGeneratorSave.FORMAT.codec().parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    private static void assertInvalid(Tag tag) {
        var result = FuelGeneratorSave.FORMAT.codec().parse(NbtOps.INSTANCE, tag);
        assertTrue(result.error().isPresent(), tag.toString());
        assertTrue(result.result().isEmpty(), tag.toString());
        assertThrows(IllegalStateException.class, result::getOrThrow, tag.toString());
    }

    @Test
    void hostDecoderAcceptsAbsentNodeAndRejectsCorruptExistingNodes() {
        assertEquals(
                new Snapshot(0, 0),
                FuelGeneratorSave.FORMAT.decode(Optional.empty(), BlockPos.ZERO));
        var zero =
                assertInstanceOf(
                        CompoundTag.class,
                        FuelGeneratorSave.FORMAT
                                .codec()
                                .encodeStart(NbtOps.INSTANCE, new Snapshot(0, 0))
                                .getOrThrow());
        assertEquals(IntTag.valueOf(0), zero.get("fuel_ticks"));
        assertEquals(DoubleTag.valueOf(0), zero.get("stored_energy"));
        assertEquals(new Snapshot(0, 0), decode(zero));
        var saved = state(IntTag.valueOf(17), DoubleTag.valueOf(42.5));
        assertEquals(new Snapshot(17, 42.5), readPresent(saved));

        var missingField = state(IntTag.valueOf(1), null);
        var original = missingField.copy();
        assertThrows(IllegalStateException.class, () -> readPresent(missingField));
        assertEquals(original, missingField);
        assertThrows(IllegalStateException.class, () -> readPresent(StringTag.valueOf("invalid")));
    }

    private static Snapshot readPresent(Tag value) {
        return FuelGeneratorSave.FORMAT.decode(
                Optional.of(new Dynamic<>(NbtOps.INSTANCE, value)), BlockPos.ZERO);
    }

    @Test
    void integerFuelAndFractionalEnergyRoundTripThroughRealNbtCodec() {
        var input = state(IntTag.valueOf(123), DoubleTag.valueOf(1200.5));
        var snapshot = decode(input);
        assertEquals(new Snapshot(123, 1200.5), snapshot);
        var encoded =
                FuelGeneratorSave.FORMAT
                        .codec()
                        .encodeStart(NbtOps.INSTANCE, snapshot)
                        .getOrThrow();
        var saved = assertInstanceOf(CompoundTag.class, encoded);
        assertEquals(IntTag.valueOf(123), saved.get("fuel_ticks"));
        assertEquals(DoubleTag.valueOf(1200.5), saved.get("stored_energy"));
        assertEquals(snapshot, decode(saved));
        assertEquals(
                new Snapshot(Integer.MAX_VALUE, 4000),
                decode(state(IntTag.valueOf(Integer.MAX_VALUE), DoubleTag.valueOf(4000))));
        assertEquals(
                0.0, decode(state(IntTag.valueOf(0), DoubleTag.valueOf(-0.0))).storedEnergy(), 0.0);
    }

    @Test
    void existingNodeMustContainBothCorrectlyTypedInRangeFields() {
        assertInvalid(StringTag.valueOf("invalid"));
        for (var tag :
                List.of(
                        state(null, null),
                        state(IntTag.valueOf(1), null),
                        state(null, DoubleTag.valueOf(10)),
                        state(StringTag.valueOf("1"), DoubleTag.valueOf(10)),
                        state(IntTag.valueOf(1), StringTag.valueOf("10")),
                        state(DoubleTag.valueOf(1.5), DoubleTag.valueOf(10)),
                        state(IntTag.valueOf(-1), DoubleTag.valueOf(10)),
                        state(LongTag.valueOf((long) Integer.MAX_VALUE + 1), DoubleTag.valueOf(10)),
                        state(DoubleTag.valueOf(Double.NaN), DoubleTag.valueOf(10)),
                        state(IntTag.valueOf(1), DoubleTag.valueOf(-0.01)),
                        state(IntTag.valueOf(1), DoubleTag.valueOf(4000.01)),
                        state(IntTag.valueOf(1), DoubleTag.valueOf(Double.NaN)),
                        state(IntTag.valueOf(1), DoubleTag.valueOf(Double.POSITIVE_INFINITY)))) {
            assertInvalid(tag);
        }
    }
}
