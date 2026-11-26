package com.industrialage.ia2.compat;

import com.industrialage.ia2.gameplay.CableBlock;
import com.industrialage.ia2.gameplay.MachineBlock;
import com.industrialage.ia2.industrial.PowerBlock;
import com.industrialage.ia2.industrial.WorkshopBlock;
import com.industrialage.ia2.logistics.TransportBlock;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaClientPlugin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/** Shared NeoForge WTHIT client plugin rendering IC2 EU, fluid, reactor, and boat telemetry. */
public final class Ic2WthitClientPlugin implements IWailaClientPlugin {
    @Override
    public void register(IClientRegistrar registrar) {
        registrar.body(BlockProvider.INSTANCE, MachineBlock.class);
        registrar.body(BlockProvider.INSTANCE, WorkshopBlock.class);
        registrar.body(BlockProvider.INSTANCE, PowerBlock.class);
        registrar.body(BlockProvider.INSTANCE, TransportBlock.class);
        registrar.body(CableProvider.INSTANCE, CableBlock.class);
        registrar.body(EntityProvider.INSTANCE, Entity.class);
    }

    private enum BlockProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            for (Component line : Ic2ProbeData.readBlock(accessor.getData().raw())) {
                tooltip.addLine(line);
            }
        }
    }

    private enum CableProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            tooltip.addLine(Component.translatable("top.ic2.conductor"));
        }
    }

    private enum EntityProvider implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public void appendBody(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
            for (Component line : Ic2ProbeData.readEntity(accessor.getData().raw())) {
                tooltip.addLine(line);
            }
        }
    }
}
