package com.industrialage.ia2.logistics;

import com.industrialage.ia2.industrial.WorkshopThermal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Cell-specific validation and container accounting after vanilla matches the recipe pattern. */
final class CellCrafting {
    enum Use {
        EMPTY(""),
        FILL(""),
        WATER("minecraft:water"),
        LAVA("minecraft:lava"),
        COOLANT("ic2:coolant"),
        WEED_EX("ic2:weed_ex"),
        BIND_WATER("minecraft:water"),
        RECOVER("");

        final String fluid;

        Use(String fluid) {
            this.fluid = fluid;
        }

        String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        static Use parse(String value) {
            return valueOf(value.toUpperCase(Locale.ROOT));
        }
    }

    private static final Set<String> CAPSULES =
            Set.of(
                    "ic2:electrolyzed_water_cell",
                    "ic2:hydration_cell",
                    "ic2:bio_cell",
                    "ic2:hydrated_coal_cell",
                    "ic2:biofuel_cell",
                    "ic2:coalfuel_cell");

    private CellCrafting() {}

    static boolean matches(Use use, List<ItemStack> inputs) {
        if (use == Use.FILL) return !fill(inputs).isEmpty();
        if (use == Use.RECOVER) {
            var source = singleInput(inputs);
            return TransportPlatform.cellContainsFluid(source)
                    || CAPSULES.contains(TransportPlatform.itemId(source));
        }
        boolean found = false;
        for (var input : inputs) {
            if (input.is(Items.BUCKET)) continue;
            boolean cell = input.getItem() instanceof CellItem;
            boolean bucket = input.getItem() instanceof BucketItem;
            if (!cell && !bucket) continue;
            var contents =
                    cell
                            ? TransportPlatform.cellContents(input)
                            : TransportPlatform.bucketContents(input);
            if (use == Use.EMPTY) {
                if (!cell || contents.amount() != 0) return false;
            } else {
                if (use == Use.BIND_WATER
                        && (!cell
                                || CellFamily.of(
                                                CellFamily.itemPath(
                                                        TransportPlatform.itemId(input)))
                                        != CellFamily.UNIVERSAL)) return false;
                if (contents.amount() != CellFamily.CAPACITY
                        || !WorkshopThermal.matchesCanonical(contents.id(), use.fluid))
                    return false;
            }
            found = true;
        }
        return found;
    }

    static ItemStack assemble(Use use, List<ItemStack> inputs, ItemStack result) {
        if (use == Use.FILL) return fill(inputs);
        if (!matches(use, inputs)) return ItemStack.EMPTY;
        if (use == Use.RECOVER) {
            return recover(singleInput(inputs));
        }
        if (use == Use.BIND_WATER) {
            for (var input : inputs) {
                if (input.getItem() instanceof CellItem)
                    return TransportPlatform.bindCell(input, result);
            }
        }
        return result;
    }

    static ItemStack recover(ItemStack source) {
        if (source.getItem() instanceof CellItem)
            return TransportPlatform.emptyCellRemainder(source);
        return CAPSULES.contains(TransportPlatform.itemId(source))
                ? TransportPlatform.emptyUniversalCell(source)
                : ItemStack.EMPTY;
    }

    static NonNullList<ItemStack> remainders(
            Use use, List<ItemStack> inputs, NonNullList<ItemStack> vanilla) {
        if (use == Use.RECOVER) return NonNullList.withSize(inputs.size(), ItemStack.EMPTY);
        if (use.fluid.isEmpty() || use == Use.BIND_WATER) return vanilla;
        for (int i = 0; i < inputs.size(); i++) {
            var input = inputs.get(i);
            if (input.getItem() instanceof CellItem) {
                vanilla.set(i, TransportPlatform.emptyCellRemainder(input));
            }
        }
        return vanilla;
    }

    private static ItemStack singleInput(List<ItemStack> inputs) {
        ItemStack source = ItemStack.EMPTY;
        for (var input : inputs) {
            if (input.isEmpty()) continue;
            if (!source.isEmpty()) return ItemStack.EMPTY;
            source = input;
        }
        return source;
    }

    private static ItemStack fill(List<ItemStack> inputs) {
        ItemStack cell = ItemStack.EMPTY;
        ItemStack bucket = ItemStack.EMPTY;
        for (var input : inputs) {
            if (input.isEmpty()) continue;
            if (input.getItem() instanceof CellItem && cell.isEmpty()) cell = input;
            else if (input.getItem() instanceof BucketItem && bucket.isEmpty()) bucket = input;
            else return ItemStack.EMPTY;
        }
        return cell.isEmpty() || bucket.isEmpty() || TransportPlatform.cellContainsFluid(cell)
                ? ItemStack.EMPTY
                : TransportPlatform.fillFromBucket(cell, bucket);
    }
}
