package com.industrialage.ia2.compatqa;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.industrial.PowerNode;
import com.industrialage.ia2.logistics.TransportNode;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The packaged native transfer APIs enforce machine slot roles, independent of GUI access. */
final class MachineSlotRoleChecks {
    interface ItemsPort {
        boolean accepts(int slot, ItemStack stack);

        int insert(int slot, ItemStack stack, boolean simulate);

        ItemStack take(int slot, int maximum, boolean simulate);
    }

    private MachineSlotRoleChecks() {}

    static void run(ServerLevel level, ServerPlayer player, List<String> report) {
        var pos = new BlockPos(6, 150, 6);
        power(level, player, pos);
        processors(level, player, pos.above(8));
        routes(level, player, pos.above(16));
        trading(level, player, pos.above(24));
        reactor(level, player, pos.above(32));
        hatch(level, player, pos.above(60));
        nozzle(level, player, pos.above(68));
        report.add(
                "Power and World slot roles: native insert/simulation/commit, vanilla hoppers, a fitted nozzle, charging and reactor completion, routing and trade inputs, reactor hatch and manual controls.");
    }

    private static void power(ServerLevel level, ServerPlayer player, BlockPos pos) {
        block(level, pos, "classic_mfe");
        var menu = menu(level, player, pos);
        var port = items(level, pos, Direction.UP);
        var uncharged = stack("re_battery", 4);
        var nearlyFull = stack("re_battery", 1);
        var charged = stack("re_battery", 1);
        SlotRoleHost.charge(nearlyFull, 9999);
        SlotRoleHost.charge(charged, 10_000);
        require(port.accepts(0, uncharged), "classic MFE detects charging work");
        require(
                port.insert(0, uncharged, true) == 1 && menu.getSlot(0).getItem().isEmpty(),
                "battery intake quotes one unit without mutating old stack");
        require(
                !port.accepts(0, charged) && port.insert(0, charged, false) == 0,
                "already full supply does not occupy an empty charge bay");
        require(
                port.insert(1, uncharged, false) == 0
                        && port.insert(1, charged, false) == 1
                        && port.take(1, 1, true).isEmpty()
                        && port.take(1, 1, false).isEmpty(),
                "supply bay accepts live battery and protects it from automation");
        require(menu.getSlot(1).mayPickup(player), "player can still remove a supply battery");
        require(
                port.insert(0, nearlyFull, false) == 1
                        && port.take(0, 1, true).isEmpty()
                        && port.take(0, 1, false).isEmpty(),
                "battery with 9999/10000 EU is not a completed item");
        var sink = hopper(level, pos.below());
        tickHopper(level, sink);
        require(sink.isEmpty(), "vanilla lower hopper cannot take the unfinished battery");
        ((PowerNode) level.getBlockEntity(pos)).receive(1);
        // An empty discharge slot isolates the last EU used by the charging bay.
        menu.getSlot(1).set(ItemStack.EMPTY);
        tick(level, pos);
        require(
                SlotRoleHost.energy(menu.getSlot(0).getItem()) == 10_000
                        && SlotRoleHost.energy(port.take(0, 1, true)) == 10_000,
                "the actual charging tick releases the full battery for extraction");
        tickHopper(level, sink);
        require(
                SlotRoleHost.energy(sink.getItem(0)) == 10_000
                        && menu.getSlot(0).getItem().isEmpty(),
                "vanilla hopper collects exactly the finished charge");
        level.removeBlock(pos.below(), false);

        block(level, pos, "rt_generator");
        menu = menu(level, player, pos);
        port = items(level, pos, Direction.UP);
        var pellet = stack("rtg_pellet", 1);
        require(
                menu.getSlot(0).mayPlace(pellet)
                        && !port.accepts(0, pellet)
                        && port.insert(0, pellet, false) == 0,
                "permanent RTG pellet is installed by hand");
        menu.getSlot(0).set(pellet);
        tick(level, pos);
        require(
                menu.getSlot(0).getItem().is(CompatHost.item("rtg_pellet"))
                        && port.take(0, 1, false).isEmpty(),
                "RTG operation keeps its pellet installed");

        block(level, pos, "rci_rsh");
        menu = menu(level, player, pos);
        port = items(level, pos, Direction.DOWN);
        var redstone = new ItemStack(Items.REDSTONE_BLOCK, 3);
        var upgrade = stack("pulling_upgrade", 1);
        require(
                port.insert(0, redstone, false) == 3
                        && port.take(0, 3, false).isEmpty()
                        && port.take(1, 1, false).isEmpty(),
                "injector accepts only its reagent and cannot unload it");
        require(
                menu.getSlot(10).mayPlace(upgrade)
                        && !port.accepts(10, upgrade)
                        && port.insert(10, upgrade, false) == 0,
                "injector upgrade remains an exclusive manual control");
        menu.getSlot(1).set(new ItemStack(Items.BUCKET));
        require(port.take(1, 1, false).isEmpty(), "legacy misplaced bucket is not an output");

        block(level, pos, "geo_generator");
        menu = menu(level, player, pos);
        port = items(level, pos, Direction.UP);
        var lava = new ItemStack(Items.LAVA_BUCKET);
        require(
                port.insert(0, new ItemStack(Items.WATER_BUCKET), false) == 0
                        && port.insert(0, lava, true) == 1
                        && lava.is(Items.LAVA_BUCKET)
                        && lava.getCount() == 1
                        && menu.getSlot(0).getItem().isEmpty(),
                "native geothermal probe rejects water and leaves the offered lava and machine untouched");
        require(
                port.insert(0, lava, false) == 1 && port.take(0, 1, false).isEmpty(),
                "geothermal valid fuel enters, and cannot leave the input");
        tick(level, pos);
        require(
                menu.getSlot(1).getItem().is(Items.BUCKET)
                        && port.take(1, 1, false).is(Items.BUCKET),
                "geothermal processing produces a genuine extractable empty bucket");
    }

