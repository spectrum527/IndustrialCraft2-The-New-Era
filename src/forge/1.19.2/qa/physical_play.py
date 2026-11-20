"""Drive the real Forge client, isolated dedicated server, and version-mismatched client."""

from dataclasses import dataclass
import json
import os
from pathlib import Path
import re
import socket
import subprocess
import sys
import time

from verify import ROOT, server_setup, write_json

# Shared, clean-room client control protocol (sequenced input, deadlines and state snapshots).
sys.path.insert(0, str(ROOT / "src/test"))
from platform_play_check import Client, change_config, integrated_checks, leave
from magnetizer_check import check_magnetizer
from protocol_mismatch import check_variant


def logs(directory, side):
    path = directory / f"{side}.log"
    return path.read_text(errors="replace") if path.is_file() else ""


def spawn(directory, side, port, manifest, rejecting=None):
    launch = manifest[side]
    keep = {"PATH", "HOME", "USER", "LOGNAME", "TMPDIR", "TEMP", "TMP", "LANG",
            "DISPLAY", "XAUTHORITY", "XDG_RUNTIME_DIR", "WAYLAND_DISPLAY", "SystemRoot",
            "LD_LIBRARY_PATH", "LIBGL_ALWAYS_SOFTWARE", "ALSOFT_DRIVERS"}
    env = {key: value for key, value in os.environ.items() if key in keep or key.startswith(("LC_", "MESA_"))}
    env.update(launch["env"])
    classpath = launch["classpath"]
    if rejecting:
        original, variant = (manifest[key] for key in ("productionClasses", "mismatchClasses"))
        ids = env["MOD_CLASSES"].split(os.pathsep)
        if ids.count("ic2%%" + original) != 1:
            raise AssertionError("Cannot locate the production Forge registrar in the client mod list")
        env["MOD_CLASSES"] = os.pathsep.join(
            "ic2%%" + variant if entry == "ic2%%" + original else entry for entry in ids)
        classpath = os.pathsep.join(variant if entry == original else entry
                                     for entry in classpath.split(os.pathsep))
    jvm = [arg for arg in launch["jvm"] if not arg.startswith("-Dic2.qa.clientPort=")]
    if rejecting == "server":
        jvm.append("-Dic2.qa.acceptOlderServer=true")
    args = [launch["java"], *jvm, f"-Dic2.qa.clientPort={port}", "-cp", classpath,
            launch["main"], *launch["args"]]
    with (directory / f"{side}.log").open("wb") as log:
        return subprocess.Popen(args, cwd=directory / side, env=env, stdin=subprocess.PIPE,
                                stdout=log, stderr=subprocess.STDOUT, text=True, start_new_session=True)


@dataclass
class Session:
    directory: Path
    server: subprocess.Popen
    client: subprocess.Popen

    def command(self, command):
        self.server.stdin.write(command + "\n")
        self.server.stdin.flush()


def until(predicate, label, process, timeout=120):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError(f"{label} ended with exit {process.returncode}")
        if predicate():
            return
        time.sleep(0.25)
    raise TimeoutError(label)


def resource_checks(client):
    pack = client.directory / "resourcepacks/qa-status"
    asset = pack / "assets/ic2/status/client.json"
    asset.parent.mkdir(parents=True)
    write_json(pack / "pack.mcmeta", {"pack": {"pack_format": 9, "description": "Forge 1.19.2 QA"}})
    for action, message, expected, label in [
        ("pack-on", "Copper resource", "Copper resource", "external-pack"),
        ("pack-refresh", 42, "Copper resource", "invalid-resource-retains-old"),
        ("pack-refresh", "Tin resource", "Tin resource", "resource-recovery"),
        ("pack-off", "", "Default client resources", "restored-resource"),
    ]:
        write_json(asset, {"message": message})
        offset = (client.run.directory / "client.log").stat().st_size
        client.act(action)
        client.check(label, lambda state: state["reloadDone"] and not state["overlay"]
                     and not state["reloadFailed"] and expected in state["text"])
        if label == "invalid-resource-retains-old":
            with (client.run.directory / "client.log").open("rb") as log:
                log.seek(offset)
                if b"Failed to load IC2 client status resource ic2:status/client.json" not in log.read():
                    raise AssertionError("The invalid resource produced no rejection evidence")


