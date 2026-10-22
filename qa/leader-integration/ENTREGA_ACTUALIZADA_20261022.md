# Entrega actualizada — 2026-10-22

Candidato de revisión: `collab/leader-pos-recovery-flow-20261022`, basado en la consolidación `23b90ee4c6f4d59370f30e204a0006110316c4d3`. El commit final se obtiene del HEAD de esta rama. No se reemplazan los commits de procedencia ni main.

## Avance verificable
- POS Swing: cuatro regresiones corregidas y reproducidas contra el candidato anterior: división que eludía conciliación, sobrescritura de caja/fecha, descarte de venta pendiente y eliminación del contexto financiero sin líneas. La prueba usa componentes Swing reales, JDBC y JVM separadas, con servicios periféricos sintéticos.
- Matriz Swing MariaDB10.11/11.8/12.3.3: RC0 en las tres, 135 aserciones Java por versión (405 en total), fallo inyectado RC1, salida de JVM/sockets y limpieza comprobadas. Se ensaya recuperación repetida, mezcla efectivo/tarjeta, referencia bancaria, cupón y contexto sin líneas.
- Maven Java21 offline package -DskipTests final: RC0. No equivale a Surefire. CORE de laboratorio SHA256 e9b36567241e65f62ea1cd641d72a9dca246143dab9d8c9205d4d45ada672369. El snapshot temporal se eliminó; el JAR sellado permanece en /tmp con ese hash en el nombre y metadata Git UNSET; no está instalado.
- Kitchen con el nuevo CORE: RC0; ocho comandas, botones Terminar/Recuperar, F12 y cierre comprobados; fallo gráfico inyectado RC1 y las cuatro JVM liberaron PID/sockets. Monitor reproducible ce5b63389b07122370c8c270aafe2a9af5eec9a64c22bd732ef38b3fbc2bc098. Evidencia qa-kitchen-pos-recovery-20261022.txt. Inspección final: cero contenedores y redes propios de Swing/Kitchen.
- SAT/ledger/DDL/banco sintético y RIF/TLS: fuentes sin cambios respecto de la consolidación anterior; conservan su evidencia histórica (106 checks por versión en banco sintético; 37 en el servicio RIF de staging). No se presentan como pruebas reejecutadas hoy.

Detalle y reproducción: `QA_SWING_PAYMENT_RECOVERY_20261022.md`; evidencia en `evidence/qa-swing-recovery-*-20261022.txt`. El informe `ENTREGA_CONSOLIDADA_20261021.md` es histórico: sus hashes de contenido corresponden al candidato de ese día, no a estos dos componentes GUI corregidos.

## Cierre operativo pendiente
Se avanza el flujo de recuperación estándar del punto 2 previo; no se certifica una sesión comercial completa. Continúan pendientes el banco autorizado y coordinador previo al diálogo, decisión autorizada de conciliación, reclamación durable y fallos de suspensión estándar, modos simple/restaurante, migración/retención, voucher tender legacy, RIF operativo y Windows/segunda pantalla. No hay desbloqueo ni recobro automático.

GO revisión del laboratorio; NO-GO producción. No se modifican .17/.18, POS activo, main, gateway, cuentas/certificados reales ni SAT/CFDI/PAC. El trabajo incierto previo de Utils sigue intacto; sus limitaciones y el fallback están registrados en NecesidadesyFallosKylGisUtils/SWING_QA_BUILD_BLOCK_20261022.md.
