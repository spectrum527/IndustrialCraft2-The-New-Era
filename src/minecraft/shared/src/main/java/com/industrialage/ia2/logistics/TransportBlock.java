package com.industrialage.ia2.logistics;

import com.industrialage.ia2.content.TransportSpec;
import com.industrialage.ia2.content.TransportSpec.Kind;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A physical item pipe, fluid pipe or tank. Each shape has only its relevant state properties. */
public class TransportBlock extends TransportBlockBase implements EntityBlock {
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
            Level world, BlockPos pos, Player player, InteractionHand hand, Direction side) {
        if (!(world.getBlockEntity(pos) instanceof TransportNode node)) return false;
        ItemStack item = player.getItemInHand(hand);
        String id = TransportPlatform.itemId(item);
        boolean server = world instanceof ServerLevel;
        if (id.equals("ic2:wrench") && spec.kind() != Kind.TANK) {
            if (server) {
                if (spec.kind() == Kind.ITEM_PIPE) {
                    world.setBlockAndUpdate(pos, node.getBlockState().setValue(OUT, side));
                } else node.toggle(side);
            }
            return true;
        }
        if (spec.kind() == Kind.FLUID_PIPE) {
            if (id.equals("ic2:crowbar")) {
                if (server) {
                    int rate = node.uncover(side);
                    if (rate > 0) Block.popResource(world, pos, TransportPlatform.coverItem(rate));
                }
                return true;
            }
            int rate =
                    switch (id) {
                        case "ic2:pump_cover_lv" -> 32;
                        case "ic2:pump_cover_mv" -> 128;
                        default -> 0;
                    };
            if (rate > 0) {
                if (server && node.cover(side, rate) && !player.getAbilities().instabuild)
                    item.shrink(1);
                return true;
            }
        }
        if (spec.kind() != Kind.ITEM_PIPE
                && server
                && id.equals("ic2:empty_cell")
                && node.liquid != null
                && TransportPlatform.isPristineCell(item)
                && node.drain(side, node.liquid, 1000, true) == 1000) {
            var named = WorkshopPlatform.namedFilledCell(node.liquid.stack(1));
            if (!named.isEmpty()) {
                node.drain(side, node.liquid, 1000, false);
                player.setItemInHand(hand, ItemUtils.createFilledResult(item, player, named));
                return true;
            }
        }
        if (spec.kind() != Kind.ITEM_PIPE
                && TransportPlatform.containerInteraction(node, side, player, hand)) return true;
        if (spec.kind() != Kind.TANK) return false;
        if (server) player.openMenu(node);
        return true;
    }

    @Override
    BlockState connect(BlockState state, Direction side, BlockState neighbor) {
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
        BlockState connect(BlockState state, Direction side, BlockState neighbor) {
            boolean connected =
                    neighbor.getBlock() instanceof TransportBlock b
                            ? (b.spec.kind() == Kind.ITEM_PIPE) == (spec.kind() == Kind.ITEM_PIPE)
                            : neighbor.hasBlockEntity();
            return state.setValue(SIDES.get(side), connected);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = defaultBlockState();
            for (Direction side : TransportNode.SIDES) {
                state =
                        connect(
                                state,
                                side,
                                context.getLevel()
                                        .getBlockState(context.getClickedPos().relative(side)));
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

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return super.getStateForPlacement(context)
                    .setValue(OUT, context.getNearestLookingDirection());
        }
    }
}
