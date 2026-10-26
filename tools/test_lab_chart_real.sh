#!/usr/bin/env bash
# Validación de un gráfico Jasper con filas reales de LAB .18 (SELECT-only).
# No cambia producción, no imprime, no exporta datos personales.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "$0")/.." && pwd)"
CONFIG="${1:?Uso: $0 <configuracion-LAB.properties> <directorio-release-leible>}"
RELEASE="${2:?Falta release legible}"
JDK=/usr/lib/jvm/java-21-temurin-jdk
test -r "$ROOT/target/kylgispos.jar"
test -r "$RELEASE/lib/jasperreports-6.21.5.jar"
test -r "$RELEASE/lib/openpdf-1.3.32.jar"
TMP="$(mktemp -d /tmp/kylgis-real-chart.XXXXXXXX)"
trap 'rm -rf -- "$TMP"' EXIT
cp -- "$ROOT/target/kylgispos.jar" "$TMP/kylgispos.jar"
"$JDK/bin/javac" -cp "$TMP/kylgispos.jar:$RELEASE/lib/*" -d "$TMP" "$ROOT/tools/ReportLabChartRealSmoke.java"
# Keep only the row-count summary (no customer/product content) for pagination QA.
"$JDK/bin/java" -Djava.awt.headless=true \
  -cp "$TMP:$ROOT/src/main/resources:$TMP/kylgispos.jar:$RELEASE/lib/openpdf-1.3.32.jar:$RELEASE/lib/*" \
  ReportLabChartRealSmoke "$CONFIG" \
  "$ROOT/src/main/resources/com/mx/kylgis/pos/reports/sales_chart_productsales.jrxml" "$TMP/graph.pdf" | tee "$TMP/rows.log"
ROWS="$(sed -n 's/.*LAB_REAL_CHART_PDF_OK rows=\([0-9]*\).*/\1/p' "$TMP/rows.log")"
test -n "$ROWS"
pdftotext -layout "$TMP/graph.pdf" "$TMP/text.txt"
python3 - "$TMP/text.txt" <<'PYTXT'
from pathlib import Path
import sys
text=Path(sys.argv[1]).read_text()
for required in ('Ventas de productos', 'Periodo:', '31 mar 2026'):
    if required not in text:
        raise SystemExit(f'LAB_CHART_LOCALE_FAIL missing={required!r}')
print('LAB_CHART_LOCALE_PASS title=es period_display=31/03/2026')
PYTXT
# Extract the actual blue bars from each rendered page, not just page count.
python3 - "$TMP/graph.pdf" "$ROWS" <<'PYPAGES'
import sys
import pymupdf
from PIL import Image
from pathlib import Path
pdf=Path(sys.argv[1])
rows=int(sys.argv[2])
with pymupdf.open(pdf) as doc:
    expected_pages=(rows+14)//15
    if len(doc)!=expected_pages:
        raise SystemExit(f'LAB_CHART_PAGE_COUNT_FAIL expected={expected_pages} actual={len(doc)}')
    page_bars=[]
    for i,page in enumerate(doc):
        pix=page.get_pixmap(matrix=pymupdf.Matrix(1,1),alpha=False)
        im=Image.frombytes('RGB',(pix.width,pix.height),pix.samples)
        colors=im.load()
        occupied=[]
        for y in range(90,min(im.height,660)):
            pixels=sum(1 for x in range(160,min(im.width,570))
                if 30<=colors[x,y][0]<=90 and 115<=colors[x,y][1]<=190
                and 200<=colors[x,y][2]<=255)
            occupied.append(pixels>=10)
        bars=0;start=None
        for idx,present in enumerate(occupied+[False]):
            if present and start is None:start=idx
            elif not present and start is not None:
                if idx-start>=3:bars+=1
                start=None
        expected=min(15,rows-i*15)
        if bars!=expected:
            raise SystemExit(f'LAB_CHART_BAR_COUNT_FAIL page={i+1} expected={expected} actual={bars}')
        page_bars.append(bars)
print(f'LAB_CHART_PAGINATION_VISUAL_PASS pages={expected_pages} bars_per_page={page_bars} total_bars={sum(page_bars)}')
PYPAGES
pdftoppm -f 1 -singlefile -scale-to 900 -png "$TMP/graph.pdf" "$TMP/graph" >/dev/null 2>&1
python3 - "$TMP/graph.png" <<'PY'
import sys
from PIL import Image
im = Image.open(sys.argv[1]).convert('RGB')
w, h = im.size
roi = im.crop((int(w*.05), int(h*.24), int(w*.95), int(h*.92)))
colored = sum(1 for r,g,b in roi.get_flattened_data() if max(r,g,b)-min(r,g,b)>28 and min(r,g,b)<220)
ink = sum(1 for r,g,b in roi.get_flattened_data() if min(r,g,b)<120)
if colored < 100 or ink < 1000:
    raise SystemExit(f'LAB_CHART_EMPTY_OR_BLANK colored={colored} ink={ink}')
print(f'LAB_CHART_VISIBLE_PASS colored_pixels={colored} ink_pixels={ink}')
PY
