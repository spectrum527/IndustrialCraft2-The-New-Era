package com.industrialage.ia2.logistics;

import com.industrialage.ia2.content.TransportSpec.Kind;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

abstract class TransportSaveBase extends BlockEntity {
    private static final int SIDE_MASK = (1 << Direction.values().length) - 1;
    private final Map<Direction, LazyOptional<IItemHandler>> itemPorts = new HashMap<>();
    private final Map<Direction, LazyOptional<IFluidHandler>> fluidPorts = new HashMap<>();

    TransportSaveBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!(this instanceof TransportNode node)) {
            return;
        }
        if (!node.cargo.isEmpty()) {
            tag.put("ic2_cargo", node.cargo.save(new CompoundTag()));
        }
        if (node.liquid != null && node.amount > 0) {
            tag.put("ic2_fluid", node.liquid.stack(node.amount).writeToNBT(new CompoundTag()));
        }
        for (int i = 0; i < node.upgrades.getContainerSize(); i++)
            if (!node.upgrades.getItem(i).isEmpty())
                tag.put("upgrade_" + i, node.upgrades.getItem(i).save(new CompoundTag()));
        tag.putInt("ic2_closed", node.closed);
        tag.putInt("ic2_pump_lv", node.lv);
        tag.putInt("ic2_pump_mv", node.mv);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (!(this instanceof TransportNode node)) {
            return;
        }
        for (int i = 0; i < node.upgrades.getContainerSize(); i++)
            node.upgrades.setItem(i, ItemStack.of(tag.getCompound("upgrade_" + i)));
        node.cargo =
                node.spec.kind() == Kind.ITEM_PIPE
                        ? ItemStack.of(tag.getCompound("ic2_cargo"))
                        : ItemStack.EMPTY;
        var fluid =
                node.spec.kind() == Kind.ITEM_PIPE
                        ? FluidStack.EMPTY
                        : FluidStack.loadFluidStackFromNBT(tag.getCompound("ic2_fluid"));
        if (node.cargo.getCount() > Math.min(node.spec.capacity(), node.cargo.getMaxStackSize())
                || fluid.getAmount() > node.spec.capacity()) {
            throw new IllegalStateException(
                    "Transport inventory exceeds capacity at " + getBlockPos());
        }
        node.liquid = fluid.isEmpty() ? null : new FluidKey(fluid);
        node.amount = fluid.getAmount();
        node.closed = tag.getInt("ic2_closed") & SIDE_MASK;
        node.lv = tag.getInt("ic2_pump_lv") & SIDE_MASK;
        node.mv = tag.getInt("ic2_pump_mv") & SIDE_MASK & ~node.lv;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (!isRemoved() && this instanceof TransportNode node) {
            if (capability == ForgeCapabilities.ITEM_HANDLER
                    && node.spec.kind() == Kind.ITEM_PIPE) {
                return itemPorts
                        .computeIfAbsent(
                                side,
                                face ->
                                        LazyOptional.of(
                                                () -> new TransportPlatform.ItemPort(node, face)))
                        .cast();
            }
            if (capability == ForgeCapabilities.FLUID_HANDLER
                    && node.spec.kind() != Kind.ITEM_PIPE) {
                return fluidPorts
                        .computeIfAbsent(
                                side,
                                face ->
                                        LazyOptional.of(
                                                () -> new TransportPlatform.FluidPort(node, face)))
                        .cast();
            }
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemPorts.values().forEach(LazyOptional::invalidate);
        fluidPorts.values().forEach(LazyOptional::invalidate);
        itemPorts.clear();
        fluidPorts.clear();
    }
}
