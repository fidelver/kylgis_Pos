# Necesidad de Desktop Commander — auditoría de tooltips

Fecha: 2026-10-24

KylGis Utils fue utilizado para localizar, leer y analizar el código, el generador y los catálogos de ayuda. La ejecución allowlisted de scripts no está disponible: `kylgis_recursos_run_task` rechazó las tareas de generación/validación de UI help por no estar autorizadas. Se usa Desktop Commander únicamente como fallback para ejecutar los scripts versionados y el build, sin acceder a producción ni a bases de datos.

Alcance: worktree canónico `homologated-pos-canonical-20261023`; no se toca .17 ni producción.

Acciones realizadas con el fallback autorizado:
- Regeneración y comprobación de `tools/generate_embedded_ui_help.py`: 17 catálogos, 976 controles, 96 literales estáticos, DRIFT=0, paridad PASS.
- Regeneración y comprobación de `tools/generate_ui_help_checklist.py`: 976 controles, 159 paneles, 46 identidades ambiguas documentadas.
- Regeneración y comprobación de `tools/audit_unlocalized_ui_strings.py`: 199 literales, 158 cubiertos, 41 pendientes clasificados.
- Empaquetado Maven Java 21: PASS.
- `tools/test_embedded_ui_help.sh target/kylgispos.jar target`: PASS; 17 locales, 976 controles, 170 controles smoke, recursos sellados en el JAR y resistencia a sobrescritura externa PASS.

Motivo del fallback adicional: el build cerrado de KylGis Utils rechazó el snapshot por el estado sucio intencional del canonical (cambios previos y los cambios de esta tarea). No se alteró el índice, HEAD, ramas, base de datos ni instancia de producción.


### Instancia de revisión levantada (2026-10-24)

- Se verificó que no existía otra instancia canónica activa antes de iniciar.
- Se intentó el arranque mediante KylGis Utils; no había un runtime dedicado compatible con este worktree/commit para ejecutar el JAR canónico, por lo que se usó Desktop Commander como fallback operativo (la limitación ya queda registrada en este documento).
- Instancia iniciada: `target/kylgispos.jar`, rama `collab/homologated-pos-canonical-20261023`, Java 21 Temurin, configuración `/tmp/kylgis-canonical-kylgis18-20261024/kylgispos.properties`, BBDD de laboratorio `.18`/`kylgis`.
- El primer intento falló por accesos reflectivos de WebLaF en Java 21. Se relanzó usando los `--add-opens` definidos por `src/scripts/kylgis-java.sh` y `-Ddirname.path` del runtime.
- Verificación final: proceso Java activo (PID 514070), ventana X11 `KylGis POS - 1.3` visible en `DISPLAY=:0`, y log con consultas SQL a `applications`, `resources` y `closedcash`; no se observan excepciones de arranque.
- Estado: instancia disponible para revisar manualmente los tooltips semánticos en la UI. No se modificó producción `.17`.


### Relanzamiento confirmado (2026-10-24, 16:24 CST)

- La instancia canónica anterior no estaba activa; las instancias productivas RIF y Plataformas permanecieron intactas.
- KylGis Utils confirmó que no tenía el proceso GUI registrado. También se intentó copiar la configuración con KylGis Utils, pero su operación sólo acepta destinos relativos al proyecto; para el archivo temporal de runtime se usó Desktop Commander como fallback y queda registrado aquí.
- Se restauró la configuración de laboratorio `kylgispos.properties` (origen voucher-hotfix-lab/rif-a-home), validando `db.URL=192.168.58.18`, `db.name/schema=kylgis`, `machine.instanceid=kylgis-payment-split-lab` y `machine.maxinstances=2`.
- Instancia canónica activa: PID Java `28137`, ventana X11 adicional `KylGis POS - 1.3`, configuración `/tmp/kylgis-canonical-kylgis18-20261024/kylgispos.properties`, JAR de `homologated-pos-canonical-20261023`, Java 21 con los accesos WebLaF requeridos.
- Log confirmado: lectura de la configuración de laboratorio, arranque de `KylGis LAB Payment Split`, capacidad `pos (UI)` y consultas SQL a `resources`/`people` sin excepciones de arranque.


### Buscadores inteligentes del panel de recetas (2026-10-24)

