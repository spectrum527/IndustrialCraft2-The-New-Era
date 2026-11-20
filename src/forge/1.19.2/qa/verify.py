"""Run independent P2 smoke, cross-JVM world, and physical client checks on Forge 43.5.0."""

import argparse
from hashlib import sha256
import json
from pathlib import Path
import re
import shutil
import subprocess
import time
import uuid
import xml.etree.ElementTree as ET

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[2]


def write_json(path, record):
    path.write_text(json.dumps(record, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def gradle(task, log, *properties):
    args = [str(ROOT / "gradlew"), "--console=plain", ":forge:1.19.2:" + task, *properties]
    print(f"{task}  {log}", flush=True)
    with log.open("w") as stream:
        completed = subprocess.run(args, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT, timeout=480)
    if completed.returncode:
        raise RuntimeError(f"{task} exited {completed.returncode}. See {log}")
    return completed.returncode


def server_setup(directory, port):
    settings = [
        "server-ip=127.0.0.1", f"server-port={port}", "online-mode=false",
        "level-name=world", "level-type=minecraft:flat", "level-seed=7541",
        "generate-structures=false", "spawn-protection=0", "view-distance=2",
        "simulation-distance=2", "max-tick-time=120000", "enable-rcon=false",
    ]
    (directory / "server.properties").write_text("\n".join(settings) + "\n")
    (directory / "eula.txt").write_text("eula=true\n")


def properties(path):
    return dict(line.replace(r"\:", ":").replace(r"\=", "=").split("=", 1) for line in path.read_text().splitlines()
                if line and not line.startswith("#"))


def world(directory, launch=None):
    from machine_evidence import machine_samples, region_generator
    if launch is None:
        from machine_launch import Development
        gradle("prepareQaWorld", directory / "prepare-world.log")
        launch = Development(MODULE / "build/qa-machine/server-launch.json")
    server_setup(directory, 0)
    run = str(uuid.uuid4())
    (directory / "run-id").write_text(run)
    pack = directory / "world/datapacks/ic2-platform-qa"
    pack.mkdir(parents=True)
    write_json(pack / "pack.mcmeta", {"pack": {"pack_format": 10, "description": "External Forge QA tags"}})
    for kind in ("blocks", "items"):
        tags = pack / "data/ic2_qa/tags" / kind
        tags.mkdir(parents=True)
        write_json(tags / f"platform_{kind}.json", {"values": ["ic2:qa_platform_pillar"]})
    report = {}
    for phase in ("write", "read"):
        exit_code = launch.run(phase, directory, directory / f"{phase}-launch.log")
        shutil.copyfile(directory / "logs/latest.log", directory / f"{phase}-server.log")
        if not (directory / "world/level.dat").is_file() or not list((directory / "world/region").glob("*.mca")):
            raise AssertionError("Missing saved world or region files")
        failure = directory / f"{phase}-failure.txt"
        if failure.is_file():
            raise AssertionError(failure.read_text())
        result = properties(directory / f"{phase}.properties")
        checks = result["assertions"].split(",")
        shared = {"factories-deferred", "block-bound-once", "item-bound-once", "external-block-tag",
                  "external-item-tag", "removed-is-air", "retained-axis-x"}
        specific = ({"observed-block-registration", "placed-24", "placed-28", "destroy-action", "removed-provider",
                     "native-forge-capability", "native-invalidation", "native-capability-removed",
                     "forge-attachment", "forge-attachment-invalidated",
                     "chunk-unloaded-before", "chunk-unloaded-after", "unloaded-target", "event-forwarded-once",
                     "side-DOWN", "side-UP", "side-NORTH", "side-SOUTH", "side-WEST", "side-EAST", "no-side",
                     "invalid-capability-inputs-rejected"}
                    if phase == "write" else {"reloaded-world", "reloaded-run", "reloaded-dimension",
                                               "reloaded-removed", "reloaded-retained"})
        specific.update({"generator-item-placement", "generator-lava-retained", "generator-saved-37"}
                        if phase == "write" else {"generator-native-cold-read", "generator-full-idle",
                                                  "generator-max-packet", "generator-removed-port"})
        machine_samples(phase, result)
        if (result["phase"] != phase or result["status"] != "passed" or result["failedChecks"] != "0"
                or int(result["passedChecks"]) != len(checks) or len(set(checks)) != len(checks)
                or not (shared | specific).issubset(checks) or result["run"] != run):
            raise AssertionError(f"Incomplete {phase} assertions {result}")
        lifetime = (directory / f"lifecycle-{phase}.txt").read_text().splitlines()
        config = (directory / f"config-{phase}.txt").read_text().splitlines()
        capabilities = (directory / f"capability-{phase}.txt").read_text().splitlines()
        expected_config = ({"58 generated defaults and snapshot values", "denied-permission-0 0.8/4000",
                            "reloaded 0.25/5000", "rejected-candidate 0.25/5000", "recovered 0.5/6000"}
                           if phase == "write" else {"reopened 0.5/6000"})
        if (lifetime[-1] != "stop" or not expected_config.issubset(config)
                or capabilities != ["before world unload = EAST", "after world unload = absent"]):
            raise AssertionError(f"Incomplete {phase} lifecycle, config, or unload evidence")
        report[phase] = dict(result, assertions=checks, lifecycle=lifetime, config=config,
                             capability=capabilities, processExit=exit_code, artifacts=launch.verify(result),
                             diskGenerator=region_generator(directory, phase))
    if report["write"]["pid"] == report["read"]["pid"]:
        raise AssertionError("World reload did not use a fresh JVM")
    return report


def smoke(directory, launch=None):
    server_setup(directory, 0)
    log = directory / "smoke-launch.log"
    exit_code = (launch.run("smoke", directory, log) if launch
                 else gradle("runQaGameTestServer", log, f"-PqaDir={directory}"))
    cases = ET.parse(directory / "game-test.xml").getroot().findall(".//testcase")
    names = [case.attrib["name"] for case in cases]
    failed = [case.attrib["name"] for case in cases if case.find("failure") is not None
              or case.find("error") is not None or case.find("skipped") is not None]
    text = log.read_text(errors="replace")
    required = re.search(r"All (\d+) required tests passed", text, re.I)
    expected = {"harnesssmoke", "generatornewstate", "generatorfractionalnbt", "generatorcorruptnbt"}
    if not expected.issubset(names) or failed or not required or int(required[1]) < len(expected):
        raise AssertionError(f"Missing executed Forge GameTest smoke or required success count in {log}")
    rejected = (directory / "generator-rejections.txt").read_text().splitlines()
    if len(set(rejected)) != 14 or len(rejected) != 14:
        raise AssertionError(f"Incomplete corrupt native NBT diagnostics: {rejected}")
    binary = directory / "generator-fractional.nbt"
    return {"cases": names, "requiredPassed": int(required[1]), "requiredFailed": 0, "processExit": exit_code,
            "nativeNbt": {"new": {"fuelTicks": 0, "storedEU": 0},
                          "roundTrip": {"fuelTicks": 123, "storedEU": 1200.5, "file": binary.name,
                                        "sha256": sha256(binary.read_bytes()).hexdigest()},
                          "corrupt": {"count": len(rejected), "diagnosedAndPreserved": rejected}}}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("suite", choices=("smoke", "world", "play", "mismatch", "all"))
    parser.add_argument("--server-launch", type=Path, help="Prepared Forge dedicated server/GameTest launch manifest")
    parser.add_argument("--output", type=Path, help="New evidence directory (created once)")
    parser.add_argument("--prepared", action="store_true", help="Reuse the previously exported play launch manifest")
    options = parser.parse_args()
    output = (options.output or MODULE / "build/reports/qa" / str(uuid.uuid4())).resolve()
    output.mkdir(parents=True, exist_ok=False)
    suites = ("smoke", "world", "play", "mismatch") if options.suite == "all" else (options.suite,)
    manifest = MODULE / "build/qa-play/launch.json"
    prepared = options.prepared
    for suite in suites:
        directory = output / suite
        directory.mkdir()
        start = time.monotonic()
        try:
            if suite in ("play", "mismatch"):
                from physical_play import exercise
                if not prepared:
                    gradle("prepareQaPlay", directory / "prepare.log")
                    prepared = True
                launch = json.loads(manifest.read_text())
                if suite == "mismatch":
                    result = {}
                    for side in ("client", "server"):
                        case = directory / (side + "-rejects")
                        case.mkdir()
                        result[side] = exercise(case, launch, rejecting=side)
                        write_json(case / "result.json", result[side])
                else:
                    result = exercise(directory, launch)
            else:
                from machine_launch import Development
                launch = Development(options.server_launch) if options.server_launch else None
                result = (smoke if suite == "smoke" else world)(directory, launch)
        except Exception as failure:
            write_json(directory / "result.json", {"status": "failed", "error": str(failure),
                                                   "seconds": round(time.monotonic() - start, 2)})
            raise
        write_json(directory / "result.json", {"status": "passed", "seconds": round(time.monotonic() - start, 2),
                                               "evidence": result})
        print(f"PASS {suite}  {directory / 'result.json'}", flush=True)


if __name__ == "__main__":
    main()
