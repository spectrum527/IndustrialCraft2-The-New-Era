package com.industrialage.ia2.forge.content;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.access.MachinePorts;
import com.industrialage.ia2.access.ProcessingItemPort;
import com.industrialage.ia2.access.ProcessingSlots;
import com.industrialage.ia2.forge.content.MachineKind.Slot;
import com.industrialage.ia2.forge.equipment.ForgeGear;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import com.industrialage.ia2.world.MachineAutomation;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

final class MachineInventory extends SimpleContainer implements ProcessingSlots {
    final MachineKind kind;
    private final BlockEntity owner;
    private final int[] visible;
    private final int[] bottom;
    private final Map<Direction, LazyOptional<IItemHandler>> handlers =
            new EnumMap<>(Direction.class);
    private LazyOptional<IItemHandler> unsided = LazyOptional.empty();
    private ItemStack completedFuel = ItemStack.EMPTY;

    MachineInventory(MachineKind kind, BlockEntity owner) {
        super(kind.slots.size());
        this.kind = kind;
        this.owner = owner;
        visible =
                IntStream.of(
                                index(Slot.INPUT),
                                index(Slot.FUEL),
                                index(Slot.OUTPUT),
                                index(Slot.CHARGE),
                                index(Slot.DISCHARGE))
                        .filter(slot -> slot >= 0)
                        .toArray();
        int output = index(Slot.OUTPUT);
        bottom =
                output < 0
                        ? visible
                        : IntStream.concat(
                                        IntStream.of(output),
                                        IntStream.of(visible).filter(slot -> slot != output))
                                .toArray();
        revive();
    }

    private int index(Slot slot) {
        return kind.slots.indexOf(slot);
    }

    ItemStack stack(Slot slot) {
        return getItem(index(slot));
    }

    void set(Slot slot, ItemStack stack) {
        setItem(index(slot), stack);
    }

    void consumeFuel() {
        completedFuel = ItemStack.EMPTY;
        ItemStack fuel = stack(Slot.FUEL);
        ItemStack remainder = WorkshopPlatform.classicFuelRemainder(fuel);
        if (remainder.isEmpty()) {
            remainder = fuel.getCraftingRemainingItem();
        }
        removeItem(index(Slot.FUEL), 1);
        if (remainder.isEmpty()) {
            return;
        }
        if (stack(Slot.FUEL).isEmpty()) {
            set(Slot.FUEL, remainder);
            completedFuel = remainder.copy();
        } else if (owner != null && owner.getLevel() != null) {
            net.minecraft.world.level.block.Block.popResource(
                    owner.getLevel(), owner.getBlockPos(), remainder);
        }
    }

    static int burnTime(ItemStack stack) {
        int fuelCan = WorkshopPlatform.classicFuelValue(stack);
        return fuelCan > 0
                ? fuelCan
                : stack.isEmpty() ? 0 : ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
    }

