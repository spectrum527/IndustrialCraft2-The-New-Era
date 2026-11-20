package com.industrialage.ia2.forge.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

abstract class InventoryDevice extends BlockEntity implements ForgeDevice, ForgePower.Endpoint {
    private final MachineInventory inventory;

    InventoryDevice(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineKind kind) {
        super(type, pos, state);
        inventory = new MachineInventory(kind, this);
    }

    @Override
    public MachineInventory inventory() {
        return inventory;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        ForgePower.attach(this);
    }

    @Override
    public void onChunkUnloaded() {
        ForgePower.detach(this);
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        ForgePower.detach(this);
        super.setRemoved();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventory.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        inventory.revive();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (!isRemoved() && capability == ForgeCapabilities.ITEM_HANDLER) {
            return inventory.handler(side).cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        inventory.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.load(tag);
    }
}
