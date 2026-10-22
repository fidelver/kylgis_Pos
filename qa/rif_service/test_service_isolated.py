#!/usr/bin/env python3
"""Real MariaDB TLS service acceptance, synthetic fixtures and owned resources only."""
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from hashlib import sha256
import json
import os
from pathlib import Path
import secrets
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import uuid

ROOT = Path(__file__).resolve().parent
IMAGE = "docker.io/library/mariadb:12.3.3"
checks = 0

def ck(value, label):
    global checks
    if not value:
        raise AssertionError(label)
    checks += 1
    print("PASS " + label, flush=True)

def run(argv, *, input=None, timeout=20):
    return subprocess.run(argv, input=input, text=True, capture_output=True, timeout=timeout, check=True)

class Fixture:
    def __init__(self):
        self.root = Path(tempfile.mkdtemp(prefix="kylgis-rif-service-fixture-"))
        self.container = "kylgis-rif-service-" + uuid.uuid4().hex[:10]
        self.process = None
        self.owned = False
        self.log = None
    def setup(self):
        r = self.root
        for d in ("server", "client", "admin", "spool"):
            (r / d).mkdir(mode=0o700)
        (r / "key").write_bytes(secrets.token_bytes(32))
        os.chmod(r / "key", 0o600)
        self.password = secrets.token_hex(20)
        self.readerpass = secrets.token_hex(20)
        (r / "server.env").write_text("MARIADB_ROOT_PASSWORD=" + self.password + "\n")
        os.chmod(r / "server.env", 0o600)
        (r / "admin/root.cnf").write_text("[client]\nuser=root\npassword=" + self.password + "\nprotocol=socket\n")
        (r / "client/reader.cnf").write_text("[client]\nuser=kylgis_recipe_audit_rif\npassword=" + self.readerpass + "\n")
        for path in (r / "admin/root.cnf", r / "client/reader.cnf"):
            os.chmod(path, 0o600)
        def openssl(*args):
            return run(["openssl", *args])
        openssl("req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "2", "-sha256",
                "-keyout", str(r / "ca.key"), "-out", str(r / "client/ca.crt"), "-subj", "/CN=KylGis-Synthetic-Service")
        openssl("req", "-newkey", "rsa:2048", "-nodes", "-keyout", str(r / "server/server.key"),
                "-out", str(r / "server.csr"), "-subj", "/CN=localhost")
        (r / "server.ext").write_text("subjectAltName=DNS:localhost\nextendedKeyUsage=serverAuth\n")
        openssl("x509", "-req", "-in", str(r / "server.csr"), "-CA", str(r / "client/ca.crt"),
                "-CAkey", str(r / "ca.key"), "-CAcreateserial", "-days", "2", "-sha256",
                "-extfile", str(r / "server.ext"), "-out", str(r / "server/server.crt"))
        shutil.copyfile(r / "client/ca.crt", r / "server/ca.crt")
        self.original = (r / "server/server.crt").read_bytes()
        # All server-readable keys are ephemeral, synthetic and removed at cleanup.
        for path in (r / "server").iterdir():
            os.chmod(path, 0o644)
        os.chmod(r, 0o711)
        os.chmod(r / "server", 0o711)
        run(["podman", "run", "-d", "--rm", "--pull=never", "--name", self.container,
             "--network=none", "--read-only", "--memory=600m", "--cpus=2",
             "--label", "kylgis.qa.scope=rif-service-synthetic-only",
             "--tmpfs", "/var/lib/mysql:rw,size=384m", "--tmpfs", "/tmp:rw,size=64m",
             "--tmpfs", "/run/mysqld:rw,size=16m",
             "--env-file", str(r / "server.env"),
             "-v", str(r / "server") + ":/server:ro,z",
             "-v", str(r / "admin") + ":/admin:ro,z", IMAGE,
             "--bind-address=127.0.0.1", "--skip-name-resolve",
             "--ssl-ca=/server/ca.crt", "--ssl-cert=/server/server.crt", "--ssl-key=/server/server.key"])
        self.owned = True
        for _ in range(100):
            check = subprocess.run(["podman", "exec", self.container, "mariadb", "--defaults-file=/admin/root.cnf", "--skip-ssl", "-e", "SELECT 1"],
                                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            log_result = run(["podman", "logs", self.container])
            logs = log_result.stdout + log_result.stderr
            if check.returncode == 0 and "MariaDB init process done. Ready for start up." in logs:
                break
            time.sleep(.3)
        else:
            raise RuntimeError("DB_START_TIMEOUT")
        self.sql("""CREATE DATABASE kylgis;
CREATE TABLE kylgis.applications(ID VARCHAR(64),VERSION VARCHAR(20)) ENGINE=InnoDB;
CREATE TABLE kylgis.products_bundle(PRODUCT VARCHAR(255),PRODUCT_BUNDLE VARCHAR(255),QUANTITY DECIMAL(12,3)) ENGINE=InnoDB;
CREATE TABLE kylgis.recipe_versions(id VARCHAR(64),product_id VARCHAR(255),status VARCHAR(20)) ENGINE=InnoDB;
INSERT INTO kylgis.applications VALUES('fixture','1.3');
INSERT INTO kylgis.products_bundle VALUES('PIZZA','SALSA',0.2),('SALSA','TOMATE',1.5);
INSERT INTO kylgis.recipe_versions VALUES('r1','SALSA','ACTIVE'),('r2','PAN','DRAFT');
CREATE USER 'kylgis_recipe_audit_rif'@'127.0.0.1' IDENTIFIED BY '""" + self.readerpass + """' REQUIRE SSL;
GRANT SELECT ON kylgis.applications TO 'kylgis_recipe_audit_rif'@'127.0.0.1';
GRANT SELECT ON kylgis.products_bundle TO 'kylgis_recipe_audit_rif'@'127.0.0.1';
GRANT SELECT ON kylgis.recipe_versions TO 'kylgis_recipe_audit_rif'@'127.0.0.1';""")
        os.chmod(r, 0o700)
        image_id = run(["podman", "image", "inspect", "--format", "{{.Id}}", IMAGE]).stdout.strip()
        if not image_id.startswith("sha256:"):
            image_id = "sha256:" + image_id
        podman = str(Path(shutil.which("podman")).resolve())
        client_sha = run(["podman", "run", "--rm", "--network=none", "--read-only", image_id,
                          "sha256sum", "/usr/bin/mariadb"]).stdout.split()[0]
        self.release = r / "service_release"
        (self.release / "vendor").mkdir(parents=True, mode=0o700)
        for source in [ROOT / "private_service.py", *sorted((ROOT / "vendor").glob("*.py"))]:
            target = self.release / source.relative_to(ROOT)
            shutil.copyfile(source, target)
            os.chmod(target, 0o444)
        manifest = {str(p.relative_to(ROOT)): sha256(p.read_bytes()).hexdigest()
                    for p in [ROOT / "private_service.py", *sorted((ROOT / "vendor").glob("*.py"))]}
        leaf_der = subprocess.run(["openssl", "x509", "-in", str(r / "server/server.crt"), "-outform", "DER"], capture_output=True, check=True).stdout
        self.cfg = {"scope": "ISOLATED_SYNTHETIC_ONLY", "container": self.container,
                    "image_id": image_id, "client_sha256": client_sha,
                    "podman": podman, "podman_sha256": sha256(Path(podman).read_bytes()).hexdigest(),
                    "ca_sha256": sha256((r / "client/ca.crt").read_bytes()).hexdigest(),
                    "leaf_sha256": sha256(leaf_der).hexdigest(), "manifest": manifest, "ttl": 180}
        self.start()
    def sql(self, statement):
        return run(["podman", "exec", "-i", self.container, "mariadb", "--defaults-file=/admin/root.cnf", "--skip-ssl", "-N", "-B"], input=statement).stdout
    def start(self, **changes):
        self.stop()
        self.cfg.update(changes)
        path = self.root / "config.json"
        path.write_text(json.dumps(self.cfg))
        os.chmod(path, 0o600)
        (self.root / "service.sock").unlink(missing_ok=True)
        self.log = open(self.root / "service.log", "ab")
        self.process = subprocess.Popen([sys.executable, "-B", str(self.release / "private_service.py"), str(path)],
                                        stdout=self.log, stderr=self.log, env={**os.environ, "PYTHONDONTWRITEBYTECODE": "1"})
        for _ in range(500):
            if (self.root / "service.sock").exists():
                ck(self.process.pid != os.getpid(), "R05_SERVICE_SEPARATE_PROCESS")
                return
            if self.process.poll() is not None:
                raise RuntimeError("SERVICE_START_FAILED")
            time.sleep(.03)
        raise RuntimeError("SERVICE_START_TIMEOUT")
    def stop(self):
        if self.process:
            self.process.terminate()
            try:
                self.process.wait(timeout=3)
            except subprocess.TimeoutExpired:
                self.process.kill()
                self.process.wait(timeout=3)
            self.process = None
        if self.log:
            self.log.close()
            self.log = None
    def request(self, value):
        with socket.socket(socket.AF_UNIX) as conn:
            conn.settimeout(25)
            conn.connect(str(self.root / "service.sock"))
            conn.sendall(json.dumps(value).encode() + b"\n")
            data = b""
            while not data.endswith(b"\n"):
                chunk = conn.recv(65536)
                if not chunk:
                    raise RuntimeError("INCOMPLETE_SERVICE_RESPONSE")
                data += chunk
                if len(data) > 4 * 1024 * 1024:
                    raise RuntimeError("SERVICE_RESPONSE_LIMIT")
            return json.loads(data)
    def capture(self):
        response = self.request({"action": "capture"})
        ck(response["ok"], "SERVICE_REAL_PINNED_SQL_CAPTURE")
        return response["result"]["capture_id"]
    def consume(self, cid):
        return self.request({"action": "consume", "capture_id": cid})
    def rotate(self, expired=False):
        r = self.root
        if expired:
            from cryptography import x509
            from cryptography.hazmat.primitives import hashes, serialization
            ca = x509.load_pem_x509_certificate((r / "client/ca.crt").read_bytes())
            key = serialization.load_pem_private_key((r / "ca.key").read_bytes(), password=None)
            csr = x509.load_pem_x509_csr((r / "server.csr").read_bytes())
            begin, end = ((2040, 2041) if expired == "future" else (2020, 2020))
            cert = (x509.CertificateBuilder().subject_name(csr.subject).issuer_name(ca.subject)
                    .public_key(csr.public_key()).serial_number(x509.random_serial_number())
                    .not_valid_before(datetime(begin,1,1,tzinfo=timezone.utc)).not_valid_after(datetime(end,1,2,tzinfo=timezone.utc))
                    .add_extension(x509.SubjectAlternativeName([x509.DNSName("localhost")]), critical=False)
                    .sign(key, hashes.SHA256()))
            (r / "server/server.crt").write_bytes(cert.public_bytes(serialization.Encoding.PEM))
        else:
            run(["openssl", "x509", "-req", "-in", str(r / "server.csr"), "-CA", str(r / "client/ca.crt"),
                 "-CAkey", str(r / "ca.key"), "-set_serial", "12345678", "-days", "2", "-sha256",
                 "-extfile", str(r / "server.ext"), "-out", str(r / "server/server.crt")])
        self.sql("FLUSH SSL;")
    def restore_cert(self):
        (self.root / "server/server.crt").write_bytes(self.original)
        self.sql("FLUSH SSL;")
    def cleanup(self):
        self.stop()
        if (self.root / "service.log").exists():
            for line in (self.root / "service.log").read_text(errors="replace").splitlines():
                if line.startswith(("STAGING_", "ValueError:", "ModuleNotFoundError:", "SyntaxError:", "PermissionError:")):
                    print(line, flush=True)
        if self.owned:
            run(["podman", "stop", "-t", "5", self.container])
            ck(subprocess.run(["podman", "container", "exists", self.container]).returncode != 0, "SERVICE_OWNED_CONTAINER_REMOVED")
        shutil.rmtree(self.root)
        print("RIF_SERVICE_FIXTURE_RESOURCES_CLEANED", flush=True)

def main():
    f = Fixture()
    try:
        f.setup()
        ck(not f.request({"action": "capture", "stdout": "forged", "tls_proven": True})["ok"], "R05_FORGED_PHASE_RESULT_REJECTED_AT_SERVICE")
        ck(not f.request({"action": "capture", "sql": "SELECT 1"})["ok"], "R05_PUBLIC_SQL_REJECTED")
        cid = f.capture()
        result = f.consume(cid)
        ck(result["ok"], "R19_AUTHENTIC_PROCESS_CAPTURE_CONSUMED")
        phases = result["result"]["receipt"]["phases"]
        ck(len(phases)==2 and phases[0]["connection_id"]!=phases[1]["connection_id"]
           and all(not p["transaction_open_after_sql"] for p in phases), "R19_SAME_SESSION_SQL_END_MARKER_PROVES_CLOSED_TRX")
        ck(not f.consume(cid)["ok"], "R26_REPLAY_BLOCKED")
        f.start()
        ck(not f.consume(cid)["ok"], "R26_REPLAY_BLOCKED_AFTER_SERVICE_RESTART")
        ck((f.root / "spool").stat().st_mode & 0o077 == 0 and (f.root / "service.sock").stat().st_mode & 0o077 == 0, "R26_PRIVATE_SPOOL_AND_SOCKET_ACL")
        cid = f.capture()
        os.chmod(f.root / "spool", 0o755)
        ck(not f.consume(cid)["ok"], "R26_RUNTIME_SPOOL_ACL_WEAKENING_REJECTED")
        os.chmod(f.root / "spool", 0o700)
        ck(f.consume(cid)["ok"], "R26_ACL_RESTORED_UNCONSUMED_CAPTURE_REMAINS_VALID")
        client_sha = f.cfg["client_sha256"]
        f.start(client_sha256="0"*64)
        ck(not f.request({"action":"capture"})["ok"], "R06_ACTUAL_MARIADB_BINARY_PIN_MISMATCH_REJECTED")
        f.start(client_sha256=client_sha)
        original = f.cfg["leaf_sha256"]
        f.start(leaf_sha256="0"*64)
        ck(not f.request({"action":"capture"})["ok"], "R09_WRONG_PIN_REJECTED_BY_SERVICE_SQL_SESSION")
        f.start(leaf_sha256=original, ttl=1)
        cid = f.capture()
        time.sleep(1.1)
        ck(not f.consume(cid)["ok"], "R26_REAL_CLOCK_EXPIRATION_REJECTED")
        f.start(ttl=180)
        cid = f.capture()
        artifact = f.root / "spool" / (cid + ".json")
        data = json.loads(artifact.read_text())
        data["snapshot"]["bundles"]["rows"][0]["quantity"] = "999"
        artifact.write_text(json.dumps(data))
        ck(not f.consume(cid)["ok"], "R23_MIXED_OR_MUTATED_SNAPSHOT_REJECTED")
        with ThreadPoolExecutor(max_workers=2) as pool:
            ids = list(pool.map(lambda _: f.capture(), range(2)))
        ck(len(set(ids)) == 2 and all(f.consume(x)["ok"] for x in ids), "R24_TWO_CONCURRENT_CAPTURES_HAVE_DISTINCT_PRIVATE_SPOOL")
        # A source-manifest mismatch fails at service startup, without issuing SQL.
        saved = dict(f.cfg["manifest"])
        broken = dict(saved)
        broken["private_service.py"] = "0"*64
        try:
            f.start(manifest=broken)
        except RuntimeError:
            ck(True, "R06_SOURCE_MANIFEST_MISMATCH_BLOCKS_SERVICE")
        else:
            raise AssertionError("R06_BAD_MANIFEST_ACCEPTED")
        f.start(manifest=saved, between_phase_pause=True)
        before = set((f.root / "spool").glob("*.preflight"))
        with ThreadPoolExecutor(max_workers=1) as pool:
            pending = pool.submit(f.request, {"action": "capture"})
            until = time.monotonic()+15
            while set((f.root / "spool").glob("*.preflight")) == before:
                if time.monotonic()>until:
                    raise RuntimeError("PREFLIGHT_SIGNAL_TIMEOUT")
                time.sleep(.03)
            f.rotate()
            ck(not pending.result()["ok"], "R11_ROTATION_BETWEEN_SERVICE_PHASES_REJECTED")
        f.restore_cert()
        f.rotate(expired=True)
        der = subprocess.run(["openssl","x509","-in",str(f.root/"server/server.crt"),"-outform","DER"],capture_output=True,check=True).stdout
        f.start(between_phase_pause=False, leaf_sha256=sha256(der).hexdigest())
        ck(not f.request({"action":"capture"})["ok"], "R10_PRESENTED_EXPIRED_CERT_REJECTED_BY_SERVICE")
        f.restore_cert()
        f.rotate(expired="future")
        der = subprocess.run(["openssl","x509","-in",str(f.root/"server/server.crt"),"-outform","DER"],capture_output=True,check=True).stdout
        f.start(leaf_sha256=sha256(der).hexdigest())
        ck(not f.request({"action":"capture"})["ok"], "R10_PRESENTED_NOT_YET_VALID_CERT_REJECTED_BY_SERVICE")
        f.restore_cert()
        f.start(leaf_sha256=original, mvcc_pause=True)
        with ThreadPoolExecutor(max_workers=1) as pool:
            pending = pool.submit(f.request, {"action":"capture"})
            until = time.monotonic()+15
            while "DO SLEEP(2)" not in f.sql("SHOW FULL PROCESSLIST;"):
                if time.monotonic()>until:
                    raise RuntimeError("MVCC_SLEEP_NOT_OBSERVED")
                time.sleep(.03)
            f.sql("INSERT INTO kylgis.products_bundle VALUES('CONCURRENT','WRITER',1);")
            response = pending.result()
            ck(response["ok"], "R30_SERVICE_CAPTURE_COMPLETES_WITH_CONCURRENT_WRITER")
            result = f.consume(response["result"]["capture_id"])
            ck(result["ok"] and result["result"]["snapshot"]["bundles"]["declared_count"] == 2
               and f.sql("SELECT COUNT(*) FROM kylgis.products_bundle;").strip() == "3", "R30_COUNTS_AND_ROWS_SHARE_REAL_MVCC_SNAPSHOT")
        print("RIF_SERVICE_STAGING_ACCEPTANCE_PASS checks=" + str(checks), flush=True)
    finally:
        f.cleanup()

if __name__ == "__main__":
    main()
