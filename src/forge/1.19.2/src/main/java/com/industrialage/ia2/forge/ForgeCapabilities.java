package com.industrialage.ia2.forge;

import com.industrialage.ia2.platform.CapabilityFacade;
import com.industrialage.ia2.platform.RegistryFacade;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.Nullable;

/** Sided queries against loaded blocks and Forge's block-entity capability providers. */
public final class ForgeCapabilities implements CapabilityFacade {
    private static final ResourceLocation ATTACHMENT =
            new ResourceLocation("ic2", "block_capabilities");
    private final Map<String, Declaration> declarations = new HashMap<>();
    private final Map<Block, Map<Capability<?>, Declaration>> attachments = new IdentityHashMap<>();
    private final List<Runnable> pending = new ArrayList<>();
    private final Set<Level> loaded = ConcurrentHashMap.newKeySet();
    private boolean registrationClosed;

    /** Computes a currently available sided capability for a block. Null denotes absence. */
    @FunctionalInterface
    public interface BlockProvider<T> {
        /** Reads an existing block and optional block entity without changing the world. */
        @Nullable
        T get(
                Level level,
                BlockPos pos,
                BlockState state,
                @Nullable BlockEntity entity,
                @Nullable Direction side);
    }

    /** Tracks registration windows, Forge capability attachments and the lifetime of each level. */
    public ForgeCapabilities(IEventBus modBus, IEventBus gameBus) {
        // In Forge 43 the capability event precedes the block registry. Resolve handles after both.
        modBus.addListener((RegisterCapabilitiesEvent event) -> registrationClosed = true);
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(this::bindBlocks));
        gameBus.addGenericListener(BlockEntity.class, this::attach);
        gameBus.addListener(
                (LevelEvent.Load event) -> {
                    if (event.getLevel() instanceof Level level) {
                        loaded.add(level);
                    }
                });
        gameBus.addListener((LevelEvent.Unload event) -> loaded.remove(event.getLevel()));
        gameBus.addListener(
                (ServerStoppedEvent event) ->
                        loaded.removeIf(
                                level ->
                                        level instanceof ServerLevel serverLevel
                                                && serverLevel.getServer() == event.getServer()));
    }

    /** Associates an existing native block-entity capability with a common key and block. */
    public <T> void registerBlock(
            Key<T> key, Capability<T> capability, RegistryFacade.Handle<? extends Block> block) {
        queue(key, capability, null, block);
    }

    /**
     * Binds a provider to the common facade and, on blocks with entities, to Forge's native query.
     * The capability's owner registers its type with Forge.
     */
    public <T> void registerBlock(
            Key<T> key,
            Capability<T> capability,
            BlockProvider<T> provider,
            RegistryFacade.Handle<? extends Block> block) {
        queue(key, capability, Objects.requireNonNull(provider), block);
    }

    private <T> void queue(
            Key<T> key,
            Capability<T> capability,
            @Nullable BlockProvider<T> provider,
            RegistryFacade.Handle<? extends Block> block) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(capability);
        Objects.requireNonNull(block);
        if (registrationClosed) {
            throw new IllegalStateException("Block capability registration is closed");
        }
        if (!capability.getName().equals(key.valueType().getName().replace('.', '/'))) {
            throw new IllegalArgumentException("Incompatible Forge capability: " + key.name());
        }
        var declaration =
                declarations.computeIfAbsent(
                        key.name(),
                        ignored -> new Declaration(key, capability, new IdentityHashMap<>()));
        if (declaration.token() != capability || declaration.key().valueType() != key.valueType()) {
            throw new IllegalArgumentException("Conflicting capability key: " + key.name());
        }
        pending.add(
                () -> {
                    var resolved = Objects.requireNonNull(block.get());
                    if (declaration.byBlock().containsKey(resolved)) {
                        throw new IllegalArgumentException(
                                "Duplicate block capability for " + key.name());
                    }
                    declaration.byBlock().put(resolved, provider);
                    if (provider != null) {
                        var group =
                                attachments.computeIfAbsent(
                                        resolved, ignored -> new IdentityHashMap<>());
                        if (group.putIfAbsent(capability, declaration) != null) {
                            throw new IllegalArgumentException(
                                    "Duplicate Forge capability for " + key.name());
                        }
                    }
                });
    }

    private void bindBlocks() {
        pending.forEach(Runnable::run);
        pending.clear();
    }

    /** Returns a coordinate scoped to this facade and level. */
    public Target target(Level level, BlockPos position) {
        return new BlockTarget(this, Objects.requireNonNull(level), position.immutable());
    }

    @Override
    public <T> Optional<T> find(Key<T> key, Target target, Optional<Face> face) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(target);
        Objects.requireNonNull(face);
        if (!(target instanceof BlockTarget block) || block.owner() != this) {
            throw new IllegalArgumentException("Foreign block capability target");
        }
        var declaration = declarations.get(key.name());
        if (declaration == null) {
            return Optional.empty();
        }
        if (key.valueType() != declaration.key().valueType()) {
            throw new IllegalArgumentException("Incompatible capability key type: " + key.name());
        }
        var level = block.level();
        var pos = block.pos();
        if (!declaration.token().isRegistered()
                || !loaded.contains(level)
                || level.isOutsideBuildHeight(pos)) {
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
        if (!declaration.byBlock().containsKey(state.getBlock())) {
            return Optional.empty();
        }
        var side = face.map(f -> Direction.valueOf(f.name())).orElse(null);
        Object value;
        if (state.hasBlockEntity()) {
            // getBlockEntity can construct a missing entity; consult only the live chunk's map.
            var entity = chunk.getBlockEntities().get(pos);
            value =
                    entity == null || entity.isRemoved()
                            ? null
                            : entity.getCapability(declaration.token(), side).orElse(null);
        } else {
            value = declaration.provide(level, pos, state, null, side);
        }
        return Optional.ofNullable(key.valueType().cast(declaration.check(value)));
    }

    private void attach(AttachCapabilitiesEvent<BlockEntity> event) {
        var entity = event.getObject();
        var group = attachments.get(entity.getBlockState().getBlock());
        if (group != null) {
            var provider = new Attached(entity, group);
            event.addCapability(ATTACHMENT, provider);
            event.addListener(provider::invalidate);
        }
    }

    private record BlockTarget(ForgeCapabilities owner, Level level, BlockPos pos)
            implements Target {}

    private record Declaration(
            Key<?> key, Capability<?> token, Map<Block, BlockProvider<?>> byBlock) {
        private Object provide(
                Level level,
                BlockPos pos,
                BlockState state,
                @Nullable BlockEntity entity,
                @Nullable Direction side) {
            var provider = byBlock.get(state.getBlock());
            return provider == null ? null : provider.get(level, pos, state, entity, side);
        }

        private Object check(@Nullable Object value) {
            if (value != null && !key.valueType().isInstance(value)) {
                throw new IllegalStateException(
                        "Provider returned an incompatible value: " + key.name());
            }
            return value;
        }
    }

    private record Query(Capability<?> capability, @Nullable Direction side) {}

    private static final class Attached implements ICapabilityProvider {
        private final BlockEntity entity;
        private final Map<Capability<?>, Declaration> declarations;
        private final Map<Query, LazyOptional<?>> published = new HashMap<>();

        private Attached(BlockEntity entity, Map<Capability<?>, Declaration> declarations) {
            this.entity = entity;
            this.declarations = declarations;
        }

        @Override
        public <T> LazyOptional<T> getCapability(
                Capability<T> capability, @Nullable Direction side) {
            var declaration = declarations.get(capability);
            var level = entity.getLevel();
            if (declaration == null || level == null || entity.isRemoved()) {
                return LazyOptional.empty();
            }
            var value =
                    declaration.check(
                            declaration.provide(
                                    level,
                                    entity.getBlockPos(),
                                    entity.getBlockState(),
                                    entity,
                                    side));
            var query = new Query(capability, side);
            var current = published.get(query);
            if (current != null && current.orElse(null) == value) {
                return current.cast();
            }
            if (value == null) {
                if (current != null) {
                    published.remove(query);
                    current.invalidate();
                }
                return LazyOptional.empty();
            }
            var next = LazyOptional.of(() -> value);
            published.put(query, next);
            if (current != null) {
                current.invalidate();
            }
            return next.cast();
        }

        private void invalidate() {
            var previous = List.copyOf(published.values());
            published.clear();
            previous.forEach(LazyOptional::invalidate);
        }
    }
}
