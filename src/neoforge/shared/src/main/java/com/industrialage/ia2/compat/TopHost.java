package com.industrialage.ia2.compat;

import com.industrialage.ia2.gameplay.CableBlock;
import com.industrialage.ia2.gameplay.PortableGear;
import java.util.function.Function;
import mcjty.theoneprobe.api.ITheOneProbe;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.InterModComms;

final class TopHost {
    private TopHost() {}

    static void enqueue() {
        InterModComms.sendTo(
                "theoneprobe",
                "getTheOneProbe",
                () -> (Function<ITheOneProbe, Void>) TopProbe::register);
    }

    static boolean isCable(BlockState state) {
        return state.getBlock() instanceof CableBlock;
    }

    static boolean electricBoat(Entity entity) {
        var id =
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(
                        entity.getType());
        return id != null
                && id.getNamespace().equals("ic2")
                && id.getPath().equals("electric_boat");
    }

    static ItemStack riderBattery(Entity boat) {
        Entity rider = boat.getControllingPassenger();
        return rider instanceof LivingEntity living
                ? PortableGear.boatPack(living)
                : ItemStack.EMPTY;
    }

    static double stored(ItemStack pack) {
        return PortableGear.stored(pack);
    }

    static double capacity(ItemStack pack) {
        return PortableGear.capacity(pack);
    }
}
