package com.industrialage.ia2.slottest;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.power.WorldPowerStorage;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Native scenarios for locked processing inputs, power items, and completed products. */
public final class SlotRulesChecks {
    /** One automation view obtained through the loader's actual capability API. */
    public interface Port {
        /** Moves as many of the offered items as the real eligible slots accept. */
        int offer(ItemStack stack, boolean simulate);

        /** Tries a single exposed native slot, including a simulation of partial acceptance. */
        int offerAt(int slot, ItemStack stack, boolean simulate);

        /** Takes from the first permitted product slot. */
        ItemStack take(boolean simulate);
    }

    /** Adapter for the loader's native capability and portable charge storage. */
    public interface Host {
        /** Finds an active port on a physical face or via unsided lookup. */
        Port port(ServerLevel level, BlockPos pos, Direction side);

        /** Sets the known test item's native charge. */
        void charge(ItemStack battery, double eu);
    }

    private final BlockPos origin;
    private final Function<String, Block> blocks;
    private final Function<String, ItemStack> items;
    private final Host host;
    private final int baseOutput;
    private AbstractContainerMenu washer;
    private AbstractContainerMenu furnace;
    private AbstractContainerMenu batteryBox;
    private boolean checked;

    /** Creates the runtime fixture; base machines have different menu indices on Forge and Neo. */
    public SlotRulesChecks(
            BlockPos origin,
            Function<String, Block> blocks,
            Function<String, ItemStack> items,
            Host host,
            int baseOutput) {
        this.origin = origin;
        this.blocks = blocks;
        this.items = items;
        this.host = host;
        this.baseOutput = baseOutput;
    }

