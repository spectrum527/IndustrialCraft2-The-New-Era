package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.audio.IndustrialSounds;
import com.industrialage.ia2.content.MiningLaserModes;
import com.industrialage.ia2.content.MiningLaserModes.Mode;
import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Server-side mining laser operation and material drops. */
final class NeoLaser {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, "ic2");
    private static final DeferredHolder<EntityType<?>, EntityType<NativeLaser.Bolt>> BOLT =
            ENTITIES.register("laser_bullet", NativeLaser::entityType);
    private static final Map<ServerPlayer, Integer> COOLDOWN = new WeakHashMap<>();
    private static DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> mode;

    private NeoLaser() {}

    static void install(
            IEventBus bus, DeferredRegister<DataComponentType<?>> components, Dist side) {
        mode =
                components.register(
                        "laser_mode",
                        () ->
                                DataComponentType.<Integer>builder()
                                        .persistent(Codec.intRange(0, 7))
                                        .networkSynchronized(ByteBufCodecs.VAR_INT)
                                        .build());
        ENTITIES.register(bus);
        if (side == Dist.CLIENT) NativeLaser.Client.install(bus, BOLT);
    }

    static void use(PlayerInteractEvent.RightClickItem event) {
        var stack = event.getItemStack();
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) return;
        int index = stack.getOrDefault(mode.get(), 0);
        if (player.isShiftKeyDown()) {
            int next = MiningLaserModes.next(index);
            stack.set(mode.get(), next);
            NativeGameplay.overlay(
                    player,
                    Component.translatable(
                            "item.ic2.mining_laser.mode",
                            next + 1,
                            MiningLaserModes.get(next).cost()));
            return;
        }
        var profile = MiningLaserModes.get(index);
        var level = (ServerLevel) player.level();
        int tick = level.getServer().getTickCount();
        if (tick < COOLDOWN.getOrDefault(player, 0)) return;
        var look = player.getLookAngle();
        if (profile.levelOnly() && Math.abs(look.y) > 0.5) {
            NativeGameplay.overlay(player, Component.translatable("item.ic2.mining_laser.angle"));
            return;
        }
        double stored = PortableGear.stored(stack);
        if (!player.isCreative() && stored < profile.cost()) return;
        if (fire(level, player, look, profile)) {
            if (!player.isCreative()) PortableGear.setStored(stack, stored - profile.cost());
            COOLDOWN.put(player, tick + 5);
            IndustrialSounds.play(level, player.blockPosition(), MiningLaserModes.sound(index));
        }
    }

    private static boolean fire(ServerLevel level, ServerPlayer player, Vec3 look, Mode profile) {
        var forward =
                profile.levelOnly() ? new Vec3(look.x, 0, look.z).normalize() : look.normalize();
        var perpendicular = forward.cross(new Vec3(0, 1, 0)).normalize();
        if (perpendicular.lengthSqr() < 0.5) perpendicular = new Vec3(1, 0, 0);
        var up = perpendicular.cross(forward).normalize();
        int mid = profile.size() / 2;
        boolean added = false;
        for (int x = -mid; x <= mid; x++) {
            for (int y = -mid; y <= mid; y++) {
                var pupil = player.getEyePosition().add(forward.scale(0.35));
                var offset = perpendicular.scale(x).add(up.scale(y));
                var velocity =
                        profile.parallel() ? forward : forward.add(offset.scale(0.10)).normalize();
                if (profile.parallel()) pupil = pupil.add(offset.scale(0.86));
                var bolt = new NativeLaser.Bolt(BOLT.get(), level);
                bolt.aim(player, pupil, velocity, profile);
                added |= level.addFreshEntity(bolt);
            }
        }
        return added;
    }

    static boolean cut(ServerLevel level, BlockPos pos, ServerPlayer owner, Mode profile) {
        if (profile.blast()) {
            boolean changed = false;
            for (var nearby : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                changed |= cutOne(level, nearby, owner, false);
            }
            return changed;
        }
        return cutOne(level, pos, owner, profile.smelt());
    }

    private static boolean cutOne(
            ServerLevel level, BlockPos pos, ServerPlayer owner, boolean smelt) {
        if (!owner.mayBuild() || !level.hasChunkAt(pos) || !level.mayInteract(owner, pos))
            return false;
        var state = level.getBlockState(pos);
        if (state.isAir()
                || state.getDestroySpeed(level, pos) < 0
                || !NativeGameplay.allowLaserBreak(level, pos, state, owner)) return false;
        if (!smelt) return level.destroyBlock(pos, true, owner, Block.UPDATE_LIMIT);
        var drops =
                Block.getDrops(
                        state, level, pos, level.getBlockEntity(pos), owner, ItemStack.EMPTY);
        if (!level.destroyBlock(pos, false, owner, Block.UPDATE_LIMIT)) return false;
        for (var drop : drops) {
            var output = NativeGameplay.smeltingResult(level, drop);
            if (output.isEmpty()) Block.popResource(level, pos, drop);
            else {
                output.setCount(output.getCount() * drop.getCount());
                Block.popResource(level, pos, output);
            }
        }
        return true;
    }
}
