#!/usr/bin/env python3
"""Conservative Java token similarity check; Python standard library only."""
import argparse
import hashlib
import io
import json
import os
import re
import sys
from pathlib import Path

KEYWORDS = set((
    "abstract assert boolean break byte case catch char class const continue default do "
    "double else enum extends final finally float for goto if implements import instanceof "
    "int interface long native new package private protected public return short static "
    "strictfp super switch synchronized this throw throws transient try void volatile while "
    "true false null record sealed permits var yield _"
).split())
TOKEN = re.compile(
    r'(?P<skip>\s+|//[^\r\n]*|/\*[\s\S]*?\*/)'
    r'|(?P<literal>"""(?:\\[\s\S]|(?!""")[\s\S])*"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\')'
    r'|(?P<identifier>(?:[^\W\d]|\$)[\w$]*)'
    r'|(?P<number>0[xX](?:[\da-fA-F_]+(?:\.[\da-fA-F_]*)?|\.[\da-fA-F_]+)(?:[pP][+-]?[\d_]+)?[fFdDlL]?'
    r'|0[bB][01_]+[lL]?|(?:\d[\d_]*(?:\.[\d_]*)?|\.[\d_]+)(?:[eE][+-]?[\d_]+)?[fFdDlL]?)'
    r'|(?P<operator>>>>=|>>=|<<=|>>>|>>|<<|->|::|\.\.\.|[+*/%&|^!=<>-]=|\+\+|--|&&|\|\|)'
    r'|(?P<other>.)', re.DOTALL
)


def tokenize(source):
    return ["ID" if match.lastgroup == "identifier" and match[0] not in KEYWORDS else match[0]
            for match in TOKEN.finditer(source) if match.lastgroup != "skip"]


def classify(score, identical_lines=False):
    if score > 0.25 or identical_lines:
        return "fail"
    return "warn" if score > 0.15 else "pass"


def directory(value):
    if not value.strip() or not Path(value).is_dir():
        raise ValueError("not a directory: " + repr(value))
    return Path(value)


def walk_error(error):
    raise error


def inventory(root):
    paths = []
    for base, directories, names in os.walk(root, onerror=walk_error):
        directories.sort()
        paths.extend(Path(base) / name for name in sorted(names) if name.endswith(".java"))
    if not paths:
        raise ValueError("no .java files in " + str(root))
    return paths


def unique_policy_entries(pairs):
    entries = {}
    for path, digest in pairs:
        if path in entries:
            raise ValueError("duplicate boilerplate policy path: " + repr(path))
        entries[path] = digest
    return entries


def load_policy(file):
    try:
        entries = json.loads(Path(file).read_text(encoding="utf-8"),
                             object_pairs_hook=unique_policy_entries)
    except (json.JSONDecodeError, RecursionError) as error:
        raise ValueError("invalid boilerplate policy JSON: " + str(error)) from error
    if not isinstance(entries, dict):
        raise ValueError("boilerplate policy must be a JSON object")
    for path, digest in entries.items():
        if ("\\" in path or "\0" in path or re.match(r"^[A-Za-z]:", path)
                or any(part in ("", ".", "..") for part in path.split("/"))):
            raise ValueError("invalid boilerplate policy path: " + repr(path))
        if not isinstance(digest, str) or not re.fullmatch(r"[0-9a-f]{64}", digest):
            raise ValueError("invalid boilerplate policy SHA-256 for " + repr(path))
    return entries


