"""Run the isolated 1.21.1 acceptance client and dedicated server with ModDevGradle inputs."""

import argparse
from contextlib import contextmanager
from dataclasses import dataclass
import json
import os
from pathlib import Path
from protocol_mismatch import check_variant, await_incompatible, verify_incompatible
import re
import signal
import socket
import subprocess
import sys
import tempfile
from threading import Thread, current_thread, main_thread
import time
from typing import Optional


ROOT = Path(__file__).resolve().parents[2]
BUILD = ROOT / "src/neoforge/1.21.1/build"
MANIFEST = BUILD / "platform-client-acceptance/launch.json"
RUNS = BUILD / "run/platform-client-acceptance"
JAR_VERIFIER = Path(__file__).with_name("verify_runtime_jar.py").resolve()
HOST = "127.0.0.1"
SERVER_READY = re.compile(r"\bDone \(\d+(?:\.\d+)?s\)!")
ENVIRONMENT = {
    "PATH", "HOME", "USER", "LOGNAME", "TMPDIR", "TEMP", "TMP", "LANG",
    "DISPLAY", "WAYLAND_DISPLAY", "XDG_RUNTIME_DIR", "XAUTHORITY", "SystemRoot",
}


def prepare():
    subprocess.run(
        [str(ROOT / "gradlew"), ":neoforge:1.21.1:preparePlatformPlay", "--console=plain"],
        cwd=ROOT,
        check=True,
    )
    return MANIFEST


def _read(path):
    if not path.is_file():
        return ""
    return path.read_text(encoding="utf-8", errors="replace")


def _write_json(path, data):
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    temporary.replace(path)


def _mod_folders(value, original, replacement):
    entries = value.split(os.pathsep)
    target = "ic2%%" + original
    if entries.count(target) != 1:
        raise RuntimeError("ModDevGradle did not provide exactly one IC2 main classes folder")
    return os.pathsep.join("ic2%%" + replacement if entry == target else entry for entry in entries)


def _verify_jar(manifest, role, report, match):
    launch = manifest[role]
    prefix = "-Dfml.modFolders="
    declared = [arg[len(prefix):] for arg in launch["jvmArgs"] if arg.startswith(prefix)]
    if "MOD_CLASSES" in launch["environment"]:
        declared.append(launch["environment"]["MOD_CLASSES"])
    if not declared:
        raise AssertionError(f"Missing IC2 mod folders for {role}")
    expected = {Path(folder).resolve() for folder in manifest["productionOutputs"]}
    for folders in declared:
        actual = [Path(entry[len("ic2%%"):]).resolve() for entry in folders.split(os.pathsep)
                  if entry.startswith("ic2%%")]
        if set(actual) != expected or len(actual) != len(expected):
            raise AssertionError(f"The {role} launch does not use the verified IC2 production outputs")
    arguments = [
        sys.executable, str(JAR_VERIFIER), "--jar", manifest["productionJar"],
        "--report", str(report), "--match", str(match),
    ]
    for folder in actual:
        arguments.extend(("--output", str(folder)))
    subprocess.run(arguments, check=True)


def _start(role, directory, port, manifest, mode):
    launch = manifest[role]
    env = {name: value for name, value in os.environ.items()
           if name in ENVIRONMENT or name.startswith("LC_")}
    env.update(launch["environment"])
    jvm = list(launch["jvmArgs"])
    args = list(launch["args"])
    if role == "client" and mode == "bad":
        original = manifest["productionClasses"]
        replacement = manifest["mismatchClasses"]
        prefix = "-Dfml.modFolders="
        uses_mod_property = any(arg.startswith(prefix) for arg in jvm)
        if "MOD_CLASSES" in env:
            env["MOD_CLASSES"] = _mod_folders(env["MOD_CLASSES"], original, replacement)
        elif not uses_mod_property:
            raise RuntimeError("ModDevGradle did not supply the client mod folders")
        jvm = [prefix + _mod_folders(arg[len(prefix):], original, replacement)
               if arg.startswith(prefix) else arg for arg in jvm]
        args += ["--quickPlayMultiplayer", f"{HOST}:{port}"]

    command = [
        launch["java"],
        *jvm,
        f"-Dic2.qa.clientHost={HOST}",
        f"-Dic2.qa.clientPort={port}",
        "-cp",
        launch["classpath"],
        launch["mainClass"],
        *args,
    ]
    with (directory / f"{role}.log").open("wb") as log:
        return subprocess.Popen(
            command,
            cwd=directory / role,
            env=env,
            stdin=subprocess.PIPE if role == "server" else subprocess.DEVNULL,
            stdout=log,
            stderr=subprocess.STDOUT,
            encoding="utf-8",
            start_new_session=True,
        )


def _port():
    with socket.socket() as listener:
        listener.bind((HOST, 0))
        return listener.getsockname()[1]


def _setup(directory, port):
    directory.mkdir(parents=True, exist_ok=False)
    client = directory / "client"
    server = directory / "server"
    client.mkdir()
    server.mkdir()
    (client / "options.txt").write_text(
        "onboardAccessibility:false\nlang:en_us\nfullscreen:false\ntutorialStep:none\n"
        "overrideWidth:960\noverrideHeight:540\nrenderDistance:4\n",
        encoding="utf-8",
    )
    (server / "server.properties").write_text(
        "\n".join(
            [f"server-ip={HOST}", f"server-port={port}", "online-mode=false",
             "level-name=world", "level-type=minecraft:flat", "level-seed=7541",
             "generate-structures=false", "spawn-protection=0", "view-distance=3", "difficulty=peaceful",
             "simulation-distance=3", ""]
        ),
        encoding="utf-8",
    )


