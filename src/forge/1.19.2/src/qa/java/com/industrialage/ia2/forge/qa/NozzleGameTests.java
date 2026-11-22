package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.logisticstest.PipeNozzleChecks;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Forge sided pipe nozzle operations and saturation in a standard flat GameTest. */
@GameTestHolder("ic2_qa")
@PrefixGameTestTemplate(false)
public final class NozzleGameTests {
    private NozzleGameTests() {}

    static PipeNozzleChecks matrix(BlockPos origin) {
        return new PipeNozzleChecks(
                origin,
                PlayableGameTests::block,
                PlayableGameTests::stack,
                new PipeNozzleChecks.Automation() {
                    @Override
                    public ItemStack itemCargo(ServerLevel level, BlockPos pos) {
                        return level.getBlockEntity(pos)
                                .getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.SOUTH)
                                .orElseThrow(AssertionError::new)
                                .getStackInSlot(0);
                    }

                    @Override
                    public int seedWater(ServerLevel level, BlockPos pos, int amount) {
                        return level.getBlockEntity(pos)
                                .getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH)
                                .orElseThrow(AssertionError::new)
                                .fill(new FluidStack(Fluids.WATER, amount), FluidAction.EXECUTE);
                    }

                    @Override
                    public boolean reverseRejected(
                            ServerLevel level, BlockPos pos, boolean fluid, ItemStack sample) {
                        var node = level.getBlockEntity(pos);
                        if (fluid) {
                            var inject =
                                    node.getCapability(
                                                    ForgeCapabilities.FLUID_HANDLER,
                                                    Direction.SOUTH)
                                            .orElseThrow(AssertionError::new);
                            var extract =
                                    node.getCapability(
                                                    ForgeCapabilities.FLUID_HANDLER,
                                                    Direction.NORTH)
                                            .orElseThrow(AssertionError::new);
                            return inject.fill(new FluidStack(Fluids.WATER, 1), FluidAction.EXECUTE)
                                            == 0
                                    && extract.drain(1, FluidAction.EXECUTE).isEmpty();
                        }
                        var inject =
                                node.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.SOUTH)
                                        .orElseThrow(AssertionError::new);
                        var extract =
                                node.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH)
                                        .orElseThrow(AssertionError::new);
                        return inject.insertItem(0, sample, false).getCount() == sample.getCount()
                                && extract.extractItem(0, 1, false).isEmpty();
                    }
                });
    }

    /** Existing Forge fluid consumers see a permission change even if they cached a handle. */
    @GameTest(template = "smoke")
    public static void tankNotifiesCachedAutomation(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(pos, PlayableGameTests.block("bronze_tank").defaultBlockState());
        var tank = level.getBlockEntity(pos);
        var previous = tank.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH);
        var oldPort = previous.orElseThrow(AssertionError::new);
        var invalidated = new AtomicBoolean();
        previous.addListener(ignored -> invalidated.set(true));
        var water = new FluidStack(Fluids.WATER, 257);
        PlayableGameTests.check(
                helper,
                MachineAccess.set(tank, 4, MachineAccess.Mode.BLOCKED.ordinal())
                        && invalidated.get()
                        && !previous.isPresent()
                        && !tank.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH)
                                .isPresent()
                        && oldPort.fill(water, FluidAction.EXECUTE) == 0,
                "Removing a Forge tank port invalidated listeners and rejected both kinds of cached access");
        PlayableGameTests.check(
                helper,
                MachineAccess.set(tank, 4, MachineAccess.Mode.INPUT.ordinal()),
                "Restored tank face as a valid input port");
        var restored =
                tank.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH)
                        .orElseThrow(AssertionError::new);
        PlayableGameTests.check(
                helper,
                restored.fill(water, FluidAction.EXECUTE) == 257
                        && restored.drain(257, FluidAction.EXECUTE).isEmpty(),
                "Fresh input port fills but rejects reverse drain");
        helper.succeed();
    }

    /** Compares each pipe's real first tick and full delivered amount at two IC2 machines. */
    @GameTest(template = "smoke", timeoutTicks = 100)
    public static void machinePipeNozzles(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(1, 100, 1));
        var player = PlayableGameTests.player(helper, origin);
        var matrix = matrix(origin);
        java.util.function.BiConsumer<String, Boolean> check =
                (name, passed) -> PlayableGameTests.check(helper, passed, name);
        matrix.place(level, player, check);
        helper.onEachTick(
                new Runnable() {
                    private boolean opened;

                    @Override
                    public void run() {
                        if (!opened && matrix.waitForBackpressure(level, check)) {
                            opened = true;
                            matrix.letDeliver(level, player, "resumed", check);
                        }
                        if (opened && matrix.delivered(level, check)) helper.succeed();
                    }
                });
    }
}
