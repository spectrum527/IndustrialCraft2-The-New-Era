package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.equipment.ArmorEffects;
import com.industrialage.ia2.platform.NeoForgeVersion;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Short-lived jump input and hover mode for the currently equipped jetpack. */
final class JetpackControls {
    private final Map<ServerPlayer, Flight> flight = new WeakHashMap<>();
    private final Map<ServerPlayer, Integer> quantum = new WeakHashMap<>();

    static void install(IEventBus modBus, IEventBus gameBus, Dist side) {
        var controls = new JetpackControls();
        modBus.addListener(
                RegisterPayloadHandlersEvent.class,
                event ->
                        event.registrar("3")
                                .playToServer(
                                        Input.TYPE,
                                        Input.CODEC,
                                        (input, context) -> {
                                            if (context.player() instanceof ServerPlayer player) {
                                                controls.accept(player, input);
                                            }
                                        }));
        gameBus.addListener(
                PlayerTickEvent.Post.class,
                event -> {
                    if (event.getEntity() instanceof ServerPlayer player) controls.tick(player);
                });
        gameBus.addListener(
                LivingJumpEvent.class,
                event -> {
                    if (event.getEntity() instanceof ServerPlayer player
                            && controls.quantum.getOrDefault(player, -1) >= serverTick(player)) {
                        ArmorEffects.boost(player);
                    }
                });
        if (side == Dist.CLIENT) {
            var client = new Client();
            modBus.addListener(
                    RegisterKeyMappingsEvent.class,
                    event -> {
                        event.register(client.hover);
                        event.register(client.boost);
                    });
            gameBus.addListener(ClientTickEvent.Post.class, client::sendInput);
        }
    }

    private void accept(ServerPlayer player, Input input) {
        if (input.quantumBoost() && ArmorEffects.hasBoostBoots(player))
            quantum.put(player, serverTick(player) + 12);
        else quantum.remove(player);
        if (!PortableGear.hasJetpack(player)) {
            flight.remove(player);
            return;
        }
        var previous = flight.get(player);
        boolean hover = (previous != null && previous.hover()) ^ input.toggleHover();
        flight.put(player, new Flight(input.pressed() ? serverTick(player) + 12 : 0, hover));
        if (input.toggleHover())
            NativeGameplay.overlay(
                    player,
                    net.minecraft.network.chat.Component.translatable(
                            hover ? "item.ic2.jetpack.hover_on" : "item.ic2.jetpack.hover_off"));
    }

    private void tick(ServerPlayer player) {
        var state = flight.get(player);
        if (state == null) return;
        boolean thrust = state.until() != 0 && serverTick(player) <= state.until();
        if ((!thrust && (!state.hover() || player.onGround()))
                || player.isPassenger()
                || player.isSpectator()
                || player.isUnderWater()
                || player.getAbilities().flying
                || !PortableGear.payForFlight(player, state.hover())) {
            flight.remove(player);
            return;
        }
        var velocity = player.getDeltaMovement();
        double vertical = state.hover() ? (thrust ? 0.22 : 0.0) : Math.min(0.45, velocity.y + 0.16);
        player.setDeltaMovement(velocity.x, vertical, velocity.z);
        player.resetFallDistance();
        player.hurtMarked = true;
    }

    private static int serverTick(ServerPlayer player) {
        return ((ServerLevel) player.level()).getServer().getTickCount();
    }

    private record Flight(int until, boolean hover) {}

    private record Input(boolean pressed, boolean toggleHover, boolean quantumBoost)
            implements CustomPacketPayload {
        static final Type<Input> TYPE = NativeGameplay.packetType("jetpack_input");
        static final StreamCodec<RegistryFriendlyByteBuf, Input> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL,
                        Input::pressed,
                        ByteBufCodecs.BOOL,
                        Input::toggleHover,
                        ByteBufCodecs.BOOL,
                        Input::quantumBoost,
                        Input::new);

        @Override
        public Type<Input> type() {
            return TYPE;
        }
    }

    private static final class Client {
        private final net.minecraft.client.KeyMapping hover =
                NeoForgeVersion.Client.gameplayKey("key.ic2.jetpack_hover");
        private final net.minecraft.client.KeyMapping boost =
                NeoForgeVersion.Client.gameplayKey("key.ic2.quantum_boost");
        private boolean sent;
        private boolean boostSent;
        private int interval;

        void sendInput(ClientTickEvent.Post event) {
            var client = Minecraft.getInstance();
            var player = client.player;
            var connection = client.getConnection();
            if (player == null
                    || connection == null
                    || !connection.hasChannel(new Input(false, false, false))) {
                sent = false;
                boostSent = false;
                interval = 0;
                return;
            }
            boolean pressed = client.options.keyJump.isDown() && PortableGear.hasJetpack(player);
            boolean toggle = hover.consumeClick() && PortableGear.hasJetpack(player);
            boolean boosted = boost.isDown() && ArmorEffects.hasBoostBoots(player);
            if (toggle
                    || pressed != sent
                    || boosted != boostSent
                    || (pressed || boosted) && ++interval >= 5) {
                connection.send(new Input(pressed, toggle, boosted));
                interval = 0;
                sent = pressed;
                boostSent = boosted;
            }
        }
    }
}
