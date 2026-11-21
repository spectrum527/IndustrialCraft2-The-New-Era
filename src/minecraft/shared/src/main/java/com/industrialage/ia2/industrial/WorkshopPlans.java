package com.industrialage.ia2.industrial;

import com.industrialage.ia2.industrial.WorkshopRecipes.Recipe;
import com.industrialage.ia2.industrial.WorkshopRecipes.Stack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

final class WorkshopPlans {
    private WorkshopPlans() {}

    record Input(int slot, int count) {}

    record Liquid(String id, int amount) {
        static final Liquid EMPTY = new Liquid("", 0);
    }

    record Job(
            String key,
            List<Input> input,
            List<ItemStack> products,
            Liquid requested,
            Liquid product,
            int duration,
            int heat,
            int airPerTick) {}

    static Job plan(WorkshopNode node, ServerLevel level) {
        var kind = node.kind;
        if (kind == WorkshopKind.INDUCTION) {
            return smelting(node, level);
        }
        if (kind == WorkshopKind.RECYCLER) {
            var input = node.inventory.getItem(0);
            if (!input.isEmpty()) {
                return new Job(
                        "recycle/" + WorkshopPlatform.itemId(input),
                        List.of(new Input(0, 1)),
                        WorkshopPlatform.excludedFromRecycling(input)
                                ? List.of()
                                : List.of(WorkshopPlatform.stack("ic2:scrap", 1)),
                        Liquid.EMPTY,
                        Liquid.EMPTY,
                        kind.duration,
                        0,
                        0);
            }
            return null;
        }
        if (kind == WorkshopKind.CANNER && (node.mode == 1 || node.mode == 2)) {
            return container(node, 0, node.mode == 1);
        }
        if (kind == WorkshopKind.UU_ASSEMBLY) {
            var template = node.inventory.getItem(0);
            var supply = node.inventory.getItem(1);
            return WorkshopPlatform.itemId(template).equals("ic2:uu_matter")
                            && WorkshopPlatform.sameItem(template, supply)
                    ? new Job(
                            "uu_assembly/oak_log",
                            List.of(new Input(1, 1)),
                            List.of(WorkshopPlatform.stack("minecraft:oak_log", 8)),
                            Liquid.EMPTY,
                            Liquid.EMPTY,
                            kind.duration,
                            0,
                            0)
                    : null;
        }
        if (kind == WorkshopKind.CLASSIC_CANNER) {
            var fuel = classicFuel(node);
            if (fuel != null) {
                return fuel;
            }
        }
        if (kind == WorkshopKind.FLUID_BOTTLER) {
            var filling = container(node, 0, true);
            return filling == null ? container(node, 1, false) : filling;
        }
        for (var recipe : WorkshopRecipes.forType(kind.recipes(node.mode))) {
            var job = parsed(node, recipe);
            if (job != null) {
                return job;
            }
        }
        return null;
    }

    private static Job classicFuel(WorkshopNode node) {
        int cells = -1;
        int can = -1;
        for (int slot = 0; slot < 2; slot++) {
            var item = node.inventory.getItem(slot);
            String id = WorkshopPlatform.itemId(item);
            if (id.equals("ic2:coalfuel_cell") && item.getCount() >= 6) {
                cells = slot;
            }
            if (id.equals("ic2:empty_fuel_can")) {
                can = slot;
            }
        }
        return cells >= 0 && can >= 0
                ? new Job(
                        "classic_canner/coal_fuel",
                        List.of(new Input(cells, 6), new Input(can, 1)),
                        List.of(WorkshopPlatform.classicFuelCan(15288)),
                        Liquid.EMPTY,
                        Liquid.EMPTY,
                        node.kind.duration,
                        0,
                        0)
                : null;
    }

    private static Job parsed(WorkshopNode node, Recipe recipe) {
        var kind = node.kind;
        if (recipe.value("hardness") > node.bladeHardness()) {
            return null;
        }
        boolean enrich = kind == WorkshopKind.CANNER && node.mode == 3;
        var inputs = inputs(node, recipe, enrich ? 1 : 0);
        if (inputs == null) {
            return null;
        }
        Liquid requested = Liquid.EMPTY;
        Liquid product = Liquid.EMPTY;
        if (enrich) {
            requested = liquid(recipe.inputs().get(0));
            product = liquid(recipe.outputs().get(0));
            var job = enrichContainer(node, recipe, inputs.get(0), requested, product);
            if (job != null) return job;
        } else if (kind == WorkshopKind.ORE_WASHER) {
            requested = new Liquid("minecraft:water", recipe.value("amount"));
        }
        List<ItemStack> products =
                enrich
                        ? List.of()
                        : recipe.outputs().stream()
                                .map(stack -> WorkshopPlatform.stack(stack.id(), stack.count()))
                                .toList();
        int duration = recipe.value("duration");
        return new Job(
                recipe.id(),
                inputs,
                products,
                requested,
                product,
                duration > 0 ? duration : kind.duration,
                recipe.value("minHeat"),
                recipe.value("fluid"));
    }

    private static Job enrichContainer(
            WorkshopNode node, Recipe recipe, Input additive, Liquid requested, Liquid product) {
        if (additive.slot() == 0) return null;
        var vessel = node.inventory.getItem(0);
        if (!WorkshopPlatform.canSupplyContainer(vessel, true)) return null;
        var tank = new WorkshopNode.Tank(product.amount());
        tank.fill(product.id(), product.amount(), false);
        var filled = WorkshopPlatform.fillContainer(tank, vessel);
        if (filled == null
                || filled.stack().isEmpty()
                || filled.liquid().amount() != product.amount()) return null;
        return new Job(
                recipe.id() + "/container/" + WorkshopPlatform.itemId(vessel),
                List.of(additive, new Input(0, 1)),
                List.of(filled.stack()),
                requested,
                Liquid.EMPTY,
                node.kind.duration,
                0,
                0);
    }

