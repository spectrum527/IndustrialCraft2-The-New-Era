package com.industrialage.ia2.compatqa;

import com.industrialage.ia2.gameplay.IndustrialMachine;
import com.industrialage.ia2.industrial.WorkshopNode;
import com.industrialage.ia2.industrial.WorkshopPlatform;
import com.industrialage.ia2.logistics.TransportNode;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Live dedicated-server checks for ItemPipeRouter multi-segment routing, extraction nozzle OUT
 * auto-flip, rotor wear/durability, copper_ingot removal, biogas_cell presence, and dynamic tags.
 */
final class EcosystemLiveChecks {
    private static final BlockPos SRC_BARREL = new BlockPos(3, 85, 4);
    private static final BlockPos PIPE_1 = new BlockPos(4, 85, 4);
    private static final BlockPos PIPE_2 = new BlockPos(5, 85, 4);
    private static final BlockPos DECOY_BARREL = new BlockPos(5, 85, 5);
    private static final BlockPos PIPE_3 = new BlockPos(6, 85, 4);
    private static final BlockPos DST_BARREL = new BlockPos(7, 85, 4);
    private static final BlockPos SOLO_PIPE = new BlockPos(10, 85, 4);

    private static final BlockPos LOOP_SRC = new BlockPos(3, 85, 9);
    private static final BlockPos LOOP_P1 = new BlockPos(4, 85, 9);
    private static final BlockPos LOOP_SHORT = new BlockPos(5, 85, 9);
    private static final BlockPos LOOP_SINK = new BlockPos(6, 85, 9);
    private static final BlockPos LOOP_DST = new BlockPos(7, 85, 9);
    private static final BlockPos LOOP_L1 = new BlockPos(4, 85, 10);
    private static final BlockPos LOOP_L2 = new BlockPos(5, 85, 10);
    private static final BlockPos LOOP_L3 = new BlockPos(6, 85, 10);

    private static final BlockPos SAFE_WIND = new BlockPos(8, 92, 8);
    private static final BlockPos STORM_WIND = new BlockPos(8, 175, 8);
    private static final BlockPos BREAK_WIND = new BlockPos(8, 195, 8);
    private static final BlockPos TURBINE_POS = new BlockPos(8, 185, 12);
    private static final BlockPos MACERATOR_POS = new BlockPos(4, 185, 12);
    private static final BlockPos COMPRESSOR_POS = new BlockPos(6, 185, 12);

    private EcosystemLiveChecks() {}

