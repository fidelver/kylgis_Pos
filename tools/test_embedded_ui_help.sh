#!/usr/bin/env bash
# Verify contextual Swing help for every shipped locale and sealed JAR resources.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "$0")/.." && pwd)"
JAR="${1:-$ROOT/target/kylgispos.jar}"
RELEASE="${2:?Usage: $0 [candidate JAR] <readable dependency release>}"
JDK=/usr/lib/jvm/java-21-temurin-jdk
JAR="$(realpath -- "$JAR")"
test -r "$JAR"
test -r "$RELEASE/lib/jasperreports-6.21.5.jar"
# A release must not expose editable copies of the embedded help resources in
# target/reports: Maven outputs only the JAR entry for this namespace.
if [[ -d "$ROOT/target/reports/com/mx/kylgis/pos/ui/help" ]]; then
    echo "UI_HELP_EXTERNAL_RESOURCE_COPY_FORBIDDEN" >&2
    exit 4
fi
TMP="$(mktemp -d /tmp/kylgis-ui-help.XXXXXXXX)"
trap 'rm -rf -- "$TMP"' EXIT
PYTHONDONTWRITEBYTECODE=1 python3 "$ROOT/tools/generate_embedded_ui_help.py" --check
PYTHONDONTWRITEBYTECODE=1 python3 "$ROOT/tools/generate_ui_help_checklist.py" --check
PYTHONDONTWRITEBYTECODE=1 python3 "$ROOT/tools/audit_unlocalized_ui_strings.py" --check
python3 - "$JAR" <<'PY'
import sys,zipfile
with zipfile.ZipFile(sys.argv[1]) as f:
    names=set(f.namelist())
    base="com/mx/kylgis/pos/ui/help/"
    locales=("","_al_SQ","_ar","_da","_de","_el","_en_US","_es",
             "_es_AR","_es_MX","_et","_fr","_hr","_it","_nl","_pt","_pt_BR")
    for loc in locales:
        name=base+"ui_help"+loc+".properties"
        if name not in names:
            raise SystemExit("EMBEDDED_UI_HELP_MISSING "+name)
    if base+"ui_help_catalog.properties" not in names:
        raise SystemExit("EMBEDDED_UI_HELP_CATALOG_MISSING")
    if "com/mx/kylgis/pos/ui/help/UiHelp.class" not in names:
        raise SystemExit("EMBEDDED_UI_HELP_CLASS_MISSING")
    assert len(locales)==17
    catalog=base+"ui_help_catalog.properties"
    control_ids={line.split("=",1)[0] for line in f.read(catalog).decode("utf-8").splitlines()
                 if "=" in line and not line.startswith("#")}
    if len(control_ids)<900:raise SystemExit("UI_HELP_CONTROL_INVENTORY_TOO_SMALL")
    canonical_keys=None
    for loc in locales:
        member=base+"ui_help"+loc+".properties"
        entries=dict(line.split("=",1) for line in f.read(member).decode("utf-8").splitlines()
                     if "=" in line and not line.startswith("#"))
        actual={key[8:] for key in entries if key.startswith("control.")}
        if actual!=control_ids:raise SystemExit("UI_HELP_CONTROL_PARITY_FAIL "+loc)
        exact={key[7:] for key in entries if key.startswith("native.")}
        if not exact<=control_ids:raise SystemExit("UI_HELP_NATIVE_KEY_NOT_IN_CATALOG "+loc)
        if any(not value.strip() for key,value in entries.items()
               if not key.startswith("native.")):
            raise SystemExit("UI_HELP_BLANK_TRANSLATION_FAIL "+loc)
        # Expert-written legacy tips may be translated in only some
        # languages; the always-present native sentence format is the fallback.
        required={key for key in entries if not key.startswith("native.")}
        if canonical_keys is None:canonical_keys=required
        elif required!=canonical_keys:raise SystemExit("UI_HELP_LOCALE_KEYS_DRIFT "+loc)
    print("KYLGIS_UI_HELP_KEY_PARITY_PASS locales=17 controls="+str(len(control_ids))+
          " required_blanks=0 legacy_native_fallbacks_explicit=true semantic_review_required=true")
    print("KYLGIS_UI_HELP_JAR_RESOURCES_PASS locales=17 all_sealed=true")
PY
mkdir -p "$TMP/foreign/com/mx/kylgis/pos/ui/help"
echo 'tooltip.scale.request=INJECTED FROM USER DIRECTORY' \
  > "$TMP/foreign/com/mx/kylgis/pos/ui/help/ui_help_es.properties"
mkdir -p "$TMP/classes"
"$JDK/bin/javac" --release 21 -cp "$JAR:$RELEASE/lib/*" \
    -d "$TMP/classes" "$ROOT/tools/UiHelpLocaleSmoke.java"
"$JDK/bin/java" -Djava.awt.headless=true -Duser.dir="$TMP/foreign" \
  -cp "$TMP/foreign:$TMP/classes:$JAR:$RELEASE/lib/*" \
  UiHelpLocaleSmoke | tee "$TMP/locale.log"
if grep -q 'INJECTED FROM USER DIRECTORY' "$TMP/locale.log"; then
    echo "EMBEDDED_UI_HELP_CLASSPATH_SHADOW_FAIL" >&2
    exit 2
fi
grep -q 'KYLGIS_EMBEDDED_UI_HELP_QA_PASS locales=17' "$TMP/locale.log"
echo "KYLGIS_UI_HELP_EXTERNAL_OVERRIDE_RESISTANCE_PASS"
