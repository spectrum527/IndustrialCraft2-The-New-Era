"""Validate four-target common tags, vanilla symmetry, blasting recipes, and datagen invariants."""

import collections
import csv
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GENERATOR = ROOT / "src/datagen/generate_resources.py"
TARGETS = ("forge-1.19.2", "neoforge-1.21.1", "neoforge-26.1.2", "neoforge-26.2")
AUX_ITEM_MODELS = frozenset({
    "re_battery_1", "re_battery_2", "re_battery_3", "re_battery_4", "nano_saber_active",
    "quantum_helmet_charged", "quantum_chestplate_charged", "quantum_leggings_charged", "quantum_boots_charged",
})
DUAL_INTEROP_FLUIDS = frozenset({"steam", "superheated_steam", "biogas", "creosote"})
REDUNDANT_DEEPSLATE_ORES = frozenset({
    "ic2:deepslate_tin_ore", "ic2:deepslate_lead_ore", "ic2:deepslate_uranium_ore",
})


def _generate_pack(target, output, logistics=True):
    command = [sys.executable, str(GENERATOR), "--target", target, "--output", str(output)]
    if logistics:
        command.append("--logistics")
    subprocess.run(command, check=True, capture_output=True, text=True)


def _ingredient_value(entry):
    if isinstance(entry, str):
        return entry
    if "tag" in entry:
        return "#" + entry["tag"]
    return entry["item"]


