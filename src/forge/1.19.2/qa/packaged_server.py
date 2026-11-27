"""Boot the distributable JAR twice on an installed, unmodified Forge 43.5.0 server."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import queue
import shutil
import subprocess
import threading
import time


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def boot(directory, command, phase):
    process = subprocess.Popen(command, cwd=directory, stdin=subprocess.PIPE,
                               stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                               text=True, encoding="utf-8", errors="replace", bufsize=1)
    lines = queue.Queue()

    def read_output():
        with (directory / f"{phase}.log").open("w", encoding="utf-8") as out:
            for line in process.stdout:
                out.write(line)
                out.flush()
                lines.put(line)
        lines.put(None)

    reader = threading.Thread(target=read_output, daemon=True)
    reader.start()

    def wait_for(fragment, limit=120):
        deadline = time.monotonic() + limit
        try:
            while True:
                line = lines.get(timeout=max(0, deadline - time.monotonic()))
                if line is None:
                    raise RuntimeError(f"{phase}: server exited while waiting for {fragment}")
                if fragment in line:
                    return
        except queue.Empty:
            raise TimeoutError(f"{phase}: missing {fragment}; see {phase}.log") from None

    def send(commands):
        process.stdin.write("\n".join(commands) + "\n")
        process.stdin.flush()

    try:
        wait_for(')! For help')
        if phase == "write":
            send(["forceload add 0 0", "setblock 0 80 0 minecraft:emerald_block",
                  "scoreboard objectives add ic2_p2 dummy"])
        send(["execute if block 0 80 0 minecraft:emerald_block run say IC2_P2_WORLD_OK"])
        wait_for("[Server] IC2_P2_WORLD_OK")
        send(["execute store success score reload ic2_p2 run ic2 config reload",
              "execute if score reload ic2_p2 matches 1 run say IC2_P2_RELOAD_OK"])
        wait_for("[Server] IC2_P2_RELOAD_OK")
        send(["save-all flush", "stop"])
        if process.wait(timeout=90) != 0:
            raise RuntimeError(f"{phase}: unclean shutdown")
        return process.pid
    finally:
        if process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
        reader.join(timeout=5)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--libraries", type=Path, required=True,
                        help="libraries/ from an installed Forge 1.19.2-43.5.0 server")
    parser.add_argument("--eula", type=Path, required=True, help="your accepted server eula.txt")
    parser.add_argument("--directory", type=Path, required=True, help="fresh test directory")
    parser.add_argument("--java", default="java", help="Java 17 executable")
    args = parser.parse_args()
    args.directory.mkdir(parents=True, exist_ok=False)
    if "eula=true" not in args.eula.read_text().splitlines():
        parser.error("Supply the eula.txt from your accepted installation")
    variant = "win_args.txt" if os.name == "nt" else "unix_args.txt"
    arg_file = Path("libraries/net/minecraftforge/forge/1.19.2-43.5.0") / variant
    if not (args.libraries.resolve().parent / arg_file).is_file():
        parser.error("Missing Forge 43.5.0 argument file")
    (args.directory / "libraries").symlink_to(args.libraries.resolve(), target_is_directory=True)
    shutil.copyfile(args.eula, args.directory / "eula.txt")
    mods = args.directory / "mods"
    mods.mkdir()
    installed = mods / args.jar.name
    shutil.copyfile(args.jar, installed)
    fingerprint = digest(args.jar)
    (args.directory / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nlevel-seed=42\n"
        "generate-structures=false\nview-distance=3\nsimulation-distance=3\n")
    command = [args.java, "-Xmx1400M", "@" + str(arg_file), "nogui"]
    pids = []
    for phase in ("write", "read"):
        if digest(installed) != fingerprint:
            raise RuntimeError("Installed JAR changed between runs")
        pids.append(boot(args.directory, command, phase))
        if not (args.directory / "world/serverconfig/ic2-server.toml").is_file():
            raise RuntimeError("Missing per-world IC2 server configuration")
    result = {"minecraft": "1.19.2", "forge": "43.5.0", "sha256": fingerprint,
              "pids": pids, "server_exits": [0, 0], "config_reloads": 2,
              "persisted_block": "minecraft:emerald_block", "position": [0, 80, 0]}
    (args.directory / "result.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result))


if __name__ == "__main__":
    main()
