#!/usr/bin/env python3
"""Prevent Swing JLabel labelFor-self cycles (Java 21).

Checks Java source and NetBeans forms without starting the application or
touching the database.
"""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java"
bad = []
checked_java = checked_forms = 0
pattern = re.compile(
    r"\b([A-Za-z_$][A-Za-z0-9_$]*)\.setLabelFor\(\s*"
    r"([A-Za-z_$][A-Za-z0-9_$]*)\s*\)"
)
for path in sorted(JAVA.rglob("*.java")):
    checked_java += 1
    source = path.read_text(encoding="utf-8", errors="replace")
    for hit in pattern.finditer(source):
        if hit.group(1) == hit.group(2):
            line = source.count("\n", 0, hit.start()) + 1
            bad.append(f"{path.relative_to(ROOT)}:{line}:{hit.group(1)}")
for path in sorted(JAVA.rglob("*.form")):
    checked_forms += 1
    try:
        tree = ET.parse(path)
    except ET.ParseError:
        bad.append(f"{path.relative_to(ROOT)}:XML_PARSE_FAILED")
        continue
    for item in tree.getroot().iter():
        name = item.get("name")
        if not name:
            continue
        for prop in item.findall("./Properties/Property"):
            if prop.get("name") != "labelFor":
                continue
            for ref in prop.findall("ComponentRef"):
                if ref.get("name") == name:
                    bad.append(f"{path.relative_to(ROOT)}:{name}:SELF_ASSOCIATION")
assert not bad, "SWING_ACCESSIBILITY_CYCLE_FAIL " + repr(bad)
print(f"SWING_ACCESSIBILITY_CYCLE_PASS java_files={checked_java} form_files={checked_forms}")