    /** Verifies each scoped automation inlet and seeds genuine conversions for the next ticks. */
    public void place(ServerLevel level, ServerPlayer player, BiConsumer<String, Boolean> check) {
        var macerator = origin;
        checkProcessor(
                level,
                player,
                check,
                macerator,
                "macerator",
                new ItemStack(Items.COAL, 2),
                items.apply("coal_dust"),
                baseOutput,
                3 - baseOutput);
        checkProcessor(
                level,
                player,
                check,
                origin.east(2),
                "metal_former",
                new ItemStack(Items.COPPER_INGOT, 2),
                items.apply("copper_cable"),
                9,
                26);
        checkProcessor(
                level,
                player,
                check,
                origin.east(4),
                "induction_furnace",
                new ItemStack(Items.RAW_IRON, 2),
                new ItemStack(Items.IRON_INGOT),
                9,
                26);
        var cutterPos = origin.south(3);
        var cutter = place(level, player, cutterPos, "block_cutter");
        var blade = items.apply("iron_cutting_blade");
        var cutterPort = host.port(level, cutterPos, Direction.NORTH);
        check.accept(
                "processing-tool-is-manually-installable-only",
                cutter.getSlot(4).mayPlace(blade) && cutterPort.offer(blade, false) == 0);
        cutter.getSlot(4).set(blade);
        check.accept("processing-installed-blade-never-exports", cutterPort.take(false).isEmpty());
        var heaterPos = origin.east(8).south(3);
        var heater = place(level, player, heaterPos, "rt_heat_generator");
        var pellet = items.apply("rtg_pellet");
        var heaterPort = host.port(level, heaterPos, Direction.NORTH);
        check.accept(
                "processing-radioisotope-pellet-manual-only",
                heater.getSlot(0).mayPlace(pellet) && heaterPort.offer(pellet, false) == 0);
        heater.getSlot(0).set(pellet);
        check.accept(
                "processing-installed-radioisotope-pellet-is-protected",
                heaterPort.take(false).isEmpty());
        var washerPos = origin.east(2).south(3);
        washer = place(level, player, washerPos, "ore_washing_plant");
        var washerPort = host.port(level, washerPos, Direction.NORTH);
        check.accept(
                "processing-washer-rejects-wrong-fluid-and-empty-containers",
                washerPort.offer(new ItemStack(Items.LAVA_BUCKET), false) == 0
                        && washerPort.offer(new ItemStack(Items.BUCKET), false) == 0);
        check.accept(
                "processing-washer-accepts-water-and-ore",
                washerPort.offer(new ItemStack(Items.WATER_BUCKET), false) == 1
                        && washer.getSlot(6).getItem().is(Items.WATER_BUCKET)
                        && washerPort.offer(items.apply("crushed_tin"), false) == 1
                        && washerPort.take(false).isEmpty());
        var furnacePos = origin.east(4).south(3);
        furnace = place(level, player, furnacePos, "iron_furnace");
        var furnacePort = host.port(level, furnacePos, Direction.NORTH);
        check.accept(
                "processing-furnace-retains-unconsumed-fuel",
                furnacePort.offer(new ItemStack(Items.LAVA_BUCKET), false) == 1
                        && furnacePort.take(false).isEmpty());
        check.accept(
                "processing-furnace-accepts-smelting-input",
                furnacePort.offer(new ItemStack(Items.RAW_IRON), false) == 1);
        var boxPos = origin.east(6).south(3);
        batteryBox = place(level, player, boxPos, "batbox");
        var box = host.port(level, boxPos, Direction.WEST);
        var empty = items.apply("re_battery");
        empty.setCount(3);
        check.accept(
                "processing-stackable-empty-batteries-bounded-at-charge-port",
                box.offerAt(1, empty, false) == 0
                        && box.offer(empty, true) == 1
                        && batteryBox.getSlot(0).getItem().isEmpty()
                        && box.offer(empty, false) == 1
                        && box.offer(empty, false) == 0
                        && batteryBox.getSlot(0).getItem().getCount() == 1
                        && empty.getCount() == 3);
        batteryBox.getSlot(0).set(ItemStack.EMPTY);
        var full = items.apply("re_battery");
        host.charge(full, 10_000);
        check.accept(
                "processing-batbox-full-battery-routes-to-discharge-and-is-protected",
                box.offer(full, false) == 1
                        && batteryBox.getSlot(0).getItem().isEmpty()
                        && batteryBox.getSlot(1).getItem().is(full.getItem())
                        && box.take(false).isEmpty());
        batteryBox.getSlot(1).set(ItemStack.EMPTY);
        var partial = items.apply("re_battery");
        host.charge(partial, 9_950);
        check.accept(
                "processing-batbox-direct-discharge-admits-partial-battery",
                box.offerAt(1, partial, true) == 1
                        && batteryBox.getSlot(1).getItem().isEmpty()
                        && box.offerAt(1, partial, false) == 1
                        && box.offerAt(1, partial, false) == 0
                        && batteryBox.getSlot(1).getItem().getCount() == 1
                        && box.take(false).isEmpty());
        batteryBox.getSlot(1).set(ItemStack.EMPTY);
        check.accept(
                "processing-batbox-partial-battery-routes-to-charge-and-waits",
                box.offer(partial, false) == 1
                        && batteryBox.getSlot(0).getItem().is(partial.getItem())
                        && batteryBox.getSlot(1).getItem().isEmpty()
                        && box.take(false).isEmpty());
        check.accept(
                "processing-batbox-charged-with-local-power",
                ((WorldPowerStorage) level.getBlockEntity(boxPos))
                        .exchangeStoredEnergy(100, false));
    }

