package com.industrialage.ia2.gametest;

import com.industrialage.ia2.gameplay.IndustrialMachine;
import com.industrialage.ia2.gameplay.PortableGear;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;

/** Native state checks for recharging, powered mining, and all three launchable boat types. */
final class GearChecks {
    private static final BlockPos BOX = new BlockPos(28, 102, 18);
    private static final BlockPos LASER_ORIGIN = new BlockPos(31, 117, 21);
    private static final BlockPos LASER_TARGET = LASER_ORIGIN.west(4).above();
    private final ItemStack drill = new ItemStack(NativeGameplayFixture.item("drill"));
    private boolean firedLaser;
    private int modeCheck = 1;
    private long nextShot;
    private boolean done;

    void setup(ServerLevel level, BiConsumer<String, Boolean> check) {
        var player = FakePlayerFactory.getMinecraft(level);
        var seeded = new ItemStack(NativeGameplayFixture.item("batbox"));
        seeded.set(EnergyTierChecks.energyComponent(), 500.0);
        NativeGameplayFixture.place(level, player, "batbox", BOX, seeded);
        check.accept(
                "empty-drill-speed",
                drill.getDestroySpeed(Blocks.IRON_ORE.defaultBlockState()) == 1);
        var box = (IndustrialMachine) level.getBlockEntity(BOX);
        var menu = box.createMenu(24, player.getInventory(), player);
        check.accept(
                "charge-slot-accepts-low-tier-drill",
                menu.slots.getFirst().mayPlace(drill)
                        && !menu.slots
                                .getFirst()
                                .mayPlace(
                                        new ItemStack(
                                                NativeGameplayFixture.item("iridium_drill"))));
        box.setItem(0, drill);
        handTools(level, check);
    }

