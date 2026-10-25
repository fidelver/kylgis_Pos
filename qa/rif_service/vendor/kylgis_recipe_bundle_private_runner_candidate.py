#!/usr/bin/env python3
"""RIF recipe/bundle private runner CANDIDATE for release audit; no release ID.

Not installed or registered. run_live() and dispatch() explicitly deny operation.
This is a REVIEWABLE backend: fixed SQL, bounded subprocess, no shell, verified
client binary and CA/leaf pins; caller-owned phase transcript/proof is NEVER
an MCP input. No LAB credentials, approved binary digest or CA provided yet.
"""
from __future__ import annotations
from dataclasses import dataclass
from hashlib import sha256
import hmac
import json
import os
from pathlib import Path
import selectors
import signal
import stat
import subprocess
import time

import kylgis_recipe_bundle_rif_reader_candidate as reader

MAX_PREFLIGHT = 64 * 1024
MAX_CAPTURE = 4 * 1024 * 1024
MAX_STDERR = 2048
MAX_PHASE_SECONDS = 12
TOTAL_SECONDS = 20
MAX_TOTAL_STDOUT = MAX_PREFLIGHT + MAX_CAPTURE
EXPECTED_CLIENT_BASENAME = "mariadb"
SAFE_LOCALE = "C.UTF-8"

class ClosedRunnerError(RuntimeError):
    """Failure must not include raw SQL output, DB rows or credentials."""

@dataclass(frozen=True)
class PhaseResult:
    phase: str
    returncode: int
    elapsed_ms: int
    stdout: bytes
    stdout_sha256: str
    stderr_sha256: str
    argv_sha256: str
    timed_out: bool = False
    output_exceeded: bool = False

def _canonical(obj):
    return json.dumps(obj,ensure_ascii=True,sort_keys=True,
                      separators=(",",":"),allow_nan=False).encode("utf8")

def _hash(data):
    return sha256(data).hexdigest()

def _client_identity(client_path, approved_binary_sha):
    """Fail closed for symlink/stale/writable or unapproved client binary."""
    if type(client_path) is not str or type(approved_binary_sha) is not str:
        raise ClosedRunnerError("approved CLI identity missing")
    p=Path(client_path)
    if (not p.is_absolute() or p.name != EXPECTED_CLIENT_BASENAME
        or len(approved_binary_sha)!=64
        or any(x not in "0123456789abcdef" for x in approved_binary_sha)
        or approved_binary_sha=="0"*64):
        raise ClosedRunnerError("unapproved CLI path/digest")
    fd=os.open(p,os.O_RDONLY|os.O_NOFOLLOW|os.O_NONBLOCK)
    try:
        a=os.fstat(fd)
        if (not stat.S_ISREG(a.st_mode) or a.st_nlink!=1 or not a.st_mode&0o111
            or a.st_mode & 0o022 or a.st_size>100*1024*1024):
            raise ClosedRunnerError("untrusted CLI file metadata")
        digest=sha256()
        while True:
            chunk=os.read(fd,65536)
            if not chunk:break
            digest.update(chunk)
        b=os.fstat(fd)
        if ((a.st_dev,a.st_ino,a.st_size,a.st_mtime_ns,a.st_ctime_ns)
            !=(b.st_dev,b.st_ino,b.st_size,b.st_mtime_ns,b.st_ctime_ns)
            or digest.hexdigest()!=approved_binary_sha):
            raise ClosedRunnerError("client changed or digest not approved")
        return p
    finally:
        os.close(fd)

def _phase_spec(phase, *, reader_module=reader):
    if phase not in ("preflight","capture"):
        raise ClosedRunnerError("only two fixed phases")
    sql=reader_module.static_sql(phase)
    if not sql or not sql.endswith(";\n"):
        raise ClosedRunnerError("malformed fixed phase SQL")
    return sql, MAX_PREFLIGHT if phase=="preflight" else MAX_CAPTURE

def prepare_private_argv(phase,ca_proof,approved_binary_sha):
    """Review-stage static argv builder; never takes SQL or destination from MCP."""
    sql,_=_phase_spec(phase)
    args=reader.static_cli_args(ca_proof)
    if not args or args[0]!=reader.MARIADB:
        raise ClosedRunnerError("unsupported client executable")
    _client_identity(args[0],approved_binary_sha)
    if not any(x.startswith("--ssl-fp=") and len(x)==len("--ssl-fp=")+64 for x in args):
        raise ClosedRunnerError("missing same-session SHA256 leaf pin")
    if "--ssl-verify-server-cert" not in args or "--ssl" not in args:
        raise ClosedRunnerError("unverified TLS client")
    if any(x.startswith("--execute") for x in args):
        raise ClosedRunnerError("untrusted SQL in argv")
    return args+["--execute="+sql]

