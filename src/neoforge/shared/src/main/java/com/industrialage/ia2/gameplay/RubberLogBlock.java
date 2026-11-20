package com.industrialage.ia2.gameplay;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.registries.DeferredRegister;

/** A rubber tree trunk can carry a single regenerating resin hole on a horizontal face. */
final class RubberLogBlock extends RotatedPillarBlock {
    private static final EnumProperty<Hole> RESIN_HOLE =
            EnumProperty.create("resin_hole", Hole.class);

    private RubberLogBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(RESIN_HOLE, Hole.NONE));
    }

    static void register(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        var log =
                NativeGameplay.block(
                        blocks,
                        "rubber_log",
                        RubberLogBlock::new,
                        () -> Properties.ofFullCopy(Blocks.OAK_LOG).randomTicks());
        var leaves =
                NativeGameplay.block(
                        blocks,
                        "rubber_leaves",
                        NativeGameplay::rubberLeaves,
                        () -> Properties.ofFullCopy(Blocks.OAK_LEAVES));
        var sapling =
                NativeGameplay.block(
                        blocks,
                        "rubber_sapling",
                        properties -> new SaplingBlock(NativeGameplay.rubberTree(), properties),
                        () -> Properties.ofFullCopy(Blocks.OAK_SAPLING));
        items.registerSimpleBlockItem(log);
        items.registerSimpleBlockItem(leaves);
        items.registerSimpleBlockItem(sapling);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RESIN_HOLE);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        var hole = state.getValue(RESIN_HOLE);
        var rotated = super.rotate(state, rotation);
        return hole == Hole.NONE
                ? rotated
                : rotated.setValue(RESIN_HOLE, Hole.at(rotation.rotate(hole.side), hole.wet));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        var hole = state.getValue(RESIN_HOLE);
        return hole != Hole.NONE && !hole.wet;
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        var hole = state.getValue(RESIN_HOLE);
        if (hole != Hole.NONE && !hole.wet && random.nextInt(8) == 0) {
            level.setBlockAndUpdate(pos, state.setValue(RESIN_HOLE, Hole.at(hole.side, true)));
        }
    }

    private enum Hole implements StringRepresentable {
        NONE(null, false),
        WET_NORTH(Direction.NORTH, true),
        WET_EAST(Direction.EAST, true),
        WET_SOUTH(Direction.SOUTH, true),
        WET_WEST(Direction.WEST, true),
        DRY_NORTH(Direction.NORTH, false),
        DRY_EAST(Direction.EAST, false),
        DRY_SOUTH(Direction.SOUTH, false),
        DRY_WEST(Direction.WEST, false);

        final Direction side;
        final boolean wet;

        Hole(Direction side, boolean wet) {
            this.side = side;
            this.wet = wet;
        }

        static Hole at(Direction side, boolean wet) {
            for (var hole : values()) {
                if (hole.side == side && hole.wet == wet) {
                    return hole;
                }
            }
            return NONE;
        }

        @Override
        public String getSerializedName() {
            return side == null ? "none" : (wet ? "wet_" : "dry_") + side.getSerializedName();
        }
    }

    static final class Treetap extends Item {
        Treetap(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            return tap(context, 0);
        }

        static InteractionResult tap(UseOnContext context, int cost) {
            var level = context.getLevel();
            var pos = context.getClickedPos();
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof RubberLogBlock)) {
                return InteractionResult.PASS;
            }
            var hole = state.getValue(RESIN_HOLE);
            if (!hole.wet || hole.side != context.getClickedFace()) {
                return InteractionResult.PASS;
            }
            var player = context.getPlayer();
            if (player != null && (!player.mayBuild() || !level.mayInteract(player, pos))) {
                return InteractionResult.FAIL;
            }
            var tool = context.getItemInHand();
            if (cost > 0
                    && (player == null
                            || !player.isCreative() && PortableGear.stored(tool) < cost)) {
                return InteractionResult.FAIL;
            }
            if (level instanceof ServerLevel server) {
                if (!server.setBlockAndUpdate(
                        pos, state.setValue(RESIN_HOLE, Hole.at(hole.side, false)))) {
                    return InteractionResult.PASS;
                }
                var resin =
                        new ItemStack(
                                IndustrialContent.RESIN.get(), 1 + server.getRandom().nextInt(3));
                if (player == null || !player.getInventory().add(resin)) {
                    Block.popResource(server, pos.relative(hole.side), resin);
                }
                if (player != null) {
                    if (cost == 0) {
                        tool.hurtAndBreak(
                                1,
                                player,
                                context.getHand() == InteractionHand.MAIN_HAND
                                        ? EquipmentSlot.MAINHAND
                                        : EquipmentSlot.OFFHAND);
                    } else if (!player.isCreative()) {
                        PortableGear.spend(tool, cost);
                    }
                }
                server.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 0.8F, 1.1F);
            }
            return InteractionResult.SUCCESS;
        }
    }
}
