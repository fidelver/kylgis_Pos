#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"
JAVA_HOME="${JAVA_HOME:-}"
if [[ -z "$JAVA_HOME" || ! -x "$JAVA_HOME/bin/java" ]]; then echo "ERROR: configure JAVA_HOME with JDK 21" >&2; exit 2; fi
JAVA="$JAVA_HOME/bin/java"
JPACKAGE="$JAVA_HOME/bin/jpackage"
MVN="${MAVEN_BIN:-mvn}"
major="$("$JAVA" -version 2>&1 | awk -F'[".]' '/version/ {print ($2 == 1 ? $3 : $2); exit}')"
[[ "$major" == "21" ]] || { echo "ERROR: JDK 21 required; detected $major" >&2; exit 3; }
[[ -x "$JPACKAGE" ]] || { echo "ERROR: jpackage is missing from JDK 21" >&2; exit 4; }
if ! git diff --quiet -- || ! git diff --cached --quiet -- || [[ -n "$(git ls-files --others --exclude-standard)" ]]; then
  echo "ERROR: worktree must be clean for a reproducible package" >&2; exit 5
fi
"$MVN" -DskipTests clean package
JAR="$(find target -maxdepth 1 -type f -name '*.jar' -print -quit)"
[[ -n "$JAR" ]] || { echo "ERROR: main JAR not found" >&2; exit 6; }
VERSION="1.3"
SAFE_VERSION="$(printf '%s' "$VERSION" | tr -cd '0-9.')"
OUT="${PORTABLE_OUT:-target/portable}"
rm -rf "$OUT"; mkdir -p "$OUT"
APP_NAME="${APP_NAME:-KylGisPOS}"
INPUT="$(dirname "$JAR")"; MAIN_JAR="$(basename "$JAR")"
"$JPACKAGE" --type app-image --name "$APP_NAME" --app-version "$SAFE_VERSION" --input "$INPUT" --main-jar "$MAIN_JAR" --dest "$OUT" --runtime-image "$JAVA_HOME" --java-options "-Dfile.encoding=UTF-8" --java-options "--enable-native-access=ALL-UNNAMED"
(cd "$OUT"; sha256sum "$APP_NAME"/* "$APP_NAME"/lib/* 2>/dev/null) > "$OUT/$APP_NAME.sha256"
printf 'PORTABLE_PACKAGE=%s/%s
' "$OUT" "$APP_NAME"
printf 'PORTABLE_SHA256=%s
' "$(sha256sum "$OUT/$APP_NAME.sha256" | awk '{print $1}')"
