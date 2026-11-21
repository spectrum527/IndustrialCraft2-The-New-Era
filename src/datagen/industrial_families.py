"""Emit resources for registered industrial families using the artist's face and icon inventory."""
import csv
import json
from pathlib import Path


def generate(repo, out, target, write):
    source = Path(__file__).resolve().parent
    forge = target.startswith("forge-")
    modern = target.startswith("neoforge-26.")
    assets = repo / "assets/ic2/textures"
    with (repo / "assets/REGISTRY-ART-CATALOG.csv").open(encoding="utf-8") as stream:
        art = {row["registry_id"].split(":")[1]: row for row in csv.DictReader(stream)}
    with (source / "machine-surfaces.tsv").open(encoding="utf-8") as stream:
        textures = {(row.pop("id"), row.pop("state")): row for row in csv.DictReader(stream, delimiter="\t")}
    blocks, items = {}, {}
    for file in sorted((source / "families").glob("*.json")):
        family = json.loads(file.read_text())
        if "targets" in family and target not in family["targets"]:
            continue
        blocks.update(family.get("blocks", {}))
        items.update(family.get("items", {}))
    def checked(path):
        if not (assets / (path + ".png")).is_file():
            raise ValueError(f"Missing registered item or face sprite: {path}")
        return "ic2:" + path

    def model(name, data):
        write("assets/ic2/models/" + name, data)

    def item_definition(name):
        if modern:
            write("assets/ic2/items/" + name, {"model": {"type": "minecraft:model", "model": "ic2:item/" + name}})

    for name, entry in items.items():
        if entry.get("customModel"):
            continue
        sprite = entry.get("texture")
        if not sprite:
            choices = art[name]["user_redraw_png_paths"].split(" | ")
            icons = [p.split("/textures/")[1][:-4] for p in choices if "/textures/items/" in p]
            sprite = next((p for p in icons if p.rsplit("/", 1)[-1] in (name, name + "_0")), None)
            if not sprite and icons:
                sprite = icons[0]
        if not sprite:
            raise ValueError(f"Choose an artwork path for {name}")
        parent = "minecraft:item/handheld" if entry.get("handheld") else "minecraft:item/generated"
        item_model = {"parent": parent, "textures": {"layer0": checked(sprite)}}
        if name in ("quantum_helmet", "quantum_chestplate", "quantum_leggings", "quantum_boots"):
            charged = "ic2:item/" + name + "_charged"
            model("item/" + name + "_charged", {"parent": parent, "textures": {
                "layer0": checked(sprite), "layer1": checked(sprite + "_overlay")}})
            if modern:
                write("assets/ic2/items/" + name, {"model": {
                    "type": "minecraft:condition", "property": "ic2:quantum_charged",
                    "on_true": {"type": "minecraft:model", "model": charged},
                    "on_false": {"type": "minecraft:model", "model": "ic2:item/" + name}}})
            else:
                item_model["overrides"] = [{"predicate": {"ic2:quantum_charged": 1}, "model": charged}]
        elif name == "nano_saber":
            active = "ic2:item/nano_saber_active"
            model("item/nano_saber_active", {"parent": parent, "textures": {
                "layer0": checked("items/tool/electric/nano_saber/active_1")}})
            if modern:
                write("assets/ic2/items/nano_saber", {"model": {
                    "type": "minecraft:condition", "property": "minecraft:has_component",
                    "component": "ic2:saber_active",
                    "on_true": {"type": "minecraft:model", "model": active},
                    "on_false": {"type": "minecraft:model", "model": "ic2:item/nano_saber"}}})
            else:
                item_model["overrides"] = [{"predicate": {"ic2:saber_active": 1}, "model": active}]
        else:
            item_definition(name)
        model("item/" + name, item_model)

    rot = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
           "up": {"x": 270}, "down": {"x": 90}}
    if "jetpack_attachment_plate" in items:
        path = "recipes" if forge else "recipe"
        write("data/ic2/" + path + "/jetpack_attachment",
              {"type": "ic2:jetpack_attachment", "category": "equipment"})
    if "scrap_box" in items:
        path = "loot_tables" if forge else "loot_table"
        table = json.loads((source / "scrap-box-loot.json").read_text())
        write("data/ic2/" + path + "/gameplay/scrap_box", table)
    for name, entry in blocks.items():
        if entry.get("customModel"):
            continue
        normal = entry.get("faces") or textures.get((name, "off"))
        if not normal and entry.get("texture"):
            normal = dict.fromkeys(("down","up","north","south","west","east"), entry["texture"])
        if not normal:
            raise ValueError(f"Missing authored block faces for {name}")
        lit = dict(normal)
        lit.update(entry.get("activeFaces") or textures.get((name, "on"), {}))
        variants = {}
        facing = entry.get("facing", "none")
        directions = {key: val for key, val in rot.items() if facing in ("six", "all") or key not in ("up","down")}
        for active in (False, True) if entry.get("active") else (False,):
            model_name = name + ("_active" if active else "")
            faces = {side: checked(path) for side, path in (lit if active else normal).items()}
            faces["particle"] = faces["north"]
            model("block/" + model_name, {"parent":"minecraft:block/cube", "textures":faces})
            for side, rotation in (directions.items() if facing != "none" else [("", {})]):
                props = (["facing=" + side] if side else []) + (["active=" + str(active).lower()] if entry.get("active") else [])
                variants[",".join(props)] = {"model": "ic2:block/" + model_name, **rotation}
        write("assets/ic2/blockstates/" + name, {"variants": variants})
        if entry.get("worldBlockOnly"):
            continue
        model("item/" + name, {"parent": "ic2:block/" + name})
        item_definition(name)
        path = "loot_tables" if forge else "loot_table"
        drop = entry.get("drop", name)
        loot = {"type":"minecraft:item","name":"ic2:" + drop}
        if entry.get("portable"):
            if forge:
                ops = [{"source":"Items","target":"BlockEntityTag.Items","op":"replace"}]
                if entry.get("ownerLocked"):
                    ops.append({"source":"owner","target":"BlockEntityTag.owner","op":"replace"})
                loot["functions"] = [{"function":"minecraft:copy_nbt","source":"block_entity", "ops":ops}]
            else:
                components = ["minecraft:container"]
                if entry.get("ownerLocked"):
                    components.append("minecraft:custom_data")
                loot["functions"] = [{"function":"minecraft:copy_components","source":"block_entity",
                                      "include":components}]
        write("data/ic2/" + path + "/blocks/" + name, {"type":"minecraft:block",
              "pools":[{"rolls":1, "conditions":[{"condition":"minecraft:survives_explosion"}],
                        "entries":[loot]}]})

    with (repo / "spec/registry/industrial-fluids.tsv").open() as stream:
        fluids = list(csv.DictReader(stream, delimiter="\t"))
    for fluid in fluids:
        name = fluid["path"]
        still = checked(fluid["still_texture"])
        write("assets/ic2/blockstates/" + name, {"variants":{"":{"model":"ic2:block/" + name}}})
        model("block/" + name, {"parent":"minecraft:block/block", "textures":{"particle":still}})
        bucket = name + "_bucket"
        sprite = "items/bucket/" + name
        if (assets / (sprite + ".png")).is_file():
            layers = {"layer0":checked(sprite)}
        else:
            layers = {"layer0":"minecraft:item/bucket", "layer1":checked("items/bucket_contents/" + name)}
        model("item/" + bucket, {"parent":"minecraft:item/generated","textures":layers})
        item_definition(bucket)

    registered = {file.stem for file in (out / "assets/ic2/models/item").glob("*.json")}
    eligible = set(items) | {name for name, entry in blocks.items() if not entry.get("worldBlockOnly")}
    # Base registrars provide their own models and some recipes. Include observed routes for
    # registered base outputs that have no recipe at all, while preserving existing routes.
    directory = out / "data/ic2" / ("recipes" if forge else "recipe")
    existing_outputs = set()
    for file in directory.rglob("*.json"):
        value = json.loads(file.read_text()).get("result")
        if isinstance(value, dict):
            value = value.get("id", value.get("item"))
        if isinstance(value, str) and value.startswith("ic2:"):
            existing_outputs.add(value.removeprefix("ic2:"))
    eligible.update(registered - existing_outputs)
    if eligible:
        _crafting(repo, registered, eligible, forge, modern, write)


