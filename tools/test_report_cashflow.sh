#!/usr/bin/env bash
# QA semántica sintética del Flujo de efectivo: datos, periodo y total.
# No conecta BD, no envía a impresoras, no produce PDF persistente.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "$0")/.." && pwd)"
if [[ "$#" -ne 1 ]]; then
    echo "Uso: bash tools/test_report_cashflow.sh /ruta/release-solo-lectura" >&2
    exit 4
fi
RELEASE_ROOT="$1"
JAR="$RELEASE_ROOT/kylgispos.jar"
LIB="$RELEASE_ROOT/lib"
JDK="/usr/lib/jvm/java-21-temurin-jdk"
[[ -r "$JAR" && -d "$LIB" ]] || { echo "QA_ERROR: faltan bibliotecas" >&2; exit 4; }
command -v pdftotext >/dev/null || { echo "QA_ERROR: falta pdftotext" >&2; exit 4; }
TMP="$(mktemp -d /tmp/kylgis-report-cashflow.XXXXXXXX)"
trap 'rm -rf -- "$TMP"' EXIT
"$JDK/bin/javac" -cp "$LIB/openpdf-1.3.32.jar:$JAR:$LIB/*" \
    -d "$TMP" "$ROOT/tools/ReportCashflowSmoke.java"
"$JDK/bin/java" -Djava.awt.headless=true -Duser.language=en \
    -Duser.country=US \
    -cp "$TMP:$ROOT/src/main/resources:$LIB/openpdf-1.3.32.jar:$JAR:$LIB/*" \
    ReportCashflowSmoke \
    "$ROOT/src/main/resources/com/mx/kylgis/pos/reports/sales_cashflow.jrxml" \
    "$TMP/cashflow.pdf"
pdftotext -raw "$TMP/cashflow.pdf" "$TMP/cashflow.txt"
python3 - "$TMP/cashflow.txt" <<'PY'
import pathlib
import sys
text = pathlib.Path(sys.argv[1]).read_text(encoding="utf-8")
norm = "".join(text.split())
expected = [
    "Flujo de efectivo", "Periodo:", "Efectivo", "Total flujo",
    "Oct 1, 2026", "Oct 18, 2026", "$1,234.50",
]
missing = [key for key in expected if "".join(key.split()) not in norm]
count = norm.count("$1,234.50")
if missing or count != 2:
    print("KYLGIS_CASHFLOW_CONTENT_FAIL "
          "missing=" + ",".join(missing) + " occurrences=" + str(count))
    raise SystemExit(2)
print("KYLGIS_CASHFLOW_CONTENT_OK title=true period=true payment=true "
      "displayed_row_total_equals_summary=true occurrences=2")
PY
