package com.industrialage.ia2.gametest;

import com.industrialage.ia2.gameplay.IndustrialMachine;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Verifies the extended voltage and redstone devices in a real loaded chunk. */
final class EnergyTierChecks {
    private static final BlockPos SOURCE = new BlockPos(18, 104, 29);
    private static final BlockPos SPLITTER = SOURCE.east();
    private static final BlockPos INSULATOR = SPLITTER.east();
    private static final BlockPos DETECTOR = INSULATOR.east();
    private static final BlockPos SINK = DETECTOR.east();
    private static final BlockPos GEAR = new BlockPos(26, 104, 29);
    private int ticks;
    private boolean powered;
    private boolean cut;
    private double beforeCut;
    private double sourceBeforeCut;
    private boolean done;

    void setup(ServerLevel level, BiConsumer<String, Boolean> check) {
        var player = FakePlayerFactory.getMinecraft(level);
        var source = new ItemStack(NativeGameplayFixture.item("batbox"));
        source.set(energyComponent(), 3_200.0);
        NativeGameplayFixture.place(level, player, "batbox", SOURCE, source);
        NativeGameplayFixture.place(level, player, "splitter_cable", SPLITTER);
        NativeGameplayFixture.place(level, player, "glass_fibre_cable", INSULATOR);
        NativeGameplayFixture.place(level, player, "detector_cable", DETECTOR);
        NativeGameplayFixture.place(level, player, "batbox", SINK);
        int offset = 0;
        for (var entry : Map.of("cesu", 300_000, "mfe", 4_000_000, "mfsu", 40_000_000).entrySet()) {
            var pos = GEAR.north(2 * offset++);
            NativeGameplayFixture.place(level, player, entry.getKey(), pos);
            check.accept(
                    entry.getKey() + "-capacity",
                    machine(level, pos).capacity() == entry.getValue());
        }
    }

    private void wrenchEnergy(ServerLevel level, BiConsumer<String, Boolean> check) {
        var player = FakePlayerFactory.getMinecraft(level);
        var pos = GEAR.above(2);
        var seeded = new ItemStack(NativeGameplayFixture.item("batbox"));
        seeded.set(energyComponent(), 1_000.0);
        NativeGameplayFixture.place(level, player, "batbox", pos, seeded);
        var wrench = new ItemStack(NativeGameplayFixture.item("wrench"));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() - 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        var hit =
                new BlockHitResult(
                        Vec3.atCenterOf(pos).add(0, 0, -0.5), Direction.NORTH, pos, false);
        check.accept(
                "wrench-dismantles-storage",
                player.gameMode
                                .useItemOn(player, level, wrench, InteractionHand.MAIN_HAND, hit)
                                .consumesAction()
                        && wrench.getDamageValue() == 1
                        && level.getBlockEntity(pos) == null);
        var dropped =
                level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
                        .filter(entity -> entity.getItem().is(NativeGameplayFixture.item("batbox")))
                        .findFirst()
                        .orElseThrow();
        var replacement = dropped.getItem().copy();
        dropped.discard();
        check.accept(
                "wrench-retains-800-eu", replacement.getOrDefault(energyComponent(), 0.0) == 800);
        NativeGameplayFixture.place(level, player, "batbox", pos, replacement);
        check.accept("wrench-restored-800-eu", machine(level, pos).energy() == 800);
    }

    boolean tick(ServerLevel level, BiConsumer<String, Boolean> check) {
        if (done) return true;
        ticks++;
        if (ticks == 1) wrenchEnergy(level, check);
        var source = machine(level, SOURCE);
        var sink = machine(level, SINK);
        if (!powered && ticks >= 10) {
            check.accept(
                    "splitter-blocked-unpowered", source.energy() == 3_200 && sink.energy() == 0);
            level.setBlockAndUpdate(SPLITTER.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            powered = true;
        } else if (powered
                && !cut
                && sink.energy() > 0
                && level.getBlockState(DETECTOR).getSignal(level, DETECTOR, Direction.UP) == 15) {
            double packets = (3_200 - source.energy()) / 32;
            check.accept(
                    "detector-redstone-and-line-loss",
                    packets > 0 && packets == (int) packets && sink.energy() == packets * 31);
            beforeCut = sink.energy();
            sourceBeforeCut = source.energy();
            level.setBlockAndUpdate(SPLITTER.below(), Blocks.STONE.defaultBlockState());
            cut = true;
        } else if (cut
                && level.getBlockState(DETECTOR).getSignal(level, DETECTOR, Direction.UP) == 0) {
            check.accept(
                    "splitter-and-detector-stop",
                    sink.energy() == beforeCut && source.energy() == sourceBeforeCut);
            done = true;
        }
        return done;
    }

    @SuppressWarnings("unchecked")
    static DataComponentType<Double> energyComponent() {
        return (DataComponentType<Double>)
                BuiltInRegistries.DATA_COMPONENT_TYPE.stream()
                        .filter(
                                type ->
                                        BuiltInRegistries.DATA_COMPONENT_TYPE
                                                .getKey(type)
                                                .toString()
                                                .equals("ic2:eu"))
                        .findFirst()
                        .orElseThrow();
    }

    private static IndustrialMachine machine(ServerLevel level, BlockPos pos) {
        return (IndustrialMachine) level.getBlockEntity(pos);
    }
}
