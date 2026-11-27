package com.industrialage.ia2.gametest;

import com.industrialage.ia2.logisticstest.PipeNozzleChecks;
import com.industrialage.ia2.slottest.SlotRulesChecks;
import com.industrialage.ia2.wrenchtest.WrenchHits;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;

/** Proves the published capabilities and player tools across a cold process restart. */
final class TransportChecks {
    private static final BlockPos ITEM_PIPE = new BlockPos(19, 98, 26);
    private static final BlockPos BEND = ITEM_PIPE.east();
    private static final BlockPos OUTPUT = BEND.south();
    private static final BlockPos BARREL = OUTPUT.east();
    private static final BlockPos FLUID_PIPE = new BlockPos(25, 98, 26);
    private static final BlockPos SUPPLY = FLUID_PIPE.west();
    private static final BlockPos TANK = FLUID_PIPE.south();
    private static final BlockPos WET_ITEM_PIPE = new BlockPos(3, 166, 3);
    private static final BlockPos WET_FLUID_PIPE = new BlockPos(3, 166, 7);
    private static final BlockPos WATER_FLOW_SOURCE = new BlockPos(9, 166, 3);
    private static final BlockPos FALL_WATER_ITEM = new BlockPos(11, 166, 7);
    private static final BlockPos FALL_WATER_FLUID = new BlockPos(8, 166, 7);
    private static final BlockPos LAVA_FLOW_SOURCE = new BlockPos(9, 166, 11);
    private static final BlockPos SOLO_STRAIGHT_PIPE = new BlockPos(19, 98, 19);
    private static final BlockPos SOLO_BEND_PIPE = new BlockPos(24, 98, 19);
    private static final BlockPos SOLO_LATE_PIPE = new BlockPos(29, 98, 19);
    private static final BlockPos AUTO_UP_SRC = new BlockPos(18, 140, 18);
    private static final BlockPos AUTO_UP_OPPOSING_SRC = new BlockPos(21, 140, 18);
    private static final BlockPos AUTO_UP_TOPDOWN_SRC = new BlockPos(29, 140, 22);
    private static final BlockPos AUTO_CLIMB_SRC = new BlockPos(24, 140, 18);
    private static final BlockPos AUTO_DIP_UP_SRC = new BlockPos(18, 142, 22);
    private static final BlockPos AUTO_Z_SRC = new BlockPos(23, 140, 22);
    private static final BlockPos AUTO_TEE_SRC = new BlockPos(18, 140, 26);
    private static final BlockPos AUTO_U_SRC = new BlockPos(18, 145, 25);
    private static final BlockPos NOZZLE_CLIMB_SRC = new BlockPos(24, 140, 26);
    private static final BlockPos NOZZLE_EXT_ONLY_SRC = new BlockPos(24, 144, 22);
    private static final BlockPos COLD_CORNER_SRC = new BlockPos(27, 144, 18);
    private final PipeNozzleChecks nozzles =
            new PipeNozzleChecks(
                    new BlockPos(3, 132, 3),
                    NativeGameplayFixture::block,
                    id -> new ItemStack(NativeGameplayFixture.item(id)),
                    new PipeNozzleChecks.Automation() {
                        @Override
                        public ItemStack itemCargo(ServerLevel level, BlockPos pos) {
                            return NativeTransferAccess.cargo(level, pos, Direction.SOUTH);
                        }

                        @Override
                        public int seedWater(ServerLevel level, BlockPos pos, int amount) {
                            return NativeTransferAccess.fill(
                                    level, pos, Direction.NORTH, water(amount), false);
                        }

                        @Override
                        public boolean reverseRejected(
                                ServerLevel level, BlockPos pos, boolean fluid, ItemStack sample) {
                            if (fluid) {
                                return NativeTransferAccess.fill(
                                                        level,
                                                        pos,
                                                        Direction.SOUTH,
                                                        water(1),
                                                        false)
                                                == 0
                                        && NativeTransferAccess.drain(
                                                        level, pos, Direction.NORTH, 1, false)
                                                == 0;
                            }
                            return NativeTransferAccess.insert(
                                                    level, pos, Direction.SOUTH, sample, false)
                                            == 0
                                    && NativeTransferAccess.extract(
                                                    level, pos, Direction.NORTH, 1, false)
                                            == 0;
                        }
                    });
    private final SlotRulesChecks slotRules =
            new SlotRulesChecks(
                    new BlockPos(3, 151, 3),
                    NativeGameplayFixture::block,
                    id -> new ItemStack(NativeGameplayFixture.item(id)),
                    new SlotRulesHost(),
                    1);
    private boolean openedOutlet;
    private boolean releasedBackpressure;
    private boolean finishedFluid;
    private boolean finishedItems;
    private boolean finishedSingleSegment;
    private boolean finishedCornerAndVertical;
    private boolean placedLateTarget;
    private boolean checkedWaterlogging;
    private boolean seededColdCorner;

    void writeStart(ServerLevel level, BiConsumer<String, Boolean> check) {
        var player = FakePlayerFactory.getMinecraft(level);
        level.setChunkForced(0, 0, true);
        level.getChunk(0, 0);
        nozzles.place(level, player, check);
        slotRules.place(level, player, check);
        NativeGameplayFixture.place(level, player, "steel_item_pipe_small", ITEM_PIPE);
        rotate(level, ITEM_PIPE, Direction.EAST);
        NativeGameplayFixture.place(level, player, "bronze_fluid_pipe_medium", FLUID_PIPE);
        check.accept(
                "item-pipe-simulation",
                NativeTransferAccess.insert(
                                        level, ITEM_PIPE, Direction.WEST, namedDiamonds(23), true)
                                == 4
                        && NativeTransferAccess.cargo(level, ITEM_PIPE, Direction.WEST).isEmpty());
        check.accept(
                "item-pipe-capacity-and-direction",
                NativeTransferAccess.insert(
                                        level, ITEM_PIPE, Direction.WEST, namedDiamonds(23), false)
                                == 4
                        && NativeTransferAccess.extract(level, ITEM_PIPE, Direction.WEST, 4, false)
                                == 0
                        && NativeTransferAccess.extract(level, ITEM_PIPE, Direction.EAST, 4, true)
                                == 4
                        && NativeTransferAccess.cargo(level, ITEM_PIPE, Direction.EAST).getCount()
                                == 4);
        check.accept(
                "fluid-simulation",
                NativeTransferAccess.fill(level, FLUID_PIPE, Direction.WEST, water(5_000), true)
                                == 2_400
                        && volume(level, FLUID_PIPE) == 0);
        check.accept(
                "fluid-seeded",
                NativeTransferAccess.fill(level, FLUID_PIPE, Direction.WEST, water(717), false)
                                == 717
                        && NativeTransferAccess.fill(
                                        level,
                                        FLUID_PIPE,
                                        Direction.EAST,
                                        new FluidStack(Fluids.LAVA, 1),
                                        false)
                                == 0);
        var cover =
                click(
                        level,
                        FLUID_PIPE,
                        Direction.WEST,
                        new ItemStack(NativeGameplayFixture.item("pump_cover_lv")));
        var duplicate =
                click(
                        level,
                        FLUID_PIPE,
                        Direction.WEST,
                        new ItemStack(NativeGameplayFixture.item("pump_cover_mv")));
        check.accept(
                "pump-cover-exact-consumption",
                cover.isEmpty()
                        && duplicate.getCount() == 1
                        && NativeTransferAccess.drain(level, FLUID_PIPE, Direction.WEST, 717, true)
                                == 0
                        && NativeTransferAccess.drain(level, FLUID_PIPE, Direction.EAST, 717, true)
                                == 717
                        && volume(level, FLUID_PIPE) == 717);
        rotate(level, FLUID_PIPE, Direction.SOUTH);
        check.accept(
                "valve-closed",
                NativeTransferAccess.fill(level, FLUID_PIPE, Direction.SOUTH, water(1), false)
                        == 0);
        setupWaterlogChecks(level, player, check);
        setupColdCornerSourceCheck(level);
    }