class CommonTagsAndRecipesTest(unittest.TestCase):
    def test_four_targets_logistics_and_no_logistics_invariants(self):
        with (ROOT / "spec/registry/industrial-fluids.tsv").open(encoding="utf-8") as stream:
            fluids = [row["path"] for row in csv.DictReader(stream, delimiter="\t")]

        for target in TARGETS:
            forge = target.startswith("forge-")
            modern = target.startswith("neoforge-26.")
            ns = "forge" if forge else "c"
            item_dir = "items" if forge else "item"
            block_dir = "blocks" if forge else "block"
            fluid_dir = "fluids" if forge else "fluid"
            recipe_dir = "recipes" if forge else "recipe"
            loot_dir = "loot_tables" if forge else "loot_table"

            for logistics in (True, False):
                with self.subTest(target=target, logistics=logistics), tempfile.TemporaryDirectory() as tmp:
                    out = Path(tmp) / "pack"
                    _generate_pack(target, out, logistics=logistics)

                    items = {p.stem for p in (out / "assets/ic2/models/item").glob("*.json")} - AUX_ITEM_MODELS
                    blocks = {p.stem for p in (out / "assets/ic2/blockstates").glob("*.json")}

                    tag_graph = {}
                    all_tag_keys = set()
                    dangling = []
                    bad_paths = []

                    for file in sorted((out / "data").rglob("*.json")):
                        rel = file.relative_to(out).as_posix()
                        parts = rel.split("/")
                        if parts[2] != "tags":
                            continue
                        t_ns, t_kind = parts[1], parts[3]
                        tag_name = "/".join(parts[4:])[:-5]
                        all_tag_keys.add((t_kind, f"{t_ns}:{tag_name}"))

                        # 1. Singular/plural directory enforcement
                        if forge and t_kind in ("item", "block", "fluid"):
                            bad_paths.append(("singular_on_1192", rel))
                        if not forge and t_kind in ("items", "blocks", "fluids"):
                            bad_paths.append(("plural_on_121+", rel))

                        # 2. Strict namespace isolation (only 4 dual-interop fluids allowed across ns)
                        if forge and t_ns == "c":
                            if t_kind != "fluids" or tag_name not in DUAL_INTEROP_FLUIDS:
                                bad_paths.append(("c_leak_on_1192", rel))
                        if not forge and t_ns == "forge":
                            if t_kind != "fluid" or tag_name not in DUAL_INTEROP_FLUIDS:
                                bad_paths.append(("forge_leak_on_121+", rel))

                        # 3. Anti-arbitrage red line (A37): IC2 must never publish wires/*
                        if tag_name == "wires" or tag_name.startswith("wires/"):
                            bad_paths.append(("forbidden_wires_tag", rel))

                        # 4. Enchantable tags must never exist on Forge 1.19.2; 26.x drops enchantable/sword
                        if forge and tag_name.startswith("enchantable/"):
                            bad_paths.append(("enchantable_on_1192", rel))
                        if modern and tag_name == "enchantable/sword":
                            bad_paths.append(("removed_enchantable_sword_on_26x", rel))

                        values = json.loads(file.read_text(encoding="utf-8"))["values"]
                        node = (t_kind, f"{t_ns}:{tag_name}")
                        tag_graph[node] = []
                        for entry in values:
                            vid = entry["id"] if isinstance(entry, dict) else entry
                            required = entry.get("required", True) if isinstance(entry, dict) else True
                            if vid.startswith("#"):
                                tag_graph[node].append((t_kind, vid[1:], required))
                            elif required and vid.startswith("ic2:"):
                                short = vid[4:]
                                if t_kind == item_dir and short not in items:
                                    dangling.append((rel, vid))
                                elif t_kind == block_dir and short not in blocks:
                                    dangling.append((rel, vid))

                    self.assertEqual(bad_paths, [], f"{target} logistics={logistics}")
                    self.assertEqual(dangling, [], f"{target} logistics={logistics}")

                    # Missing required subtags & cycle detection
                    missing_subtags = [
                        (node, nxt)
                        for node, edges in tag_graph.items()
                        for kind, ref, req in edges
                        if req and not ref.startswith("minecraft:") and (kind, ref) not in all_tag_keys
                        for nxt in [(kind, ref)]
                    ]
                    self.assertEqual(missing_subtags, [], f"{target} logistics={logistics}")

                    state = {}
                    def check_cycle(node):
                        state[node] = 1
                        for kind, ref, _ in tag_graph.get(node, ()):
                            nxt = (kind, ref)
                            self.assertNotEqual(state.get(nxt), 1, f"Tag cycle {node} -> {nxt}")
                            if nxt not in state and nxt in tag_graph:
                                check_cycle(nxt)
                        state[node] = 2
                    for node in tag_graph:
                        if node not in state:
                            check_cycle(node)

                    # Recipe invariants: syntax, 0 duplicates, 0 redundant deepslate smelting, blasting coverage
                    recipes_root = out / "data/ic2" / recipe_dir
                    by_sig = collections.defaultdict(list)
                    blasting_count = 0
                    for file in sorted(recipes_root.rglob("*.json")):
                        rel = file.relative_to(recipes_root).as_posix()
                        recipe = json.loads(file.read_text(encoding="utf-8"))
                        rtype = recipe["type"]
                        ingredients = list(recipe.get("key", {}).values()) + recipe.get("ingredients", [])
                        if "ingredient" in recipe:
                            ingredients.append(recipe["ingredient"])
                        for ing in ingredients:
                            if modern:
                                self.assertIsInstance(ing, str, f"{target}: {rel}")
                            else:
                                self.assertIsInstance(ing, dict, f"{target}: {rel}")
                        if rtype in ("minecraft:smelting", "minecraft:blasting"):
                            self.assertNotIn(_ingredient_value(recipe["ingredient"]), REDUNDANT_DEEPSLATE_ORES, rel)
                        if rtype == "minecraft:blasting":
                            blasting_count += 1
                            self.assertEqual(recipe["cookingtime"], 100, rel)
                        res_v = recipe.get("result")
                        out_id = res_v.get("id", res_v.get("item")) if isinstance(res_v, dict) else res_v
                        cnt = res_v.get("count", 1) if isinstance(res_v, dict) else 1
                        if rtype == "minecraft:crafting_shaped":
                            grid = tuple(
                                "|".join(json.dumps(recipe["key"].get(ch, ""), sort_keys=True) for ch in row)
                                for row in recipe["pattern"]
                            )
                            by_sig[(rtype, out_id, cnt, grid)].append(rel)
                        elif rtype == "minecraft:crafting_shapeless":
                            ings = tuple(sorted(json.dumps(x, sort_keys=True) for x in recipe["ingredients"]))
                            by_sig[(rtype, out_id, cnt, ings)].append(rel)
                        elif rtype in ("minecraft:smelting", "minecraft:blasting"):
                            by_sig[(rtype, out_id, cnt, json.dumps(recipe["ingredient"], sort_keys=True))].append(rel)

                    dups = [files for files in by_sig.values() if len(files) > 1]
                    self.assertEqual(dups, [], f"{target} logistics={logistics}")
                    self.assertEqual(blasting_count, 27 if logistics else 6, f"{target} logistics={logistics}")

                    # Full-build (--logistics) specific contracts
                    if not logistics:
                        continue

                    def read_json(rel_path):
                        path = out / rel_path
                        self.assertTrue(path.is_file(), f"Missing {rel_path} for {target}")
                        return json.loads(path.read_text(encoding="utf-8"))

                    # Shears loot table + crop_harvester recipe
                    leaves_loot = read_json(f"data/ic2/{loot_dir}/blocks/rubber_leaves.json")
                    shears_term = leaves_loot["pools"][0]["entries"][0]["children"][0]["conditions"][0]["terms"][0]
                    expected_pred = {"tag": "forge:shears"} if forge else {"items": "#c:tools/shear"}
                    self.assertEqual(shears_term["predicate"], expected_pred)

                    harvester = read_json(f"data/ic2/{recipe_dir}/industry/shaped/crop_harvester.json")
                    harvester_vals = {_ingredient_value(v) for v in harvester["key"].values()}
                    self.assertIn("#forge:shears" if forge else "#c:tools/shear", harvester_vals)
                    self.assertIn(f"#{ns}:circuits/basic", harvester_vals)

                    # rubber_boots wool, batch_crafter wrench, electric_wrench direct wrench
                    boots = read_json(f"data/ic2/{recipe_dir}/industry/shaped/rubber_boots.json")
                    self.assertIn("#minecraft:wool", {_ingredient_value(v) for v in boots["key"].values()})

                    crafter = read_json(f"data/ic2/{recipe_dir}/industry/shaped/batch_crafter.json")
                    crafter_vals = {_ingredient_value(v) for v in crafter["key"].values()}
                    self.assertIn(f"#{ns}:tools/wrench", crafter_vals)
                    self.assertIn(f"#{ns}:circuits/advanced", crafter_vals)

                    ewrench = read_json(f"data/ic2/{recipe_dir}/industry/shapeless/electric_wrench.json")
                    self.assertIn("ic2:wrench", [_ingredient_value(v) for v in ewrench["ingredients"]])

                    # rubber_logs replaces book/ingredient_280; book/ingredient_79 uses mapped tags
                    self.assertFalse((out / f"data/ic2/tags/{item_dir}/book/ingredient_280.json").exists())
                    rplanks = read_json(f"data/ic2/{recipe_dir}/industry/shapeless/rubber_planks.json")
                    self.assertEqual([_ingredient_value(v) for v in rplanks["ingredients"]], ["#ic2:rubber_logs"])
                    ing79 = read_json(f"data/ic2/tags/{item_dir}/book/ingredient_79.json")["values"]
                    ing79_ids = {v["id"] for v in ing79}
                    self.assertIn(f"#{ns}:storage_blocks/iron", ing79_ids)
                    self.assertIn("#forge:obsidian" if forge else "#c:obsidians", ing79_ids)

                    # Parent tags, block ores subtag aggregation, ore_rates, relocation protection, buckets, circuits, cables
                    for parent in (
                        "ingots", "dusts", "small_dusts", "tiny_dusts", "plates", "dense_plates",
                        "item_casings", "circuits", "batteries", "cables", "raw_materials", "raw_ores",
                        "crushed_raw_materials", "purified_ores", "ores", "storage_blocks",
                        "gems", "nuggets", "fuels", "tools", "armors", "buckets",
                        "fences", "fence_gates", "crops", "drinks", "foods",
                    ):
                        read_json(f"data/{ns}/tags/{item_dir}/{parent}.json")

                    block_ores = read_json(f"data/{ns}/tags/{block_dir}/ores.json")["values"]
                    self.assertEqual(block_ores, [f"#{ns}:ores/tin", f"#{ns}:ores/lead", f"#{ns}:ores/uranium"])
                    read_json(f"data/{ns}/tags/{block_dir}/ore_rates/singular.json")
                    read_json(f"data/{ns}/tags/{item_dir}/ore_rates/singular.json")

                    reloc = set(read_json(f"data/{ns}/tags/{block_dir}/relocation_not_supported.json")["values"])
                    for block_id in (
                        "ic2:nuclear_reactor", "ic2:coke_kiln", "ic2:personal_chest",
                        "ic2:wooden_storage_box", "ic2:macerator", "ic2:batbox", "ic2:mfsu",
                    ):
                        self.assertIn(block_id, reloc)

                    buckets = set(read_json(f"data/{ns}/tags/{item_dir}/buckets.json")["values"])
                    self.assertEqual(len(buckets), len(fluids))
                    for fluid in fluids:
                        self.assertIn(f"#{ns}:buckets/{fluid}", buckets)
                        self.assertEqual(
                            read_json(f"data/{ns}/tags/{item_dir}/buckets/{fluid}.json")["values"],
                            [f"ic2:{fluid}_bucket"],
                        )

                    # Rubber wood block/item symmetry & boats
                    for mc_tag in (
                        "saplings", "leaves", "logs", "logs_that_burn", "planks",
                        "wooden_slabs", "slabs", "wooden_stairs", "stairs",
                        "wooden_fences", "fences", "fence_gates",
                        "wooden_buttons", "buttons", "wooden_pressure_plates",
                        "wooden_doors", "doors", "wooden_trapdoors", "trapdoors", "signs",
                    ):
                        read_json(f"data/minecraft/tags/{block_dir}/{mc_tag}.json")
                        read_json(f"data/minecraft/tags/{item_dir}/{mc_tag}.json")
                    self.assertEqual(
                        read_json(f"data/minecraft/tags/{item_dir}/boats.json")["values"],
                        ["ic2:rubber_boat"],
                    )
                    if not forge:
                        self.assertEqual(
                            read_json(f"data/c/tags/{block_dir}/stripped_logs.json")["values"],
                            ["ic2:stripped_rubber_log"],
                        )
                        self.assertEqual(
                            read_json(f"data/c/tags/{item_dir}/stripped_woods.json")["values"],
                            ["ic2:stripped_rubber_wood"],
                        )
                        # Enchantable checks on 1.21.1 vs 26.x
                        read_json("data/minecraft/tags/item/enchantable/mining.json")
                        read_json("data/minecraft/tags/item/enchantable/armor.json")
                        if modern:
                            read_json("data/minecraft/tags/item/enchantable/melee_weapon.json")
                            read_json("data/minecraft/tags/item/enchantable/sweeping.json")
                        else:
                            read_json("data/minecraft/tags/item/enchantable/sword.json")


if __name__ == "__main__":
    unittest.main()
