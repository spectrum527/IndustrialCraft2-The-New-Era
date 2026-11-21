package com.industrialage.ia2.industrial;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

/** Incremental loaded-water search with a cached, verifiable natural-surface witness. */
final class WaterConnectivity {
    private static final int POOL_KU = 4;
    private static final int SEARCH_LIMIT = 262_144;
    private static final int SEARCH_PER_TICK = 128;
    private static final int SURFACE_PER_TICK = 64;
    private static final int RECHECK_PER_TICK = 128;
    private static final int MIN_SURFACE_COLUMNS = 64;
    private static final int MIN_SURFACE_SPAN = 16;
    private static final Direction[] NEIGHBORS = {
        Direction.DOWN,
        Direction.UP,
        Direction.NORTH,
        Direction.SOUTH,
        Direction.WEST,
        Direction.EAST
    };
    private static final Direction[] HORIZONTAL = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    private static final WeakHashMap<ServerLevel, TerrainCache> TERRAIN = new WeakHashMap<>();

    private Long2LongOpenHashMap parents;
    private LongArrayFIFOQueue pending;
    private LongArrayList visited;
    private Long2LongOpenHashMap boundary;
    private LongArrayList boundaryOrder;
    private LongOpenHashSet rejectedSurface;
    private LongArrayList path;
    private LongArrayList witness;
    private LongArrayList candidatePath;
    private LongArrayList candidateWitness;
    private long candidateCursor;
    private int candidateRate;
    private SurfaceCheck surface;
    private long origin = Long.MIN_VALUE;
    private int granted;
    private int pathCursor;
    private int witnessCursor;
    private int visitedCursor;
    private int boundaryCursor;

    int available(WorkshopNode node, ServerLevel level) {
        long start = node.getBlockPos().relative(WindRotorMount.face(node)).asLong();
        if (origin != start) reset(start);
        if (granted > 0) {
            if (validCached(level)) return granted;
            reset(start);
        }
        var terrain = TERRAIN.computeIfAbsent(level, ignored -> new TerrainCache());
        if (candidatePath != null) {
            for (int count = 0; count < RECHECK_PER_TICK; count++) {
                if (!loadedWater(level, BlockPos.of(candidateCursor))) {
                    reset(start);
                    return POOL_KU;
                }
                candidatePath.add(candidateCursor);
                long previous = parents.get(candidateCursor);
                if (previous == candidateCursor) {
                    path = candidatePath;
                    witness = candidateWitness;
                    granted = candidateRate;
                    candidatePath = null;
                    candidateWitness = null;
                    parents = null;
                    pending = null;
                    visited = null;
                    boundary = null;
                    boundaryOrder = null;
                    rejectedSurface = null;
                    return granted;
                }
                candidateCursor = previous;
            }
            return POOL_KU;
        }
        if (surface != null) {
            int result = surface.step(level, terrain);
            if (result > 0) {
                candidatePath = new LongArrayList();
                candidateWitness = surface.accepted;
                candidateCursor = surface.anchor;
                candidateRate = result;
                surface = null;
                return POOL_KU;
            }
            if (result == 0) {
                rejectedSurface.addAll(surface.acceptedColumns);
                surface = null;
            } else {
                return POOL_KU;
            }
        }
        for (int count = 0; count < SEARCH_PER_TICK && !pending.isEmpty(); count++) {
            long packed = pending.dequeueLong();
            var pos = BlockPos.of(packed);
            if (!loadedWater(level, pos)) {
                reset(start);
                return POOL_KU;
            }
            for (var side : NEIGHBORS) consider(level, pos.relative(side), packed);
            int sea = level.getChunkSource().getGenerator().getSeaLevel();
            int rate = surfaceRate(level, pos, sea);
            long column = columnKey(pos.getX(), pos.getZ());
            if (rate > 0 && !rejectedSurface.contains(column)) {
                surface = new SurfaceCheck(packed, rate, sea);
                break;
            }
        }
        if (pending.isEmpty() && surface == null) pollStableBoundary(level, start);
        return POOL_KU;
    }

    private void reset(long start) {
        origin = start;
        parents = new Long2LongOpenHashMap();
        pending = new LongArrayFIFOQueue();
        visited = new LongArrayList();
        boundary = new Long2LongOpenHashMap();
        boundaryOrder = new LongArrayList();
        rejectedSurface = new LongOpenHashSet();
        path = null;
        witness = null;
        candidatePath = null;
        candidateWitness = null;
        surface = null;
        granted = 0;
        pathCursor = witnessCursor = visitedCursor = boundaryCursor = 0;
        parents.put(start, start);
        visited.add(start);
        pending.enqueue(start);
    }

