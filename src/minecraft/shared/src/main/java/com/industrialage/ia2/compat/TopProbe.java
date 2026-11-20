package com.industrialage.ia2.compat;

import com.industrialage.ia2.industrial.PowerNode;
import com.industrialage.ia2.industrial.WorkshopNode;
import com.industrialage.ia2.logistics.FluidContents;
import com.industrialage.ia2.logistics.TransportNode;
import com.industrialage.ia2.power.EuBuffer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeHitEntityData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoEntityProvider;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

final class TopProbe {
    private TopProbe() {}

    static Void register(ITheOneProbe probe) {
        probe.registerProvider(new TopBlockProvider());
        probe.registerEntityProvider(new Entities());
        return null;
    }

    abstract static class Blocks implements IProbeInfoProvider {
        @Override
        public void addProbeInfo(
                ProbeMode mode,
                IProbeInfo info,
                Player player,
                Level level,
                BlockState state,
                IProbeHitData hit) {
            var pos = hit.getPos();
            if (!(level instanceof ServerLevel server) || !level.hasChunkAt(pos)) return;
            var device = level.getBlockEntity(pos);
            if (device instanceof EuBuffer buffer) {
                var reading = buffer.bufferedEu();
                if (reading.capacity() > 0) {
                    info.text(
                            Component.translatable(
                                    "top.ic2.energy",
                                    eu(reading.stored()),
                                    eu(reading.capacity())));
                }
            }
            if (TopHost.isCable(state)) {
                info.text(Component.translatable("top.ic2.conductor"));
            }
            if (device instanceof TransportNode transport) {
                transport.fluidContents().ifPresent(contents -> fluid(info, contents));
            } else if (device instanceof WorkshopNode workshop) {
                workshop.fluidContents().forEach(contents -> fluid(info, contents));
            } else if (device instanceof PowerNode power) {
                power.fluidContents().forEach(contents -> fluid(info, contents));
                power.electricalReactorOutput(server)
                        .ifPresent(
                                output ->
                                        info.text(
                                                Component.translatable(
                                                        "top.ic2.output", eu(output))));
            }
        }
    }

    private static void fluid(IProbeInfo info, FluidContents contents) {
        info.text(
                Component.translatable(
                        "top.ic2.fluid", contents.name(), contents.amount(), contents.capacity()));
    }

    private static String eu(double amount) {
        return BigDecimal.valueOf(amount)
                .setScale(3, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static final class Entities implements IProbeInfoEntityProvider {
        @Override
        public String getID() {
            return "ic2:electric_entity";
        }

        @Override
        public void addProbeEntityInfo(
                ProbeMode mode,
                IProbeInfo info,
                Player player,
                Level level,
                Entity entity,
                IProbeHitEntityData hit) {
            if (!(level instanceof ServerLevel) || !TopHost.electricBoat(entity)) return;
            var pack = TopHost.riderBattery(entity);
            info.text(
                    pack.isEmpty()
                            ? Component.translatable("top.ic2.no_rider_battery")
                            : Component.translatable(
                                    "top.ic2.rider_battery",
                                    pack.getHoverName(),
                                    eu(TopHost.stored(pack)),
                                    eu(TopHost.capacity(pack))));
        }
    }
}
