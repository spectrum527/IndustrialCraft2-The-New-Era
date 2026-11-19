"""Check the isolated protocol-version variant and its actual negotiation results."""

from hashlib import sha256
import json
from pathlib import Path
import re
import time

MISMATCH = re.compile(
    r"Channel \[(ic2:echo_(?:request|reply))\] failed to connect: .*"
    r"wants the payload to be version: (\d+), but the server wants it to be version: (\d+)!"
)

def _class_hashes(folder):
    root = Path(folder)
    return {path.relative_to(root).as_posix(): sha256(path.read_bytes()).hexdigest()
            for path in root.rglob("*.class")}


def check_variant(manifest):
    production = _class_hashes(manifest["productionClasses"])
    variant = _class_hashes(manifest["mismatchClasses"])
    if production.keys() != variant.keys():
        raise AssertionError("The incompatible client added or removed class files")
    changed = [name for name, digest in production.items() if variant[name] != digest]
    if changed != [manifest["registrarClass"]]:
        raise AssertionError(f"Unexpected classes changed in the incompatible client: {changed}")
    registrar = changed[0]
    return {
        "comparedClasses": len(production),
        "changedClass": registrar,
        "productionSha256": production[registrar],
        "isolatedClientSha256": variant[registrar],
    }


def await_incompatible(run, timeout):
    deadline = time.monotonic() + timeout
    client_log = run.directory / "client.log"
    configured_versions = set(run.protocols.values())
    if len(configured_versions) != 2:
        raise AssertionError("The incompatible run uses the same protocol on both sides")
    while time.monotonic() < deadline:
        evidence = {}
        for line in client_log.read_text(errors="replace").splitlines():
            match = MISMATCH.search(line)
            if match is None:
                continue
            channel, reported_client, reported_server = match.groups()
            if {reported_client, reported_server} == configured_versions:
                evidence[channel] = {
                    "line": line,
                    "reportedClientVersion": reported_client,
                    "reportedServerVersion": reported_server,
                }
        if set(evidence) == {"ic2:echo_request", "ic2:echo_reply"}:
            return evidence
        if run.client.poll() is not None or run.server.poll() is not None:
            raise RuntimeError("A process exited before establishing the incompatible-handshake result")
        time.sleep(0.25)
    raise TimeoutError("The client did not report both IC2 protocol mismatches")


def verify_incompatible(run, evidence):
    server_log = (run.directory / "server.log").read_text(errors="replace")
    joins = server_log.count("joined the game")
    logins = server_log.count("logged in with entity id")
    echoes = (server_log + (run.directory / "client.log").read_text(errors="replace")).count("IC2 echo completed on")
    server_rejections = [
        line for line in server_log.splitlines()
        if "ServerConfigurationPacketListenerImpl" in line and "lost connection: Incompatible client" in line
    ]
    if not server_rejections:
        raise AssertionError("No dedicated server configuration-stage rejection was recorded")
    if joins or logins:
        raise AssertionError("An incompatible client entered play")
    if echoes:
        raise AssertionError("An echo handler ran on an incompatible connection")
    report = {
        "clientProtocol": run.protocols["client"],
        "serverProtocol": run.protocols["server"],
        "variant": run.variant,
        "played": bool(joins or logins),
        "joins": joins,
        "logins": logins,
        "echoes": echoes,
        "serverRejections": server_rejections,
        "evidence": evidence,
    }
    (run.directory / "mismatch-result.json").write_text(json.dumps(report, indent=2) + "\n")
