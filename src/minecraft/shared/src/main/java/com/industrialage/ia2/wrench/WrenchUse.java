package com.industrialage.ia2.wrench;

import com.industrialage.ia2.content.PortableEquipment;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Applies the mechanical and electric wrench's shared hit, permission and charging rules. */
public final class WrenchUse {
    private WrenchUse() {}

    /** The authorized surface and its controls as seen under the player's crosshair. */
    public record Preview(
            WrenchSurface surface, List<WrenchSurface.Area> areas, WrenchSurface.Area selected) {
        /** Emits a camera-relative colored quad for each valid control. */
        public void draw(BlockPos pos, Vec3 camera, BiConsumer<Vec3, Integer> vertex) {
            for (var area : areas) {
                int alpha = area.equals(selected) ? 216 : 112;
                int color = alpha << 24 | (area.center() ? 0xff453f : 0x268dff);
                vertex.accept(surface.vertex(pos, area.u0(), area.v0(), camera), color);
                vertex.accept(surface.vertex(pos, area.u1(), area.v0(), camera), color);
                vertex.accept(surface.vertex(pos, area.u1(), area.v1(), camera), color);
                vertex.accept(surface.vertex(pos, area.u0(), area.v1(), camera), color);
            }
        }
    }

    /** The main hand wins; an empty main hand allows a held offhand wrench. */
    public static InteractionHand hand(Player player) {
        var main = player.getMainHandItem();
        if (WrenchNative.isWrench(main)) return InteractionHand.MAIN_HAND;
        return main.isEmpty() && WrenchNative.isWrench(player.getOffhandItem())
                ? InteractionHand.OFF_HAND
                : null;
    }

    /**
     * Builds the renderable preview from the live target using the same permission and geometry.
     */
    public static Preview preview(Level level, Player player, BlockHitResult hit) {
        var hand = hand(player);
        if (hand == null || player.isSpectator()) return null;
        var pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof WrenchTarget target)
                || !target.wrenchable(state)
                || !player.mayBuild()
                || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, hit.getDirection(), player.getItemInHand(hand))
                || !target.wrenchPermits(level, pos, player)) return null;
        var shape = state.getShape(level, pos, CollisionContext.of(player));
        var surface = WrenchSurface.find(shape, hit);
        if (surface == null) return null;
        var areas =
                surface.areas().stream()
                        .filter(area -> area.center() || target.wrenchSide(state, area.direction()))
                        .toList();
        return new Preview(surface, areas, surface.area(hit, areas));
    }

    static InteractionResult use(
            Level level, Player player, InteractionHand packetHand, BlockHitResult hit) {
        var pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        var hand = hand(player);
        if (hand == null
                || player.isSpectator()
                || !(state.getBlock() instanceof WrenchTarget target)
                || !target.wrenchable(state)) return InteractionResult.PASS;
        if (packetHand != InteractionHand.MAIN_HAND && packetHand != hand)
            return InteractionResult.FAIL;
        var preview = preview(level, player, hit);
        if (preview == null || preview.selected() == null) return InteractionResult.FAIL;
        var selected = preview.selected();
        var tool = player.getItemInHand(hand);
        int cost = selected.center() ? 1000 : PortableEquipment.ELECTRIC_WRENCH.cost;
        if (!player.isCreative() && !WrenchNative.canPay(tool, cost)) return InteractionResult.FAIL;
        if (level instanceof ServerLevel server) {
            if (!(player instanceof ServerPlayer operator)) return InteractionResult.FAIL;
            boolean changed =
                    selected.center()
                            ? target.wrenchRemove(server, pos, state, operator)
                            : target.wrenchTurn(server, pos, state, selected.direction());
            if (!changed) return InteractionResult.FAIL;
            if (!player.isCreative()) WrenchNative.pay(tool, operator, hand, cost);
        }
        return InteractionResult.SUCCESS;
    }
}
