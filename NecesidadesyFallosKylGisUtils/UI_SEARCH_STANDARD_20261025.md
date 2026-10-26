# Homologación de búsqueda de productos — 2026-10-25

## Alcance
- Se creó `ProductSearchDialog`, buscador modal de una sola línea con coincidencias explícitas por nombre, referencia, código y categoría.
- `JProductFinder` delega al buscador estándar, conservando los filtros de producto normal, auxiliar y general de cada consumidor.
- La lupa de Productos y Productos por almacén usa el mismo selector y reposiciona el registro elegido.
- Los paneles de Producción de recetas y Conversión de UOM conservan su modelo de datos y agregan el selector modal con nombre, referencias, código y categoría.
- Se mantienen Aceptar/Cancelar, doble clic, Enter, Escape y selección vacía sin elegir automáticamente la primera coincidencia.

## Verificación
- Compilación Java 21: `JAVA_HOME=/usr/lib/jvm/java-21-temurin-jdk mvn -q -DskipTests package` — SUCCESS.
- No se modificó producción `.17`; la validación corresponde al worktree canónico y laboratorio `.18`.

## Limitación de KylGis Recursos
KylGis Recursos se utilizó para leer, buscar y editar el código. La compilación Maven se ejecutó mediante Desktop Commander porque KylGis Recursos no expone ejecución de procesos/build; se usó únicamente el dispositivo Fedora autorizado y no se tocaron procesos de producción.
- Se agregó el acceso al buscador estándar también al selector de producto de MaterialFilter, conservando la clave seleccionada que usa el filtro.
- Segunda verificación: JAVA_HOME=/usr/lib/jvm/java-21-temurin-jdk mvn -q test — SUCCESS.
- La instancia de laboratorio fue relanzada con el JAR recompilado y permanece activa (PID de Java 64885); las instancias .17 permanecen intactas.


## Segunda pasada: lupas genéricas de inventario

Se sustituyó el diálogo antiguo de tres campos (Buscar/Donde/Condición) de JNavigator por StandardSearchDialog. La nueva ventana busca en una sola línea sobre todos los valores visibles entregados por el Vectorer, muestra cada coincidencia completa y exige selección explícita antes de reposicionar el registro.

Quedaron cubiertos automáticamente los paneles que usan el navegador común, incluidos:

- Productos auxiliares (AuxiliarPanel)
- Categorías (CategoriesPanel)
- Bundle de productos (BundlePanel)
- Atributos de producto (AttributeUsePanel)
- Valores de atributo (AttributeValuesPanel)

El selector de productos especializado y los selectores de recetas conservan su búsqueda contextual por nombre, referencia/código y categoría.

Validación posterior:
- mvn -q -DskipTests package con Java 21: correcto.
- mvn -q test con Java 21: correcto.
- Instancia de laboratorio .18 reiniciada con el jar actualizado, PID 70503; sin excepciones de arranque.
- .17 de producción no fue detenido ni modificado.


## Corrección de empaquetado

La primera ejecución incremental de package encontró una clase temporal de una compilación previa al ensamblar el JAR. Se eliminó únicamente target del worktree de laboratorio (sin tocar fuentes ni producción), y la reconstrucción limpia terminó correctamente:
- mvn -q clean package con Java 21: correcto.
- mvn -q test con Java 21: correcto.
- Instancia de laboratorio .18 reiniciada con el JAR limpio, PID 71672; arranque correcto y sin excepciones.


Ajuste final de formato: se retiraron líneas vacías generadas durante la sustitución del bloque de navegación. `mvn -q package` volvió a terminar con código 0 y la instancia quedó reiniciada con PID 72633.


Validación final del mismo árbol de fuentes: `mvn -q test` con Java 21 terminó con código 0 después del ajuste de formato.


## Ajuste de filtro superior de Productos — 2026-10-25

- Se homologó el filtro de producto con el selector modal usado en Recetas: nombre, código y referencia se eligen desde un botón y se muestran como producto seleccionado.
- Se conservó únicamente Categoría como filtro visible; los filtros de precio de compra y precio de venta quedaron fuera de la interfaz y generan condiciones NONE.
- El botón de la fila del producto ahora lista los resultados con un icono de lista; el botón conserva la consulta existente y reutiliza el filtro de categoría y la clave código/referencia seleccionada.
- Se retiraron del navegador de Productos los dos controles circulares redundantes de recarga/listado; el listado se ejecuta desde el filtro superior.
- Se mantuvo la búsqueda del navegador de Productos mediante el selector modal estándar.
- Verificación: `mvn -q package` y `mvn -q test` con Java 21 finalizaron con código 0.
- Instancia de laboratorio reiniciada con el artefacto canónico, conectada a `kylgis` en `.18`: PID 80985. El log de arranque no reporta excepciones ni ClassNotFoundException.
- Producción `.17` no fue modificada.


### Ajuste posterior

