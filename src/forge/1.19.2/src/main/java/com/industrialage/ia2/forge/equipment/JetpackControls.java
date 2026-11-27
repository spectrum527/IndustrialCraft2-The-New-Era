package com.industrialage.ia2.forge.equipment;

import com.industrialage.ia2.equipment.ArmorEffects;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;

/** Short-lived jump input and hover mode for the currently equipped jetpack. */
final class JetpackControls {
    private final Map<ServerPlayer, Flight> flight = new WeakHashMap<>();
    private final Map<ServerPlayer, Integer> quantum = new WeakHashMap<>();

    static void install(IEventBus modBus, IEventBus gameBus) {
        var controls = new JetpackControls();
        var channel =
                NetworkRegistry.newSimpleChannel(
                        new ResourceLocation("ic2", "jetpack_input"),
                        () -> "3",
                        "3"::equals,
                        "3"::equals);
        channel.messageBuilder(Input.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(
                        (input, buffer) -> {
                            buffer.writeBoolean(input.pressed());
                            buffer.writeBoolean(input.toggleHover());
                            buffer.writeBoolean(input.quantumBoost());
                        })
                .decoder(
                        buffer ->
                                new Input(
                                        buffer.readBoolean(),
                                        buffer.readBoolean(),
                                        buffer.readBoolean()))
                .consumerMainThread(
                        (input, context) -> {
                            var player = context.get().getSender();
                            if (player != null) controls.accept(player, input);
                        })
                .add();
        gameBus.addListener(
                (TickEvent.PlayerTickEvent event) -> {
                    if (event.phase == TickEvent.Phase.END
                            && event.player instanceof ServerPlayer player) {
                        controls.tick(player);
                    }
                });
        gameBus.addListener(
                (LivingJumpEvent event) -> {
                    if (event.getEntity() instanceof ServerPlayer player
                            && controls.quantum.getOrDefault(player, -1)
                                    >= player.server.getTickCount()) {
                        ArmorEffects.boost(player);
                    }
                });
        if (FMLEnvironment.dist == Dist.CLIENT) {
            var client = new Client();
            modBus.addListener(
                    (RegisterKeyMappingsEvent event) -> {
                        event.register(client.hover);
                        event.register(client.boost);
                    });
            gameBus.addListener(
                    (TickEvent.ClientTickEvent event) -> {
                        if (event.phase == TickEvent.Phase.END) client.send(channel::sendToServer);
                    });
        }
    }

    private void accept(ServerPlayer player, Input input) {
        if (input.quantumBoost() && ArmorEffects.hasBoostBoots(player))
            quantum.put(player, player.server.getTickCount() + 12);
        else quantum.remove(player);
        if (!ForgeGear.hasJetpack(player)) {
            flight.remove(player);
            return;
        }
        var previous = flight.get(player);
        boolean hover = (previous != null && previous.hover()) ^ input.toggleHover();
        flight.put(
                player, new Flight(input.pressed() ? player.server.getTickCount() + 12 : 0, hover));
        if (input.toggleHover())
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            hover ? "item.ic2.jetpack.hover_on" : "item.ic2.jetpack.hover_off"),
                    true);
    }

    private void tick(ServerPlayer player) {
        var state = flight.get(player);
        if (state == null) return;
        if (!ForgeGear.hasJetpack(player)) {
            flight.remove(player);
            return;
        }
        boolean thrust = state.until() != 0 && player.server.getTickCount() <= state.until();
        if (!thrust && !state.hover()) {
            flight.remove(player);
            return;
        }
        if ((!thrust && player.isOnGround())
                || player.isPassenger()
                || player.isSpectator()
                || player.isUnderWater()
                || player.getAbilities().flying) {
            if (thrust) flight.put(player, new Flight(0, state.hover()));
            return;
        }
        if (!ForgeGear.payForFlight(player, state.hover())) {
            if (thrust) flight.put(player, new Flight(0, state.hover()));
            return;
        }
        var velocity = player.getDeltaMovement();
        double vertical = state.hover() ? (thrust ? 0.22 : 0.0) : Math.min(0.45, velocity.y + 0.16);
        player.setDeltaMovement(velocity.x, vertical, velocity.z);
        player.resetFallDistance();
        player.hurtMarked = true;
    }

    private record Flight(int until, boolean hover) {}

    private record Input(boolean pressed, boolean toggleHover, boolean quantumBoost) {}

    private static final class Client {
        private final KeyMapping hover =
                new KeyMapping(
                        "key.ic2.jetpack_hover",
                        InputConstants.UNKNOWN.getValue(),
                        KeyMapping.CATEGORY_MISC);
        private final KeyMapping boost =
                new KeyMapping(
                        "key.ic2.quantum_boost",
                        InputConstants.UNKNOWN.getValue(),
                        KeyMapping.CATEGORY_MISC);
        private boolean sent;
        private boolean boostSent;
        private int interval;

        void send(java.util.function.Consumer<Input> send) {
            var client = Minecraft.getInstance();
            var player = client.player;
            if (player == null || client.getConnection() == null) {
                sent = false;
                boostSent = false;
                interval = 0;
                return;
            }
            boolean pressed = client.options.keyJump.isDown() && ForgeGear.hasJetpack(player);
            boolean toggle = hover.consumeClick() && ForgeGear.hasJetpack(player);
            boolean boosted = boost.isDown() && ArmorEffects.hasBoostBoots(player);
            if (toggle
                    || pressed != sent
                    || boosted != boostSent
                    || (pressed || boosted) && ++interval >= 5) {
                send.accept(new Input(pressed, toggle, boosted));
                sent = pressed;
                boostSent = boosted;
                interval = 0;
            }
        }
    }
}
