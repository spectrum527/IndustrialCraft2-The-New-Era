package com.industrialage.ia2.industrial;

import com.industrialage.ia2.access.MachinePortCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.wrapper.InvWrapper;

/** Preserves native stacks on world save and in portable box drops. */
abstract class BoxHost extends BlockEntity {
    BoxHost(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private NonNullList<ItemStack> list() {
        var n = (BoxNode) this;
        var list = NonNullList.withSize(n.kind.slots, ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) list.set(i, n.items.getItem(i));
        return list;
    }

    private void restore(NonNullList<ItemStack> list) {
        var n = (BoxNode) this;
        for (int i = 0; i < list.size(); i++) n.items.setItem(i, list.get(i));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        var node = (BoxNode) this;
        ContainerHelper.saveAllItems(tag, list());
        if (node.owner != null) tag.putUUID("owner", node.owner);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        var node = (BoxNode) this;
        var items = NonNullList.withSize(node.kind.slots, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        restore(items);
        try {
            node.owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        } catch (IllegalArgumentException malformed) {
            node.owner = null;
        }
    }

    private final MachinePortCache automation = new MachinePortCache(this);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (!isRemoved()
                && cap == ForgeCapabilities.ITEM_HANDLER
                && ((BoxNode) this).kind.automatable())
            return automation.item(side, () -> new InvWrapper(((BoxNode) this).items)).cast();
        return super.getCapability(cap, side);
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
}
