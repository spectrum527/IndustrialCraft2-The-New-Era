package com.industrialage.ia2.world;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

final class ObscuredWall extends Block {
    static final EnumProperty<Material> MATERIAL = EnumProperty.create("material", Material.class);

    ObscuredWall(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(MATERIAL, Material.STONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MATERIAL);
    }

    static boolean use(Level level, BlockPos pos, Player player, ItemStack tool, BlockState state) {
        if (player.isShiftKeyDown()) {
            var sample =
                    state.getBlock() instanceof ObscuredWall
                            ? state.getValue(MATERIAL)
                            : Material.find(WorldNative.blockId(state.getBlock()));
            if (sample == null) return false;
            if (level instanceof ServerLevel) {
                WorldNative.setSample(tool, sample.id);
                WorldNative.reply(
                        player,
                        Component.translatable(
                                "ic2.world.camouflage_sample",
                                WorldNative.block(CropSpecies.qualify(sample.id)).getName()));
            }
            return true;
        }
        if (!(state.getBlock() instanceof WorldBlocks.PaintedWall)
                && !(state.getBlock() instanceof ObscuredWall)) {
            return false;
        }
        var material = Material.find(WorldNative.sample(tool));
        if (material == null || state.hasProperty(MATERIAL) && state.getValue(MATERIAL) == material)
            return false;
        if (!(level instanceof ServerLevel)) return true;
        if (!player.isCreative() && !WorldNative.spend(tool, 1000)) {
            WorldNative.reply(player, Component.translatable("ic2.world.empty_charge"));
            return true;
        }
        return level.setBlockAndUpdate(
                pos,
                WorldNative.block("ic2:obscured_wall")
                        .defaultBlockState()
                        .setValue(MATERIAL, material));
    }

    static boolean applySheet(
            Level level,
            BlockPos pos,
            Player player,
            ItemStack sheet,
            BlockState state,
            boolean lava) {
        if (!(state.getBlock() instanceof WorldBlocks.PaintedWall)
                && !(state.getBlock() instanceof ObscuredWall)) return false;
        var appearance = lava ? Material.LAVA : Material.WATER;
        if (state.hasProperty(MATERIAL) && state.getValue(MATERIAL) == appearance) return false;
        if (level instanceof ServerLevel
                && level.setBlockAndUpdate(
                        pos,
                        WorldNative.block("ic2:obscured_wall")
                                .defaultBlockState()
                                .setValue(MATERIAL, appearance))
                && !player.isCreative()) sheet.shrink(1);
        return true;
    }

    enum Material implements StringRepresentable {
        STONE,
        COBBLESTONE,
        DEEPSLATE,
        STONE_BRICKS,
        BRICKS,
        POLISHED_ANDESITE,
        END_STONE,
        SANDSTONE,
        OAK_PLANKS,
        SPRUCE_PLANKS,
        BIRCH_PLANKS,
        JUNGLE_PLANKS,
        ACACIA_PLANKS,
        DARK_OAK_PLANKS,
        COPPER_BLOCK,
        IRON_BLOCK,
        GOLD_BLOCK,
        QUARTZ_BLOCK,
        NETHERRACK,
        NETHER_BRICKS,
        OBSIDIAN,
        WATER,
        LAVA,
        BASALT("ic2:basalt"),
        RUBBER_PLANKS("ic2:rubber_planks"),
        REINFORCED_STONE("ic2:reinforced_stone");

        final String id;

        Material() {
            id = name().toLowerCase(Locale.ROOT);
        }

        Material(String id) {
            this.id = id;
        }

        static Material find(String id) {
            for (var material : values()) {
                if (material.id.equals(id) || CropSpecies.qualify(material.id).equals(id))
                    return material;
            }
            return null;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
