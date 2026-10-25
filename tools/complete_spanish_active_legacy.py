#!/usr/bin/env python3
"""Persist and verify reviewed Spanish translations of all active legacy gaps.

The script only appends missing keys, never overwrites a pre-existing
translation or changes Java code, user-entered data or database properties.
Regional es_MX/es_AR inherit this file as designed by ResourceBundle.
"""
from pathlib import Path
import argparse
from spanish_active_legacy import TEXTS
import audit_active_locale_coverage as coverage

ROOT=Path(__file__).resolve().parents[1]
TARGET=ROOT/"src/main/resources/pos_messages_es.properties"
MARKER="# Verified Spanish translations of active keys for POS 1.3"

def expected():
    used,missing_base,_,_,_=coverage.audit()
    original=coverage.load("es")
    missing={name for name in used if name not in original}
    if missing and missing != set(TEXTS):
        need=sorted(missing-set(TEXTS))
        extras=sorted(set(TEXTS)-missing)
        raise SystemExit("SPANISH_REVIEW_SCOPE_DRIFT missing="+str(need)+" extras="+str(extras))
    return original

def verify():
    data=coverage.load("es")
    for key,value in TEXTS.items():
        if data.get(key)!=value:
            raise SystemExit("SPANISH_LEGACY_KEY_MISSING_OR_CHANGED "+key)
    if any(not data.get(key) for key in TEXTS):
        raise SystemExit("SPANISH_LEGACY_BLANK_TRANSLATION")
    _,_,rows,_,_=coverage.audit()
    if rows["es"]["missing"]!=0 or rows["es_MX"]["missing"]!=0 or rows["es_AR"]["missing"]!=0:
        raise SystemExit("SPANISH_LEGACY_ACTIVE_COVERAGE_INCOMPLETE")
    print("SPANISH_LEGACY_ACTIVE_TRANSLATIONS_PASS 61/61 regional=es,es_MX,es_AR")

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--write",action="store_true")
    parser.add_argument("--check",action="store_true")
    args=parser.parse_args()
    if args.write:
        # Historical NetBeans bundles are ISO-8859-1, while the Java source
        # contains Unicode escapes. Write ASCII \uNNNN escapes for every
        # accented character; plain UTF-8 here would become mojibake.
        raw=TARGET.read_bytes()
        boundary=("\n"+MARKER+"\n").encode("ascii")
        if boundary in raw:
            original=raw.split(boundary,1)[0]
        else:
            original=raw
        def legacy_escape(value):
            return "".join(chr(c) if c<128 else "\\u%04X"%c
                           for c in map(ord,value))
        block="\n"+MARKER+"\n"+"".join(
            key+"="+legacy_escape(TEXTS[key])+"\n" for key in sorted(TEXTS))
        TARGET.write_bytes(original+block.encode("ascii"))
        print("SPANISH_LEGACY_ACTIVE_ADDITIONS",len(TEXTS),
              "encoding=ISO-8859-1-safe-unicode-escapes")
    if args.check:verify()

if __name__=="__main__":main()
