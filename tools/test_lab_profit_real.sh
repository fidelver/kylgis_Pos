#!/usr/bin/env bash
# Source-derived profit report, real LAB .18 data, zero DB mutations.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "$0")/.." && pwd)"
CONFIG="${1:?Usage: $0 <LAB-properties> <readable-release> [QA-PDF-path]}"
REL="${2:?Missing readable release}"
OUTPUT="${3:-}"
if [[ -n "$OUTPUT" && "$OUTPUT" != /home/fam/POS/QA_Release13/* ]]; then
    echo "Only temporary or QA output paths are accepted." >&2
    exit 3
fi
JDK=/usr/lib/jvm/java-21-temurin-jdk
TMP="$(mktemp -d /tmp/kylgis-lab-profit.XXXXXXXX)"
trap 'rm -rf -- "$TMP"' EXIT
test -r "$ROOT/target/kylgispos.jar"
cp -- "$ROOT/target/kylgispos.jar" "$TMP/kylgispos.jar"
PYTHONDONTWRITEBYTECODE=1 python3 - "$ROOT/tools" "$TMP/report.sql" <<'PY'
import sys,pathlib
sys.path.insert(0,sys.argv[1])
import audit_report_contracts as audit
sql=audit.query_source((audit.REPORTS/"sales_productsalesprofit.bs").read_text())
assert sql.count("?(QBF_FILTER)")==1
sql=sql.replace("?(QBF_FILTER)",
                "(receipts.DATENEW >= ? AND receipts.DATENEW < ?)")
pathlib.Path(sys.argv[2]).write_text(sql)
PY
"$JDK/bin/javac" -cp "$TMP/kylgispos.jar:$REL/lib/*" \
    -d "$TMP" "$ROOT/tools/ReportLabProfitRealSmoke.java"
"$JDK/bin/java" -Djava.awt.headless=true \
    -cp "$TMP:$ROOT/src/main/resources:$TMP/kylgispos.jar:$REL/lib/openpdf-1.3.32.jar:$REL/lib/*" \
    ReportLabProfitRealSmoke "$CONFIG" "$TMP/report.sql" \
    "$ROOT/src/main/resources/com/mx/kylgis/pos/reports/sales_productsalesprofit.jrxml" \
    "$TMP/profit.pdf"
pdftotext -layout "$TMP/profit.pdf" "$TMP/profit.txt"
grep -Fq "Ajustes sin producto" "$TMP/profit.txt"
grep -Fq "Ventas: beneficio" "$TMP/profit.txt"
echo LAB_PROFIT_PDF_ADJUSTMENT_LABEL_PASS
if [[ -n "$OUTPUT" ]]; then cp -- "$TMP/profit.pdf" "$OUTPUT"; fi
