package com.industrialage.ia2.gametest;

import com.industrialage.ia2.industrial.BoxBlock;
import com.industrialage.ia2.industrial.BoxNode;
import com.industrialage.ia2.wrenchtest.WrenchHits;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Real survival break, pickup and item placement for each registered storage inventory. */
final class StorageChecks {
    private record Box(String id, int slots) {}

    private static final List<Box> BOXES =
            List.of(
                    new Box("personal_chest", 54), new Box("wooden_storage_box", 27),
                    new Box("iron_storage_box", 45), new Box("bronze_storage_box", 45),
                    new Box("steel_storage_box", 63), new Box("iridium_storage_box", 126));
    private static final List<String> METHODS = List.of("hand", "wrong", "tool", "wrench");
    private static final BlockPos POS = new BlockPos(8, 112, 8);
    private static String dropEvent = "not fired";
    private static ItemEntity eventEntity;
    private FakePlayer owner, outsider;
    private Box box;
    private String method, key;
    private ItemStack special;
    private int index;
    private boolean awaitingDrops;
    private Runnable wrenchDrops;
    private Runnable utilityDrops;

    static void traceDrops(net.neoforged.neoforge.event.level.BlockDropsEvent event) {
        if (event.getPos().equals(POS)) {
            dropEvent =
                    event.getDrops().stream()
                            .map(entity -> entity.getItem().toString())
                            .toList()
                            .toString();
            eventEntity = event.getDrops().isEmpty() ? null : event.getDrops().get(0);
        }
    }

