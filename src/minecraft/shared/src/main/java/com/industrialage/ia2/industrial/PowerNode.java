package com.industrialage.ia2.industrial;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.energy.graph.EnergyGraph.Ports;
import com.industrialage.ia2.energy.graph.PacketBudget;
import com.industrialage.ia2.logistics.FluidContents;
import com.industrialage.ia2.platform.CapabilityFacade.Face;
import com.industrialage.ia2.power.EuBuffer;
import com.industrialage.ia2.power.WorldEnergy;
import com.industrialage.ia2.power.WorldPowerPort;
import com.industrialage.ia2.reactor.ElectricReactor;
import java.util.EnumSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Real-world EU source, accumulator, transformer or reactor connected to its level's energy graph.
 */
public final class PowerNode extends PowerSaveBase
        implements com.industrialage.ia2.access.MachineAccessTarget,
                WorldPowerPort,
                EuBuffer,
                com.industrialage.ia2.power.WorldPowerStorage,
                MenuProvider {
    private static final Set<Face> ALL = Set.of(Face.values());
    private static final Ports OUTPUT = new Ports(Set.of(), ALL, false);
    static final String LAVA = "minecraft:lava";
    static final int LAVA_CAPACITY = 8_000;
    private static final int LAVA_PER_TICK = 2;
    final PowerKind kind;
    final PowerInventory inventory;
    final PlayerMachines.LiftContacts liftContacts;
    final ElectricReactor reactor = new ElectricReactor();
    int chambers;
    boolean fluidMode;
    int coolant;
    int hotCoolant;
    BlockPos vesselCenter;

    PowerNode coolantTank() {
        if (kind == PowerKind.NUCLEAR_REACTOR) return fluidMode ? this : null;
        return kind == PowerKind.REACTOR_FLUID_PORT && getLevel() instanceof ServerLevel server
                ? ReactorVessel.core(this, server)
                : null;
    }

    /** Current cold and hot coolant in millibuckets, as held by the controller. */
    public int coolantAmount(boolean hot) {
        return hot ? hotCoolant : coolant;
    }

    /** Fluid mode suppresses electrical output from both the controller and its chambers. */
    public boolean isFluidReactor() {
        return kind == PowerKind.NUCLEAR_REACTOR && fluidMode;
    }

    int automationLimit(int slot, ItemStack stack) {
        if (kind == PowerKind.REACTOR_ACCESS_HATCH)
            return PowerPlatform.path(stack).equals("heatpack") ? 64 : 1;
        return inventory.automationLimit(slot, stack);
    }

    net.minecraft.world.WorldlyContainer automationInventory() {
        return kind == PowerKind.REACTOR_ACCESS_HATCH ? new ReactorHatch(this) : inventory;
    }

    int mode;
    boolean stepUp;
    int fuel;
    java.util.UUID owner;
    int saleSize = 1000;
    int trades;
    double credit;
    BlockPos target;
    long cooldownUntil;
    int liquid;
    double energy;
    double waterFraction;
    private int lastSignal = -1;
    private int nearbyWater;
    private int windObstructions;
    private boolean linked;
    private long lastTick = Long.MIN_VALUE;
    private double remaining;

    /** Creates the machine state for this physical block. */
    public PowerNode(BlockPos pos, BlockState state) {
        super(PowerPlatform.type(), pos, state);
        kind = ((PowerBlock) state.getBlock()).kind;
        inventory = new PowerInventory(this);
        liftContacts = kind == PowerKind.MAGNETIZER ? new PlayerMachines.LiftContacts() : null;
    }

    void tick(ServerLevel level) {
        if (kind.vesselPart()) {
            if (kind == PowerKind.REACTOR_FLUID_PORT
                    && !inventory.getItem(0).isEmpty()
                    && level.getGameTime() % 8 == 0) servicePort(level);
            return;
        }
        long now = level.getGameTime();
        if (lastTick == now) return;
        boolean first = lastTick == Long.MIN_VALUE;
        lastTick = now;
        if (kind.chargeBlock()) {
            if (level.hasNeighborSignal(worldPosition)) GameCharges.prime(level, this);
            return;
        }
        if (!linked) {
            WorldEnergy.connect(this);
            linked = true;
        }
        boolean active = false;
        if (kind.lowTier() > 0) {
            boolean next = mode == 2 || mode == 0 && level.hasNeighborSignal(worldPosition);
            if (stepUp != next) {
                stepUp = next;
                WorldEnergy.connect(this);
                setChanged();
            }
            active = stepUp;
        } else if (kind == PowerKind.NUCLEAR_REACTOR) {
            if (first || (now + worldPosition.asLong()) % 20 == 0) {
                boolean powered = level.hasNeighborSignal(worldPosition);
                int count = 0;
                for (Direction side : Direction.values()) {
                    BlockPos pos = worldPosition.relative(side);
                    if (level.hasChunkAt(pos)
                            && level.getBlockEntity(pos) instanceof PowerNode n
                            && n.kind == PowerKind.REACTOR_CHAMBER
                            && n.controller(level) == this) {
                        count++;
                        powered |= level.hasNeighborSignal(pos);
                    }
                }
                // A first tick after a reload only discovers the currently attached chambers.
                // A component cycle must keep world cadence across a close/reopen.
                if (count < chambers) ejectHidden(level, count);
                chambers = count;
                int shell = chambers == 6 ? ReactorVessel.inspect(level, worldPosition) : -1;
                boolean wasFluid = fluidMode;
                fluidMode = shell >= 0;
                if (fluidMode != wasFluid) MachineAccess.refresh(this);
                if (fluidMode) {
                    powered |= shell > 0;
                    refillCoolant();
                }
                if ((now + worldPosition.asLong()) % 20 == 0) {
                    reactor.cycle(inventory, count, powered, PowerSettings.multiplier("nuclear"));
                    if (fluidMode) {
                        int heat = reactor.ventedHeat();
                        int warmed =
                                Math.min(
                                        heat * 2,
                                        Math.min(
                                                coolant, ReactorVessel.TANK_CAPACITY - hotCoolant));
                        warmed &= ~1;
                        coolant -= warmed;
                        hotCoolant += warmed;
                        reactor.returnHeat(heat - warmed / 2);
                    }
                    setChanged();
                    if (reactor.heat() >= reactor.limit()) {
                        PowerPlatform.meltdown(
                                level,
                                worldPosition,
                                ElectricReactor.meltdownPower(inventory, chambers));
                        return;
                    }
                }
            } else if (fluidMode) {
                refillCoolant();
            }
            remaining = fluidMode ? 0 : reactor.output();
            active = reactor.output() > 0 || reactor.heat() >= 1000;
            if (fluidMode) {
                serviceContainer(56, true);
            }
        } else if (kind.playerMachine()) {
            int dischargeSlot = inventory.dischargeSlot();
            if (dischargeSlot >= 0) {
                double room =
                        kind == PowerKind.ENERGY_O_MAT ? Math.min(credit, capacity()) : capacity();
                double used =
                        PowerPlatform.discharge(
                                inventory.getItem(dischargeSlot),
                                Math.max(0, room - energy),
                                capacity(),
                                kind == PowerKind.ENERGY_O_MAT ? 4 : upgradeTier());
                if (used > 0) {
                    energy += used;
                    setChanged();
                }
            }
            if ((kind == PowerKind.RCI_LZH || kind == PowerKind.RCI_RSH) && now % 8 == 0) {
                var upgrades =
                        java.util.List.of(
                                inventory.getItem(10),
                                inventory.getItem(11),
                                inventory.getItem(12),
                                inventory.getItem(13));
                int slot = (int) ((now / 8) % 9);
                com.industrialage.ia2.world.MachineAutomation.transfer(
                        level,
                        worldPosition,
                        inventory,
                        slot,
                        -1,
                        upgrades,
                        item -> inventory.acceptsAutomation(slot, item));
            }
            active = PlayerMachines.tick(this, level);
        } else if (kind != PowerKind.REACTOR_CHAMBER && !kind.storage()) {
            double generated = generate(level, now, first);
            if (generated > 0 && energy < capacity()) {
                energy = Math.min(capacity(), energy + generated);
                setChanged();
            }
            active = generated > 0;
        }
        int chargeSlot = kind.chargeSlot();
        if (chargeSlot >= 0) {
            double input =
                    kind.storage()
                            ? PowerPlatform.discharge(
                                    inventory.getItem(1),
                                    Math.max(0, capacity() - energy),
                                    capacity(),
                                    kind.itemTier())
                            : 0;
            energy += input;
            double output =
                    PowerPlatform.charge(inventory.getItem(chargeSlot), energy, kind.itemTier());
            energy -= output;
            if (input > 0 || output > 0) setChanged();
        }
        if (kind.storage() && lastSignal != redstoneSignal()) {
            lastSignal = redstoneSignal();
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        BlockState current = getBlockState();
        if (current.getValue(PowerBlock.ACTIVE) != active)
            level.setBlock(worldPosition, current.setValue(PowerBlock.ACTIVE, active), 2);
    }

    private double generate(ServerLevel level, long now, boolean first) {
        switch (kind) {
            case SOLAR_GENERATOR:
                return PowerSettings.sun(level, worldPosition)
                        ? PowerSettings.multiplier("solar")
                        : 0;
            case RT_GENERATOR:
                int pellets = 0;
                for (int i = 0; i < 6; i++) if (inventory.part(i).equals("rtg_pellet")) pellets++;
                return pellets == 0
                        ? 0
                        : (1 << (pellets - 1)) * PowerSettings.multiplier("radioisotope");
            case GEO_GENERATOR:
                refillLava();
                double gain = 20 * PowerSettings.multiplier("geothermal");
                if (capacity() - energy < gain || liquid < LAVA_PER_TICK) return 0;
                liquid -= LAVA_PER_TICK;
                setChanged();
                return gain;
            case WIND_GENERATOR:
                if (first || Math.floorMod(now + worldPosition.asLong(), 128) == 0) {
                    windObstructions = 0;
                    for (BlockPos p :
                            BlockPos.betweenClosed(
                                    worldPosition.offset(-4, -2, -4),
                                    worldPosition.offset(4, 4, 4))) {
                        if (!p.equals(worldPosition)
                                && level.hasChunkAt(p)
                                && !level.isEmptyBlock(p)) windObstructions++;
                    }
                }
                return com.industrialage.ia2.content.WindHeight.response(worldPosition.getY())
                        * breeze(level, now)
                        * com.industrialage.ia2.content.WindHeight.clearance(windObstructions)
                        * PowerSettings.multiplier("wind");
            case WATER_GENERATOR:
                if (fuel == 0 && energy < capacity()) {
                    var water =
                            WorkshopCells.measure(inventory.getItem(0), WorkshopThermal::isWater);
                    if (water != null && storeContainerRemainder(water.stack())) {
                        inventory.removeItem(0, 1);
                        fuel = 500;
                    }
                }
                if (fuel > 0) {
                    fuel--;
                    setChanged();
                    return PowerSettings.multiplier("water");
                }
                if (first || Math.floorMod(now + worldPosition.asLong(), 64) == 0) {
                    nearbyWater = 0;
                    for (BlockPos p :
                            BlockPos.betweenClosed(
                                    worldPosition.offset(-1, -1, -1),
                                    worldPosition.offset(1, 1, 1))) {
                        if (level.hasChunkAt(p) && level.getFluidState(p).is(FluidTags.WATER))
                            nearbyWater++;
                    }
                }
                if (nearbyWater > 0 && energy < capacity()) {
                    waterFraction += nearbyWater * 0.01 * PowerSettings.multiplier("water");
                    int whole = (int) waterFraction;
                    waterFraction -= whole;
                    setChanged();
                    return whole;
                }
                return 0;
            default:
                return 0;
        }
    }

    private static double breeze(ServerLevel level, long tick) {
        // Shared fluctuations for installations in the same weather, continuous across a
        // save/restart.
        long epoch = Math.floorDiv(tick, 1024);
        var random = new java.util.Random(level.getSeed() + epoch * 0x9e3779b97f4a7c15L);
        double value = 3.6 + 2.6 * random.nextDouble();
        return value * (level.isThundering() ? 1.5 : level.isRaining() ? 1.25 : 1.0);
    }

    /** Potential wind at this altitude in the current weather, before local obstruction. */
    public static double ambientWind(ServerLevel level, int altitude) {
        return com.industrialage.ia2.content.WindHeight.response(altitude)
                * breeze(level, level.getGameTime());
    }

    /** Current native wind generator output, or a negative value for a different device. */
    public double measuredWind(ServerLevel level) {
        if (kind != PowerKind.WIND_GENERATOR) return -1;
        return ambientWind(level, worldPosition.getY())
                * com.industrialage.ia2.content.WindHeight.clearance(windObstructions)
                * PowerSettings.multiplier("wind");
    }

    private void refillLava() {
        for (int queued = inventory.getItem(0).getCount(); queued > 0; queued--) {
            int room = LAVA_CAPACITY - liquid;
            if (room <= 0) return;
            var intake = new WorkshopNode.Tank(room);
            var fuel = WorkshopPlatform.drainWholeContainer(intake, inventory.getItem(0));
            if (fuel == null || !WorkshopThermal.isLava(fuel.liquid().id())) return;
            int volume = fuel.liquid().amount();
            if (fillLava(volume, true) != volume || !storeContainerRemainder(fuel.stack())) return;
            inventory.removeItem(0, 1);
            fillLava(volume, false);
        }
    }

    private boolean storeContainerRemainder(ItemStack remainder) {
        if (remainder.isEmpty()) return true;
        ItemStack previous = inventory.getItem(1);
        if (!previous.isEmpty()
                && (!WorkshopPlatform.sameItem(previous, remainder)
                        || previous.getCount() + remainder.getCount() > previous.getMaxStackSize()))
            return false;
        if (previous.isEmpty()) inventory.setItem(1, remainder);
        else previous.grow(remainder.getCount());
        return true;
    }

    /** Native lava fluid ports use this method for both simulated and committed input. */
    public int fillLava(int offered, boolean simulate) {
        if (kind != PowerKind.GEO_GENERATOR || offered <= 0) return 0;
        int accepted = Math.min(offered, LAVA_CAPACITY - liquid);
        if (!simulate && accepted > 0) {
            liquid += accepted;
            setChanged();
        }
        return accepted;
    }

    PowerNode controller(ServerLevel level) {
        PowerNode result = null;
        for (Direction d : Direction.values()) {
            BlockPos p = worldPosition.relative(d);
            if (level.hasChunkAt(p)
                    && level.getBlockEntity(p) instanceof PowerNode core
                    && core.kind == PowerKind.NUCLEAR_REACTOR) {
                if (result != null) return null;
                result = core;
            }
        }
        return result;
    }

    private void ejectHidden(ServerLevel level, int count) {
        for (int i = 0; i < 54; i++)
            if (!ElectricReactor.exposed(i, count) && !inventory.getItem(i).isEmpty()) {
                Block.popResource(level, worldPosition, inventory.removeItemNoUpdate(i));
            }
        setChanged();
    }

    /** Current hull heat available to the machine menu and monitoring integrations. */
    public int hullHeat() {
        return reactor.heat();
    }

    /** Current continuous reactor generation before network demand and line loss. */
    public double reactorOutput() {
        return reactor.output();
    }

    @Override
    public EuBuffer.Reading bufferedEu() {
        return new EuBuffer.Reading(storedEu(), capacity());
    }

    /** Live EU/t for a reactor core or an attached reactor chamber in electric mode. */
    public OptionalDouble electricalReactorOutput(ServerLevel level) {
        var core =
                switch (kind) {
                    case NUCLEAR_REACTOR -> this;
                    case REACTOR_CHAMBER -> controller(level);
                    default -> null;
                };
        return core == null || core.fluidMode
                ? OptionalDouble.empty()
                : OptionalDouble.of(core.reactorOutput());
    }

    /** Live fluid contents of a geothermal inlet or a fluid-mode reactor. */
    public List<FluidContents> fluidContents() {
        if (kind == PowerKind.GEO_GENERATOR) {
            return List.of(readTank(LAVA, liquid, LAVA_CAPACITY));
        }
        var core = coolantTank();
        return core == null
                ? List.of()
                : List.of(
                        readTank("ic2:coolant", core.coolant, ReactorVessel.TANK_CAPACITY),
                        readTank("ic2:hot_coolant", core.hotCoolant, ReactorVessel.TANK_CAPACITY));
    }

    private static FluidContents readTank(String id, int amount, int capacity) {
        // The inlet has a fixed fluid type even while empty.
        return new FluidContents(
                WorkshopPlatform.previewFluid(WorkshopPlatform.previewFluidId(id)),
                amount,
                capacity);
    }

    /** Current stored EU, excluding temporary reactor offers. */
    public double storedEu() {
        return energy;
    }

    /** Active component grid width. */
    public int coreWidth() {
        return ElectricReactor.width(chambers);
    }

    /** Current component use at a fixed save slot. */
    public int componentUse(int slot) {
        return inventory.wear(slot);
    }

    void changeMode(ServerLevel level) {
        mode = (mode + 1) % (kind.storage() ? 7 : 3);
        WorldEnergy.connect(this);
        setChanged();
    }

    int redstoneSignal() {
        if (!kind.storage()) return 0;
        double unit = packetLimit(), maximum = capacity();
        return switch (mode) {
            case 1 -> energy >= maximum - 20 * unit ? 15 : 0;
            case 2 -> energy > unit && energy < maximum - unit ? 15 : 0;
            case 3 -> energy < maximum - unit ? 15 : 0;
            case 4 -> energy < unit ? 15 : 0;
            default -> 0;
        };
    }

    private Face front() {
        return Face.valueOf(getBlockState().getValue(BlockStateProperties.FACING).name());
    }

    @Override
    public Ports energyPorts() {
        if (kind.vesselPart()) return new Ports(Set.of(), Set.of(), false);
        int low = kind.lowTier();
        if (kind.playerMachine())
            return kind.poweredPlayerMachine()
                    ? new Ports(ALL, Set.of(), false)
                    : new Ports(Set.of(), Set.of(), false);
        if (low == 0 && !kind.storage()) return OUTPUT;
        var high = EnumSet.of(front());
        var others = EnumSet.complementOf(high);
        return low > 0 && !stepUp ? new Ports(high, others, false) : new Ports(others, high, false);
    }

    private int upgradeCount(String id) {
        int start =
                kind == PowerKind.MAGNETIZER
                        ? 2
                        : kind == PowerKind.RCI_RSH || kind == PowerKind.RCI_LZH ? 10 : -1;
        if (start < 0) return 0;
        int count = 0;
        for (int i = start; i < start + 4; i++)
            if (inventory.part(i).equals(id)) count += inventory.count(i);
        return count;
    }

    int upgradeTier() {
        return Math.min(5, 1 + upgradeCount("transformer_upgrade"));
    }

    private void refillCoolant() {
        for (int queued = inventory.getItem(54).getCount(); queued > 0; queued--) {
            if (!serviceContainer(54, false)) return;
        }
    }

    private boolean serviceContainer(int slot, boolean filling) {
        var input = inventory.getItem(slot);
        if (input.isEmpty() || !filling && coolant >= ReactorVessel.TANK_CAPACITY) return false;
        String liquid = filling ? "ic2:hot_coolant" : "ic2:coolant";
        WorkshopPlans.ContainerResult move;
        if (filling) {
            var tank = new WorkshopNode.Tank(ReactorVessel.TANK_CAPACITY);
            tank.fill(liquid, hotCoolant, false);
            move = WorkshopPlatform.fillContainer(tank, input);
        } else {
            var tank = new WorkshopNode.Tank(ReactorVessel.TANK_CAPACITY - coolant);
            move = WorkshopPlatform.drainWholeContainer(tank, input);
        }
        if (move == null || !WorkshopThermal.matchesCanonical(move.liquid().id(), liquid))
            return false;
        int volume = move.liquid().amount();
        if (volume <= 0 || (filling ? hotCoolant : ReactorVessel.TANK_CAPACITY - coolant) < volume)
            return false;
        var returned = move.stack();
        var output = inventory.getItem(slot + 1);
        if (!returned.isEmpty()
                && !output.isEmpty()
                && (!WorkshopPlatform.sameItem(output, returned)
                        || returned.getCount() + output.getCount() > output.getMaxStackSize()))
            return false;
        inventory.removeItem(slot, 1);
        if (output.isEmpty()) inventory.setItem(slot + 1, returned);
        else if (!returned.isEmpty()) output.grow(returned.getCount());
        if (filling) hotCoolant -= volume;
        else coolant += volume;
        setChanged();
        return true;
    }

    private void servicePort(ServerLevel level) {
        var core = coolantTank();
        if (core == null) return;
        boolean extract = inventory.part(0).equals("fluid_ejector_upgrade");
        var upgrade = inventory.getItem(0);
        int limit =
                Math.min(
                        1000,
                        100
                                * com.industrialage.ia2.content.ProcessingUpgrades.transferLimit(
                                        upgrade.getCount()));
        for (var side : com.industrialage.ia2.world.MachineAutomation.faces(upgrade)) {
            if (!MachineAccess.allowsUpgrade(this, side, extract, true)) continue;
            var target = worldPosition.relative(side);
            if (!level.hasChunkAt(target) || level.getBlockEntity(target) instanceof PowerNode)
                continue;
            if (extract && core.hotCoolant > 0) {
                core.hotCoolant -=
                        WorkshopPlatform.pushFluid(
                                level,
                                target,
                                side.getOpposite(),
                                "ic2:hot_coolant",
                                Math.min(limit, core.hotCoolant));
            } else if (!extract && core.coolant < ReactorVessel.TANK_CAPACITY) {
                var drawn =
                        WorkshopPlatform.pullMatchingFluid(
                                level,
                                target,
                                side.getOpposite(),
                                WorkshopThermal::isCoolant,
                                Math.min(limit, ReactorVessel.TANK_CAPACITY - core.coolant),
                                false);
                if (drawn != null) core.coolant += drawn.amount();
            }
        }
        core.setChanged();
    }

    double capacity() {
        if (kind.lowTier() > 0) return 8 * PacketBudget.packetSize(kind.lowTier());
        return switch (kind) {
            case RCI_RSH, RCI_LZH -> 10_000 + 10_000 * upgradeCount("energy_storage_upgrade");
            case ENERGY_O_MAT -> 10_000;
            case TESLA_COIL -> 10_000;
            case MAGNETIZER -> 100 + 10_000 * upgradeCount("energy_storage_upgrade");
            case CLASSIC_MFE -> 600_000;
            case CLASSIC_MFSU -> 10_000_000;
            case GEO_GENERATOR -> 2400;
            case RT_GENERATOR -> 20000;
            case WATER_GENERATOR -> 4;
            case WIND_GENERATOR -> 32;
            case SOLAR_GENERATOR -> 2;
            default -> 0;
        };
    }

    @Override
    public double packetLimit() {
        if (kind.lowTier() > 0) return PacketBudget.packetSize(kind.lowTier() + (stepUp ? 0 : 1));
        if (kind == PowerKind.MAGNETIZER || kind == PowerKind.RCI_RSH || kind == PowerKind.RCI_LZH)
            return PacketBudget.packetSize(upgradeTier());
        return switch (kind) {
            case ENERGY_O_MAT -> 2048;
            case CLASSIC_MFSU -> 512;
            case CLASSIC_MFE, TESLA_COIL -> 128;
            default -> 32;
        };
    }

    private double packet() {
        return kind.lowTier() > 0
                ? PacketBudget.packetSize(kind.lowTier() + (stepUp ? 1 : 0))
                : packetLimit();
    }

    @Override
    public int packetsPerTick() {
        return kind.lowTier() > 0 && !stepUp ? Math.min(4, (int) (energy / packet())) : 1;
    }

    @Override
    public double offer() {
        if (kind.storage()
                && mode >= 5
                && getLevel() instanceof ServerLevel level
                && level.hasNeighborSignal(worldPosition)
                && !(mode == 6 && energy > capacity() - 20 * packetLimit())) return 0;
        if (kind.playerMachine()) return 0;
        if (kind == PowerKind.CREATIVE_GENERATOR)
            return PacketBudget.packetSize(Math.max(1, Math.min(5, mode + 1)));
        if (kind == PowerKind.NUCLEAR_REACTOR) return remaining;
        if (kind == PowerKind.REACTOR_CHAMBER && getLevel() instanceof ServerLevel level) {
            var core = controller(level);
            return core == null ? 0 : core.offer();
        }
        double packet = packet();
        return kind.lowTier() > 0 || kind.storage()
                ? energy >= packet ? packet : 0
                : Math.min(energy, packet);
    }

    @Override
    public double draw(double eu) {
        if (eu <= 0) return 0;
        if (kind == PowerKind.CREATIVE_GENERATOR) return eu;
        if (kind == PowerKind.REACTOR_CHAMBER && getLevel() instanceof ServerLevel level) {
            var core = controller(level);
            return core == null ? 0 : core.draw(eu);
        }
        if (kind == PowerKind.NUCLEAR_REACTOR) {
            double used = Math.min(eu, remaining);
            remaining -= used;
            return used;
        }
        double used = Math.min(eu, energy);
        energy -= used;
        setChanged();
        return used;
    }

    @Override
    public int availableStoredEnergy() {
        return kind.storage() ? (int) energy : 0;
    }

    @Override
    public boolean exchangeStoredEnergy(int delta, boolean simulate) {
        if (!kind.storage() || energy + delta < 0 || energy + delta > capacity()) return false;
        if (!simulate && delta != 0) {
            energy += delta;
            setChanged();
        }
        return true;
    }

    @Override
    public double receive(double eu) {
        if ((kind.lowTier() == 0 && !kind.storage() && !kind.poweredPlayerMachine())
                || !Double.isFinite(eu)
                || eu <= 0) return 0;
        double available =
                kind == PowerKind.ENERGY_O_MAT ? Math.min(capacity(), credit) : capacity();
        double accepted = Math.min(eu, Math.max(0, available - energy));
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
        WorldEnergy.disconnect(this);
        linked = false;
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        WorldEnergy.disconnect(this);
        linked = false;
        lastTick = Long.MIN_VALUE;
        super.onChunkUnloaded();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ic2." + kind.path);
    }

    @Override
    public BlockEntity accessController() {
        if (!(getLevel() instanceof ServerLevel level)) return null;
        return switch (kind) {
            case REACTOR_ACCESS_HATCH, REACTOR_FLUID_PORT -> ReactorVessel.core(this, level);
            case REACTOR_CHAMBER -> controller(level);
            default -> null;
        };
    }

    @Override
    public void visitAccessPorts(ServerLevel level, Consumer<BlockEntity> visitor) {
        if (kind != PowerKind.NUCLEAR_REACTOR) return;
        for (var pos :
                BlockPos.betweenClosed(
                        worldPosition.offset(-2, -2, -2), worldPosition.offset(2, 2, 2))) {
            if (level.hasChunkAt(pos)
                    && level.getBlockEntity(pos) instanceof PowerNode port
                    && (port.kind == PowerKind.REACTOR_ACCESS_HATCH
                            || port.kind == PowerKind.REACTOR_FLUID_PORT
                            || port.kind == PowerKind.REACTOR_CHAMBER)) visitor.accept(port);
        }
    }

    @Override
    public boolean exchangeHeldFluid(Player player, InteractionHand hand, Direction side) {
        return (kind == PowerKind.GEO_GENERATOR || kind == PowerKind.REACTOR_FLUID_PORT)
                && PowerPlatform.handFluid(this, player, hand);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory player, Player opener) {
        if (kind.trader() && owner == null) {
            owner = opener.getUUID();
            setChanged();
        }
        return new PowerMenu(id, player, inventory);
    }
}
