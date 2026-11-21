package com.industrialage.ia2.watertest;

import com.industrialage.ia2.industrial.PowerNode;
import com.industrialage.ia2.industrial.WorkshopMenu;
import com.industrialage.ia2.industrial.WorkshopNode;
import com.industrialage.ia2.power.EuBuffer;
import com.industrialage.ia2.wrenchtest.WrenchHits;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Native survival assembly and dedicated-server water kinetic line checks. */
public final class WaterKineticChecks {
    private final Function<String, Block> block;
    private final Function<String, Item> item;
    private final BiConsumer<String, Boolean> check;
    private final BlockPos water;
    private final BlockPos converter;
    private final BlockPos battery;
    private final String phase;
    private ServerPlayer player;
    private int stage;
    private int stageTicks;
    private double previousEu;
    private double receivedKu;
    private double blockedEu;

    public WaterKineticChecks(
            Function<String, Block> block,
            Function<String, Item> item,
            BiConsumer<String, Boolean> check,
            BlockPos water,
            String phase) {
        this.block = block;
        this.item = item;
        this.check = check;
        this.water = water;
        this.converter = water.east();
        this.battery = water.east(2);
        this.phase = phase;
    }

    public void start(ServerLevel level, ServerPlayer player) {
        this.player = player;
        int cx = water.getX() >> 4;
        int cz = water.getZ() >> 4;
        for (int x = cx - 1; x <= cx + 1; x++)
            for (int z = cz - 1; z <= cz + 1; z++) {
                level.setChunkForced(x, z, true);
                level.getChunk(x, z);
            }
        stage = phase.equals("waterRead") ? 100 : phase.equals("waterWrite") ? 0 : 200;
    }