def machine_menu(client):
    session = client.run
    for command in ("forceload add 25 25", "fill 24 89 22 27 89 27 minecraft:stone",
                    "setblock 25 90 25 ic2:batbox[facing=north]",
                    "data merge block 25 90 25 {ic2_energy:35000.0d}",
                    "tp P2Tester 25.5 90 23.5"):
        session.command(command)
    client.check("native-batbox-arrived", lambda state: state.get("nativeMachine") == "ic2:batbox")
    client.act("use-native-machine")
    client.check("native-batbox-screen", lambda state: state["screen"] == "MachineScreen"
                 and state.get("machineEnergy") == 35000 and state.get("machineCapacity") == 40000)
    client.act("cycle-native-redstone")
    client.check("native-batbox-mode-roundtrip", lambda state: state.get("machineRedstone") == 1)
    client.act("snapshot-native-machine")
    until(lambda: (session.directory / "client/screenshots/native-batbox.png").is_file(),
          "Native machine screenshot", session.client)
    client.act("esc")
    client.check("native-batbox-close", lambda state: not state["screen"])


def workshop_menu(client):
    session = client.run
    for command in ("forceload add 29 25",
                    "fill 28 89 22 30 89 26 minecraft:stone",
                    "setblock 29 90 25 ic2:metal_former[facing=north]",
                    'data merge block 29 90 25 {ic2_energy:3500.0d,ic2_mode:1,'
                    'Items:[{Slot:0b,id:"minecraft:copper_ingot",Count:2b},'
                    '{Slot:18b,id:"ic2:overclocker_upgrade",Count:1b}]}',
                    "tp P2Tester 29.5 90 23.5"):
        session.command(command)
    client.check("native-former-arrived", lambda state: state.get("nativeWorkshop") == "ic2:metal_former")
    client.act("use-native-workshop")
    before = client.check("native-former-live-upgrade-and-recipe",
                          lambda state: state["screen"] == "WorkshopScreen"
                          and state.get("workshopDuration") == 140 and state.get("workshopEnergy", 0) > 0
                          and state.get("workshopProgress", 0) > 0
                          and "Copper Plate" in state.get("workshopRecipe", "")
                          and state.get("workshopMode") == "Rolling"
                          and "upgrade" in state.get("workshopSlot", "").lower())
    client.act("snapshot-native-workshop")
    until(lambda: (session.directory / "client/screenshots/native-metal-former-upgrade.png").is_file(),
          "Native upgraded former screenshot", session.client)
    client.check("native-former-progress-advanced-live",
                 lambda state: state.get("workshopProgress", 0) > before["workshopProgress"]
                 and state["workshopEnergy"] < before["workshopEnergy"])
    client.act("cycle-native-redstone")
    client.check("native-former-mode-button-reached-server",
                 lambda state: state.get("workshopMode") == "Cutting" and state.get("workshopProgress") == 0)
    client.act("esc")
    client.check("native-former-closed", lambda state: not state["screen"])

    session.command("setblock 29 90 25 ic2:ore_washing_plant[facing=north]")
    session.command('data merge block 29 90 25 {ic2_energy:16000.0d,'
                    'ic2_input_fluid:{FluidName:"minecraft:water",Amount:2000},'
                    'Items:[{Slot:0b,id:"ic2:crushed_tin",Count:2b}]}')
    client.check("native-washer-arrived", lambda state: state.get("nativeWorkshop") == "ic2:ore_washing_plant")
    client.act("use-native-workshop")
    client.check("native-washer-tank-and-recipe-live",
                 lambda state: state.get("workshopFluid") == 2000
                 and "water" in state.get("workshopFluidName", "").lower()
                 and "Purified" in state.get("workshopRecipe", "")
                 and state.get("workshopProgress", 0) > 0)
    client.act("snapshot-native-washer")
    until(lambda: (session.directory / "client/screenshots/native-ore-washer.png").is_file(),
          "Native ore washer screenshot", session.client)
    client.act("esc")
    client.check("native-washer-closed", lambda state: not state["screen"])


