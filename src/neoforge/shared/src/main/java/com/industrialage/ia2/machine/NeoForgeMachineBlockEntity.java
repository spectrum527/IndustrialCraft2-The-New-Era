package com.industrialage.ia2.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Binds a machine's components to the lifetime of one loaded server-side block entity. */
public abstract class NeoForgeMachineBlockEntity extends BlockEntity {
    /** Constructs the native host for this instance's component group. */
    protected NeoForgeMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Returns the same component group for the lifetime of this block entity. */
    protected abstract BlockEntityBase machineComponents();

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel && !isRemoved()) {
            machineComponents().onLoad();
        }
    }

    /** Runs one native block-entity tick and marks changed component state for saving. */
    public final void tickMachine() {
        if (!(getLevel() instanceof ServerLevel) || isRemoved()) {
            return;
        }
        var group = machineComponents();
        // A component can remove its own block synchronously during the tick.
        if (group.serverTick() && group.isLoaded()) {
            setChanged();
        }
    }

    @Override
    public void onChunkUnloaded() {
        try {
            if (getLevel() instanceof ServerLevel) {
                machineComponents().onUnload();
            }
        } finally {
            super.onChunkUnloaded();
        }
    }

    @Override
    public void setRemoved() {
        try {
            if (getLevel() instanceof ServerLevel) {
                machineComponents().onUnload();
            }
        } finally {
            super.setRemoved();
        }
    }
}
