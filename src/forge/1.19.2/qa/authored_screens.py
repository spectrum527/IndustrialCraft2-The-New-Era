"""Check original machine atlas pixels and native slots in one real Forge client."""

import argparse
from hashlib import sha256
import json
import re
from pathlib import Path
import time

from physical_play import exercise, logs, until
from verify import MODULE, ROOT, write_json

POSITION = "29 90 25"
WORKSHOP_LAYOUTS = ROOT / "assets/ic2/gui/workshop-layouts.json"
TEXTURES = ROOT / "assets/ic2/textures/gui"
POWER_LAYOUTS = ROOT / "assets/ic2/gui/power-layouts.json"


def initial_state(path):
    if path == "metal_former":
        return ('{ic2_energy:3500.0d,ic2_mode:1,Items:['
                '{Slot:0b,id:"minecraft:copper_ingot",Count:2b},'
                '{Slot:18b,id:"ic2:overclocker_upgrade",Count:1b}]}')
    if path == "ore_washing_plant":
        return ('{ic2_energy:16000.0d,ic2_input_fluid:{FluidName:"minecraft:water",Amount:1000},'
                'Items:[{Slot:0b,id:"ic2:crushed_tin",Count:2b},'
                '{Slot:6b,id:"minecraft:water_bucket",Count:1b}]}')
    return None


def assert_artwork(file, panel, artwork, limit, layout, strict):
    """Compare original upper-screen atlas pixels at the documented native scale."""
    from PIL import Image

    with Image.open(file) as screenshot, Image.open(artwork) as atlas:
        source = atlas.convert("RGBA")
        screen = screenshot.convert("RGB")
        scale = panel["guiScale"]
        left, top = panel["left"], panel["top"]
        same = protected = protected_matches = 0
        sampled = 0
        masks = [field["rect"] for key in ("controls", "draw", "meters", "labels")
                 for field in layout.get(key, [])]
        for index in layout.get("frames", []):
            x, y = layout["slots"][str(index)]
            masks.append((x-1, y-1, 18, 18))
        if layout.get("inventoryTitle") and layout.get("inventory"):
            masks.append((0, layout["inventory"][1]-11, 70, 11))
        for native in panel["slots"]:
            if native["index"] < 27 and native.get("count", 0) > 0:
                masks.append((native["x"]-2, native["y"]-2, 22, 21))
        for y in range(16, limit):
            for x in range(5, panel["width"] - 5):
                rgba = source.getpixel((x, y))
                if rgba[3] != 255:
                    continue
                actual = screen.getpixel((int((left + x + 0.5) * scale),
                                          int((top + y + 0.5) * scale)))
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


def reactor_tiles(file, panel, layout):
    """Compare each of the 36 blocked slots to the independently authored 18px overlay."""
    from PIL import Image

    slots = layout["slots"]
    exposed = {entry["index"] for entry in panel["slots"]}
    closed = [(slots[str(index)][0] - 1, slots[str(index)][1] - 1)
              for index in range(54) if index not in exposed]
    same = compared = 0
    scale = panel["guiScale"]
    with Image.open(file) as screenshot, Image.open(TEXTURES / "guinuclearreactor.png") as atlas:
        actual = screenshot.convert("RGB")
        ink = atlas.convert("RGBA")
        for x, y in closed:
            for sy in range(18):
                for sx in range(18):
                    expected = ink.getpixel((212 + sx, sy))
                    if expected[3] != 255:
                        continue
                    px = int((panel["left"] + x + sx + 0.5) * scale)
                    py = int((panel["top"] + y + sy + 0.5) * scale)
                    same += actual.getpixel((px, py)) == expected[:3]
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
    client.act("snapshot-ui:" + name)
    file = client.directory / f"screenshots/authored-{name}.png"
    until(lambda: complete_png(file), f"Native screenshot of {name}", client.run.client, timeout=20)
    return file


def item_matches(actual, expected):
    return (actual.get("item") == expected["item"]
            and actual.get("count") == expected.get("count", 1)
            and expected.get("tag", "") in actual.get("tag", ""))


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
        current = client.check(name + "-" + label, matches, timeout=10)
        png = capture(client, name + "_" + label)
        outputs.append({"geometry": current["uiPanel"], "png": str(png)})
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


def verify_workshop(path, layout, geometry):
    if geometry.get("device") != path or geometry.get("atlas") != layout["atlas"]:
        raise AssertionError(f"{path} opened a different Workshop atlas")
    if [geometry["width"], geometry["height"]] != layout["panel"]:
        raise AssertionError(f"{path} original panel dimensions changed")
    expected = layout["slots"]
    machine = {str(slot["index"]): [slot["x"], slot["y"]]
               for slot in geometry["slots"] if slot["index"] < 27}
    if path == "uu_scanner":
        expected = {key: expected[key] for key in ("0",)}
    elif path == "pattern_storage":
        expected = {key: expected[key] for key in ("14",)}
    if machine != expected:
        raise AssertionError(f"{path} visible slots {machine} differ from {expected}")


def verify_other(path, device, panel):
    if "size" in device and [panel["width"], panel["height"]] != device["size"]:
        raise AssertionError(f"{path} dimensions differ from {device['size']}")
    if "slots" in device and len(panel["slots"]) != device["slots"]:
        raise AssertionError(f"{path} expected {device['slots']} slots, found {len(panel['slots'])}")
    actual = {(slot["x"], slot["y"]) for slot in panel["slots"]}
    if any(tuple(point) not in actual for point in device.get("anchors", [])):
        raise AssertionError(f"{path} does not show all specified slot anchors")


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
    geometry = state["uiPanel"]
    layout = layouts.get(path, {"atlas": device.get("atlas"), "inventory": device.get("inventory")})
    file = capture(client, path)
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
        upper = layout["inventory"][1] - 3 if layout.get("inventory") else geometry["height"] - 4
        pixel_audit = assert_artwork(file, geometry, resource, upper, layout,
                                     path in ("metal_former", "ore_washing_plant"))
    result = {"png": str(file), "screen": state["screen"], "title": state["title"],
              "geometry": geometry, "atlas": atlas, "pixels": pixel_audit}
    if closed_cells is not None:
        result["closedChamberPixels"] = closed_cells
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
        client.check("authored-metal-mode-hotspot-works",
                     lambda state: state.get("workshopMode") == "Cutting", timeout=8)
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
