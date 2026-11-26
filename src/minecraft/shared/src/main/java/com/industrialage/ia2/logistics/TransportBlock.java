package com.industrialage.ia2.logistics;

import com.industrialage.ia2.content.TransportSpec;
import com.industrialage.ia2.content.TransportSpec.Kind;
import com.industrialage.ia2.logistics.TransportNode.Nozzle;
import com.industrialage.ia2.wrench.WrenchTarget;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A physical item pipe, fluid pipe or tank. Each shape has only its relevant state properties. */
public class TransportBlock extends TransportBlockBase implements EntityBlock, WrenchTarget {
    final TransportSpec spec;
    static final EnumProperty<Direction> OUT = BlockStateProperties.FACING;
    private static final Map<Direction, BooleanProperty> SIDES =
            Map.of(
                    Direction.UP,
                    BlockStateProperties.UP,
                    Direction.DOWN,
                    BlockStateProperties.DOWN,
                    Direction.EAST,
                    BlockStateProperties.EAST,
                    Direction.WEST,
                    BlockStateProperties.WEST,
                    Direction.NORTH,
                    BlockStateProperties.NORTH,
                    Direction.SOUTH,
                    BlockStateProperties.SOUTH);

    TransportBlock(Properties props, TransportSpec spec) {
        super(props);
        this.spec = spec;
    }

    static TransportBlock create(Properties props, TransportSpec spec) {
        return switch (spec.kind()) {
            case ITEM_PIPE -> new ItemPipe(props, spec);
            case FLUID_PIPE -> new Pipe(props, spec);
            case TANK -> new TransportBlock(props, spec);
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransportNode(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        if (!(world instanceof ServerLevel server)
                || type != TransportPlatform.type()
                || (spec.kind() == Kind.TANK && !spec.path().equals("tank"))) return null;
        return (level, pos, current, entity) -> ((TransportNode) entity).tick(server);
    }

    @Override
    boolean interact(
            Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof TransportNode node)) return false;
        ItemStack item = player.getItemInHand(hand);
        String id = TransportPlatform.itemId(item);
        boolean server = world instanceof ServerLevel;
        var side = hit.getDirection();
        if (spec.kind() != Kind.TANK
                && serviceAttachment(world, pos, node, player, item, attachmentFace(node, hit), id))
            return true;
        if (spec.kind() != Kind.ITEM_PIPE
                && TransportPlatform.containerInteraction(node, side, player, hand)) return true;
        if (spec.kind() != Kind.TANK) return false;
        if (server) player.openMenu(node);
        return true;
    }

    private Direction attachmentFace(TransportNode node, BlockHitResult hit) {
        var pos = hit.getBlockPos();
        var offset = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        double rim = (6 - spec.size()) / 16.0 - 0.00001;
        for (var face : TransportNode.SIDES) {
            if (!connected(node.getBlockState(), face)) continue;
            double point = offset.get(face.getAxis());
            if ((face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 - point : point)
                    < rim) return face;
        }
        return hit.getDirection();
    }

