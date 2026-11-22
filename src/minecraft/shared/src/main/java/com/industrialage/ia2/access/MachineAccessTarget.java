package com.industrialage.ia2.access;

import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/** A placed IC2 device, including the external ports governed by its controller. */
public interface MachineAccessTarget {
    /** Some storage hosts share a block entity type with simple pipe segments. */
    default boolean exposesMachineAccess() {
        return true;
    }

    /** Discards cached sided automation handles when the loader maintains them on the device. */
    default void invalidateMachinePorts() {}

    /** Uses the ordinary player interaction with the machine's original physical fluid port. */
    default boolean exchangeHeldFluid(Player player, InteractionHand hand, Direction side) {
        return false;
    }

    /** The controller also governing this automation port, or null for a standalone machine. */
    default BlockEntity accessController() {
        return null;
    }

    /** Refreshes the exposed ports of a formed multiblock after editing its controller. */
    default void visitAccessPorts(ServerLevel level, Consumer<BlockEntity> visitor) {}
}