def _wait_for_server(server, directory, timeout):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if server.poll() is not None:
            raise RuntimeError(f"Server exited during startup with status {server.returncode}")
        if SERVER_READY.search(_read(directory / "server.log")):
            return
        time.sleep(0.2)
    raise TimeoutError(f"Server did not finish starting in {timeout} seconds")


def _join(program, grace):
    try:
        return program.wait(grace)
    except subprocess.TimeoutExpired:
        program.terminate()
    try:
        return program.wait(10)
    except subprocess.TimeoutExpired:
        program.kill()
        return program.wait(10)


def _stop_client(client, directory):
    if client.poll() is None:
        _write_json(
            directory / "client/qa-input.txt",
            {"seq": time.time_ns() % (2**31 - 1), "action": "quit"},
        )
    return _join(client, 20)


@dataclass(frozen=True)
class AcceptanceRun:
    directory: Path
    port: int
    server: subprocess.Popen
    client: subprocess.Popen
    protocols: dict
    variant: Optional[dict]

    def command(self, command):
        if "\n" in command or "\r" in command:
            raise ValueError("Send one dedicated server command at a time")
        self.server.stdin.write(command + "\n")
        self.server.stdin.flush()


@contextmanager
def launch(directory=None, mode="normal", prepared=False, startup_timeout=120):
    """Yield the real processes after starting the server, and stop both on exit."""
    if mode not in ("normal", "bad"):
        raise ValueError(f"Unknown protocol mode {mode}")
    if not prepared:
        prepare()
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    server_protocol = manifest["protocols"]["production"]
    protocols = {
        "server": server_protocol,
        "client": manifest["protocols"]["mismatch"] if mode == "bad" else server_protocol,
    }
    variant = check_variant(manifest) if mode == "bad" else None
    port = _port()
    if directory is None:
        RUNS.mkdir(parents=True, exist_ok=True)
        directory = Path(tempfile.mkdtemp(prefix="run-", dir=RUNS)) / "session"
    directory = Path(directory).resolve()
    _setup(directory, port)

    client = server = None
    client_role = "client-base" if mode == "bad" else "client"
    server_stamp = directory / "server-jar-start.json"
    client_stamp = directory / f"{client_role}-jar-start.json"
    baseline = manifest["verifiedJar"]
    handles_term = current_thread() is main_thread()
    if handles_term:
        def cancel(signum, frame):
            raise SystemExit(128 + signum)
        previous_term = signal.signal(signal.SIGTERM, cancel)
    try:
        _verify_jar(manifest, "server", server_stamp, baseline)
        server = _start("server", directory, port, manifest, mode)
        _wait_for_server(server, directory, startup_timeout)
        _verify_jar(manifest, "client", client_stamp, baseline)
        client = _start("client", directory, port, manifest, mode)
        _write_json(
            directory / "run-details.json",
            {
                "mode": mode, "host": HOST, "port": port,
                "clientPid": client.pid, "serverPid": server.pid,
                "protocols": protocols, "variant": variant,
            },
        )
        print(f"IC2 QA {mode} {HOST}:{port} in {directory}", flush=True)
        yield AcceptanceRun(directory, port, server, client, protocols, variant)
    finally:
        if handles_term:
            signal.signal(signal.SIGTERM, signal.SIG_IGN)
        try:
            interrupted = sys.exc_info()[0] is not None
            client_exit = server_exit = None
            try:
                if client is not None:
                    client_exit = _stop_client(client, directory)
            finally:
                if server is not None:
                    if server.poll() is None:
                        try:
                            server.stdin.write("stop\n")
                            server.stdin.flush()
                        except BrokenPipeError:
                            pass
                    server_exit = _join(server, 30)
                    server.stdin.close()
                _write_json(directory / "run-result.json", {"clientExit": client_exit, "serverExit": server_exit})
            if server is not None:
                _verify_jar(manifest, "server", directory / "server-jar-end.json", server_stamp)
            if client is not None:
                _verify_jar(manifest, "client", directory / f"{client_role}-jar-end.json", client_stamp)
                if mode == "bad" and check_variant(manifest) != variant:
                    raise AssertionError("The incompatible client changed while running")
            if not interrupted and (client_exit != 0 or server_exit != 0):
                raise RuntimeError(f"Unexpected QA client/server exit statuses {client_exit}/{server_exit}")
        finally:
            if handles_term:
                signal.signal(signal.SIGTERM, previous_term)


def _forward_console(run):
    for line in sys.stdin:
        try:
            run.command(line.rstrip("\r\n"))
        except (BrokenPipeError, ValueError):
            return


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    preparation = parser.add_mutually_exclusive_group()
    preparation.add_argument("--prepare-only", action="store_true")
    preparation.add_argument("--prepared", action="store_true")
    parser.add_argument("--mode", choices=["normal", "bad"], default="normal")
    parser.add_argument("--directory", type=Path)
    parser.add_argument("--timeout", type=int, default=300)
    args = parser.parse_args()
    if args.prepare_only:
        print(prepare())
        return
    with launch(args.directory, args.mode, args.prepared) as run:
        if args.mode == "bad":
            evidence = await_incompatible(run, args.timeout)
        else:
            Thread(target=_forward_console, args=(run,), daemon=True).start()
            deadline = time.monotonic() + args.timeout
            while run.client.poll() is None and run.server.poll() is None:
                if time.monotonic() >= deadline:
                    raise TimeoutError("The interactive QA session elapsed")
                time.sleep(0.25)
    if args.mode == "bad":
        verify_incompatible(run, evidence)


if __name__ == "__main__":
    main()
