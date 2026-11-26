package com.industrialage.ia2.world;

import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

final class WorldBlocks {
    private WorldBlocks() {}

    static void install() {
        for (String metal : List.of("lead", "uranium")) {
            ordinary(
                    metal + "_ore",
                    p ->
                            new Block(
                                    p.strength(3, 3)
                                            .sound(SoundType.STONE)
                                            .requiresCorrectToolForDrops()));
            ordinary(
                    "deepslate_" + metal + "_ore",
                    p ->
                            new Block(
                                    p.strength(4.5F, 3)
                                            .sound(SoundType.DEEPSLATE)
                                            .requiresCorrectToolForDrops()));
        }
        for (String material : List.of("tin", "bronze", "lead", "silver", "steel", "uranium")) {
            ordinary(material + "_block", p -> new Block(metal(p).strength(4, 8)));
        }
        for (String material : List.of("tin", "lead", "uranium")) {
            ordinary("raw_" + material + "_block", p -> new Block(metal(p).strength(5, 6)));
        }
        ordinary(
                "basalt",
                p ->
                        new Block(
                                p.strength(1.25F, 4.2F)
                                        .sound(SoundType.BASALT)
                                        .requiresCorrectToolForDrops()));
        ordinary(
                "reinforced_stone",
                p ->
                        new Block(
                                p.strength(12, 180)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops()));
        ordinary(
                "reinforced_glass",
                p -> WorldNative.glass(p.strength(6, 100).sound(SoundType.GLASS).noOcclusion()));
        ordinary(
                "reinforced_door",
                p -> WorldNative.door(metal(p).strength(8, 180).noOcclusion(), true));
        ordinary(
                "refractory_bricks",
                p ->
                        new Block(
                                p.strength(3, 12)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops()));
        ordinary("iron_fence", p -> WorldNative.ironFence(metal(p).noOcclusion()));
        ordinary("rubber_sheet", p -> new Sheet(p.strength(0.3F).sound(SoundType.WOOL), false));
        ordinary(
                "resin_sheet", p -> new Sheet(p.strength(0.3F).sound(SoundType.HONEY_BLOCK), true));
        ordinary("wool_sheet", p -> new Sheet(p.strength(0.2F).sound(SoundType.WOOL), false));
        for (String name :
                List.of(
                        "wooden_scaffold",
                        "reinforced_wooden_scaffold",
                        "iron_scaffold",
                        "reinforced_iron_scaffold")) {
            boolean iron = name.contains("iron");
            boolean reinforced = name.startsWith("reinforced");
            ordinary(
                    name,
                    p ->
                            new Scaffold(
                                    p.strength(iron ? 1.2F : 0.3F)
                                            .sound(iron ? SoundType.METAL : SoundType.WOOD)
                                            .noOcclusion()
                                            .dynamicShape(),
                                    reinforced));
        }

        ordinary("rubber_planks", p -> new Block(wood(p)));
        ordinary("rubber_wood", p -> new RotatedPillarBlock(wood(p)));
        ordinary("stripped_rubber_log", p -> new RotatedPillarBlock(wood(p)));
        ordinary("stripped_rubber_wood", p -> new RotatedPillarBlock(wood(p)));
        ordinary("rubber_slab", p -> new SlabBlock(wood(p).noOcclusion()));
        ordinary(
                "rubber_stairs",
                p ->
                        new StairBlock(
                                WorldNative.block("ic2:rubber_planks").defaultBlockState(),
                                wood(p).noOcclusion()));
        ordinary("rubber_fence", p -> new FenceBlock(wood(p).noOcclusion()));
        ordinary("rubber_fence_gate", p -> WorldNative.gate(wood(p).noOcclusion()));
        ordinary("rubber_button", p -> WorldNative.button(wood(p).strength(0.5F).noOcclusion()));
        ordinary(
                "rubber_pressure_plate",
                p -> WorldNative.plate(wood(p).strength(0.5F).noOcclusion()));
        ordinary("rubber_door", p -> WorldNative.door(wood(p).strength(3).noOcclusion(), false));
        ordinary("rubber_trapdoor", p -> WorldNative.trapdoor(wood(p).strength(3).noOcclusion()));

        ordinary(
                "foam",
                p ->
                        new WetFoam(
                                WorldNative.noCollision(p)
                                        .strength(0.3F)
                                        .sound(SoundType.WOOL)
                                        .noOcclusion()
                                        .randomTicks()));
        WorldPlatform.registerBlock(
                "obscured_wall",
                p -> new ObscuredWall(p.strength(2, 12).sound(SoundType.STONE)),
                false);
        for (DyeColor color : DyeColor.values()) {
            ordinary(
                    color.getName() + "_wall",
                    p -> new PaintedWall(p.strength(2, 12).sound(SoundType.STONE)));
            WorldPlatform.registerItem(
                    color.getName() + "_painter", p -> new Item(p.durability(32)));
        }
        WorldPlatform.registerItem("painter", Item::new);
        WorldPlatform.registerItem("water_sheet", Item::new);
        WorldPlatform.registerItem("lava_sheet", Item::new);
        WorldPlatform.registerItem("foam_sprayer", p -> new FoamEquip(p.stacksTo(1), 8000));
        WorldPlatform.registerItem(
                "cf_pack", p -> new FoamEquip(WorldNative.packProperties(p), 80_000));

        ordinary(
                "mining_pipe",
                p -> new MiningPipe(p.strength(0.5F).sound(SoundType.METAL).noOcclusion()));
        WorldPlatform.registerBlock(
                "mining_pipe_tip",
                p -> new MiningPipe(p.strength(0.5F).sound(SoundType.METAL).noOcclusion()),
                false);
        for (String name :
                List.of(
                        "blank",
                        "chilling",
                        "cultivation",
                        "desertification",
                        "flatification",
                        "irrigation",
                        "mushroom")) {
            WorldPlatform.registerItem(name + "_tfbp", p -> new Item(p.stacksTo(1)));
        }
    }