    private boolean serviceAttachment(
            Level world,
            BlockPos pos,
            TransportNode node,
            Player player,
            ItemStack item,
            Direction side,
            String id) {
        Nozzle mode =
                switch (id) {
                    case "ic2:pipe_extraction_nozzle" -> Nozzle.EXTRACTION;
                    case "ic2:pipe_injection_nozzle" -> Nozzle.INJECTION;
                    default -> Nozzle.NONE;
                };
        int pumpRate =
                spec.kind() == Kind.FLUID_PIPE
                        ? switch (id) {
                            case "ic2:pump_cover_lv" -> 32;
                            case "ic2:pump_cover_mv" -> 128;
                            default -> 0;
                        }
                        : 0;
        boolean crowbar = id.equals("ic2:crowbar");
        if (mode == Nozzle.NONE && pumpRate == 0 && !crowbar) return false;
        if (!player.mayBuild()
                || !world.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, side, item)) return false;
        if (!(world instanceof ServerLevel level)) return true;
        if (crowbar) {
            var removed = node.removeNozzle(side);
            if (removed != Nozzle.NONE) {
                Block.popResource(level, pos, TransportPlatform.nozzleItem(removed));
                configurationChanged(level, pos, true);
            } else {
                int rate = node.uncover(side);
                if (rate != 0) {
                    Block.popResource(level, pos, TransportPlatform.coverItem(rate));
                    configurationChanged(level, pos, false);
                }
            }
        } else if (pumpRate > 0 ? node.cover(side, pumpRate) : node.installNozzle(side, mode)) {
            if (!player.getAbilities().instabuild) item.shrink(1);
            if (mode == Nozzle.EXTRACTION && spec.kind() == Kind.ITEM_PIPE) {
                var state = level.getBlockState(pos);
                if (state.hasProperty(OUT) && state.getValue(OUT) == side) {
                    Direction redirected =
                            ItemPipeRouter.redirectedOutput(
                                    side,
                                    candidate -> {
                                        var adjacent = pos.relative(candidate);
                                        return level.hasChunkAt(adjacent)
                                                && level.getBlockEntity(adjacent)
                                                        instanceof TransportNode next
                                                && node.linksTo(candidate, next);
                                    });
                    level.setBlock(pos, state.setValue(OUT, redirected), Block.UPDATE_CLIENTS);
                }
            }
            configurationChanged(level, pos, mode != Nozzle.NONE);
        }
        return true;
    }

    private static void configurationChanged(
            ServerLevel level, BlockPos pos, boolean updateNozzles) {
        refreshConnections(level, pos);
        if (updateNozzles) {
            var state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Re-evaluates arms touching a changed device or pipe without loading another chunk. */
    public static void refreshConnections(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return;
        var original = level.getBlockState(pos);
        var current = original;
        for (var face : TransportNode.SIDES) {
            var adjacent = pos.relative(face);
            if (!level.hasChunkAt(adjacent)) continue;
            var neighbor = level.getBlockState(adjacent);
            if (original.getBlock() instanceof Pipe pipe)
                current = pipe.connect(current, face, neighbor, level, pos);
            if (neighbor.getBlock() instanceof Pipe pipe) {
                var connected =
                        pipe.connect(neighbor, face.getOpposite(), original, level, adjacent);
                if (connected != neighbor)
                    level.setBlock(adjacent, connected, Block.UPDATE_CLIENTS);
            }
        }
        if (current != original) level.setBlock(pos, current, Block.UPDATE_CLIENTS);
    }

    /** The authoritative arm state shared by the native collision shape and the nozzle renderer. */
    public static boolean connected(BlockState state, Direction side) {
        var arm = SIDES.get(side);
        return state.hasProperty(arm) && state.getValue(arm);
    }

    @Override
    public boolean wrenchUsesLoot() {
        return true;
    }

    @Override
    public boolean wrenchSide(BlockState state, Direction side) {
        return spec.kind() == Kind.FLUID_PIPE || WrenchTarget.super.wrenchSide(state, side);
    }

    @Override
    public boolean wrenchTurn(ServerLevel level, BlockPos pos, BlockState state, Direction side) {
        if (spec.kind() == Kind.ITEM_PIPE) {
            if (!WrenchTarget.super.wrenchTurn(level, pos, state, side)) return false;
        } else if (!(level.getBlockEntity(pos) instanceof TransportNode node)
                || !node.toggle(side)) {
            return false;
        }
        refreshConnections(level, pos);
        return true;
    }

    @Override
    BlockState connect(
            BlockState state,
            Direction side,
            BlockState neighbor,
            LevelReader world,
            BlockPos pos) {
        return state;
    }

    private static class Pipe extends TransportBlock {
        private final PipeGeometry geometry;

        Pipe(Properties props, TransportSpec spec) {
            super(props, spec);
            BlockState state = defaultBlockState();
            for (var property : SIDES.values()) state = state.setValue(property, false);
            registerDefaultState(state);
            geometry = PipeGeometry.ofSize(spec.size());
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            SIDES.values().forEach(builder::add);
        }

        @Override
        BlockState connect(
                BlockState state,
                Direction side,
                BlockState neighbor,
                LevelReader world,
                BlockPos pos) {
            if (!(world instanceof ServerLevel level)) return state;
            var adjacent = pos.relative(side);
            var arm = SIDES.get(side);
            if (!level.hasChunkAt(adjacent)) return state.setValue(arm, false);
            var node =
                    level.getBlockEntity(pos) instanceof TransportNode transport ? transport : null;
            var target = level.getBlockEntity(adjacent);
            var face = side.getOpposite();
            if (neighbor.getBlock() instanceof Pipe pipe) {
                boolean linked =
                        spec.kind() == pipe.spec.kind()
                                && (node == null || node.exposesPipeFace(side))
                                && (!(target instanceof TransportNode next)
                                        || next.exposesPipeFace(face));
                return state.setValue(arm, linked);
            }
            boolean intake =
                    node == null
                            ? TransportNode.acceptsDefaultInput(state, side)
                            : node.permitsInbound(side);
            boolean outlet =
                    node == null
                            ? TransportNode.acceptsDefaultOutput(state, side)
                            : node.permitsOutbound(side);
            boolean allowed =
                    (intake && TransportNode.machineOutput(target, face))
                            || (outlet && TransportNode.machineInput(target, face));
            boolean connected =
                    allowed
                            && (spec.kind() == Kind.ITEM_PIPE
                                    ? TransportPlatform.hasItemPort(level, adjacent, face)
                                    : TransportPlatform.hasFluidPort(level, adjacent, face));
            return state.setValue(arm, connected);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = defaultBlockState();
            var level = context.getLevel();
            var pos = context.getClickedPos();
            if (state.hasProperty(OUT)) {
                var clicked = context.getClickedFace();
                var against = pos.relative(clicked.getOpposite());
                Direction facing = context.getNearestLookingDirection();
                if (level.hasChunkAt(against)) {
                    var neighbor = level.getBlockState(against);
                    boolean againstItemPipe =
                            neighbor.getBlock() instanceof Pipe pipe
                                    && pipe.spec.kind() == Kind.ITEM_PIPE;
                    boolean againstPort =
                            level instanceof ServerLevel server
                                    && TransportPlatform.hasItemPort(server, against, clicked);
                    if (againstItemPipe || againstPort) {
                        facing = clicked;
                    }
                }
                state = state.setValue(OUT, facing);
            }
            for (Direction side : TransportNode.SIDES) {
                var adjacent = pos.relative(side);
                if (level.hasChunkAt(adjacent)) {
                    state = connect(state, side, level.getBlockState(adjacent), level, pos);
                }
            }
            return state;
        }

        @Override
        public VoxelShape getShape(
                BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
            return geometry.forState(state);
        }
    }

    /** One profile per bore diameter. Each axis has four possible spans across the central cube. */
    private static final class PipeGeometry {
        private static final java.util.List<PipeGeometry> BORES =
                java.util.stream.IntStream.range(0, 4)
                        .mapToObj(size -> new PipeGeometry(6 - size))
                        .toList();
        private final VoxelShape[][][] joined = new VoxelShape[4][4][4];

        private PipeGeometry(int gap) {
            int end = 16 - gap;
            VoxelShape[] eastWest = new VoxelShape[4],
                    vertical = new VoxelShape[4],
                    northSouth = new VoxelShape[4];
            for (int ends = 0; ends < 4; ends++) {
                int start = ends % 2 == 1 ? 0 : gap;
                int finish = ends >= 2 ? 16 : end;
                eastWest[ends] = Block.box(start, gap, gap, finish, end, end);
                vertical[ends] = Block.box(gap, start, gap, end, finish, end);
                northSouth[ends] = Block.box(gap, gap, start, end, end, finish);
            }
            for (int x = 0; x < 4; x++)
                for (int y = 0; y < 4; y++) {
                    VoxelShape cross = Shapes.or(eastWest[x], vertical[y]);
                    for (int z = 0; z < 4; z++) joined[x][y][z] = Shapes.or(cross, northSouth[z]);
                }
        }

        static PipeGeometry ofSize(int size) {
            return BORES.get(size);
        }

        private static int span(
                BlockState state, BooleanProperty negative, BooleanProperty positive) {
            return (state.getValue(negative) ? 1 : 0) + (state.getValue(positive) ? 2 : 0);
        }

        VoxelShape forState(BlockState state) {
            int x = span(state, BlockStateProperties.WEST, BlockStateProperties.EAST);
            int y = span(state, BlockStateProperties.DOWN, BlockStateProperties.UP);
            int z = span(state, BlockStateProperties.NORTH, BlockStateProperties.SOUTH);
            return joined[x][y][z];
        }
    }

    private static final class ItemPipe extends Pipe {
        ItemPipe(Properties props, TransportSpec spec) {
            super(props, spec);
            registerDefaultState(defaultBlockState().setValue(OUT, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(OUT);
        }
    }
}
