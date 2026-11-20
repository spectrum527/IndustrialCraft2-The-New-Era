"""Check original machine atlas pixels and native slots in one real Forge client."""

import argparse
from hashlib import sha256
from itertools import combinations
import json
import math
import os
import re
from pathlib import Path
from struct import unpack
import time

from physical_play import exercise, logs, until
from verify import MODULE, ROOT, write_json

POSITION = "29 90 25"
WORKSHOP_LAYOUTS = ROOT / "assets/ic2/gui/workshop-layouts.json"
TEXTURES = ROOT / "assets/ic2/textures/gui"
POWER_LAYOUTS = ROOT / "assets/ic2/gui/power-layouts.json"
ELECTROLYZER_WELLS = ((78, 16), (36, 61), (57, 61), (78, 61), (99, 61), (120, 61))


def initial_state(path):
    if path == "metal_former":
        return ('{ic2_energy:3500.0d,ic2_mode:1,Items:['
                '{Slot:0b,id:"minecraft:copper_ingot",Count:2b},'
                '{Slot:18b,id:"ic2:overclocker_upgrade",Count:1b}]}')
    if path == "ore_washing_plant":
        return ('{ic2_energy:16000.0d,ic2_input_fluid:{FluidName:"minecraft:water",Amount:1000},'
                'Items:[{Slot:0b,id:"ic2:crushed_tin",Count:2b},'
                '{Slot:6b,id:"minecraft:water_bucket",Count:1b}]}')
    if path == "semifluid_generator":
        return '{ic2_energy:350.0d}'
    if path == "pump":
        return '{ic2_input_fluid:{FluidName:"minecraft:water",Amount:1000}}'
    return None


def framebuffer_pixel(panel, x, y):
    zoom = panel.get("panelScale", 1)
    scale = panel["guiScale"]
    return (int((panel["left"] + (x + 0.5) * zoom) * scale),
            int((panel["top"] + (y + 0.5) * zoom) * scale))


def assert_artwork(file, panel, artwork, limit, layout, strict):
    """Compare original upper-screen atlas pixels at the documented native scale."""
    from PIL import Image

    with Image.open(file) as screenshot, Image.open(artwork) as atlas:
        source = atlas.convert("RGBA")
        screen = screenshot.convert("RGB")
        width = layout["panel"][0]
        same = protected = protected_matches = 0
        sampled = 0
        masks = [field["rect"] for key in ("controls", "draw", "meters", "labels")
                 for field in layout.get(key, [])]
        if artwork.name == "guielectrolyzer.png":
            masks.extend((x, y, 18, 18) for x, y in ELECTROLYZER_WELLS)
        for index in layout.get("frames", []):
            x, y = layout["slots"][str(index)]
            masks.append((x-1, y-1, 18, 18))
        if layout.get("inventoryTitle") and layout.get("inventory"):
            masks.append((0, layout["inventory"][1]-11, 70, 11))
        for native in panel["slots"]:
            if native["index"] < 27 and native.get("count", 0) > 0:
                masks.append((native["x"]-2, native["y"]-2, 22, 21))
        for y in range(16, limit):
            for x in range(5, width - 5):
                rgba = source.getpixel((x, y))
                if rgba[3] != 255:
                    continue
                actual = screen.getpixel(framebuffer_pixel(panel, x, y))
                sampled += 1
                is_exact = actual == rgba[:3]
                same += is_exact
                if not any(px <= x < px + width and py <= y < py + height
                           for px, py, width, height in masks):
                    protected += 1
                    protected_matches += is_exact
    if not protected or protected_matches / protected < 0.83:
        raise AssertionError(f"{artwork.name} authored machine body matched {same}/{sampled} pixels")
    if strict and protected_matches != protected:
        raise AssertionError(f"{artwork.name} original areas matched {protected_matches}/{protected} pixels")
    return {"compared": sampled, "identical": same, "fraction": same / sampled,
            "staticCompared": protected, "staticExact": protected_matches}


def water_well(file, panel):
    """A filled tank's native water texture must be visible inside the authored 16px well."""
    from PIL import Image

    pixels = 0
    with Image.open(file) as screenshot:
        screen = screenshot.convert("RGB")
        for y in range(16):
            for x in range(16):
                red, green, blue = screen.getpixel(framebuffer_pixel(panel, 80 + x, 36 + y))
                pixels += blue - red > 40 and blue - green > 25
    if pixels < 16 * 16 * 3 // 4:
        raise AssertionError(f"Native water well contained only {pixels}/256 blue water pixels")
    return {"blueLiquidPixels": pixels, "sampledPixels": 16 * 16}


