package com.industrialage.ia2.world;

import com.industrialage.ia2.energy.graph.EnergyGraph.Ports;
import com.industrialage.ia2.platform.CapabilityFacade.Face;
import com.industrialage.ia2.power.WorldEnergy;
import com.industrialage.ia2.power.WorldPowerPort;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

final class WorldMachine extends WorldSaveBase implements WorldPowerPort, MenuProvider {
    private static final Ports INPUT = new Ports(Set.of(Face.values()), Set.of(), false);
    final WorldMachineBlock.Kind kind;
    final WorldInventory inventory;
    private long lastTick = Long.MIN_VALUE;
    private long lastWork = Long.MIN_VALUE;
    private boolean attached;
    private double energy;
    private int water;
    private int weedEx;
    int cursor;
    int pipeDepth;
    int scanDepth = 1;
    boolean blacklist;
    boolean withdrawing;
    int ticketX = 1;
    int ticketZ;
    boolean chunkEnabled = true;
    private net.minecraft.world.level.ChunkPos hostTicket;
    private net.minecraft.world.level.ChunkPos targetTicket;

    WorldMachine(BlockPos pos, BlockState state) {
        super(WorldPlatform.machineType(), pos, state);
        kind = ((WorldMachineBlock) state.getBlock()).kind;
        inventory = new WorldInventory(this, kind.size());
    }

    void interact(Player player, Direction face) {
        if (player.isShiftKeyDown()) {
            if (kind == WorldMachineBlock.Kind.CHUNK_LOADER) {
                configureChunks(player, face);
                return;
            }
            if (kind == WorldMachineBlock.Kind.ADVANCED_MINER) {
                blacklist = !blacklist;
                cursor = 0;
                scanDepth = 1;
            } else if (kind == WorldMachineBlock.Kind.MINER) {
                withdrawing = !withdrawing;
            }
            setChanged();
            String state =
                    switch (kind) {
                        case MINER -> withdrawing ? "ic2.world.withdraw" : "ic2.world.mine";
                        case ADVANCED_MINER ->
                                blacklist ? "ic2.world.blacklist" : "ic2.world.whitelist";
                        default -> "ic2.world.no_mode";
                    };
            WorldNative.reply(
                    player,
                    Component.translatable(
                            "ic2.world.status",
                            Component.translatable("block.ic2." + kind.path),
                            (int) energy,
                            Component.translatable(state)));
        } else {
            player.openMenu(this);
        }
    }

    @Override
    public Component getDisplayName() {
        String name =
                switch (kind) {
                    case CROPMATRON, CLASSIC_CROPMATRON -> "ic2.world.menu.care";
                    case TERRAFORMER -> "ic2.world.menu.blueprint";
                    case MINER -> "ic2.world.menu.miner";
                    case ADVANCED_MINER -> "ic2.world.menu.advanced_miner";
                    case BATCH_CRAFTER -> "ic2.world.menu.batch";
                    case ITEM_BUFFER -> "ic2.world.menu.buffer";
                    case SORTING_MACHINE -> "ic2.world.menu.sort";
                    case WEIGHTED_DISTRIBUTOR -> "ic2.world.menu.distribute";
                    default -> "block.ic2." + kind.path;
                };
        return Component.translatable(name);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory slots, Player player) {
        return new WorldMenu(id, slots, this);
    }

    void tick(ServerLevel server) {
        long time = server.getGameTime();
        if (lastTick == time) return;
        lastTick = time;
        if (!attached && kind.packet > 0) {
            attached = true;
            WorldEnergy.connect(this);
        }
        var battery = inventory.getItem(kind.batterySlot());
        if (kind.packet > 0
                && !battery.isEmpty()
                && receive(WorldNative.discharge(battery, capacity() - energy, itemTier())) > 0) {
            inventory.setChanged();
        }
        if (kind == WorldMachineBlock.Kind.CROPMATRON) {
            refillFromBuckets();
            if (time % 4 == 0) drawMatronFluids(server);
        }
        boolean running =
                switch (kind) {
                    case CROP_HARVESTER, CROPMATRON, CLASSIC_CROPMATRON -> cropWork(server, time);
                    case TERRAFORMER -> Terraforming.tick(this, server, time);
                    case MINER, ADVANCED_MINER -> Mining.tick(this, server, time);
                    default -> WorldAutomation.tick(this, server, time);
                };
        if (running) lastWork = time;
        boolean active = lastWork != Long.MIN_VALUE && time - lastWork < 20;
        var state = getBlockState();
        if (state.getValue(WorldMachineBlock.ACTIVE) != active)
            server.setBlock(
                    worldPosition,
                    state.setValue(WorldMachineBlock.ACTIVE, active),
                    Block.UPDATE_CLIENTS);
    }

