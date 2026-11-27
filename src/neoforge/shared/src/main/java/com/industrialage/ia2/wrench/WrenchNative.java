package com.industrialage.ia2.wrench;

import com.industrialage.ia2.gameplay.PortableGear;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Native permissions and tool costs for the single wrench interaction path. */
public final class WrenchNative {
    private WrenchNative() {}

    /** Hooks the validated block-click packet after other handlers have had a chance to veto it. */
    public static void install(IEventBus bus) {
        bus.addListener(
                EventPriority.LOWEST,
                PlayerInteractEvent.RightClickBlock.class,
                WrenchNative::onBlockUse);
    }

    private static void onBlockUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getUseItem().isFalse()) return;
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
        var id = BuiltInRegistries.ITEM.getKey(tool.getItem());
        return (id.getNamespace().equals("ic2")
                        && (id.getPath().equals("wrench")
                                || id.getPath().equals("electric_wrench")))
                || WorkshopPlatform.tagged(tool, "c:tools/wrench");
    }

    private static boolean electric(ItemStack tool) {
        var id = BuiltInRegistries.ITEM.getKey(tool.getItem());
        return id.getNamespace().equals("ic2") && id.getPath().equals("electric_wrench");
    }

    static boolean canPay(ItemStack tool, int cost) {
        return !electric(tool) || PortableGear.stored(tool) >= cost;
    }

    static void pay(ItemStack tool, ServerPlayer player, InteractionHand hand, int cost) {
        if (electric(tool)) PortableGear.spend(tool, cost);
        else if (tool.isDamageableItem())
            tool.hurtAndBreak(
                    1,
                    player,
                    hand == InteractionHand.MAIN_HAND
                            ? EquipmentSlot.MAINHAND
                            : EquipmentSlot.OFFHAND);
    }

    static boolean mayBreak(
            ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        return !CommonHooks.fireBlockBreak(
                        level, player.gameMode.getGameModeForPlayer(), player, pos, state)
                .isCanceled();
    }
}
