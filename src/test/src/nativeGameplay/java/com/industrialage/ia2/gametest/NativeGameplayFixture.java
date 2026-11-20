package com.industrialage.ia2.gametest;

import com.industrialage.ia2.gameplay.IndustrialMachine;
import com.industrialage.ia2.worldtest.WorldChecks;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Exercises the actual registered factories, block tickers, inventories, and recipes. */
@Mod("ic2_native_gameplay")
public final class NativeGameplayFixture {
    private static final BlockPos GENERATOR = new BlockPos(19, 95, 19);
    private static final BlockPos SUPPLY = GENERATOR.east();
    private static final BlockPos STORAGE = SUPPLY.east();
    private static final BlockPos LINE = STORAGE.east();
    private static final BlockPos MILL = LINE.east();
    private static final BlockPos SIDE_STORAGE = STORAGE.south();
    private static final BlockPos FURNACE = MILL.east(2);
    private static final BlockPos RUBBER = FURNACE.east(2);

    private final String phase = System.getProperty("ic2.native.phase");
    private final Properties checks = new Properties();
    private final TransportChecks transport = new TransportChecks();
    private final EnergyTierChecks energyTiers = new EnergyTierChecks();
    private final MeterChecks meter = new MeterChecks();
    private GearChecks gear;
    private final PowerChecks power = new PowerChecks();
    private final WorkshopChecks workshop = new WorkshopChecks();
    private final StorageChecks storage = new StorageChecks();
    private WorldChecks worldProgram;
    private com.industrialage.ia2.worldtest.FoodPaintChecks worldDetails;
    private int ticks;
    private boolean finished;
    private int disconnectedAt;
    private double sourceAtDisconnection;
    private double storageAtDisconnection;
    private boolean finishedEnergyLine;
    private boolean verifiedBoats;

