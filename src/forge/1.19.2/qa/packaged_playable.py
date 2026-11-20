"""Exercise the production-only Forge 43.5.0 JAR on a fresh dedicated world and restart it."""

import argparse
from collections import Counter
from contextlib import contextmanager
import gzip
import json
import os
from pathlib import Path
import queue
import re
import shutil
import struct
import subprocess
import threading
import time
import zipfile
import zlib

from machine_evidence import nbt
from packaged_server import digest


class Server:
    def __init__(self, process, log):
        self.process = process
        self.log = log
        self.lines = queue.Queue()
        self.checks = []
        self.forest = None
        self.reader = threading.Thread(target=self.collect, daemon=True)
        self.reader.start()

    def collect(self):
        with self.log.open("w", encoding="utf-8") as file:
            for line in self.process.stdout:
                file.write(line)
                file.flush()
                self.lines.put(line)
        self.lines.put(None)

    def wait(self, marker, seconds):
        timeout = time.monotonic() + seconds
        while time.monotonic() < timeout:
            try:
                line = self.lines.get(timeout=max(0, timeout - time.monotonic()))
            except queue.Empty:
                return False
            if line is None:
                raise RuntimeError(f"Server exited before {marker}. See {self.log}")
            if marker in line if isinstance(marker, str) else marker.search(line):
                return line
        return ''

    def send(self, *commands):
        self.process.stdin.write("\n".join(commands) + "\n")
        self.process.stdin.flush()

    def require(self, name, *conditions, seconds=45):
        marker = f"IC2_PLAYABLE_{name}"
        command = f"execute {' '.join(conditions)} run say {marker}"
        timeout = time.monotonic() + seconds
        while time.monotonic() < timeout:
            self.send(command)
            if self.wait(f"[Server] {marker}", min(1, timeout - time.monotonic())):
                self.checks.append(name)
                return
        parts = " ".join(conditions)
        raise AssertionError(f"{name}: {parts}. See {self.log}")

    def state(self, *positions):
        self.send(*(f"data get block {pos}" for pos in positions))


@contextmanager
def boot(directory, command, phase):
    with subprocess.Popen(command, cwd=directory, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                          stderr=subprocess.STDOUT, encoding="utf-8", errors="replace",
                          text=True, bufsize=1) as process:
        server = Server(process, directory / f"{phase}.log")
        try:
            if not server.wait(")! For help", 120):
                raise RuntimeError(f"{phase} never became ready. See {server.log}")
            yield server
        finally:
            if process.poll() is None:
                server.send("save-all flush", "stop")
                try:
                    process.wait(timeout=50)
                except subprocess.TimeoutExpired:
                    process.terminate()
                    try:
                        process.wait(timeout=10)
                    except subprocess.TimeoutExpired:
                        process.kill()
                        process.wait()
            server.reader.join(timeout=5)
        if process.returncode != 0:
            raise RuntimeError(f"{phase} exited with {process.returncode}. See {server.log}")
        text = server.log.read_text(encoding="utf-8", errors="replace")
        parse_errors = [line for line in text.splitlines() if ("ic2:" in line or "ic2/" in line)
                        and ("Parsing error" in line or "Couldn't parse" in line or "Failed to parse" in line)]
        if parse_errors:
            raise AssertionError("\n".join(parse_errors))


MAIN = [f"{x} 150 0" for x in range(5)]
GAP = [f"{x} 150 5" for x in range(3)]
LOSS = [f"{x} 150 10" for x in range(7)]
RESTART = [f"{x} 150 13" for x in range(3)]
SIDECAR = "2 150 -1"
FIRST_CHEST = "5 149 0"


def block(pos, name):
    return f"setblock {pos} {name}"


def above(pos):
    x, y, z = map(int, pos.split())
    return f"{x} {y + 1} {z}"


def feed(pos, item, count):
    hopper = above(pos)
    return [block(hopper, "minecraft:hopper[facing=down]"),
            f"item replace block {hopper} container.0 with {item} {count}"]


def energy(pos, value):
    return f"if data block {pos} {{ic2_energy:{value}.0d}}"


def generator(pos, ticks, eu):
    return f"if data block {pos} {{generator:{{fuel_ticks:{ticks},stored_energy:{eu}.0d}}}}"


def items(pos, item, count, slot=0):
    return f'if data block {pos} Items[{{Slot:{slot}b,id:"{item}",Count:{count}b}}]'


