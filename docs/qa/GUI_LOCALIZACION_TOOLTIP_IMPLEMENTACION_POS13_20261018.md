# KylGis POS 1.3 — Diseño e implementación de ayuda contextual e internacionalización

## Estado verificable (18/oct/2026)

- Rama aislada: `collab/pos13-locales-tooltips-20261018`, iniciada desde RC2 `360dc940759f95f351677c279667bc92e8f54f1a`.
- Núcleo funcional: `src/main/java/com/mx/kylgis/pos/ui/help/UiHelp.java`.
- Recursos internos: `src/main/resources/com/mx/kylgis/pos/ui/help/ui_help*.properties` (17 locales + catálogo).
- Generador determinista: `tools/generate_embedded_ui_help.py` y diccionarios `tools/ui_help_extra_translations.py`.
- Auditoría de código: **952 campos Swing** en **155 clases**; 108 candidatos literales, de los que 78 tienen traducción curada; los restantes requieren examen semántico, son técnicos o HTML de ejemplo.
- Compilación Maven offline Java 21 satisfactoria. Smoke inicial de idiomas: **17 locales y 153 controles simulados**, incluidos panels Plataformas, Botonera de ventas y configuración de báscula; inyección de archivo externo en classpath rechazada, recursos consultados desde el JAR. Este smoke **no significa 952 controles comprobados gráficamente**.

## Checklist de revisión por panel

El inventario exhaustivo se generó en `docs/qa/CHECKLIST_952_CONTROLES_POS13_20261018.md`. Para cada clase:
- [ ] Abrir realmente el panel y todos sus diálogos/estados en LAB `.18`.
- [ ] Hover/foco en botones e iconos sin texto: descripción del **resultado**.
- [ ] Campos: significado, unidades, formatos válidos y si el dato es obligatorio.
- [ ] Combos y radios: qué opciones se eligen y qué comportamiento cambian.
- [ ] Checks/toggles: diferencia precisa entre activado y desactivado.
- [ ] Tablas/listas: qué filas contienen, selección y acciones disponibles; cabeceras si son interactivas.
- [ ] Pestañas: título y tooltip de cada pestaña.
- [ ] Deshabilitados: motivo y condición para volver a habilitarlos cuando corresponda.
- [ ] Dialogos y acciones dinámicas: generación de tooltip al crear control.
- [ ] Texto fijo: título, etiqueta, placeholder, error, advertencia, ayuda, validación y tooltip en todos los locales.
- [ ] Idiomas y escritura RTL para árabe; no truncar en alemán, francés y portugués.
- [ ] Al cambiar la preferencia regional, el texto y tooltip provienen del locale esperado.

## Implementación técnica integrada

### 1. Establecer nombres semánticos
La clase `UiHelp` consulta:
1. La clave explícita `kylgis.help.override` (p.ej. el selector de carácter de petición de peso).
2. El texto visible o nombre accesible ya localizado.
3. La asociación `JLabel.setLabelFor` o etiqueta adyacente.
4. `ui_help_catalog.properties` con ID `paquete.Clase#campo`.
5. Un fallback localizado por tipo de control para componentes dinámicos no nombrados.

**Ejemplo de nuevo control:**
```java
JComboBox<String> requestCommand = new JComboBox<>();
UiHelp.set(requestCommand, "scale.request");
```
Se modifica una entrada `tooltip.scale.request` por locale y se ejecuta el generador. Para nuevos campos Swing con nombre estable, el catálogo puede generarse automáticamente desde el código fuente.

### 2. Seguridad de recursos y actualización
- `UiHelp.load()` utiliza la ubicación física del JAR que contiene `UiHelp.class` y lee la entrada `com/mx/kylgis/pos/ui/help/ui_help_*.properties`. No busca en directorios del usuario.
- Cambiar o agregar ayuda: editar las fuentes y diccionarios, regenerar `--write`, auditar `--check`, compilar JDK 21 y firmar/cotejar SHA256 del artefacto de release.
- Cambios de archivo local como `~/ui_help_es.properties` **no** modifican el tooltip del POS empaquetado. Sustituir o parchear el propio JAR sigue siendo técnicamente posible; hashes e instalación controlada protegen la distribución, no representan DRM.
- Locales originales de POS permanecen sin sobrescribir; los nuevos mensajes se empaquetan dentro del JAR y se resuelven al ejecutar.

