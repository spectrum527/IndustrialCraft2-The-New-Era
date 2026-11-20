package com.industrialage.ia2.industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Minecraft's timed block-to-entity transition. The server owns the fuse and inventory. */
final class GameCharges {
    private GameCharges() {}

    static boolean primingItem(ItemStack item) {
        return item.is(Items.TNT) || PowerPlatform.path(item).equals("itnt");
    }

    static boolean cartridge(ItemStack stack) {
        String id = PowerPlatform.path(stack);
        return id.contains("uranium") || id.contains("plutonium") || id.contains("mox");
    }

    static boolean prime(ServerLevel level, PowerNode node) {
        if (node.isRemoved()) return false;
        var kind = node.kind;
        boolean nuclear = kind != PowerKind.ITNT;
        if (nuclear && !PowerSettings.gameChargesEnabled()) return false;
        var inner = node.inventory.getItem(0);
        var outer = node.inventory.getItem(1);
        if (kind == PowerKind.NUKE && !primingItem(outer)) return false;
        float effect =
                kind == PowerKind.ITNT
                        ? 5.5F
                        : kind == PowerKind.CLASSIC_NUKE
                                ? PowerSettings.gameChargeLimit()
                                : Math.min(
                                        PowerSettings.gameChargeLimit(),
                                        4F
                                                + outer.getCount()
                                                + (cartridge(inner) ? inner.getCount() * 2F : 0));
        if (effect <= 0) return false;
        BlockPos pos = node.getBlockPos();
        var actor = ChargeNative.actor(level, nuclear, node.getBlockState(), effect);
        actor.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        actor.setDeltaMovement(
                (level.getRandom().nextDouble() - 0.5) * 0.04,
                0.20,
                (level.getRandom().nextDouble() - 0.5) * 0.04);
        actor.setFuse(nuclear ? 300 : 60);
        if (!level.addFreshEntity(actor)) return false;
        node.inventory.clearContent();
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }
}
