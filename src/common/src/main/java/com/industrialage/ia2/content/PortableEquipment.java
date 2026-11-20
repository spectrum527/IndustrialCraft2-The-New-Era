package com.industrialage.ia2.content;

import java.util.List;

/** Electrical limits of portable items shared by the native inventory adapters. */
public enum PortableEquipment {
    RE_BATTERY("re_battery", 10_000, 100, 1, Kind.BATTERY),
    SINGLE_USE_BATTERY("single_use_battery", 1_200, 1_200, 1, Kind.DISPOSABLE),
    ADVANCED_RE_BATTERY("advanced_re_battery", 100_000, 256, 2, Kind.BATTERY),
    ENERGY_CRYSTAL("energy_crystal", 1_000_000, 2_048, 3, Kind.BATTERY),
    LAPOTRON_CRYSTAL("lapotron_crystal", 10_000_000, 8_092, 4, Kind.BATTERY),
    CHARGING_RE_BATTERY("charging_re_battery", 40_000, 128, 1, Kind.CHARGER),
    ADVANCED_CHARGING_RE_BATTERY("advanced_charging_re_battery", 400_000, 1_024, 2, Kind.CHARGER),
    CHARGING_ENERGY_CRYSTAL("charging_energy_crystal", 4_000_000, 8_192, 3, Kind.CHARGER),
    CHARGING_LAPOTRON_CRYSTAL("charging_lapotron_crystal", 40_000_000, 32_768, 4, Kind.CHARGER),
    BATPACK("batpack", 60_000, 100, 1, Kind.PACK),
    ADVANCED_BATPACK("advanced_batpack", 600_000, 1_000, 2, Kind.PACK),
    ENERGY_PACK("energy_pack", 2_000_000, 1_000, 3, Kind.PACK),
    ELECTRIC_JETPACK("electric_jetpack", 30_000, 60, 1, Kind.FLIGHT),
    NIGHT_VISION_GOGGLES("night_vision_goggles", 200_000, 200, 1, Kind.ARMOR),
    NANO_HELMET("nano_helmet", 1_000_000, 1_600, 3, Kind.ARMOR),
    NANO_CHESTPLATE("nano_chestplate", 1_000_000, 1_600, 3, Kind.ARMOR),
    NANO_LEGGINGS("nano_leggings", 1_000_000, 1_600, 3, Kind.ARMOR),
    NANO_BOOTS("nano_boots", 1_000_000, 1_600, 3, Kind.ARMOR),
    QUANTUM_HELMET("quantum_helmet", 10_000_000, 12_000, 4, Kind.ARMOR),
    QUANTUM_CHESTPLATE("quantum_chestplate", 10_000_000, 12_000, 4, Kind.ARMOR),
    QUANTUM_LEGGINGS("quantum_leggings", 10_000_000, 12_000, 4, Kind.ARMOR),
    QUANTUM_BOOTS("quantum_boots", 10_000_000, 12_000, 4, Kind.ARMOR),
    MINING_LASER("mining_laser", 300_000, 512, 3, Kind.LASER),
    NANO_SABER("nano_saber", 160_000, 500, 3, Kind.TOOL),
    ELECTRIC_TREETAP("electric_treetap", 10_000, 100, 1, Kind.TOOL, 1, 50),
    ELECTRIC_WRENCH("electric_wrench", 12_000, 250, 1, Kind.TOOL, 1, 100),
    WIND_METER("wind_meter", 10_000, 100, 1, Kind.TOOL, 1, 50),
    CROP_ANALYZER("cropnalyzer", 100_000, 128, 2, Kind.SCANNER),
    OD_SCANNER("scanner", 100_000, 128, 1, Kind.SCANNER),
    OV_SCANNER("advanced_scanner", 1_000_000, 512, 2, Kind.SCANNER),
    OBSCURATOR("obscurator", 100_000, 250, 2, Kind.SCANNER),
    DRILL("drill", 30_000, 100, 1, Kind.DRILL, 8, 50),
    DIAMOND_DRILL("diamond_drill", 30_000, 100, 1, Kind.DRILL, 16, 80),
    IRIDIUM_DRILL("iridium_drill", 300_000, 1_000, 3, Kind.DRILL, 24, 800),
    CHAINSAW("chainsaw", 30_000, 100, 1, Kind.SAW, 12, 100);

    /** Item roles used by charge slots, player packs, and mining tools. */
    public enum Kind {
        BATTERY,
        DISPOSABLE,
        CHARGER,
        PACK,
        FLIGHT,
        ARMOR,
        LASER,
        SCANNER,
        TOOL,
        DRILL,
        SAW
    }

    /** Registry path without the IC2 namespace. */
    public final String path;

    /** Maximum charge, in EU. */
    public final int capacity;

    /** Maximum ordinary charge transfer per tick, in EU. */
    public final int rate;

    /** Minimum electrical tier of an item charger. */
    public final int tier;

    /** Native item role. */
    public final Kind kind;

    /** Charged mining speed on the item's supported blocks. */
    public final float speed;

    /** EU required for one powered mining operation. */
    public final int cost;

    PortableEquipment(String path, int capacity, int rate, int tier, Kind kind) {
        this(path, capacity, rate, tier, kind, 1, 0);
    }

    PortableEquipment(
            String path, int capacity, int rate, int tier, Kind kind, float speed, int cost) {
        this.path = path;
        this.capacity = capacity;
        this.rate = rate;
        this.tier = tier;
        this.kind = kind;
        this.speed = speed;
        this.cost = cost;
    }

    /** Item registrations in stable order. */
    public static final List<PortableEquipment> CATALOG = List.of(values());

    /** Whether the normal machine discharge slot may draw power from this item. */
    public boolean canProvide() {
        return kind == Kind.BATTERY
                || kind == Kind.DISPOSABLE
                || kind == Kind.CHARGER
                || kind == Kind.PACK;
    }

    /** Whether this item can be worn in the chest equipment slot. */
    public boolean wearable() {
        return kind == Kind.PACK || kind == Kind.FLIGHT;
    }

    /** Base stack limit while the item has no stored charge. */
    public int emptyStackSize() {
        if (this == RE_BATTERY || kind == Kind.DISPOSABLE) return 64;
        return kind == Kind.BATTERY || kind == Kind.CHARGER ? 16 : 1;
    }

    /** Removes non-finite, negative, or over-capacity charge read from an item. */
    public double stored(double energy) {
        return Double.isFinite(energy) ? Math.min(capacity, Math.max(0, energy)) : 0;
    }

    /** Amount this item can accept at the supplied charger tier, in EU. */
    public double chargeAmount(double offered, double stored, int chargerTier) {
        return kind != Kind.DISPOSABLE
                        && chargerTier >= tier
                        && Double.isFinite(offered)
                        && offered > 0
                ? Math.min(Math.min(rate, offered), capacity - stored(stored))
                : 0;
    }

    /** Amount a compatible machine can receive from this item during one discharge. */
    public double dischargeAmount(double requested, double stored, int machineTier) {
        return canProvide() && machineTier >= tier && Double.isFinite(requested) && requested > 0
                ? Math.min(Math.min(rate, requested), stored(stored))
                : 0;
    }
}
