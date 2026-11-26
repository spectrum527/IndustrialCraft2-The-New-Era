package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.industrial.BoxNode;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Runs the storage matrix as a native Forge GameTest. */
@GameTestHolder("ic2_storage")
@PrefixGameTestTemplate(false)
public final class StorageGameTests {
    private StorageGameTests() {}

    /** All 24 combinations use survival game-mode breaks, item pickup and placement. */
    @GameTest(template = "smoke", timeoutTicks = 80)
    public static void allBoxRemovalMethods(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        StorageMatrix.run(
                helper.getLevel(),
                pos,
                PlayableGameTests.player(helper, pos),
                PlayableGameTests.player(helper, pos),
                (name, passed) -> {
                    if (!passed) helper.fail(name);
                });
        helper.succeed();
    }

    /** Verifies deterministic NBT/name/damage/count ordering and 126-slot Iridium integrity. */
    @GameTest(template = "smoke", timeoutTicks = 20)
    public static void iridiumBoxDeterministicSortAndIntegrity(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var player = PlayableGameTests.player(helper, pos);
        PlayableGameTests.place(
                helper, player, pos, PlayableGameTests.stack("iridium_storage_box"));
        var node = (BoxNode) helper.getLevel().getBlockEntity(pos);
        var menu = node.menu(1, player.getInventory(), 0);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        var swordDamaged = new ItemStack(Items.IRON_SWORD);
        swordDamaged.setDamageValue(15);
        var swordSlightlyWorn = new ItemStack(Items.IRON_SWORD);
        swordSlightlyWorn.setDamageValue(3);
        var swordBeta = new ItemStack(Items.IRON_SWORD);
        swordBeta.setHoverName(Component.literal("Beta"));
        var swordAlphaB = new ItemStack(Items.IRON_SWORD);
        swordAlphaB.setHoverName(Component.literal("Alpha"));
        swordAlphaB.getOrCreateTag().putString("cell", "water");
        var swordAlphaA = new ItemStack(Items.IRON_SWORD);
        swordAlphaA.setHoverName(Component.literal("Alpha"));
        swordAlphaA.getOrCreateTag().putString("cell", "biogas");

        menu.getSlot(0).set(new ItemStack(Items.REDSTONE, 10));
        menu.getSlot(1).set(swordDamaged);
        menu.getSlot(2).set(swordBeta);
        menu.getSlot(3).set(new ItemStack(Items.DIAMOND, 12));
        menu.getSlot(4).set(swordAlphaB);
        menu.getSlot(5).set(swordSlightlyWorn);
        menu.getSlot(6).set(new ItemStack(Items.DIAMOND, 64));
        menu.getSlot(7).set(swordAlphaA);

        PlayableGameTests.check(helper, menu.clickMenuButton(player, 1), "Sort button accepted");
        PlayableGameTests.check(
                helper,
                ItemStack.matches(new ItemStack(Items.DIAMOND, 64), menu.getSlot(0).getItem())
                        && ItemStack.matches(
                                new ItemStack(Items.DIAMOND, 12), menu.getSlot(1).getItem())
                        && ItemStack.matches(swordAlphaA, menu.getSlot(2).getItem())
                        && ItemStack.matches(swordAlphaB, menu.getSlot(3).getItem())
                        && ItemStack.matches(swordBeta, menu.getSlot(4).getItem())
                        && ItemStack.matches(swordSlightlyWorn, menu.getSlot(5).getItem())
                        && ItemStack.matches(swordDamaged, menu.getSlot(6).getItem())
                        && ItemStack.matches(
                                new ItemStack(Items.REDSTONE, 10), menu.getSlot(7).getItem()),
                "Deterministic ID + damage + name + NBT + count-desc order");

        // Stress all 126 slots for zero loss / zero duplication and contiguous compaction.
        for (int i = 0; i < 126; i++) menu.getSlot(i).set(ItemStack.EMPTY);
        var items =
                new Item[] {
                    Items.STONE,
                    Items.COBBLESTONE,
                    Items.DIAMOND,
                    Items.EMERALD,
                    Items.REDSTONE,
                    Items.ENDER_PEARL
                };
        for (int i = 0; i < 126; i++) {
            if (i % 5 == 0) continue;
            var item = items[i % items.length];
            int count = item == Items.ENDER_PEARL ? (1 + (i % 11)) : (1 + ((i * 7) % 31));
            menu.getSlot(i).set(new ItemStack(item, count));
        }
        menu.getSlot(125).set(new ItemStack(Items.APPLE, 3));

        Map<String, Integer> before = countByItem(menu, 126);
        PlayableGameTests.check(helper, menu.clickMenuButton(player, 1), "126-slot sort accepted");
        PlayableGameTests.check(
                helper, before.equals(countByItem(menu, 126)), "Zero item loss or duplication");
        PlayableGameTests.check(
                helper,
                ItemStack.matches(new ItemStack(Items.APPLE, 3), menu.getSlot(0).getItem()),
                "Apple from slot 125 sorted to slot 0");
        boolean seenEmpty = false;
        for (int i = 0; i < 126; i++) {
            if (menu.getSlot(i).getItem().isEmpty()) seenEmpty = true;
            else PlayableGameTests.check(helper, !seenEmpty, "No gaps in compacted 126-slot box");
        }
        helper.succeed();
    }

    private static Map<String, Integer> countByItem(
            net.minecraft.world.inventory.AbstractContainerMenu menu, int slots) {
        Map<String, Integer> totals = new HashMap<>();
        for (int i = 0; i < slots; i++) {
            var stack = menu.getSlot(i).getItem();
            if (stack.isEmpty()) continue;
            var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            totals.merge(String.valueOf(id), stack.getCount(), Integer::sum);
        }
        return totals;
    }
}
