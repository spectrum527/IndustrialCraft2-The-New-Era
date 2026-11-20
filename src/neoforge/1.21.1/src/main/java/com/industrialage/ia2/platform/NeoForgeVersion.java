package com.industrialage.ia2.platform;

import com.industrialage.ia2.machine.NeoForgeMachineBlockEntity;
import com.industrialage.ia2.network.EchoProtocol;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Dynamic;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

/** Bindings to host APIs that differ across the supported NeoForge versions. */
public final class NeoForgeVersion {
    private NeoForgeVersion() {}

    /** Looks up a stack's smelting fuel value using this game's active fuel API. */
    public static int furnaceBurnTime(ItemStack stack, Level level) {
        return stack.getBurnTime(RecipeType.SMELTING);
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
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.put(
                    format.key(),
                    format.codec().encodeStart(NbtOps.INSTANCE, savedState()).getOrThrow());
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            var raw =
                    Optional.ofNullable(tag.get(format.key()))
                            .<Dynamic<?>>map(value -> new Dynamic<>(NbtOps.INSTANCE, value));
            restoreState(format.decode(raw, getBlockPos()));
        }
    }

    static boolean canReload(CommandSourceStack source) {
        return source.hasPermission(2);
    }

    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String fullId) {
        return new CustomPacketPayload.Type<>(ResourceLocation.parse(fullId));
    }

    /** {@return the namespaced identifier of the level's dimension} */
    public static String dimension(ServerLevel level) {
        return level.dimension().location().toString();
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
                        registrar.playToClient(packet.type(), packet.codec(), packet.handler());
                    }
                });
    }

    /** Host bindings used by the physical client after side selection. */
    public static final class Client {
        private Client() {}

        /** {@return an unbound key mapping active in gameplay} */
        public static KeyMapping gameplayKey(String translationKey) {
            return new KeyMapping(
                    translationKey,
                    KeyConflictContext.IN_GAME,
                    InputConstants.UNKNOWN,
                    KeyMapping.CATEGORY_MISC);
        }

        /** Opens the named resource from the currently selected client resource packs. */
        public static BufferedReader openResource(ResourceManager manager, String resourceId)
                throws IOException {
            return manager.openAsReader(ResourceLocation.parse(resourceId));
        }

        /** Registers a reload listener in this version's client setup window. */
        public static void registerReload(
                IEventBus modBus, String listenerId, PreparableReloadListener listener) {
            modBus.addListener(
                    RegisterClientReloadListenersEvent.class,
                    event -> event.registerReloadListener(listener));
        }
    }
}
