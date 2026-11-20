package com.industrialage.ia2.world;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A temporary seed input/output, returned to the owner when the analyzer closes. */
final class AnalyzerMenu extends AbstractContainerMenu {
    private final SimpleContainer contents = new SimpleContainer(3);
    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack analyzer;
    final SimpleContainerData data = new SimpleContainerData(2);

    static void open(Player player, InteractionHand hand) {
        player.openMenu(
                new SimpleMenuProvider(
                        (id, inventory, owner) ->
                                new AnalyzerMenu(
                                        WorldMenus.analyzer(),
                                        id,
                                        inventory,
                                        owner,
                                        hand,
                                        owner.getItemInHand(hand)),
                        Component.translatable("item.ic2.cropnalyzer")));
    }

    AnalyzerMenu(
            MenuType<AnalyzerMenu> type,
            int id,
            Inventory player,
            Player owner,
            InteractionHand hand,
            ItemStack analyzer) {
        super(type, id);
        this.owner = owner;
        this.hand = hand;
        this.analyzer = analyzer;
        addSlot(
                new Slot(contents, 0, 8, 7) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return CropSeed.read(stack) != null;
                    }
                });
        addSlot(
                new Slot(contents, 1, 41, 7) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
        addSlot(
                new Slot(contents, 2, 152, 7) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        String id = WorldNative.itemId(stack);
                        return id.startsWith("ic2:")
                                && (id.contains("battery")
                                        || id.endsWith("energy_crystal")
                                        || id.endsWith("lapotron_crystal"));
                    }
                });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addPlayer(player, 9 + row * 9 + col, 8 + 18 * col, 141 + 18 * row);
        for (int col = 0; col < 9; col++) addPlayer(player, col, 8 + 18 * col, 199);
        addDataSlots(data);
    }

    private void addPlayer(Inventory inventory, int index, int x, int y) {
        addSlot(
                new Slot(inventory, index, x, y) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return analyzer == null || getItem() != analyzer;
                    }
                });
    }

    @Override
    public boolean stillValid(Player player) {
        return owner == null || owner == player && owner.getItemInHand(hand) == analyzer;
    }

    @Override
    public void broadcastChanges() {
        if (owner != null) {
            ItemStack battery = contents.getItem(2);
            if (!battery.isEmpty()) {
                double space = WorldNative.charge(analyzer.copy(), 1000, 2);
                if (space > 0) {
                    double supply = WorldNative.discharge(battery, space, 2);
                    if (supply > 0) {
                        WorldNative.charge(analyzer, supply, 2);
                        contents.setChanged();
                    }
                }
            }
            int energy = (int) WorldNative.stored(analyzer);
            data.set(0, energy & 0xffff);
            data.set(1, energy >>> 16);
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (owner == null || id != 0 || !stillValid(player)) return false;
        ItemStack input = contents.getItem(0), output = contents.getItem(1);
        var seed = CropSeed.read(input);
        if (seed == null || seed.genes().scan() >= 4) return false;
        ItemStack result = input.copy();
        result.setCount(1);
        CropSeed.write(result, seed.crop(), seed.genes().scanMore());
        if (!output.isEmpty()
                && (!WorldNative.sameStack(output, result)
                        || output.getCount() >= output.getMaxStackSize())) return false;
        var scanned = WorldInteractions.scan(player, analyzer, seed.crop(), seed.genes(), true);
        if (scanned == seed.genes()) return false;
        input.shrink(1);
        if (output.isEmpty()) contents.setItem(1, result);
        else output.grow(1);
        contents.setChanged();
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (owner != null)
            for (int i = 0; i < 3; i++) {
                ItemStack stack = contents.removeItemNoUpdate(i);
                if (!stack.isEmpty() && !player.getInventory().add(stack))
                    player.drop(stack, false);
            }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), prior = stack.copy();
        boolean moved =
                index < 3
                        ? com.industrialage.ia2.gui.MenuTransfer.move(
                                stack, slots, 3, slots.size(), true, WorldNative::sameStack)
                        : com.industrialage.ia2.gui.MenuTransfer.move(
                                stack, slots, 0, 3, false, WorldNative::sameStack);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return prior;
    }
}
