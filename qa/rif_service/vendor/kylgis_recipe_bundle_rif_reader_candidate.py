#!/usr/bin/env python3
"""Isolated RIF-only recipe/bundle reader/parser candidate, no release ID.

The only public dispatch operation returns its static contract. No DB process
can be started by this candidate; run_live is explicitly fail-closed pending
trusted TLS leaf/SAN verification and a leader-assigned release approval.
The parser consumes synthetic MariaDB CLI output for isolated QA.
"""
from __future__ import annotations
from decimal import Decimal, InvalidOperation
import hashlib
import json
import re

import kylgis_recipe_bundle_rif_tls_gates_candidate as sec
import kylgis_recipe_bundle_rif_capture_contract_candidate as fmt

SCHEMA="kylgis"
PROFILE="rif"
CNF="/home/server-conf/.config/kylgis/pos-recipe-audit-rif.cnf"
MARIADB="/usr/bin/mariadb"
EXPECTED_PORT="3306"
CLIENT_MAX_OUTPUT=4*1024*1024
MAX_ROWS=10000
# Subprocess argv is static and can never come from an MCP caller.
COMMON_FLAGS=("--batch","--raw","--skip-column-names",
              "--connect-timeout=2","--skip-reconnect",
              "--max-allowed-packet=4194304", "--database=kylgis")

# Each phase MUST be a separate TLS-authenticated client process;
# each transcript comes from one process/session, not stitched pieces.
PREFLIGHT_SQL=(
 "SELECT 'I',HEX(DATABASE()),@@port,HEX(CURRENT_USER()),HEX(@@hostname),CONNECTION_ID()",
 "SHOW SESSION STATUS LIKE 'Ssl_cipher'",
 "SHOW GRANTS FOR CURRENT_USER",
 "SELECT 'T',HEX(TABLE_NAME),HEX(TABLE_TYPE),HEX(COALESCE(ENGINE,'')) "
 "FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME "
 "IN ('applications','products_bundle','recipe_versions') ORDER BY TABLE_NAME",
)
CAPTURE_SQL=(
 "SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ",
 "START TRANSACTION WITH CONSISTENT SNAPSHOT, READ ONLY",
 "SELECT 'I',HEX(DATABASE()),@@port,HEX(CURRENT_USER()),HEX(@@hostname),CONNECTION_ID()",
 "SHOW SESSION STATUS LIKE 'Ssl_cipher'",
 "SELECT 'C',(SELECT COUNT(*) FROM products_bundle),"
 "(SELECT COUNT(*) FROM recipe_versions),"
 "(SELECT COUNT(*) FROM applications),"
 "(SELECT MIN(VERSION) FROM applications),"
 "(SELECT MAX(VERSION) FROM applications)",
 "SELECT 'B',HEX(PRODUCT),HEX(PRODUCT_BUNDLE),CAST(QUANTITY AS CHAR) "
 "FROM products_bundle ORDER BY PRODUCT,PRODUCT_BUNDLE LIMIT 10001",
 "SELECT 'R',HEX(product_id),HEX(status),HEX(id) "
 "FROM recipe_versions ORDER BY product_id,id LIMIT 10001",
 "COMMIT",
)
ALLOWED_PHASES={"preflight":PREFLIGHT_SQL,"capture":CAPTURE_SQL}
HEX=re.compile(r"[0-9A-Fa-f]+\Z")
COUNT=re.compile(r"(?:0|[1-9][0-9]*)\Z")
CLAIMED_HOST="192.168.58.18"

class ReaderError(ValueError):pass

def static_sql(phase):
 if type(phase) is not str or phase not in ALLOWED_PHASES:
  raise ReaderError("invalid phase")
 return ";\n".join(ALLOWED_PHASES[phase])+";\n"

