package com.industrialage.ia2.industrial;

import static org.junit.jupiter.api.Assertions.*;

import com.industrialage.ia2.gui.DeviceAccessMenu;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/**
 * Host-adapter contracts for storage box / personal safe permissions, workbench action isolation,
 * canonical NBT signature ordering, and BoxScreen sort button geometry on Forge 1.19.2.
 */
class BoxSortContractTest {
    @Test
    void enforcesPermissionsAndLeavesIndustrialWorkbenchActionZeroUntouched() {
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

    @Test
    void canonicalTagFormattingIsOrderIndependentAndDeterministic() {
        var first = new CompoundTag();
        first.putString("z", "last");
        first.putInt("a", 1);

        var second = new CompoundTag();
        second.putInt("a", 1);
        second.putString("z", "last");

        assertEquals(BoxSort.canonicalTag(first), BoxSort.canonicalTag(second));
        assertTrue(BoxSort.canonicalTag(first).contains("a=1"));
        assertTrue(BoxSort.canonicalTag(first).contains("z=\"last\""));
        assertEquals("", BoxSort.canonicalTag(null));
    }
}
