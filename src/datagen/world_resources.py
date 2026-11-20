"""Generate agriculture, terrain and construction data from registered world families."""
import csv
import json
from pathlib import Path

COLORS = ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
          "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")
CABLES = ("copper", "insulated_copper", "tin", "insulated_tin", "gold", "insulated_gold",
          "double_insulated_gold", "iron", "insulated_iron", "double_insulated_iron",
          "triple_insulated_iron", "glass_fibre", "detector", "splitter")
SIDES = ("down", "up", "north", "south", "west", "east")
HORIZONTAL = ("north", "east", "south", "west")


def generate(repo, out, target, write):
    iridium_chests(target, write)
    forge = target.startswith("forge")
    modern = target.startswith("neoforge-26.")
    loot = "loot_tables" if forge else "loot_table"
    recipe = "recipes" if forge else "recipe"
    blocktags = "blocks" if forge else "block"
    itemtags = "items" if forge else "item"
    loader = "forge" if forge else "neoforge"
    world = json.loads((Path(__file__).resolve().parent / "families/world.json").read_text())
    with (repo / "assets/REGISTRY-ART-CATALOG.csv").open() as f:
        catalog = list(csv.DictReader(f))

    def texture(sprite):
        if sprite.startswith("minecraft:"):
            return sprite
        if not (repo / "assets/ic2/textures" / (sprite + ".png")).is_file():
            raise ValueError(f"Missing world sprite {sprite}")
        return "ic2:" + sprite

    def model(kind, part, value):
        write(f"assets/ic2/models/{kind}/{part}", value)

    def block_model(part, value):
        model("block", part, value)

    def cube(part, sprite, cutout=False):
        data = {"parent": "minecraft:block/cube_all", "textures": {"all": texture(sprite)}}
        if cutout:
            data["render_type"] = "minecraft:cutout"
        block_model(part, data)

    def file_model(name, **params):
        return {"model": f"ic2:block/{name}", **params}

    def variant(name, options=None):
        write("assets/ic2/blockstates/" + name, {"variants": options or {"": file_model(name)}})

    def multipart(name, parts):
        write("assets/ic2/blockstates/" + name, {"multipart": parts})

    def item_model(name, parent=None, sprite=None):
        content = {"parent": "ic2:block/" + (parent or name)}
        if sprite:
            content = {"parent": "minecraft:item/generated", "textures": {"layer0": texture(sprite)}}
        model("item", name, content)
        if modern:
            write("assets/ic2/items/" + name, {"model": {"type": "minecraft:model", "model": f"ic2:item/{name}"}})

    survives = {"condition": "minecraft:survives_explosion"}
    silk_predicate = {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}         if forge else {"predicates": {"minecraft:enchantments":
                   [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}
    silk = {"condition": "minecraft:match_tool", "predicate": silk_predicate}

    def drop(name, item=None, conditions=None, functions=None):
        entry = {"type": "minecraft:item", "name": item or f"ic2:{name}"}
        pools = {"rolls": 1, "conditions": conditions if conditions is not None else [survives], "entries": [entry]}
        if functions:
            entry["functions"] = functions
        write(f"data/ic2/{loot}/blocks/{name}", {"type": "minecraft:block", "pools": [pools]})

    def merge_tag(namespace, kind, name, values):
        path = f"data/{namespace}/tags/{kind}/{name}"
        old = out / (path + ".json")
        if old.exists():
            values = [*json.loads(old.read_text())["values"], *values]
        write(path, {"replace": False, "values": list(dict.fromkeys(values))})

    stick_texture = {
        False: "blocks/crop/stick",
        True: "blocks/crop/stick_upgraded",
    }
    for crossed, sprite in stick_texture.items():
        name = "crop_stick_crossed" if crossed else "crop_stick"
        block_model(name, {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                           "textures": {"cross": texture(sprite)}})
    stick_parts = [{"when": {"crossed": str(crossed).lower()}, "apply": file_model(
        "crop_stick_crossed" if crossed else "crop_stick")} for crossed in (False, True)]
    multipart("crop_stick", stick_parts)
    item_model("crop_stick", sprite="items/crop/crop_stick")
    for row in catalog:
        if row["categories"] != "作物":
            continue
        name = row["registry_id"][4:]
        sprites = [p.split("/textures/")[1][:-4] for p in row["user_redraw_png_paths"].split(" | ")]
        parts = stick_parts.copy()
        for age, sprite in enumerate(sprites):
            stage = name + "_" + str(age)
            block_model(stage, {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                         "textures": {"cross": texture(sprite)}})
            choices = str(age) if age < len(sprites) - 1 else "|".join(str(a) for a in range(age, 8))
            parts.append({"when": {"age": choices}, "apply": file_model(stage)})
        multipart(name, parts)

    regular_foams = {"foam": "blocks/cf/foam", "reinforced_foam": "blocks/cf/reinforced_foam"}
    for name, sprite in regular_foams.items():
        cube(name, sprite)
    variant("foam", {"reinforced=false": file_model("foam"), "reinforced=true": file_model("reinforced_foam")})
    item_model("foam")
    drop("foam")
    for cable in CABLES:
        name = cable + "_foam_cable"
        hardened = name + "_hardened"
        cube(name, "blocks/cf/foam")
        cube(hardened, "blocks/cf/wall_white")
        variant(name, {"hardened=false": file_model(name), "hardened=true": file_model(hardened)})
        drop(name, f"ic2:{cable}_cable")
    for color in COLORS:
        name = color + "_wall"
        cube(name, "blocks/cf/wall_" + color)
        variant(name)
        item_model(name)
        drop(name)

    palette = ("stone", "cobblestone", "deepslate", "stone_bricks", "bricks", "polished_andesite", "end_stone",
               "sandstone", "oak_planks", "spruce_planks", "birch_planks", "jungle_planks", "acacia_planks",
               "dark_oak_planks", "copper_block", "iron_block", "gold_block", "quartz_block", "netherrack",
               "nether_bricks", "obsidian", "water", "lava", "basalt", "rubber_planks", "reinforced_stone")
    camouflage = {}
    for name in palette:
        if name in ("water", "lava"):
            model_name = f"obscured_wall_{name}"
            sprite = "minecraft:block/" + name + "_still"
            cube(model_name, sprite)
            item_model(name + "_sheet", sprite=sprite)
            camouflage["material=" + name] = file_model(model_name)
        elif name in ("basalt", "rubber_planks", "reinforced_stone"):
            model_name = f"obscured_wall_{name}"
            sprite = "blocks/resource/rubber/planks" if name == "rubber_planks" else "blocks/resource/" + name
            cube(model_name, sprite)
            camouflage["material=" + name] = file_model(model_name)
        else:
            camouflage["material=" + name] = {"model": "minecraft:block/" + name}
    variant("obscured_wall", camouflage)
    drop("obscured_wall", "ic2:white_wall")

    for metal, vein, count, minimum, maximum in (("lead", 7, 8, -48, 48), ("uranium", 4, 4, -56, 24)):
        targets = []
        for deep, replacement in (("", "stone"), ("deepslate_", "deepslate")):
            name = deep + metal + "_ore"
            cube(name, "blocks/resource/ore/" + name)
            variant(name)
            item_model(name)
            table = {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
                {"type": "minecraft:alternatives", "children": [
                    {"type": "minecraft:item", "name": f"ic2:{name}", "conditions": [silk]},
                    {"type": "minecraft:item", "name": f"ic2:raw_{metal}", "functions": [
                        {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                        {"function": "minecraft:explosion_decay"}]}]}]}]}
            write(f"data/ic2/{loot}/blocks/{name}", table)
            targets.append({"target": {"predicate_type": "minecraft:tag_match",
                            "tag": f"minecraft:{replacement}_ore_replaceables"}, "state": {"Name": f"ic2:{name}"}})
        generate_vein(write, loader, metal + "_ore", vein, count, minimum, maximum, targets)
    generate_vein(write, loader, "basalt", 24, 3, -54, 8, [
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
         "state": {"Name": "ic2:basalt"}}])

    def geom(name, parts, side, end=None, cutout=False):
        art = {"side": texture(side), "end": texture(end or side), "particle": texture(side)}
        data = {"parent": "minecraft:block/block", "textures": art, "elements": [
            {"from": start, "to": endp, "faces": {face: {"texture": "#end" if face in ("up", "down")
             else "#side"} for face in SIDES}} for start, endp in parts]}
        if cutout:
            data["render_type"] = "minecraft:cutout"
        block_model(name, data)

    for path, sprite in (("mining_pipe", "mining_pipe"), ("mining_pipe_tip", "mining_pipe_tip")):
        geom(path, [([5, 0, 5], [11, 16, 11])], "blocks/machine/resource/" + sprite)
        variant(path)
        if path == "mining_pipe":
            item_model(path)
        drop(path, "ic2:mining_pipe")
    for name in ("wooden_scaffold", "reinforced_wooden_scaffold", "iron_scaffold", "reinforced_iron_scaffold"):
        material = "iron" if "iron" in name else "wood"
        prefix = "reinforced_" if name.startswith("reinforced_") else ""
        sides = f"blocks/{prefix}{material}_scaffold_sides"
        end = f"blocks/{material}_scaffold_bottomtop"
        frame = [([0, 13, 0], [16, 16, 16])]
        for x, z in ((0, 0), (0, 13), (13, 0), (13, 13)):
            frame.append(([x, 0, z], [x+3, 13, z+3]))
        geom(name, frame, sides, end, True)
        variant(name)
        item_model(name)
        drop(name)

    wood = "blocks/resource/rubber/planks"
    for name, side, end in (
        ("rubber_wood", "rubber_wood_sides", "rubber_wood_sides"),
        ("stripped_rubber_wood", "stripped_rubber_log", "stripped_rubber_log"),
        ("stripped_rubber_log", "stripped_rubber_log", "stripped_rubber_log_top")):
        for axis, ends in (("x", ("west", "east")), ("y", ("up", "down")), ("z", ("north", "south"))):
            faces = {direction: texture("blocks/resource/" + (end if direction in ends else side)) for direction in SIDES}
            block_model(f"{name}_{axis}", {"parent": "minecraft:block/cube", "textures": {**faces, "particle": faces["north"]}})
        variant(name, {f"axis={axis}": file_model(name + "_" + axis) for axis in "xyz"})
        item_model(name, name + "_y")
        drop(name)

    def vanilla(name, parent, textures, cutout=False):
        data = {"parent": f"minecraft:block/{parent}",
                "textures": {key: texture(sprite) for key, sprite in textures.items()}}
        if cutout:
            data["render_type"] = "minecraft:cutout"
        block_model(name, data)

    name = "rubber_slab"
    slab = {face: wood for face in ("side", "top", "bottom")}
    vanilla(name, "slab", slab)
    vanilla(name + "_top", "slab_top", slab)
    variant(name, {"type=bottom": file_model(name), "type=top": file_model(name + "_top"),
                   "type=double": file_model("rubber_planks")})
    item_model(name)
    drop(name, functions=[{"function": "minecraft:set_count", "count": 2, "conditions": [
        {"condition": "minecraft:block_state_property", "block": "ic2:rubber_slab", "properties": {"type": "double"}}]}])

    name = "rubber_stairs"
    for suffix, parent in (("", "stairs"), ("_outer", "outer_stairs"), ("_inner", "inner_stairs")):
        vanilla(name + suffix, parent, slab)
    stairs = {}
    for direction, angle in (("east", 0), ("south", 90), ("west", 180), ("north", 270)):
        for half in ("top", "bottom"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                base = shape.partition("_")[0]
                adjusted = angle - (90 if shape.endswith("_left") else 0) + (90 if half == "top" and shape != "straight" else 0)
                data = file_model(name if base == "straight" else name + "_" + base, uvlock=True)
                if adjusted % 360:
                    data["y"] = adjusted % 360
                if half == "top":
                    data["x"] = 180
                stairs[f"facing={direction},half={half},shape={shape}"] = data
    variant(name, stairs)
    item_model(name)
    drop(name)

    for name, sprite in (("rubber_fence", wood), ("iron_fence", "blocks/fence_iron")):
        vanilla(name, "fence_post", {"texture": sprite}, True)
        vanilla(name + "_arm", "fence_side", {"texture": sprite}, True)
        vanilla(name + "_inventory", "fence_inventory", {"texture": sprite}, True)
        parts = [{"apply": file_model(name)}] + [
            {"when": {direction: "true"}, "apply": file_model(name + "_arm", y=i*90, uvlock=True)}
            for i, direction in enumerate(HORIZONTAL)]
        multipart(name, parts)
        item_model(name, name + "_inventory")
        drop(name)

    name = "rubber_fence_gate"
    gates = {}
    for wall in (False, True):
        for opened in (False, True):
            suffix = ("_wall" if wall else "") + ("_open" if opened else "")
            vanilla(name + suffix, "template_fence_gate" + suffix, {"texture": wood})
            for i, direction in enumerate(("south", "west", "north", "east")):
                gates[f"facing={direction},in_wall={str(wall).lower()},open={str(opened).lower()}"] = file_model(name + suffix, y=i*90, uvlock=True)
    variant(name, gates)
    item_model(name)
    drop(name)

    name = "rubber_button"
    buttons = {}
    for pressed in (False, True):
        assembly = name + ("_pressed" if pressed else "")
        vanilla(assembly, "button_pressed" if pressed else "button", {"texture": wood})
        for face, x in (("floor", 0), ("wall", 90), ("ceiling", 180)):
            for i, direction in enumerate(HORIZONTAL):
                buttons[f"face={face},facing={direction},powered={str(pressed).lower()}"] = file_model(assembly,
                    x=x, y=(i + (2 if face == "ceiling" else 0))*90 % 360, uvlock=True)
    vanilla(name + "_inventory", "button_inventory", {"texture": wood})
    variant(name, buttons)
    item_model(name, name + "_inventory")
    drop(name)

    if target == "neoforge-26.2":
        for rotation in range(4):
            vanilla("rubber_sign_rot_" + str(rotation), "template_sign_rot_" + str(rotation),
                    {"all": wood, "particle": wood})
        variant("rubber_sign", {f"rotation={r}": file_model("rubber_sign_rot_" + str(r % 4), y=r // 4 * 90)
                                 for r in range(16)})
        vanilla("rubber_wall_sign", "template_wall_sign", {"all": wood, "particle": wood})
        variant("rubber_wall_sign", {f"facing={d}": file_model("rubber_wall_sign", y=(i + 2) % 4 * 90)
                                      for i, d in enumerate(HORIZONTAL)})
    else:
        for sign in ("rubber_sign", "rubber_wall_sign"):
            block_model(sign, {"parent": "minecraft:block/block", "textures": {"particle": texture(wood)}})
            variant(sign)
    item_model("rubber_sign", sprite="items/rubber_sign")
    drop("rubber_sign")
    drop("rubber_wall_sign", "ic2:rubber_sign")

    name = "rubber_pressure_plate"
    vanilla(name, "pressure_plate_up", {"texture": wood})
    vanilla(name + "_down", "pressure_plate_down", {"texture": wood})
    variant(name, {"powered=false": file_model(name), "powered=true": file_model(name + "_down")})
    item_model(name)
    drop(name)

    for name, prefix in (("rubber_door", "blocks/resource/rubber/door_"), ("reinforced_door", "blocks/reinforced_door_")):
        doors = {}
        for top in (False, True):
            for right in (False, True):
                for opened in (False, True):
                    half = "top" if top else "bottom"
                    hinge = "right" if right else "left"
                    suffix = f"_{half}_{hinge}" + ("_open" if opened else "")
                    vanilla(name + suffix, "door" + suffix, {"bottom": prefix + "bottom", "top": prefix + "top"}, True)
                    for i, direction in enumerate(("east", "south", "west", "north")):
                        doors[f"facing={direction},half={'upper' if top else 'lower'},hinge={hinge},open={str(opened).lower()}"] =                             file_model(name + suffix, y=i*90)
        variant(name, doors)
        item_model(name, sprite="items/" + name)
        drop(name, conditions=[survives, {"condition": "minecraft:block_state_property", "block": "ic2:" + name,
                                         "properties": {"half": "lower"}}])

    name = "rubber_trapdoor"
    sprite = "blocks/resource/rubber/trapdoor"
    trapdoors = {}
    for mode in ("bottom", "top", "open"):
        vanilla(name + "_" + mode, "template_orientable_trapdoor_" + mode, {"texture": sprite}, True)
    for half in ("bottom", "top"):
        for i, direction in enumerate(HORIZONTAL):
            trapdoors[f"facing={direction},half={half},open=false"] = file_model(name + "_" + half, y=i*90)
            trapdoors[f"facing={direction},half={half},open=true"] = file_model(name + "_open",
                x=180 if half == "top" else 0, y=(i + (2 if half == "top" else 0))*90 % 360)
    variant(name, trapdoors)
    item_model(name, name + "_bottom")
    drop(name)

    for name, sprite in (("rubber_sheet", "blocks/rubber_sheet"), ("resin_sheet", "blocks/resin_sheet"),
                         ("wool_sheet", "minecraft:block/white_wool")):
        geom(name, [([0, 0, 0], [16, 1, 16])], sprite)
        variant(name)
        item_model(name)
        drop(name)
    cube("reinforced_glass", "blocks/reinforced_glass", True)
    variant("reinforced_glass")
    item_model("reinforced_glass")
    drop("reinforced_glass")

    roots = ["rubber_log", "rubber_wood", "stripped_rubber_log", "stripped_rubber_wood"]
    timber = roots + ["rubber_planks", "rubber_slab", "rubber_stairs", "rubber_fence", "rubber_fence_gate",
                      "rubber_button", "rubber_pressure_plate", "rubber_door", "rubber_trapdoor", "rubber_sign", "rubber_wall_sign"]
    scaffolds = ["wooden_scaffold", "reinforced_wooden_scaffold", "iron_scaffold", "reinforced_iron_scaffold"]
    ore_names = [deep + metal + "_ore" for deep in ("", "deepslate_") for metal in ("tin", "lead", "uranium")]
    pickaxe = ore_names + [p for p in world["blocks"] if p.endswith("_block")] + [
        "basalt", "reinforced_glass", "reinforced_stone", "refractory_bricks", "reinforced_door", "iron_fence",
        "mining_pipe", "mining_pipe_tip", "crop_harvester", "cropmatron", "classic_cropmatron", "miner", "advanced_miner", "terraformer"]
    tag_data = {"mineable/pickaxe": pickaxe, "needs_stone_tool": ["lead_ore", "deepslate_lead_ore"],
                "needs_iron_tool": ["uranium_ore", "deepslate_uranium_ore"], "mineable/axe": timber + scaffolds[:2],
                "climbable": scaffolds, "logs": roots, "logs_that_burn": roots, "planks": ["rubber_planks"],
                "wooden_fences": ["rubber_fence"], "fences": ["rubber_fence"], "fence_gates": ["rubber_fence_gate"],
                "wooden_buttons": ["rubber_button"], "wooden_pressure_plates": ["rubber_pressure_plate"],
                "wooden_slabs": ["rubber_slab"], "wooden_stairs": ["rubber_stairs"], "wooden_doors": ["rubber_door"],
                "wooden_trapdoors": ["rubber_trapdoor"], "doors": ["rubber_door", "reinforced_door"],
                "standing_signs": ["rubber_sign"], "wall_signs": ["rubber_wall_sign"], "signs": ["rubber_sign", "rubber_wall_sign"]}
    for tag, blocks in tag_data.items():
        merge_tag("minecraft", blocktags, tag, [f"ic2:{p}" for p in blocks])
    for tag, blocks in (("logs", roots), ("logs_that_burn", roots), ("planks", ["rubber_planks"])):
        merge_tag("minecraft", itemtags, tag, [f"ic2:{p}" for p in blocks])
    merge_tag("ic2", blocktags, "ores", [f"ic2:{p}" for p in ore_names])
    merge_tag("forge" if forge else "c", blocktags, "ores", [f"ic2:{p}" for p in ore_names])

    def craft(name, pattern, key, ingredients, result, count=1):
        output = {"item" if forge else "id": result, "count": count}
        def ingredient(id):
            return id if modern else ({"tag": id[1:]} if id.startswith("#") else {"item": id})
        data = {"type": "minecraft:crafting_shaped" if pattern else "minecraft:crafting_shapeless", "result": output}
        if pattern:
            data.update({"pattern": pattern, "key": {symbol: ingredient(id) for symbol, id in key.items()}})
        else:
            data["ingredients"] = [ingredient(id) for id in ingredients]
        write(f"data/ic2/{recipe}/world/{name}", data)

    craft("iron_fence", ["PPP", "PPP"], {"P": "ic2:iron_plate"}, [], "ic2:iron_fence", 12)
    craft("foam_sprayer", [" PT", "PCP", " P "], {"P": "ic2:iron_casing", "T": "ic2:treetap",
           "C": "ic2:empty_cell"}, [], "ic2:foam_sprayer")
    for fluid in ("water", "lava"):
        craft(fluid + "_sheet", None, {}, ["ic2:resin_sheet", "minecraft:" + fluid + "_bucket"], "ic2:" + fluid + "_sheet")
    craft("hydration_cell", None, {}, ["ic2:water_cell", "minecraft:clay_ball"], "ic2:hydration_cell")
    craft("classic_cropmatron", ["CBC", "HMH", "SSS"], {"C": "ic2:circuit", "B": "minecraft:chest",
           "H": "ic2:hydration_cell", "M": "ic2:machine", "S": "ic2:crop_stick"}, [], "ic2:classic_cropmatron")
    craft("empty_mug", ["S S", " S "], {"S": "minecraft:stone"}, [], "ic2:empty_mug")
    craft("reinforced_wooden_scaffold", None, {}, ["ic2:wooden_scaffold", "ic2:iron_fence"], "ic2:reinforced_wooden_scaffold")
    craft("reinforced_iron_scaffold", None, {}, ["ic2:iron_scaffold", "ic2:iron_fence"], "ic2:reinforced_iron_scaffold")


def iridium_chests(target, write):
    """Fit independent chest pools to the measured 1.2.4 drops and count ranges."""
    forge = target.startswith("forge-")
    loader = "forge" if forge else "neoforge"
    loot = "loot_tables" if forge else "loot_table"
    # Pool entries contain roll count, approximate chance per roll and observed stack range.
    distributions = {}
    config = Path(__file__).resolve().parents[2] / "spec/registry/chest-supplements.tsv"
    for row in config.read_text(encoding="utf-8").splitlines()[1:]:
        chest, *numbers = row.split("\t")
        def batch(fields):
            rolls, chance, low, high = fields
            return (int(rolls), float(chance), int(low), int(high)) if int(rolls) else None
        distributions[chest] = batch(numbers[:4]), batch(numbers[4:])

    entries = []
    for chest, pools in distributions.items():
        additions = []
        for suffix, parameters in zip(("shard", "ore"), pools):
            if parameters is None:
                continue
            rolls, chance, low, high = parameters
            count = low if low == high else {"type": "minecraft:uniform", "min": low, "max": high}
            additions.append({"name": "iridium_" + suffix, "rolls": rolls, "entries": [{
                "type": "minecraft:item", "name": "ic2:iridium_" + suffix,
                "conditions": [{"condition": "minecraft:random_chance", "chance": chance}],
                "functions": [{"function": "minecraft:set_count", "count": count}],
            }]})
        name = "iridium_" + chest
        entries.append("ic2:" + name)
        write(f"data/ic2/{loot}/chests/{name}", {"type": "minecraft:chest", "pools": additions})
        if forge:
            continue
        write(f"data/ic2/loot_modifiers/{name}", {
            "type": "neoforge:add_table",
            "conditions": [{"condition": loader + ":loot_table_id", "loot_table_id": "minecraft:chests/" + chest}],
            "table": "ic2:chests/" + name,
        })
    # 26.x discovers typed modifiers by registry folder. Only the 1.21.1 loader reads an index.
    if target == "neoforge-1.21.1":
        write("data/neoforge/loot_modifiers/global_loot_modifiers", {"replace": False, "entries": entries})


def generate_vein(write, loader, name, size, count, minimum, maximum, targets):
    write("data/ic2/worldgen/configured_feature/" + name, {"type": "minecraft:ore", "config": {
        "size": size, "discard_chance_on_air_exposure": 0.0, "targets": targets}})
    write("data/ic2/worldgen/placed_feature/" + name, {"feature": "ic2:" + name, "placement": [
        {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"}, {"type": "minecraft:height_range",
         "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": minimum},
                    "max_inclusive": {"absolute": maximum}}}, {"type": "minecraft:biome"}]})
    write(f"data/ic2/{loader}/biome_modifier/add_" + name,
          {"type": loader + ":add_features", "biomes": "#minecraft:is_overworld", "features": "ic2:" + name,
           "step": "underground_ores"})
