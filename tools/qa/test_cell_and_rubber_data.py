"""Exercise the shipping data generator for each loader's native resource layout."""

import csv
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


class CellAndRubberDataTest(unittest.TestCase):
    def test_shipped_cell_recipes_and_rubber_biomes(self):
        targets = ["forge-1.19.2"] if (ROOT / "src/forge/1.19.2").is_dir() else [
            "neoforge-1.21.1", "neoforge-26.1.2", "neoforge-26.2"
        ]
        with (ROOT / "spec/registry/industrial-fluids.tsv").open(encoding="utf-8") as source:
            fluids = [definition["path"] for definition in csv.DictReader(source, delimiter="\t")]
        for target in targets:
            with self.subTest(target=target), tempfile.TemporaryDirectory() as temp:
                output = Path(temp) / "pack"
                subprocess.run([
                    sys.executable, str(ROOT / "src/datagen/generate_resources.py"),
                    "--target", target, "--output", str(output), "--logistics",
                ], check=True, capture_output=True, text=True)
                data = output / "data/ic2"
                forge = target.startswith("forge-")
                recipes = data / ("recipes" if forge else "recipe")
                field = "item" if forge else "id"
                def read(folder, name):
                    return json.loads((folder / (name + ".json")).read_text(encoding="utf-8"))
                def item(ingredient):
                    return ingredient if isinstance(ingredient, str) else ingredient.get("item")
                basic = read(recipes, "empty_cell")
                self.assertEqual(basic["pattern"], ["TGT"])
                self.assertEqual(item(basic["key"]["T"]), "ic2:tin_casing")
                self.assertEqual(item(basic["key"]["G"]), "minecraft:glass_pane")
                self.assertEqual(basic["result"], {field: "ic2:empty_cell", "count": 4})
                for liquid in ("water", "lava"):
                    for recipe, shell, result in [
                        ("cells/basic_"+liquid, "empty_cell", "basic_"+liquid+"_cell"),
                        ("industry/shapeless/"+liquid+"_cell", "fluid_cell", liquid+"_cell"),
                    ]:
                        derived = read(recipes, recipe)
                        self.assertEqual(derived["cell_use"], "fill")
                        self.assertEqual(derived["result"][field], "ic2:"+result)
                        self.assertCountEqual([item(i) for i in derived["ingredients"]], ["ic2:"+shell, "minecraft:"+liquid+"_bucket"])
                        restored = read(recipes, "cells/recover_"+result)
                        self.assertEqual(restored["cell_use"], "recover")
                        self.assertEqual(restored["result"][field], "ic2:"+shell)
                for fluid in fluids:
                    fill = read(recipes, "cells/fill_"+fluid)
                    self.assertEqual(fill["cell_use"], "fill")
                    self.assertIn("ic2:fluid_cell", [item(i) for i in fill["ingredients"]])
                    if fluid in ("weed_ex", "air", "biogas"):
                        self.assertEqual(fill["result"][field], "ic2:"+fluid+"_cell")
                        recovered = read(recipes, "cells/recover_"+fluid+"_cell")
                        self.assertEqual(recovered["cell_use"], "recover")
                        self.assertEqual(recovered["result"][field], "ic2:fluid_cell")
                    elif target.startswith("neoforge-26"):
                        self.assertEqual(fill["cell_preview_fluid"], "ic2:"+fluid)
                    elif forge:
                        self.assertEqual(fill["result"]["nbt"]["Fluid"]["FluidName"], "ic2:"+fluid)
                    else:
                        self.assertEqual(fill["result"]["components"]["ic2:cell_fluid"]["id"], "ic2:"+fluid)
                self.assertFalse((output / "assets/ic2/models/item/copper_ingot.json").exists())
                self.assertFalse((recipes / "classic/copper_ingot.json").exists())
                self.assertTrue((output / "assets/ic2/models/item/biogas_cell.json").exists())
                for recipe, consumed in [("classic/bio_cell", "empty"), ("world/hydration_cell", "bind_water"), ("classic/heavy_water_bucket", "water"), ("classic/weed_ex_bucket", "weed_ex")]:
                    self.assertEqual(read(recipes, recipe)["cell_use"], consumed)
                tag = read(data, "tags/worldgen/biome/has_rubber_trees")
                self.assertFalse(tag["replace"])
                self.assertEqual(set(tag["values"]), {
                    "#minecraft:is_forest", "#minecraft:is_jungle", "minecraft:plains",
                    "minecraft:sunflower_plains", "minecraft:snowy_plains", "minecraft:ice_spikes",
                })
                modifier = read(data, ("forge" if forge else "neoforge")+"/biome_modifier/add_rubber_tree")
                self.assertEqual(modifier["biomes"], "#ic2:has_rubber_trees")
                self.assertEqual(modifier["features"], "ic2:rubber_tree")
                placed = read(data, "worldgen/placed_feature/rubber_tree")["placement"]
                self.assertIn({"type": "minecraft:rarity_filter", "chance": 6}, placed)
                predicates = next(p["predicate"] for p in placed if p["type"] == "minecraft:block_predicate_filter")
                self.assertEqual(predicates["type"], "minecraft:any_of")
                self.assertEqual(predicates["predicates"][0]["state"]["Name"], "ic2:rubber_sapling")
                ground = predicates["predicates"][1]["predicates"]
                self.assertEqual(ground[0]["blocks"], ["minecraft:snow_block"])
                self.assertEqual(ground[1]["tag"], "minecraft:dirt")
