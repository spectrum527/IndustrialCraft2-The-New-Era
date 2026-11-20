"""Drive actual heat, air, water, and EU production lines using the Forge release JAR."""

import argparse
import json
import os
import re
from pathlib import Path
import shutil
import time
import zipfile

from packaged_playable import block, boot, energy, feed, items, merge, seed_eu
from packaged_server import digest
from workshop_connected import advanced_lines, kinetic_routes, matter_lines, metal_lines, steam_lines, thermal_lines


def fuelled_line(server, z, name, mode, ingredient, count):
    source, cable, device = (f"{x} 158 {z}" for x in range(3))
    server.send(block(source, "ic2:generator"), block(cable, "ic2:insulated_copper_cable"),
                block(device, f"ic2:{name}"), merge(device, f"{{ic2_mode:{mode}}}"),
                *feed(source, "minecraft:coal", 1), *feed(device, ingredient, count))
    return device


def pumped_input(device, fluid, volume):
    x, y, z = map(int, device.split())
    pipe = f"{x+1} {y} {z}"
    tank = f"{x+2} {y} {z}"
    return (block(pipe, "ic2:bronze_fluid_pipe_medium"), block(tank, "ic2:bronze_tank"),
            merge(pipe, "{ic2_pump_mv:32}"),
            merge(tank, f'{{ic2_fluid:{{FluidName:"{fluid}",Amount:{volume}}}}}'))


def workshop(server):
    server.send("forceload add 0 0 16 16", "fill 0 157 0 12 157 15 minecraft:stone")
    extrusion = fuelled_line(server, 1, "metal_former", 0, "minecraft:copper_ingot", 1)
    plate = fuelled_line(server, 4, "metal_former", 1, "minecraft:copper_ingot", 1)
    cut = fuelled_line(server, 7, "metal_former", 2, "ic2:copper_plate", 1)

    washer = "2 158 10"
    server.send(block("0 158 10", "ic2:batbox[facing=east]"),
                block("1 158 10", "ic2:insulated_copper_cable"),
                block(washer, "ic2:ore_washing_plant"), seed_eu("0 158 10", 16000),
                *feed(washer, "ic2:crushed_tin", 1), *pumped_input(washer, "minecraft:water", 1000))

    canner = fuelled_line(server, 13, "canner", 3, "ic2:bio_chaff", 1)
    server.send(*pumped_input(canner, "minecraft:water", 1000))

    furnace = "9 158 1"
    heater = "8 158 1"
    server.send(block("6 158 1", "ic2:batbox[facing=east]"),
                block("7 158 1", "ic2:insulated_copper_cable"),
                block(heater, "ic2:electric_heat_generator[facing=east]"),
                block(furnace, "ic2:blast_furnace[facing=west]"),
                merge(heater, '{Items:[{Slot:4b,id:"ic2:coil",Count:10b}]}'),
                seed_eu("6 158 1", 32000), *feed(furnace, "minecraft:iron_ingot", 1),
                *pumped_input(furnace, "ic2:air", 6000))

    server.require("native_air_and_heat_started_steel", f"if block {furnace} ic2:blast_furnace[active=true]",
                   f"unless data block {furnace} {{ic2_progress:0}}", seconds=120)
    server.require("former_extrudes_three_copper_cables", items(extrusion, "ic2:copper_cable", 3, 9))
    server.require("former_rolls_one_copper_plate", items(plate, "ic2:copper_plate", 1, 9))
    server.require("former_cuts_two_copper_cables", items(cut, "ic2:copper_cable", 2, 9))
    for machine in (extrusion, plate, cut):
        server.require("former_paid_2000_eu_" + machine.split()[-1], energy(machine, 2000))
    server.require("native_canner_enriched_1000_mb",
                   f'if data block {canner} {{ic2_output_fluid:{{FluidName:"ic2:biomass",Amount:1000}}}}',
                   f"unless data block {canner} ic2_input_fluid")
    server.require("pumped_washer_made_three_products", items(washer, "ic2:purified_tin", 1, 9),
                   items(washer, "ic2:small_tin_dust", 2, 10), items(washer, "ic2:stone_dust", 1, 11),
                   f"unless data block {washer} ic2_input_fluid")
    server.require("heated_iron_completed_6000_ticks_to_steel", items(furnace, "ic2:steel_ingot", 1, 9),
                   items(furnace, "ic2:slag", 1, 10), f"unless data block {furnace} ic2_input_fluid",
                   seconds=380)
    server.state(heater, furnace, washer, canner)


