"""Exercise A22 using authored, ignored fixtures and real dual-version builds."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[2]
FIXTURE = ROOT / "assets/ia2/build/p0-t4-fixture"
PREFIX = "assets/ia2/build/p0-t4-fixture/"
FILES = ["overlap.txt", "new-only.txt", "external-only.txt"]


def gradle(*arguments):
    subprocess.run([str(ROOT / "gradlew"), *arguments, "--console=plain"], cwd=ROOT, check=True)


def verify(expected):
    jars = sorted(ROOT.glob("src/neoforge/*/build/libs/ia2-neoforge-*.jar"))
    assert len(jars) == 2, jars
    for path in jars:
        with zipfile.ZipFile(path) as jar:
            for name, text in expected.items():
                if text is None:
                    assert PREFIX + name not in jar.namelist(), (path, name)
                else:
                    assert jar.read(PREFIX + name).decode() == text, (path, name)


if FIXTURE.exists():
    raise RuntimeError("Fixture already exists; refusing to overwrite: " + str(FIXTURE))
absent = dict.fromkeys(FILES)
try:
    gradle("build")
    verify(absent)
    with tempfile.TemporaryDirectory(prefix="ia2-p0-assets-") as temporary:
        external = Path(temporary)
        argument = "-Pia2.legacyAssetsDir=" + str(external)
        gradle("build", argument)
        verify(absent)
        old = external / PREFIX
        old.mkdir(parents=True)
        (old / "overlap.txt").write_text("authored external fixture")
        (old / "external-only.txt").write_text("authored external fixture")
        FIXTURE.mkdir(parents=True)
        (FIXTURE / "overlap.txt").write_text("authored new fixture")
        (FIXTURE / "new-only.txt").write_text("authored new fixture")
        gradle("build", argument)
        verify({"overlap.txt": "authored new fixture", "new-only.txt": "authored new fixture",
                "external-only.txt": "authored external fixture"})
finally:
    if FIXTURE.exists():
        shutil.rmtree(FIXTURE)
    parent = FIXTURE.parent
    if parent.exists() and not any(parent.iterdir()):
        parent.rmdir()
    gradle("build")
    verify(absent)
print("A22 verified: absent, empty, external-only, new-only, new wins, no stale fixtures")
