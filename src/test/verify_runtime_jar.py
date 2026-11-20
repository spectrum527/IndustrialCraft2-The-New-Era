"""Compare each development mod class/resource with the release JAR for the same target."""

import argparse
import hashlib
import json
from pathlib import Path
from zipfile import BadZipFile, ZipFile


PACKAGING = {"META-INF/MANIFEST.MF", "LICENSE", "COPYING", "LICENSE-ASSETS"}
TEST_SOURCE = Path(__file__).resolve().parent / "src"
FIXTURE_MARKERS = (b"ic2_gametest", b"ic2_modern_smoke", b"ic2:qa_platform_pillar", b"ic2:qa_fuel_generator", b"ic2:qa_side",
                   b"ic2_qa:platform_blocks", b"ic2_qa:platform_items", b"ic2accept")
TEST_PREFIXES = ("com/industrialage/ia2/gametest/", "com/industrialage/ia2/modernsmoke/",
                 "data/ic2_gametest/", "assets/ic2_modern_smoke/", "data/ic2_modern_smoke/", "assets/ic2_qa/",
                 "data/ic2_qa/", "org/junit/", "org/opentest4j/", "org/apiguardian/")


def fixture_classes():
    return {source.relative_to(folder).with_suffix("").as_posix()
            for folder in TEST_SOURCE.glob("*/java") for source in folder.rglob("*.java")}


def fingerprint(entries):
    digest = hashlib.sha256()
    for name, sha256 in sorted(entries.items()):
        digest.update(name.encode("utf-8") + b"\0" + bytes.fromhex(sha256))
    return digest.hexdigest()


def verify(jar, roots, previous=None):
    expected, actual, problems = {}, {}, []
    for folder in roots:
        if not folder.is_dir():
            problems.append(f"Missing development output: {folder}")
            continue
        for file in folder.rglob("*"):
            if file.is_file():
                name = file.relative_to(folder).as_posix()
                value = hashlib.sha256(file.read_bytes()).hexdigest()
                if name in expected and expected[name] != value:
                    problems.append(f"Conflicting development outputs: {name}")
                expected[name] = value
    if not any(name.endswith(".class") for name in expected):
        problems.append("No development classes found")
    if "META-INF/neoforge.mods.toml" not in expected:
        problems.append("Missing development mod metadata")
    test_classes = fixture_classes()
    with ZipFile(jar) as archive:
        for entry in archive.infolist():
            if entry.is_dir():
                continue
            name = entry.filename
            contents = archive.read(entry)
            if name in actual:
                problems.append(f"Duplicate JAR entry: {name}")
            actual[name] = hashlib.sha256(contents).hexdigest()
            test_class = name.endswith(".class") and name[:-6].split("$", 1)[0] in test_classes
            if name.startswith(TEST_PREFIXES) or test_class or any(key in contents for key in FIXTURE_MARKERS):
                problems.append(f"Test-only content in production JAR: {name}")
    for name in sorted(expected.keys() | actual.keys()):
        if name not in actual:
            problems.append(f"Missing from production JAR: {name}")
        elif name not in expected:
            if name not in PACKAGING:
                problems.append(f"Unexpected production JAR entry: {name}")
        elif actual[name] != expected[name]:
            problems.append(f"Different bytes: {name}")
    report = {"jar": str(jar.resolve()), "jarSha256": hashlib.sha256(jar.read_bytes()).hexdigest(),
              "sourceTreeSha256": fingerprint(expected), "productionEntries": len(expected),
              "entries": expected, "problems": problems}
    if previous:
        prior = json.loads(previous.read_text(encoding="utf-8"))
        if prior.get("problems") != [] or any(prior.get(k) != report[k]
                for k in ("jarSha256", "sourceTreeSha256")):
            problems.append(f"Tested artifact changed since {previous}")
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", required=True, type=Path)
    parser.add_argument("--output", required=True, action="append", type=Path, help="ModDev production output directory")
    parser.add_argument("--report", required=True, type=Path)
    parser.add_argument("--match", type=Path, help="Earlier report for the same running candidate")
    args = parser.parse_args()
    try:
        report = verify(args.jar, args.output, args.match)
    except (BadZipFile, OSError, ValueError) as error:
        report = {"jar": str(args.jar), "problems": [str(error)]}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.report.with_name(args.report.name + ".tmp")
    temporary.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    temporary.replace(args.report)
    if report["problems"]:
        raise SystemExit("\n".join(report["problems"]))
    print(f"Verified {report['productionEntries']} development entries against {args.jar.name} {report['jarSha256']}")


if __name__ == "__main__":
    main()
