import json
from pathlib import Path
import unittest


ASSETS = Path(__file__).resolve().parents[2] / "assets/ic2"


class KineticPanelTest(unittest.TestCase):
    def test_available_and_delivered_kinetic_power_have_separate_readouts(self):
        layouts = json.loads((ASSETS / "gui/workshop-layouts.json").read_text())["machines"]
        turbines = [layouts[f"{source}_kinetic_generator"] for source in ("wind", "water")]
        converter = layouts["kinetic_generator"]
        for turbine in turbines:
            self.assertEqual([label["text"] for label in turbine["labels"]], ["kinetic_available", "active"])
            self.assertEqual(turbine["slots"]["0"], [80, 27])
            self.assertFalse(turbine["inventoryTitle"])
        self.assertIn("kinetic_input", [label["text"] for label in converter["labels"]])
        for panel in (*turbines, converter):
            for label in panel["labels"]:
                _, top, _, height = label["rect"]
                self.assertLessEqual(top + height, panel["inventory"][1])
        for language in ("zh_cn", "en_us"):
            translations = json.loads((ASSETS / f"lang/{language}.json").read_text())
            for key in ("kinetic_available", "kinetic_input"):
                self.assertIn("%s KU/t", translations[f"gui.ic2.workshop.{key}"])
