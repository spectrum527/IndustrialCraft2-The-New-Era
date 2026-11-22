package com.industrialage.ia2.world;

import com.industrialage.ia2.access.MachinePortCache;
import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

/** Maps persistent world-device state to the native save API. */
abstract class WorldSaveBase extends BlockEntity {
    WorldSaveBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private final MachinePortCache automation = new MachinePortCache(this);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (!isRemoved() && this instanceof WorldMachine node) {
            if (cap == ForgeCapabilities.ITEM_HANDLER)
                return automation.item(side, () -> itemPort(node.inventory, side)).cast();
            if (cap == ForgeCapabilities.FLUID_HANDLER
                    && node.kind == WorldMachineBlock.Kind.CROPMATRON)
                return automation.fluid(side, () -> new WorldFluidAccess.Port(node)).cast();
        }
        return super.getCapability(cap, side);
    }

    private static SidedInvWrapper itemPort(WorldInventory inventory, Direction side) {
        Direction face = side == null ? Direction.UP : side;
        return new SidedInvWrapper(inventory, face) {
            @Override
            public boolean isItemValid(int slot, ItemStack item) {
                return inventory.canPlaceItemThroughFace(slot, item, face);
            }
        };
    }

    /** Refreshes listeners for item and fluid automation on this device. */
    public void invalidateMachinePorts() {
        automation.invalidate();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        invalidateMachinePorts();
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
