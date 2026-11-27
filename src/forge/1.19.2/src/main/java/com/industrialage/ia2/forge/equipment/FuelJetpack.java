package com.industrialage.ia2.forge.equipment;

import com.industrialage.ia2.forge.content.ForgeContent;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** The wearable 30-bucket biogas tank. */
final class FuelJetpack extends Item {
    private static final int CAPACITY = 30_000;

    FuelJetpack() {
        super(ForgeContent.itemProperties("jetpack").stacksTo(1));
    }

    private static boolean accepts(FluidStack fluid) {
        if (fluid.isEmpty()) return false;
        var key = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        return key != null
                && WorkshopPlatform.fluidMatches(key.toString(), "ic2:biogas", "forge:biogas");
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag serialized) {
        return new FluidHandlerItemStack(stack, CAPACITY) {
            @Override
            public boolean canFillFluidType(FluidStack fluid) {
                return super.canFillFluidType(fluid) && accepts(fluid);
            }
        };
    }

    static boolean consume(ItemStack pack, boolean hover, boolean creative) {
        var holder = pack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
        if (holder.isEmpty()) return false;
        var tank = holder.orElseThrow();
        var stored = tank.getFluidInTank(0);
        if (!accepts(stored)) return false;
        int demand = hover ? 1 : 2;
        if (tank.drain(demand, FluidAction.SIMULATE).getAmount() != demand) return false;
        return creative || tank.drain(demand, FluidAction.EXECUTE).getAmount() == demand;
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.CHEST;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return volume(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(13D * volume(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xB7CC6E;
    }

    private static int volume(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .map(handler -> handler.getFluidInTank(0).getAmount())
                .orElse(0);
    }
}