- Se añadieron dos campos de búsqueda de una sola línea en `RecipeManagementPanel`: uno para `Producto resultante` y otro para `Producto componente`.
- La búsqueda acepta simultáneamente términos parciales de nombre, código/referencia de artículo y categoría; los términos separados por espacios deben coincidir en el mismo producto. No se solicita elegir el tipo de búsqueda.
- El catálogo ahora trae `code` y nombre de categoría mediante `LEFT JOIN categories`; los resultados muestran nombre y sus identificadores y conservan el filtro de ingredientes (`ISRECIPEINGREDIENT=TRUE`) sólo para componentes.
- Se agregaron textos localizados `recipe.catalog.search` y `recipe.catalog.search.help` a las 17 variantes de `pos_messages`; el archivo neerlandés requirió fallback de Desktop Commander porque no estaba en UTF-8 y KylGis Utils rechazó la escritura.
- Verificación: `mvn -DskipTests package` terminó en `BUILD SUCCESS`; `bash tools/test_recipes_transformations.sh` devolvió `KYLGIS_RECIPE_SELFTEST_OK tests=21`; el JAR contiene las nuevas claves españolas.
- KylGis Build Capabilities no tiene tarea habilitada para `kylgis-pos`, por lo que la compilación se ejecutó con Desktop Commander y queda documentada como fallback.
- La instancia canónica fue reiniciada con el JAR actualizado y permanece activa contra `.18/kylgis` (PID Java `31181`, ventana X11 adicional `KylGis POS - 1.3`).


### Paridad de ayuda después de agregar los buscadores

- Los dos campos nuevos elevaron el inventario a 978 controles. Se regeneraron los 17 bundles de ayuda embebida y el checklist con `generate_embedded_ui_help.py --write` y `generate_ui_help_checklist.py --write`.
- Validación final: `UI_HELP_BUNDLES=17 CONTROLS=978 STATIC_LITERAL_KEYS=96 DRIFT=0`, `UI_HELP_CHECKLIST_PASS controls=978 panels=159 semantic_review=46`, `KYLGIS_UI_HELP_KEY_PARITY_PASS`, `KYLGIS_EMBEDDED_UI_HELP_QA_PASS locales=17 testedControls=170` y `KYLGIS_UI_HELP_EXTERNAL_OVERRIDE_RESISTANCE_PASS`.
- Se volvió a empaquetar el JAR y se reinició la instancia canónica con el bundle actualizado; Java PID `32499`, ventana adicional `KylGis POS - 1.3`, configuración `.18/kylgis` confirmada.


### Selector emergente de productos en recetas (2026-10-24)

- Se reemplazó el filtrado directo que dejaba seleccionada la primera coincidencia por dos botones selectores: `Producto resultante` y `Producto componente`.
- Cada botón abre una ventana modal con una única línea de búsqueda, búsqueda por nombre/código/referencia/categoría y una lista completa de coincidencias. La lista conserva todas las coincidencias; no selecciona automáticamente la primera. El usuario debe hacer clic en el resultado y confirmar o hacer doble clic.
- Los resultados muestran nombre, referencia/código y categoría para distinguir variantes como Pepperoni chica, mediana, extra, etc.
- Si no se ha elegido un producto, el formulario deja el selector en `Seleccionar producto`; esto evita guardar accidentalmente la primera coincidencia.
- Se añadieron las claves localizadas `recipe.catalog.choose`, `recipe.catalog.select.help` y `recipe.catalog.no.results` a las 17 variantes de mensajes; neerlandés requirió fallback de Desktop Commander por codificación heredada.
- Validación: `mvn -DskipTests package` `BUILD SUCCESS`; `KYLGIS_RECIPE_SELFTEST_OK tests=22`; ayuda embebida `CONTROLS=986 DRIFT=0`, `KYLGIS_UI_HELP_KEY_PARITY_PASS`, `KYLGIS_EMBEDDED_UI_HELP_QA_PASS locales=17 testedControls=170` y resistencia a sobrescritura externa PASS.
- Instancia canónica reiniciada con el JAR actualizado: PID Java `41439`, ventana adicional `KylGis POS - 1.3`, `.18/kylgis` confirmado. Producción no fue modificada.


### Selector emergente: botones y tooltips corregidos (2026-10-24)

