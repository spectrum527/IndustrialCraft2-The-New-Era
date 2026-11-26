"""Crafting data for basic and universal cells, with native content and remainder checks."""

import csv


WATER_CELLS = ["ic2:basic_water_cell", "ic2:water_cell", "ic2:empty_cell", "ic2:fluid_cell"]
LAVA_CELLS = ["ic2:basic_lava_cell", "ic2:lava_cell", "ic2:empty_cell", "ic2:fluid_cell"]
CHEMICAL_SHELLS = {"ic2:bio_cell", "ic2:hydrated_coal_cell", "ic2:weed_ex_cell", "ic2:near_depleted_uranium"}
WATER_REAGENTS = {"ic2:coal_fuel_dust", "ic2:cold_coffee_mug", "ic2:hydrated_tin_dust", "ic2:heavy_water_bucket"}
RECOVERABLE = (
    "water_cell", "lava_cell", "air_cell", "weed_ex_cell", "biogas_cell", "electrolyzed_water_cell",
    "hydration_cell", "bio_cell", "hydrated_coal_cell", "biofuel_cell", "coalfuel_cell",
)


def cell_recipe(recipe, modern):
    """Attach the cell rules to the vanilla layouts that use liquid or an empty shell."""
    if recipe.get("type") not in ("minecraft:crafting_shaped", "minecraft:crafting_shapeless"):
        return recipe
    result = recipe["result"]
    output = result.get("id", result.get("item"))
    replacements = {}
    rule = recipe.get("cell_use")
    if output in CHEMICAL_SHELLS:
        replacements["ic2:empty_cell"] = "ic2:fluid_cell"
    if output in ("ic2:water_cell", "ic2:lava_cell") and rule is None:
        replacements["ic2:empty_cell"] = "ic2:fluid_cell"
        rule = "fill"
    if output in WATER_REAGENTS:
        replacements["ic2:water_cell"] = "#ic2:crafting/water_cells"
        rule = "water"
    elif output == "ic2:heatpack":
        replacements["ic2:lava_cell"] = "#ic2:crafting/lava_cells"
        rule = "lava"
    elif output == "ic2:hydration_cell" and rule is None:
        replacements["ic2:water_cell"] = "#ic2:crafting/universal_water_cells"
        rule = "bind_water"
    elif output == "ic2:reactor_coolant_cell":
        rule = "coolant"
    elif output == "ic2:weed_ex_bucket":
        replacements["ic2:weed_ex_cell"] = "#ic2:crafting/weed_ex_cells"
        rule = "weed_ex"

    def ingredient(entry):
        nonlocal rule
        name = entry if isinstance(entry, str) else entry.get("item")
        if name is None:
            return entry
        name = replacements.get(name, name)
        if rule is None and name in ("ic2:empty_cell", "ic2:fluid_cell"):
            rule = "empty"
        if modern:
            return name
        return {"tag": name[1:]} if name.startswith("#") else {"item": name}

    if "key" in recipe:
        recipe["key"] = {symbol: ingredient(value) for symbol, value in recipe["key"].items()}
    else:
        recipe["ingredients"] = [ingredient(value) for value in recipe["ingredients"]]
    if rule is not None:
        recipe["type"] = recipe["type"].replace("minecraft:crafting_", "ic2:cell_")
        recipe["cell_use"] = rule
    return recipe


def expand_liquid_choices(choices):
    """The measured recipe book lists older cell IDs; named basic cells carry the same fluids."""
    for bucket, cells in (("minecraft:water_bucket", WATER_CELLS), ("minecraft:lava_bucket", LAVA_CELLS)):
        if bucket in choices and "ic2:fluid_cell" in choices:
            return list(dict.fromkeys([*choices, *cells]))
    return choices


def generate_cell_recipes(repo, forge, modern, write):
    recipe_dir = "recipes" if forge else "recipe"
    tags = "items" if forge else "item"
    for name, values in {
        "water_cells": WATER_CELLS, "lava_cells": LAVA_CELLS,
        "universal_water_cells": ["ic2:fluid_cell", "ic2:water_cell"],
        "weed_ex_cells": ["ic2:fluid_cell", "ic2:weed_ex_cell"],
    }.items():
        write(f"data/ic2/tags/{tags}/crafting/{name}", {"replace": False, "values": values})

    def craft(name, inputs, result, use, preview_fluid=None):
        ingredients = list(inputs) if modern else [{"item": entry} for entry in inputs]
        recipe = {
            "type": "ic2:cell_shapeless", "cell_use": use, "ingredients": ingredients, "result": result,
        }
        if preview_fluid:
            recipe["cell_preview_fluid"] = preview_fluid
        write(f"data/ic2/{recipe_dir}/cells/{name}", recipe)

    def stack(path):
        return {"item" if forge else "id": "ic2:" + path, "count": 1}

    for liquid in ("water", "lava"):
        craft("basic_" + liquid, ["ic2:empty_cell", "minecraft:" + liquid + "_bucket"],
              stack("basic_" + liquid + "_cell"), "fill")
    with (repo / "spec/registry/industrial-fluids.tsv").open(encoding="utf-8") as source:
        for definition in csv.DictReader(source, delimiter="\t"):
            fluid = definition["path"]
            preview_fluid = None
            if fluid in ("air", "weed_ex", "biogas"):
                sample = stack(fluid + "_cell")
            else:
                sample = stack("fluid_cell")
                if forge:
                    sample["nbt"] = {"Fluid": {"FluidName": "ic2:" + fluid, "Amount": 1000}}
                elif modern:
                    # Modern fluid components become available after recipes have been decoded.
                    preview_fluid = "ic2:" + fluid
                else:
                    sample["components"] = {"ic2:cell_fluid": {"id": "ic2:" + fluid, "amount": 1000}}
            craft("fill_" + fluid, ["ic2:fluid_cell", "ic2:" + fluid + "_bucket"], sample, "fill", preview_fluid)
    for path in ("empty_cell", "basic_water_cell", "basic_lava_cell", "fluid_cell", *RECOVERABLE):
        shell = "empty_cell" if path in ("empty_cell", "basic_water_cell", "basic_lava_cell") else "fluid_cell"
        craft("recover_" + path, ["ic2:" + path], stack(shell), "recover")
