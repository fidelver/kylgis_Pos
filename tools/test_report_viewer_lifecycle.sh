#!/usr/bin/env bash
# In-memory Jasper viewer lifecycle regression; no DB or GUI session needed.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "$0")/.." && pwd)"
RELEASE="${1:?Usage: test_report_viewer_lifecycle.sh <readable-release-directory>}"
JDK=/usr/lib/jvm/java-21-temurin-jdk
test -r "$RELEASE/kylgispos.jar"
test -r "$RELEASE/lib/jasperreports-6.21.5.jar"
TMP="$(mktemp -d /tmp/kylgis-viewer-lifecycle.XXXXXXXX)"
trap 'rm -rf -- "$TMP"' EXIT
"$JDK/bin/javac" -cp "$RELEASE/kylgispos.jar:$RELEASE/lib/*" \
  -d "$TMP" "$ROOT/tools/ReportViewerLifecycleSmoke.java"
timeout 45 "$JDK/bin/java" -Djava.awt.headless=false \
  -cp "$TMP:$RELEASE/kylgispos.jar:$RELEASE/lib/*" ReportViewerLifecycleSmoke
