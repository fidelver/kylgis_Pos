#!/usr/bin/env python3
"""Fail-closed portability gate for ALL source and packaged POS Jasper reports.

No network, SQL or printer access. Portable logical AWT fonts are required
throughout the report editor and its NetBeans-generated .form descriptors.
A release JAR can be inspected with --jar to prevent stale templates.
"""
import argparse
from collections import Counter
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
REPORT_ROOT = ROOT / "src/main/resources/com/mx/kylgis/pos/reports"
EDITOR_ROOT = ROOT / "src/main/java/com/mx/kylgis/pos/reports"
TEMPLATE_ROOT = ROOT / "src/main/resources/com/mx/kylgis/pos/templates"
LOGICAL = {"SansSerif", "Serif", "Monospaced", "Dialog", "DialogInput"}
PDF_BASE14 = {
    "Helvetica", "Helvetica-Bold", "Helvetica-Oblique", "Helvetica-BoldOblique",
    "Courier", "Courier-Bold", "Courier-Oblique", "Courier-BoldOblique",
    "Times-Roman", "Times-Bold", "Times-Italic", "Times-BoldItalic",
    "Symbol", "ZapfDingbats",
}
NS = "{http://jasperreports.sourceforge.net/jasperreports}"


def audit(jar=None, expected_head=None):
    failures = []
    source_templates = sorted(REPORT_ROOT.glob("*.jrxml"))
    if len(source_templates) != 67:
        failures.append(f"Unexpected number of report templates: {len(source_templates)} (expected 67)")
    declared = Counter()
    for source in source_templates:
        try:
            raw = source.read_bytes()
            root = ET.fromstring(raw)
        except (OSError, ET.ParseError) as exc:
            failures.append(f"{source.name}: XML/read error: {exc}")
            continue
        if root.tag.rsplit("}", 1)[-1] != "jasperReport":
            failures.append(f"{source.name}: unexpected Jasper root {root.tag}")
        if b"arial" in raw.lower():
            failures.append(f"{source.name}: legacy Arial literal")
        for element in root.iter():
            for attr in ("fontName", "fontFamily"):
                family = element.attrib.get(attr)
                if family:
                    declared[family] += 1
                    if family not in LOGICAL:
                        failures.append(f"{source.name}: nonportable {attr}={family!r}")
            font = element.attrib.get("pdfFontName")
            if font and font not in PDF_BASE14:
                failures.append(f"{source.name}: unknown/nonembedded PDF font {font!r}")

    editor_java = sorted(EDITOR_ROOT.glob("*.java"))
    editor_forms = sorted(EDITOR_ROOT.glob("*.form"))
    for path in editor_java + editor_forms:
        try:
            raw = path.read_text(encoding="utf-8")
        except OSError as exc:
            failures.append(f"{path.name}: read error: {exc}")
            continue
        if "arial" in raw.lower():
            failures.append(f"{path.name}: legacy Arial reference")
    for form in editor_forms:
        matching_java = form.with_suffix(".java")
        if not matching_java.exists():
            failures.append(f"{form.name}: missing matching generated Java source")

    ticket_templates = sorted(TEMPLATE_ROOT.glob("*.xml"))
    for source in ticket_templates:
        try:
            if b"arial" in source.read_bytes().lower():
                failures.append(f"{source.name}: unsupported Arial in closing/ticket template")
        except OSError as exc:
            failures.append(f"{source.name}: read error: {exc}")

    packaged = 0
    if jar is not None:
        try:
            with zipfile.ZipFile(jar) as archive:
                names = set(archive.namelist())
                prefix = "com/mx/kylgis/pos/reports/"
                packaged_reports = {
                    p for p in names if p.startswith(prefix) and p.endswith(".jrxml")
                }
                packaged = len(packaged_reports)
                expected = {prefix + p.name for p in source_templates}
                missing = sorted(expected - packaged_reports)
                extra = sorted(packaged_reports - expected)
                if missing or extra:
                    failures.append(f"JAR reports mismatch: missing={missing}, extra={extra}")
                for source in source_templates:
                    member = prefix + source.name
                    if member not in names:
                        continue
                    packaged_data = archive.read(member)
                    if packaged_data != source.read_bytes():
                        failures.append(f"JAR contains stale/modified template {source.name}")
                    if b"arial" in packaged_data.lower():
                        failures.append(f"JAR contains Arial in {source.name}")
                # Protect closing/ticket resources from silently shipping older code.
                ticket_prefix = "com/mx/kylgis/pos/templates/"
                for source in ticket_templates:
                    member = ticket_prefix + source.name
                    if member not in names:
                        failures.append(f"JAR missing ticket template {source.name}")
                    elif archive.read(member) != source.read_bytes():
                        failures.append(f"JAR contains stale ticket template {source.name}")
                if expected_head is not None:
                    manifest = archive.read("META-INF/MANIFEST.MF").decode(
                        "utf-8", errors="replace"
                    )
                    if "Git-Commit: " + expected_head not in manifest:
                        failures.append("JAR manifest does not match expected Git HEAD")
                    if "Git-Dirty: false" not in manifest:
                        failures.append("JAR manifest is dirty")
        except (OSError, KeyError, zipfile.BadZipFile) as exc:
            failures.append(f"Cannot verify release JAR: {exc}")

    if failures:
        print("KYLGIS_REPORT_FONT_PORTABILITY_FAIL",len(failures))
        for problem in failures[:30]:
            print("FAIL",problem)
        return 1
    print(
        "KYLGIS_REPORT_FONT_PORTABILITY_OK",
        f"templates={len(source_templates)}",
        f"fonts={dict(declared)}",
        f"editor_java={len(editor_java)}",
        f"editor_forms={len(editor_forms)}",
        f"ticket_templates={len(ticket_templates)}",
        f"packaged={packaged if jar else 'not_checked'}",
    )
    return 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, default=None)
    parser.add_argument("--expected-head", default=None)
    args = parser.parse_args()
    if args.expected_head is not None and args.jar is None:
        parser.error("--expected-head requires --jar")
    return audit(args.jar, args.expected_head)


if __name__ == "__main__":
    sys.exit(main())