- Se sustituyó la referencia de tooltip “Recargar lista completa” por “Buscar productos”, ya que el botón ejecuta la consulta con los filtros seleccionados.
- Tras recompilar y pasar las pruebas nuevamente, la instancia de laboratorio quedó reiniciada con PID 82604.


### Corrección del navegador de Productos — 2026-10-25

- Se verificó la captura: el control tachado corresponde a la recarga del registro actual y se eliminó únicamente ese botón.
- Se restauró el botón circular de recarga/listado completo posterior a las flechas, necesario para limpiar/recargar el estado del filtro y la categoría.
- Compilación y pruebas repetidas con código 0.
- Instancia de laboratorio reiniciada contra `kylgis` en `.18`: PID 84519.


### Corrección de limpieza de filtros — 2026-10-25

- El botón circular conservado ahora ejecuta una acción específica de Productos: limpia la selección de nombre/código/referencia, restablece Categoría al valor inicial y recarga el listado completo.
- El botón superior con icono de lista conserva los filtros actuales y vuelve a ejecutar la consulta.
- Verificación: `mvn -q package` y `mvn -q test` finalizaron con código 0.
- Instancia de laboratorio reiniciada contra `kylgis` en `.18`: PID 87489; sin excepciones de arranque.


### Reubicación del botón de limpieza — 2026-10-25

- Se añadió un segundo botón junto al botón de listado en el filtro superior.
- El botón circular conserva la limpieza de Categoría y del producto seleccionado (nombre/código/referencia), y después muestra todos los productos.
- El botón circular ya no permanece en la barra inferior; el control tachado sigue eliminado.
- Verificación: mvn -q package y mvn -q test con código 0.
- Laboratorio .18 reiniciado con PID 89120; sin excepciones de arranque.


### Ajuste final de estado legado — 2026-10-25

- El botón circular superior también limpia los controles internos heredados de nombre y código/referencia, además de Categoría.
- El botón de limpieza quedó junto al botón de listado en la segunda fila del filtro.
- Compilación y pruebas finales correctas.
- Laboratorio .18 reiniciado con PID 90149; producción .17 sin cambios.


## StockManagement / líneas de existencias — 2026-10-25

- `JInventoryLines` ahora deja la descripción dinámica para ocupar el ancho disponible; cantidad, precio y valor de compra conservan anchos máximos de 90, 100 y 120 px.
- La fila seleccionada usa un fondo azul claro y texto oscuro para mejorar el contraste y la lectura.
- El botón de recarga/consulta de existencias (`m_jBtnShowStock`) se posiciona debajo de la tabla de existencias y se fuerza al frente del `AbsoluteLayout`, evitando que quede oculto detrás del `JScrollPane`.
- Se agregó un buscador accesible debajo de Documento en `StockManagement`; reutiliza `JProductFinder` mediante la misma acción del buscador lateral y conserva la búsqueda por nombre/código/referencia/categoría.
- Validación: `mvn -q package` y `mvn -q test` finalizaron con código 0.
- Instancia de laboratorio reiniciada con el artefacto canónico, conectada a `kylgis` en `.18`; PID activo 94805. Producción `.17` no fue modificada.

- Revalidación final tras el ajuste de altura del panel: `mvn -q package && mvn -q test` finalizó con código 0; la instancia fue reiniciada con el JAR resultante y quedó activa sin excepciones en el log.


## Revalidación de carga de paneles de inventario (2026-10-24)

- Causa diagnosticada: la instancia se estaba compilando/arrancando con Java 8 aunque el proyecto exige Java 21. El JAR anterior no correspondía al estado actual del código; al abrir la sesión se producían `NoClassDefFoundError` para clases que sí están incluidas en `kylgispos.jar`.
- Corrección operativa: recompilación con JDK 21 explícito y reinicio de la instancia canónica contra la BBDD `kylgis` en `.18`.
- Verificación: `mvn -q package` y `mvn -q test` con Java 21 finalizaron correctamente; `KylGisCheck` reportó `java.compatibility=CERTIFIED`, `capability.pos=OK`, `result=OK`.
- El JAR final contiene `StockManagement`, `JInventoryLines`, `RecipeManagementPanel` y `SaleRecipeConsumptionPanel`; el log de arranque no presenta `NoClassDefFoundError` ni `ClassNotFoundException`.


## Ajuste de proporción del panel de conversiones UOM (2026-10-24)

- `ProductUomConversionPanel` ahora utiliza un `JSplitPane` horizontal.
- El editor inicia con aproximadamente 55% del ancho y la lista de conversiones con 45%, evitando que el panel derecho quede reducido a una columna estrecha.
- El divisor queda redimensionable y el cambio conserva las acciones, tabla y búsqueda existentes.
- Verificación: `mvn -q package` y `mvn -q test` con JDK 21 finalizaron correctamente; instancia canónica reiniciada sobre `.18`.


## Corrección de persistencia del divisor UOM (2026-10-24)

