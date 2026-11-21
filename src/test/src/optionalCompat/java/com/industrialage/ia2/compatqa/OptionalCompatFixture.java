package com.industrialage.ia2.compatqa;

import com.industrialage.ia2.compat.CuriosBack;
import com.industrialage.ia2.gameplay.PortableGear;
import com.industrialage.ia2.industrial.PowerNode;
import com.industrialage.ia2.industrial.WorkshopNode;
import com.industrialage.ia2.power.WorldPowerStorage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Exercises installed optional mods and the production IC2 JAR on a dedicated server. */
@Mod("ic2_compat_qa")
public final class OptionalCompatFixture {
    private static final BlockPos PAD = new BlockPos(8, 70, 8);
    private static final BlockPos BATBOX = PAD.east(2);
    private static final BlockPos TANK = BATBOX.east(2);
    private static final BlockPos WASHER = TANK.east(2);
    private static final BlockPos GEO = PAD.south(2);
    private static final BlockPos CABLE = BATBOX.south(2);

    private int startupTicks;

    /** Registers the dedicated test entry point. */
    public OptionalCompatFixture() {
        if (System.getProperty("ic2.compat.mode") == null) return;
        NeoForge.EVENT_BUS.addListener(
                ServerStartedEvent.class,
                event -> event.getServer().overworld().setChunkForced(0, 0, true));
        NeoForge.EVENT_BUS.addListener(
                ServerTickEvent.Post.class,
                event -> {
                    if (++startupTicks == 30) run(event.getServer());
                });
    }

    private void run(MinecraftServer server) {
        var level = server.overworld();
        var report = new ArrayList<String>();
        try {
            var mode = System.getProperty("ic2.compat.mode");
            boolean top = mode.equals("both") || mode.equals("top");
            boolean curios = mode.equals("both") || mode.equals("curios");
            verifyMod("ic2", true, report);
            verifyMod("theoneprobe", top, report);
            verifyMod("curios", curios, report);
            level.getChunkAt(PAD);
            var player = CompatHost.player(level);
            place(level, PAD, "batbox_chargepad");
            player.setPos(PAD.getX() + 0.5, PAD.getY() + 1, PAD.getZ() + 0.5);
            level.addNewPlayer(player);
            player.setOnGround(true);
            var pack = new ItemStack(CompatHost.item("batpack"));
            if (curios) {
                CuriosChecks.equip(player, pack);
                report.add("Curios validates all five IC2 packs and rejects diamond");
                require(CuriosBack.slots(player).get(0) == pack, "live Curios item reference");
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            } else {
                require(CuriosBack.slots(player).isEmpty(), "no Curios stacks");
                player.setItemSlot(EquipmentSlot.CHEST, pack);
            }
            var pad = (WorldPowerStorage) level.getBlockEntity(PAD);
            require(pad.exchangeStoredEnergy(701, false), "seed pad");
            CompatHost.tick(level, PAD);
            require(
                    PortableGear.stored(pack) == 100 && pad.availableStoredEnergy() == 601,
                    "native pad transfers exactly 100 EU into one equipped stack; pack="
                            + PortableGear.stored(pack)
                            + ", pad="
                            + pad.availableStoredEnergy()
                            + ", nearby="
                            + level.getEntitiesOfClass(
                                            net.minecraft.server.level.ServerPlayer.class,
                                            new net.minecraft.world.phys.AABB(PAD.above()))
                                    .size());
            report.add(
                    curios
                            ? "Curios pack charge 0 -> 100 EU; pad 701 -> 601 EU"
                            : "Chest pack charge 0 -> 100 EU; pad 701 -> 601 EU");

            var boat = CompatHost.boat(level);
            boat.setPos(8.5, 70, 12.5);
            level.addFreshEntity(boat);
            if (top) TopReply.entity(level, player, boat, "top.ic2.no_rider_battery");
            require(player.startRiding(boat), "mount electric boat");
            require(boat.getControllingPassenger() == player, "native controlling rider");
            require(PortableGear.boatPack(player) == pack, "boat uses live equipped battery");
            if (top) {
                TopReply.entity(
                        level,
                        player,
                        boat,
                        "top.ic2.rider_battery",
                        pack.getHoverName(),
                        "100",
                        "60000");
                report.add("TOP boat rider battery 100 / 60000 EU");
                place(level, CABLE, "copper_cable");
                TopReply.block(level, player, CABLE, "top.ic2.conductor");
                report.add("TOP conductor reports 0 EU local buffer");
                place(level, BATBOX, "batbox");
                var box = (WorldPowerStorage) level.getBlockEntity(BATBOX);
                require(box.exchangeStoredEnergy(723, false), "seed box");
                TopReply.block(level, player, BATBOX, "top.ic2.energy", "723", "40000");
                require(box.exchangeStoredEnergy(-128, false), "discharge box");
                TopReply.block(level, player, BATBOX, "top.ic2.energy", "595", "40000");
                report.add("TOP BatBox 723 -> 595 / 40000 EU");
                place(level, TANK, "tank");
                TopReply.block(
                        level,
                        player,
                        TANK,
                        "top.ic2.fluid",
                        net.minecraft.network.chat.Component.translatable("top.ic2.empty"),
                        0,
                        24000);
                require(CompatHost.water(level, TANK, 1234) == 1234, "fill water tank");
                TopReply.block(level, player, TANK, "top.ic2.fluid", "Water", 1234, 24000);
                report.add("TOP tank empty -> Water 1234 / 24000 mB");
                place(level, WASHER, "ore_washing_plant");
                var washer = (WorkshopNode) level.getBlockEntity(WASHER);
                require(washer.receive(567.25) == 567.25, "seed washer");
                require(CompatHost.water(level, WASHER, 321) == 321, "fill washer");
                TopReply.block(level, player, WASHER, "top.ic2.energy", "567.25", "16000");
                TopReply.block(level, player, WASHER, "top.ic2.fluid", "Water", 321, 8000);
                report.add("TOP washer 567.25 / 16000 EU and Water 321 / 8000 mB");
                washer.receive(0.0045);
                TopReply.block(level, player, WASHER, "top.ic2.energy", "567.255", "16000");
                report.add("TOP fractional EU rounded to three decimal places");
                place(level, GEO, "geo_generator");
                var geo = (PowerNode) level.getBlockEntity(GEO);
                require(geo.fillLava(765, false) == 765, "seed geothermal lava");
                TopReply.block(level, player, GEO, "top.ic2.fluid", "Lava", 765, 8000);
                TopReply.block(level, player, GEO, "top.ic2.energy", "0", "2400");
                report.add("TOP geothermal Lava 765 / 8000 mB and 0 / 2400 EU");
            }
            if (curios) {
                var removed = CuriosChecks.unequip(player);
                require(
                        CuriosBack.slots(player).isEmpty() && PortableGear.stored(removed) == 100,
                        "the original stack retains EU after unequipping");
                report.add("Curios retained 100 EU after unequipping");
            }
            player.stopRiding();
            chargeQuantumSuit(level, player, report);
            player.discard();
            Files.writeString(
                    Path.of("compat-checks.txt"), "passed\n" + String.join("\n", report) + "\n");
        } catch (Throwable problem) {
            try {
                var details = new java.io.StringWriter();
                problem.printStackTrace(new java.io.PrintWriter(details));
                Files.writeString(
                        Path.of("compat-checks.txt"),
                        "failed\n" + String.join("\n", report) + "\n" + details);
            } catch (java.io.IOException io) {
                problem.addSuppressed(io);
                throw new IllegalStateException(problem);
            }
        } finally {
            server.halt(false);
        }
    }

