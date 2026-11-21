import json
from pathlib import Path
import unittest


class AuthoredTankWindowTest(unittest.TestCase):
    def test_vertical_fill_covers_the_glass_inside_the_painted_frame(self):
        resource = Path(__file__).resolve().parents[2] / 'assets/ic2/gui/workshop-layouts.json'
        machines = json.loads(resource.read_text(encoding='utf-8'))['machines']
        # The four redrawn tanks have a 20 x 55 frame with a three-pixel bevel on each side.
        frames = {
            'ore_washing_plant': (60, 20),
            'fluid_bottler': (78, 34),
            'fluid_regulator': (78, 34),
        }
        pump = next(p for p in machines['pump']['draw'] if p['atlas'] == 'common')
        self.assertEqual([20, 55], pump['rect'][2:])
        frames['pump'] = pump['rect'][:2]
        for machine, (left, top) in frames.items():
            with self.subTest(machine=machine):
                inlet, = (m for m in machines[machine]['meters'] if m['value'] == 'inlet')
                self.assertEqual('up', inlet['direction'])
                self.assertEqual([left + 3, top + 3, 14, 49], inlet['rect'])
