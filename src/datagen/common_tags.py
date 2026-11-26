"""Emit standard ecosystem common tags (c: on NeoForge, forge: on Forge 1.19.2) for IC2 items, blocks, and fluids."""

import csv


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
        item_id = [f"ic2:small_{name}_dust"]
        emit(item_dir, f"tiny_dusts/{name}", item_id)
        emit(item_dir, f"small_dusts/{name}", item_id)
    emit(item_dir, "tiny_dusts", [f"#{ns}:tiny_dusts/{name}" for name in small_dusts])

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

    # ---- Raw Materials / Raw Ores ----
    raw_metals = ["tin", "lead", "uranium"]
    for name in raw_metals:
        item_id = [f"ic2:raw_{name}"]
        emit(item_dir, f"raw_materials/{name}", item_id)
        emit(item_dir, f"raw_ores/{name}", item_id)
    emit(item_dir, "raw_materials", [f"#{ns}:raw_materials/{name}" for name in raw_metals])
    if forge:
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
    emit(item_dir, "ores", [f"#{ns}:ores/{name}" for name in ores])
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
        emit(block_dir, f"storage_blocks/{name}", blocks)
        emit(item_dir, f"storage_blocks/{name}", blocks)
        if forge:
            emit(block_dir, f"blocks/{name}", blocks)
            emit(item_dir, f"blocks/{name}", blocks)
    emit(block_dir, "storage_blocks", [f"#{ns}:storage_blocks/{name}" for name in storage_blocks])
    emit(item_dir, "storage_blocks", [f"#{ns}:storage_blocks/{name}" for name in storage_blocks])

    # ---- Misc Materials & Tools ----
    emit(item_dir, "coal_coke", ["ic2:coke"])
    emit(item_dir, "fuels/coal_coke", ["ic2:coke"])
    emit(item_dir, "rubbers", ["ic2:rubber"])
    emit(item_dir, "rubber", ["ic2:rubber"])
    emit(item_dir, "tools/wrench", ["ic2:wrench", "ic2:electric_wrench"])
    emit(item_dir, "gems/diamond", ["ic2:industrial_diamond"])
    emit(item_dir, "ores/iridium", ["ic2:iridium_ore"])
    emit(item_dir, "nuggets/iridium", ["ic2:iridium_shard"])

    # ---- Fluids (including c:steam / forge:steam and c:biogas / forge:biogas) ----
    with (repo / "spec/registry/industrial-fluids.tsv").open(encoding="utf-8") as stream:
        for row in csv.DictReader(stream, delimiter="\t"):
            fluid = row["path"]
            emit(fluid_dir, fluid, [f"ic2:{fluid}", f"ic2:flowing_{fluid}"])
            # Also publish steam under both c: and forge: on NeoForge/Forge for maximum cross-mod interop.
            if fluid in ("steam", "superheated_steam", "biogas"):
                other_ns = "c" if forge else "forge"
                emit(fluid_dir, fluid, [f"ic2:{fluid}", f"ic2:flowing_{fluid}"], namespace=other_ns)
