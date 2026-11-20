package com.industrialage.ia2.forge.qa;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2_qa")
@PrefixGameTestTemplate(false)
public final class ContainmentGameTests {
    private ContainmentGameTests() {}

    /** Uses the actual native menu to exercise the filter, twelfth slot and tagged item save. */
    @GameTest(template = "smoke")
    public static void containmentFilterAndPersistence(GameTestHelper helper) {
        var profile = new GameProfile(UUID.randomUUID(), "[ContainmentTest]");
        var owner = FakePlayerFactory.get(helper.getLevel(), profile);
        var box = PlayableGameTests.stack("containment_box");
        owner.setItemInHand(InteractionHand.MAIN_HAND, box);
        box.getItem().use(helper.getLevel(), owner, InteractionHand.MAIN_HAND);
        require(
                owner.containerMenu instanceof ChestMenu,
                "Opened the registered portable box menu");
        var menu = (ChestMenu) owner.containerMenu;
        for (var rejected : new ItemStack[] {new ItemStack(Items.DIRT), box.copy()}) {
            menu.getSlot(18).set(rejected);
            require(
                    menu.quickMoveStack(owner, 18).isEmpty()
                            && menu.getSlot(18).getItem() == rejected,
                    "Shift-click rejected nonradioactive and nested contents");
            menu.setCarried(rejected.copy());
            menu.clicked(0, 0, ClickType.PICKUP, owner);
            require(menu.getSlot(0).getItem().isEmpty(), "Direct click rejected invalid contents");
        }
        for (int slot = 0; slot < 12; slot++) {
            var isotope = PlayableGameTests.stack("uranium_235");
            isotope.getOrCreateTag().putInt("quality_test_marker", slot + 100);
            menu.setCarried(isotope);
            menu.clicked(slot, 0, ClickType.PICKUP, owner);
            require(
                    menu.getCarried().isEmpty(),
                    "Accepted radioactive content in real slot " + slot);
        }
        var isotope = PlayableGameTests.stack("uranium_235");
        menu.setCarried(isotope);
        menu.clicked(12, 0, ClickType.PICKUP, owner);
        require(
                menu.getSlot(12).getItem().isEmpty() && menu.getCarried() == isotope,
                "Blocked overflow in the visual second row");
        var restoredBox = ItemStack.of(box.save(new CompoundTag()));
        owner.setItemInHand(InteractionHand.MAIN_HAND, restoredBox);
        restoredBox.getItem().use(helper.getLevel(), owner, InteractionHand.MAIN_HAND);
        var restored = (ChestMenu) owner.containerMenu;
        for (int slot = 0; slot < 12; slot++) {
            var saved = restored.getSlot(slot).getItem();
            require(
                    !saved.isEmpty()
                            && saved.getTag() != null
                            && saved.getTag().getInt("quality_test_marker") == slot + 100,
                    "Reload retained the exact tagged cargo in slot " + slot);
        }
        helper.succeed();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
