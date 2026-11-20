"""Run all standalone Forge release-server acceptance profiles against one immutable JAR."""

import argparse
from concurrent.futures import ThreadPoolExecutor
import json
from pathlib import Path
import shutil
import subprocess
import sys
import time

from packaged_server import digest


PROFILES = {
    "cold-steel": ("packaged_workshop.py", "--directory", ()),
    "steam": ("packaged_workshop.py", "--directory", ("--steam",)),
    "advanced-workshop": ("packaged_workshop.py", "--directory", ("--advanced",)),
    "thermal-kinetic": ("packaged_workshop.py", "--directory", ("--thermal",)),
    "ore-solar-compressor": ("packaged_workshop.py", "--directory", ("--metal-lines",)),
    "starter-logistics-restart": ("packaged_playable.py", "--directory", ()),
    "cross-chunk-bottler": ("packaged_workshop.py", "--directory", ("--automation",)),
    "matter-generation": ("packaged_workshop.py", "--directory", ("--matter",)),
    "native-power-restart": ("packaged_power.py", "--instance", ()),
    "server-startup-restart": ("packaged_server.py", "--directory", ()),
}


def run_profile(root, name, common, expected):
    runner, directory_flag, flags = PROFILES[name]
    artifacts = root / name
    log_path = root / (name + ".log")
    command = [sys.executable, "-B", str(Path(__file__).with_name(runner)), *common,
               directory_flag, str(artifacts), *flags]
    start = time.monotonic()
    try:
        with log_path.open("w", encoding="utf-8") as log:
            completed = subprocess.run(command, stdout=log, stderr=subprocess.STDOUT, timeout=660)
        if completed.returncode != 0:
            raise RuntimeError(f"Exited with code {completed.returncode}")
        report = json.loads((artifacts / "result.json").read_text())
        installed = next((artifacts / "mods").glob("*.jar"))
        if report["sha256"] != expected or digest(installed) != expected:
            raise ValueError("The installed release JAR digest did not match the run snapshot")
        result = {"status": "passed", "report": str(artifacts / "result.json")}
    except (OSError, subprocess.SubprocessError, RuntimeError, ValueError, KeyError, StopIteration) as error:
        result = {"status": "failed", "problem": str(error), "log": str(log_path)}
    result["seconds"] = round(time.monotonic() - start, 2)
    print(result["status"].upper(), name, flush=True)
    return name, result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ("jar", "libraries", "eula", "directory"):
        parser.add_argument("--" + key, type=Path, required=True)
    parser.add_argument("--java", default="java")
    parser.add_argument("--jobs", type=int, choices=range(1, 5), default=3)
    args = parser.parse_args()
    root = args.directory.resolve()
    root.mkdir(parents=True, exist_ok=False)
    snapshot = root / args.jar.name
    shutil.copyfile(args.jar.resolve(), snapshot)
    expected = digest(snapshot)
    common = ["--jar", str(snapshot), "--libraries", str(args.libraries.resolve()),
              "--eula", str(args.eula.resolve()), "--java", args.java]
    with ThreadPoolExecutor(max_workers=args.jobs) as workers:
        futures = [workers.submit(run_profile, root, name, common, expected) for name in PROFILES]
        cases = dict(future.result() for future in futures)
    passed = all(row["status"] == "passed" for row in cases.values())
    report = {"status": "passed" if passed else "failed", "artifact": snapshot.name,
              "sha256": expected, "cases": cases}
    (root / "result.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    if not passed:
        raise SystemExit(f"Read failed profiles at {root / 'result.json'}")


if __name__ == "__main__":
    main()
