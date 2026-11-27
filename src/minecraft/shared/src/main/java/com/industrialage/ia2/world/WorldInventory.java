package com.industrialage.ia2.world;

import com.industrialage.ia2.access.MachineAccess;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

final class WorldInventory extends SimpleContainer implements WorldlyContainer {
    private final int[] slots;
    private final WorldMachine owner;

    WorldInventory(WorldMachine owner, int size) {
        super(size);
        this.owner = owner;
        slots = java.util.stream.IntStream.range(0, size).toArray();
    }

    WorldMachine owner() {
        return owner;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        owner.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        var pos = owner.getBlockPos();
        return !owner.isRemoved()
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return slots.clone();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot < 0
                || slot >= getContainerSize()
                || stack.isEmpty()
                || !MachineAccess.allowsInput(owner, side)) return false;
        if (owner.kind.packet > 0 && slot == owner.kind.batterySlot())
            return WorldNative.canDischarge(stack, owner.itemTier())
                    && WorldNative.stored(stack) > 0;
        return switch (owner.kind) {
            case BATCH_CRAFTER -> slot < 9 && getItem(slot + 18).is(stack.getItem());
            case ITEM_BUFFER -> slot < 26;
            case SORTING_MACHINE -> slot < 18;
            case WEIGHTED_DISTRIBUTOR -> slot < 18 && side == WorldAutomation.front(owner);
            case MINER -> slot == 2 && canPlaceItem(slot, stack);
            case CLASSIC_CROPMATRON, CROPMATRON -> slot < 3 && canPlaceItem(slot, stack);
            default -> false;
        };
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 0
                && slot < getContainerSize()
                && !stack.isEmpty()
                && MachineAccess.allowsOutput(owner, side)
                && (owner.kind == WorldMachineBlock.Kind.ITEM_BUFFER
                        ? slot < 26 && side == WorldAutomation.front(owner)
                        : output(slot));
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        String id = WorldNative.itemId(stack);
        if (owner.kind.packet > 0 && slot == owner.kind.batterySlot())
            return WorldNative.canDischarge(stack, owner.itemTier());
        return switch (owner.kind) {
            case CROPMATRON ->
                    switch (slot) {
                        case 0 -> isFertilizer(stack);
                        case 1 -> isWaterBucket(stack) || id.equals("ic2:water_cell");
                        case 2 -> isWeedExBucket(stack) || id.equals("ic2:weed_ex_cell");
                        case 9, 10, 11, 12 -> id.equals("ic2:fluid_pulling_upgrade");
                        default -> false;
                    };
            case CLASSIC_CROPMATRON ->
                    switch (slot) {
                        case 0 -> isFertilizer(stack);
                        case 1 -> id.equals("ic2:hydration_cell");
                        case 2 -> id.equals("ic2:weed_ex_cell");
                        default -> false;
                    };
            case TERRAFORMER -> slot == 0 && Terraforming.blueprint(id);
            case MINER ->
                    switch (slot) {
                        case 0 ->
                                id.equals("ic2:drill")
                                        || id.equals("ic2:diamond_drill")
                                        || id.equals("ic2:iridium_drill");
                        case 1 -> id.equals("ic2:scanner") || id.equals("ic2:advanced_scanner");
                        case 2 -> id.equals("ic2:mining_pipe");
                        default -> false;
                    };
            case ADVANCED_MINER ->
                    switch (slot) {
                        case 0 -> id.equals("ic2:scanner") || id.equals("ic2:advanced_scanner");
                        case 1, 2, 3 -> true;
                        case 9 -> id.equals("ic2:redstone_inverter_upgrade");
                        default -> false;
                    };
            case BATCH_CRAFTER -> slot < 9 || slot >= 18;
            case ITEM_BUFFER -> slot < 26 || id.equals("ic2:ejector_upgrade");
            case SORTING_MACHINE -> slot < 24;
            case WEIGHTED_DISTRIBUTOR ->
                    slot < 24 || slot == 25 && id.equals("ic2:pulling_upgrade");
            default -> false;
        };
    }

    private boolean output(int slot) {
        return switch (owner.kind) {
            case CROP_HARVESTER -> slot < 8;
            case CROPMATRON, CLASSIC_CROPMATRON, MINER -> slot >= 3 && slot < 8;
            case ADVANCED_MINER -> slot >= 4 && slot < 8;
            case BATCH_CRAFTER -> slot >= 9 && slot < 17;
            case ITEM_BUFFER -> slot < 26;
            default -> false;
        };
    }

    static boolean isFertilizer(ItemStack stack) {
        return !stack.isEmpty()
                && (WorldNative.itemId(stack).equals("ic2:fertilizer")
                        || WorkshopPlatform.tagged(stack, "c:fertilizers"));
    }

    static boolean isWaterBucket(ItemStack stack) {
        return !stack.isEmpty()
                && (WorldNative.itemId(stack).equals("minecraft:water_bucket")
                        || WorkshopPlatform.tagged(stack, "c:buckets/water"));
    }

    static boolean isWeedExBucket(ItemStack stack) {
        return !stack.isEmpty()
                && (WorldNative.itemId(stack).equals("ic2:weed_ex_bucket")
                        || WorkshopPlatform.tagged(stack, "c:buckets/weed_ex"));
    }

    boolean has(int slot, String id) {
        return WorldNative.itemId(getItem(slot)).equals(id);
    }

    boolean store(List<ItemStack> produced) {
        if (produced.isEmpty()) return true;
        var plan = new ItemStack[getContainerSize()];
        for (int slot = 0; slot < plan.length; slot++) plan[slot] = getItem(slot).copy();
        for (var stack : produced) {
            int remaining = stack.getCount();
            for (int slot = 0; slot < plan.length && remaining > 0; slot++) {
                if (!output(slot)) continue;
                var prior = plan[slot];
                if (prior.isEmpty() || WorldNative.sameStack(prior, stack)) {
                    int amount = Math.min(remaining, stack.getMaxStackSize() - prior.getCount());
                    if (amount <= 0) continue;
                    if (prior.isEmpty()) {
                        prior = stack.copy();
                        prior.setCount(amount);
                        plan[slot] = prior;
                    } else {
                        prior.grow(amount);
                    }
                    remaining -= amount;
                }
            }
            if (remaining > 0) return false;
        }
        for (int i = 0; i < plan.length; i++) if (output(i)) super.setItem(i, plan[i]);
        return true;
    }
}
