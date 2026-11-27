"""Exercise A22 using authored, ignored fixtures and every current platform build."""
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[2]
FIXTURE = ROOT / "assets/ic2/build/p0-t4-fixture"
PREFIX = "assets/ic2/build/p0-t4-fixture/"
FILES = ["overlap.txt", "new-only.txt", "external-only.txt", "authored.png"]
OLD_MODEL = "assets/ia2/models/block/build/p0-test.json"
PRIVATE_TEXTURES = [
    "blocks/machine/build/p0-test.png", "items/build/p0-test.png",
    "blocks/wiring/cable/build/p0-test.png", "items/build/p0-test.png.mcmeta",
    "gui/build/p0-test.png",
]
TARGETS = sorted(
    (loader, path.parent.name)
    for loader in ("forge", "neoforge")
    for path in (ROOT / "src" / loader).glob("*/build.gradle")
)
MOD_VERSION = next(
    line.partition("=")[2].strip()
    for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines()
    if line.partition("=")[0].strip() == "mod_version"
)
if not TARGETS:
    raise RuntimeError("No platform target build files found")

EXTRA_GRADLE_ARGS = sys.argv[1:]


def gradle(*arguments):
    subprocess.run(
        [str(ROOT / "gradlew"), *arguments, *EXTRA_GRADLE_ARGS, "--console=plain"],
        cwd=ROOT,
        check=True,
    )


def verify(expected):
    for loader, target in TARGETS:
        path = (ROOT / "src" / loader / target / "build/libs"
                / f"ic2-{loader}-{target}-{MOD_VERSION}.jar")
        assert path.is_file(), f"Missing JAR for {loader}-{target}: {path}"
        with zipfile.ZipFile(path) as jar:
            names = set(jar.namelist())
            assert OLD_MODEL not in names, path
            for resource in PRIVATE_TEXTURES:
                assert "assets/ic2/textures/" + resource not in names, (path, resource)
            for name, text in expected.items():
                if text is None:
                    assert PREFIX + name not in names, (path, name)
                else:
                    assert jar.read(PREFIX + name).decode() == text, (path, name)


if FIXTURE.exists():
    raise RuntimeError("Fixture already exists; refusing to overwrite: " + str(FIXTURE))
absent = dict.fromkeys(FILES)
try:
    gradle("build")
    verify(absent)
    with tempfile.TemporaryDirectory(prefix="ic2-p0-assets-") as temporary:
        external = Path(temporary)
        argument = "-Pic2.legacyAssetsDir=" + str(external)
        gradle("build", argument)
        verify(absent)
        old_model = external / OLD_MODEL
        old_model.parent.mkdir(parents=True)
        old_model.write_text("{}")
        for resource in PRIVATE_TEXTURES:
            sample = external / "assets/ic2/textures" / resource
            sample.parent.mkdir(parents=True, exist_ok=True)
            sample.write_text("authored private filter fixture")
        old = external / PREFIX
        old.mkdir(parents=True)
        (old / "overlap.txt").write_text("authored external fixture")
        (old / "external-only.txt").write_text("authored external fixture")
        (old / "authored.png").write_text("private sprite")
        FIXTURE.mkdir(parents=True)
        (FIXTURE / "authored.png").write_text("authored sprite")
        (FIXTURE / "overlap.txt").write_text("authored new fixture")
        (FIXTURE / "new-only.txt").write_text("authored new fixture")
        gradle("build", argument)
        verify({"overlap.txt": "authored new fixture", "new-only.txt": "authored new fixture",
                "external-only.txt": "authored external fixture", "authored.png": "authored sprite"})
finally:
    if FIXTURE.exists():
        shutil.rmtree(FIXTURE)
    parent = FIXTURE.parent
    if parent.exists() and not any(parent.iterdir()):
        parent.rmdir()
    gradle("build")
    verify(absent)
print(
    f"A22 verified across {len(TARGETS)} targets "
    f"({', '.join(f'{l}-{t}' for l, t in TARGETS)}): "
    "absent, empty, external-only, new-only, new wins, private PNG/animation excluded, no stale fixtures"
)
