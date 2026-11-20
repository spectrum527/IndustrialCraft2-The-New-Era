#!/usr/bin/env python3
"""Generate a redacted release originality audit without changing check.py."""
import argparse
from bisect import bisect_left
from collections import Counter, defaultdict
from dataclasses import dataclass, field
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

import check

VERSIONS = ("2.9.40", "2.8.222", "1.2.4-port.20261120")
PROJECTS = {
    "neoforge-1.21.1": (":common", ":neoforge:1.21.1"),
    "neoforge-26.1.2": (":common", ":neoforge:26.1.2"),
    "neoforge-26.2": (":common", ":neoforge:26.2"),
    "forge-1.19.2": (":forge:1.19.2",),
}
INVENTORY_PREFIX = "ORIGINALITY_INVENTORY "
SEED_LENGTH = 16
METHOD_TYPES = {"C": "constructor", "M": "method", "B": "initializer block", "F": "field initializer"}
IMPORT_WINDOW_CATEGORIES = {"import_only", "mixed_blank_import"}


def sha(data):
    return hashlib.sha256(data).hexdigest()


def tree_fingerprint(items):
    return sha("\n".join(f"{path} {digest}" for path, digest in items).encode("utf-8"))


@dataclass
class Body:
    kind: str
    start: int
    end: int
    tokens: tuple
    alpha: tuple


@dataclass
class Source:
    absolute: Path
    path: str
    sha256: str = field(init=False)
    text: str = field(init=False)
    parser_errors: int = 0
    spans: list = field(default_factory=list)
    bodies: list = field(default_factory=list)
    grams: set = field(default_factory=set)

    def __post_init__(self):
        data = self.absolute.read_bytes()
        self.sha256 = sha(data)
        self.text = data.decode("utf-8")

    @property
    def identity(self):
        return self.path, self.sha256


def slices(items, length):
    for index in range(len(items) - length + 1):
        yield items[index:index + length]


def source_offset_map(text):
    escaped_characters = [index for index, char in enumerate(text) if ord(char) > 0xFFFF]
    surrogate_positions = [index + offset + 1 for offset, index in enumerate(escaped_characters)]
    return lambda position: position - bisect_left(surrogate_positions, position)


def parse_sources(sources, java):
    with tempfile.TemporaryDirectory(prefix="ic2-originality-") as folder:
        manifest = Path(folder) / "inputs.txt"
        helper = Path(folder) / "ExecutableRanges.java"
        manifest.write_text("".join(f"{source.absolute}\n" for source in sources), encoding="utf-8")
        helper.write_text(JAVA_SOURCE, encoding="utf-8")
        result = subprocess.run([java, str(helper), str(manifest)], check=True,
                                encoding="utf-8", capture_output=True)
    seen = set()
    for row in result.stdout.splitlines():
        index, errors, *ranges = row.split("\t")
        source = sources[int(index)]
        seen.add(int(index))
        convert = source_offset_map(source.text)
        source.parser_errors = int(errors)
        for entry in ranges:
            kind, start, end = entry.split(":")
            source.spans.append((kind, convert(int(start)), convert(int(end))))
        split_bodies(source)
    if seen != set(range(len(sources))):
        raise ValueError("Javac did not report a result for every input file")


def split_bodies(source):
    classes = sorted((start, end) for kind, start, end in source.spans if kind == "X")
    for kind, start, end in source.spans:
        if kind not in METHOD_TYPES:
            continue
        cursor = start
        for left, right in classes:
            if start <= left and right <= end and cursor < right:
                append_body(source, kind, cursor, left)
                cursor = right
        append_body(source, kind, cursor, end)


def append_body(source, kind, start, end):
    matches = [match for match in check.TOKEN.finditer(source.text, start, end)
               if match.lastgroup != "skip"]
    if not matches:
        return
    tokens = tuple(match[0] for match in matches)
    alpha = tuple("ID" if match.lastgroup == "identifier" and match[0] not in check.KEYWORDS else match[0]
                  for match in matches)
    body = Body(METHOD_TYPES[kind], start, end, tokens, alpha)
    source.bodies.append(body)
    source.grams.update(slices(alpha, 5))