    private static void processors(ServerLevel level, ServerPlayer player, BlockPos pos) {
        block(level, pos, "advanced_miner");
        var menu = menu(level, player, pos);
        var port = items(level, pos, Direction.UP);
        var filter = new ItemStack(Items.IRON_ORE);
        require(
                menu.getSlot(1).mayPlace(filter)
                        && !port.accepts(1, filter)
                        && port.insert(1, filter, false) == 0,
                "advanced miner's filter is hand-configured");
        menu.getSlot(1).set(filter);
        require(
                port.take(1, 1, false).isEmpty() && menu.getSlot(1).mayPickup(player),
                "filter is retained, with ordinary manual removal");
        var scanner = stack("advanced_scanner", 1);
        require(
                menu.getSlot(0).mayPlace(scanner) && port.insert(0, scanner, false) == 0,
                "advanced miner requires a hand-installed scanner");
        menu.getSlot(0).set(scanner);
        require(port.take(0, 1, false).isEmpty(), "installed scanner is retained");
        var supply = stack("energy_crystal", 1);
        SlotRoleHost.charge(supply, 2000);
        var wrongTier = stack("lapotron_crystal", 1);
        SlotRoleHost.charge(wrongTier, 2000);
        require(
                port.insert(8, wrongTier, false) == 0
                        && port.insert(8, supply, false) == 1
                        && port.take(8, 1, false).isEmpty(),
                "advanced miner accepts compatible providers, never supplying them to automation");
        menu.getSlot(4).set(new ItemStack(Items.RAW_IRON, 2));
        require(
                port.take(4, 1, true).is(Items.RAW_IRON)
                        && menu.getSlot(4).getItem().getCount() == 2
                        && port.take(4, 2, false).getCount() == 2,
                "mined result supports simulated and actual withdrawal");

        block(level, pos, "miner");
        menu = menu(level, player, pos);
        port = items(level, pos, Direction.UP);
        var drill = stack("drill", 1);
        var minerScanner = stack("scanner", 1);
        var miningPipe = stack("mining_pipe", 2);
        require(
                menu.getSlot(0).mayPlace(drill) && menu.getSlot(1).mayPlace(minerScanner),
                "ordinary miner still permits hand installation");
        require(
                !port.accepts(0, drill)
                        && port.insert(0, drill, false) == 0
                        && !port.accepts(1, minerScanner)
                        && port.insert(1, minerScanner, false) == 0,
                "ordinary miner drill and scanner are hand-installed");
        require(
                port.insert(2, miningPipe, false) == 2 && port.take(2, 2, false).isEmpty(),
                "miner accepts the expendable tubes without exposing them as finished output");

        block(level, pos, "terraformer");
        menu = menu(level, player, pos);
        port = items(level, pos, Direction.UP);
        var blueprint = stack("cultivation_tfbp", 1);
        var poweredTool = stack("drill", 1);
        SlotRoleHost.charge(poweredTool, 1000);
        var terraSupply = stack("energy_crystal", 1);
        SlotRoleHost.charge(terraSupply, 2000);
        require(
                menu.getSlot(0).mayPlace(blueprint)
                        && !port.accepts(0, blueprint)
                        && port.insert(0, blueprint, false) == 0,
                "the terraforming blueprint is a persistent, manual program");
        menu.getSlot(0).set(blueprint);
        require(
                port.take(0, 1, false).isEmpty()
                        && !port.accepts(8, poweredTool)
                        && port.insert(8, poweredTool, false) == 0
                        && port.insert(8, terraSupply, false) == 1,
                "the terraformer keeps its program, rejects powered non-supply tools and accepts native energy supply");

        block(level, pos, "batch_crafter");
        menu = menu(level, player, pos);
        port = items(level, pos, Direction.NORTH);
        var plank = new ItemStack(Items.OAK_PLANKS);
        menu.getSlot(18).set(plank.copy());
        require(
                !port.accepts(18, plank)
                        && port.insert(18, plank, false) == 0
                        && port.take(18, 1, false).isEmpty(),
                "craft template is excluded from item automation");
        require(
                port.insert(0, new ItemStack(Items.DIRT), false) == 0
                        && port.insert(0, plank, false) == 1
                        && port.take(0, 1, false).isEmpty(),
                "batch inputs must match their template");
        menu.getSlot(9).set(new ItemStack(Items.STICK, 4));
        require(port.take(9, 4, false).getCount() == 4, "batch craft output remains extractable");
    }

