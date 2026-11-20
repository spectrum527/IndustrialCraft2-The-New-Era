package com.industrialage.ia2.machine;

import static com.industrialage.ia2.machine.FuelGenerator.CAPACITY;

import com.industrialage.ia2.machine.NeoForgeMachineBlockEntity.SavedState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** The generator payload stored with the block entity, independent of the native save API. */
final class FuelGeneratorSave {
    private FuelGeneratorSave() {}

    private static final Codec<Integer> FUEL_TICKS_CODEC =
            new PrimitiveCodec<>() {
                @Override
                public <T> DataResult<Integer> read(DynamicOps<T> ops, T input) {
                    return ops.getNumberValue(input)
                            .flatMap(
                                    number -> {
                                        int ticks = number.intValue();
                                        if (ticks < 0 || ticks != number.doubleValue()) {
                                            return DataResult.error(
                                                    () -> "Invalid fuel tick count: " + number);
                                        }
                                        return DataResult.success(ticks);
                                    });
                }

                @Override
                public <T> T write(DynamicOps<T> ops, Integer ticks) {
                    return ops.createInt(ticks);
                }
            };

    private static final Codec<FuelGenerator.Snapshot> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            FUEL_TICKS_CODEC
                                                    .fieldOf("fuel_ticks")
                                                    .forGetter(FuelGenerator.Snapshot::fuelTicks),
                                            Codec.doubleRange(-0.0, CAPACITY)
                                                    .fieldOf("stored_energy")
                                                    .forGetter(
                                                            FuelGenerator.Snapshot::storedEnergy))
                                    .apply(instance, FuelGenerator.Snapshot::new));

    static final SavedState<FuelGenerator.Snapshot> FORMAT =
            new SavedState<>("generator", CODEC, new FuelGenerator.Snapshot(0, 0));
}
