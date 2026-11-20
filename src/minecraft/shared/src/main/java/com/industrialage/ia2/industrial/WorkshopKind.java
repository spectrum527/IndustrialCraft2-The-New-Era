package com.industrialage.ia2.industrial;

enum WorkshopKind {
    METAL_FORMER("metal_former", 200, 10, 4000, 1),
    ORE_WASHER("ore_washing_plant", 500, 16, 16000, 1),
    CENTRIFUGE("centrifuge", 500, 48, 48000, 2),
    BLAST_FURNACE("blast_furnace", 6000, 0, 0, 0),
    INDUCTION("induction_furnace", 200, 16, 10000, 2),
    RECYCLER("recycler", 45, 1, 180, 1),
    BLOCK_CUTTER("block_cutter", 500, 32, 32000, 2),
    CANNER("canner", 100, 1, 1600, 1),
    SOLID_CANNER("solid_canner", 100, 1, 1600, 1),
    COKE_KILN("coke_kiln", 1200, 0, 0, 0),
    COKE_GRATE("coke_kiln_grate", 0, 0, 0, 0),
    COKE_HATCH("coke_kiln_hatch", 0, 0, 0, 0),
    UU_ASSEMBLY("uu_assembly_bench", 160, 4, 1600, 1),
    SOLAR_DISTILLER("solar_distiller", 64, 0, 0, 0),
    HEAT_EXCHANGER("liquid_heat_exchanger", 0, 0, 0, 0),
    WEIGHTED_DISTRIBUTOR("weighted_fluid_distributor", 0, 0, 0, 0),
    CLASSIC_CANNER("classic_canner", 100, 1, 600, 1),
    FLUID_BOTTLER("fluid_bottler", 100, 2, 400, 1),
    FLUID_REGULATOR("fluid_regulator", 20, 10, 10000, 1),
    FLUID_DISTRIBUTOR("fluid_distributor", 0, 0, 0, 0),
    PUMP("pump", 20, 2, 40, 1),
    ELECTROLYZER("electrolyzer", 200, 32, 32000, 2),
    STIRLING("stirling_generator", 0, 0, 1000, 1),
    FLUID_HEAT("fluid_heat_generator", 0, 0, 0, 0),
    RADIOISOTOPE_HEAT("rt_heat_generator", 0, 0, 0, 0),
    FERMENTER("fermenter", 0, 0, 0, 0),
    SEMIFLUID("semifluid_generator", 0, 0, 16000, 1),
    STEAM_BOILER("steam_generator", 0, 0, 0, 0),
    STEAM_REPRESSURIZER("steam_repressurizer", 0, 16, 10000, 2),
    CONDENSER("condenser", 0, 2, 32000, 2),
    STEAM_TURBINE("steam_kinetic_generator", 0, 0, 0, 0),
    KINETIC("kinetic_generator", 0, 0, 10000, 3),
    ELECTRIC_KINETIC("electric_kinetic_generator", 0, 0, 10000, 4),
    MANUAL_KINETIC("manual_kinetic_generator", 0, 0, 0, 0),
    STIRLING_KINETIC("stirling_kinetic_generator", 0, 0, 0, 0),
    WIND_KINETIC("wind_kinetic_generator", 0, 0, 0, 0),
    WATER_KINETIC("water_kinetic_generator", 0, 0, 0, 0),
    CLASSIC_ELECTROLYZER("classic_electrolyzer", 200, 0, 0, 0),
    MASS_FABRICATOR("mass_fabricator", 0, 0, 1000000, 4),
    CLASSIC_MASS("classic_mass_fabricator", 0, 0, 1000000, 4),
    MATTER_GENERATOR("matter_generator", 0, 0, 1000000, 4),
    UU_SCANNER("uu_scanner", 3300, 256, 1000000, 4),
    PATTERN_STORAGE("pattern_storage", 0, 0, 0, 0),
    REPLICATOR("replicator", 3, 32, 2000000, 4),
    ELECTRIC_HEAT("electric_heat_generator", 0, 0, 10000, 4),
    SOLID_HEAT("solid_heat_generator", 0, 0, 0, 0);