    private static void routes(ServerLevel level, ServerPlayer player, BlockPos pos) {
        for (String id : List.of("sorting_machine", "weighted_item_distributor", "item_buffer")) {
            block(level, pos, id);
            level.setBlockAndUpdate(
                    pos,
                    level.getBlockState(pos)
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
            var front = items(level, pos, Direction.EAST);
            var back = items(level, pos, Direction.WEST);
            var menu = menu(level, player, pos);
            var cargo = new ItemStack(Items.COBBLESTONE, 4);
            var control = id.equals("item_buffer") ? stack("ejector_upgrade", 1) : cargo;
            int controlSlot = id.equals("item_buffer") ? 26 : 18;
            require(
                    front.insert(0, cargo, false) == 4
                            && !front.accepts(controlSlot, control)
                            && front.insert(controlSlot, control, false) == 0,
                    id + " cargo and configuration have distinct input roles");
            menu.getSlot(controlSlot).set(control.copy());
            require(
                    back.take(0, 4, false).isEmpty() && front.take(controlSlot, 1, false).isEmpty(),
                    id + " preserves its designated route and template");
            if (id.equals("item_buffer")) {
                require(
                        front.take(0, 4, false).getCount() == 4,
                        "buffer yields cargo from the established front outlet");
            } else {
                require(
                        front.take(0, 4, false).isEmpty(),
                        id + " cargo remains queued for its active routing");
            }
            if (id.equals("weighted_item_distributor")) {
                require(
                        back.insert(1, cargo, false) == 0,
                        "weighted router only accepts from the designated intake");
            }
        }
    }

    private static void trading(ServerLevel level, ServerPlayer player, BlockPos pos) {
        for (String id : List.of("trade_o_mat", "energy_o_mat")) {
            block(level, pos, id);
            var menu = menu(level, player, pos);
            var port = items(level, pos, Direction.UP);
            var diamond = new ItemStack(Items.DIAMOND, 2);
            var coal = new ItemStack(Items.COAL, 3);
            require(!port.accepts(2, diamond), "unpriced trader cannot accept arbitrary deposits");
            menu.getSlot(0).set(diamond.copy());
            require(
                    port.take(0, 1, false).isEmpty()
                            && port.insert(2, coal, false) == 0
                            && port.insert(2, diamond, false) == 2
                            && port.take(2, 2, false).isEmpty(),
                    id + " protects the quote and takes only matching payment");
            if (id.equals("trade_o_mat")) {
                menu.getSlot(1).set(coal.copy());
                require(
                        port.take(1, 1, false).isEmpty(),
                        "merchant goods sample remains installed");
                level.setBlockAndUpdate(pos.east(), Blocks.CHEST.defaultBlockState());
                ((net.minecraft.world.Container) level.getBlockEntity(pos.east()))
                        .setItem(0, coal.copy());
                tick(level, pos);
                require(
                        port.take(3, 3, false).getCount() == 3,
                        "native trade yields actual sold goods");
                level.removeBlock(pos.east(), false);
            } else {
                var battery = stack("re_battery", 1);
                require(port.insert(3, battery, false) == 0, "customer bay is manually assigned");
                menu.getSlot(3).set(battery);
                require(port.take(3, 1, false).isEmpty(), "customer-owned item cannot be siphoned");
            }
        }
    }

    private static void reactor(ServerLevel level, ServerPlayer player, BlockPos position) {
        var pos = position;
        while (Math.floorMod(level.getGameTime() + pos.asLong(), 20) != 0) pos = pos.above();
        block(level, pos, "nuclear_reactor");
        level.setBlockAndUpdate(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        var menu = menu(level, player, pos);
        var port = items(level, pos, Direction.WEST);
        var fuel = stack("uranium_fuel_rod", 1);
        var breeder = stack("depleted_isotope_fuel_rod", 1);
        SlotRoleHost.wear(fuel, 19_999);
        SlotRoleHost.wear(breeder, 9_999);
        var vent = stack("heat_vent", 1);
        require(
                port.insert(0, fuel, false) == 1
                        && port.insert(1, breeder, false) == 1
                        && port.insert(2, vent, false) == 0
                        && port.take(0, 1, false).isEmpty()
                        && port.take(1, 1, false).isEmpty(),
                "fuel and breeder can be fed while cooling equipment must be installed manually");
        require(menu.getSlot(2).mayPlace(vent), "manual cooling installation remains available");
        menu.getSlot(2).set(vent);
        tick(level, pos);
        require(
                port.take(0, 1, false).is(CompatHost.item("depleted_uranium_fuel_rod"))
                        && port.take(1, 1, false).is(CompatHost.item("re_enriched_uranium"))
                        && port.take(2, 1, false).isEmpty()
                        && menu.getSlot(2).mayPickup(player),
                "actual reactor cycle unlocks same-slot products and retains permanent vent");
        var lithium = stack("lithium_fuel_rod", 1);
        SlotRoleHost.wear(lithium, 9_998);
        require(
                port.insert(0, lithium, false) == 1 && port.take(0, 1, false).isEmpty(),
                "lithium's unchanged item ID is not enough to mark it complete");
        var fullyIrradiated = stack("lithium_fuel_rod", 1);
        SlotRoleHost.wear(fullyIrradiated, 9_999);
        menu.getSlot(0).set(fullyIrradiated);
        require(
                port.take(0, 1, false).is(CompatHost.item("lithium_fuel_rod"))
                        && port.insert(0, fullyIrradiated, false) == 0,
                "lithium completion permits output without re-inserting it as feed");
    }

    private static void hatch(ServerLevel level, ServerPlayer player, BlockPos core) {
        for (int x = -2; x <= 2; x++)
            for (int y = -2; y <= 2; y++)
                for (int z = -2; z <= 2; z++)
                    if (Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) == 2)
                        block(level, core.offset(x, y, z), "reactor_vessel");
        for (Direction side : Direction.values())
            block(level, core.relative(side), "reactor_chamber");
        block(level, core, "nuclear_reactor");
        var hatch = core.east(2);
        block(level, hatch, "reactor_access_hatch");
        tick(level, core);
        var menu = menu(level, player, core);
        var port = items(level, hatch, Direction.EAST);
        var input = stack("depleted_isotope_fuel_rod", 1);
        require(
                port.insert(0, input, false) == 1 && port.take(0, 1, false).isEmpty(),
                "formed hatch keeps core input in place");
        menu.getSlot(0).set(stack("re_enriched_uranium", 1));
        var controller = level.getBlockEntity(core);
        for (int local = 0; local < 6; local++) {
            if (MachineAccess.worldSide(MachineAccess.front(controller), local) != Direction.EAST)
                continue;
            require(
                    MachineAccess.set(controller, local, 3) && port.take(0, 1, false).isEmpty(),
                    "cached formed hatch honors active controller mode");
            require(MachineAccess.set(controller, local, 0), "restore controller default");
        }
        require(
                items(level, hatch, Direction.EAST)
                        .take(0, 1, false)
                        .is(CompatHost.item("re_enriched_uranium")),
                "formed/reactivated hatch exposes actual reactor products");
    }

    private static void nozzle(ServerLevel level, ServerPlayer player, BlockPos source) {
        block(level, source, "advanced_miner");
        var menu = menu(level, player, source);
        menu.getSlot(0).set(stack("advanced_scanner", 1));
        menu.getSlot(1).set(new ItemStack(Items.DIAMOND_ORE));
        var pipe = source.east();
        level.setBlockAndUpdate(
                pipe,
                CompatHost.block("bronze_item_pipe_small")
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.NORTH));
        var fitting = stack("pipe_extraction_nozzle", 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, fitting);
        var contact = Vec3.atCenterOf(pipe).add(-.5, 0, 0);
        player.setPos(pipe.getX() - .1, pipe.getY() + .5, pipe.getZ() + .5);
        player.gameMode.useItemOn(
                player,
                level,
                fitting,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(contact, Direction.WEST, pipe, false));
        require(
                ((TransportNode) level.getBlockEntity(pipe)).nozzle(Direction.WEST)
                        == TransportNode.Nozzle.EXTRACTION,
                "fitting attached to the source");
        tick(level, pipe);
        var output = items(level, pipe, Direction.NORTH);
        require(
                output.take(0, 4, false).isEmpty()
                        && menu.getSlot(0).getItem().is(CompatHost.item("advanced_scanner"))
                        && menu.getSlot(1).getItem().is(Items.DIAMOND_ORE),
                "fitted extraction cannot move a scanner or mining filter");
        menu.getSlot(4).set(new ItemStack(Items.RAW_IRON, 2));
        tick(level, pipe);
        require(
                output.take(0, 4, false).is(Items.RAW_IRON)
                        && menu.getSlot(4).getItem().isEmpty()
                        && menu.getSlot(0).getItem().is(CompatHost.item("advanced_scanner"))
                        && menu.getSlot(1).getItem().is(Items.DIAMOND_ORE),
                "fitted extraction moves the actual output and preserves both held components");
    }