    private static void setupColdCornerSourceCheck(ServerLevel level) {
        var pipeBlock = NativeGameplayFixture.block("bronze_item_pipe_small");
        level.setBlockAndUpdate(COLD_CORNER_SRC, Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, COLD_CORNER_SRC.east(), Direction.EAST);
        setPipe(level, pipeBlock, COLD_CORNER_SRC.east().above(), Direction.UP);
        ((Container) level.getBlockEntity(COLD_CORNER_SRC))
                .setItem(0, new ItemStack(Items.AMETHYST_SHARD, 8));
    }

    boolean writeTick(ServerLevel level, BiConsumer<String, Boolean> check) {
        if (!seededColdCorner
                && NativeTransferAccess.cargo(level, COLD_CORNER_SRC.east(), Direction.EAST)
                                .getCount()
                        == 4) {
            seededColdCorner = true;
        }
        if (!checkedWaterlogging
                && !level.getFluidState(WET_ITEM_PIPE.east()).isEmpty()
                && !level.getFluidState(WET_FLUID_PIPE.east()).isEmpty()
                && level.getFluidState(WATER_FLOW_SOURCE.north()).getType() == Fluids.FLOWING_WATER
                && level.getFluidState(LAVA_FLOW_SOURCE.north()).getType() == Fluids.FLOWING_LAVA) {
            var tinyItem = NativeGameplayFixture.block("bronze_item_pipe_tiny");
            var tinyFluid = NativeGameplayFixture.block("steel_fluid_pipe_tiny");
            check.accept(
                    "waterlogged-pipes-spread-water",
                    level.getFluidState(WET_ITEM_PIPE.east()).getType() == Fluids.FLOWING_WATER
                            && level.getFluidState(WET_FLUID_PIPE.east()).getType()
                                    == Fluids.FLOWING_WATER);
            check.accept(
                    "flowing-water-preserves-pipes",
                    isDryPipe(level, WATER_FLOW_SOURCE.east(), tinyItem)
                            && isDryPipe(level, WATER_FLOW_SOURCE.west(), tinyFluid)
                            && isDryPipe(level, FALL_WATER_ITEM, tinyItem)
                            && isDryPipe(level, FALL_WATER_FLUID, tinyFluid));
            check.accept(
                    "flowing-lava-preserves-pipes",
                    isDryPipe(level, LAVA_FLOW_SOURCE.east(), tinyItem)
                            && isDryPipe(level, LAVA_FLOW_SOURCE.west(), tinyFluid));
            checkedWaterlogging = true;
        }
        return nozzles.waitForBackpressure(level, check)
                && slotRules.tick(level, check)
                && checkedWaterlogging
                && seededColdCorner;
    }

