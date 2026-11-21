package com.industrialage.ia2.watertest;

import com.industrialage.ia2.industrial.PowerNode;
import com.industrialage.ia2.industrial.WorkshopMenu;
import com.industrialage.ia2.industrial.WorkshopNode;
import com.industrialage.ia2.power.EuBuffer;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Actual world-generated water, artificial basins, and native survival-built KU lines. */
public final class WaterNaturalChecks {
    private static final BlockPos RIVER = new BlockPos(93, 60, -35);
    private static final BlockPos OCEAN = new BlockPos(128, 60, -96);
    private static final BlockPos DEEP = new BlockPos(320, 60, -320);
    private static final BlockPos POOL = new BlockPos(93, 74, -23);
    private static final BlockPos FROZEN_RIVER = new BlockPos(192, 59, -1184);
    private static final BlockPos FROZEN_OCEAN = new BlockPos(192, 59, -1152);
    private final Function<String, Block> block;
    private final Function<String, Item> item;
    private final BiConsumer<String, Boolean> check;
    private final String phase;
    private final BlockPos source;
    private final BlockPos converter;
    private final BlockPos battery;
    private final int expected;
    private ServerPlayer player;
    private int stage;
    private int stageTicks;
    private double previousEu;

    public WaterNaturalChecks(
            Function<String, Block> block,
            Function<String, Item> item,
            BiConsumer<String, Boolean> check,
            String phase) {
        this.block = block;
        this.item = item;
        this.check = check;
        this.phase = phase;
        source =
                switch (phase) {
                    case "waterNaturalRiver" -> RIVER;
                    case "waterNaturalOcean", "waterSeabed" -> OCEAN;
                    case "waterNaturalDeep" -> DEEP;
                    case "waterFrozenRiver" -> FROZEN_RIVER;
                    case "waterFrozenOcean" -> FROZEN_OCEAN;
                    case "waterCanal" -> POOL;
                    default ->
                            throw new IllegalArgumentException(
                                    "Unknown natural water phase " + phase);
                };
        converter = source.east();
        battery = source.east(2);
        expected =
                phase.equals("waterNaturalOcean")
                                || phase.equals("waterSeabed")
                                || phase.equals("waterFrozenOcean")
                        ? 300
                        : phase.equals("waterNaturalDeep") ? 400 : 150;
    }

    public void start(ServerLevel level, ServerPlayer player) {
        this.player = player;
        int cx = source.getX() >> 4, cz = source.getZ() >> 4;
        for (int x = cx - 2; x <= cx + 2; x++)
            for (int z = cz - 2; z <= cz + 2; z++) {
                level.setChunkForced(x, z, true);
                level.getChunk(x, z);
            }
    }

