"""Drive three representative survival removals from a real Forge client over localhost."""
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import time
from pathlib import Path

import physical_play
from physical_play import exercise, logs

POS = "29 90 25"


def require(session, label, condition, timeout=30):
    marker = "IC2_STORAGE_" + label.upper()
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        session.command(f"execute {condition} run say {marker}")
        time.sleep(0.35)
        if marker in logs(session.directory, "server"):
            return
        if session.server.poll() is not None:
            raise RuntimeError(f"Server exited waiting for {label}")
    raise AssertionError(f"Missing server assertion {label}: {condition}")


def profile(client):
    session = client.run
    client.enter("connect", "connected-survival-client")
    for cmd in ("gamerule doMobSpawning false", "scoreboard objectives add storageqa dummy",
                "forceload add 29 25", "fill 27 89 22 31 89 28 minecraft:stone",
                "gamemode survival P2Tester"):
        session.command(cmd)
    cases = [
        ("wooden_storage_box", 27, None),
        ("iron_storage_box", 45, "minecraft:diamond_pickaxe"),
        ("personal_chest", 54, "ic2:wrench"),
    ]
    results = {}
    for name, size, tool in cases:
        label = name.replace("_storage_box", "").replace("personal_chest", "personal")
        mid, last = size // 2, size - 1
        payload = (f'Items:[{{Slot:0b,id:"minecraft:diamond",Count:2b}},'
                   f'{{Slot:{mid}b,id:"minecraft:iron_sword",Count:1b,tag:{{Damage:7,ic2_qa_marker:123}}}},'
                   f'{{Slot:{last}b,id:"minecraft:redstone",Count:17b}}]')
        session.command("clear P2Tester")
        session.command("tp P2Tester 29.5 90 23.5")
        session.command(f"setblock {POS} ic2:{name}")
        session.command(f"data merge block {POS} {{{payload}}}")
        if name == "personal_chest":
            session.command(f"data modify block {POS} owner set from entity P2Tester UUID")
        if tool:
            session.command(f"item replace entity P2Tester hotbar.0 with {tool}")
        require(session, label + "_setup", f"if block {POS} ic2:{name} if data block {POS} {{{payload}}}")
        client.check(label + "-loaded", lambda state: state.get("storageBlock") == "ic2:" + name)
        if tool:
            client.check(label + "-tool", lambda state: any(i["item"] == tool for i in state.get("storageHotbar", [])))
            client.act("storage-select:" + tool)
        else:
            client.act("storage-select:minecraft:air")
        client.act("storage-wrench" if name == "personal_chest" else "storage-mine")
        client.check(label + "-removed-by-client", lambda state: state.get("storageBlock") == "minecraft:air"
                     and not state.get("storageMining"), timeout=100)
        expected = '{Item:{id:"ic2:' + name + '",Count:1b,tag:{BlockEntityTag:{' + payload + '}}}}'
        deadline = time.monotonic() + 30
        marker = "IC2_STORAGE_" + label.upper() + "_DROP"
        while time.monotonic() < deadline:
            session.command(f"execute store result score #drops storageqa run execute positioned {POS} if entity @e[type=minecraft:item,distance=..4]")
            session.command(f"execute positioned {POS} if score #drops storageqa matches 1 if entity @e[type=minecraft:item,distance=..4,nbt={expected}] run say {marker}")
            time.sleep(.35)
            if marker in logs(session.directory, "server"):
                break
        else:
            raise AssertionError(f"{label}: expected exactly one packed item entity with all NBT")
        session.command("tp P2Tester 29.5 90 25.5")
        client.check(label + "-picked-up", lambda state: any(item["item"] == "ic2:" + name
                     and item["count"] == 1 for item in state.get("storageHotbar", [])))
        require(session, label + "_no_scattered_items",
                f"positioned {POS} unless entity @e[type=minecraft:item,distance=..4]")
        session.command("tp P2Tester 29.5 90 23.5")
        client.act("storage-select:ic2:" + name)
        time.sleep(.5)
        client.act("storage-place")
        client.check(label + "-replaced", lambda state: state.get("storageBlock") == "ic2:" + name)
        require(session, label + "_contents",
                f"if data block {POS} {{{payload}}} unless data entity P2Tester {{Inventory:[{{id:\"ic2:{name}\"}}]}}")
        if name == "personal_chest":
            for index in range(4):
                session.command(f"execute store result score #owner{index} storageqa run data get block {POS} owner[{index}]")
                session.command(f"execute store result score #player{index} storageqa run data get entity P2Tester UUID[{index}]")
            equality = " ".join(f"if score #owner{i} storageqa = #player{i} storageqa" for i in range(4))
            require(session, label + "_owned", equality)
        results[name] = {"tool": tool or "empty hand", "slots": [0, mid, last],
                         "drop": "one packed entity", "pickup": "player inventory", "replaced": True}
        session.command(f"setblock {POS} air")
    return {"storage": results, "runtime": "real 1.19.2 client connected to dedicated server"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--installed-jar", type=Path)
    parser.add_argument("--qa-jar", type=Path)
    parser.add_argument("--libraries", type=Path)
    args = parser.parse_args()
    if bool(args.installed_jar) != bool(args.qa_jar) or bool(args.installed_jar) != bool(args.libraries):
        parser.error("--installed-jar, --qa-jar and --libraries must be provided together")
    args.output.mkdir(parents=True, exist_ok=False)
    manifest = json.loads(args.manifest.read_text())
    if args.installed_jar:
        original_spawn = physical_play.spawn
        production = args.installed_jar.resolve()
        qa = args.qa_jar.resolve()
        libraries = args.libraries.resolve()

        def spawn(directory, side, port, config, rejecting=None):
            if side != "server":
                return original_spawn(directory, side, port, config, rejecting)
            server_dir = directory / "server"
            (server_dir / "libraries").symlink_to(libraries, target_is_directory=True)
            (server_dir / "mods").mkdir()
            shutil.copy2(production, server_dir / "mods" / production.name)
            shutil.copy2(qa, server_dir / "mods" / qa.name)
            java = manifest["server"]["java"]
            command = [java, "-Xmx1200M", "@libraries/net/minecraftforge/forge/1.19.2-43.5.0/unix_args.txt", "nogui"]
            with (directory / "server.log").open("wb") as log:
                return subprocess.Popen(command, cwd=server_dir, env=os.environ.copy(),
                                        stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT,
                                        text=True, start_new_session=True)

        physical_play.spawn = spawn
    result = exercise(args.output, manifest, profile=profile)
    if args.installed_jar:
        digest = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
        result["serverArtifact"] = args.installed_jar.name
        result["serverSha256"] = digest(args.installed_jar)
        if digest(args.output / "server/mods" / args.installed_jar.name) != result["serverSha256"]:
            raise AssertionError("Installed server JAR differs from the supplied candidate")
        result["serverRuntime"] = "installed reobfuscated Forge JAR"
        result["clientRuntime"] = "ForgeGradle named output with client QA driver"
    (args.output / "result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")


if __name__ == "__main__":
    main()
