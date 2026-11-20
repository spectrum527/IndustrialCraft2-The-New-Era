package com.industrialage.ia2.industrial;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent inventory; each open menu gets its own page view of this same set of stacks. */
public final class BoxNode extends BoxHost {
    final BoxKind kind;
    final SimpleContainer items;
    UUID owner;

    public BoxNode(BlockPos pos, BlockState state) {
        super(BoxPlatform.type(), pos, state);
        kind = ((BoxBlock) state.getBlock()).kind;
        items =
                new SimpleContainer(kind.slots) {
                    @Override
                    public void setChanged() {
                        super.setChanged();
                        BoxNode.this.setChanged();
                    }
                };
    }

    /**
     * Read/write view for a player at this inventory, with the same permissions as a block
     * interaction.
     */
    public net.minecraft.world.inventory.AbstractContainerMenu menu(
            int id, net.minecraft.world.entity.player.Inventory viewer, int page) {
        if (!permits(viewer.player)
                || page < 0
                || page > (kind.slots <= 54 ? 0 : (kind.slots - 1) / 45)
                || kind.slots == 0) return null;
        return BoxMenu.open(id, viewer, this, page);
    }

    boolean permits(Player player) {
        return owner == null || player.getUUID().equals(owner);
    }

    void open(Player player, int page) {
        if (getLevel() == null || !permits(player)) return;
        if (kind == BoxKind.PERSONAL_CHEST && owner == null) {
            owner = player.getUUID();
            setChanged();
        }
        var title = Component.translatable("block.ic2." + kind.path);
        if (kind == BoxKind.INDUSTRIAL_WORKBENCH) {
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inv, who) ->
                                    new CraftingMenu(
                                            id,
                                            inv,
                                            ContainerLevelAccess.create(
                                                    getLevel(), worldPosition)) {
                                        @Override
                                        public boolean stillValid(Player p) {
                                            return inReach(p);
                                        }
                                    },
                            title));
            return;
        }
        if (kind.slots > 54)
            title = Component.translatable("ic2.box.page", title, page + 1, (kind.slots + 44) / 45);
        player.openMenu(
                new SimpleMenuProvider((id, inventory, who) -> menu(id, inventory, page), title));
    }

    /**
     * Scatters the personal safe's contents on removal; portable boxes restore theirs on placement.
     */
    void releaseContents(net.minecraft.world.level.Level level, BlockPos pos) {
        if (kind == BoxKind.PERSONAL_CHEST)
            net.minecraft.world.Containers.dropContents(level, pos, items);
    }

    boolean inReach(Player player) {
        return !isRemoved()
                && permits(player)
                && player.distanceToSqr(
                                worldPosition.getX() + .5,
                                worldPosition.getY() + .5,
                                worldPosition.getZ() + .5)
                        <= 64;
    }
}
