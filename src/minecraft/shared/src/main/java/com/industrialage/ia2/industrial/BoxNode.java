package com.industrialage.ia2.industrial;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent inventory shared by viewers and serialized by the native block entity. */
public final class BoxNode extends BoxHost
        implements com.industrialage.ia2.access.MachineAccessTarget {
    final BoxKind kind;
    final SimpleContainer items;
    UUID owner;
    long inventoryRevision;

    public BoxNode(BlockPos pos, BlockState state) {
        super(BoxPlatform.type(), pos, state);
        kind = ((BoxBlock) state.getBlock()).kind;
        items =
                new SimpleContainer(kind.slots) {
                    @Override
                    public void setChanged() {
                        super.setChanged();
                        inventoryRevision++;
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
        if (!permits(viewer.player) || page < 0 || page > (kind.slots - 1) / 45) return null;
        return new BoxMenu(id, viewer, kind, this, page * 45);
    }

    boolean permits(Player player) {
        return permitsOwner(owner, player == null ? null : player.getUUID());
    }

    static boolean permitsOwner(UUID owner, UUID actor) {
        return owner == null || (actor != null && actor.equals(owner));
    }

    void open(Player player, int page) {
        if (getLevel() == null || !permits(player)) return;
        if (kind == BoxKind.PERSONAL_CHEST && owner == null) {
            owner = player.getUUID();
            setChanged();
        }
        var title = Component.translatable("block.ic2." + kind.path);
        player.openMenu(
                new SimpleMenuProvider((id, inventory, who) -> menu(id, inventory, page), title));
    }

    /** Scatters workbench contents when the block is removed. */
    void releaseContents(net.minecraft.world.level.Level level, BlockPos pos) {
        if (kind == BoxKind.INDUSTRIAL_WORKBENCH)
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
