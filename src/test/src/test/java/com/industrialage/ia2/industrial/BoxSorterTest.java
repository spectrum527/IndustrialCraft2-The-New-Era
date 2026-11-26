package com.industrialage.ia2.industrial;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/** Unit tests for the pure {@link BoxSorter} stack merger and deterministic comparator. */
class BoxSorterTest {
    private static final class FakeStack {
        final String itemId;
        final int damage;
        final String displayName;
        final String components;
        final int maxStackSize;
        int count;

        FakeStack(
                String itemId,
                int damage,
                String displayName,
                String components,
                int maxStackSize,
                int count) {
            this.itemId = itemId;
            this.damage = damage;
            this.displayName = displayName;
            this.components = components;
            this.maxStackSize = maxStackSize;
            this.count = count;
        }

        String signature() {
            return itemId
                    + "|"
                    + damage
                    + "|"
                    + displayName
                    + "|"
                    + components
                    + "|"
                    + maxStackSize;
        }

        boolean sameItem(FakeStack other) {
            return Objects.equals(itemId, other.itemId)
                    && damage == other.damage
                    && Objects.equals(displayName, other.displayName)
                    && Objects.equals(components, other.components)
                    && maxStackSize == other.maxStackSize;
        }

        FakeStack copyWithCount(int newCount) {
            return new FakeStack(itemId, damage, displayName, components, maxStackSize, newCount);
        }
    }

    private static final BoxSorter.StackOps<FakeStack> OPS =
            BoxSorter.ops(
                    s -> s.count <= 0,
                    s -> s.count,
                    s -> s.maxStackSize,
                    FakeStack::sameItem,
                    FakeStack::copyWithCount,
                    (s, count) -> s.count = count,
                    s ->
                            new BoxSorter.SortKey(
                                    s.itemId, s.damage, s.displayName, s.components, s.count));

    private static FakeStack stack(String id, int count) {
        return new FakeStack(id, 0, id, "", 64, count);
    }

    private static FakeStack stack(
            String id, int damage, String displayName, String components, int maxStack, int count) {
        return new FakeStack(id, damage, displayName, components, maxStack, count);
    }

    @Test
    void mergesFragmentedStacksAndRespectsItemAndContainerMaxStackSizes() {
        List<FakeStack> input =
                List.of(
                        stack("minecraft:cobblestone", 20),
                        stack("minecraft:ender_pearl", 0, "Ender Pearl", "", 16, 10),
                        stack("minecraft:cobblestone", 30),
                        stack("minecraft:iron_sword", 0, "Iron Sword", "", 1, 1),
                        stack("minecraft:ender_pearl", 0, "Ender Pearl", "", 16, 12),
                        stack("minecraft:cobblestone", 30),
                        stack("minecraft:iron_sword", 0, "Iron Sword", "", 1, 1));

        List<FakeStack> sorted = BoxSorter.mergeAndSort(input, 64, OPS);
        assertEquals(6, sorted.size());

        assertEquals("minecraft:cobblestone", sorted.get(0).itemId);
        assertEquals(64, sorted.get(0).count);
        assertEquals("minecraft:cobblestone", sorted.get(1).itemId);
        assertEquals(16, sorted.get(1).count);

        assertEquals("minecraft:ender_pearl", sorted.get(2).itemId);
        assertEquals(16, sorted.get(2).count);
        assertEquals("minecraft:ender_pearl", sorted.get(3).itemId);
        assertEquals(6, sorted.get(3).count);

        assertEquals("minecraft:iron_sword", sorted.get(4).itemId);
        assertEquals(1, sorted.get(4).count);
        assertEquals("minecraft:iron_sword", sorted.get(5).itemId);
        assertEquals(1, sorted.get(5).count);

        // Container maxStackSize lower than item maxStackSize caps merged stacks.
        List<FakeStack> capped =
                BoxSorter.mergeAndSort(
                        List.of(stack("minecraft:stone", 20), stack("minecraft:stone", 25)),
                        32,
                        OPS);
        assertEquals(2, capped.size());
        assertEquals(32, capped.get(0).count);
        assertEquals(13, capped.get(1).count);
    }

