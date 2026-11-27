"""Standalone JVM launches for the Forge development run or an explicitly installed candidate."""

import hashlib
import json
import os
from pathlib import Path
import subprocess


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def java_process(command, directory, log, environment=None):
    with log.open("w") as stream:
        result = subprocess.run(command, cwd=directory, env=environment, stdin=subprocess.DEVNULL,
                                stdout=stream, stderr=subprocess.STDOUT, timeout=240)
    if result.returncode:
        raise RuntimeError(f"Forge exited {result.returncode}. See {log}")
    return result.returncode


class Development:
    def __init__(self, manifest):
        self.config = json.loads(manifest.read_text())
        self.production = Path(self.config["jar"])
        self.initial_sha = sha(self.production)

    def run(self, phase, directory, log):
        settings = self.config["smoke" if phase == "smoke" else "world"]
        jvm = [arg for arg in settings["jvm"] if not arg.startswith("-Dic2.qa.worldPhase=")]
        if phase != "smoke":
            jvm.append("-Dic2.qa.worldPhase=" + phase)
        args = [settings["java"], *jvm, "-cp", settings["classpath"], settings["main"], *settings["args"]]
        return java_process(args, directory, log, {**os.environ, **settings["env"]})

    def verify(self, result):
        folders = self.config["world"]["env"]["MOD_CLASSES"].split(os.pathsep)
        inputs = {}
        for mod in ("ic2", "ic2_qa"):
            declared = [Path(p.split("%%", 1)[1]).resolve() for p in folders if p.startswith(mod + "%%")]
            origin = Path(result["generator.modFile." + mod]).resolve()
            if origin not in declared:
                raise AssertionError(f"Loaded {mod} at {origin} outside its declared Forge outputs")
            inputs[mod] = [str(path) for path in declared]
        if sha(self.production) != self.initial_sha:
            raise AssertionError("The reobfuscated candidate changed during the development run")
        return {"runtime": "ForgeGradle named outputs", "loadedModDirectories": inputs,
                "correspondingReobfuscatedJar": self.production.name, "sha256": self.initial_sha}