    private void consider(ServerLevel level, BlockPos pos, long from) {
        long packed = pos.asLong();
        if (parents.containsKey(packed)) return;
        if (!loadedWater(level, pos) || parents.size() >= SEARCH_LIMIT) {
            if (!boundary.containsKey(packed)) {
                boundary.put(packed, from);
                boundaryOrder.add(packed);
            }
            return;
        }
        parents.put(packed, from);
        visited.add(packed);
        pending.enqueue(packed);
    }

    private void pollStableBoundary(ServerLevel level, long start) {
        for (int count = 0; count < RECHECK_PER_TICK / 2 && count < visited.size(); count++) {
            if (visitedCursor >= visited.size()) visitedCursor = 0;
            long key = visited.getLong(visitedCursor++);
            var pos = BlockPos.of(key);
            if (level.hasChunkAt(pos) && !level.getFluidState(pos).is(FluidTags.WATER)) {
                reset(start);
                return;
            }
        }
        for (int count = 0; count < RECHECK_PER_TICK / 2 && count < boundaryOrder.size(); count++) {
            if (boundaryCursor >= boundaryOrder.size()) boundaryCursor = 0;
            long key = boundaryOrder.getLong(boundaryCursor++);
            if (!boundary.containsKey(key) || !loadedWater(level, BlockPos.of(key))) continue;
            long from = boundary.get(key);
            if (!loadedWater(level, BlockPos.of(from))) {
                reset(start);
                return;
            }
            if (parents.size() >= SEARCH_LIMIT) return;
            boundary.remove(key);
            rejectedSurface.clear();
            parents.put(key, from);
            visited.add(key);
            pending.enqueue(key);
        }
        if (boundaryOrder.size() > boundary.size() * 2L + 1024) {
            var compact = new LongArrayList(boundary.size());
            for (long key : boundaryOrder) if (boundary.containsKey(key)) compact.add(key);
            boundaryOrder.clear();
            boundaryOrder.addAll(compact);
            boundaryCursor = 0;
        }
    }

    private boolean validCached(ServerLevel level) {
        for (int count = 0; count < RECHECK_PER_TICK / 2 && count < path.size(); count++) {
            if (pathCursor >= path.size()) pathCursor = 0;
            if (!loadedWater(level, BlockPos.of(path.getLong(pathCursor++)))) return false;
        }
        int sea = level.getChunkSource().getGenerator().getSeaLevel();
        for (int count = 0; count < RECHECK_PER_TICK / 2 && count < witness.size(); count++) {
            if (witnessCursor >= witness.size()) witnessCursor = 0;
            var pos = BlockPos.of(witness.getLong(witnessCursor++));
            if (surfaceRate(level, pos, sea) == 0) return false;
        }
        var anchor = BlockPos.of(path.getLong(0));
        return surfaceRate(level, anchor, sea) == granted;
    }

    private static boolean loadedWater(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getFluidState(pos).is(FluidTags.WATER);
    }

    private static int surfaceRate(ServerLevel level, BlockPos pos, int sea) {
        if (!level.hasChunkAt(pos) || !level.getFluidState(pos).is(FluidTags.WATER)) return 0;
        if (pos.getY() != sea - 1 && (pos.getY() != sea - 2 || !isIce(level, pos.above())))
            return 0;
        var biome = level.getBiome(pos);
        if (biome.is(Biomes.DEEP_OCEAN)
                || biome.is(Biomes.DEEP_COLD_OCEAN)
                || biome.is(Biomes.DEEP_FROZEN_OCEAN)
                || biome.is(Biomes.DEEP_LUKEWARM_OCEAN)) return 400;
        if (biome.is(Biomes.OCEAN)
                || biome.is(Biomes.COLD_OCEAN)
                || biome.is(Biomes.FROZEN_OCEAN)
                || biome.is(Biomes.WARM_OCEAN)
                || biome.is(Biomes.LUKEWARM_OCEAN)) return 300;
        if (biome.is(Biomes.RIVER) || biome.is(Biomes.FROZEN_RIVER)) return 150;
        return 0;
    }

