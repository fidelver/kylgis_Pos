# Recuperación de pagos en Swing — 2026-10-22

## Candidato
Rama `collab/leader-pos-recovery-flow-20261022`, derivada de la entrega consolidada `23b90ee4c6f4d59370f30e204a0006110316c4d3`. Incluye el CORE/SAT/RIF anterior; Kitchen continúa en su rama independiente. No hay push, merge a main ni despliegue.

## Fallos corregidos
1. Dividir una venta recuperada creaba un recibo nuevo antes de comprobar el bloqueo del original. La guardia se aplica ahora al origen, antes de abrir ReceiptSplit, copiar líneas o acceder a servicios.
2. JPanelTicket.setActiveTicket sobrescribía caja activa y fecha al recuperar en otro terminal. Una venta pendiente conserva ambos datos hasta la conciliación; los borradores ordinarios siguen recibiendo caja y hora actuales. El operador actual se adjunta como antes.
3. En modo estándar, deleteTicket descartaba el ticket pendiente. Ahora rechaza esa acción sin registrar una anulación ni cambiar el recibo activo.
4. saveCurrentTicket eliminaba cualquier ticket sin líneas. Ahora conserva los que requieren conciliación aunque las líneas se hayan quitado, manteniendo sus pagos, referencias y cupones.

No se incorpora desbloqueo, segundo cargo, devolución ni una decisión bancaria automática. El guardia backend existente permanece en vigor.

## Prueba ejecutable
`qa/sandbox/run_swing_payment_recovery_isolated.py` ejecuta componentes reales JPanelTicket, JTicketsBagShared y JTicketsBagSharedList, DataLogicReceipts y serialización JDBC. AppView, permisos y recursos periféricos son dobles sintéticos. No es una sesión integral con login, banco real, impresora o autorización de supervisor.

Cada ejecución crea su red Podman interna, MariaDB efímera y catálogo pos_qa_swing. Publica sólo un puerto aleatorio en 127.0.0.1, crea un usuario sintético limitado a ese catálogo y configura user.home/prefs en su directorio temporal. No usa .17/.18 ni archivos de configuración reales. Resuelve dependencias offline de los nombres del manifest del CORE sellado y usa las seis aperturas de módulos ya existentes en src/scripts/kylgis-java.sh para WebLaF.

Fuentes Java congeladas antes de compilar; no se modifica un runner en ejecución. El proceso inicial suspende mediante bag.deactivate(), persiste CONTENT real y termina antes de la JVM de recuperación. Otra JVM pulsa la lista y el botón del ticket real, recupera, comprueba contexto financiero, intenta total/dividir/descartar y vuelve a suspender. Dos recuperaciones adicionales comprueban la repetición y el caso sin líneas. Las acciones Total entran por stateTransition('='), Dividir usa doClick() cuando el botón está habilitado; en ticket vacío también se invoca el handler protegido. Descartar prueba el método que ejecuta la acción una vez aceptada la confirmación. Los diálogos se cierran sólo en ventanas propias del proceso, sin entrada global al escritorio.

Datos: dos unidades de producto sintético, pagos 4 efectivo + 6 tarjeta, referencia QA-BANK-REF, cupón descuento de 20, caja QA-ORIGIN-CASH, operación fija de fixture y fecha previa. La prueba no pretende validar la asignación contable del descuento ni ejecutar saveTicket exitoso; éstos tienen su batería SAT anterior. Sí comprueba la negativa backend de un TicketInfo recuperado antes de cualquier acceso a DB/banco.

135 aserciones Java positivas por versión: fase suspend=4; recover=33 dos veces; suspend-empty=33; recover-empty=32. Además, salida efectiva de cada JVM, ausencia de sockets por PID, fallo gráfico inyectado con RC1 y limpieza de recursos propios. La ejecución 10.11 con --verify-baseline prueba cuatro regresiones contra el CORE anterior: origen de caja, dividir, descartar y eliminación del contexto sin líneas. Cada baseline debe fallar con el marcador esperado; esos fallos no se cuentan como aceptación del candidato.

## Resultados
- MariaDB 10.11: RC0; 135 aserciones positivas, cuatro regresiones reproducidas y fallo inyectado RC1. Evidencia qa-swing-recovery-10.11-20261022.txt. Base sellada aec91a53f5af68d1f5d4c8963f43508d3471ac0bf74bd08e3bf660a171ed72ee, con los dos componentes GUI recompilados desde la rama.
- MariaDB 11.8 y 12.3.3: ambas RC0; 135 aserciones positivas por versión, fallo inyectado RC1, PID/sockets liberados y limpieza. Base sellada del nuevo CORE e9b36567241e65f62ea1cd641d72a9dca246143dab9d8c9205d4d45ada672369. Evidencias qa-swing-recovery-11.8-20261022.txt y qa-swing-recovery-12.3.3-20261022.txt. Total de la matriz: 405 aserciones Java positivas, además de las comprobaciones del runner.
- Maven Java21 offline package -DskipTests sobre snapshot temporal de las correcciones finales: RC0. No se ejecutó Surefire por ese comando. Jar de laboratorio `/tmp/kylgis-core-pos-recovery-e9b36567241e65f62ea1cd641d72a9dca246143dab9d8c9205d4d45ada672369.jar`, SHA256 e9b36567241e65f62ea1cd641d72a9dca246143dab9d8c9205d4d45ada672369. No es un release comercial: metadata Git UNSET en el snapshot.
- Kitchen con ese nuevo CORE: RC0, KITCHEN_CORE_FUNCTIONAL_QA_PASS. Ocho comandas, botones Terminar/Recuperar, F12 y cierre; cuatro JVM con PID/sockets liberados y fallo gráfico inyectado RC1. Monitor SHA256 ce5b63389b07122370c8c270aafe2a9af5eec9a64c22bd732ef38b3fbc2bc098, idéntico al anterior. Evidencia qa-kitchen-pos-recovery-20261022.txt. Inspección posterior: cero contenedores y redes propios de Swing/Kitchen restantes.
- Los intentos preliminares con error de compilación del harness, falta de opens de WebLaF o expectativa de clic en botón deshabilitado se corrigieron; no se certifican. Sus recursos propios fueron limpiados.

## Alcance pendiente
Avance del punto 2 de la entrega consolidada: suspensión/recuperación Swing estándar y protección del contexto ya tienen evidencia de proceso y DB. Falta el flujo integral de cajero, coordinador previo al diálogo bancario y decisión autorizada de conciliación. No se infiere aprobación bancaria de SQL ni se elimina ninguna marca para permitir un recobro.

La recuperación estándar sigue utilizando su protocolo histórico que elimina la fila compartida al abrirla; este trabajo no implementa una reclamación durable ni protección ante caída mientras el ticket está abierto. Tampoco certifica concurrencia del selector compartido, política de edición/anulación en modos simple/restaurante, fallos al guardar la suspensión, ni la sesión completa de esos modos. Son gates adicionales antes de liberar el POS; no equivalen a la carrera SAT ya probada con UUID durable.

Permanecen las condiciones de banco/adquirente autorizado, migración aprobada, voucher tender legacy, RIF operativo y Windows/segunda pantalla de la entrega consolidada. Los impedimentos de compilación Utils y Git nativo se registran en NecesidadesyFallosKylGisUtils/SWING_QA_BUILD_BLOCK_20261022.md.
