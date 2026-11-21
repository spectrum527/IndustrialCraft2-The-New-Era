package com.industrialage.ia2.industrial;

import com.industrialage.ia2.content.ProcessingUpgrades;
import com.industrialage.ia2.energy.graph.EnergyGraph.Ports;
import com.industrialage.ia2.energy.graph.PacketBudget;
import com.industrialage.ia2.logistics.FluidContents;
import com.industrialage.ia2.platform.CapabilityFacade.Face;
import com.industrialage.ia2.power.EuBuffer;
import com.industrialage.ia2.power.WorldEnergy;
import com.industrialage.ia2.power.WorldPowerPort;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A placed industrial processor. Native items and fluids commit together when one job completes.
 */
public final class WorkshopNode extends WorkshopSaveBase
        implements WorldPowerPort, EuBuffer, MenuProvider {
    private static final int BLAST_FURNACE_WORK_HEAT = 50_000;
    private static final int BLAST_FURNACE_HEAT_CAPACITY = 100_000;
    private static final Set<Face> SIDES = Set.copyOf(EnumSet.allOf(Face.class));
    final WorkshopKind kind;
    final WorkshopInventory inventory = new WorkshopInventory(this);
    final Tank inlet;
    final Tank outlet;
    final Tank byproduct;
    double energy;
    int heat;
    int heatFuelTicks;
    boolean solidProducesAsh;
    int progress;
    int mode;
    int cycles;
    int amplifier;
    int fractionalMatter;
    int cycleDuration;
    ItemStack preview = ItemStack.EMPTY;
    int auxiliaryUnits;
    long handledAt = Long.MIN_VALUE;
    long kineticAt = Long.MIN_VALUE;
    String lastRecipe = "";
    private long lastServerTick = Long.MIN_VALUE;
    private long generatedHeatAt = Long.MIN_VALUE;
    private int deliveredHeat;

    WorkshopNode(BlockPos pos, BlockState state) {
        super(WorkshopPlatform.type(), pos, state);
        kind = ((WorkshopBlock) state.getBlock()).kind;
        inlet = new Tank(kind.inputVolume());
        outlet =
                switch (kind) {
                    case FLUID_BOTTLER,
                                    FLUID_REGULATOR,
                                    FLUID_DISTRIBUTOR,
                                    WEIGHTED_DISTRIBUTOR,
                                    PUMP,
                                    SEMIFLUID,
                                    FLUID_HEAT ->
                            inlet;
                    default -> new Tank(kind.outputVolume());
                };
        byproduct =
                new Tank(
                        kind == WorkshopKind.ELECTROLYZER || kind == WorkshopKind.STEAM_TURBINE
                                ? kind.outputVolume()
                                : 0);
    }

    void tick(ServerLevel level) {
        lastServerTick = level.getGameTime();
        servicePowerItem();
        WorkshopAutomation.tick(this, level);
        if (kind == WorkshopKind.PUMP) {
            serviceContainer(0, WorkshopInventory.OUTPUT_START, true, "");
        }
        if (kind == WorkshopKind.SOLID_HEAT) {
            tickSolidHeat(level);
            return;
        }
        if (kind == WorkshopKind.ELECTRIC_HEAT) {
            setActive(level, generatedHeatAt >= lastServerTick - 1);
            return;
        }
        if (WorkshopDevices.tick(this, level)
                || WorkshopThermal.tick(this, level)
                || WorkshopKinetic.tick(this, level)
                || WorkshopMatter.tick(this, level)
                || WorkshopCokeKiln.tick(this, level)) {
            return;
        }
        var job = WorkshopPlans.plan(this, level);
        preview = job == null || job.products().isEmpty() ? ItemStack.EMPTY : job.products().get(0);
        var outputs =
                job == null
                        ? null
                        : kind == WorkshopKind.INDUCTION
                                ? inductionOutputs(job)
                                : packed(job.products());
        if (kind == WorkshopKind.BLAST_FURNACE) {
            heatBlastFurnace(level);
            if (inlet.amount == 0
                    || outputs != null
                            && heat >= BLAST_FURNACE_WORK_HEAT
                            && inlet.amount < job.airPerTick()) {
                refillAirCell();
            }
        }
        boolean preheated =
                (kind == WorkshopKind.CENTRIFUGE || kind == WorkshopKind.INDUCTION)
                        && warmElectric(level, outputs == null ? null : job);
        if (job == null || outputs == null) {
            resetProgress();
            setActive(level, kind == WorkshopKind.BLAST_FURNACE && heat >= BLAST_FURNACE_WORK_HEAT);
            return;
        }
        if (!job.key().equals(lastRecipe)) {
            progress = 0;
            lastRecipe = job.key();
            setChanged();
        }
        var upgrades = WorkshopAutomation.basic(this);
        var stats = WorkshopAutomation.stats(this, job.duration(), upgrades);
        int duration = stats.duration();
        if (cycleDuration != duration) {
            if (cycleDuration > 0) {
                progress = ProcessingUpgrades.progress(progress, cycleDuration, duration);
            }
            cycleDuration = duration;
            setChanged();
        }
        var required = job.requested();
        if (kind == WorkshopKind.ORE_WASHER
                && energy >= stats.energyPerTick()
                && !inlet.has(required.id(), required.amount())) {
            serviceContainer(
                    WorkshopInventory.WASHER_WATER_SLOT,
                    WorkshopInventory.WASHER_CONTAINER_RETURN_SLOT,
                    false,
                    required.id());
        }
        boolean operating = canWork(job, stats.energyPerTick());
        if (operating) {
            energy -= stats.energyPerTick();
            if (kind == WorkshopKind.CENTRIFUGE
                    && !preheated
                    && progress + 1 < duration
                    && energy >= 1) {
                energy -= 1;
            }
            if (job.airPerTick() > 0) {
                inlet.drain(job.airPerTick(), false);
            }
            int advances = kind == WorkshopKind.INDUCTION ? Math.max(1, heat / 500) : 1;
            progress += advances;
            if (progress >= duration) {
                apply(level, job, outputs);
                if (stats.operations() > 1) {
                    applyAdditionalOperations(level, job, stats.operations() - 1);
                }
                progress = 0;
            }
            setChanged();
        }
        if (!operating
                && kind == WorkshopKind.CENTRIFUGE
                && energy < kind.workingEu
                && progress > 0) {
            resetProgress();
        }
        setActive(
                level,
                operating || kind == WorkshopKind.BLAST_FURNACE && heat >= BLAST_FURNACE_WORK_HEAT);
    }

    private void applyAdditionalOperations(ServerLevel level, WorkshopPlans.Job first, int count) {
        for (int batch = 0; batch < count; batch++) {
            var next = WorkshopPlans.plan(this, level);
            if (next == null || !next.key().equals(first.key()) || !canWork(next, 0)) {
                return;
            }
            var products = packed(next.products());
            if (products == null) {
                return;
            }
            apply(level, next, products);
        }
    }

    private boolean canWork(WorkshopPlans.Job job, int operatingEu) {
        var required = job.requested();
        var product = job.product();
        return energy >= operatingEu
                && (required.amount() == 0 || inlet.has(required.id(), required.amount()))
                && (job.airPerTick() == 0 || inlet.has("ic2:air", job.airPerTick()))
                && (product.amount() == 0
                        || WorkshopPlatform.fluidExists(product.id())
                                && outlet.fill(product.id(), product.amount(), true)
                                        == product.amount())
                && (kind != WorkshopKind.CENTRIFUGE || heat > job.heat())
                && (kind != WorkshopKind.BLAST_FURNACE || heat >= BLAST_FURNACE_WORK_HEAT);
    }

    boolean canOutputTo(int lane, ItemStack product) {
        var current = inventory.getItem(WorkshopInventory.OUTPUT_START + lane);
        return current.isEmpty()
                || WorkshopPlatform.sameItem(current, product)
                        && current.getCount() + product.getCount() <= current.getMaxStackSize();
    }

    void appendOutput(ItemStack product) {
        var existing = inventory.getItem(WorkshopInventory.OUTPUT_START);
        if (existing.isEmpty()) {
            inventory.setItem(WorkshopInventory.OUTPUT_START, product);
        } else {
            existing.grow(product.getCount());
            setChanged();
        }
    }

    private List<ItemStack> inductionOutputs(WorkshopPlans.Job job) {
        var slots =
                new ArrayList<>(
                        List.of(
                                inventory.getItem(WorkshopInventory.OUTPUT_START).copy(),
                                inventory.getItem(WorkshopInventory.OUTPUT_START + 1).copy()));
        for (int index = 0; index < job.input().size(); index++) {
            int lane = job.input().get(index).slot();
            var product = job.products().get(index);
            var current = slots.get(lane);
            if (current.isEmpty()) {
                slots.set(lane, product.copy());
            } else {
                current.grow(product.getCount());
            }
        }
        return slots;
    }

    private List<ItemStack> packed(List<ItemStack> products) {
        var slots = new ArrayList<ItemStack>();
        for (int slot = WorkshopInventory.OUTPUT_START;
                slot < WorkshopInventory.OUTPUT_START + kind.outputSlots();
                slot++) {
            slots.add(inventory.getItem(slot).copy());
        }
        for (var result : products) {
            if (result.isEmpty()) {
                return null;
            }
            int remaining = result.getCount();
            for (int index = 0; index < slots.size() && remaining > 0; index++) {
                var there = slots.get(index);
                if (!there.isEmpty() && WorkshopPlatform.sameItem(there, result)) {
                    int moved = Math.min(remaining, there.getMaxStackSize() - there.getCount());
                    there.grow(moved);
                    remaining -= moved;
                }
            }
            for (int index = 0; index < slots.size() && remaining > 0; index++) {
                if (slots.get(index).isEmpty()) {
                    int moved = Math.min(remaining, result.getMaxStackSize());
                    var added = result.copy();
                    added.setCount(moved);
                    slots.set(index, added);
                    remaining -= moved;
                }
            }
            if (remaining > 0) {
                return null;
            }
        }
        return slots;
    }

    private void apply(ServerLevel level, WorkshopPlans.Job job, List<ItemStack> outputs) {
        for (var input : job.input()) {
            inventory.removeItem(input.slot(), input.count());
        }
        inlet.drain(job.requested().amount(), false);
        outlet.fill(job.product().id(), job.product().amount(), false);
        if (kind != WorkshopKind.RECYCLER || level.getRandom().nextInt(8) == 0) {
            writeOutput(outputs);
        }
    }

    private void writeOutput(List<ItemStack> outputs) {
        for (int index = 0; index < outputs.size(); index++) {
            inventory.setItem(WorkshopInventory.OUTPUT_START + index, outputs.get(index));
        }
    }

    private void refillAirCell() {
        var cell = inventory.getItem(WorkshopInventory.AUX_SLOT);
        if (!WorkshopPlatform.itemId(cell).equals("ic2:air_cell")
                || inlet.fill("ic2:air", 1000, true) != 1000) {
            return;
        }
        var returned = inventory.getItem(WorkshopInventory.RETURN_SLOT);
        var emptyCell = WorkshopPlatform.stack("ic2:empty_cell", 1);
        if (!returned.isEmpty()
                && (!WorkshopPlatform.sameItem(returned, emptyCell)
                        || returned.getCount() >= returned.getMaxStackSize())) {
            return;
        }
        inventory.removeItem(WorkshopInventory.AUX_SLOT, 1);
        inlet.fill("ic2:air", 1000, false);
        if (returned.isEmpty()) {
            inventory.setItem(WorkshopInventory.RETURN_SLOT, emptyCell);
        } else {
            returned.grow(1);
            inventory.setItem(WorkshopInventory.RETURN_SLOT, returned);
        }
    }

    private void heatBlastFurnace(ServerLevel level) {
        var face = WorkshopBlock.direction(getBlockState());
        int old = heat;
        int gained =
                WorkshopDevices.neighboringHeat(
                        this, level, face, Math.max(1, BLAST_FURNACE_HEAT_CAPACITY - heat));
        heat =
                gained == 0
                        ? Math.max(0, heat - 1)
                        : Math.min(BLAST_FURNACE_HEAT_CAPACITY, heat + gained);
        if (heat != old) {
            setChanged();
        }
    }

    private boolean warmElectric(ServerLevel level, WorkshopPlans.Job job) {
        int target = kind == WorkshopKind.INDUCTION ? 10_000 : job == null ? 0 : job.heat() + 1;
        boolean warm = job != null || WorkshopAutomation.effectiveSignal(this, level);
        int old = heat;
        if (warm && heat < target && energy >= 1) {
            energy -= 1;
            heat++;
        } else if ((!warm || energy < 1) && heat > 0) {
            heat--;
        }
        if (heat != old) {
            setChanged();
        }
        return heat > old;
    }

    private void resetProgress() {
        if (progress != 0 || !lastRecipe.isEmpty()) {
            progress = 0;
            lastRecipe = "";
            cycleDuration = 0;
            setChanged();
        }
    }

    int bladeHardness() {
        return switch (WorkshopPlatform.itemId(inventory.getItem(WorkshopInventory.AUX_SLOT))) {
            case "ic2:iron_cutting_blade" -> 3;
            case "ic2:steel_cutting_blade" -> 6;
            case "ic2:diamond_cutting_blade" -> 9;
            default -> 0;
        };
    }

    void cycleMode() {
        selectMode((mode + 1) % kind.modes());
    }

    void selectMode(int requested) {
        if (mode != requested) {
            mode = requested;
            resetProgress();
            setChanged();
        }
    }

    private void servicePowerItem() {
        var stack = inventory.getItem(WorkshopInventory.POWER_ITEM_SLOT);
        if (stack.isEmpty()) return;
        int tier = energyTier();
        if (kind.chargesItem()) {
            draw(PowerPlatform.charge(stack, energy, tier));
        } else if (kind.dischargesItem()) {
            double room = energyCapacity() - energy;
            if (room > 0) receive(PowerPlatform.discharge(stack, room, tier));
        }
    }

    private void serviceContainer(int inputSlot, int returnSlot, boolean filling, String allowed) {
        var input = inventory.getItem(inputSlot);
        if (input.isEmpty()) return;
        var moved =
                filling
                        ? WorkshopPlatform.containerRecipe(inlet, input, true)
                        : WorkshopCells.drain(inlet, input, allowed);
        if (moved == null) return;
        var remainder = moved.stack();
        var previous = inventory.getItem(returnSlot);
        if (!remainder.isEmpty()
                && !previous.isEmpty()
                && (!WorkshopPlatform.sameItem(previous, remainder)
                        || previous.getCount() + remainder.getCount()
                                > previous.getMaxStackSize())) {
            return;
        }
        int amount = moved.liquid().amount();
        String fluid = moved.liquid().id();
        if (filling) {
            if (!inlet.has(fluid, amount)) return;
            inlet.drain(amount, false);
        } else {
            if (inlet.fill(fluid, amount, true) != amount) return;
            inlet.fill(fluid, amount, false);
        }
        inventory.removeItem(inputSlot, 1);
        if (previous.isEmpty()) {
            inventory.setItem(returnSlot, remainder);
        } else if (!remainder.isEmpty()) {
            previous.grow(remainder.getCount());
        }
        setChanged();
    }

    String modeKey() {
        return "gui.ic2.workshop." + kind.path + "." + mode;
    }

    /** Finds a loaded pump with one complete liter for a neighboring compressor. */
    public static WorkshopNode connectedWaterPump(ServerLevel level, BlockPos compressor) {
        for (var face : Direction.values()) {
            var pos = compressor.relative(face);
            if (level.hasChunkAt(pos)
                    && level.getBlockEntity(pos) instanceof WorkshopNode pump
                    && pump.kind == WorkshopKind.PUMP
                    && !pump.isRemoved()
                    && pump.accessibleOutput(face.getOpposite()) != null
                    && pump.outlet.has("minecraft:water", WorkshopCells.VOLUME)) {
                return pump;
            }
        }
        return null;
    }

    /** Commits the checked liter once the compressor is ready to publish its snowball. */
    public void consumeCompressionWater() {
        if (kind == WorkshopKind.PUMP && outlet.has("minecraft:water", WorkshopCells.VOLUME)) {
            outlet.drain(WorkshopCells.VOLUME, false);
            setChanged();
        }
    }

    boolean exposesIntake(Direction side) {
        if (kind == WorkshopKind.PUMP) {
            return false;
        }
        if (side == null) {
            return inlet.capacity > 0;
        }
        Direction direction = WorkshopBlock.direction(getBlockState());
        return switch (kind) {
            case FLUID_REGULATOR -> side != direction;
            case FLUID_DISTRIBUTOR -> (side == direction) == (mode == 0);
            case WEIGHTED_DISTRIBUTOR -> side == direction;
            case ELECTROLYZER -> side.getAxis().isHorizontal();
            default -> inlet.capacity > 0;
        };
    }

    Tank accessibleOutput(Direction side) {
        if (outlet.capacity == 0) {
            return null;
        }
        if (side == null) {
            return outlet;
        }
        Direction direction = WorkshopBlock.direction(getBlockState());
        return switch (kind) {
            case ELECTROLYZER ->
                    side == Direction.UP ? byproduct : side == Direction.DOWN ? outlet : null;
            case FLUID_DISTRIBUTOR -> (side != direction) == (mode == 0) ? outlet : null;
            case WEIGHTED_DISTRIBUTOR -> side != direction ? outlet : null;
            case PUMP -> side == direction ? null : outlet;
            case COKE_GRATE -> side == Direction.DOWN ? outlet : null;
            case FLUID_REGULATOR, FLUID_HEAT, SEMIFLUID -> null;
            default -> outlet;
        };
    }

    boolean canReceiveFluid(String id) {
        return switch (kind) {
            case ORE_WASHER, ELECTROLYZER, SOLAR_DISTILLER -> id.equals("minecraft:water");
            case BLAST_FURNACE -> id.equals("ic2:air");
            case FERMENTER -> id.equals("ic2:biomass");
            case HEAT_EXCHANGER -> id.equals("ic2:hot_coolant");
            case REPLICATOR -> id.equals("ic2:uu_matter");
            case SEMIFLUID, FLUID_HEAT -> WorkshopThermal.validFuel(kind, id);
            case STEAM_BOILER, STIRLING_KINETIC ->
                    id.equals("minecraft:water") || id.equals("ic2:distilled_water");
            case STEAM_REPRESSURIZER, STEAM_TURBINE, CONDENSER ->
                    id.equals("ic2:steam") || id.equals("ic2:superheated_steam");
            default -> WorkshopPlatform.fluidExists(id);
        };
    }

    int receiveFluid(String id, int requested, boolean simulate) {
        if (!canReceiveFluid(id)) {
            return 0;
        }
        int accepted = inlet.fill(id, requested, simulate);
        if (!simulate && accepted > 0) {
            setChanged();
        }
        return accepted;
    }

    int outputFluid(Tank tank, String expected, int requested, boolean simulate) {
        if (tank == null || (!expected.isEmpty() && !tank.id.equals(expected))) {
            return 0;
        }
        int drained = tank.drain(requested, simulate);
        if (!simulate && drained > 0) {
            setChanged();
        }
        return drained;
    }

    int pullHeat(ServerLevel level, Direction side, int maximum) {
        if (isRemoved() || maximum <= 0 || side != WorkshopBlock.direction(getBlockState())) {
            return 0;
        }
        switch (kind) {
            case SOLID_HEAT -> {
                return drawSolidHeat(level, maximum);
            }
            case RADIOISOTOPE_HEAT, FLUID_HEAT -> {
                return WorkshopThermal.drawHeat(this, level, maximum);
            }
            case HEAT_EXCHANGER -> {
                return WorkshopThermal.exchangeHeat(this, level, maximum);
            }
            case ELECTRIC_HEAT -> {}
            default -> {
                return 0;
            }
        }
        int coils = inventory.installedComponents("ic2:coil");
        if (coils == 0) return 0;
        if (generatedHeatAt != level.getGameTime()) {
            deliveredHeat = 0;
        }
        int units = Math.min(maximum, Math.min((int) (energy * 2), 10 * coils - deliveredHeat));
        if (units > 0) {
            generatedHeatAt = level.getGameTime();
            deliveredHeat += units;
            energy -= units * 0.5;
            setChanged();
            return units;
        }
        return 0;
    }

    private void tickSolidHeat(ServerLevel level) {
        if (heat > Integer.MAX_VALUE - 20) {
            setActive(level, false);
            return;
        }
        if (heatFuelTicks > 0) {
            heat += 20;
            if (--heatFuelTicks == 0 && solidProducesAsh) {
                var ash = WorkshopPlatform.stack("ic2:ashes", 1);
                var output = packed(List.of(ash));
                if (output == null) {
                    net.minecraft.world.level.block.Block.popResource(level, getBlockPos(), ash);
                } else {
                    writeOutput(output);
                }
            }
            setChanged();
        } else {
            var fuel = inventory.getItem(0);
            int burn = WorkshopPlatform.solidFuelTime(level, fuel) / 4;
            if (burn > 0) {
                solidProducesAsh = WorkshopPlatform.itemId(fuel).equals("minecraft:coal");
                var consumed = inventory.removeItem(0, 1);
                var remainder = WorkshopPlatform.fuelRemainder(consumed);
                if (!remainder.isEmpty()) {
                    if (inventory.getItem(0).isEmpty()) {
                        inventory.setItem(0, remainder);
                    } else {
                        net.minecraft.world.level.block.Block.popResource(
                                level, getBlockPos(), remainder);
                    }
                }
                heatFuelTicks = burn;
                setChanged();
            }
        }
        setActive(level, heatFuelTicks > 0);
    }

    private int drawSolidHeat(ServerLevel level, int maximum) {
        long time = level.getGameTime();
        if (generatedHeatAt != time) {
            deliveredHeat = 0;
        }
        int heatUnits = Math.min(heat, Math.min(maximum, 20 - deliveredHeat));
        if (heatUnits > 0) {
            generatedHeatAt = time;
            deliveredHeat += heatUnits;
            heat -= heatUnits;
            setChanged();
        }
        return heatUnits;
    }

    void setActive(ServerLevel level, boolean active) {
        var state = getBlockState();
        if (state.getValue(WorkshopBlock.ACTIVE) != active) {
            level.setBlockAndUpdate(getBlockPos(), state.setValue(WorkshopBlock.ACTIVE, active));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ic2." + kind.path);
    }

    /** Rotor rating for a wind kinetic device, zero when empty or negative for another kind. */
    public int windRotor() {
        return kind == WorkshopKind.WIND_KINETIC ? WorkshopKinetic.rotor(this) : -1;
    }

    /** Measures wind using the working rotor and native obstruction checks without drawing work. */
    public int windPotential(ServerLevel level) {
        return kind == WorkshopKind.WIND_KINETIC
                ? WorkshopKinetic.wind(this, level, WorkshopBlock.direction(getBlockState()))
                : 0;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory player, Player opener) {
        return new WorkshopMenu(id, player, this);
    }

    @Override
    public boolean ranThisTick(ServerLevel world) {
        return !isRemoved() && world == getLevel() && lastServerTick == world.getGameTime();
    }

    @Override
    public Ports energyPorts() {
        return kind.generatesPower()
                ? new Ports(Set.of(), SIDES, false)
                : new Ports(kind.capacity > 0 ? SIDES : Set.of(), Set.of(), false);
    }

    @Override
    public double offer() {
        return kind.generatesPower() ? Math.min(energy, packetLimit()) : 0;
    }

    @Override
    public double draw(double requested) {
        if (!Double.isFinite(requested) || requested <= 0 || !kind.generatesPower()) {
            return 0;
        }
        double supplied = Math.min(energy, requested);
        energy -= supplied;
        if (supplied > 0) {
            setChanged();
        }
        return supplied;
    }

    @Override
    public double receive(double offered) {
        if (!Double.isFinite(offered) || offered <= 0 || kind.generatesPower()) {
            return 0;
        }
        double amount = Math.max(0, Math.min(offered, energyCapacity() - energy));
        if (amount > 0) {
            energy += amount;
            WorkshopMatter.applyAmplifier(this, amount);
            setChanged();
        }
        return amount;
    }

    @Override
    public EuBuffer.Reading bufferedEu() {
        return new EuBuffer.Reading(energy, energyCapacity());
    }

    /** Physical input, output, and byproduct tanks, each listed once. */
    public List<FluidContents> fluidContents() {
        var tanks = new ArrayList<FluidContents>(3);
        if (inlet.capacity > 0) tanks.add(readTank(inlet));
        if (outlet != inlet && outlet.capacity > 0) tanks.add(readTank(outlet));
        if (byproduct.capacity > 0) tanks.add(readTank(byproduct));
        return tanks;
    }

    private static FluidContents readTank(Tank tank) {
        return tank.amount == 0
                ? FluidContents.empty(tank.capacity)
                : new FluidContents(
                        WorkshopPlatform.previewFluid(WorkshopPlatform.previewFluidId(tank.id)),
                        tank.amount,
                        tank.capacity);
    }

    int energyCapacity() {
        var upgrades = WorkshopAutomation.basic(this);
        return upgrades.equals(ProcessingUpgrades.NONE) || !WorkshopAutomation.supported(kind)
                ? kind.capacity
                : WorkshopAutomation.stats(this, kind.duration, upgrades).capacity();
    }

    @Override
    public double packetLimit() {
        return PacketBudget.packetSize(energyTier());
    }

    int energyTier() {
        return kind.tier + WorkshopAutomation.basic(this).transformers();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        WorldEnergy.connect(this);
    }

    @Override
    public void onChunkUnloaded() {
        WorldEnergy.disconnect(this);
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        WorldEnergy.disconnect(this);
        super.setRemoved();
    }

    static final class Tank {
        final int capacity;
        String id = "";
        int amount;

        Tank(int capacity) {
            this.capacity = capacity;
        }

        boolean has(String fluid, int needed) {
            return id.equals(fluid) && amount >= needed;
        }

        int fill(String fluid, int incoming, boolean simulate) {
            if (incoming <= 0 || fluid.isEmpty() || (!id.isEmpty() && !id.equals(fluid))) {
                return 0;
            }
            int accepted = Math.min(incoming, capacity - amount);
            if (!simulate && accepted > 0) {
                id = fluid;
                amount += accepted;
            }
            return accepted;
        }

        int drain(int requested, boolean simulate) {
            int taken = Math.min(amount, Math.max(0, requested));
            if (!simulate) {
                amount -= taken;
                if (amount == 0) {
                    id = "";
                }
            }
            return taken;
        }

        void restore(String fluid, int saved) {
            amount = 0;
            id = "";
            if (WorkshopPlatform.fluidExists(fluid)) {
                fill(fluid, saved, false);
            }
        }
    }
}
