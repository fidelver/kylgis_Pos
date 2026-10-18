#!/usr/bin/env bash
# No database access, no printer: four PDF reports with meaningful synthetic records.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "$0")/.." && pwd)"
RELEASE="${1:?Required read-only release directory for Jasper dependencies}"
JDK=/usr/lib/jvm/java-21-temurin-jdk
test -r "$RELEASE/kylgispos.jar"
command -v pdftotext >/dev/null
TMP="$(mktemp -d /tmp/kylgis-finance-content.XXXXXXXX)"
trap 'rm -rf -- "$TMP"' EXIT
"$JDK/bin/javac" -cp "$RELEASE/lib/openpdf-1.3.32.jar:$RELEASE/kylgispos.jar:$RELEASE/lib/*" \
  -d "$TMP" "$ROOT/tools/ReportFinanceContentSmoke.java"
"$JDK/bin/java" -Djava.awt.headless=true -Duser.language=es -Duser.country=MX \
  -cp "$TMP:$ROOT/src/main/resources:$RELEASE/lib/openpdf-1.3.32.jar:$RELEASE/kylgispos.jar:$RELEASE/lib/*" \
  ReportFinanceContentSmoke "$ROOT/src/main/resources/com/mx/kylgis/pos/reports" "$TMP"
for name in sales_extproducts sales_closedpos_1 sales_productsalesprofit vouchers; do
  pdftotext -raw "$TMP/$name.pdf" "$TMP/$name.txt"
done
python3 - "$TMP" <<'PY'
import pathlib,sys
folder=pathlib.Path(sys.argv[1])
checks={
"sales_extproducts":["QA CLIENTE UNO","QA PRODUCTO PRIMERO","QA PRODUCTO SEGUNDO","150.25","174.29","Periodo de consulta","01/10/2026"],
"sales_closedpos_1":["QA-CAJA","QA-CIERRE","Periodo de consulta","01/10/2026"],
"sales_productsalesprofit":["QA PRODUCTO UTILIDAD","ESTIMADO","12.00"],
"vouchers":["QA BONO","900","32.50"],
}
for name, required in checks.items():
    content=(folder/(name+".txt")).read_text(encoding="utf-8")
    plain=" ".join(content.split()).casefold()
    absent=[word for word in required if " ".join(word.split()).casefold() not in plain]
    if absent:
        print("FINANCIAL_REPORT_FAIL",name, "missing",absent,"text_excerpt",plain[:350])
        sys.exit(2)
    print("FINANCIAL_REPORT_CONTENT_PASS",name,"values_checked",len(required))
print("KYLGIS_FINANCE_CONTENT_OK reports=4")
PY
