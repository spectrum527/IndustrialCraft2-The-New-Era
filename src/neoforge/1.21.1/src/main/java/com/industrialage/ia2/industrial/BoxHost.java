package com.industrialage.ia2.industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Preserves native stacks on world save and in portable box drops. */
abstract class BoxHost extends BlockEntity {
    BoxHost(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private NonNullList<ItemStack> list() {
        var n = (BoxNode) this;
        var list = NonNullList.withSize(n.kind.slots, ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) list.set(i, n.items.getItem(i));
        return list;
    }

    private void restore(NonNullList<ItemStack> list) {
        var n = (BoxNode) this;
        for (int i = 0; i < list.size(); i++) n.items.setItem(i, list.get(i));
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        var node = (BoxNode) this;
        ContainerHelper.saveAllItems(tag, list(), lookup);
        if (node.owner != null) tag.putUUID("owner", node.owner);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        var node = (BoxNode) this;
        var items = NonNullList.withSize(node.kind.slots, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, lookup);
        restore(items);
        node.owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        var node = (BoxNode) this;
        if (node.kind.portable())
            builder.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list()));
        if (node.kind == BoxKind.PERSONAL_CHEST && node.owner != null) {
            var tag = new CompoundTag();
            tag.putString("ic2_box_owner", node.owner.toString());
            builder.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        var n = (BoxNode) this;
        if (n.kind.portable()) {
            var content = input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            var items = NonNullList.withSize(n.kind.slots, ItemStack.EMPTY);
            content.copyInto(items);
            restore(items);
            if (n.kind == BoxKind.PERSONAL_CHEST) {
                var data = input.get(DataComponents.CUSTOM_DATA);
                if (data != null) {
                    try {
                        n.owner =
                                java.util.UUID.fromString(
                                        data.copyTag().getString("ic2_box_owner"));
                    } catch (IllegalArgumentException ignored) {
                        // An item without a valid owner remains unclaimed.
                    }
                }
            }
        }
    }
}