def static_cli_args(ca_proof):
 flags=sec.tls_flags(ca_proof)
 # Only an operator-approved, privately verified DER leaf SHA256 is accepted.
 # MariaDB 12.3 validates --ssl-fp during the actual query-bearing TLS session;
 # a separate OpenSSL probe must never substitute for this binding.
 if type(ca_proof) is not dict or type(ca_proof.get("leaf_sha256")) is not str:
  raise ReaderError("NO_CERTIFICABLE_TLS: approved leaf pin missing")
 approved=sec.trust_pins(ca_proof["sha256"],ca_proof["leaf_sha256"])
 return [MARIADB,"--defaults-file="+CNF,*flags,
         "--ssl-fp="+approved["approved_leaf_sha256"],*COMMON_FLAGS]

def _records(text,max_bytes=None):
 if max_bytes is None:max_bytes=CLIENT_MAX_OUTPUT
 if type(text) is not str or len(text.encode("utf-8"))>max_bytes:
  raise ReaderError("invalid or oversized transcript")
 if "\x00" in text or "\r" in text:
  raise ReaderError("control characters in transcript")
 return [line.split("\t") for line in text.splitlines()]

def _decode(hextext,label):
 if type(hextext) is not str or len(hextext)>2048 or not HEX.fullmatch(hextext) or len(hextext)%2:
  raise ReaderError("invalid HEX "+label)
 try:result=bytes.fromhex(hextext).decode("utf-8","strict")
 except UnicodeError as exc:raise ReaderError("invalid UTF8 "+label) from exc
 if not result or len(result)>255 or "\x00" in result:
  raise ReaderError("invalid decoded "+label)
 return result

def _integer(value,label,max_value=MAX_ROWS):
 if type(value) is not str or not COUNT.fullmatch(value) or len(value)>12:
  raise ReaderError("invalid integer "+label)
 result=int(value)
 if result>max_value:raise ReaderError("row bound "+label)
 return result

def _identity(row):
 if len(row)!=6 or row[0]!="I":
  raise ReaderError("identity record missing or malformed")
 schema=_decode(row[1],"schema")
 user=_decode(row[3],"account")
 host=_decode(row[4],"hostname")
 port=_integer(row[2],"port",65535)
 connection=_integer(row[5],"connection id",2**31)
 if schema!=SCHEMA or port!=3306 or not host or user.split("@",1)[0]!=sec.USER:
  raise ReaderError("wrong RIF identity")
 return {"schema":schema,"user":user,"hostname":host,
         "port":port,"connection_id":str(connection)}

def _tls_record(records):
 hits=[r for r in records if r and r[0]=="Ssl_cipher"]
 if len(hits)!=1 or len(hits[0])!=2 or not hits[0][1].strip():
  raise ReaderError("NO_CERTIFICABLE_TLS: Ssl_cipher absent/empty")
 if len(hits[0][1])>128 or any(c in hits[0][1] for c in ("\n","\x00")):
  raise ReaderError("invalid TLS cipher")
 return hits[0][1]

def parse_preflight(transcript):
 rows=_records(transcript,max_bytes=64*1024)
 ids=[r for r in rows if r and r[0]=="I"]
 if len(ids)!=1:raise ReaderError("ambiguous preflight identity")
 identity=_identity(ids[0])
 cipher=_tls_record(rows)
 grants=[r[0] for r in rows if len(r)==1 and r[0].startswith("GRANT ")]
 sec.grants(grants,identity["user"],require_ssl=True)
 meta={}
 for row in rows:
  if not row or row[0]!="T":continue
  if len(row)!=4:raise ReaderError("invalid TABLE record")
  name=_decode(row[1],"table")
  kind=_decode(row[2],"table type")
  engine=_decode(row[3],"table engine")
  if name in meta:raise ReaderError("duplicate TABLE metadata")
  meta[name]=(kind,engine)
 expected_tags={"I","T","Ssl_cipher"}
 for row in rows:
  if not row:raise ReaderError("empty record")
  if row[0] not in expected_tags and not (len(row)==1 and row[0].startswith("GRANT ")):
   raise ReaderError("unexpected preflight protocol record")
 missing=sec.TABLES-set(meta)
 if missing:
  return {"status":"NO_RECIPES_SCHEMA","complete":False,
          "missing_tables":sorted(missing),"cipher":cipher}
 if set(meta)!=sec.TABLES or any(info!=("BASE TABLE","InnoDB") for info in meta.values()):
  raise ReaderError("unexpected table engine/type/scope")
 return {"status":"READY","schema":SCHEMA,"identity":identity,"cipher":cipher,
         "engine_by_table":{key:"InnoDB" for key in sorted(meta)},
         "complete":True}

