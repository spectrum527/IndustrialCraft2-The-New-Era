package com.industrialage.ia2.gameplay;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** A rechargeable flight tank uses biogas exclusively. */
final class FuelJetpack extends Item {
    static final int CAPACITY = 30_000;
    static DeferredHolder<Item, FuelJetpack> item;
    static DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> fluid;

    FuelJetpack(Properties properties) {
        super(properties);
    }

    static void install(
            IEventBus bus,
            DeferredRegister.Items items,
            DeferredRegister<DataComponentType<?>> components) {
        item = items.registerItem("jetpack", properties -> new FuelJetpack(properties.stacksTo(1)));
        fluid =
                components.register(
                        "jetpack_fuel",
                        () ->
                                DataComponentType.<SimpleFluidContent>builder()
                                        .persistent(SimpleFluidContent.CODEC)
                                        .networkSynchronized(SimpleFluidContent.STREAM_CODEC)
                                        .build());
        NativeGameplay.registerFlightFuel(bus);
    }

    static boolean accepts(Fluid fuel) {
        return BuiltInRegistries.FLUID.getKey(fuel).toString().equals("ic2:biogas");
    }

    static boolean consume(ItemStack stack, boolean hover, boolean creative) {
        int cost = hover ? 1 : 2;
        var stored = stack.get(fluid.get());
        if (stored == null || stored.getAmount() < cost || !accepts(stored.getFluid()))
            return false;
        if (!creative) {
            if (stored.getAmount() == cost) stack.remove(fluid.get());
            else
                stack.set(
                        fluid.get(),
                        SimpleFluidContent.copyOf(
                                stored.copy().copyWithAmount(stored.getAmount() - cost)));
        }
        return true;
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.CHEST;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(fluid.get());
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        var stored = stack.get(fluid.get());
        return stored == null ? 0 : (int) Math.round(13D * stored.getAmount() / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xB7CC6E;
    }
}