def setup(server):
    server.send(
        "forceload add -16 -16 16 16",
        "fill -1 149 -2 8 149 14 minecraft:stone",
        block(MAIN[0], "ic2:generator"),
        block(MAIN[1], "ic2:insulated_copper_cable"),
        block(MAIN[2], "ic2:batbox[facing=east]"),
        block(MAIN[3], "ic2:insulated_tin_cable"),
        block(MAIN[4], "ic2:macerator"),
        block(SIDECAR, "ic2:batbox[facing=east]"),
        block(FIRST_CHEST, "minecraft:chest"),
        block("4 149 0", "minecraft:hopper[facing=east]"),
        block(GAP[0], "ic2:generator"), block(GAP[2], "ic2:batbox"),
        block(LOSS[0], "ic2:generator"),
        *(block(pos, "ic2:copper_cable") for pos in LOSS[1:6]),
        block(LOSS[6], "ic2:batbox"),
        block(RESTART[0], "ic2:generator"), block(RESTART[2], "ic2:macerator"),
        f"data merge block {RESTART[2]} {{ic2_energy:200.0d}}",
        *feed(MAIN[0], "minecraft:coal", 1),
        *feed(MAIN[4], "minecraft:coal", 2),
        *feed(GAP[0], "minecraft:coal", 1),
        *feed(LOSS[0], "minecraft:coal", 1),
        *feed(RESTART[0], "minecraft:coal", 1),
        *feed(RESTART[2], "minecraft:raw_copper", 1),
    )
    server.require("native_hopper_to_machine", f'if data block {MAIN[4]} Items[{{id:"minecraft:coal"}}]')
    server.require("saved_partial_job", energy(RESTART[2], 0),
                   f"if data block {RESTART[2]} {{ic2_progress:100}}",
                   items(RESTART[2], "minecraft:raw_copper", 1))
    server.require("disconnected_generator_retained_4000_eu", generator(GAP[0], 0, 4000), energy(GAP[2], 0))
    server.require("five_cable_loss_3600_eu", generator(LOSS[0], 0, 0), energy(LOSS[6], 3600))
    server.require("two_coal_dust_delivered_to_chest", items(FIRST_CHEST, "ic2:coal_dust", 2))
    server.require("chain_eu_conserved", generator(MAIN[0], 0, 0), energy(MAIN[2], 1600),
                   energy(MAIN[4], 1200), energy(SIDECAR, 0))
    server.state(MAIN[0], MAIN[2], MAIN[4], FIRST_CHEST, RESTART[2], GAP[0], GAP[2], LOSS[6])
    setup_energy_devices(server)
    setup_logistics(server)
    discover_forest(server)


def restart(server):
    server.require("persisted_balances_and_product", energy(MAIN[2], 1600),
                   energy(MAIN[4], 1200), items(FIRST_CHEST, "ic2:coal_dust", 2), energy(LOSS[6], 3600))
    server.require("persisted_partial_job", energy(RESTART[2], 0),
                   f"if data block {RESTART[2]} {{ic2_progress:100}}",
                   items(RESTART[2], "minecraft:raw_copper", 1))
    server.require("persisted_disconnected_source", generator(GAP[0], 0, 4000), energy(GAP[2], 0))
    server.send(block(GAP[1], "ic2:tin_cable"),
                block(RESTART[1], "ic2:insulated_copper_cable"))
    server.require("reconnected_4000_eu", generator(GAP[0], 0, 0), energy(GAP[2], 4000))
    server.require("resumed_copper_job", items(RESTART[2], "ic2:crushed_copper", 2, 2))
    server.send(f"setblock {RESTART[2]} minecraft:air destroy")
    server.require("destroy_dropped_inventory",
                   'if entity @e[type=minecraft:item,x=2,y=150,z=13,distance=..4,'
                   'nbt={Item:{id:"ic2:crushed_copper",Count:2b}}]')
    server.state(MAIN[2], MAIN[4], FIRST_CHEST, GAP[2], LOSS[6])
    restart_logistics(server)


CABLES = ("copper_cable", "insulated_copper_cable", "tin_cable", "insulated_tin_cable",
          "gold_cable", "insulated_gold_cable", "double_insulated_gold_cable", "iron_cable",
          "insulated_iron_cable", "double_insulated_iron_cable", "triple_insulated_iron_cable",
          "glass_fibre_cable", "detector_cable", "splitter_cable")
ITEM_SRC, ITEM_PIPE, ITEM_DEST = "24 150 2", "25 150 2", "26 150 3"
ITEM_COLD_SRC, ITEM_COLD_PIPE, ITEM_COLD_DEST = "24 150 6", "25 150 6", "26 150 6"
WATER_SRC, WATER_PIPE, WATER_DEST = "24 150 10", "25 150 10", "26 150 10"
WATER_COLD_PIPE, WATER_COLD_DEST = "25 150 14", "26 150 14"


def merge(pos, value):
    return f"data merge block {pos} {value}"


def seed_eu(pos, value):
    return merge(pos, f"{{ic2_energy:{value}.0d}}")


def water(amount):
    return f'ic2_fluid:{{FluidName:"minecraft:water",Amount:{amount}}}'


