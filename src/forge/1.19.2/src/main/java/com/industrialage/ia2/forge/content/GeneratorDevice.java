package com.industrialage.ia2.forge.content;

import com.industrialage.ia2.forge.equipment.ForgeGear;
import com.industrialage.ia2.machine.FuelGenerator;
import com.industrialage.ia2.machine.FuelGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

/** Forge 1.19.2 fuel generator block entity exposing EU and item capabilities. */
public final class GeneratorDevice extends FuelGeneratorBlockEntity
        implements ForgeDevice, ForgePower.Endpoint {
    private final boolean producing;
    private final MachineInventory inventory = new MachineInventory(MachineKind.GENERATOR, this);

    GeneratorDevice(BlockPos pos, BlockState state) {
        this(pos, state, configuredOutput());
    }

    private GeneratorDevice(BlockPos pos, BlockState state, int output) {
        super(ForgeContent.GENERATOR.get(), pos, state, output);
        producing = output > 0;
    }

    private static int configuredOutput() {
        float multiplier =
                ForgePower.setting("balance/energy/generator/generator", Float.class, 1f);
        return Math.max(0, Math.round(multiplier * FuelGenerator.DEFAULT_ENERGY_PER_TICK));
    }

    void tick() {
        if (isLoadedServerMachine()) {
            var battery = inventory.stack(MachineKind.Slot.CHARGE);
            drawLocalEnergy(ForgeGear.charge(battery, energy(), 1));
            if (producing) {
                tickMachine();
            }
        }
    }

    @Override
    protected ItemStack fuelStack() {
        return inventory.stack(MachineKind.Slot.FUEL);
    }

    @Override
    protected void consumeFuel() {
        inventory.consumeFuel();
    }

    @Override
    public MachineInventory inventory() {
        return inventory;
    }

    @Override
    public double energy() {
        return savedState().storedEnergy();
    }

    @Override
    public double capacity() {
        return FuelGenerator.CAPACITY;
    }

    @Override
    public boolean active() {
        return isActive();
    }

    @Override
    public boolean emits(Direction face) {
        return true;
    }

    @Override
    public double offers() {
        return isLoadedServerMachine() ? Math.min(energy(), FuelGenerator.OUTPUT_PACKET_LIMIT) : 0;
    }

    @Override
    public double draw(double eu) {
        return drawPacket(eu);
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
