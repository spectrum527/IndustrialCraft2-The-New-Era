"""Exercise P2 in a physical Minecraft client connected to a separate dedicated server."""

import argparse
import json
import re
import shutil
import time
from pathlib import Path

from client_acceptance import launch


class Client:
    def __init__(self, run):
        self.run = run
        self.directory = run.directory / "client"
        self.sequence = 0
        self.checks = {}

    def act(self, name):
        self.sequence += 1
        pending = self.directory / "qa-input.tmp"
        pending.write_text(json.dumps({"seq": self.sequence, "action": name}))
        pending.replace(self.directory / "qa-input.txt")
        output = self.directory / "qa-output.json"
        deadline = time.monotonic() + 120
        while time.monotonic() < deadline:
            if self.run.client.poll() is not None:
                raise RuntimeError(f"Client exited {self.run.client.returncode} during {name}")
            if output.exists():
                state = json.loads(output.read_text())
                if state.get("seq") == self.sequence:
                    if "error" in state:
                        raise RuntimeError(f"{name}: {state['error']}")
                    return state
            time.sleep(0.1)
        raise TimeoutError(f"Client did not handle {name}")

    def check(self, name, expected, timeout=90):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            state = self.act("state")
            if expected(state):
                self.checks[name] = state
                (self.run.directory / "play-checks.json").write_text(
                    json.dumps(self.checks, ensure_ascii=False, indent=2) + "\n")
                return state
            time.sleep(0.2)
        raise AssertionError(f"{name}: {state}")

    def enter(self, action, name):
        self.act(action)
        return self.check(name, lambda s: s["world"] and not s["screen"] and not s["overlay"], 150)

    def chat_result(self, action, name, predicate):
        before = self.act("state")["chatCount"]
        self.act(action)
        return self.check(name, lambda s: s["chatCount"] > before and predicate(s["chat"]))


def change_config(path, ratio, heat):
    text = path.read_text()
    for key, value in [("energyRetainedInStorageBlockDrops", ratio), ("hU_per_run", heat)]:
        pattern = rf"(?m)^(\s*{key}\s*=\s*)[^#\r\n]+"
        text, count = re.subn(pattern, lambda m: m[1] + str(value), text)
        if count != 1:
            raise ValueError(f"Expected one {key} in {path}, found {count}")
    path.write_text(text)


def resource_checks(client):
    pack = client.directory / "resourcepacks/qa-status"
    asset = pack / "assets/ic2/status/client.json"
    asset.parent.mkdir(parents=True)
    (pack / "pack.mcmeta").write_text(json.dumps(
        {"pack": {"pack_format": 34, "description": "IC2 P2 acceptance"}}))
    for action, data, expected, label in [
        ("pack-on", '{"message":"Copper resource"}', "Copper resource", "external-pack"),
        ("pack-refresh", '{"message":42}', "Copper resource", "invalid-resource-retains-old"),
        ("pack-refresh", '{"message":"Tin resource"}', "Tin resource", "resource-recovery"),
        ("pack-off", "{}", "Default client resources", "default-resource-restored"),
    ]:
        asset.write_text(data)
        log = client.run.directory / "client.log"
        offset = log.stat().st_size
        client.act(action)
        client.check(label, lambda s: s["reloadDone"] and not s["overlay"]
                     and not s["reloadFailed"] and expected in s["text"])
        if label == "invalid-resource-retains-old":
            with log.open("rb") as stream:
                stream.seek(offset)
                if b"Failed to load IC2 client status resource ic2:status/client.json" not in stream.read():
                    raise AssertionError("No rejection was recorded for the invalid resource candidate")