def normal(client):
    session = client.run
    # The player stands still with screens open during the resource and machine checks.
    session.command("gamerule doMobSpawning false")
    client.enter("connect", "dedicated-join")
    client.act("press")
    client.check("unbound-no-action", lambda s: s["world"] and not s["screen"])
    client.act("bind")
    client.act("chat")
    client.act("press")
    client.check("chat-focus", lambda s: s["screen"] == "ChatScreen")
    client.act("esc")
    client.check("chat-key-not-replayed", lambda s: not s["screen"])
    client.act("press")
    client.check("status-screen", lambda s: s["screen"] == "StatusScreen" and s["title"] == "IC2 Status"
                 and "Default client resources" in s["text"] and "No echo reply" in s["text"])
    client.act("ping")
    echo = client.check("echo-reply", lambda s: re.search(r"Last echo reply -?\d+", s["text"]))
    number = re.search(r"Last echo reply (-?\d+)", echo["text"])[1]
    for side, flow in [("server", "CLIENT_TO_SERVER"), ("client", "SERVER_TO_CLIENT")]:
        observed = re.findall(rf"IC2 echo completed on {flow} sequence (-?\d+)", (session.directory / side / "logs/debug.log").read_text(errors="replace"))
        if observed != [number]:
            raise AssertionError(f"{side} handled {observed}, expected exactly one {number}")
    resource_checks(client)
    client.act("esc")
    client.check("status-esc", lambda s: not s["screen"])
    file = session.directory / "server/world/serverconfig/ic2-server.toml"
    change_config(file, 0.25, 5000)
    client.chat_result("reload-config", "non-operator-denied",
                       lambda msg: "Unknown" in msg or "permission" in msg.lower() or "HERE" in msg)
    session.command("op P2Tester")
    client.chat_result("values", "denial-kept-current-values", lambda msg: "0.8/4000" in msg)
    for ratio, heat, expected, message, label in [
        (0.25, 5000, "0.25/5000", "Reloaded IC2", "operator-reload"),
        (1.5, 6000, "0.25/5000", "rejected", "invalid-config"),
        (0.5, 6000, "0.5/6000", "Reloaded IC2", "corrected-reload"),
    ]:
        change_config(file, ratio, heat)
        client.chat_result("reload-config", label, lambda msg: message in msg)
        client.chat_result("values", label + "-snapshot", lambda msg: expected in msg)
    client.act("menu")
    client.check("server-opened-menu", lambda s: s["screen"] == "SampleScreen"
                 and s["title"] == "IC2 acceptance menu" and s["menuId"] > 0)
    client.act("press")
    client.check("bound-key-keeps-menu", lambda s: s["screen"] == "SampleScreen" and s["menuId"] > 0)
    client.act("esc")
    client.check("closed-menu", lambda s: not s["screen"] and s["menuId"] == 0)
    machine_menu(client)
    workshop_menu(client)
    lift = check_magnetizer(client, session.command, session.directory / "server.log")
    write_json(session.directory / "magnetizer-checks.json", lift)
    leave(client, "left-server")
    client.act("press")
    client.check("bound-key-keeps-main-menu", lambda s: s["screen"] == "TitleScreen" and not s["world"])
    client.enter("connect", "rejoined-server")
    client.act("press")
    client.check("cleared-echo", lambda s: s["screen"] == "StatusScreen" and "No echo reply" in s["text"])
    leave(client, "left-server-again")
    integrated_checks(client, accept_experimental=True)
    return {"echo": number}