    private void handTools(ServerLevel level, BiConsumer<String, Boolean> check) {
        var player = FakePlayerFactory.getMinecraft(level);
        var resin = new BlockPos(22, 109, 22);
        var tap = new ItemStack(NativeGameplayFixture.item("electric_treetap"));
        var wet =
                NativeGameplayFixture.block("rubber_log")
                        .getStateDefinition()
                        .getPossibleStates()
                        .stream()
                        .filter(state -> state.toString().contains("axis=y,resin_hole=wet_north"))
                        .findFirst()
                        .orElseThrow();
        level.setBlockAndUpdate(resin, wet);
        player.setPos(22.5, 109, 20.5);
        var hole = new BlockHitResult(Vec3.atCenterOf(resin), Direction.NORTH, resin, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, tap);
        player.gameMode.useItemOn(player, level, tap, InteractionHand.MAIN_HAND, hole);
        check.accept("electric-tap-empty-leaves-resin", level.getBlockState(resin).equals(wet));
        PortableGear.restoreEnergy(tap, 500);
        var tapped = player.gameMode.useItemOn(player, level, tap, InteractionHand.MAIN_HAND, hole);
        check.accept(
                "electric-tap-harvests-once",
                tapped.consumesAction()
                        && PortableGear.stored(tap) == 450
                        && level.getBlockState(resin).toString().contains("resin_hole=dry_north")
                        && tap.getDamageValue() == 0);
        double left = PortableGear.stored(tap);
        player.gameMode.useItemOn(player, level, tap, InteractionHand.MAIN_HAND, hole);
        check.accept("electric-tap-dry-hole-costs-zero", PortableGear.stored(tap) == left);

        var box = resin.east(3);
        NativeGameplayFixture.place(level, player, "batbox", box);
        var wrench = new ItemStack(NativeGameplayFixture.item("electric_wrench"));
        PortableGear.restoreEnergy(wrench, 2_000);
        var side = new BlockHitResult(Vec3.atCenterOf(box), Direction.SOUTH, box, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        var rotated =
                player.gameMode.useItemOn(player, level, wrench, InteractionHand.MAIN_HAND, side);
        check.accept(
                "electric-wrench-rotates-and-debits",
                rotated.consumesAction()
                        && level.getBlockState(box).getValue(BlockStateProperties.FACING)
                                == Direction.SOUTH
                        && PortableGear.stored(wrench) == 1_900
                        && wrench.getDamageValue() == 0);
        var beforeBreak = PortableGear.stored(wrench);
        player.setShiftKeyDown(true);
        try {
            var opposite = new BlockHitResult(Vec3.atCenterOf(box), Direction.NORTH, box, false);
            var dismantled =
                    player.gameMode.useItemOn(
                            player, level, wrench, InteractionHand.MAIN_HAND, opposite);
            check.accept(
                    "electric-wrench-dismantles-and-debits",
                    dismantled.consumesAction()
                            && level.getBlockState(box).isAir()
                            && PortableGear.stored(wrench) == beforeBreak - 1_000);
        } finally {
            player.setShiftKeyDown(false);
        }

        var wind = new ItemStack(NativeGameplayFixture.item("wind_meter"));
        PortableGear.restoreEnergy(wind, 500);
        player.setItemInHand(InteractionHand.MAIN_HAND, wind);
        check.accept(
                "powered-wind-meter-reads-ambient",
                player.gameMode
                                .useItem(player, level, wind, InteractionHand.MAIN_HAND)
                                .consumesAction()
                        && PortableGear.stored(wind) == 450);
        var saber = new ItemStack(NativeGameplayFixture.item("nano_saber"));
        PortableGear.restoreEnergy(saber, 5_000);
        player.setItemInHand(InteractionHand.MAIN_HAND, saber);
        player.gameMode.useItem(player, level, saber, InteractionHand.MAIN_HAND);
        check.accept("nano-saber-switches-on", activeSaber(saber));
        player.gameMode.useItem(player, level, saber, InteractionHand.MAIN_HAND);
        check.accept("nano-saber-switches-off", !activeSaber(saber));
    }

    private static boolean activeSaber(ItemStack stack) {
        return stack.getComponents().keySet().stream()
                .anyMatch(
                        type ->
                                BuiltInRegistries.DATA_COMPONENT_TYPE
                                        .getKey(type)
                                        .toString()
                                        .equals("ic2:saber_active"));
    }

    boolean tick(ServerLevel level, BiConsumer<String, Boolean> check) {
        if (done) return true;
        if (firedLaser) {
            if (!level.getBlockState(LASER_TARGET).isAir()) return false;
            check.accept("laser-mined-block", true);
            if (level.getGameTime() < nextShot) return false;
            done = laserModes(level, check);
            return done;
        }
        if (drill.getOrDefault(EnergyTierChecks.energyComponent(), 0.0) < 500) return false;
        var box = (IndustrialMachine) level.getBlockEntity(BOX);
        check.accept(
                "charged-drill-account",
                box.energy() == 0
                        && drill.getDestroySpeed(Blocks.IRON_ORE.defaultBlockState()) == 8);
        var chargedDrill = box.removeItem(0, 1);
        var player = FakePlayerFactory.getMinecraft(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, chargedDrill);
        var iron = BOX.east();
        level.setBlockAndUpdate(iron, Blocks.IRON_ORE.defaultBlockState());
        check.accept(
                "drill-mines-for-50-eu",
                player.gameMode.destroyBlock(iron)
                        && chargedDrill.getOrDefault(EnergyTierChecks.energyComponent(), 0.0)
                                == 450);
        fillJetpack(level, check);
        var names = new String[] {"rubber_boat", "carbon_boat", "electric_boat"};
        for (int i = 0; i < names.length; i++) {
            var center = new BlockPos(19 + i * 4, 113, 27);
            for (var pool :
                    BlockPos.betweenClosed(center.offset(-2, 0, -2), center.offset(2, 0, 2))) {
                level.setBlockAndUpdate(pool.below(), Blocks.STONE.defaultBlockState());
                boolean wall =
                        Math.abs(pool.getX() - center.getX()) == 2
                                || Math.abs(pool.getZ() - center.getZ()) == 2;
                level.setBlockAndUpdate(
                        pool, (wall ? Blocks.STONE : Blocks.WATER).defaultBlockState());
            }
            player.setPos(center.getX() + 0.5, center.getY() + 1.25, center.getZ() + 0.5);
            player.setXRot(90);
            var item = new ItemStack(NativeGameplayFixture.item(names[i]));
            player.setItemInHand(InteractionHand.MAIN_HAND, item);
            check.accept(
                    "launch-" + names[i],
                    player.gameMode
                                    .useItem(player, level, item, InteractionHand.MAIN_HAND)
                                    .consumesAction()
                            && item.isEmpty());
            String type = "ic2:" + names[i];
            check.accept(
                    "entity-" + names[i],
                    level.getEntitiesOfClass(
                                            Entity.class,
                                            new AABB(center).inflate(2),
                                            entity ->
                                                    BuiltInRegistries.ENTITY_TYPE
                                                            .getKey(entity.getType())
                                                            .toString()
                                                            .equals(type))
                                    .size()
                            == 1);
        }
        level.setBlockAndUpdate(LASER_TARGET, Blocks.STONE.defaultBlockState());
        player.setPos(LASER_ORIGIN.getX() + 0.5, LASER_ORIGIN.getY(), LASER_ORIGIN.getZ() + 0.5);
        player.setXRot(0);
        player.setYRot(90);
        player.setYHeadRot(90);
        var laser = new ItemStack(NativeGameplayFixture.item("mining_laser"));
        laser.set(EnergyTierChecks.energyComponent(), 2_000.0);
        player.setItemInHand(InteractionHand.MAIN_HAND, laser);
        check.accept(
                "laser-fires-and-debits",
                player.gameMode
                                .useItem(player, level, laser, InteractionHand.MAIN_HAND)
                                .consumesAction()
                        && laser.getOrDefault(EnergyTierChecks.energyComponent(), 0.0) == 750);
        firedLaser = true;
        nextShot = level.getGameTime() + 6;
        return false;
    }

    static void verifyColdBoats(ServerLevel level, BiConsumer<String, Boolean> check) {
        var basin = new AABB(17, 110, 24, 29, 117, 31);
        for (String kind : new String[] {"rubber_boat", "carbon_boat", "electric_boat"}) {
            boolean restored =
                    level.getEntitiesOfClass(
                                            Entity.class,
                                            basin,
                                            entity ->
                                                    BuiltInRegistries.ENTITY_TYPE
                                                            .getKey(entity.getType())
                                                            .toString()
                                                            .equals("ic2:" + kind))
                                    .size()
                            == 1;
            check.accept("cold-saved-" + kind, restored);
        }
    }

    private boolean laserModes(ServerLevel level, BiConsumer<String, Boolean> check) {
        var costs = new int[] {1250, 100, 5000, 3000, 2500, 10000, 5000, 3000};
        var shots = new int[] {1, 1, 1, 1, 1, 25, 1, 9};
        var player = FakePlayerFactory.getMinecraft(level);
        var origin = LASER_ORIGIN.above(28).west();
        player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
        player.setYRot(90);
        player.setYHeadRot(90);
        var stack = new ItemStack(NativeGameplayFixture.item("mining_laser"));
        stack.set(EnergyTierChecks.energyComponent(), 12_000.0);
        stack.set(laserModeComponent(), Math.min(modeCheck, 7));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setXRot(modeCheck == 8 ? 75 : 0);
        var sight = new AABB(origin.above()).inflate(3);
        int before = laserBolts(level, sight);
        if (modeCheck == 8) {
            stack.set(laserModeComponent(), 3);
            player.gameMode.useItem(player, level, stack, InteractionHand.MAIN_HAND);
            check.accept(
                    "laser-steep-aim-costs-zero",
                    laserBolts(level, sight) == before
                            && stack.getOrDefault(EnergyTierChecks.energyComponent(), 0.0)
                                    == 12_000);
            return true;
        }
        var result = player.gameMode.useItem(player, level, stack, InteractionHand.MAIN_HAND);
        check.accept(
                "laser-mode-" + modeCheck,
                result.consumesAction()
                        && laserBolts(level, sight) - before == shots[modeCheck]
                        && stack.getOrDefault(EnergyTierChecks.energyComponent(), 0.0)
                                == 12_000 - costs[modeCheck]);
        modeCheck++;
        nextShot = level.getGameTime() + 6;
        return false;
    }

    private static int laserBolts(ServerLevel level, AABB sight) {
        return level.getEntitiesOfClass(
                        Entity.class,
                        sight,
                        entity ->
                                BuiltInRegistries.ENTITY_TYPE
                                        .getKey(entity.getType())
                                        .toString()
                                        .equals("ic2:laser_bullet"))
                .size();
    }

    @SuppressWarnings("unchecked")
    private static DataComponentType<Integer> laserModeComponent() {
        return (DataComponentType<Integer>)
                BuiltInRegistries.DATA_COMPONENT_TYPE.stream()
                        .filter(
                                type ->
                                        BuiltInRegistries.DATA_COMPONENT_TYPE
                                                .getKey(type)
                                                .toString()
                                                .equals("ic2:laser_mode"))
                        .findFirst()
                        .orElseThrow();
    }

    private void fillJetpack(ServerLevel level, BiConsumer<String, Boolean> check) {
        var pos = BOX.south(3);
        var player = FakePlayerFactory.getMinecraft(level);
        NativeGameplayFixture.place(level, player, "bronze_tank", pos);
        var pack = NativeGameplayFixture.item("jetpack");
        NativeTransferAccess.fill(
                level, pos, Direction.UP, new FluidStack(Fluids.WATER, 1_000), false);
        var returned = TransportChecks.click(level, pos, Direction.UP, new ItemStack(pack));
        check.accept(
                "jetpack-rejects-water",
                NativeTransferAccess.itemFluidVolume(returned) == 0
                        && NativeTransferAccess.drain(level, pos, Direction.UP, 1_000, false)
                                == 1_000);
        var biogas =
                BuiltInRegistries.FLUID.stream()
                        .filter(
                                fluid ->
                                        BuiltInRegistries.FLUID
                                                .getKey(fluid)
                                                .toString()
                                                .equals("ic2:biogas"))
                        .findFirst()
                        .orElseThrow();
        NativeTransferAccess.fill(level, pos, Direction.UP, new FluidStack(biogas, 1_000), false);
        returned = TransportChecks.click(level, pos, Direction.UP, new ItemStack(pack));
        check.accept(
                "jetpack-filled-with-biogas",
                NativeTransferAccess.itemFluidVolume(returned) == 1_000
                        && NativeTransferAccess.volume(level, pos, Direction.UP) == 0);
    }
}
