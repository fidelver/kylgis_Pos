#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "$0")/.." && pwd)"
JDK=/usr/lib/jvm/java-21-temurin-jdk
TMP="$(mktemp -d /tmp/kylgis-period-qa.XXXXXXXX)"
trap 'rm -rf -- "$TMP"' EXIT
"$JDK/bin/javac" --release 21 -d "$TMP" \
 "$ROOT/src/main/java/com/mx/kylgis/pos/reports/ReportPeriodContext.java" \
 "$ROOT/tools/ReportPeriodContextSmoke.java"
"$JDK/bin/java" -cp "$TMP" ReportPeriodContextSmoke
