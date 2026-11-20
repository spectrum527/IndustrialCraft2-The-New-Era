package com.industrialage.ia2.machine;

import java.util.function.DoubleUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

/** Burns the host inventory's furnace fuel and persists the generator's buffered EU. */
public abstract class FuelGeneratorBlockEntity extends ForgeMachineBlockEntity {
    private static final String GENERATOR = "generator";
    private static final String FUEL_TICKS = "fuel_ticks";
    private static final String STORED_ENERGY = "stored_energy";
    private final FuelGenerator generator;
    private final BlockEntityBase components;

    /** Creates the generator using the host's configured output per working tick. */
    protected FuelGeneratorBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state);
        generator = new FuelGenerator(energyPerTick, this::burnTime, this::consumeFuel);
        components = new BlockEntityBase(generator);
    }

    @Override
    protected final BlockEntityBase machineComponents() {
        return components;
    }

    /** Returns the current fuel slot or {@link ItemStack#EMPTY} when it is vacant. */
    protected abstract ItemStack fuelStack();

    /** Consumes one accepted fuel item and handles its inventory remainder. */
    protected abstract void consumeFuel();

    private int burnTime() {
        ItemStack stack = fuelStack();
        if (stack.isEmpty() || stack.is(Items.LAVA_BUCKET)) {
            return 0;
        }
        return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
    }

    /** Returns remaining fuel ticks and the current double-precision EU balance. */
    public final FuelGenerator.Snapshot savedState() {
        return generator.snapshot();
    }

    /** Returns the live burn reserve as a fraction of the active fuel. */
    public final double fuelRatio() {
        int capacity = generator.fuelCapacity();
        return capacity == 0 ? 0 : (double) generator.snapshot().fuelTicks() / capacity;
    }

    /** Reports activity during the most recent loaded tick. */
    public final boolean isActive() {
        return generator.isActive();
    }

    /** Debits at most one 32 EU packet. The calling network round controls the cadence. */
    public final double drawPacket(double requested) {
        return draw(requested, generator::extractEnergy);
    }

    /** Debits local slot charging independently of the generator's network packet rating. */
    protected final double drawLocalEnergy(double requested) {
        return draw(requested, generator::extractLocalEnergy);
    }

    private double draw(double requested, DoubleUnaryOperator debit) {
        if (!isLoadedServerMachine()) {
            return 0;
        }
        double sent = debit.applyAsDouble(requested);
        if (sent > 0) {
            setChanged();
        }
        return sent;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        var state = savedState();
        var payload = new CompoundTag();
        payload.putInt(FUEL_TICKS, state.fuelTicks());
        payload.putInt("fuel_capacity", generator.fuelCapacity());
        payload.putDouble(STORED_ENERGY, state.storedEnergy());
        tag.put(GENERATOR, payload);
    }

    @Override
    public void load(CompoundTag tag) {
        var state = readState(tag);
        super.load(tag);
        generator.restore(state);
        generator.restoreFuelCapacity(tag.getCompound(GENERATOR).getInt("fuel_capacity"));
    }

    private FuelGenerator.Snapshot readState(CompoundTag tag) {
        var value = tag.get(GENERATOR);
        if (value == null) {
            return new FuelGenerator.Snapshot(0, 0);
        }
        if (!(value instanceof CompoundTag payload)) {
            throw invalidState("expected a compound");
        }
        double ticks = number(payload, FUEL_TICKS);
        if (ticks < 0 || ticks != (int) ticks) {
            throw invalidState(FUEL_TICKS + " must be a nonnegative int, got " + ticks);
        }
        double energy = number(payload, STORED_ENERGY);
        if (!(energy >= 0 && energy <= FuelGenerator.CAPACITY)) {
            throw invalidState(STORED_ENERGY + " is outside the generator capacity: " + energy);
        }
        return new FuelGenerator.Snapshot((int) ticks, energy);
    }

    private double number(CompoundTag tag, String key) {
        if (tag.get(key) instanceof NumericTag number) {
            return number.getAsDouble();
        }
        throw invalidState(key + " must be present and numeric");
    }

    private IllegalStateException invalidState(String detail) {
        return new IllegalStateException(
                "Invalid generator state at " + getBlockPos() + " (" + detail + ")");
    }
}