    private static void chargeQuantumSuit(
            ServerLevel level, ServerPlayer player, List<String> report) {
        var slots =
                List.of(
                        EquipmentSlot.HEAD,
                        EquipmentSlot.CHEST,
                        EquipmentSlot.LEGS,
                        EquipmentSlot.FEET);
        var pieces = List.of("helmet", "chestplate", "leggings", "boots");
        for (int i = 0; i < slots.size(); i++) {
            player.setItemSlot(
                    slots.get(i), new ItemStack(CompatHost.item("quantum_" + pieces.get(i))));
        }
        player.setPos(PAD.getX() + 0.5, PAD.getY() + 1, PAD.getZ() + 0.5);
        player.setOnGround(true);
        for (var charger : List.of("mfe_chargepad", "mfsu_chargepad")) {
            place(level, PAD, charger);
            var pad = (WorldPowerStorage) level.getBlockEntity(PAD);
            require(pad.exchangeStoredEnergy(48_007, false), "seed " + charger);
            CompatHost.tick(level, PAD);
            int accepted = charger.equals("mfsu_chargepad") ? 12_000 : 0;
            for (var slot : slots) {
                require(
                        PortableGear.stored(player.getItemBySlot(slot)) == accepted,
                        charger + " charge in " + slot);
            }
            require(
                    pad.availableStoredEnergy() == 48_007 - 4 * accepted,
                    charger + " energy conservation");
        }
        report.add(
                "Quantum suit: MFE transfers 0 EU; MFSU transfers 12000 EU into each of four worn pieces, 7 EU left");
    }

    private static void place(ServerLevel level, BlockPos pos, String path) {
        require(
                level.setBlockAndUpdate(pos, CompatHost.block(path).defaultBlockState()),
                "place " + path);
    }

    private static void verifyMod(String mod, boolean expected, List<String> report)
            throws Exception {
        var loaded = ModList.get().getModContainerById(mod);
        require(loaded.isPresent() == expected, "correct optional mod presence for " + mod);
        if (loaded.isEmpty()) return;
        var file = loaded.get().getModInfo().getOwningFile().getFile().getFilePath();
        require(
                Files.isSameFile(file, Path.of("mods", file.getFileName().toString())),
                "installed " + mod + " artifact");
        report.add(mod + " " + loaded.get().getModInfo().getVersion() + " / " + file.getFileName());
    }

    static void require(boolean condition, String detail) {
        if (!condition) throw new AssertionError(detail);
    }
}
