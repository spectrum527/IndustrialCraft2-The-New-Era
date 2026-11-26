package com.industrialage.ia2.compat;

import com.industrialage.ia2.industrial.PowerNode;
import com.industrialage.ia2.industrial.WorkshopNode;
import com.industrialage.ia2.logistics.FluidContents;
import com.industrialage.ia2.logistics.TransportNode;
import com.industrialage.ia2.power.EuBuffer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared server-to-client telemetry serialization for IC2 blocks and electric vehicles, consumed by
 * Jade and WTHIT without linking any optional mod APIs.
 */
final class Ic2ProbeData {
    private static final String PRESENT = "ic2_probe";
    private static final String EU_STORED = "ic2_eu";
    private static final String EU_CAP = "ic2_eu_cap";
    private static final String CONDUCTOR = "ic2_cable";
    private static final String OUTPUT = "ic2_out";
    private static final String HAS_OUTPUT = "ic2_has_out";
    private static final String FLUIDS = "ic2_fluids";
    private static final String ENTITY_BOAT = "ic2_boat";
    private static final String HAS_BATTERY = "ic2_has_bat";
    private static final String BATTERY_NAME = "ic2_bat_name";

    private Ic2ProbeData() {}

    static void writeBlock(
            CompoundTag tag, ServerLevel level, BlockState state, BlockEntity device) {
        boolean any = false;
        if (device instanceof EuBuffer buffer) {
            var reading = buffer.bufferedEu();
            if (reading.capacity() > 0) {
                tag.putDouble(EU_STORED, reading.stored());
                tag.putDouble(EU_CAP, reading.capacity());
                any = true;
            }
        }
        if (TopHost.isCable(state)) {
            tag.putBoolean(CONDUCTOR, true);
            any = true;
        }
        var fluidList = new ListTag();
        if (device instanceof TransportNode transport) {
            transport.fluidContents().ifPresent(c -> addFluid(fluidList, c));
        } else if (device instanceof WorkshopNode workshop) {
            workshop.fluidContents().forEach(c -> addFluid(fluidList, c));
        } else if (device instanceof PowerNode power) {
            power.fluidContents().forEach(c -> addFluid(fluidList, c));
            power.electricalReactorOutput(level)
                    .ifPresent(
                            out -> {
                                tag.putBoolean(HAS_OUTPUT, true);
                                tag.putDouble(OUTPUT, out);
                            });
            if (ProbeNbt.getBoolean(tag, HAS_OUTPUT)) any = true;
        }
        if (!fluidList.isEmpty()) {
            tag.put(FLUIDS, fluidList);
            any = true;
        }
        if (any) {
            tag.putBoolean(PRESENT, true);
        }
    }

    static List<Component> readBlock(CompoundTag tag) {
        if (tag == null || !ProbeNbt.getBoolean(tag, PRESENT)) return List.of();
        var lines = new ArrayList<Component>();
        double cap = ProbeNbt.getDouble(tag, EU_CAP);
        if (cap > 0) {
            lines.add(
                    Component.translatable(
                            "top.ic2.energy", eu(ProbeNbt.getDouble(tag, EU_STORED)), eu(cap)));
        }
        if (ProbeNbt.getBoolean(tag, CONDUCTOR)) {
            lines.add(Component.translatable("top.ic2.conductor"));
        }
        for (var entry : ProbeNbt.getCompounds(tag, FLUIDS)) {
            lines.add(
                    Component.translatable(
                            "top.ic2.fluid",
                            ProbeNbt.getString(entry, "name"),
                            ProbeNbt.getInt(entry, "amount"),
                            ProbeNbt.getInt(entry, "cap")));
        }
        if (ProbeNbt.getBoolean(tag, HAS_OUTPUT)) {
            lines.add(
                    Component.translatable("top.ic2.output", eu(ProbeNbt.getDouble(tag, OUTPUT))));
        }
        return lines;
    }

    static void writeEntity(CompoundTag tag, Entity entity) {
        if (!TopHost.electricBoat(entity)) return;
        tag.putBoolean(ENTITY_BOAT, true);
        var pack = TopHost.riderBattery(entity);
        if (!pack.isEmpty()) {
            tag.putBoolean(HAS_BATTERY, true);
            tag.putString(BATTERY_NAME, pack.getHoverName().getString());
            tag.putDouble(EU_STORED, TopHost.stored(pack));
            tag.putDouble(EU_CAP, TopHost.capacity(pack));
        }
    }

    static List<Component> readEntity(CompoundTag tag) {
        if (tag == null || !ProbeNbt.getBoolean(tag, ENTITY_BOAT)) return List.of();
        if (!ProbeNbt.getBoolean(tag, HAS_BATTERY)) {
            return List.of(Component.translatable("top.ic2.no_rider_battery"));
        }
        return List.of(
                Component.translatable(
                        "top.ic2.rider_battery",
                        ProbeNbt.getString(tag, BATTERY_NAME),
                        eu(ProbeNbt.getDouble(tag, EU_STORED)),
                        eu(ProbeNbt.getDouble(tag, EU_CAP))));
    }

    private static void addFluid(ListTag list, FluidContents contents) {
        var entry = new CompoundTag();
        entry.putString("name", contents.name().getString());
        entry.putInt("amount", contents.amount());
        entry.putInt("cap", contents.capacity());
        list.add(entry);
    }

    private static String eu(double amount) {
        if (!Double.isFinite(amount)) return "0";
        var display = new DecimalFormat("0.###", DecimalFormatSymbols.getInstance(Locale.ROOT));
        display.setRoundingMode(RoundingMode.HALF_UP);
        String digits = display.format(BigDecimal.valueOf(amount));
        return digits.equals("-0") ? "0" : digits;
    }
}