    public boolean tick(ServerLevel level) {
        if (!level.isPositionEntityTicking(water)) return false;
        if (stage != 0 && stage != 200 && !node(level, water).ranThisTick(level)) return false;
        if ((stage >= 4 && stage <= 30 || stage >= 100 && stage <= 102 || stage >= 201)
                && level.getBlockEntity(converter) instanceof WorkshopNode sink
                && !sink.ranThisTick(level)) return false;
        switch (stage) {
            case 0 -> {
                check.accept("registered-river-biome", level.getBiome(water).is(Biomes.RIVER));
                placeTop(level, water, "water_kinetic_generator", 90);
                level.setBlockAndUpdate(water.below(), Blocks.WATER.defaultBlockState());
                check.accept(
                        "survival-water-facing-east", direction(level, water) == Direction.EAST);
                var menu = menu(level, water);
                check.accept(
                        "rotor-slot-rejects-wood-and-stone",
                        !menu.getSlot(0).mayPlace(new ItemStack(item.apply("wooden_rotor")))
                                && !menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)));
                check.accept(
                        "rotor-slot-accepts-four-materials",
                        menu.getSlot(0).mayPlace(stack("bronze_rotor"))
                                && menu.getSlot(0).mayPlace(stack("iron_rotor"))
                                && menu.getSlot(0).mayPlace(stack("steel_rotor"))
                                && menu.getSlot(0).mayPlace(stack("carbon_rotor")));
                player.getInventory().setItem(0, stack("wooden_rotor"));
                menu.clicked(54, 0, ClickType.QUICK_MOVE, player);
                check.accept(
                        "quick-move-rejects-wood",
                        menu.getSlot(0).getItem().isEmpty()
                                && player.getInventory().getItem(0).is(item.apply("wooden_rotor")));
                level.setBlockAndUpdate(water.above(), Blocks.HOPPER.defaultBlockState());
                hopper(level).setItem(0, stack("wooden_rotor"));
                next();
            }
            case 1 -> {
                if (++stageTicks < 20) break;
                check.accept(
                        "automation-rejects-wood",
                        menu(level, water).getSlot(0).getItem().isEmpty()
                                && hopper(level).getItem(0).is(item.apply("wooden_rotor")));
                hopper(level).setItem(0, stack("bronze_rotor"));
                next();
            }
            case 2 -> {
                if (++stageTicks < 20) break;
                var menu = menu(level, water);
                check.accept(
                        "automation-inserts-bronze",
                        menu.getSlot(0).getItem().is(item.apply("bronze_rotor"))
                                && hopper(level).getItem(0).isEmpty());
                level.setBlockAndUpdate(water.above(), Blocks.WATER.defaultBlockState());
                menu.clicked(0, 0, ClickType.PICKUP, player);
                check.accept(
                        "normal-pickup-bronze", menu.getCarried().is(item.apply("bronze_rotor")));
                menu.setCarried(ItemStack.EMPTY);
                install(menu, "steel_rotor");
                check.accept(
                        "quick-move-inserts-steel",
                        menu.getSlot(0).getItem().is(item.apply("steel_rotor")));
                next();
            }
            case 3 -> {
                if (++stageTicks < 4) break;
                var source = node(level, water);
                check.accept(
                        "isolated-pool-unloaded-available-ku",
                        menu(level, water).motion() == 4
                                && source.visualRotorSpeed() == 4
                                && source.visualRotorType() == 4);
                var tag = source.getUpdatePacket().getTag();
                check.accept(
                        "visual-packet-only-rotor-and-speed",
                        tag != null
                                && tag.size() == 2
                                && tag.contains("rotor")
                                && tag.contains("speed"));
                placeTop(level, converter, "kinetic_generator", -90);
                placeTop(level, battery, "classic_mfsu", 90);
                check.accept(
                        "survival-converter-faces-water",
                        direction(level, converter) == Direction.WEST);
                next();
            }
            case 4 -> {
                if (++stageTicks < 5) break;
                check.accept(
                        "water-to-eu-load",
                        menu(level, water).motion() == 4
                                && menu(level, converter).motion() == 4
                                && batteryEu(level, battery) > 0);
                previousEu = totalEu(level);
                receivedKu = 0;
                next();
            }
            case 5 -> {
                receivedKu += menu(level, converter).motion();
                if (++stageTicks < 4) break;
                check.accept(
                        "four-ku-per-eu",
                        Math.abs(totalEu(level) - previousEu - receivedKu / 4) < 0.0001);
                level.setBlockAndUpdate(water.west().above(3), Blocks.STONE.defaultBlockState());
                next();
            }
            case 6 -> {
                if (++stageTicks == 4) {
                    check.accept(
                            "steel-radius-three-blocks",
                            menu(level, water).motion() == 0
                                    && menu(level, converter).motion() == 0);
                    blockedEu = totalEu(level);
                }
                if (stageTicks < 7) break;
                check.accept(
                        "blocked-water-line-no-energy",
                        Math.abs(totalEu(level) - blockedEu) < 0.0001);
                level.setBlockAndUpdate(water.west().above(3), Blocks.WATER.defaultBlockState());
                level.setBlockAndUpdate(water.west().above(4), Blocks.STONE.defaultBlockState());
                next();
            }
            case 7 -> {
                if (++stageTicks < 4) break;
                check.accept(
                        "outside-steel-radius-keeps-running",
                        menu(level, water).motion() == 4 && menu(level, converter).motion() == 4);
                level.setBlockAndUpdate(water.west().above(4), Blocks.WATER.defaultBlockState());
                level.setBlockAndUpdate(
                        water.west().above(2), Blocks.STRUCTURE_VOID.defaultBlockState());
                next();
            }
            case 8 -> {
                var gap = water.west().above(2);
                check.accept(
                        "dry-gap-has-no-collision-or-water",
                        level.getBlockState(gap).getCollisionShape(level, gap).isEmpty()
                                && !level.getFluidState(gap)
                                        .is(net.minecraft.tags.FluidTags.WATER));
                check.accept(
                        "partial-disc-without-water-stops",
                        menu(level, water).motion() == 0 && menu(level, converter).motion() == 0);
                level.setBlockAndUpdate(water.west().above(2), Blocks.WATER.defaultBlockState());
                next();
            }
            case 9 -> {
                if (++stageTicks < 4) break;
                check.accept(
                        "restored-water-resumes",
                        menu(level, water).motion() == 4 && menu(level, converter).motion() == 4);
                placeTop(level, water.north(), "kinetic_generator", 180);
                check.accept(
                        "second-generator-aims-at-water",
                        direction(level, water.north()) == Direction.SOUTH);
                previousEu = totalEu(level);
                receivedKu = 0;
                next();
            }
            case 10 -> {
                receivedKu += menu(level, converter).motion();
                if (++stageTicks < 4) break;
                check.accept(
                        "other-face-cannot-draw",
                        menu(level, water).motion() == 4
                                && menu(level, converter).motion() == 4
                                && menu(level, water.north()).motion() == 0);
                check.accept(
                        "multiple-consumers-do-not-duplicate-available-work",
                        Math.abs(totalEu(level) - previousEu - receivedKu / 4) < 0.0001);
                level.setBlockAndUpdate(water.north(), Blocks.WATER.defaultBlockState());
                level.setBlockAndUpdate(water.north().below(), Blocks.WATER.defaultBlockState());
                wrench(level, Direction.NORTH);
                next();
            }
            case 11 -> {
                if (++stageTicks == 4) {
                    check.accept(
                            "wrong-output-face-keeps-available-ku",
                            menu(level, water).motion() == 4
                                    && menu(level, converter).motion() == 0);
                    blockedEu = totalEu(level);
                }
                if (stageTicks < 7) break;
                check.accept("wrong-face-no-energy", Math.abs(totalEu(level) - blockedEu) < 0.0001);
                wrench(level, Direction.SOUTH);
                next();
            }
            case 12 -> {
                if (++stageTicks < 4) break;
                check.accept(
                        "south-facing-unloaded",
                        menu(level, water).motion() == 4 && menu(level, converter).motion() == 0);
                wrench(level, Direction.WEST);
                // The existing converter is now in the swept plane: move it out before testing
                // west.
                level.setBlockAndUpdate(converter, Blocks.WATER.defaultBlockState());
                level.setBlockAndUpdate(converter.below(), Blocks.WATER.defaultBlockState());
                next();
            }
            case 13 -> {
                if (++stageTicks < 4) break;
                check.accept("west-facing-unloaded", menu(level, water).motion() == 4);
                placeTop(level, converter, "kinetic_generator", -90);
                wrench(level, Direction.EAST);
                next();
            }
            case 14 -> {
                if (++stageTicks < 4) break;
                check.accept("horizontal-output-restored", menu(level, converter).motion() == 4);
                // The earlier temporary placement supports sit in the new horizontal rotor disk.
                level.setBlockAndUpdate(converter.below(), Blocks.WATER.defaultBlockState());
                level.setBlockAndUpdate(battery.below(), Blocks.WATER.defaultBlockState());
                wrench(level, Direction.UP);
                placeSide(level, water.above(), "kinetic_generator", -90);
                next();
            }
            case 15 -> {
                if (++stageTicks < 5) break;
                check.accept(
                        "up-facing-sends-actual-ku",
                        direction(level, water) == Direction.UP
                                && direction(level, water.above()) == Direction.DOWN
                                && menu(level, water).motion() == 4
                                && menu(level, water.above()).motion() == 4
                                && ((EuBuffer) node(level, water.above())).bufferedEu().stored()
                                        > 0);
                level.setBlockAndUpdate(water.above(), Blocks.WATER.defaultBlockState());
                wrench(level, Direction.DOWN);
                placeSide(level, water.below(), "kinetic_generator", 90);
                next();
            }
            case 16 -> {
                if (++stageTicks < 5) break;
                check.accept(
                        "down-facing-sends-actual-ku",
                        direction(level, water) == Direction.DOWN
                                && direction(level, water.below()) == Direction.UP
                                && menu(level, water).motion() == 4
                                && menu(level, water.below()).motion() == 4
                                && ((EuBuffer) node(level, water.below())).bufferedEu().stored()
                                        > 0);
                level.setBlockAndUpdate(water.below(), Blocks.WATER.defaultBlockState());
                wrench(level, Direction.EAST);
                next();
            }
            case 17 -> {
                if (++stageTicks < 4) break;
                check.accept(
                        "horizontal-reconnect-after-vertical",
                        menu(level, converter).motion() == 4);
                replaceRotor("carbon_rotor", level, water);
                placeTop(level, water.south(2), "water_kinetic_generator", 90);
                level.setBlockAndUpdate(water.south(2).below(), Blocks.WATER.defaultBlockState());
                install(menu(level, water.south(2)), "carbon_rotor");
                next();
            }
            case 18 -> {
                if (++stageTicks < 5) break;
                check.accept(
                        "overlapping-water-rotors-stop",
                        menu(level, water).motion() == 0
                                && menu(level, water.south(2)).motion() == 0);
                level.setBlockAndUpdate(water.south(2), Blocks.WATER.defaultBlockState());
                next();
            }
            case 19 -> {
                if (++stageTicks < 5) break;
                check.accept(
                        "removing-water-overlap-recovers",
                        menu(level, water).motion() == 4 && menu(level, converter).motion() == 4);
                placeTop(level, water.south(2), "wind_kinetic_generator", 90);
                level.setBlockAndUpdate(water.south(2).below(), Blocks.WATER.defaultBlockState());
                install(menu(level, water.south(2)), "carbon_rotor");
                next();
            }
            case 20 -> {
                if (++stageTicks < 5) break;
                check.accept(
                        "wind-rotor-does-not-block-water-rotor",
                        menu(level, water).motion() == 4 && menu(level, converter).motion() == 4);
                level.setBlockAndUpdate(water.south(2), Blocks.WATER.defaultBlockState());
                replaceRotor("bronze_rotor", level, water);
                next();
            }
            case 21 -> {
                if (++stageTicks < 4) break;
                check.accept("bronze-supplies-isolated-pool-ku", menu(level, water).motion() == 4);
                level.setBlockAndUpdate(water.west().above(2), Blocks.STONE.defaultBlockState());
                next();
            }
            case 22 -> {
                if (++stageTicks < 4) break;
                check.accept("bronze-radius-two-blocks", menu(level, water).motion() == 0);
                level.setBlockAndUpdate(water.west().above(2), Blocks.WATER.defaultBlockState());
                level.setBlockAndUpdate(water.west().above(3), Blocks.STONE.defaultBlockState());
                next();
            }
            case 23 -> {
                if (++stageTicks < 4) break;
                check.accept("bronze-outside-radius-operates", menu(level, water).motion() == 4);
                level.setBlockAndUpdate(water.west().above(3), Blocks.WATER.defaultBlockState());
                replaceRotor("iron_rotor", level, water);
                next();
            }
            case 24 -> {
                if (++stageTicks < 4) break;
                check.accept("iron-supplies-isolated-pool-ku", menu(level, water).motion() == 4);
                level.setBlockAndUpdate(water.west().above(2), Blocks.STONE.defaultBlockState());
                next();
            }
            case 25 -> {
                if (++stageTicks < 4) break;
                check.accept("iron-radius-two-blocks", menu(level, water).motion() == 0);
                level.setBlockAndUpdate(water.west().above(2), Blocks.WATER.defaultBlockState());
                level.setBlockAndUpdate(water.west().above(3), Blocks.STONE.defaultBlockState());
                next();
            }
            case 26 -> {
                if (++stageTicks < 4) break;
                check.accept("iron-outside-radius-operates", menu(level, water).motion() == 4);
                level.setBlockAndUpdate(water.west().above(3), Blocks.WATER.defaultBlockState());
                replaceRotor("carbon_rotor", level, water);
                next();
            }
            case 27 -> {
                if (++stageTicks < 4) break;
                check.accept("carbon-supplies-isolated-pool-ku", menu(level, water).motion() == 4);
                level.setBlockAndUpdate(water.west().above(4), Blocks.STONE.defaultBlockState());
                next();
            }
            case 28 -> {
                if (++stageTicks < 4) break;
                check.accept("carbon-radius-four-blocks", menu(level, water).motion() == 0);
                level.setBlockAndUpdate(water.west().above(4), Blocks.WATER.defaultBlockState());
                level.setBlockAndUpdate(water.west().above(5), Blocks.STONE.defaultBlockState());
                next();
            }
            case 29 -> {
                if (++stageTicks < 4) break;
                check.accept("carbon-outside-radius-operates", menu(level, water).motion() == 4);
                level.setBlockAndUpdate(water.west().above(5), Blocks.WATER.defaultBlockState());
                // This bypasses insertion deliberately to model a pre-existing 1.0.2 save.
                menu(level, water).getSlot(0).set(stack("wooden_rotor"));
                next();
            }
            case 30 -> {
                if (++stageTicks < 4) break;
                check.accept(
                        "legacy-wood-retained-but-stopped",
                        menu(level, water).getSlot(0).getItem().is(item.apply("wooden_rotor"))
                                && menu(level, water).motion() == 0);
                return true;
            }
            case 100 -> {
                if (++stageTicks < 5) break;
                check.accept(
                        "restart-legacy-wood-preserved",
                        direction(level, water) == Direction.EAST
                                && menu(level, water)
                                        .getSlot(0)
                                        .getItem()
                                        .is(item.apply("wooden_rotor")));
                check.accept(
                        "restart-legacy-wood-not-producing",
                        menu(level, water).motion() == 0 && menu(level, converter).motion() == 0);
                var menu = menu(level, water);
                menu.clicked(0, 0, ClickType.PICKUP, player);
                check.accept(
                        "restart-legacy-wood-removable",
                        menu.getCarried().is(item.apply("wooden_rotor")));
                menu.setCarried(ItemStack.EMPTY);
                install(menu, "steel_rotor");
                previousEu = batteryEu(level, battery);
                next();
            }
            case 101 -> {
                if (++stageTicks < 6) break;
                check.accept(
                        "restart-valid-rotor-and-line-recover",
                        menu(level, water).motion() == 4
                                && menu(level, converter).motion() == 4
                                && batteryEu(level, battery) > previousEu);
                return true;
            }
            case 200 -> {
                boolean biome =
                        switch (phase) {
                            case "waterOcean" -> level.getBiome(water).is(Biomes.OCEAN);
                            case "waterDeep" -> level.getBiome(water).is(Biomes.DEEP_OCEAN);
                            case "waterPlains" -> level.getBiome(water).is(Biomes.PLAINS);
                            default -> false;
                        };
                check.accept("registered-biome-" + phase, biome);
                placeTop(level, water, "water_kinetic_generator", 90);
                level.setBlockAndUpdate(water.below(), Blocks.WATER.defaultBlockState());
                install(menu(level, water), "steel_rotor");
                next();
            }
            case 201 -> {
                if (++stageTicks < 5) break;
                int expected = 4;
                check.accept("biome-unloaded-ku-" + phase, menu(level, water).motion() == expected);
                placeTop(level, converter, "kinetic_generator", -90);
                placeTop(level, battery, "classic_mfsu", 90);
                next();
            }
            case 202 -> {
                if (++stageTicks < 6) break;
                int expected = 4;
                check.accept(
                        "biome-received-ku-" + phase, menu(level, converter).motion() == expected);
                check.accept(
                        "biome-storage-" + phase,
                        expected > 0
                                ? batteryEu(level, battery) > 0
                                : batteryEu(level, battery) == 0);
                return true;
            }
            default -> throw new IllegalStateException("Unexpected water test stage " + stage);
        }
        return false;
    }

    private void next() {
        stage++;
        stageTicks = 0;
    }

    private ItemStack stack(String id) {
        return new ItemStack(item.apply(id));
    }

    private WorkshopNode node(ServerLevel level, BlockPos pos) {
        return (WorkshopNode) level.getBlockEntity(pos);
    }

    private HopperBlockEntity hopper(ServerLevel level) {
        return (HopperBlockEntity) level.getBlockEntity(water.above());
    }

    private WorkshopMenu menu(ServerLevel level, BlockPos pos) {
        var menu = (WorkshopMenu) node(level, pos).createMenu(1, player.getInventory(), player);
        player.containerMenu = menu;
        return menu;
    }

    private void install(WorkshopMenu menu, String rotor) {
        player.getInventory().setItem(0, stack(rotor));
        menu.clicked(54, 0, ClickType.QUICK_MOVE, player);
    }

    private void replaceRotor(String rotor, ServerLevel level, BlockPos pos) {
        var menu = menu(level, pos);
        menu.clicked(0, 0, ClickType.PICKUP, player);
        menu.setCarried(ItemStack.EMPTY);
        install(menu, rotor);
    }

    private double batteryEu(ServerLevel level, BlockPos pos) {
        return ((PowerNode) level.getBlockEntity(pos)).storedEu();
    }

    private double totalEu(ServerLevel level) {
        return batteryEu(level, battery)
                + ((EuBuffer) node(level, converter)).bufferedEu().stored();
    }

    private Direction direction(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.getValue(
                state.hasProperty(BlockStateProperties.FACING)
                        ? BlockStateProperties.FACING
                        : BlockStateProperties.HORIZONTAL_FACING);
    }

    private void placeTop(ServerLevel level, BlockPos pos, String id, float yaw) {
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        player.setPos(pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5);
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(0);
        var stack = new ItemStack(block.apply(id));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false);
        check.accept(
                "survival-place-" + id + "-" + pos,
                player.gameMode
                                .useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit)
                                .consumesAction()
                        && stack.isEmpty()
                        && level.getBlockState(pos).is(block.apply(id)));
    }

    private void placeSide(ServerLevel level, BlockPos pos, String id, float pitch) {
        var support = pos.east();
        level.setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
        player.setPos(pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5);
        player.setYRot(90);
        player.setYHeadRot(90);
        player.setXRot(pitch);
        var stack = new ItemStack(block.apply(id));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atCenterOf(support), Direction.WEST, support, false);
        check.accept(
                "survival-place-vertical-" + id + "-" + pos,
                player.gameMode
                                .useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit)
                                .consumesAction()
                        && stack.isEmpty()
                        && level.getBlockState(pos).is(block.apply(id)));
        level.setBlockAndUpdate(support, Blocks.WATER.defaultBlockState());
    }

    private void wrench(ServerLevel level, Direction side) {
        player.setPos(water.getX() + 2.5, water.getY(), water.getZ() + 0.5);
        var stack = stack("wrench");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = WrenchHits.turn(level, water, side);
        check.accept(
                "native-wrench-" + side.getName(),
                player.gameMode
                                .useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit)
                                .consumesAction()
                        && direction(level, water) == side);
    }
}
