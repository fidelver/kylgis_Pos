#!/usr/bin/env python3
"""Inventory literals given to Swing title, caption, tooltip and tab APIs.
A finding needs review; it is not automatically an untranslated string.
No database or network access. Generated output is deterministic.
"""
from collections import Counter,defaultdict
from pathlib import Path
import argparse
import re
import generate_embedded_ui_help as resources
from ui_help_operational_actions import LEGACY_CAPTIONS
from functools import lru_cache

@lru_cache(maxsize=1)
def compiled_specific_ids():
    catalog=(ROOT/"src/main/resources/com/mx/kylgis/pos/ui/help/ui_help.properties").read_text(encoding="utf-8")
    return {line.split("=",1)[0][len("specific."):] for line in catalog.splitlines()
            if line.startswith("specific.")}



ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"src/main/java/com/mx/kylgis/pos"
OUT=ROOT/"docs/qa/BARRIDO_CADENAS_UI_POS13_20261018.md"
CONFIG_TAB_RAW={
    "General","Locale","Payment Method","Peripherals","System Options",
    "Ticket Setup","Company","Database Setup","Botones de venta","Plataformas","Nodos"
}

def covered_at_runtime(path,kind,value):
    code=None
    if kind=="tab" and path.endswith("/config/JPanelConfiguration.java"):
        if value not in CONFIG_TAB_RAW:return False
        code=(ROOT/path).read_text(encoding="utf-8",errors="replace")
        return "localizeConfigurationTabTitles();" in code and (
            'UiHelp.text(localeKey)' in code and 'UiHelp.tooltip(localeKey)' in code)
    if kind=="tooltip":
        code=(ROOT/path).read_text(encoding="utf-8",errors="replace")
        owner=path.removeprefix("src/main/java/").removesuffix(".java").replace("/",".")
        pattern=r'\b(\w+)\.setToolTipText\s*\(\s*"'+re.escape(value)+r'"'
        return any(owner+"#"+var in compiled_specific_ids()
                   for var in re.findall(pattern,code))
    if kind=="tooltip" and path.endswith("/inventory/ProductsEditor.java"):
        # NetBeans still stores the legacy tooltip in generated code, but the
        # constructor replaces it with a stable, sealed and localized key.
        code=(ROOT/path).read_text(encoding="utf-8",errors="replace")
        match=re.search(r'\b(\w+)\.setToolTipText\s*\(\s*"'+re.escape(value),code)
        if match:
            return bool(re.search(r'UiHelp\.set\(\s*'+re.escape(match.group(1))+
                r'\s*,\s*"product\.editor\.[a-z]+"\s*\)',code))
    return False


METHODS={
    "tooltip":r'\.setToolTipText\s*\(\s*"((?:\\.|[^"\\])*)"',
    "caption":r'\.setText\s*\(\s*"((?:\\.|[^"\\])*)"',
    "title":r'\.setTitle\s*\(\s*"((?:\\.|[^"\\])*)"',
    "tab":r'\.addTab\s*\(\s*"((?:\\.|[^"\\])*)"',
    "border":r'\.createTitledBorder\s*\(\s*"((?:\\.|[^"\\])*)"',
    "new_label":r'new\s+(?:javax\.swing\.)?(?:JLabel|JButton|JCheckBox|JRadioButton|JMenuItem|JMenu|JToggleButton)\s*\(\s*"((?:\\.|[^"\\])*)"',
}

def skip(value):
    s=value.strip()
    if not s or len(s)<3 or s.startswith(("jdbc:","http://","https://","/com/mx/","org.apache.")):
        return True
    if re.fullmatch(r"(?:jButton|jLabel|jTextField|jCheckBox|jlbl|jline)\d*",s,re.I):
        return True
    if re.fullmatch(r"[#\d\W]+",s):return True
    return False

def scan():
    result=[]
    for file in sorted(SOURCE.rglob("*.java")):
        source=file.read_text(encoding="utf-8",errors="replace")
        for category,regex in METHODS.items():
            for match in re.finditer(regex,source,re.MULTILINE):
                value=match.group(1)
                if skip(value):continue
                result.append((str(file.relative_to(ROOT)),
                    source.count("\n",0,match.start())+1,category,value))
    return sorted(set(result))

def output():
    items=scan()
    native=set(resources.TRANSLATIONS)
    curated=sum(v in native or covered_at_runtime(path,kind,v)
                for path,_,kind,v in items)
    pending=defaultdict(list)
    categories=Counter(category for _,_,category,_ in items)
    for name,line,kind,value in items:
        if value not in native and not covered_at_runtime(name,kind,value):
            pending[name].append((line,kind,value))
    lines=[
        "# Barrido complementario de literales de interfaz — POS 1.3","",
        "## Alcance","",
        "- Busca literales en APIs Swing de textos, tooltips, títulos, pestañas y bordes.",
        "- No traduce rutas, protocolos, datos del usuario ni cadenas creadas dinámicamente.",
        "- Un hallazgo puede ser técnico, plantilla HTML o texto real: requiere clasificación.",
        f"- Coincidencias: {len(items)}; mapeadas a catálogo nativo: {curated}; por revisar: {len(items)-curated}.",
        "",
        "## Tipos detectados","",
        "| API | Coincidencias |","|---|---:|"
    ]
    for kind,count in categories.most_common():
        lines.append(f"| {kind} | {count} |")
    lines+=["","## Pendientes por clase",""]
    for name,values in sorted(pending.items(),key=lambda pair:(-len(pair[1]),pair[0])):
        lines.append(f"### {name} ({len(values)})")
        for line,kind,value in sorted(values):
            display=value.replace("|","/").replace("\n"," ")
            lines.append(f"- [ ] L{line} — {kind}: {display[:170]}")
        lines.append("")
    return "\n".join(lines)+"\n",len(items),curated,len(items)-curated

def main():
    args=argparse.ArgumentParser()
    args.add_argument("--write",action="store_true")
    args.add_argument("--check",action="store_true")
    opts=args.parse_args()
    report,found,curated,pending=output()
    if opts.write:
        OUT.parent.mkdir(parents=True,exist_ok=True)
        OUT.write_text(report,encoding="utf-8")
    if opts.check and (not OUT.is_file() or OUT.read_text(encoding="utf-8")!=report):
        raise SystemExit("UI_LITERAL_AUDIT_DRIFT")
    print(f"UI_LITERAL_AUDIT records={found} curated={curated} pending={pending}")

if __name__=="__main__":main()
