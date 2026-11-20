package com.industrialage.ia2.platform;

import com.industrialage.ia2.machine.NeoForgeMachineBlockEntity;
import com.industrialage.ia2.network.EchoProtocol;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Codec;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

/** Bindings to host APIs that differ across the supported NeoForge versions. */
public final class NeoForgeVersion {
    private NeoForgeVersion() {}

    /** Looks up a stack's smelting fuel value using this game's active fuel API. */
    public static int furnaceBurnTime(ItemStack stack, Level level) {
        return stack.getBurnTime(RecipeType.SMELTING, level.fuelValues());
    }

    /** Connects a machine's portable save format to the native block entity load and save hooks. */
    public abstract static class MachineEntity<S> extends NeoForgeMachineBlockEntity {
        private final SavedState<S> format;

        /** Constructs a persistent host for one type of machine snapshot. */
        protected MachineEntity(
                BlockEntityType<?> type, BlockPos pos, BlockState state, SavedState<S> format) {
            super(type, pos, state);
            this.format = format;
        }

        /** Captures the machine's current persistent state. */
        protected abstract S savedState();

        /** Adopts the complete snapshot read from the native save. */
        protected abstract void restoreState(S snapshot);

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.store(format.key(), format.codec(), savedState());
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            restoreState(format.decode(input.read(format.key(), Codec.PASSTHROUGH), getBlockPos()));
        }
    }

    static boolean canReload(CommandSourceStack source) {
        return Commands.LEVEL_GAMEMASTERS.check(source.permissions());
    }

    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String fullId) {
        return new CustomPacketPayload.Type<>(Identifier.parse(fullId));
    }

    /** {@return the namespaced identifier of the level's dimension} */
    public static String dimension(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    static void installPayloadRegistration(
            IEventBus modBus, Dist side, Supplier<NeoForgeNetwork.Bindings> bindings) {
        modBus.addListener(
                RegisterPayloadHandlersEvent.class,
                event -> {
                    var registrar =
                            event.registrar(EchoProtocol.VERSION).executesOn(HandlerThread.MAIN);
                    var packets = bindings.get();
                    for (var packet : packets.serverbound()) {
                        registrar.playToServer(packet.type(), packet.codec(), packet.handler());
                    }
                    for (var packet : packets.clientbound()) {
                        registrar.playToClient(packet.type(), packet.codec());
                    }
                });
        if (side == Dist.CLIENT) {
            Client.installPayloadReceivers(modBus, bindings);
        }
    }

    /** Host bindings used by the physical client after side selection. */
    public static final class Client {
        private Client() {}

        private static void installPayloadReceivers(
                IEventBus modBus, Supplier<NeoForgeNetwork.Bindings> bindings) {
            modBus.addListener(
                    RegisterClientPayloadHandlersEvent.class,
                    event -> {
                        for (var packet : bindings.get().clientbound()) {
                            event.register(packet.type(), HandlerThread.MAIN, packet.handler());
                        }
                    });
        }

        /** {@return an unbound key mapping active in gameplay} */
        public static KeyMapping gameplayKey(String translationKey) {
            return new KeyMapping(
                    translationKey,
                    KeyConflictContext.IN_GAME,
                    InputConstants.UNKNOWN,
                    KeyMapping.Category.MISC);
        }

        /** Opens the named resource from the currently selected client resource packs. */
        public static BufferedReader openResource(ResourceManager manager, String resourceId)
                throws IOException {
            return manager.openAsReader(Identifier.parse(resourceId));
        }

        /** Registers a named reload listener in this version's client setup window. */
        public static void registerReload(
                IEventBus modBus, String listenerId, PreparableReloadListener listener) {
            var id = Identifier.parse(listenerId);
            modBus.addListener(
                    AddClientReloadListenersEvent.class, event -> event.addListener(id, listener));
        }
    }
}