def _crafting(repo, registered, eligible, forge, modern, write):
    """Only publish reference-book recipes whose mandatory slots have game items on this target."""
    reference = repo / "spec/registry"
    book = json.loads((reference / "repair-recipe-book.json").read_text())
    with (reference / "repair-smelting.tsv").open() as stream:
        time = {row["recipe"]:row for row in csv.DictReader(stream, delimiter="\t")}
    root = "recipes" if forge else "recipe"
    tags = "items" if forge else "item"
    for key, (kind, width, height, result, count, indices) in book["recipes"].items():
        if not result.startswith("ic2:") or result[4:] not in eligible:
            continue
        options = [book["ingredientSets"][i] for i in indices]
        if result == "ic2:fluid_cell":
            # The reference aliases both cell recipes to the same pane. A full glass center
            # leaves both manufacturing routes reachable in the same native recipe book.
            options = [["minecraft:glass"] if choices == ["minecraft:glass_pane"] else choices
                       for choices in options]
        named = {}
        symbols = []
        ingredients = []
        valid = True
        for i, choices in zip(indices, options):
            available = [id for id in choices if id.startswith("minecraft:") or id[4:] in registered]
            if choices and not available:
                valid = False
                break
            if not available:
                symbols.append(" ")
                continue
            if len(available) > 1:
                tag = "book/ingredient_" + str(i)
                write("data/ic2/tags/" + tags + "/" + tag, {"replace":False,
                      "values":[{"id":id,"required":False} for id in available]})
                ingredient = "#ic2:" + tag if modern else {"tag":"ic2:" + tag}
            else:
                id = available[0]
                if not forge:
                    id = {"minecraft:grass":"minecraft:short_grass", "minecraft:scute":"minecraft:turtle_scute"}.get(id,id)
                ingredient = id if modern else {"item":id}
            if i not in named:
                named[i] = (chr(ord("A") + len(named)), ingredient)
            symbols.append(named[i][0])
            ingredients.append(ingredient)
        if not valid:
            continue
        result_value = {"item" if forge else "id":result, "count":count}
        if kind == "smelting":
            if key not in time:
                continue
            numerics = time[key]
            data = {"type":"minecraft:smelting", "ingredient":ingredients[0],
                    "result":result if forge else result_value, "experience":float(numerics["xp"]),
                    "cookingtime":int(numerics["duration_ticks"])}
        elif width and height:
            # The reference recipe book includes all nine cells for some narrower patterns.
            stride = 3 if len(symbols) == 9 else width
            occupied = [i for i, symbol in enumerate(symbols) if symbol != " "]
            if not occupied:
                continue
            left = min(i % stride for i in occupied)
            right = max(i % stride for i in occupied)
            top = min(i // stride for i in occupied)
            bottom = max(i // stride for i in occupied)
            pattern = ["".join(symbols[row * stride + left:row * stride + right + 1])
                       for row in range(top, bottom + 1)]
            data = {"type":"minecraft:crafting_shaped", "key":dict(named.values()),
                    "pattern":pattern,
                    "result":result_value}
        else:
            data = {"type":"minecraft:crafting_shapeless", "ingredients":ingredients, "result":result_value}
        write("data/ic2/" + root + "/industry/" + key.split(":")[1], data)
