"""Independently read the saved 1.19.2 Anvil chunk and check the native generator observations."""

import gzip
import hashlib
import io
from pathlib import Path
import struct
import zlib


def digest(data):
    return hashlib.sha256(data).hexdigest()


def nbt(data):
    stream = io.BytesIO(data)

    def number(fmt):
        return struct.unpack(">" + fmt, stream.read(struct.calcsize(">" + fmt)))[0]

    def name():
        return stream.read(number("H")).decode("utf-8")

    def read(kind):
        if 1 <= kind <= 6:
            return number("bhiqfd"[kind - 1])
        if kind in (7, 11, 12):
            count = number("i")
            if count < 0:
                raise ValueError("Negative NBT array size")
            return stream.read(count * {7: 1, 11: 4, 12: 8}[kind])
        if kind == 8:
            return name()
        if kind == 9:
            child, count = number("B"), number("i")
            return [read(child) for _ in range(count)]
        if kind == 10:
            fields = {}
            while (child := number("B")):
                key = name()
                fields[key] = (child, read(child))
            return fields
        raise ValueError(f"Unexpected NBT type {kind}")

    if number("B") != 10:
        raise ValueError("Missing root compound")
    name()
    return read(10)


def region_generator(directory, phase):
    x, y, z = 29, 90, 25
    cx, cz = x >> 4, z >> 4
    region = directory / "world/region" / f"r.{cx >> 5}.{cz >> 5}.mca"
    with region.open("rb") as stream:
        stream.seek(4 * ((cx & 31) + 32 * (cz & 31)))
        location = int.from_bytes(stream.read(4), "big")
        if not location:
            raise AssertionError("Generator chunk missing from saved region")
        stream.seek((location >> 8) * 4096)
        length, compression = struct.unpack(">IB", stream.read(5))
        chunk = {1: gzip.decompress, 2: zlib.decompress, 3: bytes}[compression](stream.read(length - 1))
    data = nbt(chunk)
    if data["xPos"] != (3, cx) or data["zPos"] != (3, cz):
        raise AssertionError("Region header resolved a different chunk")
    matches = [row for row in data["block_entities"][1]
               if (row["x"][1], row["y"][1], row["z"][1]) == (x, y, z)]
    native_file = directory / f"generator-{phase}.nbt"
    raw = native_file.read_bytes()
    actual = nbt(gzip.decompress(raw))
    if phase == "write":
        if len(matches) != 1:
            raise AssertionError("Expected one persisted generator at the original coordinates")
        compare = matches[0]
    else:
        if matches:
            raise AssertionError("Destroyed generator remains in the stopped world's region")
        compare = nbt(gzip.decompress((directory / "generator-write.nbt").read_bytes()))
    for key in ("id", "x", "y", "z", "qa_slot", "generator"):
        if actual[key] != compare[key]:
            raise AssertionError(f"{phase} native block data differs from the saved generator at {key}")
    if (actual["id"] != (8, "ic2_qa:fuel_generator") or actual["generator"][1]["fuel_ticks"] != (3, 363)
            or actual["generator"][1]["stored_energy"] != (6, 370.0)
            or actual["qa_slot"][1]["id"] != (8, "minecraft:coal") or actual["qa_slot"][1]["Count"] != (1, 1)):
        raise AssertionError("Persisted generator fields have unexpected types or values")
    return {"region": str(region.relative_to(directory)), "chunk": [cx, cz], "position": [x, y, z],
            "generatorCountAfterShutdown": len(matches), "chunkNbtSha256": digest(chunk),
            "nativeSample": native_file.name, "nativeSampleSha256": digest(raw)}


def machine_samples(phase, result):
    expected = ({"lava": (0, 0, 0, "minecraft:lava_bucket"), "saved": (37, 363, 370, "minecraft:coal")}
                if phase == "write" else {"cold": (0, 363, 370, "minecraft:coal"),
                                          "full": (363, 0, 4000, "minecraft:coal"),
                                          "afterPacket": (363, 0, 3968, "minecraft:coal")})
    if result["generator.position"] != "29, 90, 25":
        raise AssertionError("Generator coordinates changed")
    for sample, values in expected.items():
        key = "generator." + sample + "."
        seen = (int(result[key + "workTicks"]), int(result[key + "fuelTicks"]),
                float(result[key + "eu"]), result[key + "item"])
        if seen != values or int(result[key + "count"]) != 1:
            raise AssertionError(f"{sample}: {seen}, expected {values} with exactly one item")
    if phase == "read" and (float(result["generator.drawnEU"]), float(result["generator.removedPortEU"])) != (32, 0):
        raise AssertionError("Loaded and removed port debits differ from 32 and 0 EU")