def bounded_process(argv, phase, *, seconds=MAX_PHASE_SECONDS,
                    env=None, _executable=None, _pass_fds=()):
    """Read both subprocess pipes with strict byte/time caps and kill group.

    This low-level helper cannot authorize DB; the surrounding runner must
    first authenticate binary hash, profile, grants, pinned leaf and credentials.
    No user-controlled command reaches it through dispatch().
    """
    if (type(argv) is not list or len(argv)<5
        or any(type(x) is not str or not x for x in argv)
        or type(seconds) not in (int,float) or not 0<seconds<=MAX_PHASE_SECONDS):
        raise ClosedRunnerError("invalid private process specification")
    _,max_out=_phase_spec(phase)
    if env is None:env={"PATH":"/usr/bin:/bin","LANG":SAFE_LOCALE}
    if type(env) is not dict or set(env)!={"PATH","LANG"}:
        raise ClosedRunnerError("unsafe process environment")
    # NEVER include stdout/stderr in error records. Credentials are in a
    # root-owned options file; process argv contains no passwords.
    start=time.monotonic()
    hasher_out=sha256();hasher_err=sha256()
    stdout=bytearray()
    timed_out=exceeded=False
    proc=subprocess.Popen(argv,stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,
                          stderr=subprocess.PIPE,cwd="/",env=env,
                          close_fds=True,start_new_session=True,
                          executable=_executable,pass_fds=_pass_fds)
    sel=selectors.DefaultSelector()
    try:
        for fd,tag in ((proc.stdout,"out"),(proc.stderr,"err")):
            os.set_blocking(fd.fileno(),False)
            sel.register(fd,selectors.EVENT_READ,data=tag)
        deadline=start+seconds
        error_bytes=0
        while sel.get_map():
            remaining=deadline-time.monotonic()
            if remaining<=0:
                timed_out=True;break
            for key,_ in sel.select(timeout=min(remaining,.1)):
                chunk=os.read(key.fileobj.fileno(),16384)
                if not chunk:
                    sel.unregister(key.fileobj);continue
                if key.data=="out":
                    hasher_out.update(chunk)
                    if len(stdout)+len(chunk)>max_out:
                        exceeded=True;break
                    stdout.extend(chunk)
                else:
                    hasher_err.update(chunk)
                    error_bytes+=len(chunk)
                    if error_bytes>MAX_STDERR:
                        exceeded=True;break
            if exceeded:break
        if not timed_out and not exceeded:
            try:proc.wait(timeout=max(.05,deadline-time.monotonic()))
            except subprocess.TimeoutExpired:timed_out=True
    finally:
        if proc.poll() is None:
            try:os.killpg(proc.pid,signal.SIGKILL)
            except ProcessLookupError:pass
        try:proc.wait(timeout=1)
        except subprocess.TimeoutExpired:
            try:proc.kill()
            except ProcessLookupError:pass
            proc.wait(timeout=1)
        sel.close()
        proc.stdout.close()
        proc.stderr.close()
    return PhaseResult(phase,proc.returncode,
        int((time.monotonic()-start)*1000),bytes(stdout),
        hasher_out.hexdigest(),hasher_err.hexdigest(),
        _hash(_canonical(argv)),timed_out,exceeded)

def bounded_process_pinned(argv,phase,*,executable,pass_fds,seconds=MAX_PHASE_SECONDS,env=None):
    """Execute exactly an already-verified OPEN binary inode via Linux procfs.

    This operation is private to the staging service; no MCP action routes to it.
    """
    if (type(executable) is not str or not executable.startswith("/proc/self/fd/")
        or type(pass_fds) is not tuple or len(pass_fds)!=1
        or type(pass_fds[0]) is not int or pass_fds[0]<3
        or executable!="/proc/self/fd/"+str(pass_fds[0])):
        raise ClosedRunnerError("invalid pinned binary descriptor")
    try:
        mode=os.fstat(pass_fds[0]).st_mode
    except OSError as exc:
        raise ClosedRunnerError("verified executable no longer open") from exc
    if not stat.S_ISREG(mode):
        raise ClosedRunnerError("pinned binary fd not regular")
    return bounded_process(argv,phase,seconds=seconds,env=env,
                           _executable=executable,_pass_fds=pass_fds)

