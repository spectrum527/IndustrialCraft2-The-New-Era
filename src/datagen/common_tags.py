"""Emit standard ecosystem common tags (c: on NeoForge, forge: on Forge 1.19.2) for IC2 items, blocks, and fluids."""

import csv

_DYE_COLORS = (
    "black", "blue", "brown", "cyan", "gray", "green", "light_blue", "light_gray",
    "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow",
)

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
    # Dusts
    "minecraft:redstone": "dusts/redstone",
    "minecraft:glowstone_dust": "dusts/glowstone",
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
    # Ores (keep deepslate_*_ore direct to avoid duplicating separate smelting recipes)
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
    # Gems, Nuggets, Rubber, Coke & Vanilla Commons
    "minecraft:diamond": "gems/diamond",
    "minecraft:emerald": "gems/emerald",
    "minecraft:lapis_lazuli": "gems/lapis",
    "minecraft:quartz": "gems/quartz",
    "ic2:iridium_shard": "nuggets/iridium",
    "ic2:rubber": "rubbers",
    "ic2:coke": "coal_coke",
    "minecraft:stick": "rods/wooden",
    "minecraft:chest": "chests/wooden",
    "minecraft:glass_pane": "glass_panes",
    "minecraft:ender_pearl": "ender_pearls",
    **{f"minecraft:{color}_dye": f"dyes/{color}" for color in _DYE_COLORS},
}

# Vanilla materials whose standard common tag name differs between Forge 1.19.2 and NeoForge 1.21.1+.
_LOADER_DIFF_ITEM_TAGS = {
    "minecraft:glass": ("forge:glass", "c:glass_blocks"),
    "minecraft:obsidian": ("forge:obsidian", "c:obsidians"),
    "minecraft:cobblestone": ("forge:cobblestone", "c:cobblestones"),
    "minecraft:stone": ("forge:stone", "c:stones"),
    "minecraft:sand": ("forge:sand", "c:sands"),
}


def common_tag_for(item_id, forge):
    """Map a single material item ID to its loader-native common tag (#forge:* or #c:*), or keep direct."""
    if not item_id or item_id.startswith("#"):
        return item_id
    if item_id in _LOADER_DIFF_ITEM_TAGS:
        return "#" + _LOADER_DIFF_ITEM_TAGS[item_id][0 if forge else 1]
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


