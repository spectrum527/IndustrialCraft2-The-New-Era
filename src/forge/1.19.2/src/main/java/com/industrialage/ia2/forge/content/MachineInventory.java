package com.industrialage.ia2.forge.content;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.access.MachinePorts;
import com.industrialage.ia2.forge.content.MachineKind.Slot;
import com.industrialage.ia2.forge.equipment.ForgeGear;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import com.industrialage.ia2.world.MachineAutomation;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.IntStream;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

final class MachineInventory extends SimpleContainer implements WorldlyContainer {
    final MachineKind kind;
    private final BlockEntity owner;
    private final int[] visible;
    private final int[] bottom;
    private final Map<Direction, LazyOptional<IItemHandler>> handlers =
            new EnumMap<>(Direction.class);
    private LazyOptional<IItemHandler> unsided;

    MachineInventory(MachineKind kind, BlockEntity owner) {
        super(kind.slots.size());
        this.kind = kind;
        this.owner = owner;
        visible =
                kind.slots.stream()
                        .filter(
                                slot ->
                                        slot != Slot.CHARGE
                                                && slot != Slot.DISCHARGE
                                                && slot != Slot.UPGRADE)
                        .mapToInt(kind.slots::indexOf)
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
    }

    void save(CompoundTag tag) {
        var items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        for (int index = 0; index < items.size(); index++) {
            items.set(index, getItem(index));
        }
        ContainerHelper.saveAllItems(tag, items);
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
                                    || device.electric() && stack.is(Items.REDSTONE)
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
        if (!MachineAccess.allowsInput(owner, face)) return false;
        var role = kind.slots.get(index);
        return (role == Slot.INPUT || role == Slot.FUEL) && canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction face) {
        if (!MachineAccess.allowsOutput(owner, face)) return false;
        return switch (kind.slots.get(index)) {
            case OUTPUT -> true;
            case INPUT, FUEL -> !canPlaceItem(index, stack);
            default -> false;
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
                ? (face == null ? unsided : handlers.get(face))
                : LazyOptional.empty();
    }

    void invalidate() {
        unsided.invalidate();
        handlers.values().forEach(LazyOptional::invalidate);
    }

    void revive() {
        unsided =
                LazyOptional.of(
                        () -> MachinePorts.item(owner, null, new SidedInvWrapper(this, null)));
        for (Direction side : Direction.values()) {
            handlers.put(
                    side,
                    LazyOptional.of(
                            () -> MachinePorts.item(owner, side, new SidedInvWrapper(this, side))));
        }
    }
}