    void readStart(ServerLevel level, BiConsumer<String, Boolean> check) {
        level.getChunk(0, 0);
        checkColdWaterlogging(level, check);
        nozzles.letDeliver(level, FakePlayerFactory.getMinecraft(level), "cold", check);
        var sample = NativeTransferAccess.cargo(level, ITEM_PIPE, Direction.WEST);
        check.accept(
                "cold-pipe-item-components",
                sample.getCount() == 4
                        && ItemStack.isSameItemSameComponents(sample, namedDiamonds(1)));
        check.accept(
                "cold-pipe-fluid-valve-and-cover",
                volume(level, FLUID_PIPE) == 717
                        && NativeTransferAccess.drain(level, FLUID_PIPE, Direction.WEST, 717, true)
                                == 0
                        && NativeTransferAccess.fill(
                                        level, FLUID_PIPE, Direction.SOUTH, water(1), true)
                                == 0);
        var player = FakePlayerFactory.getMinecraft(level);
        NativeGameplayFixture.place(level, player, "steel_item_pipe_small", BEND);
        rotate(level, BEND, Direction.SOUTH);
        NativeGameplayFixture.place(level, player, "steel_item_pipe_small", OUTPUT);
        rotate(level, OUTPUT, Direction.EAST);
        level.setBlockAndUpdate(BARREL, Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(ITEM_PIPE.west(), Blocks.BARREL.defaultBlockState());
        ((Container) level.getBlockEntity(ITEM_PIPE.west())).setItem(0, namedDiamonds(19));
        NativeGameplayFixture.place(level, player, "bronze_tank", SUPPLY);
        NativeGameplayFixture.place(level, player, "bronze_tank", TANK);
        check.accept(
                "tank-seeded",
                NativeTransferAccess.fill(level, SUPPLY, Direction.UP, water(512), false) == 512);
        setupSingleSegmentExtractionChecks(level, check);
        setupCornerAndVerticalRoutingChecks(level, player, check);
    }

    boolean tick(ServerLevel level, BiConsumer<String, Boolean> check) {
        if (!finishedItems) {
            var barrel = (Container) level.getBlockEntity(BARREL);
            var result = barrel.getItem(0);
            if (result.getCount() >= 23) {
                check.accept(
                        "item-pipe-bends-and-components",
                        result.getCount() == 23
                                && ItemStack.isSameItemSameComponents(result, namedDiamonds(1))
                                && ((Container) level.getBlockEntity(ITEM_PIPE.west())).isEmpty()
                                && NativeTransferAccess.cargo(level, ITEM_PIPE, Direction.WEST)
                                        .isEmpty());
                finishedItems = true;
            }
        }
        if (!finishedSingleSegment) {
            tickSingleSegmentExtractionChecks(level, check);
        }
        if (!finishedCornerAndVertical) {
            tickCornerAndVerticalRoutingChecks(level, check);
        }
        if (!finishedFluid) {
            tickFluid(level, check);
        }
        return nozzles.delivered(level, check)
                && finishedItems
                && finishedSingleSegment
                && finishedCornerAndVertical
                && finishedFluid;
    }

    String diagnostic(ServerLevel level) {
        return "supply="
                + volume(level, SUPPLY)
                + ", pipe="
                + volume(level, FLUID_PIPE)
                + ", tank="
                + volume(level, TANK)
                + ", outlet="
                + openedOutlet
                + ", released="
                + releasedBackpressure
                + ", items done="
                + finishedItems
                + ", single-segment done="
                + finishedSingleSegment
                + ", corner-vertical done="
                + finishedCornerAndVertical;
    }

    private void setupSingleSegmentExtractionChecks(
            ServerLevel level, BiConsumer<String, Boolean> check) {
        var pipeBlock = NativeGameplayFixture.block("bronze_item_pipe_small");
        // 1. Straight single-segment pipe placed with OUT facing empty NORTH air.
        level.setBlockAndUpdate(SOLO_STRAIGHT_PIPE.west(), Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(SOLO_STRAIGHT_PIPE.east(), Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(
                SOLO_STRAIGHT_PIPE,
                pipeBlock
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.NORTH));
        click(
                level,
                SOLO_STRAIGHT_PIPE,
                Direction.WEST,
                new ItemStack(NativeGameplayFixture.item("pipe_extraction_nozzle")));
        check.accept(
                "single-segment-straight-nozzle-redirects-air-out",
                level.getBlockState(SOLO_STRAIGHT_PIPE).getValue(BlockStateProperties.FACING)
                        == Direction.EAST);
        ((Container) level.getBlockEntity(SOLO_STRAIGHT_PIPE.west()))
                .setItem(0, new ItemStack(Items.IRON_INGOT, 6));

        // 2. 90-degree single-segment pipe with OUT facing empty EAST air + bystander chest NORTH.
        level.setBlockAndUpdate(SOLO_BEND_PIPE.west(), Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(SOLO_BEND_PIPE.south(), Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(
                SOLO_BEND_PIPE,
                pipeBlock
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.EAST));
        click(
                level,
                SOLO_BEND_PIPE,
                Direction.WEST,
                new ItemStack(NativeGameplayFixture.item("pipe_extraction_nozzle")));
        check.accept(
                "single-segment-bend-nozzle-redirects-to-target",
                level.getBlockState(SOLO_BEND_PIPE).getValue(BlockStateProperties.FACING)
                        == Direction.SOUTH);
        // Place bystander barrel opposite OUT (NORTH): must not be extracted when nozzle is active.
        level.setBlockAndUpdate(SOLO_BEND_PIPE.north(), Blocks.BARREL.defaultBlockState());
        ((Container) level.getBlockEntity(SOLO_BEND_PIPE.north()))
                .setItem(0, new ItemStack(Items.GOLD_INGOT, 3));
        ((Container) level.getBlockEntity(SOLO_BEND_PIPE.west()))
                .setItem(0, new ItemStack(Items.COPPER_INGOT, 5));

        // 3. Target placed AFTER extraction nozzle while cargo is already buffered in the pipe.
        level.setBlockAndUpdate(SOLO_LATE_PIPE.west(), Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(
                SOLO_LATE_PIPE,
                pipeBlock
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.NORTH));
        click(
                level,
                SOLO_LATE_PIPE,
                Direction.WEST,
                new ItemStack(NativeGameplayFixture.item("pipe_extraction_nozzle")));
        ((Container) level.getBlockEntity(SOLO_LATE_PIPE.west()))
                .setItem(0, new ItemStack(Items.EMERALD, 4));
    }

    private void tickSingleSegmentExtractionChecks(
            ServerLevel level, BiConsumer<String, Boolean> check) {
        if (!placedLateTarget) {
            if (NativeTransferAccess.cargo(level, SOLO_LATE_PIPE, Direction.NORTH).getCount()
                    == 4) {
                level.setBlockAndUpdate(SOLO_LATE_PIPE.south(), Blocks.BARREL.defaultBlockState());
                placedLateTarget = true;
            }
            return;
        }
        var straightDst = (Container) level.getBlockEntity(SOLO_STRAIGHT_PIPE.east());
        var bendDst = (Container) level.getBlockEntity(SOLO_BEND_PIPE.south());
        var bendBystander = (Container) level.getBlockEntity(SOLO_BEND_PIPE.north());
        var lateDst = (Container) level.getBlockEntity(SOLO_LATE_PIPE.south());
        if (straightDst.getItem(0).getCount() < 6
                || bendDst.getItem(0).getCount() < 5
                || lateDst.getItem(0).getCount() < 4) {
            return;
        }
        check.accept(
                "single-segment-extraction-nozzle-delivers-all-layouts",
                straightDst.getItem(0).is(Items.IRON_INGOT)
                        && straightDst.getItem(0).getCount() == 6
                        && ((Container) level.getBlockEntity(SOLO_STRAIGHT_PIPE.west())).isEmpty()
                        && bendDst.getItem(0).is(Items.COPPER_INGOT)
                        && bendDst.getItem(0).getCount() == 5
                        && ((Container) level.getBlockEntity(SOLO_BEND_PIPE.west())).isEmpty()
                        && bendBystander.getItem(0).is(Items.GOLD_INGOT)
                        && bendBystander.getItem(0).getCount() == 3
                        && lateDst.getItem(0).is(Items.EMERALD)
                        && lateDst.getItem(0).getCount() == 4
                        && ((Container) level.getBlockEntity(SOLO_LATE_PIPE.west())).isEmpty()
                        && level.getBlockState(SOLO_LATE_PIPE).getValue(BlockStateProperties.FACING)
                                == Direction.SOUTH);
        finishedSingleSegment = true;
    }

    private void setupCornerAndVerticalRoutingChecks(
            ServerLevel level,
            net.minecraft.server.level.ServerPlayer player,
            BiConsumer<String, Boolean> check) {
        var pipeBlock = NativeGameplayFixture.block("bronze_item_pipe_small");

        // 1. Auto-routing straight UP placed via real BlockPlaceContext (top pipe clicked onto
        // overhead barrel bottom must face UP into the barrel, not DOWN into the pipe).
        level.setBlockAndUpdate(AUTO_UP_SRC, Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(AUTO_UP_SRC.above(4), Blocks.BARREL.defaultBlockState());
        placePipeAgainst(level, player, pipeBlock, AUTO_UP_SRC, Direction.UP);
        placePipeAgainst(level, player, pipeBlock, AUTO_UP_SRC.above(), Direction.UP);
        placePipeAgainst(level, player, pipeBlock, AUTO_UP_SRC.above(4), Direction.DOWN);
        check.accept(
                "vertical-up-placed-against-overhead-sink-faces-up",
                level.getBlockState(AUTO_UP_SRC.above(3)).getValue(BlockStateProperties.FACING)
                        == Direction.UP);
        ((Container) level.getBlockEntity(AUTO_UP_SRC))
                .setItem(0, new ItemStack(Items.DIAMOND, 12));

        // 2. Auto-routing straight UP where top pipe was placed BEFORE lower pipe (OUT=DOWN
        // opposing lower pipe): must auto-align OUT=UP on delivery and never pull items back.
        level.setBlockAndUpdate(AUTO_UP_OPPOSING_SRC, Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(AUTO_UP_OPPOSING_SRC.above(4), Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, AUTO_UP_OPPOSING_SRC.above(1), Direction.UP);
        setPipe(level, pipeBlock, AUTO_UP_OPPOSING_SRC.above(2), Direction.UP);
        setPipe(level, pipeBlock, AUTO_UP_OPPOSING_SRC.above(3), Direction.DOWN);
        ((Container) level.getBlockEntity(AUTO_UP_OPPOSING_SRC))
                .setItem(0, new ItemStack(Items.EMERALD, 12));

        // 2b. Top-down opposing chain: P1(UP) -> P2(DOWN) -> P3(DOWN) -> Sink (placed P3 under
        // Sink then P2 under P3).
        level.setBlockAndUpdate(AUTO_UP_TOPDOWN_SRC, Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(AUTO_UP_TOPDOWN_SRC.above(4), Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, AUTO_UP_TOPDOWN_SRC.above(1), Direction.UP);
        setPipe(level, pipeBlock, AUTO_UP_TOPDOWN_SRC.above(2), Direction.DOWN);
        setPipe(level, pipeBlock, AUTO_UP_TOPDOWN_SRC.above(3), Direction.DOWN);
        ((Container) level.getBlockEntity(AUTO_UP_TOPDOWN_SRC))
                .setItem(0, new ItemStack(Items.PRISMARINE_SHARD, 12));

        // Cold-reload corner source check: P1 held 4 shards + external_intake=WEST across save/load
        // with OUT=EAST (air). Attaching destination barrel at P2.east() must deliver all 8 shards
        // without flipping P1.OUT back into COLD_CORNER_SRC.
        check.accept(
                "cold-corner-source-retained-cargo-and-out",
                NativeTransferAccess.cargo(level, COLD_CORNER_SRC.east(), Direction.EAST).getCount()
                                == 4
                        && level.getBlockState(COLD_CORNER_SRC.east())
                                        .getValue(BlockStateProperties.FACING)
                                == Direction.EAST);
        level.setBlockAndUpdate(COLD_CORNER_SRC.east(2).above(), Blocks.BARREL.defaultBlockState());

        // 3. Auto-routing horizontal -> UP -> horizontal (terminal pipe OUT=UP into air, sink on
        // EAST).
        level.setBlockAndUpdate(AUTO_CLIMB_SRC, Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, AUTO_CLIMB_SRC.east(), Direction.EAST);
        setPipe(level, pipeBlock, AUTO_CLIMB_SRC.east(2), Direction.EAST);
        setPipe(level, pipeBlock, AUTO_CLIMB_SRC.east(2).above(), Direction.UP);
        level.setBlockAndUpdate(AUTO_CLIMB_SRC.east(3).above(), Blocks.BARREL.defaultBlockState());
        ((Container) level.getBlockEntity(AUTO_CLIMB_SRC))
                .setItem(0, new ItemStack(Items.IRON_INGOT, 16));

        // 4. Auto-routing DOWN -> side -> UP (with a 1-block gap so the rising leg is a true
        // terminal pipe, zero nozzles, unrotated terminal OUT=UP into air).
        level.setBlockAndUpdate(AUTO_DIP_UP_SRC, Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, AUTO_DIP_UP_SRC.below(), Direction.DOWN);
        setPipe(level, pipeBlock, AUTO_DIP_UP_SRC.below(2), Direction.DOWN);
        setPipe(level, pipeBlock, AUTO_DIP_UP_SRC.below(2).east(), Direction.EAST);
        setPipe(level, pipeBlock, AUTO_DIP_UP_SRC.below(2).east(2), Direction.EAST);
        setPipe(level, pipeBlock, AUTO_DIP_UP_SRC.below().east(2), Direction.UP);
        level.setBlockAndUpdate(AUTO_DIP_UP_SRC.below().east(3), Blocks.BARREL.defaultBlockState());
        ((Container) level.getBlockEntity(AUTO_DIP_UP_SRC))
                .setItem(0, new ItemStack(Items.COPPER_INGOT, 12));

        // 5. Auto-routing horizontal Z-turn + L-turn (zero nozzles).
        level.setBlockAndUpdate(AUTO_Z_SRC, Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, AUTO_Z_SRC.east(), Direction.EAST);
        setPipe(level, pipeBlock, AUTO_Z_SRC.east(2), Direction.EAST);
        setPipe(level, pipeBlock, AUTO_Z_SRC.east(2).north(), Direction.NORTH);
        setPipe(level, pipeBlock, AUTO_Z_SRC.east(3).north(), Direction.EAST);
        level.setBlockAndUpdate(AUTO_Z_SRC.east(3).north(2), Blocks.BARREL.defaultBlockState());
        ((Container) level.getBlockEntity(AUTO_Z_SRC))
                .setItem(0, new ItemStack(Items.GOLD_INGOT, 12));

        // 6. Auto-routing T-junction splitting to two terminal barrels with backpressure failover
        // (zero nozzles): north barrel only has room for 6 coal so overflow must route south.
        level.setBlockAndUpdate(AUTO_TEE_SRC, Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, AUTO_TEE_SRC.east(), Direction.EAST);
        setPipe(level, pipeBlock, AUTO_TEE_SRC.east(2), Direction.EAST);
        setPipe(level, pipeBlock, AUTO_TEE_SRC.east(2).north(), Direction.NORTH);
        setPipe(level, pipeBlock, AUTO_TEE_SRC.east(2).south(), Direction.SOUTH);
        level.setBlockAndUpdate(AUTO_TEE_SRC.east(3).north(), Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(AUTO_TEE_SRC.east(3).south(), Blocks.BARREL.defaultBlockState());
        var teeNorthBarrel = (Container) level.getBlockEntity(AUTO_TEE_SRC.east(3).north());
        teeNorthBarrel.setItem(0, new ItemStack(Items.COAL, 58));
        for (int slot = 1; slot < teeNorthBarrel.getContainerSize(); slot++) {
            teeNorthBarrel.setItem(slot, new ItemStack(Items.STICK, 1));
        }
        ((Container) level.getBlockEntity(AUTO_TEE_SRC)).setItem(0, new ItemStack(Items.COAL, 16));

        // 6b. Auto-routing horizontal U-turn (EAST -> SOUTH -> WEST with a 1-block gap so there is
        // no shortcut, terminal OUT=SOUTH into air, zero nozzles).
        level.setBlockAndUpdate(AUTO_U_SRC, Blocks.BARREL.defaultBlockState());
        setPipe(level, pipeBlock, AUTO_U_SRC.east(), Direction.EAST);
        setPipe(level, pipeBlock, AUTO_U_SRC.east(2), Direction.EAST);
        setPipe(level, pipeBlock, AUTO_U_SRC.east(2).south(), Direction.SOUTH);
        setPipe(level, pipeBlock, AUTO_U_SRC.east(2).south(2), Direction.SOUTH);
        setPipe(level, pipeBlock, AUTO_U_SRC.east().south(2), Direction.SOUTH);
        level.setBlockAndUpdate(AUTO_U_SRC.south(2), Blocks.BARREL.defaultBlockState());
        ((Container) level.getBlockEntity(AUTO_U_SRC)).setItem(0, new ItemStack(Items.QUARTZ, 12));

        // 7. With extraction + injection nozzles across corner + UP climb + backward OUT faces +
        // continuous multi-batch flow.
        level.setBlockAndUpdate(NOZZLE_CLIMB_SRC, Blocks.BARREL.defaultBlockState());
        var nP1 = NOZZLE_CLIMB_SRC.east();
        var nP2 = NOZZLE_CLIMB_SRC.east(2);
        var nP3 = nP2.above();
        var nP4 = nP3.south();
        var nDst = nP4.east();
        setPipe(level, pipeBlock, nP1, Direction.WEST);
        setPipe(level, pipeBlock, nP2, Direction.WEST);
        setPipe(level, pipeBlock, nP3, Direction.DOWN);
        setPipe(level, pipeBlock, nP4, Direction.NORTH);
        level.setBlockAndUpdate(nDst, Blocks.BARREL.defaultBlockState());
        click(
                level,
                nP1,
                Direction.WEST,
                new ItemStack(NativeGameplayFixture.item("pipe_extraction_nozzle")));
        click(
                level,
                nP4,
                Direction.EAST,
                new ItemStack(NativeGameplayFixture.item("pipe_injection_nozzle")));
        check.accept(
                "injection-nozzle-clears-backward-pipe-out",
                level.getBlockState(nP4).getValue(BlockStateProperties.FACING) == Direction.EAST);
        // Attempting to install a nozzle onto a pipe-to-pipe face must be ignored and keep the
        // nozzle item.
        var rejectedPipeNozzle = new ItemStack(NativeGameplayFixture.item("pipe_injection_nozzle"));
        var pipeFaceResult = useItemOnPipe(level, player, nP2, Direction.WEST, rejectedPipeNozzle);
        check.accept(
                "nozzle-rejected-on-pipe-to-pipe-face",
                !pipeFaceResult.consumesAction() && player.getMainHandItem().getCount() == 1);
        ((Container) level.getBlockEntity(NOZZLE_CLIMB_SRC))
                .setItem(0, new ItemStack(Items.LAPIS_LAZULI, 24));

        // 8. Extraction nozzle ONLY (no injection nozzle) across horizontal -> UP -> corner.
        level.setBlockAndUpdate(NOZZLE_EXT_ONLY_SRC, Blocks.BARREL.defaultBlockState());
        var eP1 = NOZZLE_EXT_ONLY_SRC.east();
        var eP2 = NOZZLE_EXT_ONLY_SRC.east(2);
        var eP3 = eP2.above();
        var eDst = eP3.south();
        setPipe(level, pipeBlock, eP1, Direction.NORTH);
        setPipe(level, pipeBlock, eP2, Direction.EAST);
        setPipe(level, pipeBlock, eP3, Direction.UP);
        level.setBlockAndUpdate(eDst, Blocks.BARREL.defaultBlockState());
        click(
                level,
                eP1,
                Direction.WEST,
                new ItemStack(NativeGameplayFixture.item("pipe_extraction_nozzle")));
        ((Container) level.getBlockEntity(NOZZLE_EXT_ONLY_SRC))
                .setItem(0, new ItemStack(Items.REDSTONE, 16));
    }

    private void tickCornerAndVerticalRoutingChecks(
            ServerLevel level, BiConsumer<String, Boolean> check) {
        var upDst = (Container) level.getBlockEntity(AUTO_UP_SRC.above(4));
        var oppDst = (Container) level.getBlockEntity(AUTO_UP_OPPOSING_SRC.above(4));
        var topDownDst = (Container) level.getBlockEntity(AUTO_UP_TOPDOWN_SRC.above(4));
        var coldCornerDst = (Container) level.getBlockEntity(COLD_CORNER_SRC.east(2).above());
        var climbDst = (Container) level.getBlockEntity(AUTO_CLIMB_SRC.east(3).above());
        var dipUpDst = (Container) level.getBlockEntity(AUTO_DIP_UP_SRC.below().east(3));
        var zDst = (Container) level.getBlockEntity(AUTO_Z_SRC.east(3).north(2));
        var teeNorthDst = (Container) level.getBlockEntity(AUTO_TEE_SRC.east(3).north());
        var teeSouthDst = (Container) level.getBlockEntity(AUTO_TEE_SRC.east(3).south());
        var uDst = (Container) level.getBlockEntity(AUTO_U_SRC.south(2));
        var nozzleDst = (Container) level.getBlockEntity(NOZZLE_CLIMB_SRC.east(3).above().south());
        var extOnlyDst =
                (Container) level.getBlockEntity(NOZZLE_EXT_ONLY_SRC.east(2).above().south());

        int teeDelivered =
                (teeNorthDst.countItem(Items.COAL) - 58) + teeSouthDst.countItem(Items.COAL);
        if (upDst.countItem(Items.DIAMOND) < 12
                || oppDst.countItem(Items.EMERALD) < 12
                || topDownDst.countItem(Items.PRISMARINE_SHARD) < 12
                || coldCornerDst.countItem(Items.AMETHYST_SHARD) < 8
                || climbDst.countItem(Items.IRON_INGOT) < 16
                || dipUpDst.countItem(Items.COPPER_INGOT) < 12
                || zDst.countItem(Items.GOLD_INGOT) < 12
                || teeDelivered < 16
                || uDst.countItem(Items.QUARTZ) < 12
                || nozzleDst.countItem(Items.LAPIS_LAZULI) < 24
                || extOnlyDst.countItem(Items.REDSTONE) < 16) {
            return;
        }
        check.accept(
                "corner-and-vertical-up-routing-auto-and-nozzles",
                upDst.countItem(Items.DIAMOND) == 12
                        && ((Container) level.getBlockEntity(AUTO_UP_SRC)).isEmpty()
                        && oppDst.countItem(Items.EMERALD) == 12
                        && ((Container) level.getBlockEntity(AUTO_UP_OPPOSING_SRC)).isEmpty()
                        && level.getBlockState(AUTO_UP_OPPOSING_SRC.above(3))
                                        .getValue(BlockStateProperties.FACING)
                                == Direction.UP
                        && topDownDst.countItem(Items.PRISMARINE_SHARD) == 12
                        && ((Container) level.getBlockEntity(AUTO_UP_TOPDOWN_SRC)).isEmpty()
                        && level.getBlockState(AUTO_UP_TOPDOWN_SRC.above(3))
                                        .getValue(BlockStateProperties.FACING)
                                == Direction.UP
                        && coldCornerDst.countItem(Items.AMETHYST_SHARD) == 8
                        && ((Container) level.getBlockEntity(COLD_CORNER_SRC)).isEmpty()
                        && level.getBlockState(COLD_CORNER_SRC.east())
                                        .getValue(BlockStateProperties.FACING)
                                == Direction.EAST
                        && climbDst.countItem(Items.IRON_INGOT) == 16
                        && ((Container) level.getBlockEntity(AUTO_CLIMB_SRC)).isEmpty()
                        && dipUpDst.countItem(Items.COPPER_INGOT) == 12
                        && ((Container) level.getBlockEntity(AUTO_DIP_UP_SRC)).isEmpty()
                        && zDst.countItem(Items.GOLD_INGOT) == 12
                        && ((Container) level.getBlockEntity(AUTO_Z_SRC)).isEmpty()
                        && teeDelivered == 16
                        && teeNorthDst.countItem(Items.COAL) <= 64
                        && teeSouthDst.countItem(Items.COAL) >= 10
                        && ((Container) level.getBlockEntity(AUTO_TEE_SRC)).isEmpty()
                        && uDst.countItem(Items.QUARTZ) == 12
                        && ((Container) level.getBlockEntity(AUTO_U_SRC)).isEmpty()
                        && nozzleDst.countItem(Items.LAPIS_LAZULI) == 24
                        && ((Container) level.getBlockEntity(NOZZLE_CLIMB_SRC)).isEmpty()
                        && extOnlyDst.countItem(Items.REDSTONE) == 16
                        && ((Container) level.getBlockEntity(NOZZLE_EXT_ONLY_SRC)).isEmpty());
        finishedCornerAndVertical = true;
    }

    private static void setPipe(ServerLevel level, Block pipeBlock, BlockPos pos, Direction out) {
        level.setBlockAndUpdate(
                pos, pipeBlock.defaultBlockState().setValue(BlockStateProperties.FACING, out));
    }

    private static void placePipeAgainst(
            ServerLevel level,
            net.minecraft.server.level.ServerPlayer player,
            Block pipeBlock,
            BlockPos target,
            Direction clickedFace) {
        var stack = new ItemStack(pipeBlock);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setPos(
                target.getX() + 0.5 + clickedFace.getStepX() * 2.0,
                target.getY() + 0.5 + clickedFace.getStepY() * 2.0,
                target.getZ() + 0.5 + clickedFace.getStepZ() * 2.0);
        var hit =
                new BlockHitResult(
                        Vec3.atCenterOf(target)
                                .add(
                                        clickedFace.getStepX() * 0.5,
                                        clickedFace.getStepY() * 0.5,
                                        clickedFace.getStepZ() * 0.5),
                        clickedFace,
                        target,
                        false);
        var placed =
                ((BlockItem) stack.getItem())
                        .place(
                                new BlockPlaceContext(
                                        player, InteractionHand.MAIN_HAND, stack, hit));
        if (!placed.consumesAction()) {
            throw new AssertionError(
                    "Failed to place pipe against " + target + " on " + clickedFace);
        }
    }

    private void tickFluid(ServerLevel level, BiConsumer<String, Boolean> check) {
        if (!openedOutlet) {
            if (volume(level, SUPPLY) != 0) return;
            check.accept(
                    "pump-moved-512-into-closed-network",
                    volume(level, FLUID_PIPE) == 1_229 && volume(level, TANK) == 0);
            check.accept(
                    "tank-capacity",
                    NativeTransferAccess.fill(level, TANK, Direction.UP, water(32_001), true)
                                    == 32_000
                            && NativeTransferAccess.fill(
                                            level, TANK, Direction.UP, water(31_993), false)
                                    == 31_993);
            rotate(level, FLUID_PIPE, Direction.SOUTH);
            openedOutlet = true;
            return;
        }
        if (!releasedBackpressure) {
            if (volume(level, TANK) < 32_000) return;
            check.accept(
                    "fluid-backpressure-7",
                    volume(level, FLUID_PIPE) == 1_222
                            && NativeTransferAccess.drain(level, TANK, Direction.UP, 32_000, false)
                                    == 32_000);
            releasedBackpressure = true;
            return;
        }
        if (volume(level, TANK) < 1_222) return;
        check.accept(
                "fluid-resumed-last-millibucket",
                volume(level, FLUID_PIPE) == 0
                        && volume(level, SUPPLY) == 0
                        && volume(level, TANK) == 1_222);
        var cell =
                click(
                        level,
                        TANK,
                        Direction.UP,
                        new ItemStack(NativeGameplayFixture.item("empty_cell")));
        check.accept(
                "usable-1000mb-cell",
                cell.is(NativeGameplayFixture.item("basic_water_cell"))
                        && NativeTransferAccess.itemFluidVolume(cell) == 1_000
                        && volume(level, TANK) == 222);
        click(
                level,
                FLUID_PIPE,
                Direction.WEST,
                new ItemStack(NativeGameplayFixture.item("crowbar")));
        check.accept(
                "crowbar-recovers-pump-cover",
                level.getEntitiesOfClass(ItemEntity.class, new AABB(FLUID_PIPE).inflate(1)).stream()
                        .anyMatch(
                                entity ->
                                        entity.getItem()
                                                .is(NativeGameplayFixture.item("pump_cover_lv"))));
        finishedFluid = true;
    }

    private static void rotate(ServerLevel level, BlockPos pos, Direction side) {
        var state = level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.FACING)
                && state.getValue(BlockStateProperties.FACING) == side) return;
        var player = FakePlayerFactory.getMinecraft(level);
        var stack = new ItemStack(NativeGameplayFixture.item("wrench"));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = WrenchHits.pipeControl(level, pos, player, side);
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        if (!player.gameMode
                .useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit)
                .consumesAction())
            throw new AssertionError("No wrench action at " + pos + " toward " + side);
    }

    static ItemStack click(ServerLevel level, BlockPos pos, Direction side, ItemStack stack) {
        var player = FakePlayerFactory.getMinecraft(level);
        player.setPos(
                pos.getX() + 0.5 + 2 * side.getStepX(),
                pos.getY() + 2 * side.getStepY(),
                pos.getZ() + 0.5 + 2 * side.getStepZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit =
                new BlockHitResult(
                        Vec3.atCenterOf(pos)
                                .add(
                                        side.getStepX() * 0.5,
                                        side.getStepY() * 0.5,
                                        side.getStepZ() * 0.5),
                        side,
                        pos,
                        false);
        if (!player.gameMode
                .useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit)
                .consumesAction()) {
            throw new AssertionError(
                    "Unrecognized transport interaction at " + pos + " with " + stack);
        }
        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }

    private static FluidStack water(int amount) {
        return new FluidStack(Fluids.WATER, amount);
    }

    private static int volume(ServerLevel level, BlockPos pos) {
        return NativeTransferAccess.volume(level, pos, Direction.UP);
    }

    private static ItemStack namedDiamonds(int count) {
        var diamonds = new ItemStack(Items.DIAMOND, count);
        diamonds.set(DataComponents.CUSTOM_NAME, Component.literal("Transport sample"));
        return diamonds;
    }

    private static void setupWaterlogChecks(
            ServerLevel level,
            net.minecraft.server.level.ServerPlayer player,
            BiConsumer<String, Boolean> check) {
        encloseSpreadBasin(level, WET_ITEM_PIPE);
        encloseSpreadBasin(level, WET_FLUID_PIPE);
        var tinyItem = NativeGameplayFixture.block("bronze_item_pipe_tiny");
        var mediumFluid = NativeGameplayFixture.block("bronze_fluid_pipe_medium");
        var tinyFluid = NativeGameplayFixture.block("steel_fluid_pipe_tiny");

        // Item pipe: placing in water waterlogs, bucket toggles cleanly, non-sneak bucket does not
        // get swallowed by TransportBlock.interact, and opening neighbor schedules water tick.
        placePipeInWater(level, player, WET_ITEM_PIPE, tinyItem, "item-pipe", check);
        var itemBucket = new ItemStack(Items.WATER_BUCKET);
        player.setShiftKeyDown(false);
        check.accept(
                "item-pipe-water-bucket-defers-to-vanilla-waterlog",
                !useItemOnPipe(level, player, WET_ITEM_PIPE, Direction.UP, itemBucket)
                        .consumesAction());
        check.accept(
                "item-pipe-refill-waterlog",
                ((SimpleWaterloggedBlock) tinyItem)
                                .placeLiquid(
                                        level,
                                        WET_ITEM_PIPE,
                                        level.getBlockState(WET_ITEM_PIPE),
                                        Fluids.WATER.getSource(false))
                        && level.getBlockState(WET_ITEM_PIPE)
                                .getValue(BlockStateProperties.WATERLOGGED));
        level.setBlockAndUpdate(WET_ITEM_PIPE.east(), Blocks.AIR.defaultBlockState());
        check.accept(
                "item-pipe-update-shape-schedules-water-tick",
                level.getFluidTicks().hasScheduledTick(WET_ITEM_PIPE, Fluids.WATER));

        // Fluid pipe: placing in water waterlogs; normal right-click with water bucket fills the
        // internal pipe buffer; sneak+right-click defers to vanilla bucket waterlogging; opening
        // neighbor schedules water tick.
        placePipeInWater(level, player, WET_FLUID_PIPE, mediumFluid, "fluid-pipe", check);
        var fillBucket = new ItemStack(Items.WATER_BUCKET);
        player.setShiftKeyDown(false);
        boolean filled =
                useItemOnPipe(level, player, WET_FLUID_PIPE, Direction.UP, fillBucket)
                        .consumesAction();
        check.accept(
                "fluid-pipe-normal-bucket-fills-internal-buffer",
                filled
                        && player.getMainHandItem().is(Items.BUCKET)
                        && volume(level, WET_FLUID_PIPE) == 1_000
                        && !level.getBlockState(WET_FLUID_PIPE)
                                .getValue(BlockStateProperties.WATERLOGGED));
        player.setShiftKeyDown(true);
        boolean sneakDeferred =
                !useItemOnPipe(
                                level,
                                player,
                                WET_FLUID_PIPE,
                                Direction.UP,
                                new ItemStack(Items.WATER_BUCKET))
                        .consumesAction();
        player.setShiftKeyDown(false);
        check.accept("fluid-pipe-sneak-bucket-defers-to-waterlog", sneakDeferred);
        check.accept(
                "fluid-pipe-refill-waterlog",
                ((SimpleWaterloggedBlock) mediumFluid)
                                .placeLiquid(
                                        level,
                                        WET_FLUID_PIPE,
                                        level.getBlockState(WET_FLUID_PIPE),
                                        Fluids.WATER.getSource(false))
                        && level.getBlockState(WET_FLUID_PIPE)
                                .getValue(BlockStateProperties.WATERLOGGED));
        level.setBlockAndUpdate(WET_FLUID_PIPE.east(), Blocks.AIR.defaultBlockState());
        check.accept(
                "fluid-pipe-update-shape-schedules-water-tick",
                level.getFluidTicks().hasScheduledTick(WET_FLUID_PIPE, Fluids.WATER));

        // Flowing water and lava basins: dry tiny pipes adjacent to flowing water/lava or under
        // falling water must not be washed away.
        setupFlowBasin(level, WATER_FLOW_SOURCE, Blocks.WATER, tinyItem, tinyFluid);
        setupFlowBasin(level, LAVA_FLOW_SOURCE, Blocks.LAVA, tinyItem, tinyFluid);
        setupFallingWaterShaft(level, FALL_WATER_ITEM, tinyItem);
        setupFallingWaterShaft(level, FALL_WATER_FLUID, tinyFluid);
    }

    private static void checkColdWaterlogging(
            ServerLevel level, BiConsumer<String, Boolean> check) {
        var tinyItem = NativeGameplayFixture.block("bronze_item_pipe_tiny");
        var mediumFluid = NativeGameplayFixture.block("bronze_fluid_pipe_medium");
        var itemState = level.getBlockState(WET_ITEM_PIPE);
        var fluidState = level.getBlockState(WET_FLUID_PIPE);
        check.accept(
                "cold-waterlogged-pipes-persist",
                itemState.is(tinyItem)
                        && itemState.getValue(BlockStateProperties.WATERLOGGED)
                        && level.getFluidState(WET_ITEM_PIPE).isSource()
                        && fluidState.is(mediumFluid)
                        && fluidState.getValue(BlockStateProperties.WATERLOGGED)
                        && level.getFluidState(WET_FLUID_PIPE).isSource());
        check.accept(
                "destroying-waterlogged-pipes-leaves-water",
                level.destroyBlock(WET_ITEM_PIPE, false)
                        && level.getBlockState(WET_ITEM_PIPE).is(Blocks.WATER)
                        && level.getFluidState(WET_ITEM_PIPE).isSource()
                        && level.destroyBlock(WET_FLUID_PIPE, false)
                        && level.getBlockState(WET_FLUID_PIPE).is(Blocks.WATER)
                        && level.getFluidState(WET_FLUID_PIPE).isSource());
    }

    private static void placePipeInWater(
            ServerLevel level,
            net.minecraft.server.level.ServerPlayer player,
            BlockPos pos,
            Block pipe,
            String label,
            BiConsumer<String, Boolean> check) {
        level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
        var stack = new ItemStack(pipe);
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit =
                new BlockHitResult(
                        Vec3.atCenterOf(pos.below()).add(0, 0.5, 0),
                        Direction.UP,
                        pos.below(),
                        false);
        var placed =
                ((BlockItem) stack.getItem())
                        .place(
                                new BlockPlaceContext(
                                        player, InteractionHand.MAIN_HAND, stack, hit));
        var state = level.getBlockState(pos);
        check.accept(
                label + "-placed-in-water",
                placed.consumesAction()
                        && state.is(pipe)
                        && state.getValue(BlockStateProperties.WATERLOGGED)
                        && level.getFluidState(pos).isSource()
                        && level.getFluidState(pos).getType() == Fluids.WATER);
        var scooped = ((SimpleWaterloggedBlock) pipe).pickupBlock(player, level, pos, state);
        check.accept(
                label + "-bucket-unwaterlogs",
                scooped.is(Items.WATER_BUCKET)
                        && !level.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED)
                        && level.getFluidState(pos).isEmpty());
    }

    private static net.minecraft.world.InteractionResult useItemOnPipe(
            ServerLevel level,
            net.minecraft.server.level.ServerPlayer player,
            BlockPos pos,
            Direction side,
            ItemStack stack) {
        player.setPos(
                pos.getX() + 0.5 + 2 * side.getStepX(),
                pos.getY() + 2 * side.getStepY(),
                pos.getZ() + 0.5 + 2 * side.getStepZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit =
                new BlockHitResult(
                        Vec3.atCenterOf(pos)
                                .add(
                                        side.getStepX() * 0.5,
                                        side.getStepY() * 0.5,
                                        side.getStepZ() * 0.5),
                        side,
                        pos,
                        false);
        return player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit);
    }

    private static void encloseSpreadBasin(ServerLevel level, BlockPos pos) {
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlockAndUpdate(pos.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                boolean wall = dx == -1 || dx == 2 || dz == -1 || dz == 1;
                level.setBlockAndUpdate(
                        pos.offset(dx, 0, dz),
                        (wall || dx == 1 ? Blocks.STONE : Blocks.AIR).defaultBlockState());
            }
        }
    }

    private static void setupFlowBasin(
            ServerLevel level, BlockPos source, Block fluidBlock, Block eastPipe, Block westPipe) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 1; dz++) {
                level.setBlockAndUpdate(
                        source.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                boolean wall = dx == -2 || dx == 2 || dz == -2 || dz == 1;
                level.setBlockAndUpdate(
                        source.offset(dx, 0, dz),
                        (wall ? Blocks.STONE : Blocks.AIR).defaultBlockState());
            }
        }
        level.setBlockAndUpdate(source.east(), eastPipe.defaultBlockState());
        level.setBlockAndUpdate(source.west(), westPipe.defaultBlockState());
        level.setBlockAndUpdate(source, fluidBlock.defaultBlockState());
    }

    private static void setupFallingWaterShaft(ServerLevel level, BlockPos pipePos, Block pipe) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlockAndUpdate(
                        pipePos.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                if (dx != 0 || dz != 0) {
                    level.setBlockAndUpdate(
                            pipePos.offset(dx, 0, dz), Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(
                            pipePos.offset(dx, 1, dz), Blocks.STONE.defaultBlockState());
                }
            }
        }
        level.setBlockAndUpdate(pipePos, pipe.defaultBlockState());
        level.setBlockAndUpdate(pipePos.above(), Blocks.WATER.defaultBlockState());
    }

    private static boolean isDryPipe(ServerLevel level, BlockPos pos, Block expected) {
        var state = level.getBlockState(pos);
        return state.is(expected)
                && state.hasProperty(BlockStateProperties.WATERLOGGED)
                && !state.getValue(BlockStateProperties.WATERLOGGED);
    }
}
