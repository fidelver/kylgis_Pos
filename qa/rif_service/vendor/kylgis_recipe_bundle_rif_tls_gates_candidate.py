#!/usr/bin/env python3
"""Offline RIF-only recipe/bundle security gate candidate. NO release ID assigned.
Does not connect to DB, deploy, read credentials, run SQL or approve live TLS.
"""
import re
import hashlib
import os
import stat
from pathlib import Path

RIF="kylgis"
USER="kylgis_recipe_audit_rif"
TABLES=frozenset(("applications","products_bundle","recipe_versions"))
CA=Path("/home/server-conf/.config/kylgis/pos-recipe-audit-rif-ca.pem")
SHA=re.compile("[0-9a-f]{64}\\Z")
GRANT=re.compile(
 r"GRANT SELECT ON \x60?([A-Za-z0-9_]+)\x60?\.\x60?([A-Za-z0-9_]+)\x60?"
 r" TO ['\x60]?([A-Za-z0-9_]+)['\x60]?@['\x60]?([^'\x60 ]+)['\x60]?",re.I)

class GateError(ValueError):pass

def profile(name):
 if type(name) is not str or name!="rif":raise GateError("RIF only")
 return RIF

def grants(rows,current_user,*,require_ssl=False):
 if type(rows) is not list or not rows or type(current_user) is not str or "@" not in current_user:
  raise GateError("missing identity/grants")
 user,host=current_user.split("@",1)
 if user!=USER or not host or any(s in host for s in ("%","*","_",":")):
  raise GateError("unexpected/wildcard principal")
 found=set()
 for row in rows:
  if type(row) is not str or "\n" in row or "\r" in row:
   raise GateError("invalid grant")
  row=" ".join(row.strip().split())
  if row.upper().startswith("GRANT USAGE ON *.* TO "):
   suffix=row.split(" TO ",1)[1]
   principals=("'"+user+"'@'"+host+"'",user+"@"+host,"`"+user+"`@`"+host+"`")
   tail=None
   for subject in principals:
    if suffix.startswith(subject):
     tail=suffix[len(subject):]
     break
   if tail is None:
    raise GateError("USAGE principal mismatch")
   # Actual MariaDB SHOW GRANTS appends an authentication hash and REQUIRE SSL.
   # Accept only a properly shaped hash; never return it to callers.
   has_ssl=tail.endswith(" REQUIRE SSL")
   credential=tail[:-len(" REQUIRE SSL")] if has_ssl else tail
   if credential and not re.fullmatch(r" IDENTIFIED BY PASSWORD '[*][0-9A-Fa-f]{40}'",credential):
    raise GateError("unexpected USAGE authentication clause")
   if require_ssl and not has_ssl:
    raise GateError("dedicated account must REQUIRE SSL")
   continue
  m=GRANT.fullmatch(row)
  if not m:raise GateError("non-SELECT grant/option")
  db,table,subject,origin=m.groups()
  if db!=RIF or table not in TABLES or subject!=user or origin!=host:
   raise GateError("cross-schema/principal SELECT")
  if table in found:raise GateError("duplicate grant")
  found.add(table)
 if found!=TABLES:raise GateError("incomplete exact RIF grants")
 return {"scope_count":3,"select_only":True}

def trust_pins(ca_sha,leaf_sha):
 if not all(type(s) is str and SHA.fullmatch(s) and s!="0"*64 for s in (ca_sha,leaf_sha)):
  raise GateError("approved CA and leaf certificate SHA256 required")
 return {"approved_ca_sha256":ca_sha,"approved_leaf_sha256":leaf_sha}

def verify_pinned_ca(expected_sha256):
 """Only read a fixed private CA file, never client-selected CA/paths.

 Comparing file bytes to a separately approved digest is not proof of
 certificate validity or TLS negotiation; those require a trusted handshake.
 """
 if type(expected_sha256) is not str or not SHA.fullmatch(expected_sha256) or expected_sha256=="0"*64:
  raise GateError("approved trust anchor digest missing")
 fd=os.open(CA,os.O_RDONLY|os.O_NOFOLLOW|os.O_NONBLOCK)
 try:
  before=os.fstat(fd)
  if (not stat.S_ISREG(before.st_mode) or before.st_nlink!=1
     or before.st_uid!=os.geteuid() or before.st_mode&0o022
     or not 1<=before.st_size<=262144):
   raise GateError("CA file unsafe")
  hashed=hashlib.sha256()
  data=b""
  while True:
   piece=os.read(fd,8192)
   if not piece:break
   data+=piece
   if len(data)>262144:raise GateError("CA file too large")
   hashed.update(piece)
  after=os.fstat(fd)
  if ((before.st_dev,before.st_ino,before.st_size,before.st_mtime_ns,before.st_ctime_ns)
      !=(after.st_dev,after.st_ino,after.st_size,after.st_mtime_ns,after.st_ctime_ns)):
   raise GateError("CA changed while reading")
  if (hashed.hexdigest()!=expected_sha256 or b"-----BEGIN CERTIFICATE-----" not in data
       or b"-----END CERTIFICATE-----" not in data):
   raise GateError("CA digest or PEM structure invalid")
  return {"sha256":expected_sha256,"ca_file_verified":True,"handshake_verified":False}
 finally:os.close(fd)

def tls_flags(ca_proof=None):
 if (type(ca_proof) is not dict or ca_proof.get("ca_file_verified") is not True
     or type(ca_proof.get("sha256")) is not str or not SHA.fullmatch(ca_proof["sha256"])):
  raise GateError("pinned CA bytes not attested")
 return ["--ssl","--ssl-verify-server-cert","--ssl-ca="+str(CA)]

def tls_phase(phase,proof,approved_leaf):
 """Validate PRIVATE handshake attestation, never a client-provided assertion."""
 if phase not in ("preflight","capture") or type(proof) is not dict:
  raise GateError("invalid TLS phase")
 if (proof.get("schema")!=RIF or proof.get("principal")!=USER or proof.get("port")!=3306
     or proof.get("san_verified") is not True or proof.get("ca_verified") is not True
     or proof.get("handshake_verified") is not True
     or type(proof.get("cipher")) is not str or not proof["cipher"]
     or type(proof.get("session_id")) is not str or not proof["session_id"]
     or type(approved_leaf) is not str or not SHA.fullmatch(approved_leaf)
     or proof.get("leaf_sha256")!=approved_leaf):
  raise GateError("NO_CERTIFICABLE_TLS")
 return True

def tls_pair(preflight,capture,approved_leaf):
 tls_phase("preflight",preflight,approved_leaf)
 tls_phase("capture",capture,approved_leaf)
 if preflight["session_id"]==capture["session_id"]:
  raise GateError("independent session evidence missing")
 if preflight["cipher"]!=capture["cipher"]:
  raise GateError("cipher changed between sessions")
 return {"offline_gate_passed":True,"live_tls_certified":False}

def dispatch(req):
 if req=={"action":"recipe-bundle-rif-security-capabilities"}:
  return {"release_id":"PENDING_LEADER","candidate_only":True,"network":False,
          "writes":False,"sql":False,"profiles":["rif"]}
 raise GateError("not a deployed collector")
