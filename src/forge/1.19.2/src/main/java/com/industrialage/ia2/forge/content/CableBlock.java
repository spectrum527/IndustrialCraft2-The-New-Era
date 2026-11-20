package com.industrialage.ia2.forge.content;

import com.industrialage.ia2.content.PowerGrid;
import com.industrialage.ia2.industrial.PowerBlock;
import com.industrialage.ia2.industrial.WorkshopBlock;
import com.industrialage.ia2.world.CableColors;
import com.industrialage.ia2.world.FoamCasing;
import com.industrialage.ia2.world.WorldMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CableBlock extends Block implements EntityBlock {
    static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape CORE = box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape[] SHAPES = buildShapes();
    private final double loss;
    private final double rating;
    private final Mode mode;

    CableBlock(String path) {
        this(path, Mode.NORMAL);
    }

    CableBlock(String path, Mode mode) {
        this(
                path,
                mode,
                Properties.of(Material.METAL)
                        .strength(0.3F, 2)
                        .sound(SoundType.METAL)
                        .noOcclusion());
    }

    private CableBlock(String path, Mode mode, Properties settings) {
        super(settings);
        loss = PowerGrid.cableLoss(path);
        rating = PowerGrid.cableRating(path);
        this.mode = mode;
    }

    static CableBlock plain(String path) {
        return CableColors.paintable(path) ? new Colored(path) : new CableBlock(path);
    }

    static CableBlock foamed(String path) {
        return CableColors.paintable(path) ? new ColoredFoam(path) : new Foamed(path);
    }

    /** Applies a dye, refreshes the visible arms, and updates the installed conductor. */
    public static boolean paint(Level level, BlockPos pos, BlockState state, int color) {
        if (!(state.getBlock() instanceof Colored cable) || color == CableColors.color(state))
            return false;
        if (!(level instanceof ServerLevel server)) return true;
        var painted = state.setValue(CableColors.COLOR, color);
        for (Direction side : Direction.values()) {
            painted =
                    painted.setValue(
                            arm(side),
                            cable.connects(painted, level.getBlockState(pos.relative(side))));
        }
        if (!server.setBlockAndUpdate(pos, painted)) return false;
        var conductor = server.getBlockEntity(pos);
        if (conductor != null) ForgePower.refresh(conductor);
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction side : Direction.values()) {
            builder.add(arm(side));
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = defaultBlockState();
        if (mode == Mode.SPLITTER) {
            state = state.setValue(ACTIVE, level.hasNeighborSignal(pos));
        }
        for (Direction side : Direction.values()) {
            var neighbor = level.getBlockState(pos.relative(side));
            state = state.setValue(arm(side), connects(state, neighbor));
        }
        return state;
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction side,
            BlockState neighbor,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        return state.setValue(arm(side), connects(state, neighbor));
    }

    final boolean connects(BlockState state, BlockState neighbor) {
        return neighbor.getBlock() instanceof CableBlock && CableColors.compatible(state, neighbor)
                || neighbor.getBlock() instanceof PowerBlock
                || neighbor.getBlock() instanceof WorldMachineBlock world && world.acceptsPower()
                || neighbor.getBlock() instanceof WorkshopBlock workshop && workshop.acceptsPower()
                || neighbor.getBlock() instanceof MachineBlock machine
                        && machine.kind != MachineKind.IRON_FURNACE;
    }

    @Override
    public VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction side : Direction.values()) {
            if (state.getValue(arm(side))) {
                mask |= 1 << side.ordinal();
            }
        }
        return SHAPES[mask];
    }

    private static BooleanProperty arm(Direction side) {
        return switch (side) {
            case DOWN -> BlockStateProperties.DOWN;
            case UP -> BlockStateProperties.UP;
            case NORTH -> BlockStateProperties.NORTH;
            case SOUTH -> BlockStateProperties.SOUTH;
            case WEST -> BlockStateProperties.WEST;
            case EAST -> BlockStateProperties.EAST;
        };
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape[] arms = {
            box(6, 0, 6, 10, 6, 10), box(6, 10, 6, 10, 16, 10),
            box(6, 6, 0, 10, 10, 6), box(6, 6, 10, 10, 10, 16),
            box(0, 6, 6, 6, 10, 10), box(10, 6, 6, 16, 10, 10)
        };
        var shapes = new VoxelShape[64];
        for (int mask = 0; mask < shapes.length; mask++) {
            VoxelShape shape = CORE;
            for (int side = 0; side < arms.length; side++) {
                if ((mask & (1 << side)) != 0) {
                    shape = Shapes.or(shape, arms[side]);
                }
            }
            shapes[mask] = shape;
        }
        return shapes;
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighbor,
            BlockPos source,
            boolean moving) {
        if (mode == Mode.SPLITTER && !level.isClientSide) {
            updateSwitch(level, pos, state);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (mode == Mode.SPLITTER) {
            updateSwitch(level, pos, state);
        }
    }

    private static void updateSwitch(Level level, BlockPos pos, BlockState state) {
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(ACTIVE) != powered
                && level.setBlockAndUpdate(pos, state.setValue(ACTIVE, powered))) {
            var entity = level.getBlockEntity(pos);
            if (entity != null) {
                ForgePower.refresh(entity);
            }
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return mode == Mode.DETECTOR;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return mode == Mode.DETECTOR && state.getValue(ACTIVE) ? 15 : 0;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || mode != Mode.DETECTOR || type != ForgeContent.CONDUCTOR.get()) {
            return null;
        }
        return (world, pos, blockState, entity) ->
                ((Conductor) entity).sample(world, pos, blockState);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Conductor(pos, state);
    }

    private static class Foamed extends CableBlock {
        Foamed(String path) {
            super(
                    path,
                    path.equals("detector_cable")
                            ? Mode.DETECTOR
                            : path.equals("splitter_cable") ? Mode.SPLITTER : Mode.NORMAL,
                    Properties.of(Material.METAL)
                            .strength(1.5F, 8)
                            .sound(SoundType.STONE)
                            .randomTicks());
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
        Colored(String path) {
            super(path);
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(CableColors.COLOR);
        }
    }

    private static final class ColoredFoam extends Foamed {
        ColoredFoam(String path) {
            super(path);
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(CableColors.COLOR);
        }
    }

    static final class Special extends CableBlock {
        Special(String path) {
            super(path, path.equals("detector_cable") ? Mode.DETECTOR : Mode.SPLITTER);
            registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(ACTIVE);
        }
    }

    enum Mode {
        NORMAL,
        DETECTOR,
        SPLITTER
    }

    static final class Conductor extends BlockEntity implements ForgePower.Endpoint {
        private long lastTransfer = Long.MIN_VALUE;

        Conductor(BlockPos pos, BlockState state) {
            super(ForgeContent.CONDUCTOR.get(), pos, state);
        }

        private CableBlock cable() {
            return (CableBlock) getBlockState().getBlock();
        }

        private boolean connected() {
            return cable().mode != Mode.SPLITTER || getBlockState().getValue(ACTIVE);
        }

        @Override
        public boolean accepts(Direction face) {
            return connected();
        }

        @Override
        public boolean emits(Direction face) {
            return connected();
        }

        @Override
        public boolean conducts() {
            return true;
        }

        @Override
        public double loss() {
            return cable().loss;
        }

        boolean overCurrent(double debit) {
            return debit > cable().rating + 1;
        }

        void markTransfer() {
            if (cable().mode == Mode.DETECTOR) {
                lastTransfer = level.getGameTime();
            }
        }

        private void sample(Level level, BlockPos pos, BlockState state) {
            if ((level.getGameTime() - pos.hashCode()) % 32 != 0) {
                return;
            }
            boolean active = lastTransfer >= level.getGameTime() - 1;
            if (state.getValue(ACTIVE) != active) {
                level.setBlockAndUpdate(pos, state.setValue(ACTIVE, active));
            }
        }

        @Override
        public void onLoad() {
            super.onLoad();
            ForgePower.attach(this);
            if (level instanceof ServerLevel server && cable().mode == Mode.SPLITTER) {
                server.scheduleTick(getBlockPos(), getBlockState().getBlock(), 1);
            }
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
    }
}
