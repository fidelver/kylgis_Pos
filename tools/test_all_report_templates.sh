#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "$0")/.." && pwd)"
RELEASE="${1:?Read-only POS release with Jasper JARs required}"
JDK=/usr/lib/jvm/java-21-temurin-jdk
TMP="$(mktemp -d /tmp/kylgis-jrxml-compile.XXXXXXXX)"
trap 'rm -rf -- "$TMP"' EXIT
"$JDK/bin/javac" -cp "$RELEASE/kylgispos.jar:$RELEASE/lib/*" -d "$TMP" "$ROOT/tools/ReportTemplateCompileSmoke.java"
"$JDK/bin/java" -Djava.awt.headless=true -cp "$TMP:$RELEASE/kylgispos.jar:$RELEASE/lib/*" \
  ReportTemplateCompileSmoke "$ROOT/src/main/resources/com/mx/kylgis/pos/reports"
