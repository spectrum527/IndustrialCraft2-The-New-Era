import json
from pathlib import Path
import tempfile
import unittest
from zipfile import ZipFile

from verify_runtime_jar import verify


class RuntimeJarTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        directory = Path(temporary.name)
        self.roots = [directory / "classes", directory / "resources"]
        for root in self.roots:
            root.mkdir()
        self.jar = directory / "target.jar"
        self.entry = self.roots[0] / "Production.class"
        self.entry.write_bytes(b"one")
        metadata = self.roots[1] / "META-INF/neoforge.mods.toml"
        metadata.parent.mkdir()
        metadata.write_text('modId="ic2"')

    def package(self, extras=None):
        with ZipFile(self.jar, "w") as archive:
            for root in self.roots:
                for file in root.rglob("*"):
                    if file.is_file():
                        archive.write(file, file.relative_to(root))
            archive.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n")
            for name, value in (extras or {}).items():
                archive.writestr(name, value)

    def test_equal_outputs_pass_and_a_rebuilt_candidate_must_still_match_its_record(self):
        self.package({"NOTICE.md": b"Attribution for the release"})
        first = verify(self.jar, self.roots)
        self.assertEqual(first["problems"], [])
        report = self.jar.parent / "first.json"
        report.write_text(json.dumps(first))
        self.entry.write_bytes(b"two")
        self.assertIn("Different bytes: Production.class", verify(self.jar, self.roots)["problems"])
        self.package()
        self.assertTrue(verify(self.jar, self.roots, report)["problems"])

    def test_unknown_packaged_entries_and_missing_development_entries_fail(self):
        self.package({"stale.class": b"old"})
        (self.roots[0] / "Current.class").write_bytes(b"current")
        problems = verify(self.jar, self.roots)["problems"]
        self.assertIn("Unexpected production JAR entry: stale.class", problems)
        self.assertIn("Missing from production JAR: Current.class", problems)

    def test_test_content_in_both_outputs_cannot_pass_by_matching_itself(self):
        name = "com/industrialage/ia2/platform/NeoForgeEventContractTest$1.class"
        helper = self.roots[0] / name
        helper.parent.mkdir(parents=True)
        helper.write_bytes(b"test")
        self.entry.write_bytes(b"ic2:qa_platform_pillar")
        (self.roots[1] / "machine.json").write_bytes(b'{"block":"ic2:qa_fuel_generator"}')
        self.package()
        problems = verify(self.jar, self.roots)["problems"]
        self.assertIn("Test-only content in production JAR: " + name, problems)
        self.assertIn("Test-only content in production JAR: Production.class", problems)
        self.assertIn("Test-only content in production JAR: machine.json", problems)
