package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.industrial.BoxBlock;
import com.industrialage.ia2.industrial.BoxNode;
import com.industrialage.ia2.wrenchtest.WrenchHits;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.registries.ForgeRegistries;

/** Shared survival matrix for the development GameTest and the installed production JAR. */
final class StorageMatrix {
    private record Box(String id, int slots) {}

    private static final List<Box> BOXES =
            List.of(
                    new Box("personal_chest", 54), new Box("wooden_storage_box", 27),
                    new Box("iron_storage_box", 45), new Box("bronze_storage_box", 45),
                    new Box("steel_storage_box", 63), new Box("iridium_storage_box", 126));

    private StorageMatrix() {}

    static void run(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer owner,
            ServerPlayer outsider,
            BiConsumer<String, Boolean> check) {
        Set<String> registered =
                ForgeRegistries.BLOCKS.getValues().stream()
                        .filter(block -> block instanceof BoxBlock)
                        .map(block -> ForgeRegistries.BLOCKS.getKey(block).getPath())
                        .filter(id -> !id.equals("industrial_workbench"))
                        .collect(Collectors.toSet());
        check.accept(
                "all-registered-boxes-covered",
                registered.equals(BOXES.stream().map(Box::id).collect(Collectors.toSet())));
        owner.setGameMode(GameType.SURVIVAL);
        outsider.setGameMode(GameType.SURVIVAL);
        for (var box : BOXES)
            for (var method : List.of("hand", "wrong", "tool", "wrench"))
                exercise(level, pos, owner, outsider, box, method, check);
    }

