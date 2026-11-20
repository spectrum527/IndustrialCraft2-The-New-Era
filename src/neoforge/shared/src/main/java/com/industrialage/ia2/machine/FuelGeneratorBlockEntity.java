package com.industrialage.ia2.machine;

import com.industrialage.ia2.platform.NeoForgeVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Runs and persists the basic generator using its host's actual fuel slot. */
public abstract class FuelGeneratorBlockEntity
        extends NeoForgeVersion.MachineEntity<FuelGenerator.Snapshot> {
    private final FuelGenerator generator;
    private final BlockEntityBase components;

    /** Uses the host's configured EU per working tick. */
    protected FuelGeneratorBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state, FuelGeneratorSave.FORMAT);
        generator = new FuelGenerator(energyPerTick, this::fuelBurnTime, this::consumeFuel);
        components = new BlockEntityBase(generator);
    }

    @Override
    protected final BlockEntityBase machineComponents() {
        return components;
    }

    /** Returns the current fuel stack, or {@link ItemStack#EMPTY} for a vacant slot. */
    protected abstract ItemStack fuelStack();

    /** Consumes one accepted item from the fuel stack and handles its inventory remainder. */
    protected abstract void consumeFuel();

    /** Returns the remaining working ticks and stored EU. */
    @Override
    public final FuelGenerator.Snapshot savedState() {
        return generator.snapshot();
    }

    /** Reports whether the latest loaded server tick burned fuel. */
    public final boolean isActive() {
        return generator.isActive();
    }

    /** Debits at most one 32 EU packet on the loaded server. The caller controls cadence. */
    public final double drawPacket(double requested) {
        if (!(getLevel() instanceof ServerLevel) || isRemoved() || !components.isLoaded()) {
            return 0;
        }
        double sent = generator.extractEnergy(requested);
        if (sent > 0) {
            setChanged();
        }
        return sent;
    }

    @Override
    protected final void restoreState(FuelGenerator.Snapshot snapshot) {
        generator.restore(snapshot);
    }

    private int fuelBurnTime() {
        ItemStack stack = fuelStack();
        if (stack.isEmpty() || stack.is(Items.LAVA_BUCKET)) {
            return 0;
        }
        return NeoForgeVersion.furnaceBurnTime(stack, getLevel());
    }
}