    private void configureChunks(Player player, Direction face) {
        if (face == Direction.DOWN) {
            chunkEnabled = !chunkEnabled;
        } else if (face == Direction.UP) {
            ticketX = 0;
            ticketZ = 0;
        } else {
            ticketX = Math.max(-4, Math.min(4, ticketX + face.getStepX()));
            ticketZ = Math.max(-4, Math.min(4, ticketZ + face.getStepZ()));
        }
        WorldNative.reply(
                player,
                Component.translatable(
                        chunkEnabled ? "ic2.world.chunk_selected" : "ic2.world.chunk_off",
                        (worldPosition.getX() >> 4) + ticketX,
                        (worldPosition.getZ() >> 4) + ticketZ));
        setChanged();
    }

    boolean loadChunks(ServerLevel level) {
        if (!chunkEnabled || level.hasNeighborSignal(worldPosition) || !pay(8)) {
            releaseChunks(level);
            return false;
        }
        int x = worldPosition.getX() >> 4;
        int z = worldPosition.getZ() >> 4;
        if (hostTicket == null) {
            hostTicket = new net.minecraft.world.level.ChunkPos(x, z);
            WorldChunkTickets.hold(level, hostTicket);
        }
        var requested = new net.minecraft.world.level.ChunkPos(x + ticketX, z + ticketZ);
        if (!requested.equals(targetTicket)) {
            if (targetTicket != null && !targetTicket.equals(hostTicket))
                WorldChunkTickets.release(level, targetTicket);
            targetTicket = requested;
            if (!targetTicket.equals(hostTicket)) WorldChunkTickets.hold(level, targetTicket);
        }
        return true;
    }

    private void releaseChunks(ServerLevel level) {
        if (targetTicket != null && !targetTicket.equals(hostTicket))
            WorldChunkTickets.release(level, targetTicket);
        if (hostTicket != null) WorldChunkTickets.release(level, hostTicket);
        targetTicket = null;
        hostTicket = null;
    }

