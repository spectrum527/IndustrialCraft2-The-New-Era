package com.industrialage.ia2.world;

import com.industrialage.ia2.content.ProcessingUpgrades;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Selected-face machine upgrade transfers through the authoritative native inventory APIs. */
public final class MachineAutomation {
    private MachineAutomation() {}

    /**
     * Resolves the processing parameters from the actual upgrade stacks installed by the player.
     */
    public static ProcessingUpgrades installed(List<ItemStack> stacks) {
        return ProcessingUpgrades.count(
                id ->
                        stacks.stream()
                                .filter(stack -> id.equals(WorldNative.itemId(stack)))
                                .mapToInt(ItemStack::getCount)
                                .sum());
    }

    /**
     * Whether a machine with basic item processing can accept this upgrade in its own inventory.
     */
    public static boolean isProcessingUpgrade(ItemStack stack) {
        return ProcessingUpgrades.accepts(WorldNative.itemId(stack));
    }

    /** Qualified item ID of a native installed stack. */
    public static String id(ItemStack stack) {
        return WorldNative.itemId(stack);
    }

    /** Executes the item upgrades in slot order after the hosted process has worked. */
    public static boolean transfer(
            ServerLevel level,
            BlockPos position,
            Container inventory,
            int inputSlot,
            int outputSlot,
            List<ItemStack> installed,
            Predicate<ItemStack> validInput) {
        boolean changed = false;
        for (var stack : installed) {
            var type = WorldNative.itemId(stack);
            boolean eject =
                    type.equals("ic2:ejector_upgrade")
                            || type.equals("ic2:advanced_ejector_upgrade");
            boolean pull =
                    type.equals("ic2:pulling_upgrade")
                            || type.equals("ic2:advanced_pulling_upgrade");
            if (!eject && !pull) continue;
            var settings = settings(stack);
            int limit = ProcessingUpgrades.transferLimit(stack.getCount());
            for (var face : settings.faces()) {
                var neighbor = position.relative(face);
                if (!level.hasChunkAt(neighbor)) continue;
                if (eject) {
                    changed |=
                            eject(
                                    level,
                                    neighbor,
                                    face.getOpposite(),
                                    inventory,
                                    outputSlot,
                                    settings,
                                    limit);
                } else {
                    changed |=
                            pull(
                                    level,
                                    neighbor,
                                    face.getOpposite(),
                                    inventory,
                                    inputSlot,
                                    settings,
                                    limit,
                                    validInput);
                }
            }
        }
        return changed;
    }

    private static boolean eject(
            ServerLevel level,
            BlockPos target,
            Direction face,
            Container inventory,
            int outputSlot,
            Settings settings,
            int limit) {
        if (outputSlot < 0) return false;
        var stack = inventory.getItem(outputSlot);
        if (stack.isEmpty() || !settings.matches(stack)) return false;
        var offered = stack.copy();
        offered.setCount(Math.min(stack.getCount(), limit));
        int moved = WorldItemAccess.push(level, target, face, offered);
        if (moved <= 0) return false;
        inventory.removeItem(outputSlot, moved);
        return true;
    }

    private static boolean pull(
            ServerLevel level,
            BlockPos from,
            Direction face,
            Container inventory,
            int inputSlot,
            Settings settings,
            int limit,
            Predicate<ItemStack> validInput) {
        if (inputSlot < 0) return false;
        var existing = inventory.getItem(inputSlot);
        int space = inventory.getMaxStackSize() - existing.getCount();
        if (!existing.isEmpty())
            space = Math.min(space, existing.getMaxStackSize() - existing.getCount());
        if (space <= 0) return false;
        Predicate<ItemStack> allowed =
                stack ->
                        (existing.isEmpty() || WorldNative.sameStack(stack, existing))
                                && settings.matches(stack)
                                && validInput.test(stack);
        var moved =
                WorldItemAccess.pullMatching(level, from, face, Math.min(space, limit), allowed);
        if (moved.isEmpty()) return false;
        if (!allowed.test(moved)) {
            int returned = WorldItemAccess.push(level, from, face, moved);
            moved.shrink(returned);
            if (!moved.isEmpty()) Block.popResource(level, from, moved);
            return false;
        }
        if (existing.isEmpty()) inventory.setItem(inputSlot, moved);
        else {
            existing.grow(moved.getCount());
            inventory.setChanged();
        }
        return true;
    }

    static boolean selectSide(Level level, Player player, ItemStack upgrade, Direction face) {
        if (!player.isShiftKeyDown() || !isRouted(WorldNative.itemId(upgrade))) return false;
        if (level instanceof ServerLevel) {
            var current = settings(upgrade);
            Direction side = current.side == face ? null : face;
            save(upgrade, new Settings(side, current.item, current.denied));
            WorldNative.reply(
                    player,
                    Component.translatable(
                            "ic2.upgrades.route",
                            Component.translatable(
                                    side == null
                                            ? "ic2.upgrades.all_faces"
                                            : "ic2.upgrades.face." + side.getName())));
        }
        return true;
    }

    static boolean filter(Level level, Player player, ItemStack upgrade, ItemStack sample) {
        String path = WorldNative.itemId(upgrade);
        if (!(path.equals("ic2:advanced_ejector_upgrade")
                || path.equals("ic2:advanced_pulling_upgrade"))) return false;
        if (level instanceof ServerLevel) {
            var current = settings(upgrade);
            String id = sample.isEmpty() ? "" : WorldNative.itemId(sample);
            boolean denied = !id.isEmpty() && player.isShiftKeyDown();
            save(upgrade, new Settings(current.side, id, denied));
            WorldNative.reply(
                    player,
                    id.isEmpty()
                            ? Component.translatable("ic2.upgrades.filter_clear")
                            : Component.translatable(
                                    denied
                                            ? "ic2.upgrades.filter_exclude"
                                            : "ic2.upgrades.filter_include",
                                    sample.getHoverName()));
        }
        return true;
    }

    private static boolean isRouted(String path) {
        return switch (path) {
            case "ic2:ejector_upgrade",
                            "ic2:advanced_ejector_upgrade",
                            "ic2:pulling_upgrade",
                            "ic2:advanced_pulling_upgrade",
                            "ic2:fluid_ejector_upgrade",
                            "ic2:fluid_pulling_upgrade" ->
                    true;
            default -> false;
        };
    }

    /** Configured transfer faces, or all faces when the player has selected no particular side. */
    public static List<Direction> faces(ItemStack upgrade) {
        return settings(upgrade).faces();
    }

    private static Settings settings(ItemStack stack) {
        String[] data = WorldNative.upgradeConfig(stack).split(",", -1);
        if (data.length != 3) return new Settings(null, "", false);
        Direction side = null;
        for (Direction direction : Direction.values()) {
            if (direction.getName().equals(data[0])) side = direction;
        }
        return new Settings(side, data[1], Boolean.parseBoolean(data[2]));
    }

    private static void save(ItemStack stack, Settings settings) {
        WorldNative.setUpgradeConfig(
                stack,
                String.join(
                        ",",
                        settings.side == null ? "" : settings.side.getName(),
                        settings.item,
                        Boolean.toString(settings.denied)));
    }

    private record Settings(Direction side, String item, boolean denied) {
        List<Direction> faces() {
            return side == null ? List.of(Direction.values()) : List.of(side);
        }

        boolean matches(ItemStack stack) {
            return item.isEmpty() || denied != item.equals(WorldNative.itemId(stack));
        }
    }
}
