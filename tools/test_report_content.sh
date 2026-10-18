#!/usr/bin/env bash
# QA de contenido sintético: falla si PDF pierde identificadores o importes.
# No conecta a BD ni imprime.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "$0")/.." && pwd)"
if [[ "$#" -ne 1 ]]; then
    echo "Uso: bash tools/test_report_content.sh /ruta/release-solo-lectura" >&2
    exit 4
fi
RELEASE_ROOT="$1"
JAR="$RELEASE_ROOT/kylgispos.jar"
LIB="$RELEASE_ROOT/lib"
JDK="/usr/lib/jvm/java-21-temurin-jdk"
if [[ ! -r "$JAR" || ! -d "$LIB" ]]; then
    echo "QA_ERROR: faltan bibliotecas JasperReports legibles" >&2
    exit 4
fi
command -v pdftotext >/dev/null || { echo "QA_ERROR: falta pdftotext" >&2; exit 4; }
TMP="$(mktemp -d /tmp/kylgis-report-content.XXXXXXXX)"
trap 'rm -rf -- "$TMP"' EXIT
"$JDK/bin/javac" -cp "$LIB/openpdf-1.3.32.jar:$JAR:$LIB/*" \
    -d "$TMP" "$ROOT/tools/ReportContentSmoke.java"
"$JDK/bin/java" -Djava.awt.headless=true -Duser.language=en \
    -Duser.country=US -cp "$TMP:$LIB/openpdf-1.3.32.jar:$JAR:$LIB/*" \
    ReportContentSmoke \
    "$ROOT/src/main/resources/com/mx/kylgis/pos/reports/products.jrxml" \
    "$TMP/products.pdf"
pdftotext -raw "$TMP/products.pdf" "$TMP/products.txt"
python3 - "$TMP/products.txt" <<'PY'
import pathlib
import sys

text = pathlib.Path(sys.argv[1]).read_text(encoding="utf-8")
normalized = "".join(text.split())
required = [
    "KYLQA-CATEGORIA-SINTETICA",
    "KYLQA-REFERENCIA-001",
    "KYLQA-CODIGO-001",
    "KYLQA-PRODUCTO-CONTENIDO-001",
    "2.5",
    "$12.25",
    "$27.50",
    "$31.90",
    "Catálogo",
    "Imprimir",
]
missing = [value for value in required if "".join(value.split()) not in normalized]
if missing:
    print("KYLGIS_REPORT_CONTENT_FAIL missing=" + ",".join(missing))
    raise SystemExit(2)
print("KYLGIS_REPORT_CONTENT_OK visible_fields=4 "
      "numeric_values=4 context_labels=2 all_checks=10")
PY
