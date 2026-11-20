package com.industrialage.ia2.world;

import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeFields((key, value) -> tag.putDouble(key, value));
        var inventory = savedInventory();
        if (inventory == null) return;
        var stacks = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < stacks.size(); i++) stacks.set(i, inventory.getItem(i));
        ContainerHelper.saveAllItems(tag, stacks, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readFields(tag::getDouble);
        var inventory = savedInventory();
        if (inventory == null) return;
        var stacks = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, stacks, registries);
        for (int i = 0; i < stacks.size(); i++) inventory.setItem(i, stacks.get(i));
    }
}