def electrolyzer_wells(file, panel):
    """Verify all six empty native fluid wells against their 18px frame sprite."""
    from PIL import Image

    results = []
    with Image.open(file) as screenshot, Image.open(TEXTURES / "common.png") as atlas:
        screen = screenshot.convert("RGB")
        source = atlas.convert("RGBA")
        for x, y in ELECTROLYZER_WELLS:
            compared = identical = 0
            for dy in range(18):
                for dx in range(18):
                    expected = source.getpixel((8 + dx, 160 + dy))
                    if expected[3] != 255:
                        continue
                    compared += 1
                    identical += screen.getpixel(framebuffer_pixel(panel, x + dx, y + dy)) == expected[:3]
            if not compared or identical != compared:
                raise AssertionError(f"electrolyzer well at {x},{y} matched {identical}/{compared} pixels")
            results.append({"at": [x, y], "compared": compared, "identical": identical})
    return {"atlas": "common.png", "source": [8, 160, 18, 18], "wells": results}


def reactor_tiles(file, panel, layout):
    """Compare each of the 36 blocked slots to the independently authored 18px overlay."""
    from PIL import Image

    slots = layout["slots"]
    exposed = {entry["index"] for entry in panel["slots"]}
    closed = [(slots[str(index)][0] - 1, slots[str(index)][1] - 1)
              for index in range(54) if index not in exposed]
    same = compared = 0
    with Image.open(file) as screenshot, Image.open(TEXTURES / "guinuclearreactor.png") as atlas:
        actual = screenshot.convert("RGB")
        ink = atlas.convert("RGBA")
        for x, y in closed:
            for sy in range(18):
                for sx in range(18):
                    expected = ink.getpixel((212 + sx, sy))
                    if expected[3] != 255:
                        continue
                    same += actual.getpixel(framebuffer_pixel(panel, x + sx, y + sy)) == expected[:3]
                    compared += 1
    if len(closed) != 36 or same != compared:
        raise AssertionError(f"reactor authored closed-chamber overlays matched {same}/{compared}")
    return {"closedSlots": len(closed), "compared": compared, "identical": same}


def complete_png(file):
    if not file.is_file() or file.stat().st_size < 12:
        return False
    with file.open("rb") as source:
        source.seek(-12, 2)
        return source.read() == bytes.fromhex("0000000049454e44ae426082")


def capture(client, name):
    client.act("move-ui:-15,-15")
    client.act("state")
    for attempt in range(3):
        key = name if attempt == 0 else f"{name}_frame_{attempt + 1}"
        frame = client.act("snapshot-ui:" + key)["uiPanel"]
        file = client.directory / f"screenshots/authored-{key}.png"
        until(lambda: complete_png(file), f"Native screenshot of {key}", client.run.client, timeout=20)
        with file.open("rb") as png:
            png.seek(16)
            width, height = unpack(">II", png.read(8))
        if (width, height) == (frame["framebufferWidth"], frame["framebufferHeight"]):
            return file, frame
    raise AssertionError(f"Client framebuffer kept resizing while capturing {name}")


def validate_slot_rectangles(panel, name):
    for first, second in combinations(panel["slots"], 2):
        if abs(first["x"] - second["x"]) < 16 and abs(first["y"] - second["y"]) < 16:
            raise AssertionError(f"{name} native slots {first['index']} and {second['index']} overlap")


def item_matches(actual, expected):
    return (actual.get("item") == expected["item"]
            and actual.get("count") == expected.get("count", 1)
            and expected.get("tag", "") in actual.get("tag", ""))


def owner_clicks(client, item, corner):
    client.run.command(f"item replace entity P2Tester hotbar.1 with {item}")
    client.check("prepared-native-hand-" + item.split("{")[0], lambda state: any(
        slot["x"] == 26 and slot["y"] == 142 and slot["item"] == item.split("{")[0]
        for slot in state.get("uiPanel", {}).get("slots", [])), timeout=8)
    client.act("click-ui:34,150")
    client.act(f"click-ui:{corner[0]+8},{corner[1]+8}")


