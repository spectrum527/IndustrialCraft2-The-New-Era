package com.industrialage.ia2.gametest;

import com.industrialage.ia2.industrial.BoxNode;
import com.industrialage.ia2.industrial.PowerNode;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;

/** Production-JAR acceptance of shell ports, portable boxes, shop settlement and game entities. */
final class UtilityChecks {
    private static final BlockPos SHELL = new BlockPos(6, 180, 6),
            STORE = new BlockPos(5, 190, 5),
            INJECTOR = new BlockPos(6, 200, 5),
            PAD = new BlockPos(4, 215, 7),
            PACK = new BlockPos(8, 225, 8),
            CHARGE = new BlockPos(3, 240, 11),
            TANK = new BlockPos(10, 235, 12);

    private static ItemStack stack(String path) {
        return new ItemStack(NativeGameplayFixture.item(path));
    }

    private static void block(ServerLevel world, BlockPos pos, String path) {
        world.setBlockAndUpdate(pos, NativeGameplayFixture.block(path).defaultBlockState());
    }

    private static PowerNode node(ServerLevel level, BlockPos pos) {
        return (PowerNode) level.getBlockEntity(pos);
    }

    private static AbstractContainerMenu menu(ServerLevel level, BlockPos pos, FakePlayer player) {
        return node(level, pos).createMenu(42, player.getInventory(), player);
    }