    private static void exercise(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer owner,
            ServerPlayer outsider,
            Box box,
            String method,
            BiConsumer<String, Boolean> check) {
        String key = box.id() + "-" + method + "-";
        owner.getInventory().clearContent();
        owner.setShiftKeyDown(false);
        place(level, owner, pos, PlayableGameTests.stack(box.id()), check, key);
        if (box.id().equals("personal_chest")) {
            owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            use(level, owner, pos, ItemStack.EMPTY);
        }
        var node = (BoxNode) level.getBlockEntity(pos);
        var menu = node.menu(1, owner.getInventory(), 0);
        check.accept(key + "owner-can-open", menu != null);
        if (method.equals("hand")) {
            verifyBoxSorting(level, pos, owner, outsider, box, node, menu, check, key);
        }
        var special = new ItemStack(Items.IRON_SWORD);
        special.setDamageValue(7);
        special.setHoverName(Component.literal("Storage QA marker"));
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND, 2));
        menu.getSlot(box.slots() / 2).set(special.copy());
        menu.getSlot(box.slots() - 1).set(new ItemStack(Items.REDSTONE, 17));
        if (box.id().equals("personal_chest")) {
            outsider.setPos(pos.getX() + 2.5, pos.getY(), pos.getZ() + .5);
            check.accept(
                    key + "outsider-denied",
                    node.menu(2, outsider.getInventory(), 0) == null
                            && level.getBlockState(pos).getDestroyProgress(outsider, level, pos)
                                    == 0);
            var tool = PlayableGameTests.stack("wrench");
            outsider.setItemInHand(InteractionHand.MAIN_HAND, tool);
            use(level, outsider, pos, tool);
            check.accept(key + "outsider-wrench-denied", level.getBlockEntity(pos) == node);
            check.accept(
                    key + "no-automation-access",
                    !node.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).isPresent());
        }
        ItemStack held =
                switch (method) {
                    case "wrong" -> new ItemStack(Items.DIAMOND_SHOVEL);
                    case "tool" ->
                            new ItemStack(
                                    box.id().equals("wooden_storage_box")
                                            ? Items.DIAMOND_AXE
                                            : Items.DIAMOND_PICKAXE);
                    case "wrench" -> PlayableGameTests.stack("wrench");
                    default -> ItemStack.EMPTY;
                };
        if (method.equals("tool")) {
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SHOVEL));
            float wrongSpeed = level.getBlockState(pos).getDestroyProgress(owner, level, pos);
            owner.setItemInHand(InteractionHand.MAIN_HAND, held);
            check.accept(
                    key + "correct-tool-effective",
                    level.getBlockState(pos).getDestroyProgress(owner, level, pos) > wrongSpeed);
        } else owner.setItemInHand(InteractionHand.MAIN_HAND, held);
        if (method.equals("wrench")) {
            use(level, owner, pos, held);
            check.accept(key + "removed", level.getBlockState(pos).isAir());
        } else check.accept(key + "removed", owner.gameMode.destroyBlock(pos));
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
        check.accept(
                key + "one-drop-no-scattered-content",
                drops.size() == 1
                        && drops.get(0).getItem().is(PlayableGameTests.stack(box.id()).getItem())
                        && drops.get(0).getItem().getCount() == 1);
        var entity = drops.get(0);
        owner.setPos(entity.getX(), entity.getY(), entity.getZ());
        entity.setNoPickUpDelay();
        entity.playerTouch(owner);
        check.accept(key + "picked-up", entity.isRemoved());
        int found = -1;
        for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
            if (owner.getInventory().getItem(i).is(PlayableGameTests.stack(box.id()).getItem())) {
                check.accept(
                        key + "single-inventory-box",
                        found == -1 && owner.getInventory().getItem(i).getCount() == 1);
                found = i;
            }
        }
        check.accept(key + "inventory-contains-box", found >= 0);
        var recovered = owner.getInventory().removeItem(found, 1);
        place(level, owner, pos, recovered, check, key);
        var returned = (BoxNode) level.getBlockEntity(pos);
        var restored = returned.menu(3, owner.getInventory(), 0);
        check.accept(
                key + "contents-round-trip",
                restored != null
                        && ItemStack.matches(
                                restored.getSlot(0).getItem(), new ItemStack(Items.DIAMOND, 2))
                        && ItemStack.matches(restored.getSlot(box.slots() / 2).getItem(), special)
                        && ItemStack.matches(
                                restored.getSlot(box.slots() - 1).getItem(),
                                new ItemStack(Items.REDSTONE, 17)));
        boolean otherSlotsEmpty = true;
        for (int i = 0; i < box.slots(); i++)
            if (i != 0 && i != box.slots() / 2 && i != box.slots() - 1)
                otherSlotsEmpty &= restored.getSlot(i).getItem().isEmpty();
        check.accept(key + "all-other-slots-empty", otherSlotsEmpty);
        if (box.id().equals("personal_chest")) {
            check.accept(
                    key + "owner-remains-locked",
                    returned.menu(4, outsider.getInventory(), 0) == null
                            && level.getBlockState(pos).getDestroyProgress(outsider, level, pos)
                                    == 0);
            check.accept(
                    key + "still-no-automation-access",
                    !returned.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                            .isPresent());
        }
        check.accept(
                key + "no-residual-drops",
                level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).isEmpty());
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    private static void verifyBoxSorting(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer owner,
            ServerPlayer outsider,
            Box box,
            BoxNode node,
            net.minecraft.world.inventory.AbstractContainerMenu menu,
            BiConsumer<String, Boolean> check,
            String key) {
        owner.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        outsider.setPos(pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5);

        var wornSword = new ItemStack(Items.IRON_SWORD);
        wornSword.setDamageValue(12);
        var freshSword = new ItemStack(Items.IRON_SWORD);

        menu.getSlot(0).set(new ItemStack(Items.COBBLESTONE, 40));
        menu.getSlot(1).set(new ItemStack(Items.REDSTONE, 40));
        menu.getSlot(2).set(wornSword.copy());
        menu.getSlot(3).set(freshSword.copy());
        menu.getSlot(box.slots() / 2).set(new ItemStack(Items.COBBLESTONE, 40));
        menu.getSlot(box.slots() - 1).set(new ItemStack(Items.REDSTONE, 30));

        if (box.id().equals("personal_chest")) {
            boolean outsiderSorted = menu.clickMenuButton(outsider, 1);
            check.accept(
                    key + "sort-outsider-denied",
                    !outsiderSorted
                            && ItemStack.matches(
                                    menu.getSlot(0).getItem(), new ItemStack(Items.COBBLESTONE, 40))
                            && ItemStack.matches(
                                    menu.getSlot(box.slots() - 1).getItem(),
                                    new ItemStack(Items.REDSTONE, 30)));
        }

        check.accept(key + "sort-ignores-action-0", !menu.clickMenuButton(owner, 0));
        check.accept(key + "sort-action-accepted", menu.clickMenuButton(owner, 1));
        boolean ordered =
                ItemStack.matches(menu.getSlot(0).getItem(), new ItemStack(Items.COBBLESTONE, 64))
                        && ItemStack.matches(
                                menu.getSlot(1).getItem(), new ItemStack(Items.COBBLESTONE, 16))
                        && ItemStack.matches(menu.getSlot(2).getItem(), freshSword)
                        && ItemStack.matches(menu.getSlot(3).getItem(), wornSword)
                        && ItemStack.matches(
                                menu.getSlot(4).getItem(), new ItemStack(Items.REDSTONE, 64))
                        && ItemStack.matches(
                                menu.getSlot(5).getItem(), new ItemStack(Items.REDSTONE, 6));
        for (int i = 6; i < box.slots(); i++) {
            ordered &= menu.getSlot(i).getItem().isEmpty();
        }
        check.accept(key + "sort-merged-and-ordered", ordered);

        if (box.id().equals("iridium_storage_box")) {
            var paged = node.menu(9, owner.getInventory(), 2);
            menu.getSlot(125).set(new ItemStack(Items.APPLE, 3));
            check.accept(
                    key + "sort-offset-page-sorts-full-container",
                    paged != null
                            && paged.clickMenuButton(owner, 1)
                            && ItemStack.matches(
                                    menu.getSlot(0).getItem(), new ItemStack(Items.APPLE, 3))
                            && menu.getSlot(125).getItem().isEmpty());
        }

        for (int i = 0; i < box.slots(); i++) {
            menu.getSlot(i).set(ItemStack.EMPTY);
        }

        if (box.id().equals("wooden_storage_box")) {
            var benchPos = pos.west(2);
            place(
                    level,
                    owner,
                    benchPos,
                    PlayableGameTests.stack("industrial_workbench"),
                    check,
                    "workbench-");
            var benchNode = (BoxNode) level.getBlockEntity(benchPos);
            var benchMenu = benchNode.menu(10, owner.getInventory(), 0);
            benchMenu.getSlot(0).set(new ItemStack(Items.OAK_LOG, 1));
            benchMenu.getSlot(13).set(new ItemStack(Items.COBBLESTONE, 5));
            boolean ignoredSort = !benchMenu.clickMenuButton(owner, 1);
            boolean preserved =
                    ItemStack.matches(benchMenu.getSlot(0).getItem(), new ItemStack(Items.OAK_LOG))
                            && ItemStack.matches(
                                    benchMenu.getSlot(13).getItem(),
                                    new ItemStack(Items.COBBLESTONE, 5));
            boolean clearedGrid =
                    benchMenu.clickMenuButton(owner, 0)
                            && benchMenu.getSlot(0).getItem().isEmpty()
                            && ItemStack.matches(
                                    benchMenu.getSlot(13).getItem(),
                                    new ItemStack(Items.COBBLESTONE, 5));
            check.accept(
                    "workbench-action-0-preserved-and-sort-ignored",
                    ignoredSort && preserved && clearedGrid);
            benchMenu.getSlot(13).set(ItemStack.EMPTY);
            level.setBlockAndUpdate(benchPos, Blocks.AIR.defaultBlockState());
            owner.getInventory().clearContent();
        }
    }

    private static void place(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            ItemStack stack,
            BiConsumer<String, Boolean> check,
            String key) {
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        player.setPos(pos.getX() + 2.5, pos.getY(), pos.getZ() + .5);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false);
        check.accept(
                key + "survival-placement",
                player.gameMode
                                .useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit)
                                .consumesAction()
                        && stack.isEmpty()
                        && level.getBlockEntity(pos) instanceof BoxNode);
    }

    private static void use(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack held) {
        var hit = WrenchHits.center(level, pos, Direction.UP);
        player.gameMode.useItemOn(player, level, held, InteractionHand.MAIN_HAND, hit);
    }
}
