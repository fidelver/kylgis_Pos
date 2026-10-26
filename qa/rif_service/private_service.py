#!/usr/bin/env python3
"""Private Unix-socket service for SYNTHETIC RIF/TLS staging; never MCP/live RIF."""
from contextlib import contextmanager
from hashlib import sha256
import hmac
import json
import os
from pathlib import Path
import re
import socket
import socketserver
import stat
import struct
import subprocess
import sys
import threading
import time
import uuid

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT / "vendor"))
# Trusted dependencies are imported only after verifying their manifest.

MODE = "ISOLATED_SERVICE_STAGING"
UUID_RE = re.compile(r"[a-f0-9]{32}\Z")
ACTIVE = threading.BoundedSemaphore(2)

def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"), allow_nan=False).encode()

def private_bytes(path):
    fd = os.open(path, os.O_RDONLY | os.O_NOFOLLOW)
    try:
        info = os.fstat(fd)
        if not stat.S_ISREG(info.st_mode) or info.st_uid != os.geteuid() or info.st_mode & 0o077:
            raise ValueError("PRIVATE_FILE")
        if info.st_size > 65536:
            raise ValueError("PRIVATE_FILE_SIZE")
        return os.read(fd, 65537)
    finally:
        os.close(fd)

def private_dir(path):
    info = path.lstat()
    if not stat.S_ISDIR(info.st_mode) or info.st_uid != os.geteuid() or info.st_mode & 0o077:
        raise ValueError("PRIVATE_DIRECTORY")