### 3. Flujo para traducir títulos y demás cadenas
- Prioridad: `AppLocal.getIntString("...")` y recursos base históricos para contenido ya localizado.
- Texto nuevo: clave estable `text.<modulo>.<accion>` o `tooltip.<modulo>.<control>`, traducción en los **17** bundles.
- Literal antiguo identificado: `literal.<sha256-prefix>` vincula cadena fija existente con su traducción, sin editar archivos `.form` de NetBeans. Es capa transitoria: la migración ideal reemplaza llamadas `setText("literal")` por clave semántica.
- No traducir identificadores técnicos (JDBC URL, nombres de dispositivos, protocolos, IDs, nombres de tabla o datos introducidos por el usuario).
- Los textos HTML incrustados requieren manejo explícito del marcado; no se traducen automáticamente con el mecanismo simple de literal.

### 4. Pruebas y comandos
```bash
PYTHONDONTWRITEBYTECODE=1 python3 tools/generate_embedded_ui_help.py --check
JAVA_HOME=/usr/lib/jvm/java-21-temurin-jdk mvn -o -DskipTests package
bash tools/test_embedded_ui_help.sh target/kylgispos.jar /home/fam/POS/releases/rif-provisional-1.3-9f50d98
```
El tercero comprueba el **JAR compilado**, los 17 bundles, idioma activo, controles representativos y resistencia a override de directorio externo.

### 5. Criterios que aún requieren revisión antes del GO
- **No declarar traducidas las 109 cadenas** solo por su inventario. `docs/qa/AUDITORIA_TRADUCCIONES_ESTATICAS_POS13_20261018.md` muestra dónde siguen iguales al idioma base. Un texto igual puede ser legítimo (término técnico) o una traducción faltante: debe determinarse individualmente.
- **No declarar tooltip descriptivo en 940/940** a partir de presencia no nula: el fallback genérico «este campo» cumple una prueba estructural, pero no la exigencia semántica del usuario. El checklist enumera cada control.
- Ejecutar inventario dinámico en ventanas de ventas, pagos, periféricos, catálogos, clientes, proveedores, informes, usuarios y opciones del sistema. La UI efectivamente cargada puede incluir controles JavaFX, emergentes, componentes de terceros o acciones HTML que no aparecen en el análisis de campos `JComponent`.
- Mantener la protección de `192.168.58.17`: ninguna prueba de este módulo requiere modificar producción.

## Estrategia de cierre de la release
1. Reparar ausencias de traducción y campos dinámicos, dando prioridad a textos del flujo de venta y a configuraciones sensibles.
2. Ejecutar QA por cada locale y comparar textos visibles en componentes reales, no solo recursos.
3. Bloquear el build de release si `--check` detecta drift o si faltan entradas de bundles; agregar un gate de descripciones no genéricas para elementos prioritarios.
4. Verificar la GUI de LAB y obtener aprobación antes de modificar lanzadores productivos.


## Seguimiento de QA automático del RC3/RC4 (19/10/2026 UTC)

- Inventario ampliado a **952 controles** (incluye 12 JLabels con clic) en 155 clases; 76 identidades requieren verificación semántica antes de marcar el checklist completo.
- La asignación por reflexión vuelve a inspeccionar campos cuando se inicializan después de un evento de agregado en NetBeans. Se evita el fallo de cachear paneles incompletos.
- Se localizan títulos de bordes Swing anidados en CompoundBorder y se cubren los iconos con MouseListener.
- Los mensajes expertos existentes de la aplicación se copian al JAR si ya tienen traducción en el locale. Si el locale carece de esa traducción, una entrada native.* explícitamente vacía impide heredar una ayuda inglesa y se usa el mensaje contextual localizado.
- Nuevos avisos HTML: periféricos, copias espejo, impresión y marca, 17 idiomas, obtenidos con UiHelp.text().
- Catálogos curados de 78 literales no técnicos, con traducción específica para los 17 locales. Valores de base de datos, rutas y nombres de impresora no se traducen.
- Auditoría complementaria: **206 referencias** a literales en APIs Swing; **112 cubiertas por el catálogo curado** y **94 bajo revisión**. Su listado reproducible está en BARRIDO_CADENAS_UI_POS13_20261018.md.
- Ejecutar `tools/generate_embedded_ui_help.py --check`, `tools/generate_ui_help_checklist.py --check` y `tools/audit_unlocalized_ui_strings.py --check` antes del test del JAR.
- La compatibilidad de los 17 locales se comprueba leyendo recursos del **JAR resultante**, no solo los .properties sueltos; las entradas expertas sin traducción propia tienen fallback contextual y nunca se consultan en carpeta de usuario.

