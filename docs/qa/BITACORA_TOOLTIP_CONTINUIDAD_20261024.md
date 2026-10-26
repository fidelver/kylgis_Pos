# Bitácora de continuidad — tooltips semánticos KylGis POS

Fecha: 2026-10-24
Canonical: `homologated-pos-canonical-20261023`
Rama: `collab/homologated-pos-canonical-20261023`
HEAD: `fd3759e3448954bab762a27e0b2cde1f8b2282a6`
Entorno: Java 21, laboratorio .18, esquema `kylgis`
Restricción: no tocar producción .17 ni ejecutar migraciones o despliegues.

## Objetivo

Sustituir tooltips genéricos por descripciones de la acción real del control. Un nombre visible de usuario, impresora o botón no debe convertirse en la explicación. Los desplegables deben explicar qué decisión configura.

## Cambios

- `UiHelp.java`: las acciones de menú resuelven primero su `taskname` real; después se consultan descripciones `specific.*` y `control.*`, antes de usar el caption visible.
- `tools/generate_embedded_ui_help.py`: se añadieron descripciones semánticas para menús y un fallback orientado a acción por clase de control.
- Se evita que captions estáticos encontrados en `setText` vuelvan a crear la frase genérica.
- `ui_help_restaurant_actions.py`: impresora remota de `JPanelTicket` se describe como envío de la comanda a cocina.
- `ui_help_operational_actions.py`: selector de cliente, lectura de código de barras, buscador de productos y atributos de línea tienen acciones concretas.
- Se regeneraron los 17 bundles embebidos y el catálogo de 976 controles.
- Se regeneraron checklist y auditoría de literales.

## Evidencia

- `UI_HELP_BUNDLES=17 CONTROLS=976 STATIC_LITERAL_KEYS=96 DRIFT=0`
- `UI_HELP_EMBEDDED_RESOURCE_PARITY_PASS`
- Checklist: 976 controles, 159 paneles, 46 identidades ambiguas documentadas.
- Auditoría de literales: 199 registros, 158 cubiertos, 41 pendientes clasificados.
- Maven `package` con Java 21: PASS.
- `tools/test_embedded_ui_help.sh target/kylgispos.jar target`: PASS.
- Smoke runtime: 17 locales, 170 controles, recursos cargados desde JAR, resistencia a override externo PASS.

## Regla de herramientas

Usar KylGis Utils/KylGis Recursos primero para buscar, leer, editar, compilar, probar y documentar. Usar Desktop Commander solo como fallback cuando Utils no tenga una tarea allowlisted o rechace la ejecución. Registrar el motivo en `NecesidadesyFallosKylGisUtils`.

## Pendientes para la siguiente sesión

1. Revisar en GUI los 46 controles marcados como identidad ambigua en `docs/qa/CHECKLIST_976_CONTROLES_POS13_20261018.md`.
2. Clasificar los 41 literales pendientes de `docs/qa/BARRIDO_CADENAS_UI_POS13_20261018.md`; no traducir datos de usuario ni parámetros XML.
3. Probar visualmente en laboratorio .18 los tooltips de ventas, impresora remota, recetas, inventario, cierre de caja y reportes en español y al menos un locale adicional.
4. Antes de cualquier integración, revisar diff y estado sucio del canonical; no hacer reset ni limpiar cambios previos.