    static void setup(ServerLevel level) {
        level.getChunkAt(PIPE_1);
        var operator = FakePlayerFactory.getMinecraft(level);

        // 1. Multi-segment route with arbitrary OUT, decoy non-OUT barrel, and nozzle auto-flip.
        level.setBlockAndUpdate(SRC_BARREL, Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(DECOY_BARREL, Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(DST_BARREL, Blocks.BARREL.defaultBlockState());
        placePipe(level, PIPE_1, Direction.WEST);
        placePipe(level, PIPE_2, Direction.UP);
        placePipe(level, PIPE_3, Direction.UP);
        installNozzle(level, operator, PIPE_1, Direction.WEST, "pipe_extraction_nozzle");
        require(
                level.getBlockState(PIPE_1).getValue(BlockStateProperties.FACING) == Direction.EAST,
                "extraction nozzle on OUT face must auto-flip toward linked adjacent pipe");
        installNozzle(level, operator, PIPE_3, Direction.EAST, "pipe_injection_nozzle");
        ((Container) level.getBlockEntity(SRC_BARREL)).setItem(0, new ItemStack(Items.DIAMOND, 16));

        // Standalone pipe extraction nozzle auto-flips OUT to opposite face when no pipe neighbors.
        placePipe(level, SOLO_PIPE, Direction.WEST);
        installNozzle(level, operator, SOLO_PIPE, Direction.WEST, "pipe_extraction_nozzle");
        require(
                level.getBlockState(SOLO_PIPE).getValue(BlockStateProperties.FACING)
                        == Direction.EAST,
                "standalone extraction nozzle on OUT face must auto-flip to opposite face");

        // 2. Square-loop detour around an incompatible pipe segment.
        level.setBlockAndUpdate(LOOP_SRC, Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(LOOP_DST, Blocks.BARREL.defaultBlockState());
        placePipe(level, LOOP_P1, Direction.UP);
        placePipe(level, LOOP_SHORT, Direction.UP);
        placePipe(level, LOOP_SINK, Direction.EAST);
        placePipe(level, LOOP_L1, Direction.UP);
        placePipe(level, LOOP_L2, Direction.UP);
        placePipe(level, LOOP_L3, Direction.UP);
        installNozzle(level, operator, LOOP_P1, Direction.WEST, "pipe_extraction_nozzle");
        var blocker =
                MachineSideHost.item(
                        level, LOOP_SHORT, Direction.DOWN, new ItemStack(Items.COBBLESTONE));
        require(
                blocker != null && blocker.insert(1, false) == 1,
                "seed incompatible blocker in short branch");
        ((Container) level.getBlockEntity(LOOP_SRC)).setItem(0, new ItemStack(Items.IRON_INGOT, 8));

        // 3. Wind kinetic generators at safe altitude (Y=92, wind <= 60) and high altitude
        // (Y=175/195, wind > 60).
        placeWind(level, operator, SAFE_WIND, 0);
        placeWind(level, operator, STORM_WIND, 0);
        placeWind(level, operator, BREAK_WIND, 10_798);

        // 4. Steam kinetic generator with steam_turbine (must not wear).
        level.setBlockAndUpdate(
                TURBINE_POS,
                CompatHost.block("steam_kinetic_generator")
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.EAST));
        var turbineNode = (WorkshopNode) level.getBlockEntity(TURBINE_POS);
        turbineNode
                .createMenu(93, operator.getInventory(), operator)
                .getSlot(0)
                .set(new ItemStack(CompatHost.item("steam_turbine")));
    }

    static void verify(ServerLevel level, ServerPlayer player, List<String> report) {
        try {
            // 1. Registry presence/absence
            boolean hasCopperIngot =
                    BuiltInRegistries.ITEM.keySet().stream()
                            .anyMatch(
                                    id ->
                                            id.getNamespace().equals("ic2")
                                                    && id.getPath().equals("copper_ingot"));
            boolean hasBiogasCell =
                    BuiltInRegistries.ITEM.keySet().stream()
                            .anyMatch(
                                    id ->
                                            id.getNamespace().equals("ic2")
                                                    && id.getPath().equals("biogas_cell"));
            require(!hasCopperIngot, "ic2:copper_ingot must be absent from item registry");
            require(hasBiogasCell, "ic2:biogas_cell must be registered");

            // 2. Native rotor durabilities & steam_turbine unbreakable
            var expectedDurabilities =
                    Map.of(
                            "wooden_rotor", 10_800,
                            "bronze_rotor", 86_400,
                            "iron_rotor", 86_400,
                            "steel_rotor", 172_800,
                            "carbon_rotor", 604_800);
            for (var entry : expectedDurabilities.entrySet()) {
                var stack = new ItemStack(CompatHost.item(entry.getKey()));
                require(
                        stack.getMaxDamage() == entry.getValue(),
                        entry.getKey()
                                + " durability expected "
                                + entry.getValue()
                                + " got "
                                + stack.getMaxDamage());
            }
            require(
                    new ItemStack(CompatHost.item("steam_turbine")).getMaxDamage() == 0,
                    "steam_turbine must have zero maxDamage (no wear)");

            // 3. Multi-segment routing + decoy isolation + square-loop detour
            var src = (Container) level.getBlockEntity(SRC_BARREL);
            var decoy = (Container) level.getBlockEntity(DECOY_BARREL);
            var dst = (Container) level.getBlockEntity(DST_BARREL);
            require(
                    src.isEmpty() && decoy.isEmpty() && dst.countItem(Items.DIAMOND) == 16,
                    "multi-segment pipe routing delivered 16 diamonds while leaving decoy empty"
                            + " (src="
                            + src.countItem(Items.DIAMOND)
                            + ", decoy="
                            + decoy.countItem(Items.DIAMOND)
                            + ", dst="
                            + dst.countItem(Items.DIAMOND)
                            + ")");

            var loopSrc = (Container) level.getBlockEntity(LOOP_SRC);
            var loopDst = (Container) level.getBlockEntity(LOOP_DST);
            require(
                    loopSrc.isEmpty() && loopDst.countItem(Items.IRON_INGOT) == 8,
                    "square-loop pipe detoured 8 iron ingots around blocked short leg"
                            + " (src="
                            + loopSrc.countItem(Items.IRON_INGOT)
                            + ", dst="
                            + loopDst.countItem(Items.IRON_INGOT)
                            + ")");

            // 4. Wind rotor 1x wear, 4x over-wind wear, break-at-0, and steam_turbine non-wear
            ItemStack safeRotor = rotorIn(level, player, SAFE_WIND);
            require(
                    safeRotor.is(CompatHost.item("wooden_rotor"))
                            && safeRotor.getDamageValue() == 1,
                    "safe-wind rotor expected damage 1 after 25 ticks, got "
                            + safeRotor.getDamageValue());

            ItemStack stormRotor = rotorIn(level, player, STORM_WIND);
            require(
                    stormRotor.is(CompatHost.item("wooden_rotor"))
                            && stormRotor.getDamageValue() == 4,
                    "over-wind rotor expected 4x damage 4 after 25 ticks, got "
                            + stormRotor.getDamageValue());

            ItemStack brokenRotor = rotorIn(level, player, BREAK_WIND);
            require(
                    brokenRotor.isEmpty(),
                    "wooden rotor with 2 durability left must break under 4x wear");

            ItemStack turbine = rotorIn(level, player, TURBINE_POS);
            require(
                    turbine.is(CompatHost.item("steam_turbine")) && turbine.getDamageValue() == 0,
                    "steam_turbine must never take wear");

            // 5. Dynamic c:/forge: / raw_ores/raw_materials / blocks/storage_blocks tags &
            // IndustrialMachine
            var rawTin = new ItemStack(CompatHost.item("raw_tin"));
            var tinIngot = new ItemStack(CompatHost.item("tin_ingot"));
            var bronzeBlock = new ItemStack(CompatHost.item("bronze_block"));
            var bronzeIngot = new ItemStack(CompatHost.item("bronze_ingot"));
            require(
                    WorkshopPlatform.tagged(rawTin, "c:raw_materials/tin")
                            && WorkshopPlatform.tagged(rawTin, "c:raw_ores/tin")
                            && WorkshopPlatform.tagged(rawTin, "forge:raw_materials/tin")
                            && WorkshopPlatform.tagged(rawTin, "forge:raw_ores/tin"),
                    "raw_tin dynamic raw_ores/raw_materials c:/forge: alias match");
            require(
                    WorkshopPlatform.tagged(tinIngot, "c:ingots/tin")
                            && WorkshopPlatform.tagged(tinIngot, "forge:ingots/tin"),
                    "tin_ingot dynamic c:/forge: alias match");
            require(
                    WorkshopPlatform.tagged(bronzeBlock, "c:storage_blocks/bronze")
                            && WorkshopPlatform.tagged(bronzeBlock, "forge:storage_blocks/bronze")
                            && WorkshopPlatform.tagged(bronzeBlock, "c:blocks/bronze")
                            && WorkshopPlatform.tagged(bronzeBlock, "forge:blocks/bronze"),
                    "bronze_block dynamic blocks/storage_blocks c:/forge: alias match");

            level.setBlockAndUpdate(
                    MACERATOR_POS, CompatHost.block("macerator").defaultBlockState());
            level.setBlockAndUpdate(
                    COMPRESSOR_POS, CompatHost.block("compressor").defaultBlockState());
            var macerator = (IndustrialMachine) level.getBlockEntity(MACERATOR_POS);
            var compressor = (IndustrialMachine) level.getBlockEntity(COMPRESSOR_POS);
            require(
                    macerator.canPlaceItem(0, rawTin) && macerator.canPlaceItem(0, tinIngot),
                    "IndustrialMachine macerator accepts dynamically tag-matched raw_tin and tin_ingot");
            require(
                    compressor.canPlaceItem(0, bronzeIngot),
                    "IndustrialMachine compressor accepts dynamically tag-matched bronze_ingot");

            report.add(
                    "Ecosystem live checks: 3-pipe BFS routing, decoy isolation, nozzle OUT auto-flip, square-loop detour, 1x/4x rotor wear + break + steam_turbine non-wear, copper_ingot absent, biogas_cell present, and dynamic c:/forge: machine tag matching passed.");
        } finally {
            for (var pos :
                    List.of(
                            SRC_BARREL,
                            PIPE_1,
                            PIPE_2,
                            DECOY_BARREL,
                            PIPE_3,
                            DST_BARREL,
                            SOLO_PIPE,
                            LOOP_SRC,
                            LOOP_P1,
                            LOOP_SHORT,
                            LOOP_SINK,
                            LOOP_DST,
                            LOOP_L1,
                            LOOP_L2,
                            LOOP_L3,
                            SAFE_WIND,
                            STORM_WIND,
                            BREAK_WIND,
                            TURBINE_POS,
                            MACERATOR_POS,
                            COMPRESSOR_POS)) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static void placePipe(ServerLevel level, BlockPos pos, Direction out) {
        level.setBlockAndUpdate(
                pos,
                CompatHost.block("bronze_item_pipe_small")
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, out));
    }

    private static void installNozzle(
            ServerLevel level, ServerPlayer player, BlockPos pos, Direction face, String itemPath) {
        var nozzle = new ItemStack(CompatHost.item(itemPath));
        player.setItemInHand(InteractionHand.MAIN_HAND, nozzle);
        player.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        var contact =
                Vec3.atCenterOf(pos)
                        .add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        player.gameMode.useItemOn(
                player,
                level,
                nozzle,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(contact, face, pos, false));
        var node = (TransportNode) level.getBlockEntity(pos);
        var expected =
                itemPath.contains("extraction")
                        ? TransportNode.Nozzle.EXTRACTION
                        : TransportNode.Nozzle.INJECTION;
        require(node.nozzle(face) == expected, "failed to install " + itemPath + " on " + face);
    }

    private static void placeWind(
            ServerLevel level, ServerPlayer player, BlockPos pos, int initialDamage) {
        level.setBlockAndUpdate(
                pos,
                CompatHost.block("wind_kinetic_generator")
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.EAST));
        var rotor = new ItemStack(CompatHost.item("wooden_rotor"));
        if (initialDamage > 0) rotor.setDamageValue(initialDamage);
        ((MenuProvider) level.getBlockEntity(pos))
                .createMenu(91, player.getInventory(), player)
                .getSlot(0)
                .set(rotor);
    }

    private static ItemStack rotorIn(ServerLevel level, ServerPlayer player, BlockPos pos) {
        return ((MenuProvider) level.getBlockEntity(pos))
                .createMenu(92, player.getInventory(), player)
                .getSlot(0)
                .getItem();
    }

    private static void require(boolean condition, String detail) {
        if (!condition) throw new AssertionError(detail);
    }
}
