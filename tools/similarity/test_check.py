"""All Java fixtures are authored here; no project/reference inputs are read."""
import json
import os
import re
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

import check


class SimilarityTests(unittest.TestCase):
    def setUp(self):
        self.sandbox = tempfile.TemporaryDirectory(dir=Path(__file__).parent)
        self.addCleanup(self.sandbox.cleanup)
        self.root = Path(self.sandbox.name)
        self.candidate, self.reference = self.root / "candidate", self.root / "reference"
        self.candidate.mkdir()
        self.reference.mkdir()
        self.args = ["--candidate", str(self.candidate), "--reference", str(self.reference)]

    def pair(self, left, right):
        (self.candidate / "A.java").write_text(left, encoding="utf-8")
        (self.reference / "B.java").write_text(right, encoding="utf-8")
        with patch.dict(os.environ, {"CI": "false"}):
            return check.run(self.args)

    def test_independent_and_recursive_best_match(self):
        source = "public class A { int sum(int a, int b) { return a + b; } }"
        report, code = self.pair(source, 'enum Hue { RED, BLUE; String label = "hue"; }')
        self.assertEqual((code, report["status"]), (0, "pass"))
        nested = self.reference / "nested"
        nested.mkdir()
        (nested / "C.java").write_text(source, encoding="utf-8")
        report, code = check.run(self.args)
        self.assertEqual((code, report["files"][0]["match"]), (1, "nested/C.java"))
        self.assertEqual(report["scanned"], {"candidate": 1, "reference": 2, "comparisons": 2})

    def test_renaming_comments_whitespace_and_literals(self):
        source = 'class A { String url = "https://x/*literal*/"; int n = 2; }'
        changed = 'class B /* note */ {\nString text="https://x/*literal*/"; // note\nint x=2;}'
        report, code = self.pair(source, changed)
        self.assertEqual((code, report["files"][0]["score"]), (1, 1.0))
        self.assertIn('"https://x/*literal*/"', check.tokenize(source))
        self.assertEqual(check.tokenize('"""// literal\n/* text */"""'), ['"""// literal\n/* text */"""'])
        self.assertEqual(check.tokenize("'/' /* removed */ '\\''"), ["'/'", "'\\''"])

    def test_strict_thresholds(self):
        for shared, total, status in [(3, 20, "pass"), (4, 25, "warn"), (1, 4, "warn"), (13, 50, "fail")]:
            grams = [(str(i),) * 5 for i in range(total)]
            candidate = {"path": "A.java", "grams": set(grams[:shared]), "lines": {}}
            reference = {"path": "B.java", "grams": set(grams), "lines": {}}
            result = check.compare(candidate, [reference])
            self.assertEqual((result["score"], result["status"]), (shared / total, status))

    def test_schema_example_and_local_references(self):
        root = Path(__file__).resolve().parents[2]
        schema = json.loads((root / "parity/schema/scenario.schema.json").read_text())
        sample = json.loads(re.search(r"```json\n(.*?)\n```", (root / "parity/README.md").read_text(), re.S)[1])
        pending = [schema]
        while pending:
            node = pending.pop()
            if isinstance(node, dict):
                if "$ref" in node:
                    self.assertIn(node["$ref"].removeprefix("#/$defs/"), schema["$defs"])
                pending.extend(node.values())
            elif isinstance(node, list):
                pending.extend(node)

    def test_eight_raw_lines_checked_against_every_reference(self):
        shared = "\n".join("// authored line " + str(i) for i in range(8)) + "\n"
        source = "class A { int x = 1; }"
        self.pair(shared + source, shared + "enum B { X, Y; }")
        (self.reference / "Best.java").write_text(source, encoding="utf-8")
        report, code = check.run(self.args)
        self.assertEqual((code, report["files"][0]["match"]), (1, "Best.java"))
        self.assertIn("eight_identical_lines", report["files"][0]["reasons"])
        self.assertEqual(report["files"][0]["exact_line_matches"][0]["candidate_line"], 1)

    def test_short_empty_and_empty_directory_are_errors(self):
        for source in ("", "// comment", "a b c d"):
            report, code = self.pair(source, "class B { int y = 2; }")
            self.assertEqual((code, report["status"]), (2, "error"))
            self.assertIn("insufficient tokens", report["errors"][0])
        self.assertEqual(self.pair("class B { int y = 2; }", "")[1], 2)
        (self.reference / "B.java").unlink()
        self.assertEqual(check.run(self.args)[1], 2)

    def test_arguments_ci_skip_and_json_cli(self):
        self.pair("class A { int x = 1; }", "class B { int y = 2; }")
        with patch.dict(os.environ, {"CI": "true"}):
            report, code = check.run(self.args[:2])
            self.assertEqual((code, report["status"]), (3, "skipped"))
            self.assertTrue(report["warnings"])
            (self.candidate / "A.java").unlink()
            self.assertEqual(check.run(self.args[:2])[1], 3)
            self.assertEqual(check.run(self.args[:2] + ["--reference", str(self.root / "missing")])[1], 2)
        with patch.dict(os.environ, {"CI": "false"}):
            for args in ([], self.args[:2], self.args + ["--typo"], ["--candidate", ""]):
                self.assertEqual(check.run(args)[1], 2)
        command = [sys.executable, str(Path(check.__file__)), "--candidate", str(self.root / "missing")]
        completed = subprocess.run(command, capture_output=True, text=True, check=False)
        self.assertEqual(completed.returncode, 2)
        self.assertEqual(json.loads(completed.stdout)["status"], "error")


if __name__ == "__main__":
    unittest.main()