    /** Installs the two phases on the dedicated server. */
    public NativeGameplayFixture() {
        NeoForge.EVENT_BUS.addListener(
                net.neoforged.neoforge.event.level.BlockDropsEvent.class,
                StorageChecks::traceDrops);
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, this::startup);
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, this::sample);
    }

    private void startup(ServerStartedEvent event) {
        var server = event.getServer();
        var level = server.overworld();
        try {
            var jar =
                    ModList.get()
                            .getModContainerById("ic2")
                            .orElseThrow()
                            .getModInfo()
                            .getOwningFile()
                            .getFile()
                            .getFilePath();
            check(
                    "production-jar-loaded",
                    Files.isSameFile(jar, Path.of("mods", jar.getFileName().toString())));
            checks.setProperty("artifact", jar.getFileName().toString());
            if ("natural".equals(phase)) {
                naturalResources(level);
                check("world-saved", server.saveEverything(false, true, true));
                complete(server, null);
                return;
            }
            if (phase.equals("worldDetails") || phase.equals("worldDetailsCold")) {
                worldDetails =
                        new com.industrialage.ia2.worldtest.FoodPaintChecks(
                                NativeGameplayFixture::block,
                                NativeGameplayFixture::item,
                                this::check);
                if (phase.equals("worldDetailsCold")) {
                    worldDetails.cold(level);
                    complete(server, null);
                    return;
                }
                com.industrialage.ia2.worldtest.FoodPaintChecks.registry(
                        BuiltInRegistries.BLOCK.stream()
                                .map(block -> BuiltInRegistries.BLOCK.getKey(block).toString())
                                .filter(id -> id.startsWith("ic2:"))
                                .toList(),
                        BuiltInRegistries.ITEM.stream()
                                .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                                .filter(id -> id.startsWith("ic2:"))
                                .toList());
                worldDetails.setup(level, FakePlayerFactory.getMinecraft(level));
            } else if (phase.equals("worldProgram") || phase.equals("worldCold")) {
                worldProgram =
                        new WorldChecks(
                                NativeGameplayFixture::block,
                                NativeGameplayFixture::item,
                                com.industrialage.ia2.gameplay.PortableGear::charge,
                                this::check,
                                (com.industrialage.ia2.worldtest.WorldChecks.ProcessorMeter)
                                        (menu ->
                                                new WorldChecks.ProcessorValues(
                                                        ((com.industrialage.ia2.gameplay
                                                                                .MachineMenu)
                                                                        menu)
                                                                .progress(),
                                                        ((com.industrialage.ia2.gameplay
                                                                                .MachineMenu)
                                                                        menu)
                                                                .duration(),
                                                        ((com.industrialage.ia2.gameplay
                                                                                .MachineMenu)
                                                                        menu)
                                                                .energy(),
                                                        ((com.industrialage.ia2.gameplay
                                                                                .MachineMenu)
                                                                        menu)
                                                                .capacity())),
                                2,
                                1,
                                "machine.duration");
                if (phase.equals("worldCold")) {
                    worldProgram.cold(level, FakePlayerFactory.getMinecraft(level));
                    complete(server, null);
                    return;
                }
                worldProgram.setup(level, FakePlayerFactory.getMinecraft(level));
            } else if (phase.startsWith("power")) {
                power.start(level, phase.equals("powerRead"), this::check);
            } else if ("storage".equals(phase)) {
                level.setChunkForced(0, 0, true);
                level.getChunk(0, 0);
                storage.setup(level, this::check);
            } else if ("workshop".equals(phase)) {
                workshop.setup(level, this::check);
            } else if ("write".equals(phase)) {
                initialize(level);
            } else {
                coldState(level);
            }
            if ("worldProgram".equals(phase) || "worldDetails".equals(phase))
                server.tickRateManager().setTickRate(200F);
            else
                server.tickRateManager()
                        .requestGameToSprint("workshop".equals(phase) ? 8_500 : 1_300);
        } catch (IOException | RuntimeException | AssertionError failure) {
            complete(server, failure);
        }
    }

    private void initialize(ServerLevel level) {
        level.setChunkForced(GENERATOR.getX() >> 4, GENERATOR.getZ() >> 4, true);
        level.getChunk(GENERATOR.getX() >> 4, GENERATOR.getZ() >> 4);
        var player = FakePlayerFactory.getMinecraft(level);
        place(level, player, "generator", GENERATOR);
        place(level, player, "insulated_copper_cable", SUPPLY);
        place(level, player, "batbox", STORAGE);
        place(level, player, "tin_cable", LINE);
        place(level, player, "macerator", MILL);
        place(level, player, "batbox", SIDE_STORAGE);
        place(level, player, "iron_furnace", FURNACE);
        check(
                "survival-placement",
                level.getBlockState(STORAGE).getValue(BlockStateProperties.FACING)
                        == Direction.EAST);
        check(
                "native-generator-menu",
                machine(level, GENERATOR).createMenu(3, player.getInventory(), player).usesFuel());
        var millMenu = machine(level, MILL).createMenu(7, player.getInventory(), player);
        check(
                "native-processor-menu",
                millMenu.duration() == 300
                        && millMenu.capacity() == 1_200
                        && millMenu.machineSlots() == 7
                        && !millMenu.slots.get(1).mayPlace(new ItemStack(item("raw_tin"))));
        supply(level, GENERATOR.above(), new ItemStack(Items.COAL, 3));
        supply(level, MILL.above(), new ItemStack(item("raw_tin"), 2));
        supply(level, MILL.below(), ItemStack.EMPTY);
        var furnace = machine(level, FURNACE);
        furnace.setItem(0, new ItemStack(item("raw_tin")));
        furnace.setItem(1, new ItemStack(Items.COAL));
        var craftingInput =
                CraftingInput.of(
                        2,
                        1,
                        java.util.List.of(
                                new ItemStack(Items.IRON_INGOT),
                                new ItemStack(item("forge_hammer"))));
        var crafting =
                level.getServer()
                        .getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, craftingInput, level)
                        .orElseThrow();
        var remainder = crafting.value().getRemainingItems(craftingInput).get(1);
        check(
                "native-crafting-damages-tool",
                remainder.is(item("forge_hammer"))
                        && remainder.getCount() == 1
                        && remainder.getDamageValue() == 1);
        resin(level, player);
        transport.writeStart(level, this::check);
        energyTiers.setup(level, this::check);
        meter.setup(level, this::check);
        gear = new GearChecks();
        gear.setup(level, this::check);
        ArmorChecks.run(level, NativeArmorCheckAccess.player(level), this::check);
    }

    private void resin(ServerLevel level, FakePlayer player) {
        var log = block("rubber_log");
        var wet =
                log.getStateDefinition().getPossibleStates().stream()
                        .filter(state -> state.toString().contains("axis=y,resin_hole=wet_north"))
                        .findFirst()
                        .orElseThrow();
        level.setBlockAndUpdate(RUBBER, wet);
        var tap = new ItemStack(item("treetap"));
        player.setItemInHand(InteractionHand.MAIN_HAND, tap);
        player.setPos(RUBBER.getX() + 0.5, RUBBER.getY(), RUBBER.getZ() - 1.5);
        var hit =
                new BlockHitResult(
                        Vec3.atCenterOf(RUBBER).add(0, 0, -0.5), Direction.NORTH, RUBBER, false);
        check(
                "resin-harvest",
                player.gameMode
                                .useItemOn(player, level, tap, InteractionHand.MAIN_HAND, hit)
                                .consumesAction()
                        && tap.getDamageValue() == 1
                        && level.getBlockState(RUBBER).toString().contains("resin_hole=dry_north")
                        && player.getInventory().contains(new ItemStack(item("resin"))));
    }

    private void naturalResources(ServerLevel level) {
        var material =
                new WorldChecks(
                        NativeGameplayFixture::block,
                        NativeGameplayFixture::item,
                        com.industrialage.ia2.gameplay.PortableGear::charge,
                        this::check);
        material.natural(level);
        var ore = block("tin_ore");
        var deepOre = block("deepslate_tin_ore");
        boolean foundTin = false;
        boolean foundDeepslateTin = false;
        for (int x = 0; x < 3 && !(foundTin && foundDeepslateTin); x++) {
            for (int z = 0; z < 3; z++) {
                for (var section : level.getChunk(x, z).getSections()) {
                    foundTin |= section.maybeHas(state -> state.is(ore));
                    foundDeepslateTin |= section.maybeHas(state -> state.is(deepOre));
                }
            }
        }
        check("generated-tin-ore", foundTin && foundDeepslateTin);
        var pos = new BlockPos(29, 220, 30);
        level.setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
        var sapling = (SaplingBlock) block("rubber_sapling");
        level.setBlockAndUpdate(pos, sapling.defaultBlockState());
        sapling.performBonemeal(level, level.getRandom(), pos, level.getBlockState(pos));
        sapling.performBonemeal(level, level.getRandom(), pos, level.getBlockState(pos));
        check(
                "configured-rubber-sapling-growth",
                level.getBlockState(pos).is(block("rubber_log"))
                        && level.getBlockState(pos.above()).is(block("rubber_log")));
    }

    private void coldState(ServerLevel level) throws IOException {
        var written = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("write.properties"))) {
            written.load(reader);
        }
        level.getChunk(GENERATOR.getX() >> 4, GENERATOR.getZ() >> 4);
        var generator = machine(level, GENERATOR);
        var mill = machine(level, MILL);
        check(
                "cold-fuel",
                generator.fuelRemaining() == intValue(written, "fuel")
                        && generator.getItem(0).getCount() == intValue(written, "coal"));
        check(
                "cold-machine",
                mill.progress() == intValue(written, "progress")
                        && mill.energy() == doubleValue(written, "millEu")
                        && mill.getItem(0).getCount() == 2);
        check(
                "cold-storage",
                machine(level, STORAGE).energy() == doubleValue(written, "storageEu")
                        && level.getBlockState(STORAGE).getValue(BlockStateProperties.FACING)
                                == Direction.EAST);
        transport.readStart(level, this::check);
    }

    private void sample(ServerTickEvent.Post event) {
        if (finished) {
            return;
        }
        var server = event.getServer();
        try {
            if (++ticks > ("workshop".equals(phase) ? 8_400 : 1_250)) {
                var details =
                        "read".equals(phase) ? "; " + transport.diagnostic(server.overworld()) : "";
                throw new AssertionError("Native production line timed out" + details);
            }
            boolean done =
                    switch (phase) {
                        case "storage" -> {
                            if (ticks < 30) yield false;
                            yield storage.tick(server.overworld(), this::check);
                        }
                        case "powerWrite", "powerRead" ->
                                power.sample(
                                        server.overworld(),
                                        ticks,
                                        phase.equals("powerRead"),
                                        this::check);
                        case "worldProgram" -> {
                            var player = FakePlayerFactory.getMinecraft(server.overworld());
                            boolean ready =
                                    worldProgram.automationTick(server.overworld(), player, ticks);
                            if (ticks < 320 || !ready) yield false;
                            worldProgram.checkOutcomes(server.overworld(), player);
                            yield true;
                        }
                        case "worldDetails" ->
                                worldDetails.tick(
                                        server.overworld(),
                                        FakePlayerFactory.getMinecraft(server.overworld()),
                                        ticks);
                        case "write" -> writeStage(server.overworld());
                        case "workshop" -> workshop.tick(server.overworld(), this::check);
                        default -> readStage(server.overworld());
                    };
            if (done) {
                check("world-saved", server.saveEverything(false, true, true));
                complete(server, null);
            }
        } catch (RuntimeException | AssertionError failure) {
            complete(server, failure);
        }
    }

    private boolean writeStage(ServerLevel level) {
        var mill = machine(level, MILL);
        var generator = machine(level, GENERATOR);
        boolean tiersDone = generator.fuelRemaining() > 0 && energyTiers.tick(level, this::check);
        boolean gearDone = generator.fuelRemaining() > 0 && gear.tick(level, this::check);
        boolean meterDone = meter.tick(this::check);
        if (mill.progress() < 50
                || mill.getItem(0).getCount() < 2
                || !tiersDone
                || !gearDone
                || !meterDone) {
            return false;
        }
        var storage = machine(level, STORAGE);
        check(
                "hopper-input",
                mill.getItem(0).is(item("raw_tin"))
                        && generator.fuelRemaining() > 0
                        && generator.getItem(0).getCount() == 2);
        check(
                "directed-energy-line",
                mill.energy() > 0
                        && storage.energy() > 0
                        && machine(level, SIDE_STORAGE).energy() == 0);
        checks.setProperty("fuel", Integer.toString(generator.fuelRemaining()));
        checks.setProperty("coal", Integer.toString(generator.getItem(0).getCount()));
        checks.setProperty("progress", Integer.toString(mill.progress()));
        checks.setProperty("millEu", Double.toString(mill.energy()));
        checks.setProperty("storageEu", Double.toString(storage.energy()));
        return true;
    }

    private boolean readStage(ServerLevel level) {
        if (!verifiedBoats && ticks >= 20) {
            GearChecks.verifyColdBoats(level, this::check);
            verifiedBoats = true;
        }
        boolean transportDone = transport.tick(level, this::check);
        if (finishedEnergyLine) return transportDone;
        var generator = machine(level, GENERATOR);
        var storage = machine(level, STORAGE);
        var mill = machine(level, MILL);
        if (disconnectedAt == 0) {
            int crushed = count(hopper(level, MILL.below()), item("crushed_tin"));
            if (crushed < 4) {
                return false;
            }
            check(
                    "hopper-completed-two-batches",
                    crushed == 4
                            && mill.getItem(0).isEmpty()
                            && mill.getItem(1).isEmpty()
                            && mill.progress() == 0);
            check("iron-furnace-recipe", machine(level, FURNACE).getItem(2).is(item("tin_ingot")));
            check("non-output-face-unpowered", machine(level, SIDE_STORAGE).energy() == 0);
            check("remove-cable", level.destroyBlock(SUPPLY, false));
            disconnectedAt = ticks;
            sourceAtDisconnection = generator.energy();
            storageAtDisconnection = storage.energy();
            return false;
        }
        if (ticks - disconnectedAt < 20) {
            return false;
        }
        check(
                "cut-line-no-energy-transfer",
                generator.energy() == sourceAtDisconnection + 200
                        && storage.energy() <= storageAtDisconnection);
        check(
                "generator-break",
                level.destroyBlock(GENERATOR, false)
                        && level.getBlockEntity(GENERATOR) == null
                        && level
                                .getEntitiesOfClass(
                                        ItemEntity.class, new AABB(GENERATOR).inflate(2))
                                .stream()
                                .anyMatch(entity -> entity.getItem().is(Items.COAL)));
        finishedEnergyLine = true;
        return transportDone;
    }

    private void complete(MinecraftServer server, Throwable failure) {
        finished = true;
        checks.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
        checks.setProperty("ticks", Integer.toString(ticks));
        checks.setProperty("status", failure == null ? "passed" : "failed");
        if (failure != null) {
            checks.setProperty("problem", failure.toString());
            var trace = new java.io.StringWriter();
            failure.printStackTrace(new java.io.PrintWriter(trace));
            checks.setProperty("trace", trace.toString());
        }
        try (var writer = Files.newBufferedWriter(Path.of(phase + ".properties"))) {
            checks.store(writer, "IndustrialCraft 2 native gameplay " + phase);
        } catch (IOException error) {
            throw new IllegalStateException("Could not write native gameplay evidence", error);
        } finally {
            server.halt(false);
        }
    }

    static void place(ServerLevel level, FakePlayer player, String path, BlockPos pos) {
        place(level, player, path, pos, new ItemStack(block(path)));
    }

    static void place(
            ServerLevel level, FakePlayer player, String path, BlockPos pos, ItemStack stack) {
        var block = block(path);
        var floor = pos.below();
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        if (!level.getBlockState(pos).isAir()) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        player.setPos(pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5);
        player.setYRot(90);
        player.setYHeadRot(90);
        player.setXRot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, floor, false);
        if (!player.gameMode
                        .useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit)
                        .consumesAction()
                || !stack.isEmpty()
                || !level.getBlockState(pos).is(block)) {
            throw new AssertionError("Could not use the survival placement path for ic2:" + path);
        }
    }

    private static void supply(ServerLevel level, BlockPos pos, ItemStack stack) {
        level.setBlockAndUpdate(pos, Blocks.HOPPER.defaultBlockState());
        hopper(level, pos).setItem(0, stack);
    }

    private static HopperBlockEntity hopper(ServerLevel level, BlockPos pos) {
        return (HopperBlockEntity) level.getBlockEntity(pos);
    }

    private static IndustrialMachine machine(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof IndustrialMachine machine) {
            return machine;
        }
        throw new AssertionError("Missing production machine at " + pos);
    }

    private static int count(Container container, Item item) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    static Block block(String path) {
        return BuiltInRegistries.BLOCK.stream()
                .filter(
                        block ->
                                BuiltInRegistries.BLOCK
                                        .getKey(block)
                                        .toString()
                                        .equals("ic2:" + path))
                .findFirst()
                .orElseThrow();
    }

    static Item item(String path) {
        return BuiltInRegistries.ITEM.stream()
                .filter(
                        item ->
                                BuiltInRegistries.ITEM
                                        .getKey(item)
                                        .toString()
                                        .equals("ic2:" + path))
                .findFirst()
                .orElseThrow();
    }

    private void check(String key, boolean outcome) {
        if (!outcome) {
            throw new AssertionError(key);
        }
        checks.setProperty(key, "passed");
    }

    private static int intValue(Properties values, String key) {
        return Integer.parseInt(values.getProperty(key));
    }

    private static double doubleValue(Properties values, String key) {
        return Double.parseDouble(values.getProperty(key));
    }
}
