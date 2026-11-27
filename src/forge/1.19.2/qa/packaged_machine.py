"""Run the identical cold generator world checks with two independently reobfuscated mod JARs."""

import argparse
import os
from pathlib import Path
import shutil
import zipfile

from machine_launch import java_process, sha
from verify import world, write_json


class Installed:
    def __init__(self, args, directory):
        if "eula=true" not in args.eula.read_text().splitlines():
            raise ValueError("Provide an accepted server eula.txt")
        self.eula = args.eula
        (directory / "libraries").symlink_to(args.libraries.resolve(), target_is_directory=True)
        self.args = Path("libraries/net/minecraftforge/forge/1.19.2-43.5.0") / (
            "win_args.txt" if os.name == "nt" else "unix_args.txt")
        if not (directory / self.args).is_file():
            raise FileNotFoundError(self.args)
        self.java = str(args.java)
        self.inputs = {"ic2": args.jar.resolve(), "ic2_qa": args.fixtures.resolve()}
        mods = directory / "mods"
        mods.mkdir()
        self.files = {mod: mods / source.name for mod, source in self.inputs.items()}
        self.hashes = {mod: sha(path) for mod, path in self.inputs.items()}
        if self.files["ic2"] == self.files["ic2_qa"]:
            raise ValueError("Select separate production and QA JARs")
        for mod, path in self.inputs.items():
            with zipfile.ZipFile(path) as jar:
                files = set(jar.namelist())
                is_fixture = any(name.startswith("com/industrialage/ia2/forge/qa/") for name in files)
                machine = "com/industrialage/ia2/machine/FuelGeneratorBlockEntity.class" in files
                if is_fixture != (mod == "ic2_qa") or machine != (mod == "ic2"):
                    raise AssertionError(f"Mixed or incomplete JAR {path.name}")
            shutil.copyfile(path, self.files[mod])

    def run(self, phase, directory, log):
        shutil.copyfile(self.eula, directory / "eula.txt")
        return java_process([self.java, "-Xmx1400M", "-Dic2.qa.worldPhase=" + phase,
                             "@" + str(self.args), "nogui"], directory, log)

    def verify(self, observation):
        result = {}
        for mod, path in self.files.items():
            if (Path(observation["generator.modFile." + mod]).resolve() != path.resolve()
                    or self.hashes[mod] != sha(path) or self.hashes[mod] != sha(self.inputs[mod])):
                raise AssertionError(f"Loaded {mod} does not match the supplied JAR")
            result[mod] = {"filename": path.name, "sha256": self.hashes[mod]}
        return {"runtime": "installed Forge 43.5.0", "jars": result}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ("jar", "fixtures", "libraries", "eula", "output"):
        parser.add_argument("--" + key, required=True, type=Path)
    parser.add_argument("--java", default="java")
    args = parser.parse_args()
    directory = args.output.resolve()
    directory.mkdir(parents=True, exist_ok=False)
    try:
        result = world(directory, Installed(args, directory))
    except Exception as failure:
        write_json(directory / "result.json", {"status": "failed", "error": str(failure)})
        raise
    write_json(directory / "result.json", {"status": "passed", "evidence": result})
    print("PASS packaged machine", directory / "result.json")


if __name__ == "__main__":
    main()
