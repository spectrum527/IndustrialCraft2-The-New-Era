"""Run the water turbine acceptance fixture against a packaged mod and a local Forge installation."""

import argparse
from hashlib import sha256
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile


FLAT_WORLDS = {
    "river": ("waterWrite", "waterRead"),
    "ocean": ("waterOcean",),
    "deep_ocean": ("waterDeep",),
    "plains": ("waterPlains",),
}
NATURAL_PHASES = (
    "waterNaturalRiver", "waterNaturalOcean", "waterNaturalDeep",
    "waterCanal", "waterSeabed", "waterFrozenRiver", "waterFrozenOcean",
)


def test_world(args, root, launcher, label, phases, biome=None):
    world = root / label
    world.mkdir()
    (world / "libraries").symlink_to((args.server / "libraries").resolve(), target_is_directory=True)
    (world / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    props = [
        "server-ip=127.0.0.1", "server-port=0", "online-mode=false", "level-seed=20261121",
        "generate-structures=false", "spawn-protection=0", "view-distance=3", "simulation-distance=3",
    ]
    if biome is not None:
        settings = {
            "layers": [{"block": "minecraft:bedrock", "height": 1}, {"block": "minecraft:stone", "height": 114},
                       {"block": "minecraft:water", "height": 12}],
            "biome": "minecraft:" + biome, "structure_overrides": [],
        }
        props.extend(["level-type=minecraft:flat", "generator-settings=" + json.dumps(settings)])
    (world / "server.properties").write_text("\n".join(props) + "\n", encoding="utf-8")
    mods = world / "mods"
    mods.mkdir()
    installed = mods / args.jar.name
    shutil.copyfile(args.jar, installed)
    shutil.copyfile(args.fixture, mods / args.fixture.name)
    pids = set()
    for phase in phases:
        log = world / (phase + ".log")
        with log.open("wb") as output:
            subprocess.run(
                [args.java, "-Xmx1400M", "-XX:ActiveProcessorCount=3", "-Dic2.qa.waterPhase=" + phase,
                 "@" + str(launcher), "nogui"],
                cwd=world, stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT,
                check=True, timeout=240,
            )
        report = world / (phase + ".properties")
        fields = dict(line.split("=", 1) for line in report.read_text(encoding="iso-8859-1").splitlines()
                      if "=" in line and not line.startswith("#"))
        if fields.get("status") != "passed" or fields.get("artifact") != args.jar.name:
            raise RuntimeError(f"{phase} failed. See {report} and {log}: {fields.get('problem')}")
        if fields["pid"] in pids or not (world / "world/level.dat").is_file():
            raise RuntimeError(f"{phase} did not run in a new process with a saved world")
        pids.add(fields["pid"])
        print(f"{label}/{phase} passed in {fields['ticks']} ticks", flush=True)
    if installed.read_bytes() != args.jar.read_bytes():
        raise RuntimeError(f"The installed mod in {world} changed during testing")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", default="java")
    for name in ("server", "jar", "fixture", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    arg_file = "win_args.txt" if os.name == "nt" else "unix_args.txt"
    launcher = Path("libraries/net/minecraftforge/forge/1.19.2-43.5.0") / arg_file
    if not (args.server / launcher).is_file():
        parser.error(f"Missing Forge 43.5.0 launch arguments in {args.server}")
    args.output.mkdir(parents=True, exist_ok=True)
    root = Path(tempfile.mkdtemp(prefix="run-", dir=args.output))
    print(f"Forge water turbine results in {root}", flush=True)
    for biome, phases in FLAT_WORLDS.items():
        test_world(args, root, launcher, biome, phases, biome)
    for phase in NATURAL_PHASES:
        test_world(args, root, launcher, phase, (phase,))
    manifest = {"jar": args.jar.name, "sha256": sha256(args.jar.read_bytes()).hexdigest(),
                "phases": [p for phases in FLAT_WORLDS.values() for p in phases] + list(NATURAL_PHASES)}
    (root / "result.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()

