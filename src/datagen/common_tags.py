"""Emit standard ecosystem common tags (c: on NeoForge, forge: on Forge 1.19.2) for IC2 items, blocks, and fluids."""

import csv

_DYE_COLORS = (
    "black", "blue", "brown", "cyan", "gray", "green", "light_blue", "light_gray",
    "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow",
)

_AUX_ITEM_MODELS = frozenset({
    "re_battery_1", "re_battery_2", "re_battery_3", "re_battery_4", "nano_saber_active",
    "quantum_helmet_charged", "quantum_chestplate_charged", "quantum_leggings_charged", "quantum_boots_charged",
})

_FLUID_NAMES = (
    "air", "biogas", "biomass", "construction_foam", "coolant", "creosote",
    "distilled_water", "heavy_water", "hot_coolant", "hot_water", "hydrogen",
    "oxygen", "pahoehoe_lava", "steam", "superheated_steam", "uu_matter", "weed_ex",
)

# Vanilla materials that map directly to vanilla #minecraft:* tags on all loaders.
_VANILLA_TAG_MAPPINGS = {
    "minecraft:dirt": "#minecraft:dirt",
}

# Standard materials mapped to identical tag paths under both #forge:* (1.19.2) and #c:* (1.21.1/26.x).
_SHARED_COMMON_ITEM_TAGS = {
    # Ingots
    "minecraft:iron_ingot": "ingots/iron",
    "minecraft:gold_ingot": "ingots/gold",
    "minecraft:copper_ingot": "ingots/copper",
    "ic2:tin_ingot": "ingots/tin",
    "ic2:bronze_ingot": "ingots/bronze",
    "ic2:steel_ingot": "ingots/steel",
    "ic2:refined_iron_ingot": "ingots/refined_iron",
    "ic2:lead_ingot": "ingots/lead",
    "ic2:silver_ingot": "ingots/silver",
    "ic2:uranium_ingot": "ingots/uranium",
    "ic2:mixed_metal_ingot": "ingots/mixed_metal",
    # Plates & Dense Plates
    "ic2:alloy": "plates/advanced_alloy",
    "ic2:bronze_plate": "plates/bronze",
    "ic2:carbon_plate": "plates/carbon",
    "ic2:copper_plate": "plates/copper",
    "ic2:gold_plate": "plates/gold",
    "ic2:iron_plate": "plates/iron",
    "ic2:lapis_plate": "plates/lapis",
    "ic2:lead_plate": "plates/lead",
    "ic2:obsidian_plate": "plates/obsidian",
    "ic2:steel_plate": "plates/steel",
    "ic2:tin_plate": "plates/tin",
    "ic2:dense_bronze_plate": "dense_plates/bronze",
    "ic2:dense_copper_plate": "dense_plates/copper",
    "ic2:dense_gold_plate": "dense_plates/gold",
    "ic2:dense_iron_plate": "dense_plates/iron",
    "ic2:dense_lapis_plate": "dense_plates/lapis",
    "ic2:dense_lead_plate": "dense_plates/lead",
    "ic2:dense_obsidian_plate": "dense_plates/obsidian",
    "ic2:dense_steel_plate": "dense_plates/steel",
    "ic2:dense_tin_plate": "dense_plates/tin",
    # Item Casings
    "ic2:bronze_casing": "item_casings/bronze",
    "ic2:copper_casing": "item_casings/copper",
    "ic2:gold_casing": "item_casings/gold",
    "ic2:iron_casing": "item_casings/iron",
    "ic2:lead_casing": "item_casings/lead",
    "ic2:steel_casing": "item_casings/steel",
    "ic2:tin_casing": "item_casings/tin",
    # Circuits
    "ic2:circuit": "circuits/basic",
    "ic2:advanced_circuit": "circuits/advanced",
    # Dusts
    "minecraft:redstone": "dusts/redstone",
    "minecraft:glowstone_dust": "dusts/glowstone",
    "ic2:ashes": "dusts/ash",
    "ic2:bronze_dust": "dusts/bronze",
    "ic2:clay_dust": "dusts/clay",
    "ic2:coal_dust": "dusts/coal",
    "ic2:coal_fuel_dust": "dusts/coal_fuel",
    "ic2:copper_dust": "dusts/copper",
    "ic2:diamond_dust": "dusts/diamond",
    "ic2:emerald_dust": "dusts/emerald",
    "ic2:ender_pearl_dust": "dusts/ender_pearl",
    "ic2:energium_dust": "dusts/energium",
    "ic2:gold_dust": "dusts/gold",
    "ic2:hydrated_tin_dust": "dusts/hydrated_tin",
    "ic2:iron_dust": "dusts/iron",
    "ic2:lapis_dust": "dusts/lapis",
    "ic2:lead_dust": "dusts/lead",
    "ic2:lithium_dust": "dusts/lithium",
    "ic2:netherrack_dust": "dusts/netherrack",
    "ic2:obsidian_dust": "dusts/obsidian",
    "ic2:silicon_dioxide_dust": "dusts/silicon_dioxide",
    "ic2:silver_dust": "dusts/silver",
    "ic2:stone_dust": "dusts/stone",
    "ic2:sulfur_dust": "dusts/sulfur",
    "ic2:tin_dust": "dusts/tin",
    # Small / Tiny Dusts (small_dusts/* also includes #tiny_dusts/*)
    "ic2:small_bronze_dust": "small_dusts/bronze",
    "ic2:small_copper_dust": "small_dusts/copper",
    "ic2:small_diamond_dust": "small_dusts/diamond",
    "ic2:small_emerald_dust": "small_dusts/emerald",
    "ic2:small_gold_dust": "small_dusts/gold",
    "ic2:small_iron_dust": "small_dusts/iron",
    "ic2:small_lapis_dust": "small_dusts/lapis",
    "ic2:small_lead_dust": "small_dusts/lead",
    "ic2:small_lithium_dust": "small_dusts/lithium",
    "ic2:small_obsidian_dust": "small_dusts/obsidian",
    "ic2:small_silver_dust": "small_dusts/silver",
    "ic2:small_sulfur_dust": "small_dusts/sulfur",
    "ic2:small_tin_dust": "small_dusts/tin",
    # Raw Materials (raw_materials/* also includes #raw_ores/* for IC2 metals)
    "minecraft:raw_copper": "raw_materials/copper",
    "minecraft:raw_gold": "raw_materials/gold",
    "minecraft:raw_iron": "raw_materials/iron",
    "ic2:raw_tin": "raw_materials/tin",
    "ic2:raw_lead": "raw_materials/lead",
    "ic2:raw_uranium": "raw_materials/uranium",
    # Crushed & Purified Ores
    "ic2:crushed_copper": "crushed_raw_materials/copper",
    "ic2:crushed_gold": "crushed_raw_materials/gold",
    "ic2:crushed_iron": "crushed_raw_materials/iron",
    "ic2:crushed_lead": "crushed_raw_materials/lead",
    "ic2:crushed_silver": "crushed_raw_materials/silver",
    "ic2:crushed_tin": "crushed_raw_materials/tin",
    "ic2:crushed_uranium": "crushed_raw_materials/uranium",
    "ic2:purified_copper": "purified_ores/copper",
    "ic2:purified_gold": "purified_ores/gold",
    "ic2:purified_iron": "purified_ores/iron",
    "ic2:purified_lead": "purified_ores/lead",
    "ic2:purified_silver": "purified_ores/silver",
    "ic2:purified_tin": "purified_ores/tin",
    "ic2:purified_uranium": "purified_ores/uranium",
    # Ores
    "ic2:tin_ore": "ores/tin",
    "ic2:lead_ore": "ores/lead",
    "ic2:uranium_ore": "ores/uranium",
    "ic2:iridium_ore": "ores/iridium",
    # Storage Blocks (Forge storage_blocks/* also includes #blocks/* for IC2 blocks)
    "minecraft:iron_block": "storage_blocks/iron",
    "minecraft:gold_block": "storage_blocks/gold",
    "minecraft:copper_block": "storage_blocks/copper",
    "minecraft:coal_block": "storage_blocks/coal",
    "minecraft:lapis_block": "storage_blocks/lapis",
    "minecraft:redstone_block": "storage_blocks/redstone",
    "ic2:tin_block": "storage_blocks/tin",
    "ic2:bronze_block": "storage_blocks/bronze",
    "ic2:lead_block": "storage_blocks/lead",
    "ic2:silver_block": "storage_blocks/silver",
    "ic2:steel_block": "storage_blocks/steel",
    "ic2:uranium_block": "storage_blocks/uranium",
    "ic2:raw_tin_block": "storage_blocks/raw_tin",
    "ic2:raw_lead_block": "storage_blocks/raw_lead",
    "ic2:raw_uranium_block": "storage_blocks/raw_uranium",
    # Gems, Nuggets, Rubber, Coke, Slag, Buckets, Crops & Vanilla Commons
    "minecraft:diamond": "gems/diamond",
    "minecraft:emerald": "gems/emerald",
    "minecraft:lapis_lazuli": "gems/lapis",
    "minecraft:quartz": "gems/quartz",
    "ic2:iridium_shard": "nuggets/iridium",
    "ic2:rubber": "rubbers",
    "ic2:coke": "coal_coke",
    "ic2:slag": "slag",
    "minecraft:stick": "rods/wooden",
    "minecraft:chest": "chests/wooden",
    "minecraft:glass_pane": "glass_panes",
    "minecraft:ender_pearl": "ender_pearls",
    "minecraft:wheat": "crops/wheat",
    "minecraft:wheat_seeds": "seeds/wheat",
    "minecraft:brown_mushroom": "mushrooms",
    **{f"ic2:{fluid}_bucket": f"buckets/{fluid}" for fluid in _FLUID_NAMES},
    **{f"minecraft:{color}_dye": f"dyes/{color}" for color in _DYE_COLORS},
}

