package com.industrialage.ia2.forge.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

final class RubberTree {
    private static final ResourceKey<ConfiguredFeature<?, ?>> TREE =
            ResourceKey.create(
                    Registry.CONFIGURED_FEATURE_REGISTRY, ForgeContent.id("rubber_tree"));

    private RubberTree() {}

    static SaplingBlock sapling() {
        return new SaplingBlock(
                new AbstractTreeGrower() {
                    @Override
                    protected Holder<? extends ConfiguredFeature<?, ?>> getConfiguredFeature(
                            ServerLevel level,
                            ChunkGenerator generator,
                            BlockPos pos,
                            BlockState state,
                            RandomSource random,
                            boolean flowers) {
                        return level.registryAccess()
                                .registryOrThrow(Registry.CONFIGURED_FEATURE_REGISTRY)
                                .getHolder(TREE)
                                .orElse(null);
                    }

                    @Override
                    protected Holder<? extends ConfiguredFeature<?, ?>> getConfiguredFeature(
                            RandomSource random, boolean flowers) {
                        return null;
                    }
                },
                Block.Properties.copy(Blocks.OAK_SAPLING));
    }

    static final class Log extends RotatedPillarBlock {
        static final EnumProperty<Hole> RESIN_HOLE = EnumProperty.create("resin_hole", Hole.class);

        Log() {
            super(Properties.copy(Blocks.OAK_LOG).randomTicks());
            registerDefaultState(defaultBlockState().setValue(RESIN_HOLE, Hole.NONE));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(RESIN_HOLE);
        }

        @Override
        public BlockState rotate(
                BlockState state, net.minecraft.world.level.block.Rotation rotation) {
            var hole = state.getValue(RESIN_HOLE);
            var rotated = super.rotate(state, rotation);
            return hole.side == null
                    ? rotated
                    : rotated.setValue(RESIN_HOLE, Hole.at(rotation.rotate(hole.side), hole.wet));
        }

        @Override
        public boolean isRandomlyTicking(BlockState state) {
            var hole = state.getValue(RESIN_HOLE);
            return hole.side != null && !hole.wet;
        }

        @Override
        public void randomTick(
                BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            var hole = state.getValue(RESIN_HOLE);
            if (hole.side != null && !hole.wet && random.nextInt(8) == 0) {
                level.setBlockAndUpdate(pos, state.setValue(RESIN_HOLE, Hole.at(hole.side, true)));
            }
        }
    }

    static final class Tap extends Item {
        Tap() {
            super(ForgeContent.itemProperties("treetap").durability(16));
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            return tap(context, 0);
        }

        static InteractionResult tap(UseOnContext context, int cost) {
            var level = context.getLevel();
            var pos = context.getClickedPos();
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof Log)) {
                return InteractionResult.PASS;
            }
            var hole = state.getValue(Log.RESIN_HOLE);
            if (!hole.wet || hole.side != context.getClickedFace()) {
                return InteractionResult.PASS;
            }
            var player = context.getPlayer();
            var tool = context.getItemInHand();
            if (player != null && (!player.mayBuild() || !level.mayInteract(player, pos))) {
                return InteractionResult.FAIL;
            }
            if (cost > 0
                    && (player == null
                            || !player.isCreative()
                                    && com.industrialage.ia2.forge.equipment.ForgeGear.stored(tool)
                                            < cost)) {
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide) {
                if (!level.setBlockAndUpdate(
                        pos, state.setValue(Log.RESIN_HOLE, Hole.at(hole.side, false)))) {
                    return InteractionResult.FAIL;
                }
                var resin = new ItemStack(ForgeContent.RESIN.get(), 1 + level.random.nextInt(3));
                Block.popResourceFromFace(level, pos, hole.side, resin);
                level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1, 1);
                if (player != null) {
                    if (cost == 0) {
                        tool.hurtAndBreak(
                                1, player, holder -> holder.broadcastBreakEvent(context.getHand()));
                    } else if (!player.isCreative()) {
                        com.industrialage.ia2.forge.equipment.ForgeGear.spend(tool, cost);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    enum Hole implements StringRepresentable {
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
            for (Hole hole : values()) {
                if (hole.side == side && hole.wet == wet) {
                    return hole;
                }
            }
            return NONE;
        }

        @Override
        public String getSerializedName() {
            return side == null ? "none" : (wet ? "wet_" : "dry_") + side.getName();
        }
    }
}
