from pathlib import Path
import tempfile
import unittest

from audit_release_registry import find_production_jar


class ReleaseJarDiscoveryTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        (self.root / 'mods').mkdir()

    def touch(self, name):
        result = self.root / 'mods' / name
        result.touch()
        return result

    def test_old_and_current_versions_are_discovered_without_the_qa_fixture(self):
        for version in ('0.1.0', '1.0.0'):
            for loader, target in (('forge', '1.19.2'), ('neoforge', '26.2')):
                with self.subTest(version=version, loader=loader):
                    production = self.touch(f'ic2-{loader}-{target}-{version}.jar')
                    fixture = self.touch(f'ic2-{loader}-{target}-{version}-qa.jar')
                    sources = self.touch(f'ic2-{loader}-{target}-{version}-sources.jar')
                    javadoc = self.touch(f'ic2-{loader}-{target}-{version}-javadoc.jar')
                    directory = self.root / 'mods' / f'ic2-{loader}-{target}-directory.jar'
                    directory.mkdir()
                    self.assertEqual(production, find_production_jar(self.root))
                    production.unlink()
                    fixture.unlink()
                    sources.unlink()
                    javadoc.unlink()
                    directory.rmdir()

    def test_missing_and_ambiguous_installs_fail_with_diagnostics(self):
        self.touch('another-mod-1.0.0.jar')
        with self.assertRaisesRegex(ValueError, 'found 0'):
            find_production_jar(self.root)
        self.touch('ic2-forge-1.19.2-0.1.0.jar')
        self.touch('ic2-forge-1.19.2-1.0.0.jar')
        with self.assertRaisesRegex(ValueError, 'found 2'):
            find_production_jar(self.root)