def mismatch(client, manifest, rejecting):
    session = client.run
    variant = check_variant(manifest)
    client.act("connect")
    client.check("negotiation-refused", lambda state: not state["world"]
                 and state["screen"] in {"DisconnectedScreen", "ModMismatchDisconnectedScreen"})
    peer = "server" if rejecting == "client" else "client"
    incoming = "1" if peer == "server" else "2"
    debug = {side: (session.directory / side / "logs/debug.log").read_text(errors="replace")
             for side in ("server", "client")}
    channels = ("ic2:echo_request", "ic2:echo_reply")
    lines = debug[rejecting].splitlines()
    rejection = [line for line in lines if f"rejected their {peer} side version number" in line
                 and "during listping" not in line and all(channel in line for channel in channels)]
    if not rejection or not any(f"Terminating connection with {peer}, mismatched mod list" in line for line in lines):
        raise AssertionError(f"The {rejecting} did not reject both IC2 channels during the handshake")
    for channel in channels:
        if f"Channel '{channel}' : Version test of '{incoming}' from {peer} : REJECTED" not in debug[rejecting]:
            raise AssertionError(f"Missing incoming protocol {incoming} for {channel} on {rejecting}")
    if (re.search(r"logged in with entity id|joined the game", debug["server"])
            or any("IC2 echo completed" in content for content in debug.values())):
        raise AssertionError("Incompatible client entered play or delivered an echo")
    return {"serverProtocol": "1", "clientProtocol": "2", "rejectingPeer": rejecting,
            "rejectedRemoteVersion": incoming, "variant": variant, "serverJoins": 0,
            "echoes": 0, "rejection": rejection}


def stop(process, command=None):
    if process is None:
        return None
    if process.poll() is None and command:
        try:
            command()
        except (BrokenPipeError, ProcessLookupError):
            pass
    try:
        return process.wait(timeout=35)
    except subprocess.TimeoutExpired:
        process.terminate()
        try:
            return process.wait(timeout=8)
        except subprocess.TimeoutExpired:
            process.kill()
            return process.wait(timeout=8)


def exercise(directory, manifest, rejecting=None, profile=None):
    client_dir, server_dir = directory / "client", directory / "server"
    client_dir.mkdir()
    server_dir.mkdir()
    with socket.socket() as listener:
        listener.bind(("127.0.0.1", 0))
        port = listener.getsockname()[1]
    server_setup(server_dir, port)
    (client_dir / "options.txt").write_text(
        "lang:en_us\nfullscreen:false\noverrideWidth:960\noverrideHeight:540\nrenderDistance:4\n"
        "tutorialStep:none\npauseOnLostFocus:false\nsoundCategory_master:0.0\n")
    server = client = None
    try:
        server = spawn(directory, "server", port, manifest)
        until(lambda: re.search(r"Done \([\d.]+s\)!", logs(directory, "server")), "Dedicated server startup", server, 160)
        client = spawn(directory, "client", port, manifest, rejecting)
        session = Session(directory, server, client)
        probe = Client(session)
        probe.check("main-menu-unbound", lambda s: s["screen"] == "TitleScreen" and not s["overlay"]
                    and s["bindings"] == 1 and s["key"] == "key.keyboard.unknown", 160)
        if rejecting:
            result = mismatch(probe, manifest, rejecting)
        else:
            result = (profile or normal)(probe)
    finally:
        def quit_client():
            tmp = client_dir / "qa-quit.tmp"
            write_json(tmp, {"seq": 0, "action": "quit"})
            tmp.replace(client_dir / "qa-input.txt")
        client_exit = stop(client, quit_client)
        server_exit = stop(server, lambda: Session(directory, server, client).command("stop"))
        exits = {"server": server_exit, "client": client_exit}
        write_json(directory / "process-exits.json", exits)
    if client_exit != 0 or server_exit != 0:
        raise AssertionError(f"Unexpected process exit codes {exits}")
    return dict(result, host="127.0.0.1", port=port, serverPid=server.pid, clientPid=client.pid,
                exits=exits, checks=list(probe.checks))
