#!/usr/bin/env bash
# Jasper runtime proof: actually FILL and export ALL 67 JRXML with synthetic data.
# No database connection, printer, or production process.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "$0")/.." && pwd)"
RELEASE="${1:?Usage: test_all_report_render.sh <readable-release-directory>}"
JDK=/usr/lib/jvm/java-21-temurin-jdk
test -r "$RELEASE/kylgispos.jar"
test -r "$RELEASE/lib/jasperreports-6.21.5.jar"
test -r "$RELEASE/lib/openpdf-1.3.32.jar"
python3 "$ROOT/tools/test_report_font_portability.py" --jar "$RELEASE/kylgispos.jar"
TMP="$(mktemp -d /tmp/kylgis-all-report-render.XXXXXXXX)"
trap 'rm -rf -- "$TMP"' EXIT
"$JDK/bin/javac" -cp "$RELEASE/lib/openpdf-1.3.32.jar:$RELEASE/kylgispos.jar:$RELEASE/lib/*" \
  -d "$TMP" "$ROOT/tools/ReportAllSyntheticSmoke.java"
timeout 210 "$JDK/bin/java" -Djava.awt.headless=true \
  -cp "$TMP:$ROOT/src/main/resources:$RELEASE/lib/openpdf-1.3.32.jar:$RELEASE/kylgispos.jar:$RELEASE/lib/*" \
  ReportAllSyntheticSmoke "$ROOT/src/main/resources/com/mx/kylgis/pos/reports"
