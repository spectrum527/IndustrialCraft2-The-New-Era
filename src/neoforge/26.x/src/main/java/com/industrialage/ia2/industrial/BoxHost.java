package com.industrialage.ia2.industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        var node = (BoxNode) this;
        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), list());
        if (node.owner != null) output.putString("owner", node.owner.toString());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        var node = (BoxNode) this;
        var data =
                input.read("items", ItemStack.OPTIONAL_CODEC.listOf())
                        .orElseGet(java.util.List::of);
        for (int i = 0; i < Math.min(node.kind.slots, data.size()); i++)
            node.items.setItem(i, data.get(i));
        node.owner = input.getString("owner").map(UUID::fromString).orElse(null);
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
    protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter input) {
        super.applyImplicitComponents(input);
        var n = (BoxNode) this;
        if (!n.kind.portable()) return;
        var content = input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        var items = NonNullList.withSize(n.kind.slots, ItemStack.EMPTY);
        content.copyInto(items);
        restore(items);

        if (n.kind != BoxKind.PERSONAL_CHEST) return;
        var data = input.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var owner = data.copyTag().getString("ic2_box_owner");
        if (owner.isEmpty()) return;
        try {
            n.owner = UUID.fromString(owner.get());
        } catch (IllegalArgumentException ignored) {
            // Keep the existing owner when the item has a malformed identifier.
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (getLevel() instanceof net.minecraft.server.level.ServerLevel server)
            ((BoxNode) this).releaseContents(server, pos);
    }
}