    public boolean tick(ServerLevel level) {
        if (!level.isPositionEntityTicking(source)) return false;
        if (stage > 0 && !node(level, source).ranThisTick(level)) return false;
        if (stage > 1
                && level.getBlockEntity(converter) instanceof WorkshopNode sink
                && !sink.ranThisTick(level)) return false;
        switch (stage) {
            case 0 -> {
                if (phase.equals("waterCanal")) {
                    check.accept(
                            "canal-river-anchor-original-water",
                            level.getBiome(RIVER.above(2)).is(Biomes.RIVER)
                                    && water(level, RIVER.above(2)));
                    check.accept(
                            "canal-crosses-biome-boundary", !level.getBiome(POOL).is(Biomes.RIVER));
                    buildPool(level, POOL, 5, 5);
                    buildPipe(level, 93);
                    buildPipe(level, 97);
                } else if (phase.equals("waterSeabed")) {
                    check.accept(
                            "seabed-original-ocean-water",
                            level.getBiome(OCEAN).is(Biomes.OCEAN) && water(level, OCEAN));
                    buildPool(level, OCEAN, 5, 7);
                } else {
                    boolean correct =
                            switch (phase) {
                                case "waterNaturalRiver" -> level.getBiome(source).is(Biomes.RIVER);
                                case "waterNaturalOcean" -> level.getBiome(source).is(Biomes.OCEAN);
                                case "waterNaturalDeep" ->
                                        level.getBiome(source).is(Biomes.DEEP_OCEAN);
                                case "waterFrozenRiver" ->
                                        level.getBiome(source).is(Biomes.FROZEN_RIVER);
                                case "waterFrozenOcean" ->
                                        level.getBiome(source).is(Biomes.FROZEN_OCEAN);
                                default -> false;
                            };
                    check.accept("natural-biome-" + phase, correct);
                    check.accept("natural-rotor-fully-submerged", originalWater(level));
                    if (phase.startsWith("waterFrozen")) {
                        var cover = level.getBlockState(source.above(3));
                        check.accept(
                                "native-ice-over-underwater-rotor",
                                cover.is(Blocks.ICE)
                                        || cover.is(Blocks.PACKED_ICE)
                                        || cover.is(Blocks.BLUE_ICE));
                    }
                }
                placeTop(level, source, "water_kinetic_generator", 90);
                level.setBlockAndUpdate(source.below(), Blocks.WATER.defaultBlockState());
                check.accept(
                        "natural-survival-source-faces-east",
                        direction(level, source) == Direction.EAST);
                next();
            }
            case 1 -> {
                if (++stageTicks < 5) break;
                check.accept("natural-empty-rotor-zero", menu(level, source).motion() == 0);
                install(menu(level, source), "bronze_rotor");
                check.accept(
                        "natural-player-installs-bronze",
                        menu(level, source).getSlot(0).getItem().is(item.apply("bronze_rotor")));
                placeTop(level, converter, "kinetic_generator", -90);
                placeTop(level, battery, "classic_mfsu", 90);
                next();
            }
            case 2 -> {
                int target = isolated() ? 4 : expected;
                if (menu(level, source).motion() != target
                        || menu(level, converter).motion() != target) {
                    if (++stageTicks > 1600)
                        throw new AssertionError(
                                "Natural water did not reach "
                                        + target
                                        + " KU/t; got "
                                        + menu(level, source).motion());
                    break;
                }
                check.accept(
                        isolated() ? "closed-basin-low-output" : "natural-water-high-output", true);
                previousEu = totalEu(level);
                next();
            }
            case 3 -> {
                int target = isolated() ? 4 : expected;
                check.accept(
                        "ku-eu-exact-one-tick",
                        Math.abs(totalEu(level) - previousEu - target / 4.0) < 0.0001);
                if (!isolated()) return true;
                setGate(level, 0, true);
                next();
            }
            case 4 -> {
                if (!waitFor(level, expected)) break;
                check.accept("open-first-gate-reaches-natural-water", true);
                previousEu = totalEu(level);
                next();
            }
            case 5 -> {
                check.accept(
                        "open-gate-eu-conversion",
                        Math.abs(totalEu(level) - previousEu - expected / 4.0) < 0.0001);
                if (phase.equals("waterCanal")) setGate(level, 1, true);
                next();
            }
            case 6 -> {
                if (++stageTicks < 30) break;
                setGate(level, 0, false);
                next();
            }
            case 7 -> {
                int target = phase.equals("waterCanal") ? expected : 4;
                if (!waitFor(level, target)) break;
                check.accept(
                        phase.equals("waterCanal")
                                ? "second-channel-still-supplies"
                                : "closed-seabed-gate-returns-low",
                        true);
                if (phase.equals("waterCanal")) setGate(level, 1, false);
                else setGate(level, 0, true);
                next();
            }
            case 8 -> {
                int target = phase.equals("waterCanal") ? 4 : expected;
                if (!waitFor(level, target)) break;
                check.accept(
                        phase.equals("waterCanal")
                                ? "all-channels-closed-return-low"
                                : "reopened-seabed-gate-recovers",
                        true);
                if (phase.equals("waterCanal")) setGate(level, 0, true);
                else return true;
                next();
            }
            case 9 -> {
                if (!waitFor(level, expected)) break;
                check.accept("reopened-channel-recovers-natural-rate", true);
                return true;
            }
            default ->
                    throw new IllegalStateException("Unexpected natural water test stage " + stage);
        }
        return false;
    }

    private boolean isolated() {
        return phase.equals("waterCanal") || phase.equals("waterSeabed");
    }

    private void next() {
        stage++;
        stageTicks = 0;
    }