    final String path;
    final int duration;
    final int workingEu;
    final int capacity;
    final int tier;

    WorkshopKind(String path, int duration, int workingEu, int capacity, int tier) {
        this.path = path;
        this.duration = duration;
        this.workingEu = workingEu;
        this.capacity = capacity;
        this.tier = tier;
    }

    int modes() {
        return switch (this) {
            case METAL_FORMER -> 3;
            case CANNER -> 4;
            case FLUID_REGULATOR, FLUID_DISTRIBUTOR, STEAM_BOILER, CLASSIC_ELECTROLYZER -> 2;
            case REPLICATOR -> 3;
            default -> 1;
        };
    }

    int inputs(int mode) {
        return switch (this) {
            case CANNER -> mode == 1 || mode == 2 ? 1 : 2;
            case INDUCTION, CLASSIC_CANNER, SOLID_CANNER, FLUID_BOTTLER, UU_ASSEMBLY -> 2;
            case RADIOISOTOPE_HEAT -> 6;
            case ELECTRIC_HEAT,
                    FLUID_REGULATOR,
                    FLUID_DISTRIBUTOR,
                    ELECTROLYZER,
                    STIRLING,
                    FERMENTER,
                    STEAM_REPRESSURIZER,
                    CONDENSER,
                    STEAM_BOILER,
                    KINETIC,
                    MANUAL_KINETIC,
                    STIRLING_KINETIC,
                    PATTERN_STORAGE,
                    REPLICATOR,
                    SOLAR_DISTILLER,
                    HEAT_EXCHANGER,
                    WEIGHTED_DISTRIBUTOR,
                    COKE_GRATE ->
                    0;
            default -> 1;
        };
    }

    String recipes(int mode) {
        return switch (this) {
            case METAL_FORMER ->
                    "metal_former_"
                            + switch (mode) {
                                case 1 -> "rolling";
                                case 2 -> "cutting";
                                default -> "extruding";
                            };
            case CANNER -> mode == 0 ? "canner_bottle" : mode == 3 ? "canner_enrich" : "";
            case CLASSIC_CANNER, SOLID_CANNER -> "canner_bottle";
            case ORE_WASHER -> "ore_washer";
            default -> path;
        };
    }

    int outputSlots() {
        return switch (this) {
            case ORE_WASHER, CENTRIFUGE -> 3;
            case BLAST_FURNACE, INDUCTION -> 2;
            case ELECTRIC_HEAT,
                    FLUID_REGULATOR,
                    FLUID_DISTRIBUTOR,
                    ELECTROLYZER,
                    STIRLING,
                    RADIOISOTOPE_HEAT,
                    STEAM_REPRESSURIZER,
                    CONDENSER,
                    STEAM_BOILER,
                    STEAM_TURBINE,
                    KINETIC,
                    ELECTRIC_KINETIC,
                    MANUAL_KINETIC,
                    STIRLING_KINETIC,
                    WIND_KINETIC,
                    WATER_KINETIC,
                    UU_SCANNER,
                    PATTERN_STORAGE,
                    MATTER_GENERATOR,
                    SOLAR_DISTILLER,
                    HEAT_EXCHANGER,
                    WEIGHTED_DISTRIBUTOR,
                    COKE_GRATE,
                    COKE_HATCH ->
                    0;
            default -> 1;
        };
    }

