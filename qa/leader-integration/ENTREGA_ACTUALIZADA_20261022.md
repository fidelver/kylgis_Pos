# Entrega actualizada — 2026-10-22

Candidato actual de revisión: **collab/leader-pos-durable-suspend-20261022**, desde `5f2f73b2c382987b876f346014016c0e0ea3a04e`. Obtener el commit de entrega del HEAD de esta rama. Incluye la consolidación CORE/SAT/RIF anterior; Kitchen continúa en su rama independiente `9ea6b3fd8c4eff8526f8b826cc5dad3545ba6dec`. No hay push, merge ni despliegue.

## Resultado actual
- Recuperación financiera estándar durable: la fila compartida permanece mientras se abre el ticket; CAS por propietario de 20 caracteres compatible con LOCKED VARCHAR(20). Guardado y liberación del candado en un UPDATE atómico. Otro propietario no puede sobrescribirlo ni liberarlo.
- Error de suspensión: Desactivar/Nuevo/Hold/selector respetan la negativa, conservando modelo/ID/candado. La serialización de estos caminos falla antes de SQL; no escribe CONTENT NULL. Login respeta una desactivación rechazada.
- Operación identificada sin componentes: mantiene la marca de conciliación tras recuperar. No se habilita un segundo cobro ni se aplica una decisión bancaria automática.
- Matriz Swing MariaDB10.11/11.8/12.3.3 con ancho legacy real: todas RC0, **383 aserciones por versión / 1,149 totales**, más las comprobaciones de SIGKILL de una JVM propia, reinicio sin apropiación del candado, dos JVM en carrera con WIN/LOSE, fallos reales de serialización/SQL y limpieza. También pasó en las tres versiones el corte real del ACK del UPDATE de suspensión: DB confirmada, GUI retenida y propietario obsoleto sin capacidad de sobrescribir. Tres regresiones reproducidas contra el CORE anterior quedan en evidencia separada.
- Regresión CORE/SAT/banco sintético reejecutada en MariaDB10.11: RC0, serialización28, dos cajas con una venta y banco106; ACK perdido y guardias preservadas. El fixture de la carrera usa plantilla sin contexto y prepara el UUID después; no se elimina una guardia productiva para pasar la prueba.
- Maven Java21 offline package -DskipTests: RC0. CORE de laboratorio SHA256 **9a41467bf17b50f7d4a88697d084e462324795ca4ad1485714507ec032ecd1b8**, guardado en /tmp/kylgis-core-durable-suspend-<SHA>.jar. Metadata Git UNSET del snapshot, no release comercial. Snapshot eliminado.
- Kitchen reejecutado con ese CORE: RC0; ocho comandas, botones Terminar/Recuperar, F12 y cierre. Fallo inyectado RC1, cuatro JVM liberadas. Monitor ce5b63389b07122370c8c270aafe2a9af5eec9a64c22bd732ef38b3fbc2bc098, reproducible e idéntico al anterior.
- Inspección final: cero contenedores/redes propios restantes. El JAR intermedio 18ab03f8… anterior al ajuste del ancho del propietario se excluyó y eliminó; no debe confundirse con el candidato final.

Documentación vigente de esta iteración: `QA_DURABLE_SUSPENSION_20261022.md`. Evidencias `evidence/qa-durable-suspend-*.txt` y `evidence/qa-kitchen-durable-suspend-20261022.txt`. Los informes de commits anteriores son históricos; sus hashes, resultados y pendientes corresponden a esos candidatos.

## Condiciones pendientes
GO revisión del laboratorio; NO-GO main/producción. Se cerró la pérdida de la fila financiera al recuperar y el abandono del modelo ante fallo de suspensión estándar; no se ofrece un desbloqueo automático de ventas tras caída. Los resets de candados del harness son administración de su fixture efímera, nunca un mecanismo de conciliación del producto.

Permanecen: coordinador durable antes de la primera autorización bancaria, banco/adquirente y decisión autorizada de conciliación; modos simple/restaurante y sesión completa de cajero; fingerprint/migración/retención y despliegue coherente de clientes que respeten el candado; voucher tender legacy; RIF operativo y Windows/segunda pantalla. No se garantiza recuperar hechos anteriores a su primera persistencia ni CONTENT NULL históricos.

RIF/TLS no se modifica ni se presenta como reejecutado: conserva su evidencia previa de staging sintético (37 checks), con sus límites de identidad OS y release. No se modifican .17/.18, POS activo, main, gateway, cuentas/certificados reales ni SAT/CFDI/PAC. El job incierto de Utils permanece intacto. Las limitaciones y fallos se registran en NecesidadesyFallosKylGisUtils/DURABLE_SUSPEND_QA_20261022.md.
