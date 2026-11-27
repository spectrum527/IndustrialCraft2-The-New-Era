"""Run the independent world program fixture with the exact packaged Forge candidate."""
import argparse
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
from zipfile import ZipFile


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--java", required=True)
    parser.add_argument("--libraries", type=Path, required=True)
    parser.add_argument("--instance", type=Path, required=True)
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--fixture", type=Path, required=True)
    parser.add_argument("--eula", type=Path, required=True)
    parser.add_argument("--natural", action="store_true")
    parser.add_argument("--details", action="store_true")
    parser.add_argument("--cold", action="store_true")
    args = parser.parse_args()
    if args.natural and args.details:
        parser.error("Select either natural terrain or food and cable details")
    if "eula=true" not in args.eula.read_text().splitlines():
        parser.error("Provide the eula.txt of an accepted Forge instance")
    args.instance.mkdir(parents=True)
    mods = args.instance / "mods"
    mods.mkdir()
    installed = mods / args.jar.name
    shutil.copy2(args.jar, installed)
    expected = hashlib.sha256(args.jar.read_bytes()).hexdigest()
    # Keep the world fixture separate from QA classes which intentionally share a production
    # package for development GameTests. The installed server resolves modules by package.
    with ZipFile(args.jar) as release, ZipFile(args.fixture) as source:
        packages = {name.rpartition("/")[0] for name in release.namelist() if name.endswith(".class")}
        with ZipFile(mods / args.fixture.name, "w") as fixture:
            for entry in source.infolist():
                if not entry.is_dir() and not (entry.filename.endswith(".class")
                        and entry.filename.rpartition("/")[0] in packages):
                    fixture.writestr(entry, source.read(entry))
    shutil.copy2(args.eula, args.instance / "eula.txt")
    (args.instance / "libraries").symlink_to(args.libraries.resolve(), target_is_directory=True)
    properties = ('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nspawn-protection=0\n'
                  'allow-nether=false\nview-distance=2\nsimulation-distance=2\nlevel-seed=20261120\n')
    if not args.natural:
        properties += ('level-type=minecraft:flat\ngenerator-settings={"layers":[{"block":"minecraft:bedrock","height":1},'
                       '{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],'
                       '"biome":"minecraft:plains","structure_overrides":[]}\n')
    (args.instance / "server.properties").write_text(properties)
    options = "win_args.txt" if os.name == "nt" else "unix_args.txt"
    vm = [args.java, "-Xmx1400M", "-Dic2.qa.worldProgram=true",
          f"-Dic2.qa.worldDetails={str(args.details).lower()}"]
    server = [f"@libraries/net/minecraftforge/forge/1.19.2-43.5.0/{options}", "nogui"]
    with (args.instance / "console.log").open("wb") as log:
        subprocess.run(vm + [f"-Dic2.qa.worldNatural={str(args.natural).lower()}"] + server,
                       cwd=args.instance, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=180)
    properties = args.instance / ("world-details.properties" if args.details else "world-program.properties")
    results = properties.read_text() if properties.exists() else ""
    if "status=passed" not in results or not (args.instance / "world/level.dat").is_file():
        raise SystemExit(f"Installed world program failed: {results}\n{args.instance / 'console.log'}")
    if args.cold and not args.natural:
        with (args.instance / "console-cold.log").open("wb") as log:
            subprocess.run(vm + ["-Dic2.qa.worldCold=true"] + server,
                           cwd=args.instance, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=180)
        state = args.instance / ("world-details-cold.properties" if args.details else "world-cold.properties")
        cold = state.read_text() if state.exists() else ""
        if "status=passed" not in cold:
            raise SystemExit(f"Installed Forge cold restart failed: {cold}")
        results += "\nCold restart\n" + cold
    if hashlib.sha256(installed.read_bytes()).hexdigest() != expected:
        raise SystemExit("Installed artifact changed")
    print(f"{installed.name} {expected}\n{results}")


if __name__ == "__main__":
    main()
