package com.industrialage.ia2.world;

import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Maps persistent world-device state to the native save API. */
abstract class WorldSaveBase extends BlockEntity {
    WorldSaveBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private final net.minecraftforge.common.util.LazyOptional<WorldFluidAccess.Port> inlet =
            net.minecraftforge.common.util.LazyOptional.of(
                    () -> new WorldFluidAccess.Port((WorldMachine) this));
    private final java.util.Map<
                    net.minecraft.core.Direction,
                    net.minecraftforge.common.util.LazyOptional<
                            net.minecraftforge.items.wrapper.SidedInvWrapper>>
            sides = new java.util.EnumMap<>(net.minecraft.core.Direction.class);

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> capability,
            net.minecraft.core.Direction side) {
        if (!isRemoved() && this instanceof WorldMachine machine) {
            if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER
                    && machine.kind == WorldMachineBlock.Kind.CROPMATRON) return inlet.cast();
            if (capability
                    == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
                var face = side == null ? net.minecraft.core.Direction.UP : side;
                return sides.computeIfAbsent(
                                face,
                                direction ->
                                        net.minecraftforge.common.util.LazyOptional.of(
                                                () ->
                                                        new net.minecraftforge.items.wrapper
                                                                .SidedInvWrapper(
                                                                machine.inventory, direction)))
                        .cast();
            }
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inlet.invalidate();
        sides.values().forEach(net.minecraftforge.common.util.LazyOptional::invalidate);
        sides.clear();
    }

    abstract void writeFields(BiConsumer<String, Double> state);

    abstract void readFields(ToDoubleFunction<String> state);

    SimpleContainer savedInventory() {
        return null;
    }

    static void number(BiConsumer<String, Double> output, String name, double value) {
        output.accept(name, value);
    }

    static int integer(ToDoubleFunction<String> input, String name) {
        return Math.max(0, (int) input.applyAsDouble(name));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeFields((key, value) -> tag.putDouble(key, value));
        var inventory = savedInventory();
        if (inventory == null) return;
        var stacks = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < stacks.size(); i++) stacks.set(i, inventory.getItem(i));
        ContainerHelper.saveAllItems(tag, stacks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        readFields(tag::getDouble);
        var inventory = savedInventory();
        if (inventory == null) return;
        var stacks = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, stacks);
        for (int i = 0; i < stacks.size(); i++) inventory.setItem(i, stacks.get(i));
    }
}
