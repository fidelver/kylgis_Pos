#!/usr/bin/env bash
# Reconcile BeanShell report SQL with independent raw rows on LAB .18 (read-only).
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "$0")/.." && pwd)"
CONFIG="${1:?Usage: $0 <LAB-properties> <readable-release>}"
RELEASE="${2:?Missing readable release}"
JDK=/usr/lib/jvm/java-21-temurin-jdk
test -r "$ROOT/target/kylgispos.jar"
test -r "$RELEASE/lib/mysql-connector-j-26.7.0.jar"
TMP="$(mktemp -d /tmp/kylgis-finance-lab.XXXXXXXX)"
trap 'rm -rf -- "$TMP"' EXIT
cp -- "$ROOT/target/kylgispos.jar" "$TMP/kylgispos.jar"
PYTHONDONTWRITEBYTECODE=1 python3 - "$ROOT/tools" "$TMP/report_queries.tsv" <<'PY'
import sys,pathlib,base64
sys.path.insert(0,sys.argv[1])
import audit_report_contracts as audit
reports={
    "sales_payments":"receipts.DATENEW",
    "sales_cashflow":"receipts.DATENEW",
    "sales_categorysales":"R.DATENEW",
    "sales_taxes":"receipts.DATENEW",
    "sales_chart_productsales":"receipts.DATENEW",
    "sales_productsalesprofit":"receipts.DATENEW"
}
lines=[]
for name,col in reports.items():
    sql=audit.query_source((audit.REPORTS/(name+".bs")).read_text())
    assert sql.count("?(QBF_FILTER)")==1,name
    sql=sql.replace("?(QBF_FILTER)",f"({col} >= ? AND {col} < ?)")
    assert sql.lstrip().upper().startswith("SELECT"),name
    lines.append(name+"\t"+base64.b64encode(sql.encode()).decode())
pathlib.Path(sys.argv[2]).write_text("\n".join(lines)+"\n")
print("LAB_REPORT_QUERIES_FROM_BEANSHELL",len(lines))
PY
"$JDK/bin/javac" -cp "$TMP/kylgispos.jar:$RELEASE/lib/*" -d "$TMP" "$ROOT/tools/ReportLabFinanceReconcile.java"
"$JDK/bin/java" -Djava.awt.headless=true \
  -cp "$TMP:$TMP/kylgispos.jar:$RELEASE/lib/*" \
  ReportLabFinanceReconcile "$CONFIG" "$TMP/report_queries.tsv"
