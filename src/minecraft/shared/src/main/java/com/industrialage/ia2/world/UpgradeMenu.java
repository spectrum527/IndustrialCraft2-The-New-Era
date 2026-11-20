package com.industrialage.ia2.world;

import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Nine non-consuming sample slots edit the installed upgrade's persisted filter. */
final class UpgradeMenu extends UpgradeClickBase {
    private final SimpleContainer samples = new SimpleContainer(9);
    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack upgrade;
    final SimpleContainerData data = new SimpleContainerData(4);

    static void open(Player player, InteractionHand hand) {
        player.openMenu(
                new SimpleMenuProvider(
                        (id, inventory, who) ->
                                new UpgradeMenu(
                                        WorldMenus.upgrade(),
                                        id,
                                        inventory,
                                        who,
                                        hand,
                                        who.getItemInHand(hand)),
                        player.getItemInHand(hand).getHoverName()));
    }

    UpgradeMenu(
            MenuType<UpgradeMenu> type,
            int id,
            Inventory player,
            Player owner,
            InteractionHand hand,
            ItemStack upgrade) {
        super(type, id);
        this.owner = owner;
        this.hand = hand;
        this.upgrade = upgrade;
        for (int i = 0; i < 9; i++)
            addSlot(
                    new Slot(samples, i, 98 + 18 * (i % 3), 21 + 18 * (i / 3)) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }

                        @Override
                        public boolean mayPickup(Player player) {
                            return false;
                        }
                    });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addPlayer(player, 9 + row * 9 + col, 8 + 18 * col, 84 + 18 * row);
        for (int col = 0; col < 9; col++) addPlayer(player, col, 8 + 18 * col, 142);
        addDataSlots(data);
        refresh();
    }

    private void addPlayer(Inventory inventory, int index, int x, int y) {
        addSlot(
                new Slot(inventory, index, x, y) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return upgrade == null || getItem() != upgrade;
                    }
                });
    }

    private void refresh() {
        if (upgrade == null) return;
        var settings = MachineAutomation.settings(upgrade);
        data.set(0, settings.meta() ? 1 : 0);
        data.set(1, settings.energy() ? 1 : 0);
        data.set(2, settings.denied() ? 1 : 0);
        data.set(3, settings.side() == null ? 0 : settings.side().ordinal() + 1);
        for (int i = 0; i < 9; i++) {
            var sample = settings.filters()[i];
            ItemStack visual = ItemStack.EMPTY;
            if (sample != null)
                try {
                    var item = WorldNative.item(sample.id());
                    if (item != null) {
                        visual = new ItemStack(item);
                        if (visual.isDamageableItem()) visual.setDamageValue(sample.damage());
                    }
                } catch (RuntimeException ignored) {
                }
            samples.setItem(i, visual);
        }
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return owner == null || owner == player && owner.getItemInHand(hand) == upgrade;
    }

    private void setSample(int index, ItemStack stack) {
        var settings = MachineAutomation.settings(upgrade);
        var next = Arrays.copyOf(settings.filters(), 9);
        next[index] = stack.isEmpty() ? null : MachineAutomation.Sample.of(stack);
        MachineAutomation.save(
                upgrade,
                new MachineAutomation.Settings(
                        settings.side(),
                        next,
                        settings.denied(),
                        settings.meta(),
                        settings.energy()));
        refresh();
    }

    @Override
    protected void selectGhost(int index, Player player) {
        if (owner != null && stillValid(player)) setSample(index, getCarried());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (owner == null || !stillValid(player)) return false;
        var s = MachineAutomation.settings(upgrade);
        boolean meta = s.meta(), energy = s.energy(), denied = s.denied();
        Direction side = s.side();
        if (id == 0) meta = !meta;
        else if (id == 1) energy = !energy;
        else if (id == 2) denied = !denied;
        else if (id == 3) {
            int next = side == null ? 0 : side.ordinal() + 1;
            side = next >= Direction.values().length ? null : Direction.values()[next];
        } else return false;
        MachineAutomation.save(
                upgrade, new MachineAutomation.Settings(side, s.filters(), denied, meta, energy));
        refresh();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (owner == null || !stillValid(player) || index < 9 || index >= slots.size())
            return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var settings = MachineAutomation.settings(upgrade);
        for (int i = 0; i < 9; i++)
            if (settings.filters()[i] == null) {
                setSample(i, slot.getItem());
                break;
            }
        return ItemStack.EMPTY;
    }
}