    private boolean cropWork(ServerLevel level, long time) {
        if (energy < 1 || Math.floorMod(time, 4) != 0) return false;
        boolean changed = false;
        for (int step = 0; step < 12; step++) {
            int next = cursor++ % 243;
            BlockPos pos = worldPosition.offset(next % 9 - 4, next / 81 - 1, next / 9 % 9 - 4);
            if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof CropPlot crop))
                continue;
            boolean worked =
                    kind == WorldMachineBlock.Kind.CROP_HARVESTER
                            ? harvest(level, crop)
                            : cultivate(level, crop);
            changed |= worked;
            if (energy < 1) break;
        }
        cursor %= 243;
        return changed;
    }

    private boolean harvest(ServerLevel level, CropPlot crop) {
        if (!crop.ripe() || crop.species() == CropSpecies.WEED || energy < 31) return false;
        var stack = crop.harvestOutput();
        if (stack.isEmpty() || !inventory.store(List.of(stack))) return false;
        crop.harvest(level);
        pay(31);
        return true;
    }

    private boolean cultivate(ServerLevel level, CropPlot crop) {
        if (crop.species() == null) return false;
        boolean work = false;
        if (crop.nutrients < 20 && inventory.has(0, "ic2:fertilizer") && pay(10)) {
            inventory.removeItem(0, 1);
            crop.nutrients = Math.min(200, crop.nutrients + 100);
            work = true;
        }
        if (crop.hydration < 50 && energy >= 10 && dispense(1, false)) {
            pay(10);
            crop.hydration = Math.min(200, crop.hydration + 200);
            work = true;
        }
        if (crop.protection < 20 && energy >= 10 && dispense(2, true)) {
            pay(10);
            if (!crop.clearWeed(level)) crop.protection = 100;
            work = true;
        }
        if (work) crop.setChanged();
        return work;
    }

    private boolean dispense(int inputSlot, boolean weed) {
        if (kind == WorldMachineBlock.Kind.CROPMATRON) {
            int available = weed ? weedEx : water;
            if (available < 200) return false;
            if (weed) weedEx -= 200;
            else water -= 200;
            setChanged();
            return true;
        }
        String supply = weed ? "ic2:weed_ex_cell" : "ic2:hydration_cell";
        return replaceContainer(
                inputSlot, supply, new ItemStack(WorldNative.item("ic2:empty_cell")));
    }

    private void drawMatronFluids(ServerLevel level) {
        for (int slot = 9; slot < 13; slot++) {
            var upgrade = inventory.getItem(slot);
            if (!inventory.has(slot, "ic2:fluid_pulling_upgrade")) continue;
            int limit =
                    100
                            * com.industrialage.ia2.content.ProcessingUpgrades.transferLimit(
                                    upgrade.getCount());
            for (Direction face : MachineAutomation.faces(upgrade)) {
                var source = worldPosition.relative(face);
                if (worldPosition.equals(source)) continue;
                fill(
                        false,
                        WorldFluidAccess.drain(
                                level,
                                source,
                                face.getOpposite(),
                                "minecraft:water",
                                Math.min(limit, 4000 - water)),
                        true);
                fill(
                        true,
                        WorldFluidAccess.drain(
                                level,
                                source,
                                face.getOpposite(),
                                "ic2:weed_ex",
                                Math.min(limit, 4000 - weedEx)),
                        true);
            }
        }
    }

    private void refillFromBuckets() {
        if (water <= 3000
                && replaceContainer(1, "minecraft:water_bucket", new ItemStack(Items.BUCKET))) {
            water += 1000;
            setChanged();
        }
        if (weedEx <= 3000
                && replaceContainer(2, "ic2:weed_ex_bucket", new ItemStack(Items.BUCKET))) {
            weedEx += 1000;
            setChanged();
        }
    }

    private boolean replaceContainer(int slot, String used, ItemStack result) {
        if (!inventory.has(slot, used) || !inventory.store(List.of(result))) return false;
        inventory.removeItem(slot, 1);
        return true;
    }

    int fluid(boolean weed) {
        return weed ? weedEx : water;
    }

    void setFluid(boolean weed, int amount) {
        if (weed) weedEx = amount;
        else water = amount;
    }

    int fill(boolean weed, int offered, boolean commit) {
        if (kind != WorldMachineBlock.Kind.CROPMATRON || offered <= 0) return 0;
        int accepted = Math.min(offered, 4000 - fluid(weed));
        if (commit && accepted > 0) {
            if (weed) weedEx += accepted;
            else water += accepted;
            setChanged();
        }
        return accepted;
    }

    double storedEnergy() {
        return energy;
    }

    boolean canPay(double cost) {
        return energy >= cost;
    }

    boolean pay(double cost) {
        if (!canPay(cost)) return false;
        energy -= cost;
        setChanged();
        return true;
    }

    private double capacity() {
        return kind.packet <= 32 ? 10_000 : 60_000;
    }

    private int itemTier() {
        return kind.packet <= 32 ? 1 : kind.packet <= 128 ? 2 : 3;
    }

    @Override
    public Ports energyPorts() {
        return INPUT;
    }

    @Override
    public double packetLimit() {
        return kind.packet;
    }

    @Override
    public double receive(double offered) {
        if (kind.packet == 0) return 0;
        if (!Double.isFinite(offered) || offered <= 0) return 0;
        double accepted = Math.min(offered, capacity() - energy);
        if (accepted > 0) {
            energy += accepted;
            setChanged();
        }
        return accepted;
    }

    @Override
    public boolean ranThisTick(ServerLevel level) {
        return !isRemoved() && lastTick == level.getGameTime();
    }

    @Override
    public void setRemoved() {
        unlink();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        unlink();
        super.onChunkUnloaded();
    }

    private void unlink() {
        if (getLevel() instanceof ServerLevel level) releaseChunks(level);
        if (attached) {
            WorldEnergy.disconnect(this);
            attached = false;
        }
        lastTick = Long.MIN_VALUE;
    }

    @Override
    WorldInventory savedInventory() {
        return inventory;
    }

    @Override
    void writeFields(BiConsumer<String, Double> state) {
        number(state, "eu", energy);
        number(state, "water", water);
        number(state, "weed_ex", weedEx);
        number(state, "target", cursor);
        number(state, "pipe_depth", pipeDepth);
        number(state, "scan_depth", scanDepth);
        number(state, "blacklist", blacklist ? 1 : 0);
        number(state, "withdrawing", withdrawing ? 1 : 0);
        if (kind == WorldMachineBlock.Kind.CHUNK_LOADER) {
            number(state, "chunk_x", ticketX + 5);
            number(state, "chunk_z", ticketZ + 5);
            number(state, "chunk_disabled", chunkEnabled ? 0 : 1);
        }
    }

    @Override
    void readFields(ToDoubleFunction<String> state) {
        double saved = state.applyAsDouble("eu");
        energy = Double.isFinite(saved) ? Math.max(0, Math.min(capacity(), saved)) : 0;
        water = Math.min(4000, integer(state, "water"));
        weedEx = Math.min(4000, integer(state, "weed_ex"));
        cursor = Math.min(100000, integer(state, "target"));
        pipeDepth = Math.min(2048, integer(state, "pipe_depth"));
        scanDepth = Math.max(1, Math.min(2048, integer(state, "scan_depth")));
        blacklist = integer(state, "blacklist") != 0;
        withdrawing = integer(state, "withdrawing") != 0;
        if (state.applyAsDouble("chunk_x") > 0) {
            ticketX = Math.max(-4, Math.min(4, integer(state, "chunk_x") - 5));
            ticketZ = Math.max(-4, Math.min(4, integer(state, "chunk_z") - 5));
        }
        chunkEnabled = integer(state, "chunk_disabled") == 0;
    }
}