BOTTLER = "320 158 320"
INPUT_BARREL = "316 158 320"
OUTPUT_BARREL = "323 158 320"
WATER_TANK = "320 158 323"
STORAGE = "320 158 317"
GENERATOR = "320 158 315"
AUTOMATION_CHUNKS = "304 304 336 336"
UNLOAD_MARKER = re.compile(r"IC2_END_UNLOAD_QUERY|not loaded")


def bottled_plant(server):
    server.send(
        "forceload add " + AUTOMATION_CHUNKS,
        "fill 315 157 314 324 157 324 minecraft:stone",
        block(BOTTLER, "ic2:fluid_bottler"),
        block(INPUT_BARREL, "minecraft:barrel"),
        block(OUTPUT_BARREL, "minecraft:barrel"),
        *(block(f"{x} 158 320", "ic2:bronze_item_pipe_small[facing=east]")
          for x in (317, 318, 319, 321, 322)),
        block(GENERATOR, "ic2:generator"),
        block("320 158 316", "ic2:insulated_copper_cable"),
        block(STORAGE, "ic2:batbox[facing=south]"),
        *(block(f"320 158 {z}", "ic2:insulated_copper_cable") for z in (318, 319)),
        *(block(f"320 158 {z}", "ic2:bronze_fluid_pipe_medium") for z in (321, 322)),
        block(WATER_TANK, "ic2:bronze_tank"),
        merge("320 158 322", "{ic2_pump_mv:8}"),
        merge(WATER_TANK, '{ic2_fluid:{FluidName:"minecraft:water",Amount:5000}}'),
        f"item replace block {INPUT_BARREL} container.0 with ic2:empty_cell 5",
    )
    inputs = (items(BOTTLER, "ic2:empty_cell", 5),
              f'if data block {BOTTLER} {{ic2_input_fluid:{{FluidName:"minecraft:water",Amount:5000}}}}',
              f"unless data block {INPUT_BARREL} Items[0]",
              f"unless data block {WATER_TANK} ic2_fluid")
    server.require("item_and_fluid_pipes_cross_chunk_boundaries", *inputs)
    server.send(seed_eu(STORAGE, 64))
    server.require("eu_cable_ran_32_ticks_and_stopped", energy(STORAGE, 0), energy(BOTTLER, 0),
                   f"if data block {BOTTLER} {{ic2_progress:32}}", *inputs)
    server.send("save-all flush", "forceload remove " + AUTOMATION_CHUNKS)
    if not server.wait("Unmarked", 10):
        raise AssertionError("Could not remove the test region's chunk tickets")
    deadline = time.monotonic() + 15
    pending = {BOTTLER, INPUT_BARREL}
    while pending and time.monotonic() < deadline:
        for pos in tuple(pending):
            server.send(f"data get block {pos}", "say IC2_END_UNLOAD_QUERY")
            unloaded = False
            while (line := server.wait(UNLOAD_MARKER, 1)) and "IC2_END_UNLOAD_QUERY" not in line:
                unloaded |= "not loaded" in line
            if unloaded:
                pending.remove(pos)
    if pending:
        raise AssertionError(f"Chunk unload did not happen at {pending}")
    server.checks.append("both_production_chunks_unloaded")


