#!/usr/bin/env python3
"""Auditoría offline, sin BD ni credenciales, del contrato BeanShell→SQL→Jasper.

Código de salida 2 = al menos un reporte con campos faltantes, ordinales
incompatibles o patrón SQL de multiplicación de filas conocido.
La ejecución no aprueba validez contable ni la presencia de datos reales.
"""
from __future__ import annotations

import argparse
import ast
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
REPORTS = ROOT / "src/main/resources/com/mx/kylgis/pos/reports"
QUOTED = re.compile(r'"(?:\\.|[^"\\])*"')
FIELD = re.compile(r'report\.addField\("([^"]+)"')
MAPPING = re.compile(r'report\.setReport\("([^"]+)"\)')
SELECT = re.compile(r'\bSELECT\b', re.I)
FROM = re.compile(r'\bFROM\b', re.I)
ALIAS = re.compile(r'\bAS\s+([A-Za-z_][A-Za-z0-9_]*)\s*$', re.I)


def strip_line_comments(text: str) -> str:
    return "\n".join(line for line in text.splitlines()
                     if not line.lstrip().startswith("//"))


def query_source(text: str) -> str:
    text = strip_line_comments(text)
    m = re.search(r"report\.setSentence\((.*?)\);", text, flags=re.S)
    if not m:
        return ""
    literals = QUOTED.findall(m.group(1))
    return "".join(ast.literal_eval(value) for value in literals)


def top_level_select(sql: str) -> list[str]:
    """Cuenta expresiones SELECT sin confundir comas dentro de funciones."""
    m = SELECT.search(sql)
    if not m:
        return []
    depth = 0
    quote = False
    start = m.end()
    segments = []
    i = start
    while i < len(sql):
        ch = sql[i]
        if ch == "'":
            if quote and i + 1 < len(sql) and sql[i + 1] == "'":
                i += 2
                continue
            quote = not quote
        elif not quote:
            if ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
            elif depth == 0:
                if ch == ",":
                    segments.append(sql[start:i].strip())
                    start = i + 1
                elif sql[i:i + 4].upper() == "FROM" and (
                        i == 0 or not sql[i - 1].isalnum()) and (
                        i + 4 == len(sql) or not sql[i + 4].isalnum()):
                    segments.append(sql[start:i].strip())
                    return segments
        i += 1
    return []


def select_name(expression: str) -> str:
    m = ALIAS.search(expression)
    if m:
        return m.group(1).upper()
    m = re.search(r'([A-Za-z_][A-Za-z0-9_]*)\s*$', expression)
    return m.group(1).upper() if m else ""


def audit_file(path: Path) -> dict:
    raw = strip_line_comments(path.read_text(encoding="utf-8"))
    match = MAPPING.search(raw)
    name = path.stem
    if not match:
        return {"name": name, "status": "SKIP", "reason": "no setReport"}
    jr = REPORTS / (match.group(1).split("/")[-1] + ".jrxml")
    if not jr.is_file():
        return {"name": name, "status": "FAIL", "errors": ["template_missing"]}
    root = ET.parse(jr).getroot()
    fields = FIELD.findall(raw)
    declared = [e.attrib.get("name") for e in root
                if e.tag.rsplit("}", 1)[-1] == "field"]
    expressions = "\n".join((e.text or "") for e in root.iter()
                            if e.tag.rsplit("}", 1)[-1].endswith("Expression"))
    referenced = sorted(set(re.findall(r'\$F\{([^}]+)\}', expressions)))
    missing = sorted(set(referenced) - set(fields))
    sql = query_source(raw)
    columns = top_level_select(sql)
    aliases = [select_name(c) for c in columns]
    errors = []
    warnings = []
    if missing:
        errors.append("missing_jasper_fields")
    if not sql:
        warnings.append("no_inline_select_or_report")
    if columns and len(columns) != len(fields):
        # El parser de SQL es conservador: puede haber subconsultas o
        # transformaciones válidas. Exigir comprobación de SQL real.
        warnings.append("sql_field_ordinal_count")
    shifted = [
        {"position": i + 1, "sql": actual, "mapped": expected}
        for i, (actual, expected) in
        enumerate(zip(aliases, [s.upper() for s in fields]))
        if actual and actual != expected
    ]
    if shifted:
        # El adaptador asigna por ordinal y un alias SQL puede diferir
        # legítimamente del nombre lógico; no confundirlo con un campo ausente.
        warnings.append("sql_field_ordinal_names")
    # Independientemente de que el informe esté enlazado a un menú,
    # esta unión puede multiplicar valores contables y es un P0.
    if re.search(r'\bFROM\s+products\s*,\s*ticketlines\b', sql, re.I):
        errors.append("possible_cartesian_products_ticketlines")
    if re.search(r'products\.UNITS\b', sql, re.I):
        errors.append("suspect_products_units")
    if "report.addParameter(" in raw and "?(QBF_FILTER)" not in sql:
        warnings.append("defined_filters_not_applied")
        if name == "vouchers":
            errors.append("voucher_filters_not_applied")
    if name == "vouchers" and "tickets.ticketid AS TICKETID " in sql and "payments.voucher AS VOUCHERNUM" in sql:
        errors.append("voucher_invalid_select_concatenation")
    if "getToday()" in raw and "getTodayMinutes()" in raw:
        warnings.append("defaults_to_today")
        # Un intervalo aplicado pero ausente del cuerpo, encabezado y pie
        # oculta a qué fechas corresponden cifras y movimientos.
        if "$P{ARG}" not in expressions:
            errors.append("filtered_period_not_rendered")
    nodata = any(e.tag.rsplit("}", 1)[-1] == "noData" for e in root)
    if not nodata:
        warnings.append("no_noData_band")
    return {
        "name": name,
        "template": jr.name,
        "status": "FAIL" if errors else "CHECK" if warnings else "PASS",
        "errors": errors,
        "warnings": warnings,
        "sql_columns": len(columns),
        "mapped_columns": len(fields),
        "jasper_declared": len(declared),
        "jasper_referenced": len(referenced),
        "missing_fields": missing,
        "shifted_ordinals": shifted[:15],
    }


def audit(report_dir: Path = REPORTS) -> list[dict]:
    return [audit_file(p) for p in sorted(report_dir.glob("*.bs"))]


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--json", action="store_true")
    args = p.parse_args()
    records = audit()
    errors = [r for r in records if r["status"] == "FAIL"]
    if args.json:
        print(json.dumps({"analyzed": len(records), "hard_fail": len(errors),
                          "records": records}, ensure_ascii=False, indent=2))
    else:
        for r in errors:
            print("FAIL", r["name"], ",".join(r.get("errors", [])),
                  "missing=", ",".join(r.get("missing_fields", [])),
                  "positions=", len(r.get("shifted_ordinals", [])))
        print("REPORT_CONTRACT_AUDIT analyzed=", len(records),
              "hard_fail=", len(errors), "pass_or_check=", len(records) - len(errors))
        print("WARNING: static checks cannot prove row counts, financial correctness "
              "or SQL runtime compatibility. Require sandbox data execution.")
    return 2 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