- Se añadieron anchos mínimos de 500 px para el editor y 480 px para la lista.
- La posición inicial del divisor se aplica después del primer layout mediante `SwingUtilities.invokeLater`, evitando que el primer toque lo devuelva al ancho preferido anterior.
- El divisor continúa siendo arrastrable, pero ya no puede colapsar la lista a una columna estrecha.
- Se volvió a ejecutar `mvn -q package` y `mvn -q test` con JDK 21; ambos finalizaron correctamente. La instancia canónica fue reiniciada en `.18`.


## Catálogo de Producción de recetas (2026-10-24)

- `RecipeProductionPanel` dejó de cargar todos los productos y ahora usa `RecipeUiCatalogService.listProductionProducts()`.
- El catálogo filtra por una versión `ACTIVE` cuyo `recipe_mode` sea `PRODUCTION`; por lo tanto, excluye productos sin receta y recetas de consumo por venta (`SALE_CONSUMPTION`).
- La producción sigue trabajando con la versión activa y no permite registrar productos sin receta.
- Verificación real en BBDD `kylgis` `.18`: el catálogo devolvió 2 productos elaborados (`Masa Para Pizza` y `Salsa Para Pizza`).
- `mvn -q package` y `mvn -q test` con JDK 21 finalizaron correctamente; instancia reiniciada con el JAR actualizado.


## Dirección de movimientos en historial de transformaciones (2026-10-24)

- Se homologó la semántica respecto al almacén: `INPUT` representa entrada de existencias y `OUTPUT` representa salida.
- Producción y conversión registran ingredientes como `OUTPUT` y productos preparados como `INPUT`.
- Consumo de receta por venta registra los ingredientes como `OUTPUT`; la devolución crea el movimiento compensatorio `INPUT`.
- La reversión de producción/conversión conserva el historial y genera el movimiento contrario.
- El historial deriva la dirección del signo de `stockdiary.units`, por lo que también corrige visualmente transformaciones antiguas sin reescribirlas.
- Se añadieron locales para `transform.type.sale_recipe_consumption=Consumo por venta` y compatibilidad con las variantes heredadas `SALES_RECIPE_CONSUPTION`/`SALE_RECIPE_CONSUPTION`.
- Verificación: `mvn -q package`, `mvn -q test` y `bash tools/test_recipes_transformations.sh` finalizaron correctamente (`KYLGIS_RECIPE_SELFTEST_OK tests=22`). Instancia canónica reiniciada sobre `kylgis` `.18`; `.17` no fue modificada.


## Filtros del reporte de almacenes (2026-10-25)

- Se añadió WarehouseProductFilter, específico para los reportes de almacenes.
- El filtro de producto ahora usa una sola línea de texto para buscar por nombre, código de barras, clave/referencia o nombre de UOM; cada término se aplica como coincidencia parcial.
- El selector de categoría se conserva como filtro independiente.
- Se eliminó el filtro de cantidad/unidades (stockcurrent.UNITS), que no identifica el producto y no era necesario para este reporte.
- El botón de restablecer limpia la búsqueda y la categoría, dejando el formulario listo para ejecutar nuevamente el informe sin criterios.
- En Existencias, el campo de texto ajusta su ancho conforme se escribe (con límites para no desplazar categoría ni limpieza); en Almacenes no se muestra un botón de listar porque la ejecución corresponde exclusivamente a «Ejecutar informe…».
- Se ajustaron inventory.bs, inventory_b.bs, inventory_broken.bs e inventory_listdetail.bs; los reportes incorporan la unión con uom cuando la requieren para permitir la búsqueda por unidad de medida.
- El reporte products.bs también quedó homologado con WarehouseProductFilter y la búsqueda por UOM; se eliminaron de ese formulario los filtros separados de precio/cantidad.
- Verificación: mvn -q package, mvn -q test y bash tools/test_recipes_transformations.sh finalizaron correctamente (KYLGIS_RECIPE_SELFTEST_OK tests=22). La instancia canónica quedó reiniciada contra kylgis en .18; .17 no fue modificada.


## Homologación de catálogo y etiquetas (2026-10-25)

- salecatalog.bs, labels_product.bs, labels_barcode_sheet.bs y labels_barcode_shelfedge.bs usan WarehouseProductFilter.
- La búsqueda común combina nombre, código de barras, clave/referencia y UOM; la categoría permanece como filtro independiente.
- Se conserva la selección de proveedor y almacén del catálogo de venta y se mantienen los campos/diseños propios de cada reporte de etiquetas.
- No se agregaron filtros de existencias, cantidad ni precios como criterios de búsqueda.
- Verificación: mvn -q package, mvn -q test y tools/test_recipes_transformations.sh (KYLGIS_RECIPE_SELFTEST_OK tests=22) terminaron correctamente; la instancia canónica usa la BBDD kylgis en .18 y el log no reporta errores de carga. La BBDD .17 no fue modificada.