def compare_body_grams(candidates, references):
    for source in candidates:
        peaks = {}
        for version, files in references.items():
            best = None
            score = 0.0
            if source.grams:
                for reference in files:
                    shared = len(source.grams & reference.grams)
                    ratio = shared / (len(source.grams) + len(reference.grams) - shared)
                    if ratio > score:
                        score, best = ratio, reference
            peaks[version] = (score, best)
        yield peaks


def compare_body_runs(candidates, references, token_field):
    query = defaultdict(list)
    for source in candidates:
        for body in source.bodies:
            tokens = getattr(body, token_field)
            for offset, seed in enumerate(slices(tokens, SEED_LENGTH)):
                query[seed].append((source.identity, body, offset))

    best = defaultdict(dict)
    for version, files in references.items():
        frequencies = Counter()
        for reference in files:
            seen = set()
            for right in reference.bodies:
                right_tokens = getattr(right, token_field)
                for r_offset, seed in enumerate(slices(right_tokens, SEED_LENGTH)):
                    locations = query.get(seed)
                    if locations is None:
                        continue
                    seen.add(seed)
                    for identity, left, l_offset in locations:
                        left_tokens = getattr(left, token_field)
                        if l_offset and r_offset and left_tokens[l_offset - 1] == right_tokens[r_offset - 1]:
                            continue
                        length = SEED_LENGTH
                        limit = min(len(right_tokens) - r_offset, len(left_tokens) - l_offset)
                        while length < limit and left_tokens[l_offset + length] == right_tokens[r_offset + length]:
                            length += 1
                        current = best[identity].get(version)
                        if current is None or length > current["tokens"]:
                            best[identity][version] = {
                                "tokens": length, "candidateSegment": left.kind, "seed": seed,
                                "candidate": (left, l_offset), "reference": (reference, right, r_offset),
                            }
            frequencies.update(seen)
        for peaks in best.values():
            if version in peaks:
                peaks[version]["referenceFilesWithSeed"] = frequencies[peaks[version]["seed"]]
    return best


def line_roles(source):
    intervals = [(kind, start, end) for kind, start, end in source.spans if kind != "X"]
    pos = 0
    for line in source.text.splitlines(keepends=True):
        first = pos + len(line) - len(line.lstrip())
        last = pos + len(line.rstrip())
        if not line.strip():
            yield "blank"
        elif any(kind == "P" and start <= first and stop >= last for kind, start, stop in intervals):
            yield "import"
        elif line.lstrip().startswith("package "):
            yield "package"
        elif line.lstrip().startswith(("//", "/*", "*")):
            yield "comment"
        elif any(kind in METHOD_TYPES and start < last and stop > first for kind, start, stop in intervals):
            yield "executable"
        else:
            yield "declaration"
        pos += len(line)


def exact_line_categories(source, candidate, original, references):
    if not original["exact_line_matches"]:
        return {}, {}
    roles = list(line_roles(source))

    def category(start):
        window = set(roles[start - 1:start + 7])
        if len(window) == 1:
            return next(iter(window)) + "_only"
        return "mixed_" + "_".join(sorted(window))

    primary = Counter((match["reference"], category(match["candidate_line"]))
                      for match in original["exact_line_matches"])
    all_windows = Counter((ref["path"], category(start))
                          for ref in references
                          for window, start in candidate["lines"].items() if window in ref["lines"])
    return by_version(primary), by_version(all_windows)


def by_version(groups):
    result = defaultdict(dict)
    for (version, category), count in sorted(groups.items()):
        result[version][category] = count
    return dict(result)


def read_inventories(neo_root, forge_root, neo_log, forge_log):
    targets = {}
    for root, log in ((neo_root, neo_log), (forge_root, forge_log)):
        rows = [row.removeprefix(INVENTORY_PREFIX) for row in log.read_text(encoding="utf-8").splitlines()
                if row.startswith(INVENTORY_PREFIX)]
        if len(rows) != 1:
            raise ValueError("Gradle source inventory must contain exactly one inventory record")
        for target, projects in json.loads(rows[0]).items():
            if target in targets or set(projects) != set(PROJECTS[target]):
                raise ValueError(f"Unexpected Gradle projects for {target}")
            paths = set()
            for project in projects.values():
                compile_files = set(project["compileFiles"])
                if compile_files != set(project["sourceSetFiles"]):
                    raise ValueError(f"{target} sourceSets.main.java differs from compileJava.source")
                for path in compile_files:
                    absolute = (root / path).resolve()
                    if not absolute.is_relative_to(root / "src") or not absolute.is_file():
                        raise ValueError(f"{target} has a missing Java input or an input outside src")
                    paths.add(absolute.relative_to(root / "src"))
            if not paths:
                raise ValueError(f"No production Java files for {target}")
            targets[target] = (root, projects, sorted(paths))
    if set(targets) != set(PROJECTS):
        raise ValueError("Gradle inventories did not account for all four release targets")
    return targets


