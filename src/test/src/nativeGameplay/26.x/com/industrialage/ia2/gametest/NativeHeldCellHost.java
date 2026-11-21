package com.industrialage.ia2.gametest;

import com.industrialage.ia2.celltest.HeldCellChecks;
import com.industrialage.ia2.celltest.HeldCellChecks.Liquid;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/** Native fluid capability observations for the common hand-use matrix. */
final class NativeHeldCellHost {
    private NativeHeldCellHost() {}

    static void run(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin,
            BiConsumer<String, Boolean> check) {
        HeldCellChecks.run(level, player, origin, access(level), check);
        verifyPickup(level, player, origin.east(), check);
    }

    private static HeldCellChecks.Host access(ServerLevel level) {
        return new HeldCellChecks.Host(
                NativeGameplayFixture::item,
                NativeGameplayFixture::block,
                stack -> liquid(FluidUtil.getFirstStackContained(stack)),
                (stack, contents) -> NativeTransferAccess.filledCell(stack, nativeFluid(contents)),
                (stack, name) -> stack.set(DataComponents.CUSTOM_NAME, name),
                pos -> tank(level, pos),
                (pos, contents, simulate) ->
                        NativeTransferAccess.fill(
                                level, pos, Direction.UP, nativeFluid(contents), simulate));
    }

    private static void verifyPickup(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BiConsumer<String, Boolean> check) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(
                pos, NativeGameplayFixture.block("fluid_bottler").defaultBlockState());
        check.accept(
                "bottler-vibration-stocked",
                NativeTransferAccess.fill(
                                level, pos, Direction.UP, new FluidStack(Fluids.WATER, 1000), false)
                        == 1000);
        var pickups = new AtomicInteger();
        Consumer<VanillaGameEvent> listener =
                event -> {
                    if (event.getLevel() == level
                            && event.getVanillaEvent().equals(GameEvent.FLUID_PICKUP)
                            && BlockPos.containing(event.getEventPosition()).equals(pos))
                        pickups.incrementAndGet();
                };
        NeoForge.EVENT_BUS.addListener(VanillaGameEvent.class, listener);
        try {
            useEmptyCell(level, player, pos);
            check.accept("bottler-withdrawal-emits-one-fluid-pickup", pickups.get() == 1);
            useEmptyCell(level, player, pos);
            check.accept("empty-bottler-emits-no-fluid-pickup", pickups.get() == 1);
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            player.getInventory().clearContent();
        }
    }

    private static void useEmptyCell(ServerLevel level, ServerPlayer player, BlockPos pos) {
        var input = new ItemStack(NativeGameplayFixture.item("empty_cell"));
        player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        player.setItemInHand(InteractionHand.MAIN_HAND, input);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false);
        player.gameMode.useItemOn(player, level, input, InteractionHand.MAIN_HAND, hit);
        player.closeContainer();
    }

    private static Liquid tank(ServerLevel level, BlockPos pos) {
        var handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, Direction.UP);
        if (handler == null) throw new AssertionError("Missing native fluid port at " + pos);
        return liquid(handler.getResource(0).toStack(handler.getAmountAsInt(0)));
    }

    private static Liquid liquid(FluidStack stack) {
        return new Liquid(stack.getFluid(), stack.getAmount());
    }

    private static FluidStack nativeFluid(Liquid liquid) {
        return new FluidStack(liquid.fluid(), liquid.amount());
    }
}