    private static boolean isIce(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.FROSTED_ICE);
    }

    private static long columnKey(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    private static final class SurfaceCheck {
        final long anchor;
        final int rate;
        final int sea;
        final LongArrayFIFOQueue pending = new LongArrayFIFOQueue();
        final LongOpenHashSet queued = new LongOpenHashSet();
        final Long2LongOpenHashMap preceding = new Long2LongOpenHashMap();
        final LongOpenHashSet acceptedColumns = new LongOpenHashSet();
        final LongArrayList accepted = new LongArrayList();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        SurfaceCheck(long anchor, int rate, int sea) {
            this.anchor = anchor;
            this.rate = rate;
            this.sea = sea;
            var position = BlockPos.of(anchor);
            queued.add(anchor);
            preceding.put(anchor, anchor);
            pending.enqueue(anchor);
        }

        int step(ServerLevel level, TerrainCache terrain) {
            for (int count = 0; count < SURFACE_PER_TICK && !pending.isEmpty(); count++) {
                long current = pending.firstLong();
                var original = BlockPos.of(current);
                var top = new BlockPos(original.getX(), sea - 1, original.getZ());
                if (!level.hasChunkAt(top)) {
                    pending.dequeueLong();
                    continue;
                }
                BlockPos actual =
                        loadedWater(level, top)
                                ? top
                                : isIce(level, top) && loadedWater(level, top.below())
                                        ? top.below()
                                        : null;
                long column = columnKey(original.getX(), original.getZ());
                var from = BlockPos.of(preceding.get(current));
                int sharedY = actual == null ? sea - 1 : Math.min(actual.getY(), from.getY());
                boolean connected =
                        actual != null
                                && loadedWater(
                                        level, new BlockPos(from.getX(), sharedY, from.getZ()))
                                && loadedWater(
                                        level, new BlockPos(actual.getX(), sharedY, actual.getZ()));
                if (!connected
                        || acceptedColumns.contains(column)
                        || surfaceRate(level, actual, sea) == 0) {
                    pending.dequeueLong();
                    continue;
                }
                Boolean natural = terrain.isNaturalWater(level, actual.getX(), actual.getZ(), sea);
                if (natural == null) return -1;
                pending.dequeueLong();
                if (!natural) continue;
                accepted.add(actual.asLong());
                acceptedColumns.add(columnKey(actual.getX(), actual.getZ()));
                minX = Math.min(minX, actual.getX());
                maxX = Math.max(maxX, actual.getX());
                minZ = Math.min(minZ, actual.getZ());
                maxZ = Math.max(maxZ, actual.getZ());
                if (accepted.size() >= MIN_SURFACE_COLUMNS
                        && (maxX - minX + 1 >= MIN_SURFACE_SPAN
                                || maxZ - minZ + 1 >= MIN_SURFACE_SPAN)) return rate;
                if (queued.size() >= SEARCH_LIMIT) continue;
                for (var side : HORIZONTAL) enqueue(level, actual.relative(side), actual);
            }
            return pending.isEmpty() ? 0 : -1;
        }

        private void enqueue(ServerLevel level, BlockPos pos, BlockPos from) {
            var top = new BlockPos(pos.getX(), sea - 1, pos.getZ());
            if (!level.hasChunkAt(top)) return;
            BlockPos actual =
                    loadedWater(level, top)
                            ? top
                            : isIce(level, top) && loadedWater(level, top.below())
                                    ? top.below()
                                    : null;
            if (actual == null) return;
            int sharedY = Math.min(actual.getY(), from.getY());
            if (!loadedWater(level, new BlockPos(from.getX(), sharedY, from.getZ()))
                    || !loadedWater(level, new BlockPos(actual.getX(), sharedY, actual.getZ())))
                return;
            long key = actual.asLong();
            if (queued.add(key)) {
                preceding.put(key, from.asLong());
                pending.enqueue(key);
            }
        }
    }

    private static final class TerrainCache {
        private static final int MAX_COLUMNS = 32_768;
        private static final int PROBES_PER_TICK = 8;
        private final Map<Long, Boolean> columns =
                new LinkedHashMap<>(128, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Long, Boolean> entry) {
                        return size() > MAX_COLUMNS;
                    }
                };
        private long tick = Long.MIN_VALUE;
        private int probes;

        Boolean isNaturalWater(ServerLevel level, int x, int z, int sea) {
            long key = columnKey(x, z);
            Boolean known = columns.get(key);
            if (known != null) return known;
            if (tick != level.getGameTime()) {
                tick = level.getGameTime();
                probes = 0;
            }
            if (probes >= PROBES_PER_TICK) return null;
            probes++;
            var chunks = level.getChunkSource();
            boolean result =
                    chunks.getGenerator()
                            .getBaseColumn(x, z, level, chunks.randomState())
                            .getBlock(sea - 1)
                            .getFluidState()
                            .is(FluidTags.WATER);
            columns.put(key, result);
            return result;
        }
    }
}
