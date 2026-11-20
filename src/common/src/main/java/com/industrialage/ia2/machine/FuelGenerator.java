package com.industrialage.ia2.machine;

import java.util.Objects;
import java.util.function.IntSupplier;

/** Converts host-provided fuel to EU in the basic generator. */
public final class FuelGenerator implements BlockEntityBase.Component {
    /** EU produced per working tick at the default configuration. */
    public static final int DEFAULT_ENERGY_PER_TICK = 10;

    /** Internal energy capacity in EU. */
    public static final int CAPACITY = 4_000;

    /** Maximum EU in the generator's single output packet per network round. */
    public static final int OUTPUT_PACKET_LIMIT = 32;

    /** Persistent generator state with remaining working ticks and buffered EU. */
    public record Snapshot(int fuelTicks, double storedEnergy) {
        /** Validates that the persisted values are within the generator's normal domain. */
        public Snapshot {
            if (fuelTicks < 0 || !(storedEnergy >= 0 && storedEnergy <= CAPACITY)) {
                throw new IllegalArgumentException("Invalid generator state");
            }
        }
    }

    private final int energyPerTick;
    private final IntSupplier fuelBurnTime;
    private final Runnable consumeFuel;
    private int fuelTicks;
    private double storedEnergy;
    private boolean active;

    /**
     * Uses the host's rounded EU per working tick. The supplier reads the current non-lava fuel's
     * raw burn time without changing inventory, or returns zero for ineligible fuel. Each consume
     * call removes one item from that same fuel source.
     */
    public FuelGenerator(int energyPerTick, IntSupplier fuelBurnTime, Runnable consumeFuel) {
        if (energyPerTick < 0) {
            throw new IllegalArgumentException("Negative generator output");
        }
        this.energyPerTick = energyPerTick;
        this.fuelBurnTime = Objects.requireNonNull(fuelBurnTime);
        this.consumeFuel = Objects.requireNonNull(consumeFuel);
    }

    /** Burns one working tick when possible and reports if fuel or stored energy changed. */
    @Override
    public boolean serverTick() {
        active = false;
        if (fuelTicks == 0) {
            if (CAPACITY - storedEnergy < energyPerTick) {
                return false;
            }
            int workingTicks = fuelBurnTime.getAsInt() / 4;
            if (workingTicks <= 0) {
                return false;
            }
            consumeFuel.run();
            fuelTicks = workingTicks;
        }
        fuelTicks--;
        storedEnergy = Math.min(CAPACITY, storedEnergy + energyPerTick);
        active = true;
        return true;
    }

    /** Clears the current activity while retaining persistent state for a future load. */
    @Override
    public void onUnload() {
        active = false;
    }

    /** Debits one lossless packet for a finite, nonnegative request. The host controls cadence. */
    public double extractEnergy(double requested) {
        if (!Double.isFinite(requested) || requested < 0) {
            throw new IllegalArgumentException("Invalid generator output request");
        }
        double sent = Math.min(storedEnergy, Math.min(requested, OUTPUT_PACKET_LIMIT));
        storedEnergy -= sent;
        return sent;
    }

    /** Reports whether the latest machine tick burned fuel, including with a full buffer. */
    public boolean isActive() {
        return active;
    }

    /** Captures the state the host stores with its block entity. */
    public Snapshot snapshot() {
        return new Snapshot(fuelTicks, storedEnergy);
    }

    /** Restores host-persisted state. Activity resumes on a subsequent loaded server tick. */
    public void restore(Snapshot snapshot) {
        fuelTicks = snapshot.fuelTicks();
        storedEnergy = snapshot.storedEnergy();
        active = false;
    }
}
