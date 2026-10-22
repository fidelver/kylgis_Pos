#!/usr/bin/env python3
"""Owned synthetic database, real Swing components, distinct JVM suspension/recovery."""
import argparse
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import time
import uuid
import zipfile

ROOT = Path(__file__).resolve().parents[2]
JDK = Path("/usr/lib/jvm/temurin-21-jdk")
def run(argv, *, timeout=30, input=None, env=None):
    p = subprocess.run(argv, input=input, text=True, stdout=subprocess.PIPE,
                       stderr=subprocess.STDOUT, timeout=timeout, env=env)
    if p.returncode:
        raise RuntimeError("COMMAND_FAILED rc=%d %s\n%s" % (p.returncode, argv[0], p.stdout[-10000:]))
    return p.stdout

def dependencies(core):
    with zipfile.ZipFile(core) as jar:
        manifest = jar.read("META-INF/MANIFEST.MF").decode().replace("\r\n ", "").replace("\n ", "")
    entry = next(x for x in manifest.splitlines() if x.startswith("Class-Path: "))
    names = [Path(x).name for x in entry[len("Class-Path: "):].split()]
    wanted = set(names)
    found = {}
    for base in [Path("/home/fam/.m2/repository"), ROOT / "vendor/maven"]:
        for path in base.rglob("*.jar"):
            if path.name in wanted:
                found.setdefault(path.name, path)
    missing = wanted - found.keys()
    if missing:
        raise RuntimeError("MISSING_OFFLINE_DEPENDENCIES " + str(sorted(missing)))
    return [found[n] for n in names]

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--core-jar", required=True)
    ap.add_argument("--core-sha256", required=True)
    ap.add_argument("--mariadb", choices=["10.11", "11.8", "12.3.3"], default="10.11")
    ap.add_argument("--verify-baseline", action="store_true",
                    help="The prior sealed panel must fail the origin-cash assertion.")
    args = ap.parse_args()
    core = Path(args.core_jar).resolve()
    if "target" in core.parts or hashlib.sha256(core.read_bytes()).hexdigest() != args.core_sha256:
        raise RuntimeError("REFUSE_UNSEALED_CORE")
    if not os.environ.get("DISPLAY"):
        raise RuntimeError("DISPLAY_REQUIRED")
    image = "docker.io/library/mariadb:" + args.mariadb
    run(["podman", "image", "exists", image])
    deps = dependencies(core)
    tmp = Path(tempfile.mkdtemp(prefix="kylgis-swing-recovery-"))
    tag = uuid.uuid4().hex[:12]
    name, network = "kylgis-swing-" + tag, "kylgis-swing-net-" + tag
    made_network = made_container = False
    try:
        # Freeze the exact Java sources before compiling or starting any JVM.
        sources = [
            ROOT / "src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java",
            ROOT / "src/main/java/com/mx/kylgis/pos/sales/shared/JTicketsBagShared.java",
            ROOT / "qa/manual/SwingPaymentRecoveryProbe.java"]
        frozen = []
        for src in sources:
            dest = tmp / src.name
            data = src.read_bytes()
            dest.write_bytes(data)
            print("SOURCE_SHA256 " + src.name + "=" + hashlib.sha256(data).hexdigest(), flush=True)
            frozen.append(str(dest))
        classes = tmp / "classes"
        classes.mkdir()
        cp = ":".join([str(core)] + [str(p) for p in deps])
        run([str(JDK / "bin/javac"), "--release", "21", "-cp", cp,
             "-d", str(classes)] + frozen, timeout=90)
        print("SWING_RECOVERY_COMPILED_REAL_PANEL", flush=True)
        run(["podman", "network", "create", "--internal", network])
        made_network = True
        run(["podman", "run", "-d", "--rm", "--pull=never", "--name", name,
             "--network", network, "--publish", "127.0.0.1::3306",
             "--label", "kylgis.qa.scope=swing-recovery-" + tag,
             "--memory", "600m", "--cpus", "2",
             "--tmpfs", "/var/lib/mysql:rw,size=384m",
             "--env", "MARIADB_ALLOW_EMPTY_ROOT_PASSWORD=1", image])
        made_container = True
        for _ in range(100):
            probe = subprocess.run(["podman", "exec", name, "mariadb", "-uroot",
                                    "--protocol=SOCKET", "--skip-ssl", "-e", "SELECT 1"],
                                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            logs = run(["podman", "logs", name])
            if probe.returncode == 0 and "MariaDB init process done. Ready for start up." in logs:
                break
            time.sleep(.4)
        else:
            raise RuntimeError("SYNTHETIC_DB_NOT_READY")
        binding = run(["podman", "port", name, "3306/tcp"]).strip()
        if not binding.startswith("127.0.0.1:") or "\n" in binding:
            raise RuntimeError("REFUSE_NON_LOOPBACK_BINDING")
        port = str(int(binding.split(":")[1]))
        def sql(statement):
            return run(["podman", "exec", "-i", name, "mariadb", "-uroot",
                        "--protocol=SOCKET", "--skip-ssl"], input=statement)
        sql("""CREATE DATABASE pos_qa_swing;
CREATE TABLE pos_qa_swing.sharedtickets(
 ID VARCHAR(255) PRIMARY KEY,NAME VARCHAR(255),CONTENT MEDIUMBLOB,
 APPUSER VARCHAR(255),PICKUPID INT,LOCKED VARCHAR(255)) ENGINE=InnoDB;
CREATE USER 'qa_swing'@'%' IDENTIFIED BY '';
GRANT SELECT,INSERT,UPDATE,DELETE ON pos_qa_swing.* TO 'qa_swing'@'%';""")
        env = os.environ.copy()
        env["KYLGIS_QA_SCOPE"] = "ONLY_SYNTHETIC_SWING_RECOVERY"
        # Same compatibility opens as src/scripts/kylgis-java.sh for legacy WebLaF.
        opens = ["--add-opens=" + p + "=ALL-UNNAMED" for p in [
            "java.base/java.util", "java.base/java.lang.reflect", "java.base/java.text",
            "java.desktop/java.awt.font", "java.desktop/java.awt", "java.desktop/javax.swing.table"]]
        java = [str(JDK / "bin/java")] + opens + ["-Xmx256m", "-Duser.home=" + str(tmp),
                "-Djava.util.prefs.userRoot=" + str(tmp / "prefs")]
        def phase(mode, *, baseline=False, forced=False):
            command = java + (["-Dkylgis.qa.forceFailure=true"] if forced else [])
            classpath = (str(tmp / "baseline") if baseline else str(classes)) + ":" + cp
            p = subprocess.Popen(command + ["-cp", classpath, "SwingPaymentRecoveryProbe", port, mode],
                                 stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, env=env)
            try:
                output, _ = p.communicate(timeout=40)
            except subprocess.TimeoutExpired:
                p.kill()
                p.communicate(timeout=5)
                raise RuntimeError("SWING_JVM_TIMEOUT " + mode)
            if Path("/proc/" + str(p.pid)).exists():
                raise RuntimeError("SWING_PID_NOT_REAPED")
            if "pid=" + str(p.pid) + "," in run(["/usr/sbin/ss", "-ntp"]):
                raise RuntimeError("SWING_SOCKET_LEFT")
            if baseline:
                expected = {
                    "split-regression": "SPLIT_BLOCKED_BEFORE_PAYMENT_OR_SPLIT_DIALOG",
                    "delete-regression": "DELETE_BLOCKED_BEFORE_PAYMENT_OR_SPLIT_DIALOG",
                    "empty-regression": "EMPTY_FINANCIAL_CONTEXT_RETAINED",
                    "recover": "ORIGIN_CASH_PRESERVED_ON_OTHER_CASHBOX"}[mode]
                if p.returncode != 1 or expected not in output:
                    raise RuntimeError("BASELINE_NOT_REPRODUCED\n" + output[-8000:])
                print("PASS SEALED_BASELINE_REPRODUCES_" + expected, flush=True)
            elif forced:
                if p.returncode != 1 or "INTENTIONAL_SWING_QA_FAILURE" not in output:
                    raise RuntimeError("NEGATIVE_GATE_NOT_EFFECTIVE\n" + output[-8000:])
                print("PASS INJECTED_SWING_FAILURE_RETURNS_ONE", flush=True)
            elif p.returncode or "SWING_PAYMENT_RECOVERY_PASS phase=" + mode not in output:
                raise RuntimeError("SWING_QA_FAILED rc=%d\n%s" % (p.returncode, output[-10000:]))
            else:
                for line in output.splitlines():
                    if line.startswith(("PASS ", "SWING_PAYMENT_")):
                        print(line, flush=True)
            print("PASS SWING_JVM_EXITED_AND_SOCKETS_RELEASED " + mode, flush=True)
        if args.verify_baseline:
            baseline = tmp / "baseline"
            baseline.mkdir()
            run([str(JDK / "bin/javac"), "--release", "21", "-cp", cp, "-d", str(baseline),
                 frozen[-1]], timeout=90)
            phase("split-regression", baseline=True)
            phase("delete-regression", baseline=True)
            phase("empty-regression", baseline=True)
            phase("suspend")
            phase("recover", baseline=True)
            sql("DELETE FROM pos_qa_swing.sharedtickets;")
        phase("suspend")
        print("PASS FIRST_SWING_JVM_EXITED_BEFORE_RECOVERY", flush=True)
        phase("recover")
        # The next process recovers the ticket that the previous process re-suspended.
        phase("recover")
        phase("suspend-empty")
        phase("recover-empty")
        sql("DELETE FROM pos_qa_swing.sharedtickets;")
        phase("suspend", forced=True)
        print("SWING_PAYMENT_RECOVERY_ISOLATED_PASS mariadb=" + args.mariadb, flush=True)
    finally:
        try:
            if made_container:
                run(["podman", "stop", "-t", "5", name])
                if subprocess.run(["podman", "container", "exists", name]).returncode == 0:
                    raise RuntimeError("SWING_QA_CONTAINER_LEFT")
        finally:
            try:
                if made_network:
                    run(["podman", "network", "rm", network])
            finally:
                shutil.rmtree(tmp)
        print("SWING_OWNED_QA_RESOURCES_CLEANED", flush=True)

if __name__ == "__main__":
    main()
