package com.industrialage.ia2.industrial;

import java.util.List;
import java.util.Locale;

/** The electrical generating and transforming devices and their game registry paths. */
enum PowerKind {
    NUCLEAR_REACTOR,
    REACTOR_CHAMBER,
    SOLAR_GENERATOR,
    WATER_GENERATOR,
    WIND_GENERATOR,
    GEO_GENERATOR,
    RT_GENERATOR,
    CREATIVE_GENERATOR,
    LV_TRANSFORMER,
    MV_TRANSFORMER,
    HV_TRANSFORMER,
    EV_TRANSFORMER,
    ITNT,
    NUKE,
    CLASSIC_NUKE,
    REACTOR_VESSEL,
    REACTOR_ACCESS_HATCH,
    REACTOR_FLUID_PORT,
    REACTOR_REDSTONE_PORT,
    CLASSIC_MFE,
    CLASSIC_MFSU,
    RCI_LZH,
    RCI_RSH,
    TRADE_O_MAT,
    ENERGY_O_MAT,
    TELEPORTER,
    TESLA_COIL,
    MAGNETIZER;

    final String path = name().toLowerCase(Locale.ROOT);
    static final List<String> PARTS =
            List.of(
                    "fuel_rod",
                    "uranium_fuel_rod",
                    "dual_uranium_fuel_rod",
                    "quad_uranium_fuel_rod",
                    "mox_fuel_rod",
                    "dual_mox_fuel_rod",
                    "quad_mox_fuel_rod",
                    "depleted_uranium_fuel_rod",
                    "depleted_dual_uranium_fuel_rod",
                    "depleted_quad_uranium_fuel_rod",
                    "depleted_mox_fuel_rod",
                    "depleted_dual_mox_fuel_rod",
                    "depleted_quad_mox_fuel_rod",
                    "neutron_reflector",
                    "thick_neutron_reflector",
                    "iridium_neutron_reflector",
                    "heat_vent",
                    "advanced_heat_vent",
                    "reactor_heat_vent",
                    "overclocked_heat_vent",
                    "component_heat_vent",
                    "reactor_coolant_cell",
                    "triple_reactor_coolant_cell",
                    "sextuple_reactor_coolant_cell",
                    "rsh_condensator",
                    "lzh_condensator",
                    "reactor_plating",
                    "reactor_heat_plating",
                    "containment_reactor_plating",
                    "rtg_pellet",
                    "frequency_transmitter",
                    "heatpack",
                    "heat_exchanger",
                    "advanced_heat_exchanger",
                    "component_heat_exchanger",
                    "reactor_heat_exchanger",
                    "depleted_isotope_fuel_rod",
                    "lithium_fuel_rod",
                    "near_depleted_uranium",
                    "re_enriched_uranium",
                    "overclocker_upgrade",
                    "transformer_upgrade",
                    "energy_storage_upgrade",
                    "ejector_upgrade",
                    "advanced_ejector_upgrade",
                    "pulling_upgrade",
                    "advanced_pulling_upgrade",
                    "fluid_ejector_upgrade",
                    "fluid_pulling_upgrade",
                    "redstone_inverter_upgrade",
                    "remote_interface_upgrade");

    int chargeSlot() {
        return switch (this) {
            case GEO_GENERATOR, WATER_GENERATOR -> 2;
            case RT_GENERATOR -> 6;
            case SOLAR_GENERATOR, WIND_GENERATOR, CLASSIC_MFE, CLASSIC_MFSU -> 0;
            default -> -1;
        };
    }

    int itemTier() {
        return this == CLASSIC_MFSU ? 3 : this == CLASSIC_MFE ? 2 : 1;
    }

    boolean chargeBlock() {
        return this == ITNT || this == NUKE || this == CLASSIC_NUKE;
    }

    boolean vesselPart() {
        return this == REACTOR_VESSEL
                || this == REACTOR_ACCESS_HATCH
                || this == REACTOR_FLUID_PORT
                || this == REACTOR_REDSTONE_PORT;
    }

    int lowTier() {
        return switch (this) {
            case LV_TRANSFORMER -> 1;
            case MV_TRANSFORMER -> 2;
            case HV_TRANSFORMER -> 3;
            case EV_TRANSFORMER -> 4;
            default -> 0;
        };
    }

    boolean playerMachine() {
        return switch (this) {
            case RCI_RSH, RCI_LZH, TRADE_O_MAT, ENERGY_O_MAT, TELEPORTER, TESLA_COIL, MAGNETIZER ->
                    true;
            default -> false;
        };
    }

    boolean trader() {
        return this == TRADE_O_MAT || this == ENERGY_O_MAT;
    }

    boolean poweredPlayerMachine() {
        return switch (this) {
            case RCI_RSH, RCI_LZH, ENERGY_O_MAT, TESLA_COIL, MAGNETIZER -> true;
            default -> false;
        };
    }

    boolean storage() {
        return this == CLASSIC_MFE || this == CLASSIC_MFSU;
    }
}
