package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.content.PowerGrid;
import com.industrialage.ia2.industrial.CableShock;
import com.industrialage.ia2.industrial.PowerBlock;
import com.industrialage.ia2.industrial.WorkshopBlock;
import com.industrialage.ia2.world.CableColors;
import com.industrialage.ia2.world.FoamCasing;
import com.industrialage.ia2.world.WorldMachineBlock;
import com.industrialage.ia2.wrench.WrenchTarget;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Connectable cable with a collision shape matching its visible branches. */
public class CableBlock extends NativeGameplay.CableBlockBase
        implements SimpleWaterloggedBlock, WrenchTarget {
    @Override
    public boolean wrenchUsesLoot() {
        return true;
    }

    static final int DETECTOR_INTERVAL = 32;
    private static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final Map<Direction, BooleanProperty> CONNECTIONS =
            Map.of(
                    Direction.DOWN,
                    BlockStateProperties.DOWN,
                    Direction.UP,
                    BlockStateProperties.UP,
                    Direction.NORTH,
                    BlockStateProperties.NORTH,
                    Direction.SOUTH,
                    BlockStateProperties.SOUTH,
                    Direction.WEST,
                    BlockStateProperties.WEST,
                    Direction.EAST,
                    BlockStateProperties.EAST);
    private static final VoxelShape[][] SHAPES = {
        shapes(6, 10), shapes(5, 11), shapes(4, 12), shapes(3, 13)
    };
    final String path;
    final boolean exposed;
    private final int size;
    private final double packetLimit;

    private CableBlock(Properties properties, String path) {
        super(properties);
        this.path = path;
        exposed = !(this instanceof Foamed) && CableShock.isBare(path);
        packetLimit = PowerGrid.cableRating(path);
        size = size(path);
        BlockState state = stateDefinition.any().setValue(BlockStateProperties.WATERLOGGED, false);
        for (var property : CONNECTIONS.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    static CableBlock create(Properties properties, String path) {
        return switch (path) {
            case "detector_cable", "splitter_cable" -> new Controlled(properties, path);
            default ->
                    CableColors.paintable(path)
                            ? new Colored(properties, path)
                            : new CableBlock(properties, path);
        };
    }

    static CableBlock foamed(Properties properties, String path) {
        return CableColors.paintable(path)
                ? new ColoredFoam(properties, path)
                : new Foamed(properties, path);
    }

    /** Applies a painter and refreshes both the visual arms and the cached EU routes. */
    public static boolean paint(Level level, BlockPos pos, BlockState state, int color) {
        if (!(state.getBlock() instanceof Colored cable) || color == CableColors.color(state))
            return false;
        if (!(level instanceof ServerLevel server)) return true;
        var painted = state.setValue(CableColors.COLOR, color);
        for (Direction side : Direction.values()) {
            var neighborPos = pos.relative(side);
            painted =
                    cable.updateWorldConnection(
                            level,
                            pos,
                            painted,
                            side,
                            neighborPos,
                            level.getBlockState(neighborPos));
        }
        if (!server.setBlockAndUpdate(pos, painted)) return false;
        EnergyNetwork.invalidate(server);
        return true;
    }

    boolean conducts(BlockState state) {
        return !path.equals("splitter_cable") || state.getValue(ACTIVE);
    }

    boolean detector() {
        return path.equals("detector_cable");
    }

    double packetLimit() {
        return packetLimit;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.WATERLOGGED);
        CONNECTIONS.values().forEach(builder::add);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state =
                defaultBlockState()
                        .setValue(
                                BlockStateProperties.WATERLOGGED,
                                context.getLevel().getFluidState(context.getClickedPos()).getType()
                                        == Fluids.WATER);
        for (var direction : Direction.values()) {
            var neighbor = context.getClickedPos().relative(direction);
            state =
                    updateWorldConnection(
                            context.getLevel(),
                            context.getClickedPos(),
                            state,
                            direction,
                            neighbor,
                            context.getLevel().getBlockState(neighbor));
        }
        if (path.equals("splitter_cable")) {
            state =
                    state.setValue(
                            ACTIVE, context.getLevel().hasNeighborSignal(context.getClickedPos()));
        }
        return state;
    }

    @Override
    BlockState updateConnection(BlockState state, Direction side, BlockState neighbor) {
        var block = neighbor.getBlock();
        boolean connected =
                block instanceof CableBlock && CableColors.compatible(state, neighbor)
                        || block instanceof PowerBlock power && power.acceptsPower()
                        || block instanceof WorldMachineBlock world && world.acceptsPower()
                        || block instanceof WorkshopBlock workshop && workshop.acceptsPower()
                        || block instanceof MachineBlock machine
                                && machine.kind != MachineKind.IRON_FURNACE;
        return state.setValue(CONNECTIONS.get(side), connected);
    }

    @Override
    BlockState updateWorldConnection(
            BlockGetter level,
            BlockPos pos,
            BlockState state,
            Direction side,
            BlockPos neighborPos,
            BlockState neighbor) {
        var updated = updateConnection(state, side, neighbor);
        var property = CONNECTIONS.get(side);
        if (updated.getValue(property) || level == null) return updated;
        if (level.getBlockEntity(neighborPos)
                        instanceof com.industrialage.ia2.power.WorldPowerPort port
                && !port.isRemoved()) {
            var ports = port.energyPorts();
            if (!ports.inputs().isEmpty() || !ports.outputs().isEmpty()) {
                return updated.setValue(property, true);
            }
        }
        if (level instanceof ServerLevel server && EnergyNetwork.contains(server, neighborPos)) {
            return updated.setValue(property, true);
        }
        return updated;
    }

    static void refreshNeighbors(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return;
        var self = level.getBlockState(pos);
        for (var side : Direction.values()) {
            var cablePos = pos.relative(side);
            if (!level.hasChunkAt(cablePos)) continue;
            var cableState = level.getBlockState(cablePos);
            if (cableState.getBlock() instanceof CableBlock cable) {
                var updated =
                        cable.updateWorldConnection(
                                level, cablePos, cableState, side.getOpposite(), pos, self);
                if (updated != cableState) {
                    level.setBlock(cablePos, updated, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    @Override
    protected void onPlace(
            BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (level instanceof ServerLevel server && old.getBlock() != this) {
            EnergyNetwork.invalidate(server);
            if (detector()) {
                server.scheduleTick(pos, this, DETECTOR_INTERVAL);
            }
        }
    }

    @Override
    void updatePower(Level level, BlockPos pos, BlockState state) {
        if (path.equals("splitter_cable") && level instanceof ServerLevel server) {
            boolean active = server.hasNeighborSignal(pos);
            if (state.getValue(ACTIVE) != active) {
                server.setBlockAndUpdate(pos, state.setValue(ACTIVE, active));
                EnergyNetwork.invalidate(server);
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (detector()) {
            boolean active = EnergyNetwork.wasCarryingPower(level, pos);
            if (state.getValue(ACTIVE) != active) {
                level.setBlockAndUpdate(pos, state.setValue(ACTIVE, active));
                NativeGameplay.notifySignal(level, pos, this);
            }
            level.scheduleTick(pos, this, DETECTOR_INTERVAL);
        }
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return detector();
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return detector() && state.getValue(ACTIVE) ? 15 : 0;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (var direction : Direction.values()) {
            if (state.getValue(CONNECTIONS.get(direction))) {
                mask |= 1 << direction.ordinal();
            }
        }
        return SHAPES[size][mask];
    }

    private static int size(String path) {
        if (path.startsWith("triple_insulated_")
                || path.equals("detector_cable")
                || path.equals("splitter_cable")) return 3;
        if (path.startsWith("double_insulated_") || path.equals("insulated_iron_cable")) return 2;
        if (path.startsWith("insulated_") || path.equals("iron_cable")) return 1;
        return 0;
    }

    private static VoxelShape[] shapes(int low, int high) {
        var result = new VoxelShape[1 << Direction.values().length];
        var branches =
                Map.of(
                        Direction.DOWN, box(low, 0, low, high, low, high),
                        Direction.UP, box(low, high, low, high, 16, high),
                        Direction.NORTH, box(low, low, 0, high, high, low),
                        Direction.SOUTH, box(low, low, high, high, high, 16),
                        Direction.WEST, box(0, low, low, low, high, high),
                        Direction.EAST, box(high, low, low, 16, high, high));
        for (int mask = 0; mask < result.length; mask++) {
            var shape = box(low, low, low, high, high, high);
            for (var direction : Direction.values()) {
                if ((mask & (1 << direction.ordinal())) != 0) {
                    shape = Shapes.or(shape, branches.get(direction));
                }
            }
            result[mask] = shape;
        }
        return result;
    }

    /** Electrical topology follows the enclosed cable while the foam sets. */
    private static class Foamed extends CableBlock {
        Foamed(Properties options, String path) {
            super(
                    options.strength(1.5F, 8)
                            .sound(net.minecraft.world.level.block.SoundType.STONE)
                            .randomTicks(),
                    path);
            registerDefaultState(
                    defaultBlockState()
                            .setValue(ACTIVE, false)
                            .setValue(FoamCasing.HARDENED, false));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(ACTIVE, FoamCasing.HARDENED);
        }

        @Override
        public boolean isRandomlyTicking(BlockState state) {
            return !state.getValue(FoamCasing.HARDENED);
        }

        @Override
        public void randomTick(
                BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (random.nextInt(3) == 0)
                level.setBlockAndUpdate(pos, state.setValue(FoamCasing.HARDENED, true));
        }

        @Override
        public VoxelShape getShape(
                BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.block();
        }
    }

    private static final class Colored extends CableBlock {
        Colored(Properties properties, String path) {
            super(properties, path);
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(CableColors.COLOR);
        }
    }

    private static final class ColoredFoam extends Foamed {
        ColoredFoam(Properties properties, String path) {
            super(properties, path);
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(CableColors.COLOR);
        }
    }

    private static final class Controlled extends CableBlock {
        Controlled(Properties properties, String path) {
            super(properties, path);
            registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(ACTIVE);
        }
    }
}