def load(root, paths, policy=None):
    files = []
    for path in paths:
        relative = path.relative_to(root)
        expected = policy.get(relative.as_posix()) if policy is not None else None
        matched = False
        if expected is None:
            source = path.read_text(encoding="utf-8")
        else:
            contents = path.read_bytes()
            matched = hashlib.sha256(contents).hexdigest() == expected
            # Use the same bytes and universal newline handling as read_text.
            with io.TextIOWrapper(io.BytesIO(contents), encoding="utf-8") as text:
                source = text.read()
        tokens = tokenize(source)
        if len(tokens) < 5:
            raise ValueError("insufficient tokens (need 5): " + str(path))
        lines = source.splitlines()
        item = {"path": str(relative),
                "grams": {tuple(tokens[i:i + 5]) for i in range(len(tokens) - 4)},
                "lines": {tuple(lines[i:i + 8]): i + 1 for i in range(len(lines) - 7)}}
        if policy is not None:
            item["boilerplate_matched"] = matched
        files.append(item)
    return files


def compare(candidate, references):
    scores, exact = [], []
    for reference in references:
        left, right = candidate["grams"], reference["grams"]
        scores.append((len(left & right) / len(left | right), reference["path"]))
        for lines, start in candidate["lines"].items():
            if lines in reference["lines"]:
                exact.append({"reference": reference["path"], "candidate_line": start,
                              "reference_line": reference["lines"][lines]})
                break
    score, matched = max(scores, key=lambda pair: pair[0])
    reasons = []
    if score > 0.25:
        reasons.append("jaccard_above_25_percent")
    elif score > 0.15:
        reasons.append("jaccard_above_15_percent")
    if exact:
        reasons.append("eight_identical_lines")
    result = {"candidate": candidate["path"], "match": matched, "score": score,
              "status": classify(score, bool(exact)), "reasons": reasons,
              "exact_line_matches": exact}
    if "boilerplate_matched" in candidate:
        matched_policy = candidate["boilerplate_matched"]
        applied = matched_policy and not exact and result["status"] != "pass"
        result.update(raw_status=result["status"], boilerplate_matched=matched_policy,
                      boilerplate_applied=applied)
        if applied:
            result["status"] = "pass"
    return result


class Parser(argparse.ArgumentParser):
    def error(self, message):
        raise ValueError(message)


def run(argv=None):
    report = {"status": "error", "warnings": [], "errors": [], "files": [],
              "scanned": {"candidate": 0, "reference": 0, "comparisons": 0}}
    try:
        parser = Parser(description=__doc__)
        parser.add_argument("--candidate", required=True)
        parser.add_argument("--reference", action="append",
                            help="reference tree; repeat to compare against several decompiled versions")
        parser.add_argument("--boilerplate-policy",
                            help="JSON map of approved relative paths to raw SHA-256 digests")
        args = parser.parse_args(argv)
        candidate = directory(args.candidate)
        policy = load_policy(args.boilerplate_policy) if args.boilerplate_policy is not None else None
        if args.reference is None:
            if os.environ.get("CI", "").lower() not in ("", "0", "false", "no"):
                report["status"] = "skipped"
                report["warnings"].append("reference omitted in CI; gate was NOT evaluated")
                return report, 3
            raise ValueError("--reference is required outside CI")
        roots = [directory(value) for value in args.reference]
        candidates = inventory(candidate)
        report["scanned"]["candidate"] = len(candidates)
        reference_files = []
        for index, root in enumerate(roots):
            loaded = load(root, inventory(root))
            if len(roots) > 1:
                for item in loaded:
                    item["path"] = "ref%d:%s" % (index + 1, item["path"])
            reference_files.extend(loaded)
        report["scanned"]["reference"] = len(reference_files)
        for item in load(candidate, candidates, policy):
            report["files"].append(compare(item, reference_files))
            report["scanned"]["comparisons"] += len(reference_files)
        statuses = {item["status"] for item in report["files"]}
        report["status"] = "fail" if "fail" in statuses else "warn" if "warn" in statuses else "pass"
        return report, int("fail" in statuses)
    except (OSError, UnicodeError, ValueError) as error:
        report["errors"].append(str(error))
        return report, 2


if __name__ == "__main__":
    result, code = run()
    print(json.dumps(result, ensure_ascii=False, indent=2))
    for warning in result["warnings"]:
        print("warning: " + warning, file=sys.stderr)
    sys.exit(code)
