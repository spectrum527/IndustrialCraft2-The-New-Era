package com.industrialage.ia2.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Native block hosting powered agriculture and geographic equipment. */
public final class WorldMachineBlock extends WorldNative.MachineBase {
    static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    final Kind kind;

    WorldMachineBlock(Properties options, Kind kind) {
        super(options);
        this.kind = kind;
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(
                        BlockStateProperties.HORIZONTAL_FACING,
                        context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WorldMachine(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (WorldNative.client(level) || type != WorldPlatform.machineType()) return null;
        return (world, pos, current, blockEntity) ->
                ((WorldMachine) blockEntity).tick((ServerLevel) world);
    }

    /** Whether this block forms a consuming endpoint for adjacent EU conductors. */
    public boolean acceptsPower() {
        return kind.packet > 0;
    }

    enum Kind {
        CROP_HARVESTER("crop_harvester", 32),
        CROPMATRON("cropmatron", 32),
        CLASSIC_CROPMATRON("classic_cropmatron", 32),
        TERRAFORMER("terraformer", 512),
        MINER("miner", 32),
        ADVANCED_MINER("advanced_miner", 512),
        BATCH_CRAFTER("batch_crafter", 128),
        ITEM_BUFFER("item_buffer", 0),
        SORTING_MACHINE("sorting_machine", 32),
        WEIGHTED_DISTRIBUTOR("weighted_item_distributor", 0),
        CHUNK_LOADER("chunk_loader", 128);

        final String path;
        final int packet;

        Kind(String path, int packet) {
            this.path = path;
            this.packet = packet;
        }

        int size() {
            return switch (this) {
                case BATCH_CRAFTER, ITEM_BUFFER, SORTING_MACHINE, WEIGHTED_DISTRIBUTOR -> 27;
                case ADVANCED_MINER, CROPMATRON -> 18;
                default -> 9;
            };
        }

        int batterySlot() {
            return switch (this) {
                case BATCH_CRAFTER -> 17;
                case SORTING_MACHINE -> 24;
                case ADVANCED_MINER, CROPMATRON -> 8;
                default -> size() - 1;
            };
        }

        boolean matron() {
            return this == CROPMATRON || this == CLASSIC_CROPMATRON;
        }
    }
}
