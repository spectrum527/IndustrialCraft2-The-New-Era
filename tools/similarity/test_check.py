"""All Java fixtures are authored here; no project/reference inputs are read."""
import hashlib
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
        self.policy_file = self.root / "boilerplate.json"

    def pair(self, left, right):
        (self.candidate / "A.java").write_text(left, encoding="utf-8")
        (self.reference / "B.java").write_text(right, encoding="utf-8")
        with patch.dict(os.environ, {"CI": "false"}):
            return check.run(self.args)

    def policy_args(self, entries):
        self.policy_file.write_text(json.dumps(entries), encoding="utf-8")
        return self.args + ["--boilerplate-policy", str(self.policy_file)]

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

    def test_multiple_references_are_all_checked(self):
        second = self.root / "reference2"
        second.mkdir()
        (self.reference / "New.java").write_text(
            "public class New { void run() { System.exit(0); } }", encoding="utf-8")
        source = "\n".join("int field%d = %d;" % (i, i) for i in range(12))
        (second / "Old.java").write_text(source, encoding="utf-8")
        (self.candidate / "A.java").write_text(source, encoding="utf-8")
        report, code = check.run(self.args + ["--reference", str(second)])
        self.assertEqual(code, 1)
        self.assertEqual(report["scanned"]["reference"], 2)
        self.assertEqual(report["files"][0]["match"], "ref2:Old.java")
        self.assertIn("eight_identical_lines", report["files"][0]["reasons"])

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

    def test_policy_waives_both_score_thresholds_for_matching_relative_path(self):
        source = b"class A { int x = 1; }"
        nested = self.candidate / "nested"
        nested.mkdir()
        (nested / "A.java").write_bytes(source)
        args = self.policy_args({"nested/A.java": hashlib.sha256(source).hexdigest()})
        for right, score, status in [(source, 1.0, "fail"), (b"class B { int y = 2; }", 0.25, "warn")]:
            with self.subTest(raw_status=status):
                (self.reference / "B.java").write_bytes(right)
                raw, _ = check.run(self.args)
                report, code = check.run(args)
                result = report["files"][0]
                self.assertEqual((code, report["status"], result["status"]), (0, "pass", "pass"))
                self.assertEqual((result["score"], result["raw_status"]), (score, status))
                self.assertEqual(result["match"], "B.java")
                self.assertEqual(result["reasons"], raw["files"][0]["reasons"])
                self.assertIs(result["boilerplate_matched"], True)
                self.assertIs(result["boilerplate_applied"], True)

    def test_policy_hashes_raw_bytes_without_changing_newline_normalization(self):
        source = b'class A {\r\nString text = """\r\nfirst\rsecond\r\n""";\r\n}'
        normalized = source.replace(b"\r\n", b"\n").replace(b"\r", b"\n")
        candidate = self.candidate / "A.java"
        candidate.write_bytes(source)
        (self.reference / "B.java").write_bytes(normalized)
        args = self.policy_args({"A.java": hashlib.sha256(source).hexdigest()})
        strict, code = check.run(self.args)
        self.assertEqual((code, strict["files"][0]["score"]), (1, 1.0))
        self.assertNotIn("boilerplate_applied", strict["files"][0])
        report, code = check.run(args)
        self.assertEqual((code, report["files"][0]["score"]), (0, 1.0))
        candidate.write_bytes(normalized)
        report, code = check.run(args)
        result = report["files"][0]
        self.assertEqual((code, result["score"], result["status"]), (1, 1.0, "fail"))
        self.assertIs(result["boilerplate_matched"], False)
        self.assertIs(result["boilerplate_applied"], False)

    def test_unlisted_and_moved_candidates_still_gate_the_report(self):
        source = "class A { int x = 1; }"
        self.pair(source, source)
        args = self.policy_args({"A.java": hashlib.sha256(source.encode()).hexdigest()})
        (self.candidate / "Other.java").write_text(source, encoding="utf-8")
        report, code = check.run(args)
        self.assertEqual((code, report["status"]), (1, "fail"))
        states = {item["candidate"]: item["status"] for item in report["files"]}
        self.assertEqual(states, {"A.java": "pass", "Other.java": "fail"})
        (self.candidate / "A.java").rename(self.candidate / "Moved.java")
        report, code = check.run(args)
        self.assertEqual((code, report["status"]), (1, "fail"))
        self.assertTrue(all(not item["boilerplate_matched"] for item in report["files"]))
        self.assertTrue(all(item["status"] == "fail" for item in report["files"]))

    def test_policy_cannot_waive_eight_lines_in_either_baseline(self):
        shared = "\n".join("// authored line " + str(i) for i in range(8)) + "\n"
        source = "class A { int x = 1; }"
        candidate = (shared + source).encode()
        (self.candidate / "A.java").write_bytes(candidate)
        (self.reference / "B.java").write_text(shared + "enum B { X, Y; }", encoding="utf-8")
        second = self.root / "second"
        second.mkdir()
        (second / "Best.java").write_text(source, encoding="utf-8")
        digest = hashlib.sha256(candidate).hexdigest()
        args = self.policy_args({"A.java": digest}) + ["--reference", str(second)]
        report, code = check.run(args)
        result = report["files"][0]
        self.assertEqual((code, result["status"], result["raw_status"]), (1, "fail", "fail"))
        self.assertEqual((result["score"], result["match"]), (1.0, "ref2:Best.java"))
        self.assertEqual(result["exact_line_matches"], [
            {"reference": "ref1:B.java", "candidate_line": 1, "reference_line": 1}
        ])
        self.assertIs(result["boilerplate_matched"], True)
        self.assertIs(result["boilerplate_applied"], False)

    def test_missing_and_malformed_policies_error(self):
        args = self.args + ["--boilerplate-policy", str(self.policy_file)]
        report, code = check.run(args)
        self.assertEqual((code, report["status"]), (2, "error"))
        digest = "a" * 64
        duplicate = '{"A.java":"%s", "A.java":"%s"}' % (digest, digest)
        invalid_digests = (None, 0, digest[:-1], digest.upper(), "g" * 64)
        malformed = ["{", "[]", duplicate]
        malformed.extend(json.dumps({"A.java": value}) for value in invalid_digests)
        for invalid in malformed:
            with self.subTest(policy=invalid):
                self.policy_file.write_text(invalid, encoding="utf-8")
                report, code = check.run(args)
                self.assertEqual((code, report["status"]), (2, "error"))
                self.assertFalse(report["files"])

    def test_noncanonical_policy_paths_error(self):
        for name in ("", "/A.java", "../A.java", "nested/../A.java", "./A.java", "nested/./A.java",
                     "nested//A.java", "A.java/", "nested\\A.java", "C:/A.java", "C:A.java", "A\0.java"):
            with self.subTest(path=name):
                report, code = check.run(self.policy_args({name: "a" * 64}))
                self.assertEqual((code, report["status"]), (2, "error"))
                self.assertIn("invalid boilerplate policy path", report["errors"][0])

    def test_ci_skips_missing_baselines_only_with_valid_policy(self):
        self.policy_args({"missing.java": "a" * 64})
        args = self.args[:2] + ["--boilerplate-policy", str(self.policy_file)]
        with patch.dict(os.environ, {"CI": "true"}):
            report, code = check.run(args)
            self.assertEqual((code, report["status"]), (3, "skipped"))
            self.assertEqual(report["scanned"]["comparisons"], 0)
            self.policy_file.write_text("{} garbage", encoding="utf-8")
            report, code = check.run(args)
            self.assertEqual((code, report["status"]), (2, "error"))


if __name__ == "__main__":
    unittest.main()