def energize_workshop(client, path, layout):
    item, initial, final, delta = {
        "metal_former": ("ic2:re_battery", 300, 0, 300),
        "induction_furnace": ("ic2:advanced_re_battery", 250, 0, 250),
        "semifluid_generator": ("ic2:re_battery", 9980, 10000, -20),
    }[path]
    corner = layout["slots"]["26"]
    before = client.act("state")["workshopEnergy"]
    owner_clicks(client, f"{item}{{ic2_energy:{initial}.0d}}", corner)

    def matches(state):
        slots = state.get("uiPanel", {}).get("slots", [])
        battery = next((slot for slot in slots if slot["index"] == 26), {})
        charge = re.search(r"ic2_energy:([0-9.]+)d", battery.get("tag", ""))
        return (state.get("workshopEnergy") == before + delta
                and item_matches(battery, {"item": item})
                and (float(charge[1]) if charge else 0) == final)

    client.check(path + "-real-battery-exchanged-exact-eu", matches, timeout=8)
    file, frame = capture(client, path + "_battery")
    return {"item": item, "slot": corner, "itemBefore": initial, "itemAfter": final,
            "machineBefore": before, "machineAfter": before + delta,
            "png": str(file), "geometry": frame}


def click_steps(client, name, steps):
    outputs = []
    for step in steps:
        click = step.get("click")
        clicks = [click] if click else step.get("clicks", [])
        for x, y in clicks:
            client.act(f"click-ui:{x},{y}")
        visible = set(step.get("visible", ()))
        hidden = set(step.get("hidden", ()))
        check = step.get("state", {})
        cells = step.get("items", [])
        retained = step.get("retained", [])
        if step.get("reopen"):
            client.act("esc")
            client.check(name + "-closed-then-reopen", lambda state: not state["screen"], timeout=10)
            client.act("ui-item:" + name)

        def matches(state):
            slots = state.get("uiPanel", {}).get("slots", [])
            ids = {slot["index"] for slot in slots}
            items = all(any([slot["x"], slot["y"]] == expected["at"] and item_matches(slot, expected)
                            for slot in slots) for expected in cells)
            inventory = all(any(item_matches(stack, expected)
                                    for stack in state.get("uiInventory", []))
                            for expected in retained)
            return (bool(slots) and items and inventory and visible <= ids and not hidden.intersection(ids)
                    and all(state.get(key) == value for key, value in check.items()))

        label = step["name"]
        client.check(name + "-" + label, matches, timeout=10)
        png, frame = capture(client, name + "_" + label)
        validate_slot_rectangles(frame, name + "-" + label)
        outputs.append({"geometry": frame, "png": str(png)})
    return outputs


def prepare_block(client, path, key, device):
    session = client.run
    target = "ic2:" + device.get("target", path).removeprefix("ic2:")
    session.command(f"setblock {POSITION} minecraft:air")
    session.command("kill @e[type=minecraft:item,x=29,y=90,z=25,distance=..5]")
    session.command("clear P2Tester")
    session.command(f"setblock {POSITION} {target}{device.get('state', '')}")
    payload = device.get("nbt", initial_state(path))
    if payload:
        session.command(f"data merge block {POSITION} {payload}")
    for command in device.get("setup", []):
        if path == "nuclear_reactor_fluid":
            def translate(m):
                return " ".join(str(int(n) + d) for n, d in zip(m.groups(), (25, -10, 27)))
            command = re.sub(r"(-?\d+) (-?\d+) (-?\d+)", translate, command)
        session.command(command.replace("{target}", POSITION))
    if device.get("kind") == "item":
        held = key
    else:
        held = device.get("held", "minecraft:stick" if path == "manual_kinetic_generator" else None)
    if held:
        session.command(f"item replace entity P2Tester weapon.mainhand with {held}")
        client.check("held-author-" + path, lambda state: state.get("uiHeld") == held, timeout=15)
    client.check("placed-author-" + path, lambda state: state.get("uiTarget") == target, timeout=15)
    if path == "tank":
        session.command("item replace entity P2Tester weapon.mainhand with minecraft:water_bucket")
        client.check("tank-native-filled-bucket-held",
                     lambda state: state.get("uiHeld") == "minecraft:water_bucket", timeout=12)
        client.act("ui:" + path)
        client.check("tank-native-bucket-emptied",
                     lambda state: state.get("uiHeld") == "minecraft:bucket", timeout=12)
        session.command(f'execute if data block {POSITION} {{ic2_fluid:{{FluidName:"minecraft:water",Amount:1000}}}}'
                        ' run say IC2_AUTHORED_TANK_1000_MB')
        until(lambda: "IC2_AUTHORED_TANK_1000_MB" in logs(session.directory, "server"),
              "1000 mB from the real bucket persisted in the native tank", session.server, timeout=12)
        session.command("item replace entity P2Tester weapon.mainhand with minecraft:air")
        client.check("tank-native-now-open-empty-hand",
                     lambda state: state.get("uiHeld") == "minecraft:air", timeout=12)
    client.act("ui:" + path)


