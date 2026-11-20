package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.content.PowerGrid;
import com.industrialage.ia2.content.ProcessingMachine;
import com.industrialage.ia2.content.ProcessingMachine.Recipe;
import com.industrialage.ia2.energy.graph.EnergyGraph.Ports;
import com.industrialage.ia2.energy.graph.PacketBudget;
import com.industrialage.ia2.gameplay.MachineKind.SlotRole;
import com.industrialage.ia2.industrial.WorkshopNode;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import com.industrialage.ia2.logistics.TransportPlatform;
import com.industrialage.ia2.machine.BlockEntityBase;
import com.industrialage.ia2.machine.FuelGenerator;
import com.industrialage.ia2.platform.NeoForgeVersion;
import com.industrialage.ia2.power.WorldPowerPort;
import com.industrialage.ia2.power.WorldPowerStorage;
import com.industrialage.ia2.world.MachineAutomation;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Server-owned machine inventory, ticking logic, and EU port. */
public final class IndustrialMachine extends NativeGameplay.InventoryEntity
        implements WorldlyContainer, MenuProvider, WorldPowerPort, WorldPowerStorage {
    private static final int IRON_FURNACE_DURATION = 160;
    private final MachineKind kind;
    private final NonNullList<ItemStack> items;
    private final int[] topSlots;
    private final int[] otherSlots;
    private final FuelGenerator generator;
    private final ProcessingMachine processor;
    private final PowerGrid.Storage storage;
    private final BlockEntityBase components;
    private double stored;
    private int fuelRemaining;
    private int furnaceProgress;
    private String furnaceRecipe = "";
    private int redstoneMode;
    private int lastTick = Integer.MIN_VALUE;

    IndustrialMachine(BlockPos pos, BlockState state) {
        super(IndustrialContent.MACHINE.get(), pos, state);
        kind = ((MachineBlock) state.getBlock()).kind;
        items = NonNullList.withSize(kind.slots.size(), ItemStack.EMPTY);
        topSlots = slots(true);
        otherSlots = slots(false);
        generator =
                kind == MachineKind.GENERATOR
                        ? new FuelGenerator(
                                EnergyNetwork.generatorRate(), this::burnTime, this::consumeFuel)
                        : null;
        processor = kind.electricProcessor() ? new ProcessingMachine(kind.path) : null;
        storage = kind.storage() ? PowerGrid.storage(kind.storagePath()) : null;
        components =
                new BlockEntityBase(
                        new BlockEntityBase.Component() {
                            @Override
                            public void onLoad() {
                                EnergyNetwork.load(
                                        (ServerLevel) getLevel(), IndustrialMachine.this);
                            }

                            @Override
                            public boolean serverTick() {
                                return work((ServerLevel) getLevel());
                            }

                            @Override
                            public void onUnload() {
                                EnergyNetwork.unload(
                                        (ServerLevel) getLevel(), IndustrialMachine.this);
                                if (generator != null) {
                                    generator.onUnload();
                                }
                            }
                        });
    }

    MachineKind kind() {
        return kind;
    }

    @Override
    protected BlockEntityBase machineComponents() {
        return components;
    }

    @Override
    NonNullList<ItemStack> persistentItems() {
        return items;
    }

    @Override
    protected MachineSave savedState() {
        return new MachineSave(
                energy(),
                fuelRemaining(),
                progress(),
                processor == null ? furnaceRecipe : processor.recipeKey(),
                redstoneMode,
                processor == null ? 0 : processor.duration());
    }

    @Override
    protected void restoreState(MachineSave snapshot) {
        if (generator != null) {
            generator.restore(
                    new FuelGenerator.Snapshot(
                            snapshot.fuelTicks(),
                            Math.min(snapshot.energy(), FuelGenerator.CAPACITY)));
        } else if (processor != null) {
            refreshUpgrades();
            processor.restore(
                    snapshot.energy(), snapshot.progress(), snapshot.recipe(), snapshot.duration());
        } else if (storage != null) {
            stored = Math.min(snapshot.energy(), storage.capacity());
            redstoneMode = snapshot.redstoneMode();
        } else {
            furnaceProgress = Math.min(snapshot.progress(), IRON_FURNACE_DURATION - 1);
            fuelRemaining = snapshot.fuelTicks();
            furnaceRecipe = snapshot.recipe();
        }
    }

    private boolean work(ServerLevel level) {
        lastTick = level.getServer().getTickCount();
        int oldSignal = redstoneOutput();
        boolean changed = refreshUpgrades();
        changed |= transferBattery();
        boolean active = false;
        if (generator != null) {
            changed |= generator.serverTick();
            active = generator.isActive();
        } else if (processor != null) {
            changed |= processElectric(level);
            changed |=
                    MachineAutomation.transfer(
                            level,
                            worldPosition,
                            this,
                            kind.slots.indexOf(SlotRole.INPUT),
                            kind.slots.indexOf(SlotRole.OUTPUT),
                            upgrades(),
                            stack -> result(level, stack) != null);
            active = processor.active();
        } else if (kind == MachineKind.IRON_FURNACE) {
            changed |= processFuel(level);
            active = fuelRemaining > 0;
        } else if (kind.chargingPad()) {
            active = chargeStandingPlayers(level);
            changed |= active;
        }
        if (!isRemoved() && getBlockState().getValue(MachineBlock.ACTIVE) != active) {
            level.setBlock(
                    worldPosition,
                    getBlockState().setValue(MachineBlock.ACTIVE, active),
                    Block.UPDATE_CLIENTS);
        }
        updateSignal(oldSignal);
        return changed;
    }

    private boolean processElectric(ServerLevel level) {
        var input = item(SlotRole.INPUT);
        var pump =
                input.isEmpty() && kind == MachineKind.COMPRESSOR
                        ? WorkshopNode.connectedWaterPump(level, getBlockPos())
                        : null;
        var output =
                pump == null
                        ? result(level, input)
                        : new Output(
                                new Recipe("ic2:pumped_water", 1, "minecraft:snowball", 1),
                                NativeGameplay.stack("minecraft:snowball", 1));
        double energyBefore = processor.energy();
        int progressBefore = processor.progress();
        var recipeBefore = processor.recipeKey();
        int room = output == null ? 0 : outputSpace(output.stack());
        if (processor.tick(
                output == null ? null : output.recipe(),
                pump == null ? input.getCount() : 1,
                room)) {
            if (pump != null) {
                pump.consumeCompressionWater();
            }
            finish(output, input, processor.completedOperations());
            return true;
        }
        return energyBefore != processor.energy()
                || progressBefore != processor.progress()
                || !recipeBefore.equals(processor.recipeKey());
    }

    private boolean processFuel(ServerLevel level) {
        var input = item(SlotRole.INPUT);
        var output = result(level, input);
        int previousProgress = furnaceProgress;
        boolean changed = fuelRemaining > 0;
        if (fuelRemaining > 0) {
            fuelRemaining--;
        }
        if (output == null || outputSpace(output.stack()) < output.recipe().outputCount()) {
            furnaceProgress = 0;
            furnaceRecipe = "";
            return changed || previousProgress != 0;
        }
        if (!furnaceRecipe.equals(output.recipe().input())) {
            furnaceRecipe = output.recipe().input();
            furnaceProgress = 0;
        }
        if (fuelRemaining == 0) {
            int time = burnTime();
            if (time > 0) {
                consumeFuel();
                fuelRemaining = time;
            }
        }
        if (fuelRemaining > 0) {
            if (++furnaceProgress >= IRON_FURNACE_DURATION) {
                finish(output, input);
                furnaceProgress = 0;
            }
            return true;
        }
        return changed || previousProgress != furnaceProgress;
    }

    private Output result(ServerLevel level, ItemStack input) {
        if (input.isEmpty()) {
            return null;
        }
        if (kind == MachineKind.COMPRESSOR && TransportPlatform.cellContainsFluid(input)) {
            return null;
        }
        String id = NativeGameplay.itemId(input);
        if (kind == MachineKind.IRON_FURNACE || kind == MachineKind.ELECTRIC_FURNACE) {
            ItemStack output = NativeGameplay.smeltingResult(level, input);
            if (!output.isEmpty()) {
                return new Output(
                        new Recipe(id, 1, NativeGameplay.itemId(output), output.getCount()),
                        output);
            }
        }
        Recipe recipe = ProcessingMachine.recipe(kind.path, id);
        if (recipe == null) {
            return null;
        }
        var output = NativeGameplay.stack(recipe.output(), recipe.outputCount());
        return output.isEmpty() ? null : new Output(recipe, output);
    }

    private int outputSpace(ItemStack incoming) {
        var output = item(SlotRole.OUTPUT);
        if (!output.isEmpty() && !ItemStack.isSameItemSameComponents(output, incoming)) {
            return 0;
        }
        return Math.min(incoming.getMaxStackSize(), getMaxStackSize()) - output.getCount();
    }

    private void finish(Output output, ItemStack input) {
        finish(output, input, 1);
    }

    private void finish(Output output, ItemStack input, int operations) {
        if (!input.isEmpty()) {
            input.shrink(output.recipe().inputCount() * operations);
        }
        int slot = kind.slots.indexOf(SlotRole.OUTPUT);
        if (items.get(slot).isEmpty()) {
            items.set(
                    slot, output.stack().copyWithCount(output.recipe().outputCount() * operations));
        } else {
            items.get(slot).grow(output.recipe().outputCount() * operations);
        }
    }

    private int burnTime() {
        return fuelValue(item(SlotRole.FUEL));
    }

    private int fuelValue(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.LAVA_BUCKET) && generator != null) {
            return 0;
        }
        int fuelCan = WorkshopPlatform.classicFuelValue(stack);
        return fuelCan > 0 ? fuelCan : NeoForgeVersion.furnaceBurnTime(stack, getLevel());
    }

    private void consumeFuel() {
        int slot = kind.slots.indexOf(SlotRole.FUEL);
        ItemStack fuel = items.get(slot);
        ItemStack remainder = WorkshopPlatform.classicFuelRemainder(fuel);
        if (remainder.isEmpty()) {
            remainder = NativeGameplay.craftingRemainder(fuel);
        }
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            items.set(slot, remainder);
        } else if (!remainder.isEmpty()) {
            Containers.dropItemStack(
                    getLevel(),
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    remainder);
        }
    }

    private boolean transferBattery() {
        var discharge = item(SlotRole.DISCHARGE);
        int tier = itemTier();
        boolean changed =
                receive(PortableGear.discharge(discharge, capacity() - energy(), tier)) > 0;
        if (processor != null && discharge.is(Items.REDSTONE) && capacity() - energy() >= 800) {
            receive(800);
            discharge.shrink(1);
            changed = true;
        }
        double accepted = PortableGear.charge(item(SlotRole.CHARGE), energy(), tier);
        if (accepted > 0) {
            changed = true;
            if (generator == null) draw(accepted);
            else generator.extractLocalEnergy(accepted);
        }
        return changed;
    }

    private int itemTier() {
        return storage != null ? storage.tier() : processor == null ? 1 : processor.stats().tier();
    }

    private java.util.List<ItemStack> upgrades() {
        return items.subList(3, items.size());
    }

    private boolean refreshUpgrades() {
        return processor != null && processor.setUpgrades(MachineAutomation.installed(upgrades()));
    }

    private boolean chargeStandingPlayers(ServerLevel level) {
        if (energy() <= 0) return false;
        var feet = worldPosition.above();
        var players =
                level.getEntitiesOfClass(
                        ServerPlayer.class,
                        new AABB(feet),
                        player -> player.onGround() && player.blockPosition().equals(feet));
        boolean worked = false;
        for (var player : players) {
            var equipment =
                    java.util.List.of(
                            player.getItemBySlot(EquipmentSlot.CHEST),
                            player.getMainHandItem(),
                            player.getOffhandItem());
            for (var stack : equipment) {
                double accepted = PortableGear.charge(stack, energy(), itemTier());
                worked |= accepted > 0 && draw(accepted) > 0;
            }
        }
        return worked;
    }

    /** Buffered EU, including fractional EU remaining after cable losses. */
    public double energy() {
        if (generator != null) {
            return generator.snapshot().storedEnergy();
        }
        return processor == null ? stored : processor.energy();
    }

    /** Maximum stored EU of this machine. */
    public double capacity() {
        if (generator != null) {
            return FuelGenerator.CAPACITY;
        }
        if (processor != null) {
            return processor.capacity();
        }
        return storage == null ? 0 : storage.capacity();
    }

    /** Completed work ticks on the in-flight recipe. */
    public int progress() {
        return processor == null ? furnaceProgress : processor.progress();
    }

    int duration() {
        if (processor != null) {
            return processor.duration();
        }
        return kind == MachineKind.IRON_FURNACE ? IRON_FURNACE_DURATION : 0;
    }

    /** Remaining working fuel time, in server ticks. */
    public int fuelRemaining() {
        return generator == null ? fuelRemaining : generator.snapshot().fuelTicks();
    }

    @Override
    public Ports energyPorts() {
        return EnergyNetwork.ports(this);
    }

    @Override
    public boolean ranThisTick(ServerLevel level) {
        return lastTick == level.getServer().getTickCount() && !isRemoved();
    }

    @Override
    public boolean source() {
        return generator != null || storage != null;
    }

    @Override
    public double packetLimit() {
        return PacketBudget.packetSize(itemTier());
    }

    @Override
    public double offer() {
        if (generator != null) {
            return Math.min(energy(), FuelGenerator.OUTPUT_PACKET_LIMIT);
        }
        if (storage != null && allowsRedstoneOutput()) {
            return storage.offer(stored);
        }
        return 0;
    }

    @Override
    public int availableStoredEnergy() {
        return storage == null ? 0 : (int) stored;
    }

    @Override
    public boolean exchangeStoredEnergy(int delta, boolean simulate) {
        if (storage == null || stored + delta < 0 || stored + delta > storage.capacity()) {
            return false;
        }
        if (!simulate && delta != 0) {
            stored += delta;
            setChanged();
        }
        return true;
    }

    @Override
    public double draw(double requested) {
        int oldSignal = redstoneOutput();
        double acted;
        if (generator != null) {
            acted = generator.extractEnergy(requested);
        } else {
            acted = Math.min(requested, stored);
            stored -= acted;
        }
        if (acted > 0) {
            setChanged();
            updateSignal(oldSignal);
        }
        return acted;
    }

    @Override
    public double receive(double requested) {
        int oldSignal = redstoneOutput();
        double accepted = 0;
        if (processor != null) {
            accepted = processor.receive(requested);
        } else if (storage != null) {
            accepted = Math.min(requested, storage.capacity() - stored);
            stored += accepted;
        }
        if (accepted > 0) {
            setChanged();
            updateSignal(oldSignal);
        }
        return accepted;
    }

    int comparatorOutput() {
        return storage == null ? 0 : (int) Math.floor(15 * stored / storage.capacity());
    }

    int redstoneOutput() {
        if (storage == null) {
            return 0;
        }
        double packet = PacketBudget.packetSize(storage.tier());
        return switch (redstoneMode) {
            case 1 -> stored >= storage.capacity() - 20 * packet ? 15 : 0;
            case 2 -> stored > packet && stored < storage.capacity() - packet ? 15 : 0;
            case 3 -> stored < storage.capacity() - packet ? 15 : 0;
            case 4 -> stored < packet ? 15 : 0;
            default -> 0;
        };
    }

    private boolean allowsRedstoneOutput() {
        if (redstoneMode < 5 || !(getLevel() instanceof ServerLevel level)) {
            return true;
        }
        return !level.hasNeighborSignal(worldPosition)
                || redstoneMode == 6
                        && stored
                                > storage.capacity() - 20 * PacketBudget.packetSize(storage.tier());
    }

    void cycleRedstoneMode(ServerPlayer player) {
        int oldSignal = redstoneOutput();
        redstoneMode = (redstoneMode + 1) % 7;
        setChanged();
        updateSignal(oldSignal);
        NativeGameplay.overlay(
                player,
                Component.translatable(
                        "container.ic2.redstone_mode",
                        Component.translatable("container.ic2.redstone_mode." + redstoneMode)));
    }

    private void updateSignal(int previous) {
        if (getLevel() instanceof ServerLevel level && previous != redstoneOutput()) {
            NativeGameplay.notifySignal(level, worldPosition, getBlockState().getBlock());
        }
    }

    private ItemStack item(SlotRole role) {
        int slot = kind.slots.indexOf(role);
        return slot < 0 ? ItemStack.EMPTY : getItem(slot);
    }

    private int[] slots(boolean inputFirst) {
        return IntStream.of(
                        kind.slots.indexOf(inputFirst ? SlotRole.INPUT : SlotRole.OUTPUT),
                        kind.slots.indexOf(inputFirst ? SlotRole.OUTPUT : SlotRole.INPUT),
                        kind.slots.indexOf(SlotRole.FUEL))
                .filter(slot -> slot >= 0)
                .toArray();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? topSlots : otherSlots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        var role = kind.slots.get(slot);
        return (role == SlotRole.INPUT || role == SlotRole.FUEL) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return switch (kind.slots.get(slot)) {
            case OUTPUT -> true;
            case INPUT, FUEL -> !canPlaceItem(slot, stack);
            case CHARGE, DISCHARGE, UPGRADE -> false;
        };
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (kind.slots.get(slot)) {
            case INPUT -> getLevel() instanceof ServerLevel level && result(level, stack) != null;
            case FUEL -> fuelValue(stack) >= (generator == null ? 1 : 4);
            case CHARGE -> PortableGear.acceptsCharge(stack, itemTier());
            case DISCHARGE ->
                    PortableGear.canDischarge(stack, itemTier())
                            || processor != null && stack.is(Items.REDSTONE);
            case OUTPUT -> false;
            case UPGRADE -> MachineAutomation.isProcessingUpgrade(stack);
        };
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack removed = ContainerHelper.removeItem(items, index, count);
        if (!removed.isEmpty()) {
            if (index >= 3) refreshUpgrades();
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ContainerHelper.takeItem(items, index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        items.set(index, stack);
        stack.limitSize(getMaxStackSize(stack));
        if (index >= 3) refreshUpgrades();
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public MachineMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineMenu(id, inventory, this);
    }

    private record Output(Recipe recipe, ItemStack stack) {}
}