def durable_new(path, payload):
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600)
    try:
        with os.fdopen(fd, "wb", closefd=False) as out:
            out.write(payload)
            out.flush()
            os.fsync(fd)
    finally:
        os.close(fd)
    fd = os.open(path.parent, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
    try:
        os.fsync(fd)
    finally:
        os.close(fd)

@contextmanager
def pinned_binary(path, expected):
    fd = os.open(path, os.O_RDONLY | os.O_NOFOLLOW)
    try:
        info = os.fstat(fd)
        if not stat.S_ISREG(info.st_mode) or info.st_mode & 0o022 or info.st_nlink != 1:
            raise ValueError("EXECUTABLE_METADATA")
        digest = sha256()
        while True:
            part = os.read(fd, 65536)
            if not part:
                break
            digest.update(part)
        after = os.fstat(fd)
        if (info.st_ino, info.st_size, info.st_mtime_ns, info.st_ctime_ns) != (after.st_ino, after.st_size, after.st_mtime_ns, after.st_ctime_ns) or digest.hexdigest() != expected:
            raise ValueError("EXECUTABLE_PIN")
        yield "/proc/self/fd/" + str(fd), fd
    finally:
        os.close(fd)

class Backend:
    def __init__(self, config_path):
        self.config_path = config_path
        self.root = config_path.parent
        if not str(self.root).startswith("/tmp/kylgis-rif-service-fixture-"):
            raise ValueError("SYNTHETIC_FIXTURE_ONLY")
        private_dir(self.root)
        self.cfg = json.loads(private_bytes(config_path))
        if self.cfg["scope"] != "ISOLATED_SYNTHETIC_ONLY" or not re.fullmatch(r"sha256:[a-f0-9]{64}", self.cfg["image_id"]):
            raise ValueError("FIXTURE_MANIFEST")
        if not self.cfg["container"].startswith("kylgis-rif-service-"):
            raise ValueError("FIXTURE_CONTAINER")
        self.spool = self.root / "spool"
        private_dir(self.spool)
        self.key = private_bytes(self.root / "key")
        if len(self.key) != 32:
            raise ValueError("FIXTURE_KEY")
        self.manifest = self.cfg["manifest"]
        self.manifest_sha = sha256(canonical(self.manifest)).hexdigest()
        self.verify_manifest()
        global runner, reader
        import kylgis_recipe_bundle_private_runner_candidate as runner
        import kylgis_recipe_bundle_rif_reader_candidate as reader

    def verify_manifest(self):
        private_dir(self.root)
        private_dir(self.spool)
        expected = {str(p.relative_to(ROOT)) for p in (ROOT / "vendor").glob("*.py")} | {"private_service.py"}
        if set(self.manifest) != expected:
            raise ValueError("RELEASE_MANIFEST_FILES")
        for relative, digest in self.manifest.items():
            p = ROOT / relative
            if p.is_symlink() or sha256(p.read_bytes()).hexdigest() != digest:
                raise ValueError("RELEASE_MANIFEST_CHANGED")

    def invoke(self, args, phase):
        with pinned_binary(self.cfg["podman"], self.cfg["podman_sha256"]) as (exe, fd):
            result = runner.bounded_process_pinned(
                args, phase, executable=exe, pass_fds=(fd,), seconds=12)
        if result.returncode != 0 or result.timed_out or result.output_exceeded:
            print("STAGING_PHASE_RESULT " + phase + " rc=" + str(result.returncode), file=sys.stderr, flush=True)
        runner.inspect_phase(result, phase)
        return result

    def capture(self):
        self.verify_manifest()
        cid = uuid.uuid4().hex
        started = time.monotonic()
        # Image content address fixes the query-bearing CLI rootfs, even if tags move.
        inspected = subprocess.run([self.cfg["podman"], "inspect", self.cfg["container"]],
                                   capture_output=True, timeout=3, check=True)
        instance = json.loads(inspected.stdout)[0]
        if instance["HostConfig"]["NetworkMode"] != "none" or instance["Config"]["Labels"].get("kylgis.qa.scope") != "rif-service-synthetic-only":
            raise ValueError("FIXTURE_NETWORK")
        image = self.cfg["image_id"]
        binary = subprocess.run([self.cfg["podman"], "run", "--rm", "--pull=never",
                                 "--network=none", "--read-only", image,
                                 "sha256sum", "/usr/bin/mariadb"],
                                capture_output=True, timeout=5, check=True)
        if binary.stdout.decode().split()[0] != self.cfg["client_sha256"]:
            raise ValueError("CLIENT_IMAGE_BINARY_PIN")
        if sha256((self.root / "client/ca.crt").read_bytes()).hexdigest() != self.cfg["ca_sha256"]:
            raise ValueError("CA_PIN")
        prefix = [self.cfg["podman"], "run", "--rm", "--pull=never", "--read-only",
                  "--network=container:" + self.cfg["container"],
                  "-v", str(self.root / "client") + ":/client:ro,z",
                  image, "mariadb", "--defaults-file=/client/reader.cnf",
                  "--ssl", "--ssl-verify-server-cert", "--ssl-ca=/client/ca.crt",
                  "--ssl-fp=" + self.cfg["leaf_sha256"], "--batch", "--raw", "--skip-column-names",
                  "--skip-reconnect", "--connect-timeout=2", "--database=kylgis",
                  "--protocol=tcp", "--host=localhost", "--port=3306"]
        results, closes = [], []
        for phase in ("preflight", "capture"):
            statements = list(reader.ALLOWED_PHASES[phase])
            if phase == "capture" and self.cfg.get("mvcc_pause"):
                statements.insert(5, "DO SLEEP(2)")
            sql = ";\n".join(statements) + ";\nSELECT 'E','" + cid + "',CONNECTION_ID(),@@in_transaction;\n"
            result = self.invoke(prefix + ["--execute=" + sql], phase)
            text = result.stdout.decode("utf-8", "strict")
            lines = text.splitlines()
            end = lines.pop().split("\t")
            if len(end) != 4 or end[:2] != ["E", cid] or end[3] != "0":
                raise ValueError("TRANSACTION_END_NOT_PROVEN")
            identity = next(line.split("\t") for line in lines if line.startswith("I\t"))
            if identity[5] != end[2]:
                raise ValueError("TRANSACTION_SESSION_MIXED")
            closes.append({"phase": phase, "connection_id": end[2], "transaction_open_after_sql": False,
                           "stdout_sha256": result.stdout_sha256})
            results.append("\n".join(lines) + "\n")
            if phase == "preflight":
                durable_new(self.spool / (cid + ".preflight"), b"private synthetic phase complete\n")
                if self.cfg.get("between_phase_pause"):
                    time.sleep(2)
        if time.monotonic() - started > 20:
            raise ValueError("TOTAL_DEADLINE")
        pre = reader.parse_preflight(results[0])
        captured = reader.parse_capture(results[1], pre)
        receipt = {"mode": MODE, "capture_id": cid, "issued_at": int(time.time()),
                   "expires_at": int(time.time()) + self.cfg.get("ttl", 180),
                   "manifest_sha256": self.manifest_sha,
                   "snapshot_sha256": sha256(canonical(captured["snapshot"])).hexdigest(),
                   "phases": closes, "certifies_real_rif": False,
                   "image_id": image, "client_sha256": self.cfg["client_sha256"],
                   "leaf_sha256": self.cfg["leaf_sha256"], "ca_sha256": self.cfg["ca_sha256"]}
        artifact = {"snapshot": captured["snapshot"], "receipt": receipt}
        artifact["signature"] = hmac.new(self.key, canonical(artifact), sha256).hexdigest()
        durable_new(self.spool / (cid + ".json"), canonical(artifact))
        return {"capture_id": cid, "mode": MODE, "snapshot_sha256": receipt["snapshot_sha256"]}

    def consume(self, cid):
        self.verify_manifest()
        if type(cid) is not str or not UUID_RE.fullmatch(cid):
            raise ValueError("CAPTURE_ID")
        artifact = json.loads(private_bytes(self.spool / (cid + ".json")))
        signed = {"snapshot": artifact["snapshot"], "receipt": artifact["receipt"]}
        if not hmac.compare_digest(artifact["signature"], hmac.new(self.key, canonical(signed), sha256).hexdigest()):
            raise ValueError("SIGNATURE")
        receipt = artifact["receipt"]
        if receipt["capture_id"] != cid or receipt["manifest_sha256"] != self.manifest_sha or receipt["mode"] != MODE:
            raise ValueError("CAPTURE_BINDING")
        if not receipt["issued_at"] <= time.time() < receipt["expires_at"]:
            raise ValueError("EXPIRED")
        if sha256(canonical(artifact["snapshot"])).hexdigest() != receipt["snapshot_sha256"]:
            raise ValueError("SNAPSHOT_BINDING")
        durable_new(self.spool / (cid + ".consumed"), b"consumed\n")
        return artifact

    def dispatch(self, request):
        if type(request) is not dict:
            raise ValueError("REQUEST")
        if request == {"action": "health"}:
            return {"mode": MODE, "production_enabled": False}
        if request == {"action": "capture"}:
            if not ACTIVE.acquire(blocking=False):
                raise ValueError("BUSY")
            try:
                return self.capture()
            finally:
                ACTIVE.release()
        if set(request) == {"action", "capture_id"} and request["action"] == "consume":
            return self.consume(request["capture_id"])
        raise ValueError("CALLER_EVIDENCE_OR_SQL_FORBIDDEN")

class Handler(socketserver.StreamRequestHandler):
    def handle(self):
        self.connection.settimeout(3)
        peer = struct.unpack("3i", self.connection.getsockopt(socket.SOL_SOCKET, socket.SO_PEERCRED, 12))
        if peer[1] != os.geteuid():
            return
        try:
            raw = self.rfile.readline(4097)
            if len(raw) > 4096 or not raw.endswith(b"\n"):
                raise ValueError("BOUNDED_REQUEST")
            response = {"ok": True, "result": self.server.backend.dispatch(json.loads(raw))}
        except Exception as failure:
            print("STAGING_REQUEST_REJECTED_CLASS=" + type(failure).__name__, file=sys.stderr, flush=True)
            response = {"ok": False, "error": "REQUEST_REJECTED"} # no raw SQL, rows, credentials
        self.wfile.write(canonical(response) + b"\n")

class Server(socketserver.ThreadingUnixStreamServer):
    daemon_threads = True

def main():
    config = Path(sys.argv[1]).resolve()
    backend = Backend(config)
    endpoint = backend.root / "service.sock"
    if endpoint.exists():
        raise ValueError("EXISTING_SOCKET")
    with Server(str(endpoint), Handler) as service:
        os.chmod(endpoint, 0o600)
        service.backend = backend
        service.serve_forever(poll_interval=0.1)

if __name__ == "__main__":
    main()
