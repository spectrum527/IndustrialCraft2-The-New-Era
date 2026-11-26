package com.industrialage.ia2.world;

import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        writeFields((key, value) -> tag.putDouble(key, value));
        var inventory = savedInventory();
        if (inventory == null) return;
        tag.store(
                "contents",
                ItemStack.OPTIONAL_CODEC.listOf(),
                java.util.stream.IntStream.range(0, inventory.getContainerSize())
                        .mapToObj(inventory::getItem)
                        .toList());
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        readFields(key -> tag.getDoubleOr(key, 0));
        var inventory = savedInventory();
        if (inventory == null) return;
        var stacks =
                tag.read("contents", ItemStack.OPTIONAL_CODEC.listOf())
                        .orElseGet(java.util.List::of);
        for (int i = 0; i < inventory.getContainerSize(); i++)
            inventory.setItem(i, i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (getLevel() instanceof net.minecraft.server.level.ServerLevel server
                && savedInventory() != null) {
            net.minecraft.world.Containers.dropContents(server, pos, savedInventory());
        }
    }
}