    @Test
    void sortsDeterministicallyByIdDamageDisplayNameComponentsAndCountDescending() {
        List<FakeStack> input =
                List.of(
                        stack("minecraft:redstone", 12),
                        stack("ic2:tin_ingot", 5),
                        stack("ic2:copper_ingot", 10),
                        stack("ic2:copper_ingot", 64),
                        stack("minecraft:iron_sword", 42, "Iron Sword", "", 1, 1),
                        stack("minecraft:iron_sword", 0, "Zeta Blade", "ench=sharpness:1", 1, 1),
                        stack("minecraft:iron_sword", 0, "Alpha Blade", "ench=unbreaking:3", 1, 1),
                        stack("minecraft:iron_sword", 0, "Alpha Blade", "ench=mending:1", 1, 1),
                        stack("minecraft:iron_sword", 7, "Iron Sword", "", 1, 1),
                        stack("ic2:fluid_cell", 0, "Fluid Cell", "fluid=minecraft:water", 16, 4),
                        stack("ic2:fluid_cell", 0, "Fluid Cell", "fluid=ic2:biogas", 16, 2));

        List<FakeStack> sorted = BoxSorter.mergeAndSort(input, 64, OPS);
        assertEquals(11, sorted.size());

        // 1. ic2:copper_ingot: full stack 64 before partial stack 10
        assertEquals("ic2:copper_ingot", sorted.get(0).itemId);
        assertEquals(64, sorted.get(0).count);
        assertEquals("ic2:copper_ingot", sorted.get(1).itemId);
        assertEquals(10, sorted.get(1).count);

        // 2. ic2:fluid_cell: components tie-break (biogas < water)
        assertEquals("ic2:fluid_cell", sorted.get(2).itemId);
        assertEquals("fluid=ic2:biogas", sorted.get(2).components);
        assertEquals("ic2:fluid_cell", sorted.get(3).itemId);
        assertEquals("fluid=minecraft:water", sorted.get(3).components);

        // 3. ic2:tin_ingot
        assertEquals("ic2:tin_ingot", sorted.get(4).itemId);

        // 4. minecraft:iron_sword: damage 0 (Alpha mending < Alpha unbreaking < Zeta) < damage 7 <
        // damage 42
        assertEquals("minecraft:iron_sword", sorted.get(5).itemId);
        assertEquals(0, sorted.get(5).damage);
        assertEquals("Alpha Blade", sorted.get(5).displayName);
        assertEquals("ench=mending:1", sorted.get(5).components);

        assertEquals("minecraft:iron_sword", sorted.get(6).itemId);
        assertEquals(0, sorted.get(6).damage);
        assertEquals("Alpha Blade", sorted.get(6).displayName);
        assertEquals("ench=unbreaking:3", sorted.get(6).components);

        assertEquals("minecraft:iron_sword", sorted.get(7).itemId);
        assertEquals(0, sorted.get(7).damage);
        assertEquals("Zeta Blade", sorted.get(7).displayName);

        assertEquals("minecraft:iron_sword", sorted.get(8).itemId);
        assertEquals(7, sorted.get(8).damage);

        assertEquals("minecraft:iron_sword", sorted.get(9).itemId);
        assertEquals(42, sorted.get(9).damage);

        // 5. minecraft:redstone
        assertEquals("minecraft:redstone", sorted.get(10).itemId);
    }

    @Test
    void full126SlotIridiumBoxPreservesExactTotalsAndIsIdempotent() {
        List<FakeStack> slots = new ArrayList<>(126);
        String[] ids = {
            "minecraft:stone",
            "ic2:copper_ingot",
            "ic2:tin_ingot",
            "minecraft:diamond",
            "minecraft:redstone",
            "ic2:rubber"
        };
        for (int i = 0; i < 126; i++) {
            if (i % 11 == 0) {
                slots.add(null);
            } else if (i % 9 == 0) {
                slots.add(
                        stack(
                                "minecraft:diamond_pickaxe",
                                i % 37,
                                "Pick " + (i % 4),
                                "eff=" + (i % 3),
                                1,
                                1));
            } else if (i % 7 == 0) {
                slots.add(stack("minecraft:ender_pearl", 0, "Ender Pearl", "", 16, 1 + (i % 15)));
            } else {
                slots.add(stack(ids[i % ids.length], 1 + ((i * 13) % 37)));
            }
        }

        Map<String, Integer> beforeTotals = totalsBySignature(slots);
        List<FakeStack> sorted = BoxSorter.mergeAndSort(slots, 64, OPS);
        assertTrue(sorted.size() <= 126);
        assertEquals(beforeTotals, totalsBySignature(sorted));

        // Verify strictly non-decreasing sort keys.
        for (int i = 1; i < sorted.size(); i++) {
            var prev = OPS.sortKey(sorted.get(i - 1));
            var curr = OPS.sortKey(sorted.get(i));
            assertTrue(prev.compareTo(curr) <= 0, () -> prev + " > " + curr);
        }

        // Sorting an already-sorted list is strictly idempotent.
        List<FakeStack> secondPass = BoxSorter.mergeAndSort(sorted, 64, OPS);
        assertEquals(sorted.size(), secondPass.size());
        for (int i = 0; i < sorted.size(); i++) {
            assertEquals(sorted.get(i).signature(), secondPass.get(i).signature());
            assertEquals(sorted.get(i).count, secondPass.get(i).count);
        }
    }

    @Test
    void handlesEmptyInputAndPreservesPreexistingOverstackedSlotWithoutLoss() {
        assertTrue(BoxSorter.mergeAndSort(List.of(), 64, OPS).isEmpty());
        assertTrue(BoxSorter.mergeAndSort(List.of(stack("minecraft:dirt", 0)), 64, OPS).isEmpty());

        List<FakeStack> overstacked =
                BoxSorter.mergeAndSort(
                        List.of(stack("minecraft:dirt", 99), stack("minecraft:dirt", 5)), 64, OPS);
        assertEquals(2, overstacked.size());
        assertEquals(99, overstacked.get(0).count);
        assertEquals(5, overstacked.get(1).count);
    }

    private static Map<String, Integer> totalsBySignature(List<FakeStack> stacks) {
        Map<String, Integer> totals = new HashMap<>();
        for (FakeStack s : stacks) {
            if (s != null && s.count > 0) {
                totals.merge(s.signature(), s.count, Integer::sum);
            }
        }
        return totals;
    }
}