def parse_capture(transcript,preflight):
 if type(preflight) is not dict or preflight.get("status")!="READY":
  raise ReaderError("missing authorized preflight")
 rows=_records(transcript)
 ids=[r for r in rows if r and r[0]=="I"]
 if len(ids)!=1:raise ReaderError("ambiguous capture identity")
 identity=_identity(ids[0])
 cipher=_tls_record(rows)
 first=preflight["identity"]
 if identity["user"]!=first["user"] or identity["hostname"]!=first["hostname"]:
  raise ReaderError("authenticated identity changed")
 if identity["connection_id"]==first["connection_id"]:
  raise ReaderError("same session used for both phases")
 if cipher!=preflight["cipher"]:
  raise ReaderError("TLS cipher inconsistent between phases")
 counters=[r for r in rows if r and r[0]=="C"]
 if len(counters)!=1 or len(counters[0])!=6:
  raise ReaderError("missing or ambiguous counts")
 c=counters[0]
 nb=_integer(c[1],"bundle count")
 nr=_integer(c[2],"recipe count")
 apps=_integer(c[3],"applications count",1)
 if apps!=1 or c[4:]!=["1.3","1.3"]:
  raise ReaderError("unexpected application version")
 bundles=[];recipes=[]
 for row in rows:
  if row[0]=="B":
   if len(row)!=4:raise ReaderError("bad bundle width")
   bundles.append({"product":_decode(row[1],"bundle parent"),
                   "component":_decode(row[2],"bundle component"),
                   "quantity":row[3]})
  elif row[0]=="R":
   if len(row)!=4:raise ReaderError("bad recipe width")
   _decode(row[3],"recipe id")
   recipes.append({"product":_decode(row[1],"recipe product"),
                   "status":_decode(row[2],"recipe status")})
  elif row[0] not in {"I","C","Ssl_cipher"}:
   raise ReaderError("unexpected capture protocol record")
 if nb!=len(bundles) or nr!=len(recipes):
  raise ReaderError("truncated/inconsistent captured rows")
 tx_id="session-"+identity["connection_id"]
 meta={"profile":"rif","schema":"kylgis","application_count":1,
       "application_min_version":c[4],"application_max_version":c[5],
       "engine_by_table":preflight["engine_by_table"],
       "bundle_count":nb,"recipe_count":nr,
       "source_tx_read_only":True,"source_tx_consistent":True,
       "source_tx_id":tx_id}
 out=fmt.prepare_audit_snapshot(meta,bundles,recipes)
 out["source_tx_read_only_declared_by_fixed_sql"]=True
 out["untrusted_transcript_fixture"]=True
 out["requires_authenticated_private_tls_runner"]=True
 return out

def run_live():
 raise ReaderError("NO_GO: trusted SAN/leaf pin TLS adapter, identity and "
                   "operator authorization not provisioned")

def dispatch(request):
 if request=={"action":"recipe-bundle-rif-reader-contract"}:
  return {"family":"recipe-bundle-rif-only","release_id":"PENDING_LEADER",
          "preflight_sql_fixed":True,"capture_sql_fixed":True,
          "profile":"rif","tls_server_auth_required":True,
          "live_execution_allowed":False,"production_access":False,
          "sql_freeform":False}
 raise ReaderError("no deployed collector")
