package com.industrialage.ia2.industrial;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.content.PortableEquipment;
import com.industrialage.ia2.equipment.JetpackMount;
import com.industrialage.ia2.reactor.ElectricReactor;
import java.util.Set;
import java.util.stream.IntStream;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The slots shown to a player are the same server-owned stacks used by automation and the reactor.
 */
final class PowerInventory extends SimpleContainer
        implements WorldlyContainer, ElectricReactor.Grid {
    private static final Set<String> REACTOR_FEED =
            Set.of(
                    "uranium_fuel_rod",
                    "dual_uranium_fuel_rod",
                    "quad_uranium_fuel_rod",
                    "mox_fuel_rod",
                    "dual_mox_fuel_rod",
                    "quad_mox_fuel_rod",
                    "depleted_isotope_fuel_rod",
                    "lithium_fuel_rod");
    private static final Set<String> REACTOR_PRODUCTS =
            Set.of(
                    "depleted_uranium_fuel_rod",
                    "depleted_dual_uranium_fuel_rod",
                    "depleted_quad_uranium_fuel_rod",
                    "depleted_mox_fuel_rod",
                    "depleted_dual_mox_fuel_rod",
                    "depleted_quad_mox_fuel_rod",
                    "re_enriched_uranium");

    final PowerNode device;

    PowerInventory(PowerNode node) {
        super(size(node.kind));
        device = node;
    }

    static int size(PowerKind kind) {
        if (kind == PowerKind.NUCLEAR_REACTOR) return 58;
        return kind == PowerKind.RCI_LZH || kind == PowerKind.RCI_RSH ? 14 : 9;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        String part = PowerPlatform.path(stack);
        if (slot == device.kind.chargeSlot())
            return PowerPlatform.acceptsCharge(stack, device.kind.itemTier());
        if (device.kind.storage() && slot == 1)
            return PowerPlatform.canDischarge(stack, device.kind.itemTier());
        if (device.kind == PowerKind.NUCLEAR_REACTOR && slot >= 54) {
            if (!device.fluidMode) return false;
            if (slot == 56) return WorkshopPlatform.canSupplyContainer(stack, true);
            if (slot != 54) return false;
            return WorkshopCells.canDrain(stack, ReactorVessel.TANK_CAPACITY, "ic2:coolant");
        }
        int upgrades =
                device.kind == PowerKind.MAGNETIZER
                        ? 2
                        : device.kind == PowerKind.RCI_RSH || device.kind == PowerKind.RCI_LZH
                                ? 10
                                : -1;
        if (upgrades >= 0) {
            if (slot == upgrades - 1)
                return PowerPlatform.canDischarge(stack, device.upgradeTier());
            if (slot >= upgrades && slot < upgrades + 4)
                return part.equals("energy_storage_upgrade")
                        || part.equals("transformer_upgrade")
                        || upgrades == 10
                                && (part.equals("pulling_upgrade")
                                        || part.equals("advanced_pulling_upgrade"));
        }
        return switch (device.kind) {
            case NUCLEAR_REACTOR ->
                    ElectricReactor.exposed(slot, device.chambers)
                            && ElectricReactor.accepts(part)
                            && (!device.fluidMode
                                    || !ElectricReactor.heatStore(part)
                                    || PowerPlatform.use(stack) == 0);
            case NUKE ->
                    slot == 0 && GameCharges.cartridge(stack)
                            || slot == 1 && GameCharges.primingItem(stack);
            case RCI_RSH -> slot < 9 && stack.is(Items.REDSTONE_BLOCK);
            case RCI_LZH -> slot < 9 && stack.is(Items.LAPIS_BLOCK);
            case TRADE_O_MAT -> slot >= 0 && slot <= 2;
            case ENERGY_O_MAT ->
                    slot == 0
                            || slot == 2
                            || slot == 3 && PowerPlatform.acceptsCharge(stack, 4)
                            || slot == 4 && PowerPlatform.canDischarge(stack, 4);
            case REACTOR_FLUID_PORT ->
                    slot == 0
                            && (part.equals("fluid_pulling_upgrade")
                                    || part.equals("fluid_ejector_upgrade"));
            case RT_GENERATOR -> slot < 6 && part.equals("rtg_pellet");
            case GEO_GENERATOR -> acceptsGeothermalFuel(slot, stack);
            case WATER_GENERATOR ->
                    slot == 0 && WorkshopCells.measure(stack, "minecraft:water") != null;
            default -> false;
        };
    }

    static boolean acceptsGeothermalFuel(int slot, ItemStack stack) {
        return slot == 0 && WorkshopCells.canDrain(stack, PowerNode.LAVA_CAPACITY, PowerNode.LAVA);
    }

    @Override
    public int[] getSlotsForFace(Direction face) {
        return IntStream.range(0, getContainerSize()).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        return MachineAccess.allowsInput(device, face) && acceptsAutomation(slot, stack);
    }

    boolean acceptsAutomation(int slot, ItemStack stack) {
        if (slot < 0 || slot >= getContainerSize() || stack.isEmpty() || !canPlaceItem(slot, stack))
            return false;
        if (slot == device.kind.chargeSlot()) return needsCharge(stack);
        if (slot == dischargeSlot()) return PowerPlatform.storedEnergy(stack) > 0;
        return switch (device.kind) {
            case GEO_GENERATOR, WATER_GENERATOR, NUKE -> true;
            case NUCLEAR_REACTOR ->
                    slot >= 54
                            || REACTOR_FEED.contains(PowerPlatform.path(stack))
                                    && !reactorProduct(stack);
            case RCI_RSH, RCI_LZH -> slot < 9;
            case TRADE_O_MAT, ENERGY_O_MAT ->
                    slot == 2 && WorkshopPlatform.sameItem(getItem(0), stack);
            default -> false;
        };
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        if (slot < 0
                || slot >= getContainerSize()
                || stack.isEmpty()
                || !MachineAccess.allowsOutput(device, face)) return false;
        if (slot == device.kind.chargeSlot()) return chargeComplete(stack);
        return switch (device.kind) {
            case GEO_GENERATOR, WATER_GENERATOR -> slot == 1;
            case TRADE_O_MAT -> slot == 3;
            case NUCLEAR_REACTOR ->
                    device.fluidMode && (slot == 55 || slot == 57)
                            || ElectricReactor.exposed(slot, device.chambers)
                                    && reactorProduct(stack);
            default -> false;
        };
    }

    private static boolean reactorProduct(ItemStack stack) {
        String part = PowerPlatform.path(stack);
        return REACTOR_PRODUCTS.contains(part)
                || part.equals("lithium_fuel_rod") && PowerPlatform.use(stack) >= 9_999;
    }

    int automationLimit(int slot, ItemStack stack) {
        if (slot == device.kind.chargeSlot() || slot == dischargeSlot())
            return PowerPlatform.powerSlotLimit(stack);
        return limit(slot, stack);
    }

    int dischargeSlot() {
        return switch (device.kind) {
            case CLASSIC_MFE, CLASSIC_MFSU, MAGNETIZER -> 1;
            case RCI_RSH, RCI_LZH -> 9;
            case ENERGY_O_MAT -> 4;
            default -> -1;
        };
    }

    private boolean needsCharge(ItemStack stack) {
        var sample = stack.copy();
        sample.setCount(1);
        return PowerPlatform.charge(sample, 1, device.kind.itemTier()) > 0;
    }

    private boolean chargeComplete(ItemStack stack) {
        if (stack.getCount() != 1 || !PowerPlatform.acceptsCharge(stack, device.kind.itemTier()))
            return false;
        return PowerPlatform.storedEnergy(stack) >= PowerPlatform.equipmentCapacity(stack)
                && (!JetpackMount.installed(stack)
                        || JetpackMount.energy(stack)
                                >= PortableEquipment.ELECTRIC_JETPACK.capacity);
    }

    @Override
    public int getMaxStackSize() {
        return device.kind == PowerKind.RT_GENERATOR ? 1 : 64;
    }

    int limit(ItemStack stack) {
        return limit(0, stack);
    }

    int limit(int slot, ItemStack stack) {
        return device.kind == PowerKind.NUCLEAR_REACTOR
                        && slot < 54
                        && !PowerPlatform.path(stack).equals("heatpack")
                ? 1
                : getMaxStackSize();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty() && stack.getCount() > limit(slot, stack)) {
            ItemStack limited = stack.copy();
            limited.setCount(limit(slot, stack));
            super.setItem(slot, limited);
        } else super.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return !device.isRemoved()
                && player.distanceToSqr(
                                device.getBlockPos().getX() + 0.5,
                                device.getBlockPos().getY() + 0.5,
                                device.getBlockPos().getZ() + 0.5)
                        <= 64;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (device != null) device.setChanged();
    }

    @Override
    public String part(int slot) {
        return PowerPlatform.path(getItem(slot));
    }

    @Override
    public int count(int slot) {
        return getItem(slot).getCount();
    }

    @Override
    public int wear(int slot) {
        return PowerPlatform.use(getItem(slot));
    }

    @Override
    public void wear(int slot, int wear) {
        if (!getItem(slot).isEmpty() && wear != wear(slot))
            PowerPlatform.setUse(getItem(slot), wear);
    }

    @Override
    public void replace(int slot, String id) {
        setItem(slot, id.isEmpty() ? ItemStack.EMPTY : PowerPlatform.item(id));
    }
}