### Pendientes de certificación que no deben ocultarse

1. Semántica de 76 controles con nombres de origen débiles: mejorar el texto específico y comprobarlo mediante navegación real.
2. Revisión de 115 literales no cubiertos todavía por el catálogo curado (incluye HTML parcial, pruebas y textos ajenos al idioma).
3. La localización histórica de `pos_messages_*.properties` es parcial; hay claves sin traducción por idioma. No equivale a una cobertura íntegra solo porque el programa pueda usar fallback.
4. Inspección gráfica completa en LAB, incluyendo textos largos, truncamiento, accesibilidad, diálogos, reportes y RTL árabe.

Estas reservas son independientes de la compilación y no autorizan modificar el POS de producción.


## Mejoras comprobables del siguiente candidato (posteriores a RC4)

- `JPanelConfiguration`: 11 pestañas de configuración localizadas (título y tooltip) en 17 locales, una vez terminado el formulario generado de NetBeans. Incluye errores de persistencia y recarga de periféricos sin reinicio.
- `ProductsEditor`: 10 controles de formato con textos nativos en los 17 locales, incluyendo la **corrección real del botón Negritas**, cuyo tooltip decía erróneamente Cursivas.
- `JPanelConfigPeripheral`: acceso desplazable a las salidas espejo con descripción de acción específica, y aviso HTML localizado.
- `UiHelp.subject()`: ya no empareja una etiqueta de otra fila ni una etiqueta situada en coordenadas iniciales (0,0) cuando Swing aún no ha aplicado el layout. Pruebas nuevas para `JLabel.setLabelFor`, posiciones válidas y etiquetas lejanas.
- Barrido complementario actualizado: 206 literales detectados; 112 cuentan con traducción curada o sustitución explícita comprobada en runtime; quedan **94 hallazgos por clasificar o resolver**. Los hallazgos no implican necesariamente 94 errores visibles, pero no se consideran cerrados.
- La compilación y los tests anteriores de RC4 siguen siendo evidencia histórica; cada nueva compilación obtiene SHA y bitácora propios. Ninguna de estas modificaciones se ha aplicado al lanzador productivo RIF.


## Corrección de vocabulario y configuración de nodos (siguiente candidato)

- Vocabulario comercial nativo: 16 términos fundamentales en los 17 idiomas. Se eliminó la confusión histórica `label.product` que rotulaba «producto» como «código de barras»; ambas entradas son ahora independientes.
- Campos con nombre genérico de NetBeans: a los más relevantes se agrega contexto verificado de 26 clases (producto, cliente, proveedor, roles, categorías, impuestos, unidades, recursos, etc.). Ejemplo ES: «Capture o edite Nombre (Producto)», distinto a «Nombre (Cliente)».
- El generador quita prefijos NetBeans sin valor descriptivo (jBtn, jCombo, m_jCbo, jCheck, etc.), independientemente de mayúsculas/minúsculas.
- Los 9 roles de nodos usan texto localizable; su tooltip dinámico describe qué servicios activa cada rol y no exhibe `node.roles=...`.
- Los 13 títulos/etiquetas del formulario Nodos y cuatro campos importantes usan recursos locales dentro del JAR, asociaciones `JLabel.setLabelFor` y ayudas explícitas. Los botones iconográficos para subir o bajar un módulo describen la modificación del orden.
- Test incorporado: instancia real `JPanelConfigNodes`, nueve casillas con tooltip semántico y un campo de conexión; 17 locales. Validación pendiente de compilar JAR de este nuevo commit y volver a ejecutar el conjunto de regresión.