def generate(repo, forge, write):
    ns = "forge" if forge else "c"
    item_dir = "tags/items" if forge else "tags/item"
    block_dir = "tags/blocks" if forge else "tags/block"
    fluid_dir = "tags/fluids" if forge else "tags/fluid"

    def emit(kind_dir, tag_path, values, namespace=None):
        target_ns = namespace or ns
        write(f"data/{target_ns}/{kind_dir}/{tag_path}", {"replace": False, "values": values})

    # ---- Ingots ----
    ingots = {
        "tin": ["ic2:tin_ingot"],
        "bronze": ["ic2:bronze_ingot"],
        "steel": ["ic2:steel_ingot"],
        "refined_iron": ["ic2:refined_iron_ingot"],
        "lead": ["ic2:lead_ingot"],
        "silver": ["ic2:silver_ingot"],
        "uranium": ["ic2:uranium_ingot"],
        "mixed_metal": ["ic2:mixed_metal_ingot"],
    }
    for name, items in ingots.items():
        emit(item_dir, f"ingots/{name}", items)
    emit(item_dir, "ingots", [f"#{ns}:ingots/{name}" for name in ingots])

    # ---- Dusts ----
    dusts = {
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
    }
    for name, items in dusts.items():
        emit(item_dir, f"dusts/{name}", items)
    emit(item_dir, "dusts", [f"#{ns}:dusts/{name}" for name in dusts])

    # ---- Small / Tiny Dusts ----
    small_dusts = [
        "bronze", "copper", "diamond", "emerald", "gold", "iron",
        "lapis", "lead", "lithium", "obsidian", "silver", "sulfur", "tin",
    ]
    for name in small_dusts:
        item_id = f"ic2:small_{name}_dust"
        emit(item_dir, f"tiny_dusts/{name}", [item_id])
        emit(item_dir, f"small_dusts/{name}", [item_id, {"id": f"#{ns}:tiny_dusts/{name}", "required": False}])
    emit(item_dir, "tiny_dusts", [f"#{ns}:tiny_dusts/{name}" for name in small_dusts])
    emit(item_dir, "small_dusts", [f"#{ns}:small_dusts/{name}" for name in small_dusts])

    # ---- Plates & Dense Plates ----
    plates = {
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
    }
    for name, items in plates.items():
        emit(item_dir, f"plates/{name}", items)
    emit(item_dir, "plates", [f"#{ns}:plates/{name}" for name in plates])

    dense_plates = ["bronze", "copper", "gold", "iron", "lapis", "lead", "obsidian", "steel", "tin"]
    for name in dense_plates:
        emit(item_dir, f"dense_plates/{name}", [f"ic2:dense_{name}_plate"])
    emit(item_dir, "dense_plates", [f"#{ns}:dense_plates/{name}" for name in dense_plates])

    # ---- Raw Materials / Raw Ores ----
    raw_metals = ["tin", "lead", "uranium"]
    for name in raw_metals:
        item_id = f"ic2:raw_{name}"
        emit(item_dir, f"raw_ores/{name}", [item_id])
        emit(item_dir, f"raw_materials/{name}", [item_id, {"id": f"#{ns}:raw_ores/{name}", "required": False}])
    emit(item_dir, "raw_materials", [f"#{ns}:raw_materials/{name}" for name in raw_metals])
    emit(item_dir, "raw_ores", [f"#{ns}:raw_ores/{name}" for name in raw_metals])

    # ---- Ores (Blocks & Items) ----
    ores = {
        "tin": ["ic2:tin_ore", "ic2:deepslate_tin_ore"],
        "lead": ["ic2:lead_ore", "ic2:deepslate_lead_ore"],
        "uranium": ["ic2:uranium_ore", "ic2:deepslate_uranium_ore"],
    }
    stone_ores = [f"ic2:{m}_ore" for m in ores]
    deepslate_ores = [f"ic2:deepslate_{m}_ore" for m in ores]
    for name, blocks in ores.items():
        emit(block_dir, f"ores/{name}", blocks)
        emit(item_dir, f"ores/{name}", blocks)
    emit(item_dir, "ores/iridium", ["ic2:iridium_ore"])
    emit(item_dir, "ores", [f"#{ns}:ores/{name}" for name in ores] + [f"#{ns}:ores/iridium"])
    emit(block_dir, "ores_in_ground/stone", stone_ores)
    emit(item_dir, "ores_in_ground/stone", stone_ores)
    emit(block_dir, "ores_in_ground/deepslate", deepslate_ores)
    emit(item_dir, "ores_in_ground/deepslate", deepslate_ores)

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
    for name, blocks in storage_blocks.items():
        if forge:
            emit(block_dir, f"blocks/{name}", blocks)
            emit(item_dir, f"blocks/{name}", blocks)
            emit(block_dir, f"storage_blocks/{name}", blocks + [{"id": f"#forge:blocks/{name}", "required": False}])
            emit(item_dir, f"storage_blocks/{name}", blocks + [{"id": f"#forge:blocks/{name}", "required": False}])
        else:
            emit(block_dir, f"storage_blocks/{name}", blocks)
            emit(item_dir, f"storage_blocks/{name}", blocks)
    if forge:
        emit(block_dir, "blocks", [f"#forge:blocks/{name}" for name in storage_blocks])
        emit(item_dir, "blocks", [f"#forge:blocks/{name}" for name in storage_blocks])
    emit(block_dir, "storage_blocks", [f"#{ns}:storage_blocks/{name}" for name in storage_blocks])
    emit(item_dir, "storage_blocks", [f"#{ns}:storage_blocks/{name}" for name in storage_blocks])

    # ---- Misc Materials & Tools ----
    emit(item_dir, "fuels/coal_coke", ["ic2:coke"])
    emit(item_dir, "coal_coke", ["ic2:coke", {"id": f"#{ns}:fuels/coal_coke", "required": False}])
    emit(item_dir, "rubber", ["ic2:rubber"])
    emit(item_dir, "rubbers", ["ic2:rubber", {"id": f"#{ns}:rubber", "required": False}])
    emit(item_dir, "tools/wrench", ["ic2:wrench", "ic2:electric_wrench"])
    emit(item_dir, "wrenches", [f"#{ns}:tools/wrench"])
    emit(item_dir, "gems/diamond", ["ic2:industrial_diamond"])
    emit(item_dir, "gems", [f"#{ns}:gems/diamond"])
    emit(item_dir, "nuggets/iridium", ["ic2:iridium_shard"])
    emit(item_dir, "nuggets", [f"#{ns}:nuggets/iridium"])

    # ---- Fluids (including dual-published c:/forge: interop fluids) ----
    with (repo / "spec/registry/industrial-fluids.tsv").open(encoding="utf-8") as stream:
        for row in csv.DictReader(stream, delimiter="\t"):
            fluid = row["path"]
            emit(fluid_dir, fluid, [f"ic2:{fluid}", f"ic2:flowing_{fluid}"])
            if fluid in ("steam", "superheated_steam", "biogas", "creosote"):
                other_ns = "c" if forge else "forge"
                emit(fluid_dir, fluid, [f"ic2:{fluid}", f"ic2:flowing_{fluid}"], namespace=other_ns)
