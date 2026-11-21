package com.industrialage.ia2.industrial;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Workshop slots and synchronized quantities, using separate halves for values that exceed a short.
 */
public final class WorkshopMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = WorkshopInventory.SIZE;
    private static final int DATA_SIZE = Metric.values().length * 2;

    private final WorkshopNode machine;
    private final WorkshopKind kind;
    private final WorkshopLayout layout;
    private final Container inventory;
    private final ContainerData data;

    WorkshopMenu(int id, Inventory player, WorkshopNode machine) {
        this(id, player, machine.kind, machine.inventory, machine, live(machine));
    }

    /** Creates the client inventory using the server's registered machine kind. */
    public static WorkshopMenu fromNetwork(int id, Inventory player, FriendlyByteBuf buffer) {
        var kind = buffer.readEnum(WorkshopKind.class);
        return new WorkshopMenu(
                id,
                player,
                kind,
                new SimpleContainer(MACHINE_SLOTS),
                null,
                new SimpleContainerData(DATA_SIZE));
    }

    private WorkshopMenu(
            int id,
            Inventory player,
            WorkshopKind kind,
            Container inventory,
            WorkshopNode machine,
            ContainerData data) {
        super(WorkshopPlatform.menuType(), id);
        this.kind = kind;
        this.layout = WorkshopLayout.of(kind);
        this.inventory = inventory;
        this.machine = machine;
        this.data = data;
        for (int index = 0; index < MACHINE_SLOTS; index++) {
            int slot = index;
            var point = layout.slot(index);
            addSlot(
                    new Slot(inventory, index, point.x(), point.y()) {
                        @Override
                        public boolean isActive() {
                            return layout.shows(slot) && role(slot) != Role.HIDDEN;
                        }

                        @Override
                        public boolean mayPlace(ItemStack item) {
                            var purpose = role(slot);
                            return layout.shows(slot)
                                    && purpose.insertable
                                    && (machine == null
                                            ? acceptsClientInput(slot, item)
                                            : machine.inventory.canPlaceItem(slot, item));
                        }

                        @Override
                        public boolean mayPickup(Player player) {
                            var purpose = role(slot);
                            return layout.shows(slot)
                                    && purpose != Role.HIDDEN
                                    && purpose != Role.PATTERN;
                        }

                        @Override
                        public int getMaxStackSize() {
                            return WorkshopInventory.componentBay(kind, slot)
                                            || slot == WorkshopInventory.POWER_ITEM_SLOT
                                            || role(slot) == Role.TEMPLATE
                                            || kind == WorkshopKind.RADIOISOTOPE_HEAT && slot < 6
                                    ? 1
                                    : super.getMaxStackSize();
                        }
                    });
        }
        var playerSlots = layout.player;
        int playerX = playerSlots == null ? 0 : playerSlots.x();
        int playerY = playerSlots == null ? 0 : playerSlots.rows();
        int hotbarY = playerSlots == null ? 0 : playerSlots.hotbar();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addPlayerSlot(
                        player, 9 + row * 9 + column, playerX + 18 * column, playerY + 18 * row);
            }
        }
        for (int column = 0; column < 9; column++) {
            addPlayerSlot(player, column, playerX + 18 * column, hotbarY);
        }
        addDataSlots(data);
    }

    private void addPlayerSlot(Inventory inventory, int index, int x, int y) {
        addSlot(
                new Slot(inventory, index, x, y) {
                    @Override
                    public boolean isActive() {
                        return layout.player != null;
                    }
                });
    }

    private boolean acceptsClientInput(int slot, ItemStack item) {
        if (slot == WorkshopInventory.POWER_ITEM_SLOT) {
            return WorkshopInventory.acceptsPowerItem(kind, item, get(Metric.TIER));
        }
        if (kind == WorkshopKind.ORE_WASHER) {
            if (slot == WorkshopInventory.WASHER_WATER_SLOT)
                return WorkshopInventory.acceptsWasherWater(item);
            if (slot < kind.inputs(mode())) return WorkshopPlans.storedInput(kind, mode(), item);
        }
        if (kind == WorkshopKind.BLAST_FURNACE) {
            if (slot == WorkshopInventory.AUX_SLOT) return WorkshopInventory.acceptsBlastAir(item);
            if (slot < kind.inputs(mode())) return WorkshopPlans.storedInput(kind, mode(), item);
        }
        if (kind == WorkshopKind.FLUID_BOTTLER && slot < 2) {
            return WorkshopInventory.acceptsBottlerInput(slot, item);
        }
        if ((kind == WorkshopKind.FLUID_HEAT || kind == WorkshopKind.SEMIFLUID) && slot == 0) {
            return WorkshopInventory.acceptsFluidFuel(kind, item);
        }
        if (kind == WorkshopKind.WIND_KINETIC && slot == 0) {
            return WorkshopKinetic.ROTOR_RATINGS.containsKey(WorkshopPlatform.itemId(item));
        }
        if (kind == WorkshopKind.WATER_KINETIC && slot == 0) {
            return WorkshopKinetic.validWaterRotor(item);
        }
        if (kind == WorkshopKind.ELECTROLYZER
                && slot >= WorkshopInventory.UPGRADE_START
                && slot < WorkshopInventory.UPGRADE_END) {
            return WorkshopAutomation.acceptsElectrolyzerUpgrade(item);
        }
        return slot >= 2
                || (kind != WorkshopKind.CANNER && kind != WorkshopKind.SOLID_CANNER)
                || WorkshopPlans.acceptsCanningInput(kind, mode(), slot, item);
    }

    WorkshopLayout layout() {
        return layout;
    }

    /** Identifies the machine represented by this live native menu. */
    public String device() {
        return kind.path;
    }

    /** The original backdrop selected for this particular machine. */
    public String atlas() {
        return layout.atlas;
    }

    /** Native slot labels include controls and transfer roles specific to each workshop kind. */
    public Component slotLabel(int index) {
        if (index < 0 || index >= MACHINE_SLOTS) {
            return Component.empty();
        }
        return Component.translatable("gui.ic2.workshop.slot." + role(index).key);
    }

    private Role role(int slot) {
        if (slot == WorkshopInventory.POWER_ITEM_SLOT) {
            if (kind.chargesItem()) return Role.CHARGE;
            return kind.dischargesItem() ? Role.DISCHARGE : Role.HIDDEN;
        }
        if (WorkshopInventory.componentBay(kind, slot)) {
            return kind == WorkshopKind.ELECTRIC_HEAT ? Role.COIL : Role.MOTOR;
        }
        if (kind == WorkshopKind.PATTERN_STORAGE) {
            return slot == WorkshopMatter.FIRST_PATTERN + Math.floorMod(get(Metric.CYCLES), 12)
                    ? Role.PATTERN
                    : Role.HIDDEN;
        }
        if (kind == WorkshopKind.UU_SCANNER) {
            boolean recorded = !inventory.getItem(WorkshopMatter.FIRST_PATTERN).isEmpty();
            if (slot == WorkshopMatter.FIRST_PATTERN || slot == 0) {
                return slot == 0
                        ? (recorded ? Role.HIDDEN : Role.INPUT)
                        : (recorded ? Role.PATTERN : Role.HIDDEN);
            }
        }
        if (kind == WorkshopKind.ORE_WASHER) {
            if (slot == WorkshopInventory.WASHER_WATER_SLOT) return Role.DRAIN;
            if (slot == WorkshopInventory.WASHER_CONTAINER_RETURN_SLOT) return Role.VESSEL_RETURN;
        }
        if (kind == WorkshopKind.BLAST_FURNACE && slot == WorkshopInventory.RETURN_SLOT) {
            return Role.RETURN;
        }
        if (slot >= WorkshopInventory.OUTPUT_START
                && slot < WorkshopInventory.OUTPUT_START + kind.outputSlots()) {
            return Role.OUTPUT;
        }
        if (slot >= WorkshopInventory.UPGRADE_START
                && slot < WorkshopInventory.UPGRADE_END
                && WorkshopAutomation.supported(kind)) {
            return Role.UPGRADE;
        }
        if (slot == WorkshopInventory.AUX_SLOT && kind.auxiliarySlot()) {
            return switch (kind) {
                case BLOCK_CUTTER -> Role.BLADE;
                case ELECTRIC_HEAT -> Role.COIL;
                case BLAST_FURNACE -> Role.AIR;
                default -> Role.HIDDEN;
            };
        }
        if (slot >= kind.inputs(get(Metric.MODE))
                && (slot != 1 || inventory.getItem(slot).isEmpty())) {
            return Role.HIDDEN;
        }
        return switch (kind) {
            case PUMP -> Role.FILL;
            case FLUID_BOTTLER -> slot == 0 ? Role.FILL : Role.DRAIN;
            case CANNER ->
                    slot == 1
                            ? Role.INPUT
                            : switch (mode()) {
                                case 1 -> Role.FILL;
                                case 2 -> Role.DRAIN;
                                default -> Role.CONTAINER;
                            };
            case CLASSIC_CANNER, SOLID_CANNER -> slot == 1 ? Role.CONTAINER : Role.INPUT;
            case UU_ASSEMBLY -> slot == 0 ? Role.TEMPLATE : Role.MATTER;
            case MASS_FABRICATOR, CLASSIC_MASS, MATTER_GENERATOR -> Role.SCRAP;
            case RADIOISOTOPE_HEAT -> Role.PELLET;
            case ELECTRIC_KINETIC -> Role.MOTOR;
            case WIND_KINETIC, WATER_KINETIC, STEAM_TURBINE -> Role.ROTOR;
            case SOLID_HEAT, FLUID_HEAT, SEMIFLUID -> Role.FUEL;
            default -> Role.INPUT;
        };
    }

    private static ContainerData live(WorkshopNode machine) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                var metric = Metric.values()[index / 2];
                int value =
                        switch (metric) {
                            case ENERGY -> (int) machine.energy;
                            case CAPACITY -> machine.energyCapacity();
                            case PROGRESS ->
                                    machine.kind == WorkshopKind.ELECTROLYZER
                                                    && machine.cycleDuration == 0
                                            ? machine.progress / 2
                                            : machine.progress;
                            case DURATION ->
                                    machine.cycleDuration > 0
                                            ? machine.cycleDuration
                                            : machine.kind.duration;
                            case HEAT -> machine.heat;
                            case ACTIVE ->
                                    machine.getBlockState().getValue(WorkshopBlock.ACTIVE) ? 1 : 0;
                            case MODE -> machine.mode;
                            case CYCLES -> machine.cycles;
                            case KINETIC -> machine.auxiliaryUnits;
                            case PRODUCT -> WorkshopPlatform.previewItemId(machine.preview);
                            case IN_FLUID -> WorkshopPlatform.previewFluidId(machine.inlet.id);
                            case IN_AMOUNT -> machine.inlet.amount;
                            case OUT_FLUID -> WorkshopPlatform.previewFluidId(machine.outlet.id);
                            case OUT_AMOUNT -> machine.outlet.amount;
                            case BY_FLUID -> WorkshopPlatform.previewFluidId(machine.byproduct.id);
                            case BY_AMOUNT -> machine.byproduct.amount;
                            case TIER -> machine.energyTier();
                        };
                return index % 2 == 0 ? value & 0xffff : value >>> 16;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_SIZE;
            }
        };
    }

    private int get(Metric metric) {
        int index = metric.ordinal() * 2;
        return data.get(index + 1) << 16 | data.get(index) & 0xffff;
    }

    /** Returns the latest EU and its usable capacity after installed upgrades. */
    public int energy() {
        return get(Metric.ENERGY);
    }

    public int capacity() {
        return get(Metric.CAPACITY);
    }

    public int progress() {
        return get(Metric.PROGRESS);
    }

    public int duration() {
        return get(Metric.DURATION);
    }

    public int heat() {
        return get(Metric.HEAT);
    }

    public boolean active() {
        return get(Metric.ACTIVE) != 0;
    }

    int mode() {
        return get(Metric.MODE);
    }

    /** Kinetic KU/t synchronized to this machine menu. */
    public int motion() {
        return get(Metric.KINETIC);
    }

    Component patternName() {
        for (int slot = WorkshopMatter.FIRST_PATTERN;
                slot < WorkshopMatter.FIRST_PATTERN + 12;
                slot++) {
            if (slots.get(slot).isActive() && !inventory.getItem(slot).isEmpty()) {
                return inventory.getItem(slot).getHoverName();
            }
        }
        return Component.translatable("gui.ic2.workshop.empty");
    }

    /** Returns a translated live recipe product or idle state. */
    public Component job() {
        int result = get(Metric.PRODUCT);
        if (result != 0) {
            return Component.translatable(
                    "gui.ic2.workshop.job", WorkshopPlatform.previewItem(result).getHoverName());
        }
        return Component.translatable(
                active() ? "gui.ic2.workshop.running" : "gui.ic2.workshop.waiting");
    }

    /** Number and contents of the visibly distinct tanks in this machine. */
    public TankReadout tank(int row) {
        return switch (row) {
            case 0 ->
                    new TankReadout(
                            kind.singleTank() ? "tank" : "intake",
                            kind.inputVolume(),
                            get(Metric.IN_AMOUNT),
                            WorkshopPlatform.previewFluid(get(Metric.IN_FLUID)));
            case 1 ->
                    new TankReadout(
                            "fluid_out",
                            kind.singleTank() ? 0 : kind.outputVolume(),
                            get(Metric.OUT_AMOUNT),
                            WorkshopPlatform.previewFluid(get(Metric.OUT_FLUID)));
            case 2 ->
                    new TankReadout(
                            "fluid_byproduct",
                            kind.byproductVolume(),
                            get(Metric.BY_AMOUNT),
                            WorkshopPlatform.previewFluid(get(Metric.BY_FLUID)));
            default -> throw new IndexOutOfBoundsException(row);
        };
    }

    int tankFluid(int row) {
        return get(
                switch (row) {
                    case 0 -> Metric.IN_FLUID;
                    case 1 -> Metric.OUT_FLUID;
                    case 2 -> Metric.BY_FLUID;
                    default -> throw new IndexOutOfBoundsException(row);
                });
    }

    /** Visible tank role, maximum volume, actual volume and translated fluid type. */
    public record TankReadout(String role, int maximum, int amount, Component liquid) {}

    /** Whether to show the heat meter for heat-driven or heat-producing blocks. */
    public boolean heatMeter() {
        return kind.heatMeter();
    }

    /** Whether the screen has a server-authoritative control. */
    public boolean hasControl() {
        return kind.modes() > 1
                || kind == WorkshopKind.UU_SCANNER
                || kind == WorkshopKind.PATTERN_STORAGE;
    }

    /** Localized name of the selected mode or the separate graph action. */
    public Component controlLabel() {
        String action =
                switch (kind) {
                    case UU_SCANNER -> "record";
                    case PATTERN_STORAGE -> "next_pattern";
                    default -> kind.path + "." + get(Metric.MODE);
                };
        return Component.translatable("gui.ic2.workshop." + action);
    }

    @Override
    public boolean clickMenuButton(Player player, int action) {
        if (machine == null
                || !stillValid(player)
                || layout.controls.stream().noneMatch(control -> control.action() == action)) {
            return false;
        }
        if (kind == WorkshopKind.REPLICATOR) {
            machine.selectMode(action - 1);
        } else if (kind.modes() > 1) {
            machine.cycleMode();
        } else if (machine.getLevel() instanceof ServerLevel level) {
            WorkshopMatter.recordOrSelect(machine, level, player, action == 1 ? -1 : 1);
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine == null || inventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        var current = source.getItem();
        var old = current.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(current, MACHINE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!insertIntoWorkshop(current)) {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) {
            source.set(ItemStack.EMPTY);
        } else {
            source.setChanged();
        }
        source.onTake(player, current);
        return old;
    }

    private boolean insertIntoWorkshop(ItemStack stack) {
        int powerItem = WorkshopInventory.POWER_ITEM_SLOT;
        if (slots.get(powerItem).mayPlace(stack)
                && moveItemStackTo(stack, powerItem, powerItem + 1, false)) {
            return true;
        }
        if (WorkshopInventory.componentBay(kind, 0)) {
            return moveItemStackTo(stack, 0, WorkshopInventory.COMPONENT_BAYS, false);
        }
        if (kind == WorkshopKind.ORE_WASHER) {
            int tankSlot = WorkshopInventory.WASHER_WATER_SLOT;
            if (slots.get(tankSlot).mayPlace(stack)
                    && moveItemStackTo(stack, tankSlot, tankSlot + 1, false)) {
                return true;
            }
        }

        if (slots.get(WorkshopInventory.UPGRADE_START).mayPlace(stack)
                && moveItemStackTo(
                        stack,
                        WorkshopInventory.UPGRADE_START,
                        WorkshopInventory.UPGRADE_END,
                        false)) {
            return true;
        }
        if (kind.auxiliarySlot()
                && slots.get(WorkshopInventory.AUX_SLOT).mayPlace(stack)
                && moveItemStackTo(
                        stack, WorkshopInventory.AUX_SLOT, WorkshopInventory.AUX_SLOT + 1, false)) {
            return true;
        }
        int inputs = kind.inputs(get(Metric.MODE));
        if (kind == WorkshopKind.CANNER || kind == WorkshopKind.SOLID_CANNER) {
            for (int slot = 0; slot < inputs; slot++) {
                if (slots.get(slot).mayPlace(stack)
                        && moveItemStackTo(stack, slot, slot + 1, false)) return true;
            }
            return false;
        }
        return inputs > 0
                && (kind != WorkshopKind.ORE_WASHER && kind != WorkshopKind.BLAST_FURNACE
                        || slots.get(0).mayPlace(stack))
                && moveItemStackTo(stack, 0, inputs, false);
    }

    private enum Metric {
        ENERGY,
        CAPACITY,
        PROGRESS,
        DURATION,
        HEAT,
        ACTIVE,
        MODE,
        CYCLES,
        KINETIC,
        PRODUCT,
        IN_FLUID,
        IN_AMOUNT,
        OUT_FLUID,
        OUT_AMOUNT,
        BY_FLUID,
        BY_AMOUNT,
        TIER
    }

    private enum Role {
        HIDDEN("hidden", false),
        INPUT("input", true),
        CONTAINER("container", true),
        OUTPUT("output", false),
        FILL("fill", true),
        DRAIN("drain", true),
        CHARGE("charge", true),
        DISCHARGE("discharge", true),
        RETURN("return", false),
        VESSEL_RETURN("container_return", false),
        COIL("coil", true),
        UPGRADE("upgrade", true),
        BLADE("blade", true),
        AIR("air", true),
        SCRAP("scrap", true),
        PELLET("pellet", true),
        MOTOR("motor", true),
        ROTOR("rotor", true),
        FUEL("fuel", true),
        MATTER("matter", true),
        TEMPLATE("template", true),
        PATTERN("pattern", false);

        final String key;
        final boolean insertable;

        Role(String key, boolean insertable) {
            this.key = key;
            this.insertable = insertable;
        }
    }
}