# Vanilla materials whose standard common tag name differs between Forge 1.19.2 and NeoForge 1.21.1+.
_LOADER_DIFF_ITEM_TAGS = {
    "minecraft:glass": ("forge:glass", "c:glass_blocks"),
    "minecraft:obsidian": ("forge:obsidian", "c:obsidians"),
    "minecraft:cobblestone": ("forge:cobblestone", "c:cobblestones"),
    "minecraft:stone": ("forge:stone", "c:stones"),
    "minecraft:sand": ("forge:sand", "c:sands"),
    "minecraft:shears": ("forge:shears", "c:tools/shear"),
    "minecraft:brick": ("forge:ingots/brick", "c:bricks/normal"),
    "minecraft:slime_ball": ("forge:slimeballs", "c:slime_balls"),
    "minecraft:gravel": ("forge:gravel", "c:gravels"),
    "minecraft:leather": ("forge:leather", "c:leathers"),
    "minecraft:string": ("forge:string", "c:strings"),
}

# Vanilla materials with built-in standard #c:* tags on NeoForge 1.21.1+ but no built-in #forge:* tag on Forge 1.19.2.
_NEO_ONLY_COMMON_ITEM_TAGS = {
    "minecraft:bone_meal": "c:fertilizers",
    "minecraft:furnace": "c:player_workstations/furnaces",
    "minecraft:crafting_table": "c:player_workstations/crafting_tables",
    "minecraft:bucket": "c:buckets/empty",
    "minecraft:water_bucket": "c:buckets/water",
    "minecraft:lava_bucket": "c:buckets/lava",
    "minecraft:milk_bucket": "c:buckets/milk",
}