def git_java_blobs(root, ref):
    git = ["git", "-C", str(root)]
    commit = subprocess.check_output([*git, "rev-parse", "--verify", f"{ref}^{{commit}}"],
                                     encoding="utf-8").strip()
    prefix = subprocess.check_output([*git, "rev-parse", "--show-prefix"], encoding="utf-8").strip()
    git_hash = subprocess.check_output([*git, "rev-parse", "--show-object-format"], encoding="utf-8").strip()
    listing = subprocess.check_output([*git, "ls-tree", "-rz", "--full-tree", commit, "--", prefix or "."])
    tracked = {}
    for row in listing.rstrip(b"\0").split(b"\0"):
        metadata, raw_path = row.split(b"\t", 1)
        path = raw_path.decode("utf-8")
        mode, kind, digest = metadata.decode("ascii").split()
        if kind == "blob" and mode in ("100644", "100755") and path.endswith(".java"):
            tracked[path.removeprefix(prefix)] = digest
    return commit, git_hash, tracked


def verify_blob(source, git_hash, tracked):
    contents = source.text.encode("utf-8")
    header = f"blob {len(contents)}\0".encode("ascii")
    return hashlib.new(git_hash, header + contents).hexdigest() == tracked.get(source.path)


def reference_sources(reference_paths, repair_commit):
    sources = {version: [Source(path, path.relative_to(root).as_posix())
                         for path in check.inventory(root)] for version, root in reference_paths.items()}
    commit, git_hash, tracked = git_java_blobs(reference_paths[VERSIONS[-1]], repair_commit)
    repaired = sources[VERSIONS[-1]]
    if {source.path for source in repaired} != set(tracked):
        raise ValueError("Repair production Java inventory differs from the pinned commit")
    if not all(verify_blob(source, git_hash, tracked) for source in repaired):
        raise ValueError("Repair production Java bytes differ from the pinned commit")
    return sources, commit


def safe_runs(peaks):
    return {version: {key: detail[key] for key in ("tokens", "candidateSegment", "referenceFilesWithSeed")}
            if (detail := peaks.get(version)) else {"tokens": 0}
            for version in VERSIONS}


def review_triggers(original, body, has_parser_errors):
    triggers = []
    if original["status"] == "fail":
        triggers.append("original_failure")
    elif original["status"] == "warn" and original["score"] >= 0.22:
        triggers.append("upper_original_warning")
    if body["tokens"] >= 30 and max(body["body5GramByReference"].values()) >= 0.28:
        triggers.append("elevated_body_jaccard")
    if max(entry["tokens"] for entry in body["literalRunByReference"].values()) >= 18:
        triggers.append("retained_tokens_run")
    if max(entry["tokens"] for entry in body["normalizedRunByReference"].values()) >= 30:
        triggers.append("normalized_tokens_run")
    if has_parser_errors:
        triggers.append("candidate_parser_recovery")
    return triggers