    private boolean waitFor(ServerLevel level, int value) {
        if (menu(level, source).motion() == value && menu(level, converter).motion() == value)
            return true;
        if (++stageTicks > 1600)
            throw new AssertionError(
                    "Expected "
                            + value
                            + " KU/t after gate transition, got "
                            + menu(level, source).motion());
        return false;
    }

    private boolean originalWater(ServerLevel level) {
        if (!water(level, source) || !water(level, source.above()) || !water(level, source.below()))
            return false;
        for (int y = -2; y <= 2; y++)
            for (int z = -2; z <= 2; z++) {
                double a = Math.max(0, Math.abs(y) - .5), b = Math.max(0, Math.abs(z) - .5);
                if (a * a + b * b <= 4 && !water(level, source.west().offset(0, y, z)))
                    return false;
            }
        return true;
    }

    private boolean water(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getFluidState(pos).is(FluidTags.WATER);
    }

    private WorkshopNode node(ServerLevel level, BlockPos pos) {
        return (WorkshopNode) level.getBlockEntity(pos);
    }

    private WorkshopMenu menu(ServerLevel level, BlockPos pos) {
        var menu = (WorkshopMenu) node(level, pos).createMenu(1, player.getInventory(), player);
        player.containerMenu = menu;
        return menu;
    }

    private double totalEu(ServerLevel level) {
        return ((PowerNode) level.getBlockEntity(battery)).storedEu()
                + ((EuBuffer) node(level, converter)).bufferedEu().stored();
    }

    private Direction direction(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.getValue(
                state.hasProperty(BlockStateProperties.FACING)
                        ? BlockStateProperties.FACING
                        : BlockStateProperties.HORIZONTAL_FACING);
    }

    private void install(WorkshopMenu menu, String rotor) {
        player.getInventory().setItem(0, new ItemStack(item.apply(rotor)));
        com.industrialage.ia2.gametest.NativeWaterActions.click(menu, 54, "QUICK_MOVE", player);
    }

    private void placeTop(ServerLevel level, BlockPos pos, String id, float yaw) {
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        player.setPos(pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5);
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(0);
        var stack = new ItemStack(block.apply(id));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false);
        check.accept(
                "natural-survival-place-" + id,
                player.gameMode
                                .useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit)
                                .consumesAction()
                        && stack.isEmpty()
                        && level.getBlockState(pos).is(block.apply(id)));
    }

    private void buildPool(ServerLevel level, BlockPos center, int xRadius, int zRadius) {
        for (int x = -xRadius; x <= xRadius; x++)
            for (int y = -5; y <= 5; y++)
                for (int z = -zRadius; z <= zRadius; z++) {
                    boolean shell =
                            Math.abs(x) == xRadius || Math.abs(y) == 5 || Math.abs(z) == zRadius;
                    level.setBlockAndUpdate(
                            center.offset(x, y, z),
                            shell
                                    ? Blocks.GLASS.defaultBlockState()
                                    : Blocks.WATER.defaultBlockState());
                }
    }

    private void buildPipe(ServerLevel level, int x) {
        for (int y = 63; y <= 72; y++) {
            for (var side :
                    new Direction[] {
                        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
                    })
                level.setBlockAndUpdate(
                        new BlockPos(x, y, -35).relative(side), Blocks.GLASS.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(x, y, -35), Blocks.WATER.defaultBlockState());
        }
        for (int z = -35; z <= -29; z++) {
            var center = new BlockPos(x, 72, z);
            level.setBlockAndUpdate(center.west(), Blocks.GLASS.defaultBlockState());
            level.setBlockAndUpdate(center.east(), Blocks.GLASS.defaultBlockState());
            level.setBlockAndUpdate(center.above(), Blocks.GLASS.defaultBlockState());
            if (z != -35) level.setBlockAndUpdate(center.below(), Blocks.GLASS.defaultBlockState());
            level.setBlockAndUpdate(center, Blocks.WATER.defaultBlockState());
        }
    }

    private void setGate(ServerLevel level, int index, boolean open) {
        BlockPos gate =
                phase.equals("waterSeabed")
                        ? source.offset(-5, 2, 0)
                        : new BlockPos(index == 0 ? 93 : 97, 72, -28);
        level.setBlockAndUpdate(
                gate, open ? Blocks.WATER.defaultBlockState() : Blocks.GLASS.defaultBlockState());
    }
}