    void setup(ServerLevel level, BiConsumer<String, Boolean> check) {
        Set<String> registered =
                BuiltInRegistries.BLOCK.stream()
                        .filter(block -> block instanceof BoxBlock)
                        .map(block -> BuiltInRegistries.BLOCK.getKey(block).getPath())
                        .filter(id -> !id.equals("industrial_workbench"))
                        .collect(Collectors.toSet());
        check.accept(
                "all-registered-boxes-covered",
                registered.equals(BOXES.stream().map(Box::id).collect(Collectors.toSet())));
        check.accept("block-drops-enabled", StorageRule.blockDrops(level));
        owner = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "BoxOwner"));
        outsider = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "BoxOutsider"));
        owner.setGameMode(GameType.SURVIVAL);
        outsider.setGameMode(GameType.SURVIVAL);
        wrenchDrops =
                com.industrialage.ia2.wrenchtest.WrenchChecks.run(
                        level,
                        POS.east(5),
                        owner,
                        NativeGameplayFixture::item,
                        NativeGameplayFixture::block,
                        com.industrialage.ia2.gameplay.PortableGear::restoreEnergy,
                        com.industrialage.ia2.gameplay.PortableGear::stored,
                        check);
        utilityDrops =
                com.industrialage.ia2.wrenchtest.WrenchLootChecks.run(
                        level,
                        POS.east(5).south(5),
                        owner,
                        NativeGameplayFixture::item,
                        NativeGameplayFixture::block,
                        (pos, stack) ->
                                NativeTransferAccess.insert(
                                                level, pos, Direction.WEST, stack, false)
                                        == stack.getCount(),
                        (pos, amount) ->
                                NativeTransferAccess.fill(
                                                level,
                                                pos,
                                                Direction.WEST,
                                                new net.neoforged.neoforge.fluids.FluidStack(
                                                        net.minecraft.world.level.material.Fluids
                                                                .WATER,
                                                        amount),
                                                false)
                                        == amount,
                        check);
    }

    boolean tick(ServerLevel level, BiConsumer<String, Boolean> check) {
        if (wrenchDrops != null) {
            wrenchDrops.run();
            utilityDrops.run();
            wrenchDrops = null;
        }
        if (!awaitingDrops) {
            box = BOXES.get(index / METHODS.size());
            method = METHODS.get(index % METHODS.size());
            key = box.id() + "-" + method + "-";
            begin(level, check);
            awaitingDrops = true;
            return false;
        }
        finish(level, check);
        awaitingDrops = false;
        return ++index == BOXES.size() * METHODS.size();
    }

    private void begin(ServerLevel level, BiConsumer<String, Boolean> check) {
        owner.getInventory().clearContent();
        owner.setShiftKeyDown(false);
        NativeGameplayFixture.place(level, owner, box.id(), POS);
        if (box.id().equals("personal_chest")) {
            owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            owner.gameMode.useItemOn(
                    owner, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit(level));
        }
        var node = (BoxNode) level.getBlockEntity(POS);
        var menu = node.menu(1, owner.getInventory(), 0);
        check.accept(key + "owner-can-open", menu != null);
        special = new ItemStack(Items.IRON_SWORD);
        special.setDamageValue(7);
        special.set(DataComponents.CUSTOM_NAME, Component.literal("Storage QA marker"));
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND, 2));
        menu.getSlot(box.slots() / 2).set(special.copy());
        menu.getSlot(box.slots() - 1).set(new ItemStack(Items.REDSTONE, 17));
        if (box.id().equals("personal_chest")) {
            outsider.setPos(POS.getX() + 2.5, POS.getY(), POS.getZ() + .5);
            check.accept(
                    key + "outsider-denied",
                    node.menu(2, outsider.getInventory(), 0) == null
                            && level.getBlockState(POS).getDestroyProgress(outsider, level, POS)
                                    == 0);
            var tool = new ItemStack(NativeGameplayFixture.item("wrench"));
            outsider.setItemInHand(InteractionHand.MAIN_HAND, tool);
            outsider.gameMode.useItemOn(
                    outsider, level, tool, InteractionHand.MAIN_HAND, hit(level));
            check.accept(key + "outsider-wrench-denied", level.getBlockEntity(POS) == node);
            check.accept(key + "no-automation-access", !StorageRule.exposesAutomation(level, POS));
        }
        ItemStack held =
                switch (method) {
                    case "wrong" -> new ItemStack(Items.DIAMOND_SHOVEL);
                    case "tool" ->
                            new ItemStack(
                                    box.id().equals("wooden_storage_box")
                                            ? Items.DIAMOND_AXE
                                            : Items.DIAMOND_PICKAXE);
                    case "wrench" -> new ItemStack(NativeGameplayFixture.item("wrench"));
                    default -> ItemStack.EMPTY;
                };
        if (method.equals("tool")) {
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SHOVEL));
            float wrongSpeed = level.getBlockState(POS).getDestroyProgress(owner, level, POS);
            owner.setItemInHand(InteractionHand.MAIN_HAND, held);
            check.accept(
                    key + "correct-tool-effective",
                    level.getBlockState(POS).getDestroyProgress(owner, level, POS) > wrongSpeed);
        } else owner.setItemInHand(InteractionHand.MAIN_HAND, held);
        dropEvent = "not fired";
        eventEntity = null;
        if (method.equals("wrench")) {
            owner.gameMode.useItemOn(owner, level, held, InteractionHand.MAIN_HAND, hit(level));
            check.accept(key + "removed", level.getBlockState(POS).isAir());
        } else check.accept(key + "removed", owner.gameMode.destroyBlock(POS));
    }

    private void finish(ServerLevel level, BiConsumer<String, Boolean> check) {
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(3));
        if (!(drops.size() == 1
                && drops.get(0).getItem().is(NativeGameplayFixture.item(box.id()))
                && drops.get(0).getItem().getCount() == 1))
            throw new AssertionError(
                    key
                            + "unexpected-drops "
                            + drops.stream().map(entity -> entity.getItem().toString()).toList()
                            + " event="
                            + dropEvent
                            + " removed="
                            + (eventEntity == null ? "n/a" : eventEntity.isRemoved())
                            + " tracked="
                            + (eventEntity == null
                                    ? "n/a"
                                    : level.getEntity(eventEntity.getUUID()) != null)
                            + " position="
                            + (eventEntity == null ? "n/a" : eventEntity.position()));
        check.accept(key + "one-drop-no-scattered-content", true);
        var entity = drops.get(0);
        owner.setPos(entity.getX(), entity.getY(), entity.getZ());
        entity.setNoPickUpDelay();
        entity.playerTouch(owner);
        check.accept(key + "picked-up", entity.isRemoved());
        int slot = -1;
        for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
            if (owner.getInventory().getItem(i).is(NativeGameplayFixture.item(box.id()))) {
                check.accept(
                        key + "single-inventory-box",
                        slot == -1 && owner.getInventory().getItem(i).getCount() == 1);
                slot = i;
            }
        }
        check.accept(key + "inventory-contains-box", slot >= 0);
        var recovered = owner.getInventory().removeItem(slot, 1);
        NativeGameplayFixture.place(level, owner, box.id(), POS, recovered);
        var returned = (BoxNode) level.getBlockEntity(POS);
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
        for (int i = 0; i < box.slots(); i++) {
            if (i != 0 && i != box.slots() / 2 && i != box.slots() - 1)
                otherSlotsEmpty &= restored.getSlot(i).getItem().isEmpty();
        }
        check.accept(key + "all-other-slots-empty", otherSlotsEmpty);
        if (box.id().equals("personal_chest")) {
            check.accept(
                    key + "owner-remains-locked",
                    returned.menu(4, outsider.getInventory(), 0) == null
                            && level.getBlockState(POS).getDestroyProgress(outsider, level, POS)
                                    == 0);
            check.accept(
                    key + "still-no-automation-access", !StorageRule.exposesAutomation(level, POS));
        }
        check.accept(
                key + "no-residual-drops",
                level.getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(3)).isEmpty());
        level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
    }

    private static BlockHitResult hit(ServerLevel level) {
        return WrenchHits.center(level, POS, Direction.UP);
    }
}