    private static int quantity(Container c, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < c.getContainerSize(); i++)
            if (c.getItem(i).is(item)) count += c.getItem(i).getCount();
        return count;
    }

    @SuppressWarnings("unchecked")
    private static ItemStack worn(String id, int wear) {
        var stack = stack(id);
        var prop =
                BuiltInRegistries.DATA_COMPONENT_TYPE.stream()
                        .filter(
                                type ->
                                        BuiltInRegistries.DATA_COMPONENT_TYPE
                                                .getKey(type)
                                                .toString()
                                                .equals("ic2:reactor_use"))
                        .findFirst()
                        .orElseThrow();
        stack.set((DataComponentType<Integer>) prop, wear);
        return stack;
    }

    private static void interact(
            ServerLevel world, FakePlayer player, BlockPos pos, ItemStack held) {
        player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + 1.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        player.gameMode.useItemOn(
                player,
                world,
                held,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false));
    }

    void warm(ServerLevel level) {
        level.setChunkForced(0, 0, true);
        level.getChunk(0, 0);
    }

    void start(ServerLevel level, BiConsumer<String, Boolean> check) {
        for (int x = -2; x <= 2; x++)
            for (int y = -2; y <= 2; y++)
                for (int z = -2; z <= 2; z++)
                    if (Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) == 2)
                        block(level, SHELL.offset(x, y, z), "reactor_vessel");
        block(level, SHELL, "nuclear_reactor");
        for (var dir : Direction.values()) block(level, SHELL.relative(dir), "reactor_chamber");
        block(level, SHELL.west(2), "reactor_redstone_port");
        block(level, SHELL.east(2), "reactor_access_hatch");
        block(level, SHELL.above(2), "reactor_fluid_port");
        var player = FakePlayerFactory.getMinecraft(level);
        for (String fluid : new String[] {"heavy_water", "weed_ex"}) {
            var support = new BlockPos(11, 227, 7);
            var filled = stack(fluid + "_bucket");
            level.setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(support.above(), Blocks.AIR.defaultBlockState());
            player.setPos(support.getX() + .5, support.getY() + 2.2, support.getZ() + .5);
            player.setXRot(90);
            player.setYRot(0);
            player.setItemInHand(InteractionHand.MAIN_HAND, filled);
            player.gameMode.useItem(player, level, filled, InteractionHand.MAIN_HAND);
            check.accept(
                    "survival-bucket-places-" + fluid,
                    level.getBlockState(support.above()).is(NativeGameplayFixture.block(fluid))
                            && level.getFluidState(support.above()).isSource()
                            && player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET));
            level.setBlockAndUpdate(support.above(), Blocks.AIR.defaultBlockState());
        }
        block(level, STORE, "trade_o_mat");
        level.setBlockAndUpdate(STORE.east(), Blocks.CHEST.defaultBlockState());
        var stock = (Container) level.getBlockEntity(STORE.east());
        stock.setItem(0, new ItemStack(Items.COAL, 4));
        var trade = menu(level, STORE, player);
        trade.getSlot(0).set(new ItemStack(Items.DIAMOND));
        trade.getSlot(1).set(new ItemStack(Items.COAL, 2));
        trade.getSlot(2).set(new ItemStack(Items.DIAMOND, 3));
        trade.getSlot(3).set(new ItemStack(Items.APPLE));
        var electrical = STORE.above(4);
        block(level, electrical, "energy_o_mat");
        level.setBlockAndUpdate(electrical.east(), Blocks.BARREL.defaultBlockState());
        var energyShop = menu(level, electrical, player);
        energyShop.getSlot(0).set(new ItemStack(Items.DIAMOND));
        energyShop.getSlot(2).set(new ItemStack(Items.DIAMOND));
        energyShop.getSlot(3).set(stack("re_battery"));
        var safe = STORE.south(4);
        block(level, safe, "personal_chest");
        interact(level, player, safe, ItemStack.EMPTY);
        var storage = (BoxNode) level.getBlockEntity(safe);
        var outsider =
                FakePlayerFactory.get(
                        level,
                        new com.mojang.authlib.GameProfile(
                                java.util.UUID.fromString("f09bb9c5-ee52-432e-bd5c-5d12e7542db1"),
                                "Guest"));
        check.accept(
                "personal-safe-two-players",
                storage.menu(12, player.getInventory(), 0) != null
                        && storage.menu(13, outsider.getInventory(), 0) == null);
        var outsiderShop = menu(level, STORE, outsider);
        check.accept(
                "customer-cannot-change-price",
                !outsiderShop.getSlot(0).mayPickup(outsider)
                        && !outsiderShop.getSlot(0).mayPlace(new ItemStack(Items.DIRT)));
        var tablet = stack("iodine_tablet");
        tablet.setCount(3);
        com.industrialage.ia2.industrial.Radiation.clear(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, tablet);
        tablet.getItem().use(level, player, InteractionHand.MAIN_HAND);
        boolean retained = tablet.getCount() == 3;
        com.industrialage.ia2.industrial.Radiation.expose(player, 40, 1);
        tablet.getItem().use(level, player, InteractionHand.MAIN_HAND);
        boolean first =
                tablet.getCount() == 2 && com.industrialage.ia2.industrial.Radiation.has(player);
        tablet.getItem().use(level, player, InteractionHand.MAIN_HAND);
        check.accept(
                "native-tablet-twenty-ticks-and-healthy-no-consumption",
                retained
                        && first
                        && tablet.getCount() == 1
                        && !com.industrialage.ia2.industrial.Radiation.has(player));
        block(level, INJECTOR.west(), "nuclear_reactor");
        level.setBlockAndUpdate(
                INJECTOR,
                NativeGameplayFixture.block("rci_rsh")
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.EAST));
        var injector = menu(level, INJECTOR, player);
        injector.getSlot(0).set(new ItemStack(Items.REDSTONE_BLOCK, 2));
        node(level, INJECTOR).receive(2000);
        var grid = menu(level, INJECTOR.west(), player);
        grid.getSlot(0).set(worn("rsh_condensator", 17000));
        grid.getSlot(1).set(worn("rsh_condensator", 17001));
        block(level, PAD, "teleporter");
        block(level, PAD.east(6), "teleporter");
        block(level, PAD.west(), "batbox");
        ((com.industrialage.ia2.gameplay.IndustrialMachine) level.getBlockEntity(PAD.west()))
                .receive(1800);
        block(level, PAD.south(), "classic_mfe");
        node(level, PAD.south()).receive(1900);
        var transmitter = stack("frequency_transmitter");
        interact(level, player, PAD, transmitter);
        interact(level, player, PAD.east(6), transmitter);
        level.setBlockAndUpdate(PAD.north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        var cargo =
                new ItemEntity(
                        level,
                        PAD.getX() + .5,
                        PAD.getY() + 1.02,
                        PAD.getZ() + .5,
                        new ItemStack(Items.IRON_NUGGET, 64));
        cargo.setNoGravity(true);
        cargo.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(cargo);
        NativeGameplayFixture.place(level, player, "iridium_storage_box", PACK);
        var box = (BoxNode) level.getBlockEntity(PACK);
        box.menu(3, player.getInventory(), 0).getSlot(0).set(new ItemStack(Items.EMERALD, 3));
        box.menu(4, player.getInventory(), 2).getSlot(35).set(new ItemStack(Items.DIAMOND, 7));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        var predicted =
                net.minecraft.world.level.block.Block.getDrops(
                        level.getBlockState(PACK),
                        level,
                        PACK,
                        box,
                        player,
                        player.getMainHandItem());
        check.accept(
                "portable-box-loot-table",
                predicted.size() == 1
                        && predicted.get(0).is(NativeGameplayFixture.item("iridium_storage_box")));
        check.accept("mined-portable-box", player.gameMode.destroyBlock(PACK));
        var drops =
                level.getEntitiesOfClass(
                        ItemEntity.class,
                        new AABB(PACK).inflate(2),
                        e -> e.getItem().is(NativeGameplayFixture.item("iridium_storage_box")));
        check.accept("single-box-drop", drops.size() == 1);
        var packed = drops.get(0).getItem().copy();
        drops.get(0).discard();
        NativeGameplayFixture.place(level, player, "iridium_storage_box", PACK, packed);
        var returned = (BoxNode) level.getBlockEntity(PACK);
        check.accept(
                "126-slot-box-first-last-replaced",
                returned.menu(5, player.getInventory(), 0).getSlot(0).getItem().getCount() == 3
                        && returned.menu(6, player.getInventory(), 2)
                                        .getSlot(35)
                                        .getItem()
                                        .getCount()
                                == 7);
        check.accept(
                "iridium-author-grid-18-columns-last-slot",
                returned.menu(7, player.getInventory(), 0).getSlot(125).x == 314
                        && returned.menu(7, player.getInventory(), 0)
                                        .getSlot(125)
                                        .getItem()
                                        .getCount()
                                == 7);
        var bench = PACK.above(3);
        block(level, bench, "industrial_workbench");
        var station = (BoxNode) level.getBlockEntity(bench);
        var work = station.menu(8, player.getInventory(), 0);
        work.getSlot(0).set(new ItemStack(Items.OAK_LOG));
        work.getSlot(9).set(stack("forge_hammer"));
        work.getSlot(10).set(new ItemStack(Items.IRON_INGOT, 2));
        work.getSlot(11).set(stack("cutter"));
        work.getSlot(12).set(stack("copper_plate"));
        work.getSlot(30).set(new ItemStack(Items.CARROT, 4));
        work.broadcastChanges();
        check.accept(
                "author-workbench-three-real-recipes-and-buffer",
                work.slots.size() == 70
                        && work.getSlot(31).getItem().is(Items.OAK_PLANKS)
                        && work.getSlot(32).getItem().is(NativeGameplayFixture.item("iron_plate"))
                        && work.getSlot(33).getItem().is(NativeGameplayFixture.item("copper_cable"))
                        && station.menu(9, player.getInventory(), 0)
                                        .getSlot(30)
                                        .getItem()
                                        .getCount()
                                == 4);
        var hammerResult = work.quickMoveStack(player, 32);
        check.accept(
                "authored-workbench-takes-iron-plate",
                hammerResult.is(NativeGameplayFixture.item("iron_plate")));
        check.accept(
                "authored-workbench-debits-single-ingot",
                work.getSlot(10).getItem().getCount() == 1);
        check.accept(
                "authored-workbench-preserves-tool",
                work.getSlot(9).getItem().is(NativeGameplayFixture.item("forge_hammer")));
        player.setPos(bench.getX() + .5, bench.getY() + 1, bench.getZ() + .5);
        check.accept(
                "author-workbench-clear-grid-reclaims-log",
                work.clickMenuButton(player, 0)
                        && work.getSlot(0).getItem().isEmpty()
                        && quantity(player.getInventory(), Items.OAK_LOG) >= 1);
        var tankPos = PACK.above(6);
        block(level, tankPos, "iridium_tank");
        var tank = (com.industrialage.ia2.logistics.TransportNode) level.getBlockEntity(tankPos);
        var tankMenu = tank.createMenu(10, player.getInventory(), player);
        player.setPos(tankPos.getX() + .5, tankPos.getY() + 1, tankPos.getZ() + .5);
        tankMenu.setCarried(new ItemStack(Items.WATER_BUCKET));
        boolean put = tankMenu.clickMenuButton(player, 0) && tankMenu.getCarried().is(Items.BUCKET);
        boolean take =
                tankMenu.clickMenuButton(player, 0) && tankMenu.getCarried().is(Items.WATER_BUCKET);
        check.accept("authored-fluid-well-bucket-roundtrip", put && take);
        block(level, TANK, "tank");
        block(level, TANK.below(), "bronze_tank");
        var upgradable =
                ((net.minecraft.world.MenuProvider) level.getBlockEntity(TANK))
                        .createMenu(11, player.getInventory(), player);
        player.setPos(TANK.getX() + .5, TANK.getY() + 1, TANK.getZ() + .5);
        upgradable.setCarried(new ItemStack(Items.WATER_BUCKET));
        var eject = stack("fluid_ejector_upgrade");
        check.accept(
                "standard-tank-four-upgrades-and-real-fluid",
                upgradable.slots.size() == 40
                        && upgradable.getSlot(0).x == 152
                        && upgradable.getSlot(3).y == 62
                        && upgradable.getSlot(0).mayPlace(eject)
                        && !upgradable.getSlot(0).mayPlace(new ItemStack(Items.DIRT))
                        && upgradable.clickMenuButton(player, 0)
                        && upgradable.getCarried().is(Items.BUCKET));
        upgradable.getSlot(0).set(eject);
        var transformer = PACK.above(9);
        block(level, transformer, "lv_transformer");
        player.setPos(transformer.getX() + .5, transformer.getY() + 1, transformer.getZ() + .5);
        var transform = menu(level, transformer, player);
        check.accept(
                "authored-transformer-control",
                transform.clickMenuButton(player, 0) && transform.clickMenuButton(player, 0));
        var classic = PACK.above(11);
        block(level, classic, "classic_mfe");
        player.setPos(classic.getX() + .5, classic.getY() + .5, classic.getZ() + .5);
        var machine = node(level, classic);
        var classicUI = menu(level, classic, player);
        for (int click = 0; click < 4; click++) classicUI.clickMenuButton(player, 0);
        check.accept(
                "authored-classic-storage-equipment-and-redstone",
                classicUI.getSlot(9).x == 8
                        && classicUI.getSlot(12).x == 62
                        && level.getSignal(classic, Direction.NORTH) == 15);
        block(level, CHARGE, "itnt");
        block(level, CHARGE.east(5), "classic_nuke");
        block(level, CHARGE.east(10), "nuke");
        for (int i = 0; i < 3; i++)
            level.setBlockAndUpdate(
                    CHARGE.east(i * 5).below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
    }

    void coldTank(ServerLevel level, BiConsumer<String, Boolean> check) {
        var player = FakePlayerFactory.getMinecraft(level);
        player.setPos(TANK.getX() + .5, TANK.getY(), TANK.getZ() + .5);
        var upper =
                ((net.minecraft.world.MenuProvider) level.getBlockEntity(TANK))
                        .createMenu(7, player.getInventory(), player);
        var lower =
                ((net.minecraft.world.MenuProvider) level.getBlockEntity(TANK.below()))
                        .createMenu(8, player.getInventory(), player);
        lower.setCarried(new ItemStack(Items.BUCKET));
        check.accept(
                "standard-tank-ejects-full-bucket-and-keeps-upgrade-on-restart",
                upper.getSlot(0).getItem().is(NativeGameplayFixture.item("fluid_ejector_upgrade"))
                        && lower.clickMenuButton(player, 0)
                        && lower.getCarried().is(Items.WATER_BUCKET));
    }

    void sample(ServerLevel level, int tick, BiConsumer<String, Boolean> check) {
        var player = FakePlayerFactory.getMinecraft(level);
        if (tick == 5) {
            for (int x : new int[] {0, 5}) {
                var actors =
                        level.getEntitiesOfClass(
                                PrimedTnt.class, new AABB(CHARGE.east(x)).inflate(3));
                if (actors.size() != 1)
                    throw new AssertionError(
                            "Missing primed "
                                    + x
                                    + " in native chunk, block="
                                    + level.getBlockState(CHARGE.east(x))
                                    + " signal="
                                    + level.hasNeighborSignal(CHARGE.east(x))
                                    + " entities="
                                    + actors);
                check.accept(
                        "timed-game-entity-" + x,
                        actors.size() == 1
                                && actors.get(0).getFuse() >= (x == 0 ? 53 : 293)
                                && actors.get(0).getFuse() <= (x == 0 ? 60 : 300));
                actors.forEach(e -> e.discard());
            }
            check.accept(
                    "empty-game-charge-stays-placed",
                    level.getBlockState(CHARGE.east(10)).is(NativeGameplayFixture.block("nuke")));
        }
        if (tick == 12) {
            check.accept(
                    "one-payment-1000-eu-credit",
                    node(level, STORE.above(4)).receive(2000) == 1000);
        }
        if (tick == 40) {
            var core = node(level, SHELL);
            var hatch = SHELL.east(2);
            var port = SHELL.above(2);
            check.accept("six-chamber-fluid-mode", core.isFluidReactor() && core.coreWidth() == 9);
            var coolant =
                    new FluidStack(
                            NativeGameplayFixture.block("coolant")
                                    .defaultBlockState()
                                    .getFluidState()
                                    .getType(),
                            1000);
            check.accept(
                    "fluid-port-preview-then-fill",
                    NativeTransferAccess.fill(level, port, Direction.UP, coolant, true) == 1000
                            && core.coolantAmount(false) == 0
                            && NativeTransferAccess.fill(level, port, Direction.UP, coolant, false)
                                    == 1000
                            && core.coolantAmount(false) == 1000);
            check.accept(
                    "fluid-hatch-refuses-preheated-parts",
                    NativeTransferAccess.insert(
                                    level,
                                    hatch,
                                    Direction.EAST,
                                    worn("reactor_coolant_cell", 5000),
                                    false)
                            == 0);
            check.accept(
                    "access-hatch-loads-native-fuel",
                    NativeTransferAccess.insert(
                                    level, hatch, Direction.EAST, stack("uranium_fuel_rod"), false)
                            == 1);
            menu(level, SHELL, player).getSlot(1).set(stack("reactor_heat_vent"));
            level.setBlockAndUpdate(SHELL.west(3), Blocks.REDSTONE_BLOCK.defaultBlockState());
            var forSale = menu(level, STORE.above(4), player).getSlot(3).getItem();
            check.accept(
                    "energy-shop-gives-only-purchased-eu",
                    com.industrialage.ia2.gameplay.PortableGear.stored(forSale) == 1000
                            && node(level, STORE.above(4)).storedEu() == 0
                            && quantity(
                                            (Container) level.getBlockEntity(STORE.above(4).east()),
                                            Items.DIAMOND)
                                    == 1);
            var cashier = menu(level, STORE, player);
            check.accept(
                    "shop-refuses-delivery-on-full-output",
                    cashier.getSlot(2).getItem().getCount() == 3);
            cashier.getSlot(3).set(ItemStack.EMPTY);
            var i = menu(level, INJECTOR, player);
            check.accept(
                    "injector-exact-threshold-material-and-charge",
                    node(level, INJECTOR.west()).componentUse(0) == 17000
                            && node(level, INJECTOR.west()).componentUse(1) == 0
                            && node(level, INJECTOR).storedEu() == 1000
                            && i.getSlot(0).getItem().getCount() == 1);
            var moved =
                    level.getEntitiesOfClass(
                            ItemEntity.class,
                            new AABB(PAD.east(6)).inflate(2),
                            e -> e.getItem().is(Items.IRON_NUGGET));
            check.accept(
                    "teleporter-linked-pair-item-3482-eu",
                    moved.size() == 1
                            && ((com.industrialage.ia2.gameplay.IndustrialMachine)
                                                    level.getBlockEntity(PAD.west()))
                                            .energy()
                                    == 59
                            && node(level, PAD.south()).storedEu() == 159);
        }
        if (tick == 55) {
            var single =
                    new ItemEntity(
                            level,
                            PAD.getX() + .5,
                            PAD.getY() + 1.02,
                            PAD.getZ() + .5,
                            new ItemStack(Items.REDSTONE));
            single.setNoGravity(true);
            single.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(single);
        }
        if (tick == 85) {
            check.accept(
                    "teleporter-one-of-64-coins-costs-34",
                    !level.getEntitiesOfClass(
                                            ItemEntity.class,
                                            new AABB(PAD.east(6)).inflate(2),
                                            e -> e.getItem().is(Items.REDSTONE))
                                    .isEmpty()
                            && ((com.industrialage.ia2.gameplay.IndustrialMachine)
                                                    level.getBlockEntity(PAD.west()))
                                            .energy()
                                    == 42
                            && node(level, PAD.south()).storedEu() == 142);
        }
        if (tick == 160) {
            var core = node(level, SHELL);
            check.accept(
                    "fluid-reactor-reference-six-cycles",
                    core.componentUse(0) == 6
                            && core.hullHeat() == 0
                            && core.coolantAmount(false) == 952
                            && core.coolantAmount(true) == 48
                            && core.offer() == 0);
            var cashier = menu(level, STORE, player);
            var stock = (Container) level.getBlockEntity(STORE.east());
            check.accept(
                    "shop-two-complete-payments-out-of-stock-stops",
                    cashier.getSlot(2).getItem().getCount() == 1
                            && cashier.getSlot(3).getItem().getCount() == 4
                            && quantity(stock, Items.DIAMOND) == 2
                            && quantity(stock, Items.COAL) == 0);
            level.removeBlock(SHELL.offset(2, -2, 1), false);
        }
        if (tick == 195)
            check.accept(
                    "missing-shell-wall-disables-fluid-port",
                    !node(level, SHELL).isFluidReactor()
                            && NativeTransferAccess.fill(
                                            level,
                                            SHELL.above(2),
                                            Direction.UP,
                                            new FluidStack(
                                                    NativeGameplayFixture.block("coolant")
                                                            .defaultBlockState()
                                                            .getFluidState()
                                                            .getType(),
                                                    100),
                                            false)
                                    == 0);
    }
}