    private static List<Input> inputs(WorkshopNode node, Recipe recipe, int start) {
        var result = new ArrayList<Input>();
        int used = 0;
        for (int index = start; index < recipe.inputs().size(); index++) {
            var expected = recipe.inputs().get(index);
            int slot = matchingSlot(node, expected, used);
            if (slot < 0) {
                return null;
            }
            result.add(new Input(slot, expected.count()));
            used |= 1 << slot;
        }
        return List.copyOf(result);
    }

    private static int matchingSlot(WorkshopNode node, Stack expected, int used) {
        int slots = node.kind.inputs(node.mode);
        for (int index = 0; index < slots; index++) {
            // Enrichment accepts an old save with its additive in slot 0.
            int slot = node.kind == WorkshopKind.CANNER && node.mode == 3 ? 1 - index : index;
            var item = node.inventory.getItem(slot);
            if ((used & (1 << slot)) == 0 && item.getCount() >= expected.count()) {
                if (matches(item, expected)) {
                    return slot;
                }
            }
        }
        return -1;
    }

    static boolean acceptsCanningInput(WorkshopKind kind, int mode, int slot, ItemStack item) {
        if (slot >= kind.inputs(mode) || item.isEmpty()) return false;
        if (kind == WorkshopKind.CANNER) {
            if (mode == 1 || mode == 3 && slot == 0)
                return WorkshopPlatform.canSupplyContainer(item, true);
            if (mode == 2)
                return WorkshopCells.canDrain(item, kind.outputVolume(), ignored -> true);
        }
        int ingredient = kind == WorkshopKind.SOLID_CANNER ? 1 - slot : slot;
        return WorkshopRecipes.forType(kind.recipes(mode)).stream()
                .anyMatch(recipe -> matches(item, recipe.inputs().get(ingredient)));
    }

    private static boolean matches(ItemStack item, Stack expected) {
        String id = WorkshopPlatform.itemId(item);
        if (expected.id().startsWith("#")) {
            String tag = expected.id().substring(1);
            return WorkshopPlatform.tagged(item, tag) || WorkshopRecipes.knownMember(tag, id);
        }
        return expected.id().equals(id);
    }

    static boolean validAutomationInput(
            WorkshopNode node, ServerLevel level, int slot, ItemStack item) {
        if (!node.inventory.canPlaceItem(slot, item)) {
            return false;
        }
        return switch (node.kind) {
            case INDUCTION -> !WorkshopPlatform.smelt(level, item).isEmpty();
            case FLUID_BOTTLER, PUMP, RECYCLER, UU_ASSEMBLY, ORE_WASHER -> true;
            case CANNER, SOLID_CANNER -> true;
            case CLASSIC_CANNER ->
                    WorkshopPlatform.itemId(item).equals("ic2:coalfuel_cell")
                            || WorkshopPlatform.itemId(item).equals("ic2:empty_fuel_can")
                            || storedInput(node.kind, node.mode, item);
            default -> storedInput(node.kind, node.mode, item);
        };
    }

    static boolean storedInput(WorkshopKind kind, int mode, ItemStack item) {
        return WorkshopRecipes.forType(kind.recipes(mode)).stream()
                .flatMap(recipe -> recipe.inputs().stream())
                .anyMatch(input -> matches(item, input));
    }

    private static Liquid liquid(Stack stack) {
        return new Liquid(stack.id(), stack.count());
    }

    private static Job smelting(WorkshopNode node, ServerLevel level) {
        var inputs = new ArrayList<Input>();
        var products = new ArrayList<ItemStack>();
        var key = new StringBuilder("smelt");
        for (int slot = 0; slot < 2; slot++) {
            var item = node.inventory.getItem(slot);
            var output = WorkshopPlatform.smelt(level, item);
            if (!output.isEmpty() && node.canOutputTo(slot, output)) {
                key.append('/').append(slot).append('/').append(WorkshopPlatform.itemId(item));
                inputs.add(new Input(slot, 1));
                products.add(output);
            }
        }
        return inputs.isEmpty()
                ? null
                : new Job(
                        key.toString(),
                        List.copyOf(inputs),
                        List.copyOf(products),
                        Liquid.EMPTY,
                        Liquid.EMPTY,
                        node.kind.duration,
                        0,
                        0);
    }

    private static Job container(WorkshopNode node, int slot, boolean filling) {
        var item = node.inventory.getItem(slot);
        if (item.isEmpty()) {
            return null;
        }
        var moved =
                filling
                        ? WorkshopPlatform.fillContainer(node.inlet, item)
                        : WorkshopPlatform.drainWholeContainer(node.outlet, item);
        if (moved == null || moved.stack().isEmpty()) {
            return null;
        }
        return new Job(
                "container/"
                        + slot
                        + "/"
                        + filling
                        + "/"
                        + WorkshopPlatform.itemId(item)
                        + "/"
                        + moved.liquid().id()
                        + "/"
                        + moved.liquid().amount(),
                List.of(new Input(slot, 1)),
                List.of(moved.stack()),
                filling ? moved.liquid() : Liquid.EMPTY,
                filling ? Liquid.EMPTY : moved.liquid(),
                node.kind.duration,
                0,
                0);
    }

    record ContainerResult(ItemStack stack, Liquid liquid) {}
}
