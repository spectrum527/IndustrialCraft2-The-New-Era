package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.machine.NeoForgeMachineBlockEntity.SavedState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

/** Native representation shared by the machine variants. Inventory is stored by the host. */
record MachineSave(
        double energy,
        int fuelTicks,
        int progress,
        String recipe,
        int redstoneMode,
        int duration,
        int fuelCapacity,
        ItemStack completedFuel) {
    private static final Codec<MachineSave> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Codec.doubleRange(0, Double.MAX_VALUE)
                                                    .fieldOf("eu")
                                                    .forGetter(MachineSave::energy),
                                            Codec.intRange(0, Integer.MAX_VALUE)
                                                    .fieldOf("fuel")
                                                    .forGetter(MachineSave::fuelTicks),
                                            Codec.intRange(0, Integer.MAX_VALUE)
                                                    .fieldOf("progress")
                                                    .forGetter(MachineSave::progress),
                                            Codec.STRING
                                                    .fieldOf("recipe")
                                                    .forGetter(MachineSave::recipe),
                                            Codec.intRange(0, 6)
                                                    .optionalFieldOf("redstone_mode", 0)
                                                    .forGetter(MachineSave::redstoneMode),
                                            Codec.intRange(0, Integer.MAX_VALUE)
                                                    .optionalFieldOf("duration", 0)
                                                    .forGetter(MachineSave::duration),
                                            Codec.intRange(0, Integer.MAX_VALUE)
                                                    .optionalFieldOf("fuel_capacity", 0)
                                                    .forGetter(MachineSave::fuelCapacity),
                                            ItemStack.OPTIONAL_CODEC
                                                    .optionalFieldOf("fuel_return", ItemStack.EMPTY)
                                                    .forGetter(MachineSave::completedFuel))
                                    .apply(instance, MachineSave::new));

    static final SavedState<MachineSave> FORMAT =
            new SavedState<>(
                    "machine", CODEC, new MachineSave(0, 0, 0, "", 0, 0, 0, ItemStack.EMPTY));
}
