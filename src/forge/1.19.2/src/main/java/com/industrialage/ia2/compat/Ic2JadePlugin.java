package com.industrialage.ia2.compat;

import com.industrialage.ia2.forge.content.CableBlock;
import com.industrialage.ia2.forge.content.GeneratorDevice;
import com.industrialage.ia2.forge.content.InventoryDevice;
import com.industrialage.ia2.forge.content.MachineBlock;
import com.industrialage.ia2.industrial.PowerBlock;
import com.industrialage.ia2.industrial.PowerNode;
import com.industrialage.ia2.industrial.WorkshopBlock;
import com.industrialage.ia2.industrial.WorkshopNode;
import com.industrialage.ia2.logistics.TransportBlock;
import com.industrialage.ia2.logistics.TransportNode;
import com.industrialage.ia2.world.WorldMachine;
import com.industrialage.ia2.world.WorldMachineBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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

/** Forge 1.19.2 Jade plugin displaying IC2 EU, fluid, reactor, and electric boat telemetry. */
@WailaPlugin
public final class Ic2JadePlugin implements IWailaPlugin {
    private static final ResourceLocation DEVICE_UID =
            ResourceLocation.fromNamespaceAndPath("ic2", "device");
    private static final ResourceLocation ENTITY_UID =
            ResourceLocation.fromNamespaceAndPath("ic2", "electric_entity");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DeviceProvider.INSTANCE, InventoryDevice.class);
        registration.registerBlockDataProvider(DeviceProvider.INSTANCE, GeneratorDevice.class);
        registration.registerBlockDataProvider(DeviceProvider.INSTANCE, WorkshopNode.class);
        registration.registerBlockDataProvider(DeviceProvider.INSTANCE, PowerNode.class);
        registration.registerBlockDataProvider(DeviceProvider.INSTANCE, TransportNode.class);
        registration.registerBlockDataProvider(DeviceProvider.INSTANCE, WorldMachine.class);
        registration.registerEntityDataProvider(EntityProvider.INSTANCE, Entity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(DeviceProvider.INSTANCE, MachineBlock.class);
        registration.registerBlockComponent(DeviceProvider.INSTANCE, WorkshopBlock.class);
        registration.registerBlockComponent(DeviceProvider.INSTANCE, PowerBlock.class);
        registration.registerBlockComponent(DeviceProvider.INSTANCE, TransportBlock.class);
        registration.registerBlockComponent(DeviceProvider.INSTANCE, WorldMachineBlock.class);
        registration.registerBlockComponent(CableProvider.INSTANCE, CableBlock.class);
        registration.registerEntityComponent(EntityProvider.INSTANCE, Entity.class);
    }

    private enum DeviceProvider
            implements IBlockComponentProvider, IServerDataProvider<BlockEntity> {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return DEVICE_UID;
        }

        @Override
        public void appendServerData(
                CompoundTag data,
                ServerPlayer player,
                Level level,
                BlockEntity blockEntity,
                boolean showDetails) {
            if (level instanceof ServerLevel server && blockEntity != null) {
                Ic2ProbeData.writeBlock(data, server, blockEntity.getBlockState(), blockEntity);
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

    private enum EntityProvider implements IEntityComponentProvider, IServerDataProvider<Entity> {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return ENTITY_UID;
        }

        @Override
        public void appendServerData(
                CompoundTag data,
                ServerPlayer player,
                Level level,
                Entity entity,
                boolean showDetails) {
            if (entity != null) {
                Ic2ProbeData.writeEntity(data, entity);
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            for (Component line : Ic2ProbeData.readEntity(accessor.getServerData())) {
                tooltip.add(line);
            }
        }
    }
}