- Se corrigió el botón vacío de confirmación en el selector de productos de recetas. Algunas locales heredaban button.OK sin valor; la ventana ahora usa las claves propias recipe.catalog.accept y recipe.catalog.cancel, con traducción en los 17 locales.
- El botón Aceptar tiene ancho mínimo, tooltip recipe.catalog.accept.help y queda como botón predeterminado; el botón Cancelar tiene ancho mínimo y tooltip recipe.catalog.cancel.help.
- El botón que abre el selector usa recipe.catalog.choose.help; la lista conserva recipe.catalog.select.help. En español: “Pulsa Aceptar para utilizar el producto seleccionado” y “Pulsa Cancelar para cerrar sin seleccionar ningún producto”.
- Se regeneraron los bundles/checklist de ayuda: UI_HELP_BUNDLES=17, CONTROLS=986, DRIFT=0, UI_HELP_CHECKLIST_PASS.
- Validaciones: KYLGIS_RECIPE_SELFTEST_OK tests=22, KYLGIS_UI_HELP_KEY_PARITY_PASS, KYLGIS_UI_HELP_JAR_RESOURCES_PASS, KYLGIS_EMBEDDED_UI_HELP_QA_PASS locales=17 testedControls=170, BUILD SUCCESS.
- Se relanzó la instancia canónica contra la BBDD de laboratorio .18 con el JAR recompilado; PID Java actual 45655. Producción .17 no fue modificada.
- KylGis Utils no expone el worktree canónico para edición/compilación; se usó Desktop Commander como fallback autorizado para editar el worktree, compilar, regenerar recursos y relanzar.


### Corrección de codificación y ayuda automática del selector (2026-10-24)

- La captura reveló mojibake en los tooltips (`bÃºsqueda`, `selecciÃ³n`). El generador leía todos los bundles como latin1 aunque varias locales estaban en UTF-8. Ahora detecta UTF-8 y usa latin1 sólo para bundles legacy incompatibles.
- Se añadieron claves de ayuda explícita al generador para `recipe.catalog.choose.help`, `search.help`, `select.help`, `accept.help`, `cancel.help` y `no.ingredients`.
- Los botones y controles dinámicos del diálogo llaman a `UiHelp.set`, evitando que el refresco global sustituya sus tooltips por `format.button` (`Pulse «Aceptar» para realizar esta operación`).
- El texto esperado para es-MX queda: `Pulsa Aceptar para utilizar el producto seleccionado.` y `Pulsa Cancelar para cerrar sin seleccionar ningún producto.`
- Bundles regenerados: `UI_HELP_BUNDLES=17`, `CONTROLS=986`, `DRIFT=0`; pruebas de recetas/UI y paridad del JAR completadas.
- JAR recompilado con `BUILD SUCCESS` y la instancia canónica relanzada contra `.18`; PID Java actual `49508`. Producción `.17` no fue tocada.
- KylGis Utils no expone este worktree canónico para edición/compilación; se usó Desktop Commander como fallback documentado.


## 2026-10-24 — revisión funcional completa de tooltips

- Se incorporó `tools/ui_help_control_actions.py` como capa de revisión semántica basada en identidad de clase y campo. Los controles ya no dependen de frases automáticas como «setting», «btn», «tkt», «jpos» o «key»; cada acción se describe según su comportamiento real.
- Se cubrieron controles de configuración, catálogo, inventario, recetas y transformaciones, filtros y buscadores, selección de productos/categorías/unidades/impresoras/proveedores/impuestos/ubicaciones, ventas, tickets, devoluciones, pagos, reservaciones, mesas, reportes, JPOS/ERP y pestañas.
- Se conservaron los atributos propios de los componentes (por ejemplo `left`, `center`, `right`) y se mantuvo la codificación UTF-8 para evitar tildes dañadas o mojibake.
- Regeneración verificada: 17 locales, 986 controles, 96 literales estáticos, `DRIFT=0`; no quedaron marcadores internos en `ui_help_es_MX.properties`.
- Verificaciones aprobadas: self-test de recetas 22/22, paridad de recursos embebidos, checklist 986 controles/159 paneles, auditoría de literales, paridad de claves en 17 locales, recursos sellados en el JAR, carga de ayuda por locales y resistencia a sobrescrituras externas.
- Compilación final con Java 21: `BUILD SUCCESS`. La instancia de laboratorio se relanzó contra `.18/kylgis` y permanece activa con la compatibilidad reflexiva de WebLaF/XStream requerida por Java 21 (`java.base`/`java.desktop` abiertos sólo para esta ejecución de laboratorio).
- KylGis Utils no expuso el worktree local para el proceso de compilación/arranque; se utilizó Desktop Commander únicamente como fallback operativo y se deja constancia aquí. No se tocaron los procesos de producción `.17`.