def verify_panel_size(path, raw_size, panel):
    zoom = panel.get("panelScale", 1)
    if "panelScale" in panel:
        available = (panel["viewportWidth"], panel["viewportHeight"])
        expected_zoom = min(1, *(max(1, viewport - 4) / size
                                 for viewport, size in zip(available, raw_size)))
        if not math.isclose(zoom, expected_zoom, abs_tol=1e-6):
            raise AssertionError(f"{path} panel scale {zoom}, expected {expected_zoom}")
        if (panel["left"] < 0 or panel["top"] < 0
                or panel["left"] + panel["width"] > available[0]
                or panel["top"] + panel["height"] > available[1]):
            raise AssertionError(f"{path} panel exceeds the viewport: {panel}")
    expected = [math.ceil(size * zoom - 1e-5) for size in raw_size]
    if [panel["width"], panel["height"]] != expected:
        raise AssertionError(f"{path} dimensions differ from {expected}")


def verify_workshop(path, layout, geometry):
    if geometry.get("device") != path or geometry.get("atlas") != layout["atlas"]:
        raise AssertionError(f"{path} opened a different Workshop atlas")
    verify_panel_size(path, layout["panel"], geometry)
    expected = layout["slots"]
    machine = {str(slot["index"]): [slot["x"], slot["y"]]
               for slot in geometry["slots"] if slot["index"] < 27}
    if path == "uu_scanner":
        expected = {key: value for key, value in expected.items() if key != "14"}
    elif path == "pattern_storage":
        expected = {key: expected[key] for key in ("14",)}
    if machine != expected:
        raise AssertionError(f"{path} visible slots {machine} differ from {expected}")


def verify_other(path, device, panel):
    if "size" in device:
        verify_panel_size(path, device["size"], panel)
    if "slots" in device and len(panel["slots"]) != device["slots"]:
        raise AssertionError(f"{path} expected {device['slots']} slots, found {len(panel['slots'])}")
    actual = {(slot["x"], slot["y"]) for slot in panel["slots"]}
    if any(tuple(point) not in actual for point in device.get("anchors", [])):
        raise AssertionError(f"{path} does not show all specified slot anchors")


def check_chunk_grid(client):
    session = client.run
    log = session.directory / "server.log"
    selected = []
    for cell in (27, 28, 80):
        row, column = divmod(cell, 9)
        marker = f"IC2_CHUNK_SELECTED_{cell}"
        query = f"{{chunk_x:{column + 1}.0d,chunk_z:{row + 1}.0d}}"
        client.act(f"click-ui:{32 + column * 16},{23 + row * 16}")
        with log.open("r", encoding="utf-8", errors="replace") as stream:
            stream.seek(0, 2)

            def selected_on_server():
                session.command(f"execute if data block {POSITION} {query} run say {marker}")
                return marker in stream.read()

            until(selected_on_server, marker, session.server, timeout=10)
        selected.append(cell)
    return selected


