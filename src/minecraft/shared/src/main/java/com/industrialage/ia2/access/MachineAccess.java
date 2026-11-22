package com.industrialage.ia2.access;

import com.industrialage.ia2.logistics.TransportNode;
import com.industrialage.ia2.wrench.WrenchTarget;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Six machine-relative automation settings. Player interaction and EU use their original paths. */
public final class MachineAccess {
    static final String SAVE_KEY = "ic2_machine_sides";
    private static final int SIDE_COUNT = 6;

    /** Protocol and save values in stable order. Zero makes existing worlds fully accessible. */
    public enum Mode {
        FULL(true, true),
        BLOCKED(false, false),
        OUTPUT(false, true),
        INPUT(true, false);
        private final boolean input;
        private final boolean output;

        Mode(boolean input, boolean output) {
            this.input = input;
            this.output = output;
        }
    }

    private static final Mode[] MODES = Mode.values();

    private MachineAccess() {}

    /** Reads the compact six-face configuration used by native menu data slots. */
    public static int packed(BlockEntity node) {
        return node instanceof MachineAccessTarget target && target.exposesMachineAccess()
                ? AccessHost.read(node) & 0xfff
                : 0;
    }

    /** Front of a placed machine, using north for blocks without a facing. */
    public static Direction front(BlockEntity node) {
        if (node == null) return Direction.NORTH;
        var state = node.getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING))
            return state.getValue(BlockStateProperties.FACING);
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return Direction.NORTH;
    }

    /**
     * Resolves Up, Down, Left, Right, Front, Back. Vertical machines retain east as their right.
     */
    public static Direction worldSide(Direction front, int localFace) {
        Direction up =
                switch (front) {
                    case UP -> Direction.SOUTH;
                    case DOWN -> Direction.NORTH;
                    default -> Direction.UP;
                };
        Direction right = front.getAxis().isVertical() ? Direction.EAST : front.getClockWise();
        return switch (localFace) {
            case 0 -> up;
            case 1 -> up.getOpposite();
            case 2 -> right.getOpposite();
            case 3 -> right;
            case 4 -> front;
            case 5 -> front.getOpposite();
            default -> throw new IllegalArgumentException("Unknown local face " + localFace);
        };
    }

    /** Checks build, distance, ownership and identity at the current block position. */
    public static boolean mayConfigure(BlockEntity node, Player player) {
        if (!(node instanceof MachineAccessTarget targetMachine)
                || !targetMachine.exposesMachineAccess()
                || node.isRemoved()
                || player == null
                || player.isSpectator()
                || !player.mayBuild()
                || !(node.getLevel() instanceof ServerLevel level)
                || AccessHost.playerLevel(player) != level) return false;
        var pos = node.getBlockPos();
        if (!level.hasChunkAt(pos)
                || level.getBlockEntity(pos) != node
                || !level.mayInteract(player, pos)
                || player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > 64)
            return false;
        return !(node.getBlockState().getBlock() instanceof WrenchTarget target)
                || target.wrenchPermits(level, pos, player);
    }

    /**
     * Assigns one face without replacing other viewers' changes. Requires an authorized server
     * caller.
     */
    public static boolean set(BlockEntity node, int localFace, int mode) {
        if (!(node instanceof MachineAccessTarget target)
                || !target.exposesMachineAccess()
                || node.isRemoved()
                || !(node.getLevel() instanceof ServerLevel level)
                || !level.hasChunkAt(node.getBlockPos())
                || level.getBlockEntity(node.getBlockPos()) != node
                || localFace < 0
                || localFace >= SIDE_COUNT
                || mode < 0
                || mode >= MODES.length) return false;
        int shift = 2 * localFace;
        int previous = packed(node);
        int next = (previous & ~(3 << shift)) | (mode << shift);
        if (next != previous) {
            var tag = node.getPersistentData();
            if (next == 0) tag.remove(SAVE_KEY);
            else tag.putInt(SAVE_KEY, next);
            node.setChanged();
            refresh(node);
        }
        return true;
    }

    /**
     * Refreshes cached resource views and physical pipe arms at a changed machine and its ports.
     */
    public static void refresh(BlockEntity node) {
        if (node instanceof MachineAccessTarget target
                && target.exposesMachineAccess()
                && node.getLevel() instanceof ServerLevel level
                && !node.isRemoved()) {
            AccessHost.refresh(level, node);
            target.visitAccessPorts(level, port -> AccessHost.refresh(level, port));
        }
    }

    private static int rights(Mode mode) {
        return (mode.input ? 1 : 0) | (mode.output ? 2 : 0);
    }

    private static int sideRights(BlockEntity node, Direction world) {
        int bits = packed(node);
        if (bits == 0) return 3;
        if (world == null) {
            // An unsided automation query cannot bypass restrictions on a physical side.
            int common = 3;
            for (int face = 0; face < SIDE_COUNT; face++) {
                common &= rights(MODES[(bits >>> (2 * face)) & 3]);
            }
            return common;
        }
        var facing = front(node);
        for (int face = 0; face < SIDE_COUNT; face++) {
            if (worldSide(facing, face) == world) return rights(MODES[(bits >>> (2 * face)) & 3]);
        }
        throw new IllegalStateException("Unmapped world face " + world);
    }

    private static int allowed(BlockEntity node, Direction face) {
        if (!(node instanceof MachineAccessTarget target) || !target.exposesMachineAccess())
            return 3;
        if (node.isRemoved()) return 0;
        int local = sideRights(node, face);
        return local == 0 ? 0 : local & sideRights(target.accessController(), face);
    }

    /** Whether an automation handler may put a resource into the touched world face. */
    public static boolean allowsInput(BlockEntity machine, Direction side) {
        return (allowed(machine, side) & 1) != 0;
    }

    /** Whether an automation handler may take a resource out of the touched world face. */
    public static boolean allowsOutput(BlockEntity machine, Direction side) {
        return (allowed(machine, side) & 2) != 0;
    }

    /** Whether this face can form any physical item or fluid pipe connection. */
    public static boolean connects(BlockEntity machine, Direction side) {
        return allowed(machine, side) != 0;
    }

    /**
     * Checks this machine's transfer direction and an opposing nozzle on the same resource channel.
     */
    public static boolean allowsUpgrade(
            BlockEntity machine, Direction face, boolean eject, boolean fluid) {
        if (!(machine.getLevel() instanceof ServerLevel level)
                || !(eject ? allowsOutput(machine, face) : allowsInput(machine, face)))
            return false;
        var adjacent = machine.getBlockPos().relative(face);
        if (!level.hasChunkAt(adjacent)) return false;
        return !(level.getBlockEntity(adjacent) instanceof TransportNode pipe)
                || !pipe.blocksUpgrade(face.getOpposite(), eject, fluid);
    }
}
