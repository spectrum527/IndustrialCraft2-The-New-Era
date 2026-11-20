"""Exercise magnetic lift at normal speed with a physical survival client and a dedicated server."""

import re
import time


def check_magnetizer(client, command, server_log, height_key="height"):
    floor = 151
    location = "38 150 34"
    player = "@p"

    def read_number(query, pattern):
        with server_log.open(encoding="utf-8", errors="replace") as stream:
            stream.seek(0, 2)
            command(query)
            deadline = time.monotonic() + 15
            while time.monotonic() < deadline:
                for line in stream:
                    match = re.search(pattern, line)
                    if match:
                        return float(match[1])
                time.sleep(0.05)
        raise AssertionError(f"No server reply for {query}")

    def server_height():
        return read_number(f"data get entity {player} Pos[1]", r"entity data: ([-\d.]+)d")

    def energy():
        return read_number(f"data get block {location} eu", r"block data: ([-\d.]+)d")

    for instruction in ("forceload add 32 32 47 47", "gamemode survival " + player,
                        f"item replace entity {player} armor.feet with minecraft:air",
                        "fill 36 149 32 40 149 36 minecraft:stone",
                        f"setblock {location} ic2:magnetizer",
                        "fill 39 150 34 39 163 34 ic2:iron_fence",
                        f"tp {player} 38.5 {floor} 34.5"):
        command(instruction)
    client.check("magnetizer-standing", lambda state: abs(state.get(height_key, 0) - floor) < 0.02, 15)

    for label, boots, charge in (("bare", "air", 500), ("unpowered", "iron_boots", 0)):
        command(f"data merge block {location} {{eu:{charge}.0d}}")
        command(f"item replace entity {player} armor.feet with minecraft:{boots}")
        time.sleep(0.6)
        client.check("magnetizer-" + label, lambda state: abs(state.get(height_key, 0) - floor) < 0.02, 15)
        server_y, stored = server_height(), energy()
        if abs(server_y - floor) >= 0.02 or stored != charge:
            raise AssertionError(f"{label}: expected rest at Y={floor} and {charge} EU, got {server_y}, {stored}")

    command(f"data merge block {location} {{eu:500.0d}}")
    samples = []
    deadline = time.monotonic() + 12
    while time.monotonic() < deadline:
        local_y = client.act("state")[height_key]
        remote_y = server_height()
        remaining = energy()
        samples.append({"clientY": local_y, "serverY": remote_y, "eu": remaining})
        if min(local_y, remote_y) >= floor + 1.5 and remaining < 500:
            break
    else:
        raise AssertionError(f"Magnetizer did not lift the player on both sides: {samples}")
    command(f"item replace entity {player} armor.feet with minecraft:air")
    command(f"tp {player} 37.5 {floor} 33.5")
    return samples
