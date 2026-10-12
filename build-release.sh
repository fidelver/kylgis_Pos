#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

if ! command -v git >/dev/null 2>&1; then
    echo "ERROR: git es obligatorio para un build oficial." >&2
    exit 2
fi
if ! command -v mvn >/dev/null 2>&1; then
    echo "ERROR: mvn no está disponible." >&2
    exit 2
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "ERROR: el build oficial debe ejecutarse desde un checkout Git." >&2
    exit 2
fi

if ! git diff --quiet -- || ! git diff --cached --quiet -- \
        || [[ -n "$(git ls-files --others --exclude-standard)" ]]; then
    echo "ERROR: el worktree contiene cambios sin commit; no se generará un JAR oficial." >&2
    exit 3
fi

commit="$(git rev-parse --verify HEAD)"
branch="$(git branch --show-current)"
if [[ -z "$branch" ]]; then
    branch="DETACHED"
fi
commit_time="$(git show -s --format=%cI HEAD)"
build_time="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"
if ! command -v "$java_bin" >/dev/null 2>&1 && [[ ! -x "$java_bin" ]]; then
    echo "ERROR: Java no está disponible (JAVA_HOME=${JAVA_HOME:-<sin definir>})." >&2
    exit 2
fi
java_major="$($java_bin -version 2>&1 | awk -F'[".]' '/version/ {print ($2 == 1 ? $3 : $2); exit}')"
if [[ "$java_major" != "21" ]]; then
    echo "ERROR: el build oficial requiere JDK 21; detectado: ${java_major:-desconocido}." >&2
    exit 4
fi

echo "KylGis release build"
echo "  commit: $commit"
echo "  branch: $branch"
echo "  commit time: $commit_time"
echo "  build time: $build_time"

mvn -DskipTests \
    -Dkylgis.git.commit="$commit" \
    -Dkylgis.git.branch="$branch" \
    -Dkylgis.git.dirty=false \
    -Dkylgis.git.commitTime="$commit_time" \
    -Dkylgis.build.time="$build_time" \
    clean package

jar_file="$(find target -maxdepth 1 -type f -name '*.jar' -print -quit)"
if [[ -z "$jar_file" ]]; then
    echo "ERROR: Maven terminó sin producir un JAR principal." >&2
    exit 5
fi

manifest="$(unzip -p "$jar_file" META-INF/MANIFEST.MF | tr -d '\r')"
grep -Fqx "Git-Commit: $commit" <<<"$manifest"
grep -Fqx "Git-Branch: $branch" <<<"$manifest"
grep -Fqx "Git-Dirty: false" <<<"$manifest"
grep -Fqx "Git-Commit-Time: $commit_time" <<<"$manifest"
grep -Fqx "Build-Time: $build_time" <<<"$manifest"

sha256="$(sha256sum "$jar_file" | awk '{print $1}')"
echo "RELEASE_JAR=$jar_file"
echo "RELEASE_SHA256=$sha256"
echo "RELEASE_TRACEABILITY_OK"
