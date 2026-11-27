package com.industrialage.ia2.forge.equipment;

import com.industrialage.ia2.forge.content.ForgeContent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
    private static final ResourceLocation BIOGAS =
            ResourceLocation.fromNamespaceAndPath("ic2", "biogas");

    FuelJetpack() {
        super(ForgeContent.itemProperties("jetpack").stacksTo(1));
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag serialized) {
        return new FluidHandlerItemStack(stack, CAPACITY) {
            @Override
            public boolean canFillFluidType(FluidStack fluid) {
                return super.canFillFluidType(fluid)
                        && BIOGAS.equals(ForgeRegistries.FLUIDS.getKey(fluid.getFluid()));
            }
        };
    }

    static boolean consume(ItemStack pack, boolean hover, boolean creative) {
        var holder = pack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
        if (holder.isEmpty()) return false;
        var tank = holder.orElseThrow();
        var biogas = ForgeRegistries.FLUIDS.getValue(BIOGAS);
        if (biogas == null) return false;
        int demand = hover ? 1 : 2;
        var cost = new FluidStack(biogas, demand);
        if (tank.drain(cost, FluidAction.SIMULATE).getAmount() != demand) return false;
        return creative || tank.drain(cost, FluidAction.EXECUTE).getAmount() == demand;
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
