package com.industrialage.ia2.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Connects an instance's ordered machine components to Forge's block-entity lifecycle. */
public abstract class ForgeMachineBlockEntity extends BlockEntity {
    /** Constructs the native block entity for one component group. */
    protected ForgeMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Returns the component group owned by this block entity. */
    protected abstract BlockEntityBase machineComponents();

    private boolean onLogicalServer() {
        if (!(getLevel() instanceof ServerLevel level)) {
            return false;
        }
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException(
                    "Machine accessed outside its server thread at " + getBlockPos());
        }
        return true;
    }

    /** Reports whether this machine can service a request on its current logical server. */
    protected final boolean isLoadedServerMachine() {
        return !isRemoved() && onLogicalServer() && machineComponents().isLoaded();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemoved() && onLogicalServer()) {
            machineComponents().onLoad();
        }
    }

    /** Advances one native block-entity tick and schedules a save if the machine changed. */
    public final void tickMachine() {
        if (isLoadedServerMachine()) {
            var components = machineComponents();
            if (components.serverTick() && components.isLoaded()) {
                setChanged();
            }
        }
    }

    @Override
    public void onChunkUnloaded() {
        boolean server = onLogicalServer();
        try {
            if (server) {
                machineComponents().onUnload();
            }
        } finally {
            super.onChunkUnloaded();
        }
    }

    @Override
    public void setRemoved() {
        boolean server = onLogicalServer();
        try {
            if (server) {
                machineComponents().onUnload();
            }
        } finally {
            super.setRemoved();
        }
    }
}
