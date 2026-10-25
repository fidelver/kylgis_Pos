#!/usr/bin/env python3
"""Closed RIF recipe/bundle capture format candidate — offline, no release ID.

Turns already-collected, privately certified rows into exact schema=1 JSON
required by KylGis POS offline auditor da99b65. It does NOT connect to LAB or
authenticate TLS, and is deliberately not an MCP data-export action.
The future authorized runner must supply complete same-transaction results.
"""
from __future__ import annotations
from decimal import Decimal, InvalidOperation
import hashlib
import json

import kylgis_recipe_bundle_rif_tls_gates_candidate as security

MAX_SOURCE_ROWS=10000
MAX_JSON_BYTES=4*1024*1024
EXPECTED_VERSION="1.3"
REQUIRED_META=frozenset({
 "profile","schema","application_count","application_min_version",
 "application_max_version","engine_by_table","bundle_count","recipe_count",
 "source_tx_read_only","source_tx_consistent","source_tx_id",
})

class CaptureError(ValueError):pass

def _id(value,label):
 if type(value) is not str or not value or not value.strip() or len(value)>255 or "\x00" in value:
  raise CaptureError("invalid "+label)
 return value

def _count(value,label):
 if type(value) is not int or value<0 or value>MAX_SOURCE_ROWS:
  raise CaptureError("unbounded/invalid "+label)
 return value

def _metadata(meta):
 if type(meta) is not dict or set(meta)!=REQUIRED_META:
  raise CaptureError("unexpected collector metadata")
 security.profile(meta["profile"])
 if meta["schema"]!=security.RIF:
  raise CaptureError("wrong source schema")
 if type(meta["application_count"]) is not int or meta["application_count"]!=1:
  raise CaptureError("application count not unique")
 if (meta["application_min_version"]!=EXPECTED_VERSION
     or meta["application_max_version"]!=EXPECTED_VERSION):
  raise CaptureError("application version mismatch")
 if type(meta["engine_by_table"]) is not dict or set(meta["engine_by_table"])!=security.TABLES:
  raise CaptureError("missing or extra schema tables")
 if any(engine!="InnoDB" for engine in meta["engine_by_table"].values()):
  raise CaptureError("nontransactional schema table")
 if (meta["source_tx_read_only"] is not True
     or meta["source_tx_consistent"] is not True
     or type(meta["source_tx_id"]) is not str or not meta["source_tx_id"]):
  raise CaptureError("consistent read-only capture not proven")
 _count(meta["bundle_count"],"bundles")
 _count(meta["recipe_count"],"recipes")

def _bundle(row):
 if type(row) is not dict or set(row)!={"product","component","quantity"}:
  raise CaptureError("unexpected bundle columns")
 product=_id(row["product"],"bundle product")
 component=_id(row["component"],"bundle component")
 try:amount=Decimal(str(row["quantity"]))
 except (ValueError,InvalidOperation,TypeError) as exc:
  raise CaptureError("invalid bundle quantity") from exc
 if not amount.is_finite() or amount<=0:
  raise CaptureError("invalid bundle quantity")
 # Reject exponent bombs before converting Decimal to fixed-point string.
 if abs(amount.as_tuple().exponent)>30 or abs(amount.adjusted())>30:
  raise CaptureError("excessive quantity precision or magnitude")
 # Decimal str is stable, no floats in JSON; avoid arbitrary-precision abuse.
 if len(str(amount))>128:
  raise CaptureError("oversize bundle quantity")
 return {"product":product,"component":component,"quantity":format(amount.normalize(),"f")}

def _recipe(row):
 if type(row) is not dict or set(row)!={"product","status"}:
  raise CaptureError("unexpected recipe columns")
 product=_id(row["product"],"recipe product")
 status=row["status"]
 if type(status) is not str or status.upper() not in {"ACTIVE","DRAFT","RETIRED"}:
  raise CaptureError("invalid recipe status")
 return {"product":product,"status":status.upper()}

def prepare_audit_snapshot(meta,bundle_rows,recipe_rows):
 """PURE conversion. Caller MUST be the trusted runner, not the MCP client."""
 _metadata(meta)
 if type(bundle_rows) is not list or type(recipe_rows) is not list:
  raise CaptureError("expected complete lists")
 if len(bundle_rows)!=meta["bundle_count"] or len(recipe_rows)!=meta["recipe_count"]:
  raise CaptureError("count mismatch/truncated capture")
 if len(bundle_rows)>MAX_SOURCE_ROWS or len(recipe_rows)>MAX_SOURCE_ROWS:
  raise CaptureError("capture exceeds row bounds")
 bundles=[_bundle(row) for row in bundle_rows]
 recipes=[_recipe(row) for row in recipe_rows]
 # Keep intentional duplicate edges/versions, which POS auditor must detect.
 bundles.sort(key=lambda x:(x["product"],x["component"],x["quantity"]))
 recipes.sort(key=lambda x:(x["product"],x["status"]))
 result={"schema":1,
         "bundles":{"complete":True,"truncated":False,
                    "declared_count":len(bundles),"rows":bundles},
         "recipes":{"complete":True,"truncated":False,
                    "declared_count":len(recipes),"rows":recipes}}
 canonical=json.dumps(result,ensure_ascii=False,sort_keys=True,
                      separators=(",",":"),allow_nan=False).encode("utf-8")
 if len(canonical)>MAX_JSON_BYTES:
  raise CaptureError("capture JSON exceeds 4MiB")
 # This digest gives replay/integrity, NOT authentication or proof of origin.
 digest=hashlib.sha256(canonical).hexdigest()
 return {"status":"OFFLINE_FORMAT_VALID",
         "snapshot":result,"canonical_sha256":digest,
         "canonical_bytes":len(canonical),
         "complete":True,"db_contacted":False,
         "authentication_proven":False,"tls_proven":False}

def capture_contract():
 return {"family":"recipe-bundle-rif-only","release":"PENDING_LEADER",
         "schema":1,"auditor_commit":"da99b65b213647c54a4e1df6d7ee0f8ad7ee8cf9",
         "required_collections":["bundles","recipes"],
         "bundle_columns":["product","component","quantity"],
         "recipe_columns":["product","status"],
         "max_rows_each":MAX_SOURCE_ROWS,"max_json_bytes":MAX_JSON_BYTES,
         "source_tx":"REPEATABLE READ CONSISTENT SNAPSHOT READ ONLY",
         "network":False,"mcp_export_enabled":False,
         "operational":False,"production_contacted":False}

def dispatch(req):
 if req=={"action":"recipe-bundle-rif-capture-contract"}:
  return capture_contract()
 raise CaptureError("no online collector actions")
