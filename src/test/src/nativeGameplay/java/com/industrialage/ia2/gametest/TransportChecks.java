package com.industrialage.ia2.gametest;

import com.industrialage.ia2.logisticstest.PipeNozzleChecks;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
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
    private boolean openedOutlet;
    private boolean releasedBackpressure;
    private boolean finishedFluid;
    private boolean finishedItems;

    void writeStart(ServerLevel level, BiConsumer<String, Boolean> check) {
        var player = FakePlayerFactory.getMinecraft(level);
        level.setChunkForced(0, 0, true);
        level.getChunk(0, 0);
        nozzles.place(level, player, check);
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
    }

    boolean writeTick(ServerLevel level, BiConsumer<String, Boolean> check) {
        return nozzles.waitForBackpressure(level, check);
    }

    void readStart(ServerLevel level, BiConsumer<String, Boolean> check) {
        level.getChunk(0, 0);
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
        if (!finishedFluid) {
            tickFluid(level, check);
        }
        return nozzles.delivered(level, check) && finishedItems && finishedFluid;
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
                + finishedItems;
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
}
