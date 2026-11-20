"""Publish a generated pack without exposing partial JSON files to another Gradle process."""
from contextlib import contextmanager
from pathlib import Path
import os
import tempfile


@contextmanager
def lock_file(path):
    with path.open("a+b") as handle:
        if os.name == "nt":
            import msvcrt
            if not path.stat().st_size:
                handle.write(b"0")
                handle.flush()
            handle.seek(0)
            msvcrt.locking(handle.fileno(), msvcrt.LK_LOCK, 1)
            try:
                yield
            finally:
                handle.seek(0)
                msvcrt.locking(handle.fileno(), msvcrt.LK_UNLCK, 1)
        else:
            import fcntl
            fcntl.flock(handle, fcntl.LOCK_EX)
            try:
                yield
            finally:
                fcntl.flock(handle, fcntl.LOCK_UN)


def publish(staged, destination):
    """Commit only files owned by the generator, preserving other callers' files."""
    names = {path.relative_to(staged).as_posix() for path in staged.rglob("*.json")}
    marker = destination / ".ic2-generated-output"
    with lock_file(destination.parent / ("." + destination.name + ".lock")):
        if destination.exists() and not destination.is_dir():
            raise SystemExit("Resource output must be a directory")
        destination.mkdir(parents=True, exist_ok=True)
        if marker.is_file():
            previous = set(marker.read_text(encoding="utf-8").splitlines()[1:])
        elif any(destination.iterdir()):
            # Gradle can prune tracked outputs at the start of another task, including our marker.
            # Only the conventional generated build folder may recover by adopting its JSON files.
            if not (destination.name == "ic2-resources" and destination.parent.name == "generated"):
                raise SystemExit("Choose an empty directory for the generated pack")
            previous = set()
        else:
            previous = set()
        for name in sorted(names):
            target = destination / name
            target.parent.mkdir(parents=True, exist_ok=True)
            os.replace(staged / name, target)
        for name in previous - names:
            rel = Path(name)
            if not rel.is_absolute() and ".." not in rel.parts and rel.suffix == ".json":
                (destination / rel).unlink(missing_ok=True)
        ready = staged / marker.name
        ready.write_text("IC2 generated resources\n" + "\n".join(sorted(names)) + "\n", encoding="utf-8")
        os.replace(ready, marker)