def resume_bottling(server):
    server.send("forceload add " + AUTOMATION_CHUNKS)
    server.require("bottler_preserved_partial_work_after_restart",
                   energy(BOTTLER, 0), f"if data block {BOTTLER} {{ic2_progress:32}}",
                   items(BOTTLER, "ic2:empty_cell", 5),
                   f'if data block {BOTTLER} {{ic2_input_fluid:{{FluidName:"minecraft:water",Amount:5000}}}}')
    server.send(*feed(GENERATOR, "minecraft:coal", 1))
    server.require("fuel_item_fluid_and_eu_line_exported_five_water_cells",
                   items(OUTPUT_BARREL, "ic2:water_cell", 5), f"unless data block {BOTTLER} Items[0]",
                   *(f"unless data block 320 158 {z} ic2_fluid" for z in (321, 322, 323)),
                   f"unless data block {BOTTLER} ic2_input_fluid")
    server.send("forceload add 0 0")
    drain = fuelled_line(server, 13, "fluid_bottler", 0, "ic2:water_cell", 2)
    dual = fuelled_line(server, 4, "induction_furnace", 0, "minecraft:raw_iron", 1)
    recycler = fuelled_line(server, 6, "recycler", 0, "minecraft:glass_pane", 1)
    server.require("recycler_received_blacklisted_feed", items(recycler, "minecraft:glass_pane", 1))
    server.send(
        merge(dual, '{Items:[{Slot:0b,id:"minecraft:raw_iron",Count:1b},'
                                                       '{Slot:1b,id:"minecraft:raw_iron",Count:1b}]}'),
        f"item replace block 2 159 4 container.0 with minecraft:air")
    server.require("bottler_reverse_cell_operation", items(drain, "ic2:empty_cell", 2, 9),
                   f'if data block {drain} {{ic2_input_fluid:{{FluidName:"minecraft:water",Amount:2000}}}}',
                   f"unless data block {drain} Items[{{Slot:1b}}]")
    server.require("induction_preserved_two_equal_output_lanes",
                   items(dual, "minecraft:iron_ingot", 1, 9), items(dual, "minecraft:iron_ingot", 1, 10))
    server.require("blacklisted_input_consumed_without_scrap",
                   energy(recycler, 180), f"unless data block {recycler} Items[0]",
                   f"if block {recycler} ic2:recycler[active=false]")
    server.state(BOTTLER, OUTPUT_BARREL, drain, dual, recycler)



def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ("jar", "libraries", "eula", "directory"):
        parser.add_argument("--" + key, type=Path, required=True)
    parser.add_argument("--java", default="java")
    profile = parser.add_mutually_exclusive_group()
    profile.add_argument("--automation", action="store_true")
    profile.add_argument("--thermal", action="store_true")
    profile.add_argument("--kinetic", action="store_true")
    profile.add_argument("--metal-lines", action="store_true")
    profile.add_argument("--advanced", action="store_true")
    profile.add_argument("--steam", action="store_true")
    profile.add_argument("--matter", action="store_true")
    args = parser.parse_args()
    target = args.directory.resolve()
    target.mkdir(parents=True, exist_ok=False)
    if "eula=true" not in args.eula.read_text().splitlines():
        parser.error("Supply an accepted server eula.txt")
    variant = "win_args.txt" if os.name == "nt" else "unix_args.txt"
    relative_args = Path("net/minecraftforge/forge/1.19.2-43.5.0") / variant
    if not (args.libraries / relative_args).is_file():
        parser.error("Provide the libraries of installed Forge 43.5.0")
    with zipfile.ZipFile(args.jar) as archive:
        if any("/forge/qa/" in name for name in archive.namelist()):
            parser.error("Provide only the distribution JAR")
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
    if args.automation:
        with boot(target, command, "bottling-before-save") as server:
            bottled_plant(server)
            checks = server.checks
        with boot(target, command, "bottling-after-restart") as server:
            resume_bottling(server)
            checks += server.checks
    else:
        with boot(target, command, "workshop") as server:
            if args.matter:
                matter_lines(server)
            elif args.advanced:
                advanced_lines(server)
            elif args.metal_lines:
                metal_lines(server)
            elif args.steam:
                steam_lines(server)
            elif args.thermal:
                thermal_lines(server)
            elif args.kinetic:
                kinetic_routes(server)
            else:
                workshop(server)
            checks = server.checks
    if digest(installed) != fingerprint:
        raise AssertionError("Installed JAR changed")
    report = {"jar": installed.name, "sha256": fingerprint, "checks": checks}
    (target / "result.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
