package com.industrialage.ia2.industrial;

import static org.junit.jupiter.api.Assertions.*;

import com.industrialage.ia2.gui.DeviceAccessMenu;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Host-adapter contracts for storage box / personal safe server-side sorting, permissions,
 * workbench action isolation, and BoxScreen sort button geometry.
 */
class BoxSortContractTest {
    private static final class MockStack {
        final String itemId;
        final int damage;
        final String displayName;
        final String components;
        final int maxStackSize;
        int count;

        MockStack(
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

        boolean sameItem(MockStack other) {
            return Objects.equals(itemId, other.itemId)
                    && damage == other.damage
                    && Objects.equals(displayName, other.displayName)
                    && Objects.equals(components, other.components)
                    && maxStackSize == other.maxStackSize;
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

        MockStack copyWithCount(int next) {
            return new MockStack(itemId, damage, displayName, components, maxStackSize, next);
        }
    }

    private static final BoxSorter.StackOps<MockStack> OPS =
            BoxSorter.ops(
                    s -> s.count <= 0,
                    s -> s.count,
                    s -> s.maxStackSize,
                    MockStack::sameItem,
                    MockStack::copyWithCount,
                    (s, count) -> s.count = count,
                    s ->
                            new BoxSorter.SortKey(
                                    s.itemId, s.damage, s.displayName, s.components, s.count));

    @Test
    void allSixPortableBoxesAreEligibleAndWorkbenchIsExcluded() {
        assertEquals(27, BoxKind.WOODEN_STORAGE_BOX.slots);
        assertEquals(45, BoxKind.IRON_STORAGE_BOX.slots);
        assertEquals(45, BoxKind.BRONZE_STORAGE_BOX.slots);
        assertEquals(63, BoxKind.STEEL_STORAGE_BOX.slots);
        assertEquals(126, BoxKind.IRIDIUM_STORAGE_BOX.slots);
        assertEquals(54, BoxKind.PERSONAL_CHEST.slots);
        assertEquals(31, BoxKind.INDUSTRIAL_WORKBENCH.slots);

        for (var kind : BoxKind.values()) {
            if (kind == BoxKind.INDUSTRIAL_WORKBENCH) {
                assertFalse(kind.portable());
                assertFalse(BoxMenu.canSort(kind, 0, true, true, true));
                assertFalse(BoxMenu.canSort(kind, BoxMenu.SORT_ACTION, true, true, true));
            } else {
                assertTrue(kind.portable());
                assertTrue(BoxMenu.canSort(kind, BoxMenu.SORT_ACTION, true, true, true));
                assertFalse(BoxMenu.canSort(kind, 0, true, true, true));
                assertFalse(BoxMenu.canSort(kind, BoxMenu.SORT_ACTION, false, true, true));
                assertFalse(BoxMenu.canSort(kind, BoxMenu.SORT_ACTION, true, false, true));
                assertFalse(BoxMenu.canSort(kind, BoxMenu.SORT_ACTION, true, true, false));
            }
        }
    }

    @Test
    void personalChestPermissionAndActionIsolationAreEnforced() {
        UUID owner = UUID.randomUUID();
        UUID outsider = UUID.randomUUID();
        assertTrue(BoxNode.permitsOwner(null, outsider));
        assertTrue(BoxNode.permitsOwner(owner, owner));
        assertFalse(BoxNode.permitsOwner(owner, outsider));
        assertFalse(BoxNode.permitsOwner(owner, null));

        // Sort action (1) never collides with workbench clear (0) or DeviceAccessMenu (224..247).
        assertEquals(1, BoxMenu.SORT_ACTION);
        for (int face = 0; face < 6; face++) {
            for (int mode = 0; mode < 4; mode++) {
                assertNotEquals(BoxMenu.SORT_ACTION, DeviceAccessMenu.accessButton(face, mode));
            }
        }
    }

    @Test
    void componentSignatureCanonicalizesOrderDeterministically() {
        String first =
                BoxSort.formatComponentEntries(
                        List.of("ic2:eu=5000", "minecraft:custom_name=Alpha"));
        String reversed =
                BoxSort.formatComponentEntries(
                        List.of("minecraft:custom_name=Alpha", "ic2:eu=5000"));
        assertEquals("ic2:eu=5000;minecraft:custom_name=Alpha", first);
        assertEquals(first, reversed);
        assertEquals("", BoxSort.formatComponentEntries(List.of()));
    }

    @Test
    void iridium126SlotSortPreservesEveryItemAndOrdersDeterministically() {
        List<MockStack> slots = new ArrayList<>(BoxKind.IRIDIUM_STORAGE_BOX.slots);
        String[] ids = {
            "minecraft:stone",
            "minecraft:cobblestone",
            "minecraft:diamond",
            "minecraft:redstone",
            "ic2:copper_ingot",
            "ic2:tin_ingot"
        };
        for (int i = 0; i < BoxKind.IRIDIUM_STORAGE_BOX.slots; i++) {
            if (i % 6 == 0) {
                slots.add(null);
            } else if (i % 8 == 0) {
                slots.add(
                        new MockStack(
                                "minecraft:iron_sword",
                                i % 19,
                                "Sword " + (i % 3),
                                BoxSort.formatComponentEntries(
                                        List.of(
                                                "ic2:tag=" + (i % 2),
                                                "minecraft:damage=" + (i % 19))),
                                1,
                                1));
            } else if (i % 5 == 0) {
                slots.add(
                        new MockStack(
                                "minecraft:ender_pearl", 0, "Ender Pearl", "", 16, 1 + (i % 13)));
            } else {
                String id = ids[i % ids.length];
                slots.add(new MockStack(id, 0, id, "", 64, 1 + ((i * 11) % 35)));
            }
        }

        Map<String, Integer> totalsBefore = countBySignature(slots);
        List<MockStack> sorted = BoxSorter.mergeAndSort(slots, 64, OPS);
        assertTrue(sorted.size() <= BoxKind.IRIDIUM_STORAGE_BOX.slots);
        assertEquals(totalsBefore, countBySignature(sorted));

        for (int i = 1; i < sorted.size(); i++) {
            assertTrue(OPS.sortKey(sorted.get(i - 1)).compareTo(OPS.sortKey(sorted.get(i))) <= 0);
        }
    }

    @Test
    void sortButtonGeometryHasCleanClearanceAcrossAllBoxKinds() {
        assertEquals("gui.ic2.box.sort", BoxScreen.SORT_TOOLTIP_KEY);
        assertEquals(20, BoxScreen.SORT_RIGHT_MARGIN);
        assertEquals(4, BoxScreen.SORT_Y);
        assertEquals(12, BoxScreen.SORT_WIDTH);
        assertEquals(11, BoxScreen.SORT_HEIGHT);

        for (var kind : BoxKind.values()) {
            boolean bench = kind == BoxKind.INDUSTRIAL_WORKBENCH;
            int columns = kind == BoxKind.IRIDIUM_STORAGE_BOX ? 18 : 9;
            int width = bench ? 194 : columns * 18 + 14;
            int firstSlotTopBorder = (kind == BoxKind.PERSONAL_CHEST ? 19 : 17) - 1;
            int buttonX = BoxScreen.sortX(width);

            if (bench) {
                assertFalse(BoxScreen.inSortButton(kind, width, buttonX, BoxScreen.SORT_Y));
            } else {
                assertTrue(BoxScreen.inSortButton(kind, width, buttonX, BoxScreen.SORT_Y));
                assertTrue(
                        BoxScreen.inSortButton(
                                kind,
                                width,
                                buttonX + BoxScreen.SORT_WIDTH - 1,
                                BoxScreen.SORT_Y + BoxScreen.SORT_HEIGHT - 1));
                assertFalse(BoxScreen.inSortButton(kind, width, buttonX - 1, BoxScreen.SORT_Y));
                assertFalse(
                        BoxScreen.inSortButton(
                                kind, width, buttonX + BoxScreen.SORT_WIDTH, BoxScreen.SORT_Y));
                assertFalse(BoxScreen.inSortButton(kind, width, buttonX, BoxScreen.SORT_Y - 1));
                assertFalse(
                        BoxScreen.inSortButton(
                                kind, width, buttonX, BoxScreen.SORT_Y + BoxScreen.SORT_HEIGHT));
                // Clean vertical clearance above first slot well border (y=16 or y=18).
                assertTrue(BoxScreen.SORT_Y + BoxScreen.SORT_HEIGHT < firstSlotTopBorder);
                // Clean horizontal clearance inside panel right border.
                assertTrue(buttonX + BoxScreen.SORT_WIDTH <= width - 8);
            }
        }
    }

    private static Map<String, Integer> countBySignature(List<MockStack> stacks) {
        Map<String, Integer> totals = new HashMap<>();
        for (MockStack s : stacks) {
            if (s != null && s.count > 0) {
                totals.merge(s.signature(), s.count, Integer::sum);
            }
        }
        return totals;
    }
}