def audit(args):
    targets = read_inventories(args.neo_root, args.forge_root, args.neo_inventory, args.forge_inventory)
    supplied_references = {version: Path(path).resolve() for version, path in args.reference}
    if len(args.reference) != len(VERSIONS) or set(supplied_references) != set(VERSIONS):
        raise ValueError("Supply one source tree for each of the three baseline versions")
    references, repair_commit = reference_sources(
        {version: supplied_references[version] for version in VERSIONS}, args.repair_commit)
    all_reference_sources = [source for files in references.values() for source in files]
    checker_hash = sha(Path(check.__file__).read_bytes())
    policies = {}
    candidate_by_target = {}
    unique = {}
    copies_to_verify = set()
    target_git = {
        args.neo_root: git_java_blobs(args.neo_root / "src", args.neo_commit),
        args.forge_root: git_java_blobs(args.forge_root / "src", args.forge_commit),
    }
    for target, (repo, _, paths) in targets.items():
        tools_path = repo / "tools/similarity"
        if sha((tools_path / "check.py").read_bytes()) != checker_hash:
            raise ValueError(f"Checker versions differ for the {target} release tree")
        policy_path = tools_path / "boilerplate.json"
        policies[target] = (sha(policy_path.read_bytes()), check.load_policy(policy_path))
        candidates = []
        candidate_commit, algorithm, tracked = target_git[repo]
        for path in paths:
            source = Source(repo / "src" / path, path.as_posix())
            if not verify_blob(source, algorithm, tracked):
                raise ValueError(f"{target} Java input differs from commit {candidate_commit}: {source.path}")
            unique.setdefault(source.identity, source)
            copies_to_verify.add((source.absolute, source.sha256))
            candidates.append(source.identity)
        candidate_by_target[target] = candidates

    if len({digest for digest, _ in policies.values()}) != 1:
        raise ValueError("A26 policy versions differ across the release trees")
    parse_sources([*unique.values(), *all_reference_sources], args.java)
    if any(source.parser_errors for source in unique.values()):
        print("Candidate Javac parser diagnostics require review", flush=True)
    reference_report = {
        version: {
            "javaFiles": len(files),
            "sha256": tree_fingerprint((source.path, source.sha256) for source in files),
            "javacRecoveredFiles": sum(source.parser_errors > 0 for source in files),
            "executableRanges": sum(len(source.bodies) for source in files),
        }
        for version, files in references.items()
    }
    reference_report[VERSIONS[-1]]["gitCommit"] = repair_commit
    print(f"Parsed {len(unique)} unique candidates and {len(all_reference_sources)} reference files", flush=True)

    body_grams = dict(zip(unique, compare_body_grams(unique.values(), references)))
    literal = compare_body_runs(unique.values(), references, "tokens")
    normalized = compare_body_runs(unique.values(), references, "alpha")
    print("Compared executable bodies and initializers", flush=True)
    original_references = []
    for version, files in references.items():
        loaded = check.load(supplied_references[version], [source.absolute for source in files])
        for item in loaded:
            item["path"] = version
        original_references.extend(loaded)

    manual = {}
    if args.manual is not None:
        entries = json.loads(args.manual.read_text(encoding="utf-8"))
        for entry in entries:
            identity = entry["path"], entry["sha256"]
            if identity in manual or entry["verdict"] not in ("clear", "material", "inconclusive") or not entry["basis"]:
                raise ValueError("Invalid or duplicated manual review")
            manual[identity] = {key: entry[key] for key in ("verdict", "category", "basis")}

    rows = {}
    separately_reviewed = []
    blocked_exact = []
    for target, (repo, _, _) in targets.items():
        _, policy = policies[target]
        for identity in candidate_by_target[target]:
            if identity not in rows:
                source = unique[identity]
                candidate_root = repo / "src"
                candidate = check.load(candidate_root, [candidate_root / source.path], policy)[0]
                original = check.compare(candidate, original_references)
                primary, all_windows = exact_line_categories(source, candidate, original, original_references)
                raw = {key: original[key] for key in ("score", "match", "status", "raw_status", "reasons",
                                                     "boilerplate_matched", "boilerplate_applied")}
                raw.update(eightLineGroups=len(original["exact_line_matches"]),
                           eightLinePrimaryCategories=primary, eightLineAllCategories=all_windows)
                body = {
                    "tokens": sum(len(body.tokens) for body in source.bodies),
                    "segments": dict(sorted(Counter(body.kind for body in source.bodies).items())),
                    "body5GramByReference": {v: result[0] for v, result in body_grams[identity].items()},
                    "literalRunByReference": safe_runs(literal[identity]),
                    "normalizedRunByReference": safe_runs(normalized[identity]),
                    "javacParserErrors": source.parser_errors,
                }
                triggers = review_triggers(original, body, source.parser_errors > 0)
                rows[identity] = {"path": source.path, "sha256": source.sha256, "originalChecker": raw,
                                  "executable": body, "manualReviewTriggers": triggers}
                if triggers or identity in manual:
                    separately_reviewed.append(identity)
                    rows[identity]["manualReview"] = manual.get(identity, {"verdict": "pending"})
                if any(category not in IMPORT_WINDOW_CATEGORIES
                       for counts in all_windows.values() for category in counts):
                    blocked_exact.append(identity)
        print(f"{target}: {len(candidate_by_target[target])} production Java files", flush=True)

    counts = Counter(manual.get(identity, {}).get("verdict", "pending") for identity in separately_reviewed)
    overall = "clear"
    if blocked_exact or counts["material"]:
        overall = "requires_action"
    elif counts["inconclusive"] or counts["pending"]:
        overall = "incomplete"

    snapshots = {}
    for target, (repo, projects, _) in targets.items():
        target_files = [rows[identity] for identity in candidate_by_target[target]]
        original_counts = Counter(row["originalChecker"]["status"] for row in target_files)
        original_status = "fail" if original_counts["fail"] else "warn" if original_counts["warn"] else "pass"
        snapshots[target] = {
            "gitCommit": target_git[repo][0],
            "sourceSet": {project: {"roots": values["sourceRoots"], "javaFiles": len(values["compileFiles"])}
                          for project, values in projects.items()},
            "sha256": tree_fingerprint(candidate_by_target[target]),
            "fileCount": len(target_files),
            "originalCheckerSummary": {"status": original_status,
                                       "counts": {key: original_counts[key] for key in ("pass", "warn", "fail")}},
            "manualReviewCounts": dict(Counter(row["manualReview"]["verdict"] for row in target_files
                                                if "manualReview" in row)),
            "files": target_files,
        }

    for path, digest in copies_to_verify | {(source.absolute, source.sha256) for source in all_reference_sources}:
        if sha(path.read_bytes()) != digest:
            raise ValueError("An audited source changed during the audit; regenerate the Gradle inventory and scan again")
    java_version = subprocess.run([args.java, "--version"], check=True, encoding="utf-8",
                                  capture_output=True).stdout.splitlines()[0]
    policy_digest, policy_entries = next(iter(policies.values()))
    report = {
        "schemaVersion": 1,
        "reviewStatus": overall,
        "originalCheckerSha256": checker_hash,
        "a26Policy": {"sha256": policy_digest, "approvedEntries": policy_entries},
        "referenceSources": reference_report,
        "analysis": {
            "javac": java_version,
            "scannerSha256": sha(Path(__file__).read_bytes()),
            "gradleInitScriptSha256": sha(GRADLE_INIT_SOURCE.encode("utf-8")),
            "minimumRunTokens": SEED_LENGTH,
            "zeroRunMeans": f"No contiguous run of at least {SEED_LENGTH} tokens",
            "uniqueCandidatePathShaPairs": len(unique),
            "bodyComparisons": len(unique) * len(all_reference_sources),
            "manualReviewResults": {key: counts[key] for key in ("clear", "material", "inconclusive", "pending")},
            "nonImportEightLineCandidates": len(blocked_exact),
        },
        "targets": snapshots,
    }
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    inventory = commands.add_parser("inventory-init", help="write the read-only Gradle inventory init script")
    inventory.add_argument("path", type=Path)
    scan = commands.add_parser("audit", help="score actual production Java inputs and redact baseline locations")
    for name in ("neo-root", "forge-root"):
        scan.add_argument(f"--{name}", required=True, type=lambda path: Path(path).resolve())
    for name in ("neo-commit", "forge-commit"):
        scan.add_argument(f"--{name}", default="HEAD")
    for name in ("neo-inventory", "forge-inventory", "output"):
        scan.add_argument(f"--{name}", required=True, type=Path)
    scan.add_argument("--reference", nargs=2, action="append", required=True, metavar=("VERSION", "ROOT"))
    scan.add_argument("--repair-commit", required=True)
    scan.add_argument("--java", default="java")
    scan.add_argument("--manual", type=Path, help="JSON array of candidate path, SHA-256, verdict, category, and basis")
    args = parser.parse_args()
    if args.command == "inventory-init":
        args.path.write_text(GRADLE_INIT_SOURCE, encoding="utf-8")
        return
    report = audit(args)
    args.output.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Independent review: {report['reviewStatus']}", flush=True)


