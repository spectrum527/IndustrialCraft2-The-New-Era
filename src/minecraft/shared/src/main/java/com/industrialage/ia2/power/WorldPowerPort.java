package com.industrialage.ia2.power;

import com.industrialage.ia2.energy.graph.EnergyGraph.Ports;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The loaded-side EU service supplied by a native IC2 block entity. Units are EU and EU per packet.
 */
public interface WorldPowerPort {
    /** Native coordinate in the owner's level. */
    BlockPos getBlockPos();

    /** Whether this entity has left its level. */
    boolean isRemoved();

    /** True only when the installed block entity was ticking in the current server tick. */
    boolean ranThisTick(ServerLevel level);

    /** Oriented input/output faces; a device cannot bridge a conductor path. */
    Ports energyPorts();

    /** Whether the device can act as a source in its current mode. */
    default boolean source() {
        return !energyPorts().outputs().isEmpty();
    }

    /** Energy offered in a single packet from the current buffer. */
    default double offer() {
        return 0;
    }

    /** Number of budgeted packets for this source in this tick, before any EU is transferred. */
    default int packetsPerTick() {
        return 1;
    }

    /** Commits the source debit, including path loss. */
    default double draw(double requested) {
        return 0;
    }

    /** Commits and returns the amount actually accepted from one packet. */
    default double receive(double offered) {
        return 0;
    }

    /** Maximum rated input packet used by the world's overload rule. */
    default double packetLimit() {
        return Double.POSITIVE_INFINITY;
    }
}