    private void checkProcessor(
            ServerLevel level,
            ServerPlayer player,
            BiConsumer<String, Boolean> check,
            BlockPos pos,
            String kind,
            ItemStack feed,
            ItemStack result,
            int output,
            int batterySlot) {
        var menu = place(level, player, pos, kind);
        var port = host.port(level, pos, Direction.NORTH);
        var cachedUnsided = host.port(level, pos, null);
        check.accept(
                "processing-rejects-wrong-items-" + kind,
                port.offer(new ItemStack(Items.DIRT), false) == 0
                        && port.offer(new ItemStack(Items.BUCKET), false) == 0
                        && port.offer(items.apply("overclocker_upgrade"), false) == 0
                        && port.offer(items.apply("lapotron_crystal"), false) == 0
                        && port.offer(result, false) == 0);
        check.accept(
                "processing-accepts-real-unpowered-feed-" + kind,
                port.offer(feed, false) == 2
                        && menu.getSlot(0).getItem().is(feed.getItem())
                        && menu.getSlot(0).getItem().getCount() == 2);
        var battery = items.apply("re_battery");
        check.accept(
                "processing-empty-supply-battery-refused-" + kind,
                port.offer(battery, false) == 0 && menu.getSlot(batterySlot).getItem().isEmpty());
        host.charge(battery, 30);
        check.accept(
                "processing-supply-battery-simulation-and-one-accepted-" + kind,
                port.offer(battery, true) == 1
                        && menu.getSlot(batterySlot).getItem().isEmpty()
                        && port.offer(battery, false) == 1
                        && port.offer(battery, false) == 0
                        && menu.getSlot(batterySlot).getItem().getCount() == 1);
        menu.getSlot(output == 9 ? 18 : 3).set(items.apply("overclocker_upgrade"));
        boolean locked = cachedUnsided.take(false).isEmpty();
        for (var face : Direction.values()) {
            locked &= host.port(level, pos, face).take(false).isEmpty();
        }
        check.accept("processing-stock-battery-and-upgrades-are-protected-" + kind, locked);
        menu.getSlot(0).set(new ItemStack(Items.DIRT));
        check.accept(
                "processing-obsolete-input-stays-manually-recoverable-" + kind,
                cachedUnsided.take(false).isEmpty()
                        && port.take(false).isEmpty()
                        && menu.getSlot(0).mayPickup(player));
        menu.getSlot(0).set(feed.copy());
        menu.getSlot(output).set(result.copy());
        var entity = level.getBlockEntity(pos);
        setSide(entity, Direction.NORTH, MachineAccess.Mode.BLOCKED);
        check.accept(
                "processing-cached-face-and-unsided-deny-newly-blocked-settings-" + kind,
                cachedUnsided.offer(feed, false) == 0
                        && cachedUnsided.take(false).isEmpty()
                        && port.offer(feed, false) == 0
                        && port.take(false).isEmpty()
                        && host.port(level, pos, null) == null);
        var outlet = host.port(level, pos, Direction.SOUTH);
        check.accept(
                "processing-only-native-product-is-extracted-" + kind,
                outlet.take(true).is(result.getItem())
                        && menu.getSlot(output).getItem().is(result.getItem())
                        && outlet.take(false).is(result.getItem())
                        && outlet.take(false).isEmpty()
                        && menu.getSlot(0).getItem().getCount() == 2
                        && menu.getSlot(batterySlot).getItem().getCount() == 1);
    }

    private AbstractContainerMenu place(
            ServerLevel level, ServerPlayer player, BlockPos pos, String kind) {
        level.setBlockAndUpdate(pos, blocks.apply(kind).defaultBlockState());
        return ((MenuProvider) level.getBlockEntity(pos))
                .createMenu(0, player.getInventory(), player);
    }

    /**
     * Confirms rebuilt fuel vessels and the finished charger product after actual machine ticks.
     */
    public boolean tick(ServerLevel level, BiConsumer<String, Boolean> check) {
        if (checked) return true;
        if (!furnace.getSlot(1).getItem().is(Items.BUCKET)
                || !washer.getSlot(14).getItem().is(Items.BUCKET)) return false;
        var washerPort = host.port(level, origin.east(2).south(3), Direction.NORTH);
        var furnacePort = host.port(level, origin.east(4).south(3), Direction.NORTH);
        var boxPort = host.port(level, origin.east(6).south(3), Direction.WEST);
        if (boxPort.take(true).isEmpty()) return false;
        check.accept(
                "processing-empty-vessel-from-dedicated-washer-result",
                washerPort.take(false).is(Items.BUCKET)
                        && washerPort.take(false).isEmpty()
                        && washer.getSlot(0).getItem().is(items.apply("crushed_tin").getItem()));
        check.accept(
                "processing-transformed-same-fuel-slot-is-an-output",
                furnacePort.take(true).is(Items.BUCKET)
                        && furnacePort.take(false).is(Items.BUCKET)
                        && furnacePort.take(false).isEmpty()
                        && furnace.getSlot(0).getItem().is(Items.RAW_IRON));
        check.accept(
                "processing-only-completed-batbox-charge-leaves",
                boxPort.take(false).is(items.apply("re_battery").getItem())
                        && batteryBox.getSlot(0).getItem().isEmpty()
                        && batteryBox.getSlot(1).getItem().isEmpty());
        checked = true;
        return true;
    }

    private static void setSide(BlockEntity entity, Direction face, MachineAccess.Mode mode) {
        for (int i = 0; i < 6; i++) {
            if (MachineAccess.worldSide(MachineAccess.front(entity), i) == face) {
                if (!MachineAccess.set(entity, i, mode.ordinal()))
                    throw new AssertionError("Mode refused");
                return;
            }
        }
        throw new AssertionError("No mode for " + face);
    }
}