    private static void block(ServerLevel level, BlockPos pos, String id) {
        level.setBlockAndUpdate(pos, CompatHost.block(id).defaultBlockState());
    }

    private static AbstractContainerMenu menu(
            ServerLevel level, ServerPlayer player, BlockPos pos) {
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + 1.5);
        return ((MenuProvider) level.getBlockEntity(pos))
                .createMenu(1, player.getInventory(), player);
    }

    private static ItemsPort items(ServerLevel level, BlockPos pos, Direction side) {
        var port = SlotRoleHost.item(level, pos, side);
        require(port != null, "expected a native item capability at " + pos);
        return port;
    }

    private static ItemStack stack(String id, int count) {
        return new ItemStack(CompatHost.item(id), count);
    }

    private static HopperBlockEntity hopper(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.HOPPER.defaultBlockState());
        return (HopperBlockEntity) level.getBlockEntity(pos);
    }

    private static void tickHopper(ServerLevel level, HopperBlockEntity hopper) {
        for (int i = 0; i < 16; i++) tick(level, hopper.getBlockPos());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void tick(ServerLevel level, BlockPos pos) {
        var device = level.getBlockEntity(pos);
        var state = level.getBlockState(pos);
        BlockEntityTicker ticker =
                ((EntityBlock) state.getBlock()).getTicker(level, state, device.getType());
        require(ticker != null, "expected a native ticker at " + pos);
        ticker.tick(level, pos, state, device);
    }

    private static void require(boolean condition, String failure) {
        if (!condition) throw new AssertionError(failure);
    }
}
