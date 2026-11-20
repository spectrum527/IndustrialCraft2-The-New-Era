package com.industrialage.ia2.gametest;

import com.industrialage.ia2.content.IndustrialFluid;
import com.industrialage.ia2.gameplay.IndustrialMachine;
import com.industrialage.ia2.industrial.PowerNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Installed-JAR measurements of actual reactor, chamber, transformer, fluid and storage blocks. */
final class PowerChecks {
    private static final BlockPos CORE = new BlockPos(4, 150, 4);
    private static final BlockPos GEO = CORE.south(4);
    private static final BlockPos HV_SOURCE = CORE.south(8);
    private static final Path RECORD = Path.of("reactor-before-restart.txt");
    private final UtilityChecks utilities = new UtilityChecks();
    private int frozenHeat;
    private int frozenUse;
    private double frozenReceived;
    private double previousTransformer;
    private double largestDelivery;

    void start(ServerLevel world, boolean read, BiConsumer<String, Boolean> check) {
        world.setChunkForced(0, 0, true);
        world.getChunk(0, 0);
        if (read) {
            try {
                var saved = Files.readAllLines(RECORD);
                frozenHeat = Integer.parseInt(saved.get(0));
                frozenUse = Integer.parseInt(saved.get(1));
                frozenReceived = Double.parseDouble(saved.get(2));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            utilities.coldTank(world, check);
            var core = unit(world, CORE);
            check.accept(
                    "reactor-cold-exact",
                    core.hullHeat() == frozenHeat
                            && core.componentUse(0) == frozenUse
                            && core.reactorOutput() == 0);
            check.accept(
                    "chamber-storage-cold-exact",
                    box(world, CORE.east(2)).energy() == frozenReceived);
            world.setBlockAndUpdate(CORE.north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            return;
        }
        RegistryChecks.run(check);
        ClassicCraftChecks.run(world, check);
        utilities.warm(world);
        var player = FakePlayerFactory.getMinecraft(world);
        place(world, CORE, "nuclear_reactor", Direction.NORTH);
        place(world, CORE.east(), "reactor_chamber", Direction.EAST);
        place(world, CORE.east(2), "batbox", Direction.EAST);
        var menu = unit(world, CORE).createMenu(1, player.getInventory(), player);
        var rod = new ItemStack(NativeGameplayFixture.item("uranium_fuel_rod"));
        check.accept(
                "reactor-accepts-rod-rejects-stone",
                menu.getSlot(0).mayPlace(rod)
                        && !menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)));
        menu.getSlot(0).set(rod);
        world.setBlockAndUpdate(CORE.north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        place(world, GEO, "geo_generator", Direction.WEST);
        place(world, GEO.east(), "batbox", Direction.EAST);
        unit(world, GEO)
                .createMenu(2, player.getInventory(), player)
                .getSlot(0)
                .set(new ItemStack(Items.LAVA_BUCKET));
        place(world, HV_SOURCE, "mfe", Direction.EAST);
        place(world, HV_SOURCE.east(), "mv_transformer", Direction.WEST);
        place(world, HV_SOURCE.east(2), "cesu", Direction.EAST);
        check.accept("transformer-source-primed", box(world, HV_SOURCE).receive(1536) == 1536);
        for (IndustrialFluid f : IndustrialFluid.ALL) {
            var fluid = NativeGameplayFixture.block(f.path()).defaultBlockState().getFluidState();
            check.accept(
                    "fluid-" + f.path(),
                    !fluid.isEmpty()
                            && fluid.getType().getBucket()
                                    == NativeGameplayFixture.item(f.path() + "_bucket"));
        }
    }

    boolean sample(ServerLevel world, int tick, boolean read, BiConsumer<String, Boolean> check) {
        var core = unit(world, CORE);
        if (read) {
            if (tick < 100 || core.componentUse(0) < frozenUse + 2) return false;
            check.accept(
                    "cold-rod-continued",
                    core.hullHeat() - frozenHeat == 4 * (core.componentUse(0) - frozenUse));
            check.accept(
                    "cold-chamber-generates",
                    box(world, CORE.east(2)).energy() > frozenReceived
                            && core.reactorOutput() == 5);
            return true;
        }
        if (tick == 30) utilities.start(world, check);
        if (tick > 30) utilities.sample(world, tick - 30, check);
        double next = box(world, HV_SOURCE.east(2)).energy();
        largestDelivery = Math.max(largestDelivery, next - previousTransformer);
        previousTransformer = next;
        if (tick == 120) {
            check.accept(
                    "runtime-rod-heat-and-pulses",
                    core.hullHeat() == 4 * core.componentUse(0)
                            && core.reactorOutput() == 5
                            && core.coreWidth() == 4);
            check.accept("eu-exits-chamber", box(world, CORE.east(2)).energy() > 400);
            world.setBlockAndUpdate(CORE.north(), Blocks.AIR.defaultBlockState());
        }
        if (tick < 550) return false;
        check.accept(
                "wrench-face-transformer-4-packets",
                next == 1536 && largestDelivery == 512 && box(world, HV_SOURCE).energy() == 0);
        var player = FakePlayerFactory.getMinecraft(world);
        var menu = unit(world, GEO).createMenu(3, player.getInventory(), player);
        check.accept(
                "lava-bucket-yields-ten-thousand-eu",
                box(world, GEO.east()).energy() == 10000
                        && menu.getSlot(1).getItem().is(Items.BUCKET));
        check.accept(
                "unpowered-reactor-stopped", core.reactorOutput() == 0 && core.componentUse(0) > 0);
        try {
            Files.writeString(
                    RECORD,
                    core.hullHeat()
                            + "\n"
                            + core.componentUse(0)
                            + "\n"
                            + box(world, CORE.east(2)).energy()
                            + "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return true;
    }

    private static PowerNode unit(ServerLevel world, BlockPos pos) {
        return (PowerNode) world.getBlockEntity(pos);
    }

    private static IndustrialMachine box(ServerLevel world, BlockPos pos) {
        return (IndustrialMachine) world.getBlockEntity(pos);
    }

    private static void place(ServerLevel world, BlockPos pos, String id, Direction facing) {
        world.setBlockAndUpdate(
                pos,
                NativeGameplayFixture.block(id)
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, facing));
    }
}