def inspect(client, device, layouts):
    session = client.run
    path = device["id"].removeprefix("ic2:")
    key = "ic2:" + path
    if device.get("kind", "block") == "block" or "target" in device:
        prepare_block(client, path, key, device)
    else:
        session.command("clear P2Tester")
        for command in device.get("setup", []):
            session.command(command.replace("{player}", "P2Tester"))
        if not device.get("retainedMainHand"):
            session.command(f"item replace entity P2Tester weapon.mainhand with {key}")
        client.check("held-author-" + path, lambda state: state.get("uiHeld") == key, timeout=15)
        client.act("ui-item:" + path)
    screen_name = device.get("screen")
    state = client.check(
        "opened-author-" + path,
        lambda state: state.get("uiPanel") and (not screen_name or state["screen"] == screen_name),
        timeout=18,
    )
    if device.get("items"):
        state = client.check(path + "-scan-or-inventory-output", lambda state: all(
            any([slot["x"], slot["y"]] == item["at"] and item_matches(slot, item)
                for slot in state.get("uiPanel", {}).get("slots", []))
            for item in device["items"]), timeout=10)
    file, geometry = capture(client, path)
    validate_slot_rectangles(geometry, path)
    layout = layouts.get(path, {"atlas": device.get("atlas"), "inventory": device.get("inventory"),
                                "panel": device.get("size", [geometry["width"], geometry["height"]])})
    closed_cells = None
    if path in ("nuclear_reactor", "nuclear_reactor_chambers"):
        core_layout = json.loads(POWER_LAYOUTS.read_text())["nuclear_reactor"]
        layout["inventory"] = core_layout["inventory"]
        if path == "nuclear_reactor":
            layout["draw"] = [{"rect": [79, 24, 108, 108]}]
            closed_cells = reactor_tiles(file, geometry, core_layout)
    if path in layouts:
        verify_workshop(path, layout, geometry)
    else:
        verify_other(path, device, geometry)
    atlas = layout["atlas"]
    pixel_audit = None
    if atlas not in (None, "common") and not device.get("composed", False):
        resource = TEXTURES / f"{atlas}.png"
        if "textureSha256" in device and sha256(resource.read_bytes()).hexdigest() != device["textureSha256"]:
            raise AssertionError(f"{path} changed its authored atlas")
        upper = layout["inventory"][1] - 3 if layout.get("inventory") else layout["panel"][1] - 4
        pixel_audit = assert_artwork(file, geometry, resource, upper, layout,
                                     path in ("metal_former", "ore_washing_plant"))
    result = {"png": str(file), "screen": state["screen"], "title": state["title"],
              "geometry": geometry, "atlas": atlas, "pixels": pixel_audit}
    if path == "chunk_loader":
        result["selectedCells"] = check_chunk_grid(client)
    if closed_cells is not None:
        result["closedChamberPixels"] = closed_cells
    if path == "tank":
        result["nativeFluidPixels"] = water_well(file, geometry)
    if path == "electrolyzer":
        result["nativeFluidWells"] = electrolyzer_wells(file, geometry)
    actions = list(device.get("steps", []))
    for click in device.get("clicks", []):
        number = len(actions) + 1
        action = {"click": click["at"], "name": "click" + str(number)}
        if "field" in click:
            action["state"] = {click["field"]: click["value"]}
        if "snapshot" in click:
            action["name"] = click["snapshot"].removeprefix(path + "_")
        actions.append(action)
    if actions:
        result["steps"] = click_steps(client, path, actions)
    if path == "metal_former":
        client.act("click-ui:74,63")
        language = os.environ.get("IC2_QA_LANGUAGE", "en_us")
        locale = json.loads((ROOT / "assets/ic2/lang" / f"{language}.json").read_text(encoding="utf-8"))
        cutting = locale["gui.ic2.workshop.metal_former.2"]
        client.check("authored-metal-mode-hotspot-works",
                     lambda state: state.get("workshopMode") == cutting, timeout=8)
        client.act("click-ui:160,16")
        captured = {"item": "ic2:overclocker_upgrade", "count": 1}
        client.check("authored-metal-upgrade-first-slot-can-pick-up",
                     lambda state: item_matches(state.get("uiPanel", {}).get("carried", {}), captured),
                     timeout=8)
        # Vanilla treats two presses on one slot within 250 ms as a double-click.
        time.sleep(0.3)
        client.act("click-ui:160,16")
        client.check("authored-metal-upgrade-first-slot-can-replace",
                     lambda state: any(slot["index"] == 18 and item_matches(slot, captured)
                                       for slot in state.get("uiPanel", {}).get("slots", [])),
                     timeout=8)
    if path == "ore_washing_plant":
        client.check("authored-washer-water-slot-worked",
                     lambda state: state.get("workshopFluid") >= 1000 and any(
                         slot["index"] == 14 and slot["item"] == "minecraft:bucket"
                         for slot in state.get("uiPanel", {}).get("slots", [])), timeout=8)
    if path in ("metal_former", "induction_furnace", "semifluid_generator"):
        result["battery"] = energize_workshop(client, path, layout)
    if path == "pump":
        owner_clicks(client, "minecraft:bucket", layout["slots"]["0"])
        client.check("authored-pump-stored-water-filled-real-container",
                     lambda state: state.get("workshopFluid") == 0 and any(
                         slot["index"] == 9 and slot["item"] == "minecraft:water_bucket"
                         for slot in state.get("uiPanel", {}).get("slots", [])), timeout=8)
        file, frame = capture(client, path + "_container")
        result["container"] = {"input": "minecraft:bucket", "consumedWaterMb": 1000,
                               "output": "minecraft:water_bucket",
                               "png": str(file), "geometry": frame}
    return result