GRADLE_INIT_SOURCE = r"""
gradle.projectsEvaluated {
    def root = gradle.rootProject
    root.tasks.register('originalityProductionSources') {
        doLast {
            def targets = [
                ':neoforge:1.21.1': [':common', ':neoforge:1.21.1'],
                ':neoforge:26.1.2': [':common', ':neoforge:26.1.2'],
                ':neoforge:26.2': [':common', ':neoforge:26.2'],
                ':forge:1.19.2': [':forge:1.19.2']
            ]
            def sourceSetsByTarget = targets.findAll { target, projects -> root.findProject(target) != null }.collectEntries { target, projects ->
                def sources = projects.collectEntries { name ->
                    def project = root.project(name)
                    def main = project.extensions.getByName('sourceSets').getByName('main')
                    def compile = project.tasks.named(main.compileJavaTaskName).get()
                    def relativize = { file -> root.rootDir.toPath().relativize(file.toPath()).toString() }
                    [(name): [sourceRoots: main.java.srcDirs.collect(relativize).sort(),
                        sourceSetFiles: main.java.files.findAll { it.name.endsWith('.java') }.collect(relativize).sort(),
                        compileFiles: compile.source.files.findAll { it.name.endsWith('.java') }.collect(relativize).sort()]]
                }
                [(target.substring(1).replace(':','-')): sources]
            }
            println 'ORIGINALITY_INVENTORY ' + groovy.json.JsonOutput.toJson(sourceSetsByTarget)
        }
    }
}
"""