# Stateful / energy-network / heat-network / multiblock / container / owner-locked blocks
# that must not be relocated in-place by external mover mods.
_RELOCATION_NOT_SUPPORTED_BLOCKS = (
    # Base & extended energy machines, storage, cables
    "iron_furnace", "generator", "batbox", "macerator", "electric_furnace", "compressor", "extractor",
    "cesu", "mfe", "mfsu",
    "copper_cable", "insulated_copper_cable", "tin_cable", "insulated_tin_cable",
    "gold_cable", "insulated_gold_cable", "double_insulated_gold_cable",
    "iron_cable", "insulated_iron_cable", "double_insulated_iron_cable", "triple_insulated_iron_cable",
    "glass_fibre_cable", "detector_cable", "splitter_cable",
    "copper_foam_cable", "insulated_copper_foam_cable", "tin_foam_cable", "insulated_tin_foam_cable",
    "gold_foam_cable", "insulated_gold_foam_cable", "double_insulated_gold_foam_cable",
    "iron_foam_cable", "insulated_iron_foam_cable", "double_insulated_iron_foam_cable",
    "triple_insulated_iron_foam_cable", "glass_fibre_foam_cable", "detector_foam_cable", "splitter_foam_cable",
    # Logistics tanks & pipes
    "bronze_tank", "iron_tank", "steel_tank", "iridium_tank",
    "bronze_item_pipe_tiny", "bronze_item_pipe_small", "bronze_item_pipe_medium", "bronze_item_pipe_large",
    "bronze_fluid_pipe_tiny", "bronze_fluid_pipe_small", "bronze_fluid_pipe_medium", "bronze_fluid_pipe_large",
    "steel_item_pipe_tiny", "steel_item_pipe_small", "steel_item_pipe_medium", "steel_item_pipe_large",
    "steel_fluid_pipe_tiny", "steel_fluid_pipe_small", "steel_fluid_pipe_medium", "steel_fluid_pipe_large",
    # Chargepads
    "batbox_chargepad", "cesu_chargepad", "mfe_chargepad", "mfsu_chargepad",
    # Power, nuclear, transformers, teleporter & trading
    "nuclear_reactor", "reactor_chamber", "solar_generator", "water_generator", "geo_generator",
    "rt_generator", "creative_generator", "lv_transformer", "mv_transformer", "hv_transformer", "ev_transformer",
    "classic_mfe", "classic_mfsu", "wind_generator", "rci_lzh", "rci_rsh", "trade_o_mat", "energy_o_mat",
    "teleporter", "tesla_coil", "magnetizer", "reactor_vessel", "reactor_redstone_port",
    "reactor_fluid_port", "reactor_access_hatch", "nuke", "classic_nuke",
    # Storage boxes & personal safe
    "wooden_storage_box", "iron_storage_box", "bronze_storage_box", "steel_storage_box",
    "iridium_storage_box", "personal_chest", "industrial_workbench",
    # Workshop processing, fluid, kinetic, UU & coke kiln multiblock
    "metal_former", "ore_washing_plant", "centrifuge", "blast_furnace", "induction_furnace",
    "recycler", "block_cutter", "canner", "electric_heat_generator", "solid_heat_generator",
    "classic_canner", "fluid_bottler", "fluid_distributor", "fluid_regulator", "pump",
    "stirling_generator", "electrolyzer", "fluid_heat_generator", "rt_heat_generator",
    "fermenter", "steam_generator", "condenser", "steam_kinetic_generator", "kinetic_generator",
    "electric_kinetic_generator", "manual_kinetic_generator", "stirling_kinetic_generator",
    "wind_kinetic_generator", "water_kinetic_generator", "semifluid_generator", "steam_repressurizer",
    "classic_electrolyzer", "mass_fabricator", "classic_mass_fabricator", "matter_generator",
    "uu_scanner", "pattern_storage", "replicator", "solid_canner", "solar_distiller",
    "liquid_heat_exchanger", "weighted_fluid_distributor", "tank", "coke_kiln",
    "uu_assembly_bench", "coke_kiln_grate", "coke_kiln_hatch",
    # World machines
    "crop_harvester", "cropmatron", "classic_cropmatron", "miner", "advanced_miner", "terraformer",
    "batch_crafter", "item_buffer", "sorting_machine", "weighted_item_distributor", "chunk_loader",
)


