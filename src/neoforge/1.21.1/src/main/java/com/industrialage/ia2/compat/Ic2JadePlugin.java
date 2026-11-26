package com.industrialage.ia2.compat;

import com.industrialage.ia2.gameplay.CableBlock;
import com.industrialage.ia2.gameplay.IndustrialMachine;
import com.industrialage.ia2.gameplay.MachineBlock;
import com.industrialage.ia2.industrial.PowerBlock;
import com.industrialage.ia2.industrial.PowerNode;
import com.industrialage.ia2.industrial.WorkshopBlock;
import com.industrialage.ia2.industrial.WorkshopNode;
import com.industrialage.ia2.logistics.TransportBlock;
import com.industrialage.ia2.logistics.TransportNode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** NeoForge 1.21.1 Jade plugin displaying IC2 EU, fluid, reactor, and electric boat telemetry. */
@WailaPlugin
public final class Ic2JadePlugin implements IWailaPlugin {
    private static final ResourceLocation DEVICE_UID =
            ResourceLocation.fromNamespaceAndPath("ic2", "device");
    private static final ResourceLocation ENTITY_UID =
            ResourceLocation.fromNamespaceAndPath("ic2", "electric_entity");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DeviceProvider.INSTANCE, IndustrialMachine.class);
        registration.registerBlockDataProvider(DeviceProvider.INSTANCE, WorkshopNode.class);
        registration.registerBlockDataProvider(DeviceProvider.INSTANCE, PowerNode.class);
        registration.registerBlockDataProvider(DeviceProvider.INSTANCE, TransportNode.class);
        registration.registerEntityDataProvider(EntityProvider.INSTANCE, Entity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(DeviceProvider.INSTANCE, MachineBlock.class);
        registration.registerBlockComponent(DeviceProvider.INSTANCE, WorkshopBlock.class);
        registration.registerBlockComponent(DeviceProvider.INSTANCE, PowerBlock.class);
        registration.registerBlockComponent(DeviceProvider.INSTANCE, TransportBlock.class);
        registration.registerBlockComponent(CableProvider.INSTANCE, CableBlock.class);
        registration.registerEntityComponent(EntityProvider.INSTANCE, Entity.class);
    }

    private enum DeviceProvider
            implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return DEVICE_UID;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getLevel() instanceof ServerLevel server) {
                Ic2ProbeData.writeBlock(
                        data, server, accessor.getBlockState(), accessor.getBlockEntity());
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            for (Component line : Ic2ProbeData.readBlock(accessor.getServerData())) {
                tooltip.add(line);
            }
        }
    }

    private enum CableProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return DEVICE_UID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            tooltip.add(Component.translatable("top.ic2.conductor"));
        }
    }

    private enum EntityProvider
            implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return ENTITY_UID;
        }

        @Override
        public void appendServerData(CompoundTag data, EntityAccessor accessor) {
            Ic2ProbeData.writeEntity(data, accessor.getEntity());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            for (Component line : Ic2ProbeData.readEntity(accessor.getServerData())) {
                tooltip.add(line);
            }
        }
    }
}
