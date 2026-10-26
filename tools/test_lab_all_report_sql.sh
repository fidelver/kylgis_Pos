#!/usr/bin/env bash
# Read-only EXPLAIN of all 69 report SELECTs against MariaDB .18 LAB.
# Never uses .17, never modifies schemas, never logs credentials.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "$0")/.." && pwd)"
if [[ "$#" -ne 2 ]]; then
  echo "Usage: test_lab_all_report_sql.sh <LAB .18 config> <candidate release folder>" >&2
  exit 2
fi
CONFIG="$1"
BUNDLE="$2"
JDK=/usr/lib/jvm/java-21-temurin-jdk
test -r "$CONFIG"
test -r "$BUNDLE/kylgispos.jar"
test -r "$BUNDLE/lib/mysql-connector-j-26.7.0.jar"
TMP="$(mktemp -d /tmp/kylgis-lab-69-sql.XXXXXXXX)"
trap 'rm -rf -- "$TMP"' EXIT
PYTHONDONTWRITEBYTECODE=1 python3 - "$ROOT" "$TMP/report_sql_queries.tsv" <<'PY'
from pathlib import Path
import base64,sys
root=Path(sys.argv[1])
sys.path.insert(0,str(root/"tools"))
import audit_report_contracts as audit
files=sorted(audit.REPORTS.glob("*.bs"))
if len(files)!=69:raise SystemExit("LAB_SQL_EXPLAIN_EXPECTED_69_SCRIPTS")
entries=[]
for file in files:
    sql=audit.query_source(file.read_text(encoding="utf-8"))
    if not sql.lstrip().upper().startswith("SELECT "):
        raise SystemExit("LAB_SQL_NOT_READ_ONLY_SELECT "+file.stem)
    entries.append(file.stem+"\t"+base64.b64encode(sql.encode()).decode())
Path(sys.argv[2]).write_text("\n".join(entries)+"\n",encoding="utf-8")
print("LAB_SQL_69_QUERIES_PREPARED_FROM_BEANSHELL")
PY
"$JDK/bin/javac" --release 21 -cp "$BUNDLE/kylgispos.jar:$BUNDLE/lib/*" \
 -d "$TMP" "$ROOT/tools/ReportSqlLabExplain.java"
timeout 170 "$JDK/bin/java" -cp "$TMP:$BUNDLE/kylgispos.jar:$BUNDLE/lib/*" \
 ReportSqlLabExplain "$CONFIG" "$TMP/report_sql_queries.tsv"
