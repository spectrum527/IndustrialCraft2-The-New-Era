"""Install the exact IC2 and fixture JARs into a fresh, loopback-only Forge test instance."""

import argparse
from pathlib import Path
import shutil
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", type=Path, required=True)
    parser.add_argument("--server", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--mode", choices=["both", "none", "top", "curios"], required=True)
    parser.add_argument("artifacts", type=Path, nargs="+")
    args = parser.parse_args()

    launcher = Path("libraries/net/minecraftforge/forge/1.19.2-43.5.0/unix_args.txt")
    if not (args.server / launcher).is_file():
        parser.error(f"Missing Forge launcher at {args.server / launcher}")
    if not all(jar.is_file() for jar in args.artifacts):
        parser.error("Missing an input JAR")

    args.output.mkdir(parents=True, exist_ok=True)
    run = Path(tempfile.mkdtemp(prefix="run-", dir=args.output))
    (run / "libraries").symlink_to((args.server / "libraries").resolve(), target_is_directory=True)
    mods = run / "mods"
    mods.mkdir()
    for jar in args.artifacts:
        shutil.copy2(jar, mods / jar.name)

    (run / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (run / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nlevel-name=world\n"
        "level-seed=20261120\nspawn-protection=0\nview-distance=2\nsimulation-distance=2\n"
        "generate-structures=false\nlevel-type=flat\n"
        'generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},'
        '{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],'
        '"biome":"minecraft:plains","structures":{}}\n',
        encoding="utf-8",
    )
    log = run / "run.log"
    with log.open("wb") as output:
        subprocess.run(
            [str(args.java), "-Xmx2G", f"-Dic2.compat.mode={args.mode}", f"@{launcher}", "nogui"],
            cwd=run,
            stdout=output,
            stderr=subprocess.STDOUT,
            timeout=150,
            check=True,
        )
    report = run / "compat-checks.txt"
    if not report.is_file():
        raise SystemExit(f"Forge wrote no result. See {log}")
    result = report.read_text(encoding="utf-8")
    if not result.startswith("passed\n"):
        raise SystemExit(f"Forge compatibility failed. See {report}\n{result}")
    print(f"{report}\n{result}")


if __name__ == "__main__":
    main()