def live_checks(client):
    run = client.run
    client.check("title-unbound", lambda s: s["screen"] == "TitleScreen" and not s["overlay"]
                 and s["bindings"] == 1 and s["key"] == "key.keyboard.unknown")
    client.enter("connect", "dedicated-join")
    client.act("press")
    client.check("unbound-no-action", lambda s: s["world"] and not s["screen"])
    client.act("bind")
    client.act("chat")
    client.act("press")
    client.check("chat-retains-focus", lambda s: s["screen"] == "ChatScreen")
    client.act("esc")
    client.check("chat-key-not-replayed", lambda s: not s["screen"])
    client.act("press")
    client.check("status-screen", lambda s: s["screen"] == "StatusScreen" and s["title"] == "IC2 Status"
                 and "Default client resources" in s["text"] and "No echo reply" in s["text"])
    client.act("ping")
    echo = client.check("remote-echo", lambda s: re.search(r"Last echo reply -?\d+", s["text"]))
    sequence = re.search(r"Last echo reply (-?\d+)", echo["text"])[1]
    for side, flow in [("server", "CLIENT_TO_SERVER"), ("client", "SERVER_TO_CLIENT")]:
        log = (run.directory / (side + ".log")).read_text(errors="replace")
        values = re.findall(rf"IC2 echo completed on {flow} sequence (-?\d+)", log)
        if values != [sequence]:
            raise AssertionError(f"{side} handled {values}, expected exactly one {sequence}")
    resource_checks(client)
    client.act("esc")
    client.check("status-esc", lambda s: not s["screen"])
    config = run.directory / "server/world/serverconfig/ic2-server.toml"
    change_config(config, 0.25, 5000)
    client.chat_result("reload-config", "non-operator-rejected",
                       lambda msg: "Unknown" in msg or "permission" in msg.lower() or "HERE" in msg)
    run.command("op P2Tester")
    client.chat_result("values", "rejected-command-kept-values", lambda msg: "0.8/4000" in msg)
    client.chat_result("reload-config", "operator-reload", lambda msg: "Reloaded IC2" in msg)
    client.chat_result("values", "both-changed", lambda msg: "0.25/5000" in msg)
    change_config(config, 1.5, 6000)
    client.chat_result("reload-config", "invalid-config", lambda msg: "rejected" in msg)
    client.chat_result("values", "rollback", lambda msg: "0.25/5000" in msg)
    change_config(config, 0.5, 6000)
    client.chat_result("reload-config", "corrected-reload", lambda msg: "Reloaded IC2" in msg)
    client.chat_result("values", "corrected-values", lambda msg: "0.5/6000" in msg)
    client.act("menu")
    client.check("server-opened-menu", lambda s: s["screen"] == "SampleScreen"
                 and s["title"] == "IC2 acceptance menu" and s["menuId"] > 0)
    client.act("esc")
    client.check("server-menu-closed", lambda s: not s["screen"] and s["menuId"] == 0)
    leave(client, "remote-exit")
    client.enter("connect", "reconnected")
    client.act("press")
    client.check("old-reply-cleared", lambda s: s["screen"] == "StatusScreen" and "No echo reply" in s["text"])
    leave(client, "remote-second-exit")


def leave(client, label):
    client.act("disconnect")
    client.check(label, lambda s: s["screen"] == "TitleScreen" and not s["world"] and not s["overlay"])


def integrated_checks(client):
    run = client.run
    run.command("stop")
    if run.server.wait(timeout=90) != 0:
        raise RuntimeError("Dedicated server failed to save its world")
    world = run.directory / "server/world"
    if not (world / "level.dat").is_file():
        raise RuntimeError("Saved world missing")
    for name, ratio, heat in [("single-a", 0.5, 6000), ("single-b", 0.8, 4000)]:
        destination = client.directory / "saves" / name
        shutil.copytree(world, destination, ignore=shutil.ignore_patterns("session.lock"))
        change_config(destination / "serverconfig/ic2-server.toml", ratio, heat)
        client.enter(name, name + "-joined")
        client.check(name + "-config", lambda s: s["integrated"] and s.get("integratedRatio") == ratio
                     and s.get("integratedHeat") == heat)
        client.act("press")
        client.check(name + "-fresh-connection", lambda s: s["screen"] == "StatusScreen" and "No echo reply" in s["text"])
        leave(client, name + "-exit")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--directory", required=True, type=Path)
    parser.add_argument("--prepared", action="store_true")
    args = parser.parse_args()
    with launch(args.directory.resolve(), mode="normal", prepared=args.prepared) as session:
        probe = Client(session)
        live_checks(probe)
        integrated_checks(probe)
        print(f"{len(probe.checks)} physical-client checks passed")
