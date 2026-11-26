package com.industrialage.ia2.compat;

import com.industrialage.ia2.gameplay.IndustrialMachine;
import com.industrialage.ia2.industrial.PowerNode;
import com.industrialage.ia2.industrial.WorkshopNode;
import com.industrialage.ia2.logistics.TransportNode;
import mcp.mobius.waila.api.ICommonRegistrar;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mcp.mobius.waila.api.IWailaCommonPlugin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Shared NeoForge WTHIT common plugin syncing IC2 EU, fluid, reactor, and electric boat data. */
public final class Ic2WthitCommonPlugin implements IWailaCommonPlugin {
    @Override
    public void register(ICommonRegistrar registrar) {
        registrar.blockData(BlockData.INSTANCE, IndustrialMachine.class);
        registrar.blockData(BlockData.INSTANCE, WorkshopNode.class);
        registrar.blockData(BlockData.INSTANCE, PowerNode.class);
        registrar.blockData(BlockData.INSTANCE, TransportNode.class);
        registrar.entityData(EntityData.INSTANCE, Entity.class);
    }

    private enum BlockData implements IDataProvider<BlockEntity> {
        INSTANCE;

        @Override
        public void appendData(
                IDataWriter data, IServerAccessor<BlockEntity> accessor, IPluginConfig config) {
            var entity = accessor.getTarget();
            if (entity != null && accessor.getLevel() instanceof ServerLevel server) {
                Ic2ProbeData.writeBlock(data.raw(), server, entity.getBlockState(), entity);
            }
        }
    }

    private enum EntityData implements IDataProvider<Entity> {
        INSTANCE;

        @Override
        public void appendData(
                IDataWriter data, IServerAccessor<Entity> accessor, IPluginConfig config) {
            var entity = accessor.getTarget();
            if (entity != null) {
                Ic2ProbeData.writeEntity(data.raw(), entity);
            }
        }
    }
}
