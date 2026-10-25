#!/usr/bin/env python3
"""Build an exact, reviewable UI tooltip / translation checklist from Swing sources.
No database access and no external dependencies.
"""
from pathlib import Path
from collections import defaultdict
import argparse
import re
import generate_embedded_ui_help as helpgen

ROOT=Path(__file__).resolve().parents[1]
DOC=ROOT/"docs/qa"
WEAK={"setting","name","text","label","button","field","check","list","table","combo"}

def prepare():
    entries=helpgen.collect_controls()
    groups=defaultdict(list)
    weak=[]
    for identifier,(kind,subject) in sorted(entries.items()):
        owner,name=identifier.split("#",1)
        groups[owner].append((name,kind,subject))
        if not subject or subject.strip().lower() in WEAK:
            weak.append((identifier,kind,subject))
    bullets=[
        "# Checklist de tooltips localizados — KylGis POS 1.3",
        "",
        f"- Inventario derivado del código: **{len(entries)} controles** en "
        f"**{len(groups)} clases/paneles**.",
        "- Incluye etiquetas Swing con manejador de clic, además de botones, campos, "
        "checks, combos, tablas, pestañas y otros controles.",
        f"- **{len(weak)} controles** presentan una identidad fuente genérica o ambigua: "
        "su descripción se debe confirmar en la GUI y sustituir por un significado concreto.",
        "- Cada casilla se marca solo tras comprobar el tooltip visible y la traducción en "
        "los locales que existen, sin sustituir nombres de productos, códigos ni datos introducidos.",
        "",
        "## Validación funcional por panel",
        "",
        "- [ ] Todos los controles están presentes; probar estados deshabilitado/habilitado.",
        "- [ ] Tooltip describe la acción o el dato esperado, con unidades o riesgos cuando proceda.",
        "- [ ] El texto de botones, etiquetas, avisos, errores y títulos está localizado.",
        "- [ ] Textos correctos al cambiar locale y abrir nuevamente el panel.",
        "- [ ] Iconos clicables, campos creados dinámicamente y cuadros modales cubiertos.",
        "- [ ] Ningún tooltip depende de archivos externos al JAR.",
        "- [ ] No se altera el contenido introducido por el usuario al localizar la interfaz.",
        "",
        "## Inventario estático completo",
        "",
    ]
    for owner,items in sorted(groups.items()):
        bullets.append(f"### `{owner}` ({len(items)} elementos)")
        for name,kind,subject in items:
            warning=" **REVISIÓN SEMÁNTICA**" if subject.lower() in WEAK else ""
            bullets.append(f"- [ ] `{name}` — `{kind}` — {subject}{warning}")
        bullets.append("")
    bullets.extend(["", "## Identidades poco descriptivas detectadas", ""])
    for identifier,kind,subject in weak:
        bullets.append(f"- [ ] `{identifier}` — `{kind}` — `{subject or '(sin nombre)'}`")
    output="\n".join(bullets)+"\n"
    return output,len(entries),len(groups),len(weak)

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--write",action="store_true")
    parser.add_argument("--check",action="store_true")
    options=parser.parse_args()
    DOC.mkdir(parents=True,exist_ok=True)
    result,controls,classes,weak=prepare()
    filename=DOC/f"CHECKLIST_{controls}_CONTROLES_POS13_20261018.md"
    if options.write:
        filename.write_text(result,encoding="utf-8")
    if options.check:
        if not filename.is_file() or filename.read_text(encoding="utf-8")!=result:
            raise SystemExit("UI_HELP_CHECKLIST_DRIFT: regenerate with --write")
    print(f"UI_HELP_CHECKLIST_PASS controls={controls} panels={classes} semantic_review={weak} path={filename}")

if __name__=="__main__":
    main()