def audit(client, devices, layouts):
    session = client.run
    client.enter("connect", "authored-client-joined")
    for command in ("gamerule sendCommandFeedback false", "time set noon",
                    "fill 20 89 20 31 89 26 minecraft:stone", "tp P2Tester 29.5 90 23.5 0 15"):
        session.command(command)
    results = {}
    errors = {}
    for device in devices:
        path = device["id"].removeprefix("ic2:")
        # Preserve case isolation after temporarily equipping radioactive cargo.
        session.command("effect clear P2Tester")
        try:
            results[path] = inspect(client, device, layouts)
        except (AssertionError, TimeoutError, RuntimeError) as problem:
            errors[path] = str(problem)
        finally:
            current = client.act("state")
            if path in ("nuclear_reactor_fluid", "nuclear_reactor_chambers"):
                session.command("fill 27 88 24 31 92 29 minecraft:air")
                session.command("fill 27 89 24 31 89 26 minecraft:stone")
            if not current["world"]:
                client.enter("connect", "recovered-author-client-after-" + path)
                session.command("tp P2Tester 29.5 90 23.5 0 15")
            elif current["screen"]:
                client.act("esc")
                client.check("closed-author-" + path, lambda state: not state["screen"], timeout=10)
        if path in results and "onCloseItem" in device:
            def restored(state):
                returned = any(item_matches(stack, device["onCloseItem"])
                               for stack in state.get("uiInventory", []))
                return returned and state.get("uiHeldEu", 1000) < device.get("priorEu", 1000)

            try:
                client.check(path + "-returned-analysis-on-close", restored, timeout=10)
            except AssertionError as problem:
                errors[path] = str(problem)
                del results[path]
        write_json(session.directory / "authored-screens.json", results)
        write_json(session.directory / "authored-errors.json", errors)
    return {"authoredScreens": len(results), "nativeGeometry": results, "errors": errors}


