#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
JAR="$ROOT/target/kylgispos.jar"
TEST_SRC="$ROOT/tools/java/com/mx/kylgis/pos/inventory/transform/RecipeTransformationSelfTest.java"

find_java21_home() {
  local candidate version major
  local -a candidates=()

  if [[ -n "${JAVA_HOME:-}" ]]; then
    candidates+=("$JAVA_HOME")
  fi
  candidates+=(
    "/usr/lib/jvm/temurin-21-jdk"
    "/usr/lib/jvm/java-21-temurin-jdk"
    "/usr/lib/jvm/java-21-openjdk"
  )

  for candidate in "${candidates[@]}"; do
    [[ -x "$candidate/bin/javac" && -x "$candidate/bin/java" ]] || continue
    version="$($candidate/bin/javac -version 2>&1 | awk '{print $2}')"
    major="${version%%.*}"
    if [[ "$major" == "21" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  return 1
}

if [[ ! -f "$JAR" ]]; then
  echo "ERROR: package target missing: $JAR" >&2
  exit 2
fi
if [[ ! -f "$TEST_SRC" ]]; then
  echo "ERROR: self-test source missing: $TEST_SRC" >&2
  exit 2
fi

JAVA21_HOME="$(find_java21_home || true)"
if [[ -z "$JAVA21_HOME" ]]; then
  echo "ERROR: JDK 21 not found. Set JAVA_HOME to a JDK 21 installation." >&2
  exit 2
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

"$JAVA21_HOME/bin/javac" --release 21 -cp "$JAR" -d "$TMP" "$TEST_SRC"
"$JAVA21_HOME/bin/java" -cp "$JAR:$TMP" com.mx.kylgis.pos.inventory.transform.RecipeTransformationSelfTest
