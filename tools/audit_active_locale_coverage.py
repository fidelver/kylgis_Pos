#!/usr/bin/env python3
"""Count only Java i18n resource keys truly referenced by POS sources."""
from pathlib import Path
from collections import Counter
import re
import generate_embedded_ui_help as helpgen

ROOT=Path(__file__).resolve().parents[1]
LOCALES=("al_SQ","ar","da","de","el","en_US","es","es_AR","es_MX",
         "et","fr","hr","it","nl","pt","pt_BR")
PATTERNS=(r'AppLocal\.getIntString\(\s*"([^"]+)"',
          r'\bbundle\.getString\(\s*"([^"]+)"')
RES=ROOT/"src/main/resources"
OUT=ROOT/"docs/qa/COBERTURA_LOCALES_LEGACY_ACTIVOS_POS13_20261019.md"

def load(name):
    return helpgen.read_bundle(RES/("pos_messages"+
        ("_"+name if name else "")+".properties"))

def scan():
    counter=Counter()
    sources={}
    for file in sorted((ROOT/"src/main/java").rglob("*.java")):
        code=file.read_text(encoding="utf-8",errors="replace")
        for pattern in PATTERNS:
            for key in re.findall(pattern,code):
                counter[key]+=1
                sources.setdefault(key,file.relative_to(ROOT))
    return counter,sources

def audit():
    count,sources=scan()
    base=load("")
    used={name:n for name,n in count.items() if name in base}
    absent_base=[name for name in count if name not in base]
    by_locale={}
    for locale in LOCALES:
        region=load(locale)
        parent=load(locale.split("_")[0]) if "_" in locale else {}
        effective={**parent,**region}
        lacking=[key for key in used if key not in effective]
        by_locale[locale]={
            "own":len(region),"effective":len(effective),
            "missing":len(lacking),
            "references":sum(used[key] for key in lacking),
            "top":sorted(lacking,key=lambda x:(-used[x],x))[:25],
        }
    return used,absent_base,by_locale,base,sources

def output():
    used,not_base,locales,base,sources=audit()
    lines=[
        "# Traducciones heredadas utilizadas — KylGis POS 1.3",
        "",
        "Esta auditoría cuenta claves literales utilizadas por llamadas Java",
        "AppLocal.getIntString y bundle.getString, no claves dinámicas o de terceros.",
        "Las variantes regionales heredan primero de su lengua principal.",
        "",
        f"- Claves Java utilizadas y presentes en base: {len(used)}.",
        f"- Claves referenciadas sin valor en base: {len(not_base)}.",
        "- Una entrada ausente produce fallback; no es traducción nativa revisada.",
        "",
        "| Locale | Claves propias | Claves efectivas | Ausentes de la lengua | Usos faltantes |",
        "|---|---:|---:|---:|---:|",
    ]
    for locale,row in locales.items():
        lines.append(f"| {locale} | {row['own']} | {row['effective']} | {row['missing']} | {row['references']} |")
    lines.extend(["","## Prioridades de revisión",""])
    for locale,row in locales.items():
        lines.append(f"### {locale} — {row['missing']} entradas por completar")
        for key in row["top"]:
            preview=base[key].replace("|","/").replace("\n"," ")[:85]
            lines.append(f"- [ ] {key} ({used[key]} usos) — base: {preview} — {sources.get(key,'')}")
        lines.append("")
    if not_base:
        lines.extend(["## Claves Java sin equivalente base", ""])
        for key in sorted(not_base):
            lines.append(f"- {key} — {sources.get(key,'')}")
    lines.extend(["","## Compuerta de certificación", "",
        "No declarar la localización integral terminada sin completar las ausencias,",
        "evaluar los textos dinámicos y realizar inspecciones GUI en los 17 locales.",
        "La ayuda sellada dentro del JAR se certifica por separado.",
        ""])
    return "\n".join(lines)+"\n",len(used),locales

if __name__=="__main__":
    import argparse
    parser=argparse.ArgumentParser()
    parser.add_argument("--write",action="store_true")
    parser.add_argument("--check",action="store_true")
    opts=parser.parse_args()
    report,n,locales=output()
    if opts.write:
        OUT.parent.mkdir(parents=True,exist_ok=True)
        OUT.write_text(report,encoding="utf-8")
    if opts.check and (not OUT.is_file() or OUT.read_text(encoding="utf-8")!=report):
        raise SystemExit("ACTIVE_LOCALE_LEGACY_COVERAGE_DRIFT")
    print("ACTIVE_LOCALE_COVERAGE_PASS keys="+str(n)+" locales="+str(len(locales))+
          " untranslated="+str({k:v["missing"] for k,v in locales.items()}))