def load_devices(file):
    content = json.loads(file.read_text())
    if isinstance(content, list):
        for row in content:
            path = row["id"].removeprefix("ic2:")
            if path in ("scanner", "advanced_scanner"):
                row["setup"] = [
                    f"item replace entity P2Tester weapon.mainhand with ic2:{path}{{ic2_energy:1000.0d}}",
                    "setblock 31 90 24 minecraft:diamond_ore",
                    "setblock 34 90 23 minecraft:emerald_ore",
                ]
                row["retainedMainHand"] = True
        if any(row["id"] == "nuclear_reactor" for row in content):
            # The real north chamber of an uncovered reactor is the visible interaction face.
            content.insert(1, {
                "id": "nuclear_reactor_chambers", "kind": "block", "target": "reactor_chamber",
                "screen": "PowerScreen", "atlas": "guinuclearreactor", "size": [212,242], "slots": 90,
                "anchors": [[26,25], [170,25], [170,115], [26,161]],
                "setup": [
                    "setblock 29 90 26 ic2:nuclear_reactor",
                    "setblock 28 90 26 ic2:reactor_chamber",
                    "setblock 30 90 26 ic2:reactor_chamber",
                    "setblock 29 89 26 ic2:reactor_chamber",
                    "setblock 29 91 26 ic2:reactor_chamber",
                    "setblock 29 90 27 ic2:reactor_chamber",
                    "item replace entity P2Tester hotbar.1 with ic2:heat_vent",
                ],
                "steps": [{"name": "last_chamber_accepted", "clicks": [[52,227],[178,123]],
                           "items": [{"at":[170,115],"item":"ic2:heat_vent"}]}],
            })
        return content
    results = []
    for raw in content["panels"]:
        path = raw["id"].removeprefix("ic2:")
        x, y = raw["playerInventoryOrigin"]
        hotbar_y = raw["hotbarOrigin"][1]
        row = {"id": path, "kind": raw["kind"], "atlas": Path(raw["texture"]).stem,
               "size": raw["nativeBounds"][2:], "inventory": [x, y, hotbar_y],
               "anchors": [[x, y], [x, hotbar_y]], "textureSha256": raw["textureSha256"]}
        if path in ("tool_box", "containment_box"):
            item = "minecraft:iron_pickaxe" if path == "tool_box" else "ic2:uranium_235"
            count = 1 if path == "tool_box" else 3
            corner = [8, 41] if path == "tool_box" else [53, 19]
            expected = {"at": corner, "item": item, "count": count}
            row["setup"] = [f"item replace entity P2Tester hotbar.1 with {item} {count}"]
            row["steps"] = [{"name": "inserted", "clicks": [[34, 150], [corner[0]+8, corner[1]+8]],
                             "items": [expected]},
                            {"name": "reopened", "reopen": True, "items": [expected]}]
        if path in ("advanced_ejector_upgrade", "advanced_pulling_upgrade"):
            sample = {"at":[98, 21], "item":"minecraft:diamond"}
            gemstone = {"item":"minecraft:diamond"}
            persisted = {"item": "ic2:" + path, "tag":"v2;down;true;true;true;"}
            row["setup"] = ["item replace entity P2Tester hotbar.1 with minecraft:diamond"]
            row["steps"] = [
                {"name": "filter", "clicks":[[34, 150], [106, 29], [34, 150]],
                 "items": [sample], "retained": [gemstone]},
                {"name": "settings", "clicks":[[26, 27], [26, 49], [26, 69], [88, 70]],
                 "items": [sample], "retained": [gemstone, persisted]},
                {"name": "reopened", "reopen": True, "items": [sample], "retained":[gemstone, persisted]},
            ]
        if path == "cropnalyzer":
            seed = 'ic2:crop_seed_bag{ic2_crop_genome:"potato,1,1,1,0"}'
            row["setup"] = ["item replace entity P2Tester weapon.mainhand with ic2:cropnalyzer{ic2_energy:1000.0d}",
                            f"item replace entity P2Tester hotbar.1 with {seed}"]
            row["retainedMainHand"] = True
            expected = {"at": [41, 7], "item": "ic2:crop_seed_bag", "tag": "potato,1,1,1,1"}
            row["steps"] = [{"name": "analyzed", "clicks": [[34, 207], [16, 15], [32, 15]],
                             "items": [expected]}]
            row["onCloseItem"] = expected
        if path == "sorting_machine":
            row["steps"] = [{"name": "page2", "click": [26, 9], "visible": list(range(9,25)),
                             "hidden": list(range(9))}]
        if path == "weighted_item_distributor":
            row["steps"] = [{"name": "page2", "click": [26, 9], "visible": list(range(9,18)),
                             "hidden": list(range(9))},
                            {"name": "page3", "click": [41, 9], "visible": list(range(18,24)) + [25],
                             "hidden": list(range(18))}]
        results.append(row)
    return results


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--spec", type=Path, action="append", default=[],
                        help="Device setups or the authored world/portable panel manifest")
    parser.add_argument("--include-workshop", action="store_true", help="Add all Workshop screens")
    parser.add_argument("--only", action="append", help="Select a device; may be repeated")
    args = parser.parse_args()
    path = args.output.resolve()
    path.mkdir(parents=True)
    layouts = json.loads(WORKSHOP_LAYOUTS.read_text())["machines"]
    devices = []
    if args.include_workshop or not args.spec:
        devices.extend({"id": name, "screen": "WorkshopScreen"} for name in layouts)
    for spec in args.spec:
        devices.extend(load_devices(spec))
    by_id = {}
    for device in devices:
        current = by_id.setdefault(device["id"].removeprefix("ic2:"), {})
        for key, value in device.items():
            current.setdefault(key, value)
    devices = list(by_id.values())
    if args.only:
        devices = [device for device in devices if device["id"].removeprefix("ic2:") in args.only]
    if not devices:
        parser.error("Select at least one authored screen")
    launch = json.loads((MODULE / "build/qa-play/launch.json").read_text())
    result = exercise(path, launch, profile=lambda client: audit(client, devices, layouts))
    write_json(path / "result.json", result)
    print(f"Passed {result['authoredScreens']} real client screens")
    if result["errors"]:
        raise SystemExit(f"{len(result['errors'])} screens require inspection: {', '.join(result['errors'])}")


if __name__ == "__main__":
    main()
