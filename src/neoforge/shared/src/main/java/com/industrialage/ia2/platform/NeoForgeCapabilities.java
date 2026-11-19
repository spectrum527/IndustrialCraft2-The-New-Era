package com.industrialage.ia2.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.IBlockCapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Sided NeoForge capability bindings and live block queries. */
public final class NeoForgeCapabilities implements CapabilityFacade {
    private final Map<String, BlockCapability<?, Direction>> declarations = new HashMap<>();
    private final List<Consumer<RegisterCapabilitiesEvent>> providers = new ArrayList<>();
    private final Set<Level> loadedLevels = ConcurrentHashMap.newKeySet();
    private boolean registrationClosed;

    /** Attaches provider registration and level tracking to the corresponding host buses. */
    public NeoForgeCapabilities(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(this::bindProviders);
        gameBus.addListener(this::levelLoaded);
        gameBus.addListener(this::levelUnloaded);
        gameBus.addListener(
                ServerStoppedEvent.class,
                event ->
                        loadedLevels.removeIf(
                                level ->
                                        level instanceof ServerLevel serverLevel
                                                && serverLevel.getServer() == event.getServer()));
    }

    /** Queues one provider for a registered block and associates the host capability with a key. */
    public <T> void registerBlock(
            Key<T> key,
            BlockCapability<T, Direction> capability,
            IBlockCapabilityProvider<T, Direction> provider,
            RegistryFacade.Handle<? extends Block> block) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(block, "block");
        if (registrationClosed) {
            throw new IllegalStateException("Block capability registration is closed");
        }
        if (capability.typeClass() != key.valueType()
                || capability.contextClass() != Direction.class) {
            throw new IllegalArgumentException("Incompatible host capability: " + key.name());
        }
        var declared = declarations.putIfAbsent(key.name(), capability);
        if (declared != null && declared != capability) {
            throw new IllegalArgumentException("Conflicting capability declaration: " + key.name());
        }
        providers.add(event -> event.registerBlock(capability, provider, block.get()));
    }

    /** {@return a coordinate in a particular level, scoped to this adapter} */
    public Target target(Level level, BlockPos position) {
        return new BlockTarget(this, Objects.requireNonNull(level, "level"), position.immutable());
    }

    @Override
    public <T> Optional<T> find(Key<T> key, Target target, Optional<Face> face) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(face, "face");
        if (!(target instanceof BlockTarget block) || block.owner() != this) {
            throw new IllegalArgumentException("Foreign block capability target");
        }
        var capability = declarations.get(key.name());
        if (capability == null) {
            return Optional.empty();
        }
        if (capability.typeClass() != key.valueType()) {
            throw new IllegalArgumentException("Incompatible capability key type: " + key.name());
        }

        var level = block.level();
        var pos = block.position();
        if (!loadedLevels.contains(level) || level.isOutsideBuildHeight(pos)) {
            return Optional.empty();
        }
        var chunk =
                level.getChunkSource()
                        .getChunkNow(
                                SectionPos.blockToSectionCoord(pos.getX()),
                                SectionPos.blockToSectionCoord(pos.getZ()));
        if (chunk == null) {
            return Optional.empty();
        }
        var state = chunk.getBlockState(pos);
        BlockEntity entity = null;
        if (state.hasBlockEntity()) {
            entity = chunk.getBlockEntities().get(pos);
            // A null entity would make NeoForge fetch it from the level.
            if (entity == null || entity.isRemoved()) {
                return Optional.empty();
            }
        }
        var direction = face.map(value -> Direction.valueOf(value.name())).orElse(null);
        var value = level.getCapability(capability, pos, state, entity, direction);
        if (value != null && !key.valueType().isInstance(value)) {
            throw new IllegalStateException(
                    "Provider returned an incompatible value: " + key.name());
        }
        return Optional.ofNullable(key.valueType().cast(value));
    }

    private void bindProviders(RegisterCapabilitiesEvent event) {
        registrationClosed = true;
        providers.forEach(bind -> bind.accept(event));
        providers.clear();
    }

    private void levelLoaded(LevelEvent.Load event) {
        if (event.getLevel() instanceof Level level) {
            loadedLevels.add(level);
        }
    }

    private void levelUnloaded(LevelEvent.Unload event) {
        loadedLevels.remove(event.getLevel());
    }

    private record BlockTarget(NeoForgeCapabilities owner, Level level, BlockPos position)
            implements Target {}
}