    void load(CompoundTag tag) {
        var items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int index = 0; index < items.size(); index++) {
            setItem(index, items.get(index));
        }
        completedFuel = ItemStack.of(tag.getCompound("ic2_fuel_return"));
        if (completedFuel.isEmpty() && index(Slot.FUEL) >= 0) {
            var fuel = stack(Slot.FUEL);
            if ((kind == MachineKind.IRON_FURNACE && fuel.is(Items.BUCKET))
                    || "ic2:empty_fuel_can"
                            .equals(
                                    String.valueOf(
                                            net.minecraftforge.registries.ForgeRegistries.ITEMS
                                                    .getKey(fuel.getItem())))) {
                completedFuel = fuel.copy();
            }
        }
    }

    void save(CompoundTag tag) {
        var items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        for (int index = 0; index < items.size(); index++) {
            items.set(index, getItem(index));
        }
        ContainerHelper.saveAllItems(tag, items);
        if (index(Slot.FUEL) >= 0 && isFuelProduct(stack(Slot.FUEL))) {
            tag.put("ic2_fuel_return", completedFuel.save(new CompoundTag()));
        }
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return switch (kind.slots.get(index)) {
            case OUTPUT -> false;
            case FUEL ->
                    (kind != MachineKind.GENERATOR || !stack.is(Items.LAVA_BUCKET))
                            && burnTime(stack) / (kind == MachineKind.GENERATOR ? 4 : 1) > 0;
            case INPUT ->
                    !(owner instanceof ProcessingDevice machine) || machine.acceptsInput(stack);
            case CHARGE -> ForgeGear.acceptsCharge(stack, kind.itemTier());
            case DISCHARGE ->
                    owner instanceof ProcessingDevice device
                            ? ForgeGear.canDischarge(stack, device.itemTier())
                                    || device.electric() && isRedstoneFuel(stack)
                            : ForgeGear.canDischarge(stack, kind.itemTier());
            case UPGRADE -> MachineAutomation.isProcessingUpgrade(stack);
        };
    }

    @Override
    public int[] getSlotsForFace(Direction face) {
        return face == Direction.DOWN ? bottom : visible;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction face) {
        if (stack.isEmpty()
                || !MachineAccess.allowsInput(owner, face)
                || !canPlaceItem(index, stack)) return false;
        return switch (kind.slots.get(index)) {
            case INPUT, FUEL -> true;
            case CHARGE -> !ForgeGear.chargeComplete(stack, kind.itemTier());
            case DISCHARGE ->
                    owner instanceof ProcessingDevice device
                            ? ForgeGear.canSupplyCharge(stack, device.itemTier())
                                    || device.electric() && isRedstoneFuel(stack)
                            : ForgeGear.canSupplyCharge(stack, kind.itemTier());
            case OUTPUT, UPGRADE -> false;
        };
    }

    private static boolean isRedstoneFuel(ItemStack stack) {
        return stack.is(Items.REDSTONE) || WorkshopPlatform.tagged(stack, "c:dusts/redstone");
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction face) {
        if (stack.isEmpty() || !MachineAccess.allowsOutput(owner, face)) return false;
        return switch (kind.slots.get(index)) {
            case OUTPUT -> true;
            case FUEL -> isFuelProduct(stack);
            case CHARGE -> ForgeGear.chargeComplete(stack, kind.itemTier());
            default -> false;
        };
    }

    private boolean isFuelProduct(ItemStack stack) {
        return !completedFuel.isEmpty()
                && !stack.isEmpty()
                && stack.getCount() <= completedFuel.getCount()
                && ItemStack.isSameItemSameTags(stack, completedFuel)
                && stack.areCapsCompatible(completedFuel);
    }

    @Override
    public int automationLimit(int slot, ItemStack stack) {
        return switch (kind.slots.get(slot)) {
            case CHARGE, DISCHARGE -> ForgeGear.powerSlotLimit(stack);
            default -> stack.getMaxStackSize();
        };
    }

    @Override
    public boolean stillValid(Player player) {
        if (owner == null) {
            return true;
        }
        var pos = owner.getBlockPos();
        return owner.getLevel() == player.level
                && !owner.isRemoved()
                && owner.getLevel().getBlockEntity(pos) == owner
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (owner != null && owner.getLevel() != null) {
            if (owner instanceof ProcessingDevice machine) machine.refreshUpgrades();
            owner.setChanged();
        }
    }

    LazyOptional<IItemHandler> handler(Direction face) {
        return MachineAccess.connects(owner, face)
                ? (face == null ? unsided : handlers.getOrDefault(face, LazyOptional.empty()))
                : LazyOptional.empty();
    }

    void invalidate() {
        replacePorts(false);
    }

    void refresh() {
        replacePorts(true);
    }

    private void replacePorts(boolean reopen) {
        var previousUnsided = unsided;
        var previousSided = List.copyOf(handlers.values());
        unsided = LazyOptional.empty();
        handlers.clear();
        if (reopen) revive();
        previousUnsided.invalidate();
        previousSided.forEach(LazyOptional::invalidate);
    }

    void revive() {
        unsided =
                LazyOptional.of(
                        () -> MachinePorts.item(owner, null, new ProcessingItemPort(this, null)));
        for (Direction side : Direction.values()) {
            handlers.put(
                    side,
                    LazyOptional.of(
                            () ->
                                    MachinePorts.item(
                                            owner, side, new ProcessingItemPort(this, side))));
        }
    }
}
