package com.industrialage.ia2.industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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

/** A player-placeable workshop device with a native inventory, recipe mode and powered face. */
public class WorkshopBlock extends WorkshopBlockBase implements EntityBlock {
    private static final EnumProperty<Direction> HORIZONTAL =
            BlockStateProperties.HORIZONTAL_FACING;
    private static final EnumProperty<Direction> ALL_FACES = BlockStateProperties.FACING;
    static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    final WorkshopKind kind;

    WorkshopBlock(Properties properties, WorkshopKind kind) {
        super(properties);
        this.kind = kind;
        Direction initial =
                switch (kind) {
                    case COKE_GRATE, PUMP -> Direction.DOWN;
                    case COKE_HATCH -> Direction.UP;
                    default -> Direction.NORTH;
                };
        registerDefaultState(withDirection(defaultBlockState(), initial).setValue(ACTIVE, false));
    }

    static WorkshopBlock create(Properties properties, WorkshopKind kind) {
        return kind.sixDirections()
                ? new SixDirections(properties, kind)
                : new WorkshopBlock(properties, kind);
    }

    static Direction direction(BlockState state) {
        return state.getValue(state.hasProperty(ALL_FACES) ? ALL_FACES : HORIZONTAL);
    }

    private static BlockState withDirection(BlockState state, Direction direction) {
        return state.setValue(state.hasProperty(ALL_FACES) ? ALL_FACES : HORIZONTAL, direction);
    }

    /** Whether a neighboring EU cable may visually connect to this block. */
    public boolean acceptsPower() {
        return kind.capacity > 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (kind == WorkshopKind.COKE_HATCH || kind == WorkshopKind.COKE_GRATE) {
            return defaultBlockState();
        }
        return withDirection(
                defaultBlockState(),
                (kind.sixDirections()
                                ? context.getNearestLookingDirection()
                                : context.getHorizontalDirection())
                        .getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WorkshopNode(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        if (!(world instanceof ServerLevel))
            return (w, p, s, e) -> com.industrialage.ia2.audio.IndustrialSounds.observe(w, p, s);
        return world instanceof ServerLevel server && type == WorkshopPlatform.type()
                ? (level, pos, current, entity) -> ((WorkshopNode) entity).tick(server)
                : null;
    }

    @Override
    boolean interact(
            Level world, BlockPos pos, Player player, InteractionHand hand, Direction side) {
        if (!(world.getBlockEntity(pos) instanceof WorkshopNode node)) {
            return false;
        }
        var held = player.getItemInHand(hand);
        if (WorkshopPlatform.itemId(held).equals("ic2:wrench")) {
            if (world instanceof ServerLevel
                    && (kind.sixDirections() || side.getAxis().isHorizontal())) {
                world.setBlockAndUpdate(pos, withDirection(node.getBlockState(), side));
            }
        } else if (player.isShiftKeyDown()
                && held.isEmpty()
                && kind == WorkshopKind.WEIGHTED_DISTRIBUTOR
                && side != direction(node.getBlockState())) {
            if (world instanceof ServerLevel) {
                var priority = WorkshopDevices.changePriority(node, side);
                WorkshopPlatform.announceRoute(player, side, priority);
            }
        } else if ((kind == WorkshopKind.UU_SCANNER || kind == WorkshopKind.PATTERN_STORAGE)
                && WorkshopPlatform.itemId(held).equals("ic2:crystal_memory")) {
            if (world instanceof ServerLevel) {
                WorkshopMatter.useMemory(node, player, held);
            }
        } else if (player.isShiftKeyDown()
                && held.isEmpty()
                && (kind == WorkshopKind.UU_SCANNER || kind == WorkshopKind.PATTERN_STORAGE)) {
            if (world instanceof ServerLevel level) {
                WorkshopMatter.recordOrSelect(node, level, player);
            }
        } else if (kind == WorkshopKind.MANUAL_KINETIC
                && held.isEmpty()
                && !player.isShiftKeyDown()) {
            if (world instanceof ServerLevel level) {
                WorkshopKinetic.crank(node, level);
            }
        } else if (player.isShiftKeyDown() && held.isEmpty() && kind.modes() > 1) {
            if (world instanceof ServerLevel) {
                node.cycleMode();
                WorkshopPlatform.announceMode(player, node);
            }
        } else if (held.isEmpty() || !WorkshopPlatform.interactFluid(node, player, hand, side)) {
            if (world instanceof ServerLevel) {
                WorkshopPlatform.open(player, node);
            }
        }
        return true;
    }

    private static final class SixDirections extends WorkshopBlock {
        SixDirections(Properties properties, WorkshopKind kind) {
            super(properties, kind);
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(ALL_FACES, ACTIVE);
        }
    }
}
