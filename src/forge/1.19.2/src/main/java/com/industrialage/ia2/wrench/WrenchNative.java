package com.industrialage.ia2.wrench;

import com.industrialage.ia2.forge.equipment.ForgeGear;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;

/** Native permissions and tool costs for the single wrench interaction path. */
public final class WrenchNative {
    private static final ResourceLocation WRENCH =
            ResourceLocation.fromNamespaceAndPath("ic2", "wrench");
    private static final ResourceLocation ELECTRIC =
            ResourceLocation.fromNamespaceAndPath("ic2", "electric_wrench");

    private WrenchNative() {}

    /** Hooks the validated block-click packet after other handlers have had a chance to veto it. */
    public static void install(IEventBus bus) {
        bus.addListener(
                EventPriority.LOWEST,
                false,
                PlayerInteractEvent.RightClickBlock.class,
                WrenchNative::onBlockUse);
    }

    private static void onBlockUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getUseItem() == Event.Result.DENY) return;
        var result =
                WrenchUse.use(
                        event.getLevel(), event.getEntity(), event.getHand(), event.getHitVec());
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    static boolean isWrench(ItemStack tool) {
        if (tool.isEmpty()) return false;
        var id = ForgeRegistries.ITEMS.getKey(tool.getItem());
        return WRENCH.equals(id)
                || ELECTRIC.equals(id)
                || WorkshopPlatform.tagged(tool, "c:tools/wrench");
    }

    private static boolean electric(ItemStack tool) {
        return ELECTRIC.equals(ForgeRegistries.ITEMS.getKey(tool.getItem()));
    }

    static boolean canPay(ItemStack tool, int cost) {
        return !electric(tool) || ForgeGear.stored(tool) >= cost;
    }

    static void pay(ItemStack tool, ServerPlayer player, InteractionHand hand, int cost) {
        if (electric(tool)) ForgeGear.spend(tool, cost);
        else if (tool.isDamageableItem())
            tool.hurtAndBreak(1, player, holder -> holder.broadcastBreakEvent(hand));
    }

    static boolean mayBreak(
            ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        return ForgeHooks.onBlockBreakEvent(
                        level, player.gameMode.getGameModeForPlayer(), player, pos)
                >= 0;
    }
}