def inspect_phase(result,phase):
    if not isinstance(result,PhaseResult) or result.phase!=phase:
        raise ClosedRunnerError("phase mismatch / untrusted result")
    _,bound=_phase_spec(phase)
    if (result.returncode!=0 or result.timed_out or result.output_exceeded
        or result.elapsed_ms<0 or result.elapsed_ms>MAX_PHASE_SECONDS*1000
        or not result.stdout or len(result.stdout)>bound
        or _hash(result.stdout)!=result.stdout_sha256):
        raise ClosedRunnerError("phase failed/timeout/incomplete/excess output")
    if (len(result.stderr_sha256)!=64 or len(result.argv_sha256)!=64):
        raise ClosedRunnerError("missing process evidence hashes")
    try:transcript=result.stdout.decode("utf8","strict")
    except UnicodeDecodeError as exc:
        raise ClosedRunnerError("non UTF-8 phase transcript") from exc
    return transcript

def assemble_private_run(preflight, capture):
    """Use only runner-produced PhaseResult objects; not an external MCP action."""
    if (not isinstance(preflight,PhaseResult) or not isinstance(capture,PhaseResult)
        or preflight.elapsed_ms+capture.elapsed_ms>TOTAL_SECONDS*1000):
        raise ClosedRunnerError("total capture deadline exceeded")
    pre=inspect_phase(preflight,"preflight")
    # Do not publish anything if capture failed or is overbound.
    cap=inspect_phase(capture,"capture")
    try:
        ready=reader.parse_preflight(pre)
        if ready.get("status")!="READY":
            raise ClosedRunnerError("RIF schema not ready")
        result=reader.parse_capture(cap,ready)
    except (ValueError,TypeError,KeyError) as exc:
        raise ClosedRunnerError("internal transcript parsing rejected") from exc
    if result.get("status")!="OFFLINE_FORMAT_VALID" or result.get("complete") is not True:
        raise ClosedRunnerError("partial capture not publishable")
    evidence={"schema":1,"mode":"ISOLATED_FIXTURE_REVIEW_ONLY",
              "source_authenticity":False,"db_contacted_by_this_module":False,
              "phases":[{"phase":p.phase,"exit_code":p.returncode,
                         "elapsed_ms":p.elapsed_ms,
                         "stdout_sha256":p.stdout_sha256,
                         "stderr_sha256":p.stderr_sha256,
                         "argv_sha256":p.argv_sha256}
                        for p in (preflight,capture)],
              "snapshot_sha256":result["canonical_sha256"],
              "row_counts":{"bundles":result["snapshot"]["bundles"]["declared_count"],
                            "recipes":result["snapshot"]["recipes"]["declared_count"]}}
    return {"snapshot":result["snapshot"],"evidence":evidence,
            "evidence_sha256":_hash(_canonical(evidence)),
            "authenticity_claimed":False}

def seal_evidence(evidence,key):
    """HMAC using a secret solely from future runner's systemd credential.

    The candidate DOES NOT issue/store keys. It never validates user-supplied
    signature as evidence of a remote MariaDB session.
    """
    if type(key) is not bytes or len(key)<32:
        raise ClosedRunnerError("private seal key missing")
    if type(evidence) is not dict or evidence.get("mode")!="ISOLATED_FIXTURE_REVIEW_ONLY":
        raise ClosedRunnerError("unexpected evidence mode")
    return hmac.new(key,_canonical(evidence),sha256).hexdigest()

def verify_seal(evidence,key,signature):
    if type(signature) is not str or len(signature)!=64:
        return False
    try:return hmac.compare_digest(seal_evidence(evidence,key),signature)
    except ClosedRunnerError:return False

def capabilities():
    return {"family":"recipe-bundle-rif-private-runner",
            "release_id":"PENDING_LEADER",
            "execution_enabled":False,"lab_access":False,
            "production_access":False,"allows_external_sql":False,
            "max_phase_ms":MAX_PHASE_SECONDS*1000,
            "max_total_ms":TOTAL_SECONDS*1000,
            "max_preflight_bytes":MAX_PREFLIGHT,
            "max_capture_bytes":MAX_CAPTURE,
            "receipt_authenticity":"NOT_CERTIFIED",
            "needs_serverconf_approved_client_and_ca":True}

def run_live():
    raise ClosedRunnerError("NO_GO: private runner/CA/key/credentials not approved")

def dispatch(req):
    if req=={"action":"recipe-bundle-private-runner-capabilities"}:
        return capabilities()
    raise ClosedRunnerError("not registered: only offline QA candidate")
