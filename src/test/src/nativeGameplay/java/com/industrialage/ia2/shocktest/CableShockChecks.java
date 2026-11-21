package com.industrialage.ia2.shocktest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Single-packet, disconnected, and overlapping live-wire worlds observed at native boundaries. */
public final class CableShockChecks {
    private static final int FULL = 40_000_000;
    private static final int NO_SINK = -1;
    private static final String COPPER = "copper_cable";
    private static final String IRON = "iron_cable";
    private static final String GOLD = "gold_cable";
    private static final String BATBOX = "batbox";
    private static final String CESU = "cesu";
    private static final String MFE = "mfe";
    private static final String MFSU = "mfsu";

    private final ServerLevel world;
    private final BlockPos origin;
    private final Host host;
    private final List<Wire> wires = new ArrayList<>();
    private final List<Reading> readings = new ArrayList<>();
    private final List<Witness> witnesses = new ArrayList<>();

    /** Public registry, block-entity storage and damage observations on the selected loader. */
    public record Host(
            Function<String, Block> block,
            Function<String, Item> item,
            BiConsumer<BlockPos, Integer> seedEnergy,
            ToDoubleFunction<BlockPos> storedEnergy,
            Function<ServerLevel, Mob> ironGolem,
            Predicate<DamageSource> magic) {}

    /** Uses the given loaded column, with separate floors for isolated test circuits. */
    public CableShockChecks(ServerLevel world, BlockPos origin, Host host) {
        this.world = world;
        this.origin = origin;
        this.host = host;
    }

    /** Places native storage and fixed-position golems before any circuit is connected. */
    public void arrange() {
        var dry = floor(0);
        line("no-sink", dry, CESU, 128, COPPER, 1, NO_SINK, 0);
        observe("no-sink-unhurt", dry, 0, 1.4, 0);
        var blocked = floor(1);
        line("full-sink", blocked, CESU, 128, COPPER, 1, FULL, 0);
        observe("full-sink-unhurt", blocked, 0, 1.4, 0);

        range("tin-32", floor(2), BATBOX, 32, "tin_cable", 1, 1, false);
        var medium = floor(3);
        range("copper-128", medium, CESU, 128, COPPER, 2, 2, false);
        observe("copper-diagonal-inside-sphere", medium, -1.65, 1.65, 2);
        observe("copper-diagonal-outside-sphere", medium, 2.4, 2.4, 0);
        range("gold-512", floor(4), MFE, 512, GOLD, 3, 8, false);
        range("iron-2048", floor(5), MFSU, 2048, IRON, 4, 32, false);
        range("iron-two-origins-4096", floor(6), MFSU, 2048, IRON, 5, 64, true);

        var cover = floor(7);
        safeCover("insulated-copper", cover, "insulated_copper_cable");
        safeCover("construction-foam", floor(8), "copper_foam_cable");
        safeCover("glass-fibre", floor(9), "glass_fibre_cable");

        var partial = floor(10);
        line("partial-delivery", partial, MFSU, 2048, IRON, 1, FULL - 10, 10);
        observe("partial-delivery-ten-eu-near-one-hp", partial, 0, 1.35, 1);
        observe("partial-delivery-radius-one", partial, 0, -2.35, 0);

        var longer = floor(11);
        line("three-segments", longer, CESU, 128, COPPER, 3, 0, 128);
        observe("one-hit-across-three-segments", longer.east(), 0, 1.45, 2);

        var branched = floor(12);
        line("dead-branch", branched, BATBOX, 32, COPPER, 1, 0, 32);
        for (int step = 1; step <= 6; step++) wires.add(new Wire(branched.south(step), COPPER));
        observe("dead-branch-live-trunk-hurts", branched, 0, -1.35, 1);
        observe("dead-branch-end-unhurt", branched, 0, 7.15, 0);

        var crossing = floor(13);
        line("overlap-weaker-line", crossing, CESU, 128, COPPER, 1, 0, 128);
        line("overlap-stronger-line", crossing.south(3), MFE, 512, GOLD, 1, 0, 512);
        observe("overlap-takes-only-strongest-hit", crossing, 0, 1.65, 8);
        observe("overlap-farther-witness-takes-weaker-hit", crossing, 0, -1.5, 2);

        var hazmat = floor(14);
        line("hazmat-live-line", hazmat, CESU, 128, COPPER, 1, 0, 128);
        var protectedActor = observe("full-suit-immune", hazmat, 0, 1.4, 0);
        suit(protectedActor, true);
        var unprotectedActor = observe("three-pieces-still-shocked", hazmat, 0, -1.4, 2);
        suit(unprotectedActor, false);
    }