    private static void ordinary(String name, Function<Block.Properties, Block> factory) {
        WorldPlatform.registerBlock(name, factory, true);
    }

    static Block.Properties metal(Block.Properties settings) {
        return settings.strength(2.5F, 10).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    private static Block.Properties wood(Block.Properties settings) {
        return WorldNative.flammable(settings).strength(2, 3).sound(SoundType.WOOD);
    }

    static final class WetFoam extends Block {
        static final BooleanProperty REINFORCED = BooleanProperty.create("reinforced");

        WetFoam(Properties settings) {
            super(settings);
            registerDefaultState(defaultBlockState().setValue(REINFORCED, false));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(REINFORCED);
        }

        @Override
        public void randomTick(
                BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (random.nextInt(3) == 0) harden(level, pos, state);
        }

        boolean harden(ServerLevel level, BlockPos pos, BlockState state) {
            String hard = state.getValue(REINFORCED) ? "ic2:reinforced_stone" : "ic2:white_wall";
            return level.setBlockAndUpdate(pos, WorldNative.block(hard).defaultBlockState());
        }
    }

    static final class PaintedWall extends Block {
        PaintedWall(Properties settings) {
            super(settings);
        }
    }

    static final class Scaffold extends net.minecraft.world.level.block.ScaffoldingBlock {
        private final boolean reinforced;

        Scaffold(Properties settings, boolean reinforced) {
            super(settings);
            this.reinforced = reinforced;
        }

        @Override
        public BlockState getStateForPlacement(
                net.minecraft.world.item.context.BlockPlaceContext context) {
            var level = context.getLevel();
            var pos = context.getClickedPos();
            int distance = support(level, pos);
            return defaultBlockState()
                    .setValue(DISTANCE, distance)
                    .setValue(
                            BOTTOM,
                            distance > 0
                                    && !(level.getBlockState(pos.below()).getBlock()
                                            instanceof Scaffold))
                    .setValue(
                            WATERLOGGED,
                            level.getFluidState(pos)
                                    .is(net.minecraft.world.level.material.Fluids.WATER));
        }

        @Override
        public boolean canSurvive(
                BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
            return support(level, pos) < 7;
        }

        @Override
        public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            int distance = support(level, pos);
            if (distance >= 7) {
                level.destroyBlock(pos, true);
                return;
            }
            boolean bottom =
                    distance > 0
                            && !(level.getBlockState(pos.below()).getBlock() instanceof Scaffold);
            BlockState stable = state.setValue(DISTANCE, distance).setValue(BOTTOM, bottom);
            if (stable != state) level.setBlockAndUpdate(pos, stable);
        }

        private int support(BlockGetter level, BlockPos pos) {
            var belowPos = pos.below();
            var below = level.getBlockState(belowPos);
            if (below.getBlock() instanceof Scaffold) return below.getValue(DISTANCE);
            if (below.isFaceSturdy(level, belowPos, net.minecraft.core.Direction.UP)) return 0;
            int distance = 7;
            for (var side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                var other = level.getBlockState(pos.relative(side));
                if (other.getBlock() instanceof Scaffold)
                    distance = Math.min(distance, other.getValue(DISTANCE) + 1);
            }
            return distance > (reinforced ? 6 : 4) ? 7 : distance;
        }
    }

    private static final class Sheet extends CarpetBlock {
        Sheet(Properties settings, boolean sticky) {
            super(sticky ? settings.speedFactor(0.65F) : settings);
        }
    }

    static final class MiningPipe extends WorldNative.MiningPipeBase {
        static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
        private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE =
                box(5, 0, 5, 11, 16, 11);

        MiningPipe(Properties settings) {
            super(settings);
            registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(WATERLOGGED);
        }

        @Override
        public FluidState getFluidState(BlockState state) {
            return state.getValue(WATERLOGGED)
                    ? Fluids.WATER.getSource(false)
                    : super.getFluidState(state);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState()
                    .setValue(
                            WATERLOGGED,
                            context.getLevel().getFluidState(context.getClickedPos()).getType()
                                    == Fluids.WATER);
        }

        @Override
        public net.minecraft.world.phys.shapes.VoxelShape getShape(
                BlockState state,
                BlockGetter level,
                BlockPos pos,
                net.minecraft.world.phys.shapes.CollisionContext context) {
            return SHAPE;
        }
    }
}