def common_tag_for(item_id, forge):
    """Map a single material item ID to its loader-native common tag (#forge:* or #c:*), or keep direct."""
    if not item_id or item_id.startswith("#"):
        return item_id
    if item_id in _VANILLA_TAG_MAPPINGS:
        return _VANILLA_TAG_MAPPINGS[item_id]
    if item_id in _LOADER_DIFF_ITEM_TAGS:
        return "#" + _LOADER_DIFF_ITEM_TAGS[item_id][0 if forge else 1]
    if not forge and item_id in _NEO_ONLY_COMMON_ITEM_TAGS:
        return "#" + _NEO_ONLY_COMMON_ITEM_TAGS[item_id]
    tag_path = _SHARED_COMMON_ITEM_TAGS.get(item_id)
    if tag_path:
        return ("#forge:" if forge else "#c:") + tag_path
    return item_id


def common_tag_for_set(choices, forge):
    """Map known multi-item recipe-book ingredient sets to standard vanilla or common tags."""
    items = set(choices)
    ns = "forge" if forge else "c"
    if items == {"ic2:industrial_diamond", "minecraft:diamond"}:
        return f"#{ns}:gems/diamond"
    if items == {"minecraft:chest", "minecraft:trapped_chest"}:
        return f"#{ns}:chests/wooden"
    if items == {"ic2:rubber_log", "ic2:rubber_wood", "ic2:stripped_rubber_log", "ic2:stripped_rubber_wood"}:
        return "#ic2:rubber_logs"
    if "minecraft:oak_planks" in items and "minecraft:spruce_planks" in items:
        return "#minecraft:planks"
    if "minecraft:oak_log" in items and "minecraft:spruce_log" in items:
        return "#minecraft:logs"
    if "minecraft:white_wool" in items and "minecraft:black_wool" in items and len(items) == 16:
        return "#minecraft:wool"
    if "minecraft:white_carpet" in items and "minecraft:black_carpet" in items and len(items) == 16:
        return "#minecraft:wool_carpets"
    if "minecraft:oak_trapdoor" in items and "minecraft:iron_trapdoor" in items:
        return "#minecraft:trapdoors"
    return None