JAVA_SOURCE = r"""
import com.sun.source.tree.*;
import com.sun.source.util.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;

class ExecutableRanges {
    private record Span(char kind, int start, int end) {
        @Override
        public String toString() { return kind + ":" + start + ":" + end; }
    }

    public static void main(String[] args) throws Exception {
        List<Path> files = Files.readAllLines(Path.of(args[0])).stream().map(Path::of).toList();
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A full JDK is required.");
        try (var manager = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
            for (int offset = 0; offset < files.size(); offset += 128) {
                var paths = files.subList(offset, Math.min(files.size(), offset + 128));
                var indexes = new HashMap<URI, Integer>();
                for (int i = 0; i < paths.size(); i++) indexes.put(paths.get(i).toUri(), offset + i);
                var diagnostics = new DiagnosticCollector<JavaFileObject>();
                var task = (JavacTask) compiler.getTask(null, manager, diagnostics, List.of("-proc:none"),
                        null, manager.getJavaFileObjectsFromPaths(paths));
                var units = task.parse();
                var positions = Trees.instance(task).getSourcePositions();
                var errors = new HashMap<URI, Integer>();
                for (var diagnostic : diagnostics.getDiagnostics()) {
                    if (diagnostic.getKind() == Diagnostic.Kind.ERROR && diagnostic.getSource() != null) {
                        errors.merge(diagnostic.getSource().toUri(), 1, Integer::sum);
                    }
                }
                for (var unit : units) {
                    var spans = new ArrayList<Span>();
                    new TreePathScanner<Void, Void>() {
                        private void add(char kind, Tree tree) {
                            if (tree == null) return;
                            long start = positions.getStartPosition(unit, tree);
                            long end = positions.getEndPosition(unit, tree);
                            if (start >= 0 && end > start) spans.add(new Span(kind, (int) start, (int) end));
                        }
                        private boolean classMember() {
                            return getCurrentPath().getParentPath().getLeaf() instanceof ClassTree;
                        }
                        @Override public Void visitMethod(MethodTree tree, Void unused) {
                            add(tree.getReturnType() == null ? 'C' : 'M', tree.getBody());
                            return super.visitMethod(tree, unused);
                        }
                        @Override public Void visitBlock(BlockTree tree, Void unused) {
                            if (classMember()) add('B', tree);
                            return super.visitBlock(tree, unused);
                        }
                        @Override public Void visitVariable(VariableTree tree, Void unused) {
                            if (classMember()) add('F', tree.getInitializer());
                            return super.visitVariable(tree, unused);
                        }
                        @Override public Void visitClass(ClassTree tree, Void unused) {
                            add('X', tree);
                            return super.visitClass(tree, unused);
                        }
                        @Override public Void visitImport(ImportTree tree, Void unused) {
                            add('P', tree);
                            return super.visitImport(tree, unused);
                        }
                    }.scan(unit, null);
                    URI key = unit.getSourceFile().toUri();
                    var row = new StringJoiner("\t").add(indexes.get(key).toString())
                            .add(errors.getOrDefault(key, 0).toString());
                    for (var span : spans) row.add(span.toString());
                    System.out.println(row);
                }
            }
        }
    }
}
"""

if __name__ == "__main__":
    main()