    /** Connects all one-shot routes within one server call, before the next world tick. */
    public void connect(BiConsumer<String, Boolean> check) {
        for (var witness : witnesses) {
            if (witness.actor().getHealth() != 100
                    || witness.actor().getLastDamageSource() != null) {
                throw new AssertionError(witness.name() + " was hurt before the cable was placed");
            }
        }
        check.accept("witnesses-stayed-unhurt-before-live-cables", true);
        for (var wire : wires) {
            var state = host.block().apply(wire.path()).defaultBlockState();
            if (!world.setBlockAndUpdate(wire.pos(), state)) {
                throw new AssertionError("Could not place test wire at " + wire.pos());
            }
        }
    }

    /** Records the actual source debit, storage uptake, witness health and native damage source. */
    public void verify(BiConsumer<String, Boolean> check) {
        for (var reading : readings) {
            assertValue(
                    check,
                    reading.name(),
                    reading.eu(),
                    host.storedEnergy().applyAsDouble(reading.pos()));
        }
        for (var witness : witnesses) {
            var actor = witness.actor();
            assertValue(check, witness.name(), witness.health(), actor.getHealth());
            var source = actor.getLastDamageSource();
            boolean hurt = witness.health() < 100;
            check.accept(
                    witness.name() + (hurt ? "-magic-damage" : "-no-damage-source"),
                    hurt ? source != null && host.magic().test(source) : source == null);
            actor.discard();
        }
    }

    private BlockPos floor(int row) {
        return origin.above(13 * row);
    }

    private void range(
            String id,
            BlockPos cable,
            String donor,
            int packet,
            String wire,
            int radius,
            int damage,
            boolean dual) {
        line(id, cable, donor, packet, wire, 1, 0, packet * (dual ? 2 : 1));
        if (dual) {
            storage(cable.north(), donor, Direction.SOUTH, packet);
            readings.add(new Reading(id + "-second-source", cable.north(), 0));
        }
        // Each golem is 1.4 blocks wide. The first center is outside the radius, but its box is
        // inside.
        observe(id + "-near-box", cable, 0, radius + .35, damage);
        observe(id + "-far-box", cable, 0, -radius - .95, 0);
    }

    private void safeCover(String id, BlockPos pos, String cable) {
        line(id, pos, CESU, 128, cable, 1, 0, 128);
        observe(id + "-safe-while-transferring", pos, 0, 1.4, 0);
    }

    private void line(
            String id,
            BlockPos first,
            String donor,
            int packet,
            String wire,
            int length,
            int held,
            int delivered) {
        var source = first.west();
        var sink = first.east(length);
        storage(source, donor, Direction.EAST, packet);
        readings.add(new Reading(id + "-source-eu", source, Math.max(0, packet - delivered)));
        if (held != NO_SINK) {
            storage(sink, MFSU, Direction.EAST, held);
            readings.add(new Reading(id + "-sink-eu", sink, held + delivered));
        }
        for (int segment = 0; segment < length; segment++)
            wires.add(new Wire(first.east(segment), wire));
    }

    private void storage(BlockPos pos, String path, Direction facing, int energy) {
        var state =
                host.block()
                        .apply(path)
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, facing);
        if (!world.setBlockAndUpdate(pos, state)) {
            throw new AssertionError("Could not place test storage at " + pos);
        }
        if (energy != 0) host.seedEnergy().accept(pos, energy);
        double actual = host.storedEnergy().applyAsDouble(pos);
        if (actual != energy)
            throw new AssertionError(path + " initialized with " + actual + " EU");
    }

    private LivingEntity observe(String label, BlockPos cable, double dx, double dz, int damage) {
        double x = cable.getX() + .5 + dx;
        double y = cable.getY();
        double z = cable.getZ() + .5 + dz;
        var actor = host.ironGolem().apply(world);
        if (actor == null) throw new AssertionError("Could not create witness " + label);
        actor.setPos(x, y, z);
        actor.setNoAi(true);
        actor.setNoGravity(true);
        actor.setSilent(true);
        actor.setPersistenceRequired();
        if (!world.addFreshEntity(actor)) {
            throw new AssertionError("Could not add witness " + label + " to the test world");
        }
        witnesses.add(new Witness(label, actor, 100 - damage));
        return actor;
    }

    private void suit(LivingEntity actor, boolean complete) {
        if (complete)
            actor.setItemSlot(
                    EquipmentSlot.HEAD, new ItemStack(host.item().apply("hazmat_helmet")));
        actor.setItemSlot(
                EquipmentSlot.CHEST, new ItemStack(host.item().apply("hazmat_chestplate")));
        actor.setItemSlot(EquipmentSlot.LEGS, new ItemStack(host.item().apply("hazmat_leggings")));
        actor.setItemSlot(EquipmentSlot.FEET, new ItemStack(host.item().apply("rubber_boots")));
    }

    private static void assertValue(
            BiConsumer<String, Boolean> check, String name, double expected, double actual) {
        if (expected != actual)
            throw new AssertionError(name + " expected " + expected + ", found " + actual);
        check.accept(name, true);
    }

    private record Wire(BlockPos pos, String path) {}

    private record Reading(String name, BlockPos pos, int eu) {}

    private record Witness(String name, LivingEntity actor, int health) {}
}