    boolean sixDirections() {
        return switch (this) {
            case FLUID_REGULATOR,
                    FLUID_DISTRIBUTOR,
                    PUMP,
                    STIRLING,
                    ELECTRIC_HEAT,
                    SOLID_HEAT,
                    RADIOISOTOPE_HEAT,
                    FLUID_HEAT,
                    FERMENTER,
                    STEAM_BOILER,
                    CONDENSER,
                    STEAM_TURBINE,
                    KINETIC,
                    ELECTRIC_KINETIC,
                    MANUAL_KINETIC,
                    STIRLING_KINETIC,
                    WIND_KINETIC,
                    WATER_KINETIC,
                    HEAT_EXCHANGER,
                    WEIGHTED_DISTRIBUTOR,
                    COKE_GRATE,
                    COKE_HATCH ->
                    true;
            default -> false;
        };
    }

    boolean generatesPower() {
        return this == STIRLING || this == SEMIFLUID || this == KINETIC;
    }

    boolean dischargesItem() {
        return switch (this) {
            case METAL_FORMER,
                    ORE_WASHER,
                    CENTRIFUGE,
                    INDUCTION,
                    RECYCLER,
                    BLOCK_CUTTER,
                    CANNER,
                    SOLID_CANNER,
                    CLASSIC_CANNER,
                    FLUID_BOTTLER,
                    FLUID_REGULATOR,
                    PUMP,
                    ELECTROLYZER,
                    CONDENSER,
                    ELECTRIC_KINETIC,
                    UU_SCANNER,
                    REPLICATOR,
                    ELECTRIC_HEAT ->
                    true;
            default -> false;
        };
    }

    boolean chargesItem() {
        return this == SEMIFLUID;
    }

    boolean auxiliarySlot() {
        return this == BLOCK_CUTTER || this == ELECTRIC_HEAT || this == BLAST_FURNACE;
    }

    boolean singleTank() {
        return switch (this) {
            case FLUID_BOTTLER,
                    FLUID_REGULATOR,
                    FLUID_DISTRIBUTOR,
                    WEIGHTED_DISTRIBUTOR,
                    PUMP,
                    SEMIFLUID,
                    FLUID_HEAT ->
                    true;
            default -> false;
        };
    }

    int byproductVolume() {
        return this == ELECTROLYZER || this == STEAM_TURBINE ? outputVolume() : 0;
    }

    boolean heatMeter() {
        return switch (this) {
            case CENTRIFUGE,
                    INDUCTION,
                    BLAST_FURNACE,
                    HEAT_EXCHANGER,
                    SOLID_HEAT,
                    STEAM_BOILER,
                    FERMENTER,
                    ELECTRIC_HEAT,
                    FLUID_HEAT,
                    RADIOISOTOPE_HEAT ->
                    true;
            default -> false;
        };
    }

    int inputVolume() {
        return switch (this) {
            case ORE_WASHER, BLAST_FURNACE, CANNER, FLUID_BOTTLER, ELECTROLYZER, PUMP -> 8000;
            case FLUID_REGULATOR, FLUID_DISTRIBUTOR, WEIGHTED_DISTRIBUTOR -> 1000;
            case FERMENTER, HEAT_EXCHANGER -> 2000;
            case REPLICATOR -> 16000;
            case SEMIFLUID,
                    FLUID_HEAT,
                    STEAM_BOILER,
                    STEAM_REPRESSURIZER,
                    STEAM_TURBINE,
                    CONDENSER,
                    STIRLING_KINETIC,
                    SOLAR_DISTILLER ->
                    10000;
            default -> 0;
        };
    }

    int outputVolume() {
        return switch (this) {
            case CANNER, ELECTROLYZER, FLUID_BOTTLER, PUMP -> 8000;
            case FLUID_REGULATOR, FLUID_DISTRIBUTOR, WEIGHTED_DISTRIBUTOR -> 1000;
            case COKE_GRATE -> 10000;
            case HEAT_EXCHANGER -> 2000;
            case MATTER_GENERATOR -> 16000;
            case FERMENTER,
                    STEAM_BOILER,
                    STEAM_REPRESSURIZER,
                    STEAM_TURBINE,
                    CONDENSER,
                    STIRLING_KINETIC,
                    SOLAR_DISTILLER ->
                    10000;
            default -> 0;
        };
    }
}
