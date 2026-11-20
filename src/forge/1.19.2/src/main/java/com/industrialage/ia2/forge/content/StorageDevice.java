package com.industrialage.ia2.forge.content;

import com.industrialage.ia2.content.PowerGrid;
import com.industrialage.ia2.energy.graph.PacketBudget;
import com.industrialage.ia2.forge.equipment.ForgeGear;
import com.industrialage.ia2.power.WorldPowerStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

final class StorageDevice extends InventoryDevice implements WorldPowerStorage {
    static final String STORED_ENERGY = "ic2_energy";
    private final PowerGrid.Storage storage;
    private double energy;
    private int mode;
    private int lastSignal;
    private final boolean chargepad;
    private boolean charging;

    StorageDevice(BlockPos pos, BlockState state) {
        this(pos, state, ((MachineBlock) state.getBlock()).kind);
    }

    private StorageDevice(BlockPos pos, BlockState state, MachineKind kind) {
        super(ForgeContent.storageType(kind), pos, state, kind);
        storage = PowerGrid.storage(kind.storagePath());
        chargepad = kind.chargingPad();
    }

    void tick() {
        var charge = inventory().stack(MachineKind.Slot.CHARGE);
        var discharge = inventory().stack(MachineKind.Slot.DISCHARGE);
        draw(ForgeGear.charge(charge, energy, storage.tier()));
        receive(ForgeGear.discharge(discharge, capacity() - energy, storage.tier()));
        charging = chargepad && chargePlayers();
        updateSignal();
    }

    private boolean chargePlayers() {
        if (energy <= 0 || !(level instanceof ServerLevel server)) return false;
        var feet = worldPosition.above();
        var players =
                server.getEntitiesOfClass(
                        ServerPlayer.class,
                        new AABB(feet),
                        player -> player.isOnGround() && player.blockPosition().equals(feet));
        boolean transferred = false;
        for (var player : players) {
            var equipment =
                    java.util.List.of(
                            player.getItemBySlot(EquipmentSlot.CHEST),
                            player.getMainHandItem(),
                            player.getOffhandItem());
            for (var stack : equipment) {
                double accepted = ForgeGear.charge(stack, energy, storage.tier());
                transferred |= accepted > 0 && draw(accepted) > 0;
            }
        }
        return transferred;
    }

    @Override
    public boolean active() {
        return charging;
    }

    void retainIn(ItemStack stack) {
        double ratio =
                ForgePower.setting("balance/energyRetainedInStorageBlockDrops", Double.class, 0.8);
        if (energy > 0 && ratio > 0) {
            stack.getOrCreateTag().putDouble(STORED_ENERGY, energy * ratio);
        }
    }

    void cycleMode() {
        mode = (mode + 1) % 7;
        setChanged();
        updateSignal();
    }

    int mode() {
        return mode;
    }

    int redstoneSignal() {
        double packet = PacketBudget.packetSize(storage.tier());
        boolean emit =
                switch (mode) {
                    case 1 -> energy >= capacity() - 20 * packet;
                    case 2 -> packet < energy && energy < capacity() - packet;
                    case 3 -> energy < capacity() - packet;
                    case 4 -> energy < packet;
                    default -> false;
                };
        return emit ? 15 : 0;
    }

    private void updateSignal() {
        int signal = redstoneSignal();
        if (signal != lastSignal) {
            lastSignal = signal;
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
        }
    }

    @Override
    public double energy() {
        return energy;
    }

    @Override
    public double capacity() {
        return storage.capacity();
    }

    @Override
    public boolean accepts(Direction face) {
        return !emits(face);
    }

    @Override
    public boolean emits(Direction face) {
        return face == getBlockState().getValue(MachineBlock.OUTPUT_FACE);
    }

    @Override
    public double offers() {
        boolean blocked =
                mode == 5
                        || (mode == 6
                                && energy
                                        <= capacity()
                                                - 20 * PacketBudget.packetSize(storage.tier()));
        return blocked && level.hasNeighborSignal(getBlockPos()) ? 0 : storage.offer(energy);
    }

    @Override
    public int availableStoredEnergy() {
        return (int) energy;
    }

    @Override
    public boolean exchangeStoredEnergy(int delta, boolean simulate) {
        if (energy + delta < 0 || energy + delta > capacity()) {
            return false;
        }
        if (!simulate) {
            if (delta < 0) draw(-delta);
            if (delta > 0) receive(delta);
        }
        return true;
    }

    @Override
    public double draw(double eu) {
        double drawn = Math.min(eu, energy);
        if (drawn > 0) {
            energy -= drawn;
            setChanged();
        }
        return drawn;
    }

    @Override
    public double inputRating() {
        return PacketBudget.packetSize(storage.tier());
    }

    @Override
    public double receive(double eu) {
        if (!Double.isFinite(eu) || eu <= 0) {
            return 0;
        }
        double accepted = Math.min(eu, capacity() - energy);
        if (accepted > 0) {
            energy += accepted;
            setChanged();
        }
        return accepted;
    }

    @Override
    public void setBlockState(BlockState state) {
        boolean facingChanged =
                getBlockState().getValue(MachineBlock.OUTPUT_FACE)
                        != state.getValue(MachineBlock.OUTPUT_FACE);
        super.setBlockState(state);
        if (facingChanged) {
            ForgePower.refresh(this);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble(STORED_ENERGY, energy);
        tag.putInt("ic2_redstone_mode", mode);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        double stored = tag.getDouble(STORED_ENERGY);
        energy = Double.isFinite(stored) ? Mth.clamp(stored, 0, capacity()) : 0;
        int savedMode = tag.getInt("ic2_redstone_mode");
        mode = savedMode >= 0 && savedMode < 7 ? savedMode : 0;
    }
}
