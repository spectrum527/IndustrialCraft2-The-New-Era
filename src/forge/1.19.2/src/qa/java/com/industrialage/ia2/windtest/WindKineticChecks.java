package com.industrialage.ia2.windtest;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Dedicated-server wind line exercised via survival placement, menu clicks and wrench use. */
public final class WindKineticChecks {
    private final Function<String, Block> block;
    private final Function<String, Item> item;
    private final BiConsumer<String, Boolean> check;
    private final BlockPos wind;
    private final BlockPos converter;
    private final BlockPos battery;
    private ServerPlayer player;
    private int stage;
    private int stageTicks;
    private double previousEu;
    private double receivedKu;
    private double blockedEu;

    public WindKineticChecks(
            Function<String, Block> block,
            Function<String, Item> item,
            BiConsumer<String, Boolean> check,
            BlockPos wind) {
        this.block = block;
        this.item = item;
        this.check = check;
        this.wind = wind;
        this.converter = wind.east();
        this.battery = wind.east(2);
    }

    public void start(ServerLevel level, ServerPlayer player, boolean cold) {
        this.player = player;
        // Loading these chunks makes the complete largest rotor sweep observable without forced
        // loads
        // inside the production code.
        int cx = wind.getX() >> 4;
        int cz = wind.getZ() >> 4;
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                level.setChunkForced(x, z, true);
                level.getChunk(x, z);
            }
        }
        stage = cold ? 100 : 0;
    }

    public boolean tick(ServerLevel level) {
        if (!level.isPositionEntityTicking(wind)) return false;
        if (stage != 0 && !node(level, wind).ranThisTick(level)) return false;
        if (stage >= 2
                && stage != 100
                && stage != 101
                && !node(level, converter).ranThisTick(level)) return false;
        if (stage >= 100 && !node(level, converter).ranThisTick(level)) return false;
        switch (stage) {
            case 0 -> {
                place(level, wind, "wind_kinetic_generator", 90);
                check.accept(
                        "survival-wind-facing-output-east",
                        direction(level, wind) == Direction.EAST);
                WorkshopMenu menu = menu(level, wind);
                check.accept(
                        "rotor-slot-rejects-stone",
                        !menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)));
                install(menu, "steel_rotor");
                check.accept(
                        "shift-click-steel-into-rotor-slot",
                        menu.getSlot(0).getItem().is(item.apply("steel_rotor")));
                next();
            }
            case 1 -> {
                if (++stageTicks < 4) break;
                var node = node(level, wind);
                var menu = menu(level, wind);
                if (menu.motion() <= 0 || node.visualRotorSpeed() != menu.motion()) {
                    throw new AssertionError(
                            "Open unloaded wind: menu="
                                    + menu.motion()
                                    + ", visual="
                                    + node.visualRotorSpeed()
                                    + ", potential="
                                    + node.windPotential(level)
                                    + ", rotor="
                                    + node.windRotor()
                                    + ", position="
                                    + wind);
                }
                check.accept(
                        "open-unloaded-available-ku",
                        menu.motion() > 0 && node.visualRotorSpeed() == menu.motion());
                check.accept("steel-visual-type", node.visualRotorType() == 4);
                var tag = node.getUpdatePacket().getTag();
                check.accept(
                        "minimal-wind-update-payload",
                        tag != null
                                && tag.size() == 2
                                && tag.contains("rotor")
                                && tag.contains("speed"));
                place(level, converter, "kinetic_generator", -90);
                place(level, battery, "classic_mfsu", 90);
                check.accept(
                        "survival-converter-faces-wind",
                        direction(level, converter) == Direction.WEST);
                next();
            }
            case 2 -> {
                if (++stageTicks < 5) break;
                check.accept(
                        "wind-reaches-converter-and-load",
                        menu(level, wind).motion() > 0
                                && menu(level, converter).motion() > 0
                                && batteryEu(level) > 0);
                previousEu = totalEu(level);
                receivedKu = 0;
                next();
            }
            case 3 -> {
                receivedKu += menu(level, converter).motion();
                if (++stageTicks < 4) break;
                double gain = totalEu(level) - previousEu;
                check.accept("four-ku-becomes-one-eu", Math.abs(gain - receivedKu / 4.0) < 0.0001);
                level.setBlockAndUpdate(wind.west().above(4), Blocks.STONE.defaultBlockState());
                next();
            }
            case 4 -> {
                if (++stageTicks == 4) {
                    check.accept(
                            "steel-sweep-blocks-available-and-draw",
                            menu(level, wind).motion() == 0
                                    && menu(level, converter).motion() == 0);
                    blockedEu = totalEu(level);
                }
                if (stageTicks < 7) break;
                check.accept(
                        "blocked-line-does-not-produce",
                        Math.abs(totalEu(level) - blockedEu) < 0.0001);
                WorkshopMenu menu = menu(level, wind);
                click(menu, 0, "PICKUP");
                check.accept(
                        "normal-click-removes-steel-rotor",
                        menu.getSlot(0).getItem().isEmpty()
                                && menu.getCarried().is(item.apply("steel_rotor")));
                menu.setCarried(ItemStack.EMPTY);
                install(menu, "wooden_rotor");
                check.accept(
                        "shift-click-wood-into-rotor-slot",
                        menu.getSlot(0).getItem().is(item.apply("wooden_rotor")));
                next();
            }
            case 5 -> {
                if (++stageTicks < 4) break;
                check.accept(
                        "same-obstacle-outside-wooden-sweep",
                        menu(level, wind).motion() > 0
                                && menu(level, converter).motion() > 0
                                && node(level, wind).visualRotorType() == 1);
                level.setBlockAndUpdate(wind.west().above(4), Blocks.AIR.defaultBlockState());
                wrench(level, Direction.NORTH);
                check.accept("wrench-turns-output-face", direction(level, wind) == Direction.NORTH);
                next();
            }
            case 6 -> {
                if (++stageTicks == 4) {
                    check.accept(
                            "wrong-output-face-keeps-unloaded-ku",
                            menu(level, wind).motion() > 0 && menu(level, converter).motion() == 0);
                    blockedEu = totalEu(level);
                }
                if (stageTicks < 7) break;
                check.accept(
                        "wrong-output-face-no-receiver-increment",
                        Math.abs(totalEu(level) - blockedEu) < 0.0001);
                wrench(level, Direction.EAST);
                check.accept(
                        "wrench-restores-output-face", direction(level, wind) == Direction.EAST);
                next();
            }
            case 7 -> {
                if (++stageTicks < 4) break;
                check.accept(
                        "wrench-reconnected-line-recovers", menu(level, converter).motion() > 0);
                place(level, wind.south(5), "wind_kinetic_generator", 90);
                install(menu(level, wind.south(5)), "steel_rotor");
                next();
            }
            case 8 -> {
                if (++stageTicks < 5) break;
                check.accept(
                        "overlapping-loaded-rotors-stop",
                        menu(level, wind).motion() == 0
                                && menu(level, wind.south(5)).motion() == 0);
                level.destroyBlock(wind.south(5), false);
                next();
            }
            case 9 -> {
                if (++stageTicks < 5) break;
                check.accept(
                        "overlap-removal-recovers",
                        menu(level, wind).motion() > 0 && menu(level, converter).motion() > 0);
                var menu = menu(level, wind);
                click(menu, 0, "PICKUP");
                check.accept(
                        "pickup-retains-wooden-rotor",
                        menu.getCarried().is(item.apply("wooden_rotor")));
                player.getInventory().setItem(0, menu.getCarried().copy());
                menu.setCarried(ItemStack.EMPTY);
                next();
            }
            case 10 -> {
                if (++stageTicks < 4) break;
                check.accept(
                        "empty-rotor-stops-wind-and-converter",
                        menu(level, wind).motion() == 0
                                && menu(level, converter).motion() == 0
                                && node(level, wind).visualRotorType() == 0);
                click(menu(level, wind), 54, "QUICK_MOVE");
                next();
            }
            case 11 -> {
                if (++stageTicks < 4) break;
                check.accept(
                        "same-rotor-reinstalled-and-running",
                        menu(level, wind).getSlot(0).getItem().is(item.apply("wooden_rotor"))
                                && menu(level, converter).motion() > 0);
                return true;
            }
            case 100 -> {
                if (++stageTicks < 6) break;
                check.accept(
                        "restart-rotor-and-facing-saved",
                        direction(level, wind) == Direction.EAST
                                && menu(level, wind)
                                        .getSlot(0)
                                        .getItem()
                                        .is(item.apply("wooden_rotor")));
                check.accept(
                        "restart-ku-chain-active",
                        menu(level, wind).motion() > 0 && menu(level, converter).motion() > 0);
                previousEu = batteryEu(level);
                next();
            }
            case 101 -> {
                if (++stageTicks < 5) break;
                check.accept("restart-storage-increases", batteryEu(level) > previousEu);
                return true;
            }
            default -> throw new IllegalStateException("Unexpected wind test stage " + stage);
        }
        return false;
    }

    private void next() {
        stage++;
        stageTicks = 0;
    }

    private WorkshopNode node(ServerLevel level, BlockPos pos) {
        return (WorkshopNode) level.getBlockEntity(pos);
    }

    private WorkshopMenu menu(ServerLevel level, BlockPos pos) {
        var menu = (WorkshopMenu) node(level, pos).createMenu(1, player.getInventory(), player);
        player.containerMenu = menu;
        return menu;
    }

    private void install(WorkshopMenu menu, String rotor) {
        player.getInventory().setItem(0, new ItemStack(item.apply(rotor)));
        click(menu, 54, "QUICK_MOVE");
    }

    private void click(WorkshopMenu menu, int slot, String action) {
        // Minecraft 26 renamed ClickType to ContainerInput; invoke the same native menu API.
        try {
            var method =
                    java.util.Arrays.stream(menu.getClass().getMethods())
                            .filter(
                                    candidate ->
                                            candidate.getName().equals("clicked")
                                                    && candidate.getParameterCount() == 4)
                            .findFirst()
                            .orElseThrow();
            Object click =
                    java.util.Arrays.stream(method.getParameterTypes()[2].getEnumConstants())
                            .filter(value -> value.toString().equals(action))
                            .findFirst()
                            .orElseThrow();
            method.invoke(menu, slot, 0, click, player);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Native menu click failed", error);
        }
    }

    private double batteryEu(ServerLevel level) {
        return ((PowerNode) level.getBlockEntity(battery)).storedEu();
    }

    private double totalEu(ServerLevel level) {
        return batteryEu(level) + ((EuBuffer) node(level, converter)).bufferedEu().stored();
    }

    private Direction direction(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.getValue(
                state.hasProperty(BlockStateProperties.FACING)
                        ? BlockStateProperties.FACING
                        : BlockStateProperties.HORIZONTAL_FACING);
    }

    private void place(ServerLevel level, BlockPos pos, String id, float yaw) {
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        player.setPos(pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5);
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(0);
        var stack = new ItemStack(block.apply(id));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false);
        check.accept(
                "survival-place-" + id + "-" + pos.getZ(),
                player.gameMode
                                .useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit)
                                .consumesAction()
                        && stack.isEmpty()
                        && level.getBlockState(pos).is(block.apply(id)));
    }

    private void wrench(ServerLevel level, Direction side) {
        player.setPos(wind.getX() + 2.5, wind.getY(), wind.getZ() + 0.5);
        var stack = new ItemStack(item.apply("wrench"));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = WrenchHits.turn(level, wind, side);
        check.accept(
                "native-wrench-" + side.getName(),
                player.gameMode
                        .useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit)
                        .consumesAction());
    }
}