def verify_water(pos, amount):
    return f"if data block {pos} {{{water(amount)}}}"


def setup_energy_devices(server):
    server.send("forceload add 16 16 32 63")
    routes = []
    for index, cable in enumerate(CABLES):
        z = 20 + index * 2
        source, conductor, sink = (f"{x} 150 {z}" for x in (20, 21, 22))
        routes.append((cable, source, conductor, sink))
        server.send(block(source, "ic2:batbox[facing=east]"), block(conductor, f"ic2:{cable}"),
                    block(sink, "ic2:batbox"), seed_eu(source, 3200))
    server.send(block("21 149 43", "minecraft:stone"), block("21 150 43", "minecraft:redstone_wire"))
    _, src, cable, sink = routes[-1]
    server.require("unpowered_splitter_is_open", energy(src, 3200), energy(sink, 0))
    server.require("detector_output_fifteen", "if block 21 150 43 minecraft:redstone_wire[power=15]")
    server.send(block(above(cable), "minecraft:redstone_block"))
    for name, source, conductor, receiver in routes:
        server.require("conducts_" + name, f"if block {conductor} ic2:{name}",
                       energy(source, 0), energy(receiver, 3200))
    server.require("detector_output_idle", "if block 21 150 43 minecraft:redstone_wire[power=0]")
    for offset, (kind, cable_name, total) in enumerate(
            (("cesu", "copper_cable", 640), ("mfe", "gold_cable", 2560),
             ("mfsu", "iron_cable", 10240))):
        x = 28 + offset
        source = f"{x} 150 20"
        cable_pos = f"{x} 151 20"
        sink = f"{x} 152 20"
        server.send(block(source, f"ic2:{kind}[facing=up]"), block(cable_pos, f"ic2:{cable_name}"),
                    block(sink, f"ic2:{kind}"), seed_eu(source, total))
        server.require("vertical_" + kind + "_tier_transfer", energy(source, 0), energy(sink, total))


def setup_logistics(server):
    server.send(
        block(ITEM_SRC, "minecraft:chest"), block(ITEM_DEST, "minecraft:chest"),
        block(ITEM_PIPE, "ic2:bronze_item_pipe_tiny[facing=east]"),
        block("26 150 2", "ic2:bronze_item_pipe_small[facing=south]"),
        f'item replace block {ITEM_SRC} container.0 with minecraft:diamond{{cargo_marker:9}} 12',
        block(ITEM_COLD_SRC, "minecraft:chest"),
        block(ITEM_COLD_PIPE, "ic2:steel_item_pipe_small[facing=east]"),
        f'item replace block {ITEM_COLD_SRC} container.0 with minecraft:diamond{{cargo_marker:42}} 10',
        block(WATER_SRC, "ic2:bronze_tank"), block(WATER_PIPE, "ic2:bronze_fluid_pipe_medium"),
        block(WATER_DEST, "ic2:iron_tank"),
        merge(WATER_PIPE, "{ic2_pump_lv:16}"), merge(WATER_SRC, f"{{{water(257)}}}"),
        block(WATER_COLD_PIPE, "ic2:bronze_fluid_pipe_medium"),
        block(WATER_COLD_DEST, "ic2:steel_tank"),
        merge(WATER_COLD_PIPE, f"{{{water(717)},ic2_pump_lv:16,ic2_closed:32}}"),
    )
    server.require("item_pipe_turn_preserved_tags",
                   f'if data block {ITEM_DEST} Items[{{id:"minecraft:diamond",Count:12b,tag:{{cargo_marker:9}}}}]')
    server.require("blocked_steel_pipe_kept_four",
                   f'if data block {ITEM_COLD_PIPE} {{ic2_cargo:{{id:"minecraft:diamond",Count:4b,tag:{{cargo_marker:42}}}}}}',
                   items(ITEM_COLD_SRC, "minecraft:diamond", 6))
    server.require("lv_pumped_257_mb_to_steel_path", verify_water(WATER_DEST, 257),
                   f"unless data block {WATER_SRC} ic2_fluid", f"unless data block {WATER_PIPE} ic2_fluid")
    server.require("closed_pipe_retained_717_mb", verify_water(WATER_COLD_PIPE, 717),
                   f"unless data block {WATER_COLD_DEST} ic2_fluid")


