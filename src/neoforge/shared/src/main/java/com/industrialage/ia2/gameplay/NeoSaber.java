package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.audio.SaberAudio;
import com.industrialage.ia2.content.PortableEquipment;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Powered saber transitions, charge use and standard game combat attributes. */
final class NeoSaber {
    private static final int SWING = 400;
    private static final int IDLE = 64;
    private static final int START_THRESHOLD = 16;
    private static Supplier<DataComponentType<Boolean>> state;

    private NeoSaber() {}

    static void install(
            IEventBus modBus,
            IEventBus gameBus,
            DeferredRegister<DataComponentType<?>> components,
            Dist side) {
        state =
                components.register(
                        "saber_active",
                        () ->
                                DataComponentType.<Boolean>builder()
                                        .persistent(Codec.BOOL)
                                        .networkSynchronized(ByteBufCodecs.BOOL)
                                        .build());
        gameBus.addListener(
                ItemAttributeModifierEvent.class,
                event -> {
                    var item = event.getItemStack();
                    if (PortableGear.type(item) == PortableEquipment.NANO_SABER) {
                        event.addModifier(
                                Attributes.ATTACK_DAMAGE,
                                poweredStrike(item)
                                        ? NativeSaber.ACTIVE_DAMAGE
                                        : NativeSaber.IDLE_DAMAGE,
                                EquipmentSlotGroup.MAINHAND);
                        event.addModifier(
                                Attributes.ATTACK_SPEED,
                                NativeSaber.SPEED,
                                EquipmentSlotGroup.MAINHAND);
                    }
                });
        gameBus.addListener(LivingIncomingDamageEvent.class, NeoSaber::onHit);
        if (side == Dist.CLIENT) NativeSaber.Client.install(modBus);
    }

    static boolean active(ItemStack stack) {
        return PortableGear.type(stack) == PortableEquipment.NANO_SABER
                && stack.getOrDefault(state.get(), false);
    }

    private static boolean poweredStrike(ItemStack stack) {
        return active(stack) && PortableGear.stored(stack) >= SWING;
    }

    static void use(PlayerInteractEvent.RightClickItem event) {
        var stack = event.getItemStack();
        if (!active(stack)
                && !event.getEntity().isCreative()
                && PortableGear.stored(stack) < START_THRESHOLD) return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) return;
        if (active(stack)) stack.remove(state.get());
        else {
            stack.set(state.get(), true);
            SaberAudio.play((ServerLevel) player.level(), player, "power_up");
        }
    }

    static void tick(ServerPlayer player, ItemStack stack) {
        if (!player.isCreative()
                && player.level().getGameTime() % 16 == 0
                && active(stack)
                && !PortableGear.spend(stack, IDLE)) {
            stack.remove(state.get());
        }
    }

    private static void onHit(LivingIncomingDamageEvent event) {
        var source = event.getSource();
        if (!source.is(DamageTypes.PLAYER_ATTACK)
                || !(source.getEntity() instanceof ServerPlayer player)) return;
        var stack = player.getMainHandItem();
        if (!poweredStrike(stack)) return;
        if (!player.isCreative() && !PortableGear.spend(stack, SWING)) {
            stack.remove(state.get());
            return;
        }
        var level = (ServerLevel) player.level();
        SaberAudio.play(level, player, level.getRandom().nextBoolean() ? "swing1" : "swing2");
    }
}
