#!/usr/bin/env bash
# Synthetic boundary tests for groups of 15 product names.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "$0")/.." && pwd)"
RELEASE="${1:?Usage: $0 <readable-release>}"
JDK=/usr/lib/jvm/java-21-temurin-jdk
test -r "$ROOT/target/kylgispos.jar"
test -r "$RELEASE/lib/jasperreports-6.21.5.jar"
TMP="$(mktemp -d /tmp/kylgis-sales-pages.XXXXXXXX)"
trap 'rm -rf -- "$TMP"' EXIT
cp -- "$ROOT/target/kylgispos.jar" "$TMP/kylgispos.jar"
"$JDK/bin/javac" -cp "$TMP/kylgispos.jar:$RELEASE/lib/*" -d "$TMP" \
  "$ROOT/tools/ReportProductSalesPaginationSmoke.java"
"$JDK/bin/java" -Djava.awt.headless=true \
  -cp "$TMP:$ROOT/src/main/resources:$TMP/kylgispos.jar:$RELEASE/lib/openpdf-1.3.32.jar:$RELEASE/lib/*" \
  ReportProductSalesPaginationSmoke \
  "$ROOT/src/main/resources/com/mx/kylgis/pos/reports/sales_chart_productsales.jrxml"