def generate(repo, out, forge, modern, write):
    ns = "forge" if forge else "c"
    item_dir = "tags/items" if forge else "tags/item"
    block_dir = "tags/blocks" if forge else "tags/block"
    fluid_dir = "tags/fluids" if forge else "tags/fluid"

    registered_items = {
        file.stem for file in (out / "assets/ic2/models/item").glob("*.json")
    } - _AUX_ITEM_MODELS
    registered_blocks = {
        file.stem for file in (out / "assets/ic2/blockstates").glob("*.json")
    }

    def available_entry(kind_dir, entry):
        target = entry["id"] if isinstance(entry, dict) else entry
        required = entry.get("required", True) if isinstance(entry, dict) else True
        if not required or target.startswith("#") or not target.startswith("ic2:"):
            return True
        path = target[4:]
        if kind_dir == item_dir:
            return path in registered_items
        if kind_dir == block_dir:
            return path in registered_blocks
        return True

    def filter_values(kind_dir, values):
        seen = set()
        result = []
        for entry in values:
            if not available_entry(kind_dir, entry):
                continue
            key = (entry["id"], entry.get("required", True)) if isinstance(entry, dict) else entry
            if key not in seen:
                seen.add(key)
                result.append(entry)
        return result

    def emit(kind_dir, tag_path, values, namespace=None, allow_empty=False):
        filtered = filter_values(kind_dir, values)
        if not filtered and not allow_empty:
            return False
        target_ns = namespace or ns
        write(f"data/{target_ns}/{kind_dir}/{tag_path}", {"replace": False, "values": filtered})
        return True

    def emit_group(kind_dir, parent, mapping, namespace=None):
        target_ns = namespace or ns
        emitted = []
        for name, values in mapping.items():
            if emit(kind_dir, f"{parent}/{name}", values, namespace=target_ns):
                emitted.append(f"#{target_ns}:{parent}/{name}")
        if emitted:
            emit(kind_dir, parent, emitted, namespace=target_ns)
        return emitted

    # ---- Ingots ----
    emit_group(item_dir, "ingots", {
        "tin": ["ic2:tin_ingot"],
        "bronze": ["ic2:bronze_ingot"],
        "steel": ["ic2:steel_ingot"],
        "refined_iron": ["ic2:refined_iron_ingot"],
        "lead": ["ic2:lead_ingot"],
        "silver": ["ic2:silver_ingot"],
        "uranium": ["ic2:uranium_ingot"],
        "mixed_metal": ["ic2:mixed_metal_ingot"],
    })

    # ---- Dusts ----
    emit_group(item_dir, "dusts", {
        "ash": ["ic2:ashes"],
        "bronze": ["ic2:bronze_dust"],
        "clay": ["ic2:clay_dust"],
        "coal": ["ic2:coal_dust"],
        "coal_fuel": ["ic2:coal_fuel_dust"],
        "copper": ["ic2:copper_dust"],
        "diamond": ["ic2:diamond_dust"],
        "emerald": ["ic2:emerald_dust"],
        "ender_pearl": ["ic2:ender_pearl_dust"],
        "energium": ["ic2:energium_dust"],
        "gold": ["ic2:gold_dust"],
        "hydrated_tin": ["ic2:hydrated_tin_dust"],
        "iron": ["ic2:iron_dust"],
        "lapis": ["ic2:lapis_dust"],
        "lead": ["ic2:lead_dust"],
        "lithium": ["ic2:lithium_dust"],
        "netherrack": ["ic2:netherrack_dust"],
        "obsidian": ["ic2:obsidian_dust"],
        "silicon_dioxide": ["ic2:silicon_dioxide_dust"],
        "silver": ["ic2:silver_dust"],
        "stone": ["ic2:stone_dust"],
        "sulfur": ["ic2:sulfur_dust"],
        "tin": ["ic2:tin_dust"],
    })

    # ---- Small / Tiny Dusts ----
    small_dusts = [
        "bronze", "copper", "diamond", "emerald", "gold", "iron",
        "lapis", "lead", "lithium", "obsidian", "silver", "sulfur", "tin",
    ]
    emitted_tiny, emitted_small = [], []
    for name in small_dusts:
        item_id = f"ic2:small_{name}_dust"
        if emit(item_dir, f"tiny_dusts/{name}", [item_id]):
            emitted_tiny.append(f"#{ns}:tiny_dusts/{name}")
            if emit(item_dir, f"small_dusts/{name}", [item_id, {"id": f"#{ns}:tiny_dusts/{name}", "required": False}]):
                emitted_small.append(f"#{ns}:small_dusts/{name}")
    if emitted_tiny:
        emit(item_dir, "tiny_dusts", emitted_tiny)
    if emitted_small:
        emit(item_dir, "small_dusts", emitted_small)

    # ---- Plates & Dense Plates ----
    emit_group(item_dir, "plates", {
        "advanced_alloy": ["ic2:alloy"],
        "bronze": ["ic2:bronze_plate"],
        "carbon": ["ic2:carbon_plate"],
        "copper": ["ic2:copper_plate"],
        "gold": ["ic2:gold_plate"],
        "iron": ["ic2:iron_plate"],
        "lapis": ["ic2:lapis_plate"],
        "lead": ["ic2:lead_plate"],
        "obsidian": ["ic2:obsidian_plate"],
        "steel": ["ic2:steel_plate"],
        "tin": ["ic2:tin_plate"],
    })

    dense_plates = ["bronze", "copper", "gold", "iron", "lapis", "lead", "obsidian", "steel", "tin"]
    emit_group(item_dir, "dense_plates", {name: [f"ic2:dense_{name}_plate"] for name in dense_plates})

    # ---- Item Casings ----
    casings = ["bronze", "copper", "gold", "iron", "lead", "steel", "tin"]
    emit_group(item_dir, "item_casings", {name: [f"ic2:{name}_casing"] for name in casings})

    # ---- Circuits & Batteries ----
    emit_group(item_dir, "circuits", {
        "basic": ["ic2:circuit"],
        "advanced": ["ic2:advanced_circuit"],
    })
    emit(item_dir, "batteries", [
        "ic2:re_battery", "ic2:advanced_re_battery", "ic2:single_use_battery",
        "ic2:energy_crystal", "ic2:lapotron_crystal",
        "ic2:charging_re_battery", "ic2:advanced_charging_re_battery",
        "ic2:charging_energy_crystal", "ic2:charging_lapotron_crystal",
    ])

    # ---- Cables (strictly cables/*; never wires/copper per A37) ----
    cables = {
        "copper": ["ic2:copper_cable", "ic2:insulated_copper_cable"],
        "tin": ["ic2:tin_cable", "ic2:insulated_tin_cable"],
        "gold": ["ic2:gold_cable", "ic2:insulated_gold_cable", "ic2:double_insulated_gold_cable"],
        "iron": [
            "ic2:iron_cable", "ic2:insulated_iron_cable",
            "ic2:double_insulated_iron_cable", "ic2:triple_insulated_iron_cable",
        ],
        "glass_fibre": ["ic2:glass_fibre_cable"],
        "detector": ["ic2:detector_cable"],
        "splitter": ["ic2:splitter_cable"],
    }
    emit_group(item_dir, "cables", cables)
    emit_group(block_dir, "cables", cables)

    # ---- Raw Materials / Raw Ores / Crushed & Purified Ores ----
    raw_metals = ["tin", "lead", "uranium"]
    emitted_raw_ores, emitted_raw_mats = [], []
    for name in raw_metals:
        item_id = f"ic2:raw_{name}"
        if emit(item_dir, f"raw_ores/{name}", [item_id]):
            emitted_raw_ores.append(f"#{ns}:raw_ores/{name}")
            if emit(item_dir, f"raw_materials/{name}", [item_id, {"id": f"#{ns}:raw_ores/{name}", "required": False}]):
                emitted_raw_mats.append(f"#{ns}:raw_materials/{name}")
    if emitted_raw_mats:
        emit(item_dir, "raw_materials", emitted_raw_mats)
    if emitted_raw_ores:
        emit(item_dir, "raw_ores", emitted_raw_ores)

    processed_metals = ["copper", "gold", "iron", "lead", "silver", "tin", "uranium"]
    emit_group(item_dir, "crushed_raw_materials", {name: [f"ic2:crushed_{name}"] for name in processed_metals})
    emit_group(item_dir, "purified_ores", {name: [f"ic2:purified_{name}"] for name in processed_metals})

    # ---- Ores (Blocks & Items) ----
    ores = {
        "tin": ["ic2:tin_ore", "ic2:deepslate_tin_ore"],
        "lead": ["ic2:lead_ore", "ic2:deepslate_lead_ore"],
        "uranium": ["ic2:uranium_ore", "ic2:deepslate_uranium_ore"],
    }
    stone_ores = [f"ic2:{m}_ore" for m in ores]
    deepslate_ores = [f"ic2:deepslate_{m}_ore" for m in ores]
    emitted_block_ores = emit_group(block_dir, "ores", ores)
    emitted_item_ores = []
    for name, blocks in ores.items():
        if emit(item_dir, f"ores/{name}", blocks):
            emitted_item_ores.append(f"#{ns}:ores/{name}")
    if emit(item_dir, "ores/iridium", ["ic2:iridium_ore"]):
        emitted_item_ores.append(f"#{ns}:ores/iridium")
    if emitted_item_ores:
        emit(item_dir, "ores", emitted_item_ores)
    emit(block_dir, "ores_in_ground/stone", stone_ores)
    emit(item_dir, "ores_in_ground/stone", stone_ores)
    emit(block_dir, "ores_in_ground/deepslate", deepslate_ores)
    emit(item_dir, "ores_in_ground/deepslate", deepslate_ores)
    singular_ores = stone_ores + deepslate_ores
    if emitted_block_ores:
        emit(block_dir, "ore_rates/singular", singular_ores)
        emit(item_dir, "ore_rates/singular", singular_ores)

    # ---- Storage Blocks (Blocks & Items) ----
    storage_blocks = {
        "tin": ["ic2:tin_block"],
        "bronze": ["ic2:bronze_block"],
        "lead": ["ic2:lead_block"],
        "silver": ["ic2:silver_block"],
        "steel": ["ic2:steel_block"],
        "uranium": ["ic2:uranium_block"],
        "raw_tin": ["ic2:raw_tin_block"],
        "raw_lead": ["ic2:raw_lead_block"],
        "raw_uranium": ["ic2:raw_uranium_block"],
    }
    emitted_forge_blk, emitted_forge_itm = [], []
    emitted_sb_blk, emitted_sb_itm = [], []
    for name, blocks in storage_blocks.items():
        if forge:
            if emit(block_dir, f"blocks/{name}", blocks):
                emitted_forge_blk.append(f"#forge:blocks/{name}")
                if emit(block_dir, f"storage_blocks/{name}", blocks + [{"id": f"#forge:blocks/{name}", "required": False}]):
                    emitted_sb_blk.append(f"#forge:storage_blocks/{name}")
            if emit(item_dir, f"blocks/{name}", blocks):
                emitted_forge_itm.append(f"#forge:blocks/{name}")
                if emit(item_dir, f"storage_blocks/{name}", blocks + [{"id": f"#forge:blocks/{name}", "required": False}]):
                    emitted_sb_itm.append(f"#forge:storage_blocks/{name}")
        else:
            if emit(block_dir, f"storage_blocks/{name}", blocks):
                emitted_sb_blk.append(f"#c:storage_blocks/{name}")
            if emit(item_dir, f"storage_blocks/{name}", blocks):
                emitted_sb_itm.append(f"#c:storage_blocks/{name}")
    if forge:
        if emitted_forge_blk:
            emit(block_dir, "blocks", emitted_forge_blk)
        if emitted_forge_itm:
            emit(item_dir, "blocks", emitted_forge_itm)
    if emitted_sb_blk:
        emit(block_dir, "storage_blocks", emitted_sb_blk)
    if emitted_sb_itm:
        emit(item_dir, "storage_blocks", emitted_sb_itm)

    # ---- Misc Materials, Fuels, Crops, Drinks, Foods ----
    if emit(item_dir, "fuels/coal_coke", ["ic2:coke"]):
        emit(item_dir, "fuels", [f"#{ns}:fuels/coal_coke"])
        emit(item_dir, "coal_coke", ["ic2:coke", {"id": f"#{ns}:fuels/coal_coke", "required": False}])
    if emit(item_dir, "rubber", ["ic2:rubber"]):
        emit(item_dir, "rubbers", ["ic2:rubber", {"id": f"#{ns}:rubber", "required": False}])
    emit_group(item_dir, "gems", {"diamond": ["ic2:industrial_diamond"]})
    emit_group(item_dir, "nuggets", {"iridium": ["ic2:iridium_shard"]})
    emit(item_dir, "slag", ["ic2:slag"])
    emit(item_dir, "fertilizers", ["ic2:fertilizer"])

    emit_group(item_dir, "crops", {
        "hops": ["ic2:hops"],
        "coffee": ["ic2:coffee_beans"],
        "terra_wart": ["ic2:terra_wart"],
        "milk_wart": ["ic2:milk_wart"],
        "oil_berry": ["ic2:oil_berry"],
        "bobs_yer_uncle_ranks_berry": ["ic2:bobs_yer_uncle_ranks_berry"],
    })
    emit_group(item_dir, "drinks", {
        "coffee": ["ic2:coffee_mug", "ic2:cold_coffee_mug", "ic2:dark_coffee_mug"],
    })
    foods_values = ["ic2:filled_tin_can"]
    if emit(item_dir, "foods/berry", ["ic2:bobs_yer_uncle_ranks_berry"]):
        foods_values.append(f"#{ns}:foods/berry")
    emit(item_dir, "foods", foods_values)

    # ---- Tools, Armors & Vanilla Tool / Armor / Enchantable Tags ----
    pickaxes = ["ic2:bronze_pickaxe", "ic2:drill", "ic2:diamond_drill", "ic2:iridium_drill"]
    axes = ["ic2:bronze_axe", "ic2:chainsaw"]
    shovels = ["ic2:bronze_shovel"]
    hoes = ["ic2:bronze_hoe"]
    swords = ["ic2:bronze_sword", "ic2:nano_saber"]
    wrenches = ["ic2:wrench", "ic2:electric_wrench"]
    other_tools = [
        "ic2:mining_laser", "ic2:forge_hammer", "ic2:cutter", "ic2:crowbar",
        "ic2:treetap", "ic2:electric_treetap", "ic2:meter", "ic2:wind_meter",
        "ic2:scanner", "ic2:advanced_scanner", "ic2:cropnalyzer", "ic2:obscurator", "ic2:foam_sprayer",
    ]
    mining_tools = pickaxes + axes + shovels + hoes
    melee_weapons = swords + axes

    head_armor = [
        "ic2:bronze_helmet", "ic2:hazmat_helmet", "ic2:solar_helmet",
        "ic2:night_vision_goggles", "ic2:nano_helmet", "ic2:quantum_helmet",
    ]
    chest_armor = [
        "ic2:bronze_chestplate", "ic2:hazmat_chestplate", "ic2:alloy_chestplate",
        "ic2:nano_chestplate", "ic2:quantum_chestplate",
        "ic2:batpack", "ic2:advanced_batpack", "ic2:energy_pack",
        "ic2:jetpack", "ic2:electric_jetpack", "ic2:cf_pack",
    ]
    leg_armor = [
        "ic2:bronze_leggings", "ic2:hazmat_leggings", "ic2:nano_leggings", "ic2:quantum_leggings",
    ]
    foot_armor = [
        "ic2:bronze_boots", "ic2:rubber_boots", "ic2:static_boots", "ic2:nano_boots", "ic2:quantum_boots",
    ]
    all_armors = head_armor + chest_armor + leg_armor + foot_armor
    all_tools = mining_tools + swords + wrenches + other_tools

    # Vanilla tool & armor tags (all 4 targets)
    emit(item_dir, "pickaxes", pickaxes, namespace="minecraft")
    emit(item_dir, "axes", axes, namespace="minecraft")
    emit(item_dir, "shovels", shovels, namespace="minecraft")
    emit(item_dir, "hoes", hoes, namespace="minecraft")
    emit(item_dir, "swords", swords, namespace="minecraft")
    emit(item_dir, "head_armor", head_armor, namespace="minecraft")
    emit(item_dir, "chest_armor", chest_armor, namespace="minecraft")
    emit(item_dir, "leg_armor", leg_armor, namespace="minecraft")
    emit(item_dir, "foot_armor", foot_armor, namespace="minecraft")

    # Vanilla 1.21+/26.x enchantable tags (never emitted on 1.19.2)
    if not forge:
        durability_items = [
            "ic2:bronze_pickaxe", "ic2:bronze_axe", "ic2:bronze_shovel", "ic2:bronze_hoe", "ic2:bronze_sword",
            "ic2:bronze_helmet", "ic2:bronze_chestplate", "ic2:bronze_leggings", "ic2:bronze_boots",
            "ic2:hazmat_helmet", "ic2:hazmat_chestplate", "ic2:hazmat_leggings", "ic2:rubber_boots",
            "ic2:solar_helmet", "ic2:static_boots", "ic2:alloy_chestplate",
            "ic2:forge_hammer", "ic2:cutter", "ic2:wrench", "ic2:crowbar", "ic2:treetap",
        ]
        emit(item_dir, "enchantable/mining", mining_tools, namespace="minecraft")
        emit(item_dir, "enchantable/mining_loot", mining_tools, namespace="minecraft")
        emit(item_dir, "enchantable/durability", durability_items, namespace="minecraft")
        emit(item_dir, "enchantable/weapon", melee_weapons, namespace="minecraft")
        emit(item_dir, "enchantable/sharp_weapon", melee_weapons, namespace="minecraft")
        emit(item_dir, "enchantable/fire_aspect", swords, namespace="minecraft")
        if modern:
            emit(item_dir, "enchantable/melee_weapon", swords, namespace="minecraft")
            emit(item_dir, "enchantable/sweeping", swords, namespace="minecraft")
        else:
            emit(item_dir, "enchantable/sword", swords, namespace="minecraft")
        emit(item_dir, "enchantable/head_armor", head_armor, namespace="minecraft")
        emit(item_dir, "enchantable/chest_armor", chest_armor, namespace="minecraft")
        emit(item_dir, "enchantable/leg_armor", leg_armor, namespace="minecraft")
        emit(item_dir, "enchantable/foot_armor", foot_armor, namespace="minecraft")
        emit(item_dir, "enchantable/armor", all_armors, namespace="minecraft")
        emit(item_dir, "enchantable/equippable", all_armors, namespace="minecraft")
        emit(item_dir, "enchantable/vanishing", all_tools + all_armors, namespace="minecraft")

    # Common tool & armor tags
    tool_parent_values = []
    if emit(item_dir, "tools/wrench", wrenches):
        emit(item_dir, "wrenches", [f"#{ns}:tools/wrench"])
        tool_parent_values.append(f"#{ns}:tools/wrench")
    if forge:
        for sub, members in (
            ("pickaxes", pickaxes), ("axes", axes), ("shovels", shovels),
            ("hoes", hoes), ("swords", swords),
        ):
            if emit(item_dir, f"tools/{sub}", members):
                tool_parent_values.append(f"#forge:tools/{sub}")
        emit(item_dir, "tools", tool_parent_values + other_tools)
        emit_group(item_dir, "armors", {
            "helmets": head_armor,
            "chestplates": chest_armor,
            "leggings": leg_armor,
            "boots": foot_armor,
        })
    else:
        if emit(item_dir, "tools/mining_tool", mining_tools):
            tool_parent_values.append("#c:tools/mining_tool")
        if emit(item_dir, "tools/melee_weapon", melee_weapons):
            tool_parent_values.append("#c:tools/melee_weapon")
        emit(item_dir, "tools", tool_parent_values + other_tools)
        emit(item_dir, "armors", all_armors)

    # ---- Rubber Wood Common Tags & Boat Tag ----
    emit_group(block_dir, "fences", {"wooden": ["ic2:rubber_fence"]})
    emit_group(item_dir, "fences", {"wooden": ["ic2:rubber_fence"]})
    emit_group(block_dir, "fence_gates", {"wooden": ["ic2:rubber_fence_gate"]})
    emit_group(item_dir, "fence_gates", {"wooden": ["ic2:rubber_fence_gate"]})
    if not forge:
        emit(block_dir, "stripped_logs", ["ic2:stripped_rubber_log"])
        emit(item_dir, "stripped_logs", ["ic2:stripped_rubber_log"])
        emit(block_dir, "stripped_woods", ["ic2:stripped_rubber_wood"])
        emit(item_dir, "stripped_woods", ["ic2:stripped_rubber_wood"])
    emit(item_dir, "boats", ["ic2:rubber_boat"], namespace="minecraft")

    # ---- Relocation Protection (Blocks) ----
    emit(block_dir, "relocation_not_supported", [f"ic2:{b}" for b in _RELOCATION_NOT_SUPPORTED_BLOCKS])

    # ---- IC2 Own Semantic Tags ----
    rubber_logs = ["ic2:rubber_log", "ic2:rubber_wood", "ic2:stripped_rubber_log", "ic2:stripped_rubber_wood"]
    emit(block_dir, "rubber_logs", rubber_logs, namespace="ic2")
    emit(item_dir, "rubber_logs", rubber_logs, namespace="ic2")
    emit(item_dir, "bronze_ingots", [
        "ic2:bronze_ingot", {"id": f"#{ns}:ingots/bronze", "required": False},
    ], namespace="ic2")
    emit(item_dir, "alloys", [
        "ic2:alloy", {"id": f"#{ns}:plates/advanced_alloy", "required": False},
    ], namespace="ic2", allow_empty=True)
    emit(item_dir, "recycler_blacklist", [], namespace="ic2", allow_empty=True)

    # ---- Fluids (including dual-published c:/forge: interop fluids) & Fluid Buckets ----
    emitted_buckets = []
    with (repo / "spec/registry/industrial-fluids.tsv").open(encoding="utf-8") as stream:
        for row in csv.DictReader(stream, delimiter="\t"):
            fluid = row["path"]
            emit(fluid_dir, fluid, [f"ic2:{fluid}", f"ic2:flowing_{fluid}"])
            if fluid in ("steam", "superheated_steam", "biogas", "creosote"):
                other_ns = "c" if forge else "forge"
                emit(fluid_dir, fluid, [f"ic2:{fluid}", f"ic2:flowing_{fluid}"], namespace=other_ns)
            if emit(item_dir, f"buckets/{fluid}", [f"ic2:{fluid}_bucket"]):
                emitted_buckets.append(f"#{ns}:buckets/{fluid}")
    if emitted_buckets:
        emit(item_dir, "buckets", emitted_buckets)