def restart_logistics(server):
    server.require("saved_items_tags_pipe",
                   f'if data block {ITEM_COLD_PIPE} {{ic2_cargo:{{id:"minecraft:diamond",Count:4b,tag:{{cargo_marker:42}}}}}}',
                   f"if block {ITEM_COLD_PIPE} ic2:steel_item_pipe_small[facing=east]",
                   items(ITEM_COLD_SRC, "minecraft:diamond", 6))
    server.require("saved_fluid_covers_and_valve", verify_water(WATER_COLD_PIPE, 717),
                   f"if data block {WATER_COLD_PIPE} {{ic2_pump_lv:16,ic2_closed:32}}",
                   f"unless data block {WATER_COLD_DEST} ic2_fluid")
    server.send(block(ITEM_COLD_DEST, "minecraft:chest"), merge(WATER_COLD_PIPE, "{ic2_closed:0}"))
    server.require("reopened_item_route_delivered_ten",
                   f'if data block {ITEM_COLD_DEST} Items[{{id:"minecraft:diamond",Count:10b,tag:{{cargo_marker:42}}}}]')
    server.require("reopened_valve_drained_717_mb", verify_water(WATER_COLD_DEST, 717),
                   f"unless data block {WATER_COLD_PIPE} ic2_fluid")


FOREST = re.compile(r"The nearest minecraft:forest is at \[(-?\d+), [^,]+, (-?\d+)\]")


def discover_forest(server):
    server.send("execute positioned 0 64 0 run locate biome minecraft:forest")
    line = server.wait(FOREST, 40)
    if not line:
        raise AssertionError(f"Could not find a natural forest. See {server.log}")
    x, z = map(int, FOREST.search(line).groups())
    server.forest = [x, z]
    server.send(f"forceload add {x-48} {z-48} {x+48} {z+48}")


def generated_resources(directory):
    counts = Counter()
    names = {"ic2:tin_ore", "ic2:deepslate_tin_ore", "ic2:rubber_log", "ic2:rubber_leaves"}
    for path in (directory / "world/region").glob("*.mca"):
        with path.open("rb") as stream:
            offsets = struct.unpack(">1024I", stream.read(4096))
            for offset in filter(None, offsets):
                stream.seek((offset >> 8) * 4096)
                length, compression = struct.unpack(">IB", stream.read(5))
                data = {1: gzip.decompress, 2: zlib.decompress, 3: bytes}[compression](stream.read(length - 1))
                if b"ic2:" not in data:
                    continue
                chunk = nbt(data)
                for section in chunk["sections"][1]:
                    for row in section.get("block_states", (0, {}))[1].get("palette", (0, []))[1]:
                        name = row["Name"][1]
                        if name in names:
                            counts[name] += 1
    if not names.issubset(counts):
        raise AssertionError(f"Missing natural resources in generated region sections: {names - counts.keys()}")
    return counts

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ("jar", "libraries", "eula", "directory"):
        parser.add_argument("--" + key, type=Path, required=True)
    parser.add_argument("--java", default="java")
    args = parser.parse_args()
    target = args.directory.resolve()
    target.mkdir(parents=True, exist_ok=False)
    if "eula=true" not in args.eula.read_text().splitlines():
        parser.error("Supply an accepted server eula.txt")
    variant = "win_args.txt" if os.name == "nt" else "unix_args.txt"
    relative_args = Path("net/minecraftforge/forge/1.19.2-43.5.0") / variant
    if not (args.libraries / relative_args).is_file():
        parser.error("Provide the libraries of an installed Forge 43.5.0 instance")
    with zipfile.ZipFile(args.jar) as archive:
        entries = set(archive.namelist())
        if any("/forge/qa/" in path for path in entries):
            parser.error("Provide the reobfuscated production JAR without QA fixtures")
        required = {"data/ic2/recipes/generator.json", "data/ic2/recipes/macerator.json",
                    "assets/ic2/blockstates/macerator.json", "assets/ic2/blockstates/batbox.json"}
        if not required.issubset(entries):
            parser.error("The JAR is missing production gameplay resources")
    (target / "libraries").symlink_to(args.libraries.resolve(), target_is_directory=True)
    shutil.copyfile(args.eula, target / "eula.txt")
    mods = target / "mods"
    mods.mkdir()
    installed = mods / args.jar.name
    shutil.copyfile(args.jar, installed)
    fingerprint = digest(installed)
    (target / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nlevel-seed=20261120\n"
        "generate-structures=false\nview-distance=3\nsimulation-distance=3\n")
    command = [args.java, "-Xmx1400M", "@libraries/" + relative_args.as_posix(), "nogui"]
    observations = {}
    for phase, operation in (("write", setup), ("read", restart)):
        with boot(target, command, phase) as server:
            operation(server)
            observations[phase] = {"pid": server.process.pid, "checks": server.checks}
            if server.forest is not None:
                observations[phase]["natural_forest"] = server.forest
        if digest(installed) != fingerprint:
            raise AssertionError("Installed game JAR changed during the test")
    result = {"minecraft": "1.19.2", "forge": "43.5.0", "jar": args.jar.name,
              "sha256": fingerprint, "phases": observations,
              "worldgen_palette_hits": generated_resources(target)}
    (target / "result.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
