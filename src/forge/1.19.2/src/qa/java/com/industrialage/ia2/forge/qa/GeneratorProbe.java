package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.machine.FuelGenerator;
import com.industrialage.ia2.machine.FuelGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Owns the test inventory and observes the production block entity's native lifecycle. */
final class GeneratorProbe extends FuelGeneratorBlockEntity {
    ItemStack fuel = ItemStack.EMPTY;
    int nativeTicks;
    int workTicks;
    int dirtyNotifications;
    CompoundTag firstLoad;
    int ticksAtLoad;
    FuelGenerator.Snapshot stateAtLoad;
    ItemStack slotAtLoad;

    GeneratorProbe(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, FuelGenerator.DEFAULT_ENERGY_PER_TICK);
    }

    void insert(ItemStack stack) {
        fuel = stack;
        setChanged();
    }

    void observeNativeTick() {
        int before = dirtyNotifications;
        tickMachine();
        if (isActive() != (dirtyNotifications > before)) {
            throw new AssertionError(
                    "Incorrect dirty notification on machine tick at " + getBlockPos());
        }
        nativeTicks++;
        if (isActive()) {
            workTicks++;
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        dirtyNotifications++;
    }

    @Override
    protected ItemStack fuelStack() {
        return fuel;
    }

    @Override
    protected void consumeFuel() {
        fuel.shrink(1);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("qa_slot", fuel.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        fuel = ItemStack.of(tag.getCompound("qa_slot"));
        if (firstLoad == null) {
            firstLoad = tag.copy();
            ticksAtLoad = nativeTicks;
            stateAtLoad = savedState();
            slotAtLoad = fuel.copy();
        }
    }
}
