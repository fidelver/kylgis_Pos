# Líder CORE / SAT — COMMIT incierto tipado y bloqueo UI provisional (2026-10-21)

## Rama y principio de seguridad
Candidato EXCLUSIVAMENTE en `collab/leader-core-sat-integracion-20261021` sobre CORE 1.3, sin merge, push, despliegue, .17/.18, cambio de driver de producción, timbrado SAT o reinicios de folios.

Se reprodujo contra el método **REAL** `DataLogicSales.saveTicket()` con MySQL Connector/J legacy y corte TCP: MariaDB procesó COMMIT y emitió OK, proxy descartó la respuesta, cliente recibió excepción pero backend conservó recibo, ticket, línea, pago, canje y voucher consumido. La ausencia de ACK **no equivale** a rollback.

## Cambios de código
- `TransactionOutcomeUnknownException` nueva clase derivada de `BasicException`, marca Phase.COMMIT/ROLLBACK y causa SQL original.
- `Transaction.execute()`: separa `begin`, `transact`, `rollback` y `commit`. Error `SQLException` en COMMIT devuelve excepción tipada, **sin intentar rollback tardío ni fingir que el COMMIT falló**; fallo de rollback también queda tipado como incierto; fallo `BasicException` anterior al COMMIT con rollback satisfactorio conserva su tipo original.
- `JPanelTicket.closeTicket()`: al recibir excepción incierta registra `TicketInfo` property `kylgis.commit.outcome.unknown=true`, conserva ID de recibo/folio provisional y muestra alerta de conciliación obligatoria. La misma instancia de ticket no puede volver a intentar `closeTicket` mientras permanezca marcada. En fallos pre-COMMIT con rollback confirmado se conserva el comportamiento anterior de reset de folio.
- El flag viaja mediante `TicketInfo.copyTicket()` y `TicketInfo.writeExternal/readExternal()` (NO garantiza conservación de pagos/vouchers, que Externalizable omite históricamente).

## Pruebas ejecutadas personalmente
En 3 contenedores aislados MariaDB 10.11, 11.8 y 12.3.3, JDK21 y esquema completo POS con cargador original:
- `RealTicketStatusSaveProbe`: 10/10 PASS versión, guarda folio42/status7 sin modificar ticket ajeno ID7/status8.
- `RealVoucherDiscountRaceProbe`: 13/13 PASS versión, dos transacciones reales solo una consume el voucher de descuento.
- `RealSaveTicketAckLossProbe`: 12/12 PASS versión, `COMMIT_FAILURE_TYPED_UNKNOWN` + `COMMIT_FAILURE_PHASE_RECORDED` + server OK recibido y oculto al cliente; JDBC nuevo confirma estado durable.
- `UnknownCommitFlagSerializationProbe`: 4 verificaciones de marcado/copias/serialización/folio-status por versión, PASS.
- Total **39 comprobaciones/versión, 117 PASS, RC0**, limpieza correcta en todas. `bash -n` runner válido.
- Maven offline JDK21 sobre rama sucia previa: `f90785ce11f900a904c84407`, BUILD SUCCESS RC0, 700 fuentes (incluye nueva excepción y JPanelTicket).
- **No hay pruebas UI Swing end-to-end**, ni simulador bancario real, ni certificación de reanudación tras reboot.

## Riesgos BLOQUEANTES antes de main
1. La propiedad del ticket protege la instancia actual, pero no constituye un **ledger durable global** de operaciones inciertas que impida cobrar en nueva caja/instancia o después de reinicio. Debe diseñarse identificador durable y reconciliación con DB + banco, bloqueo de pagos en todos los dispositivos, rollback de UI, sin pérdida de voucher/payment serializados.
2. Falta prueba gráfica de flujo completa tras falla de COMMIT, suspensión/recuperación y comportamiento del botón de cobro.
3. Falta cierre de transacción bancario APPROVED/UNKNOWN y correlación por componente de pago, con idempotencia sostenible en esquema POS vigente.
4. Código base no ha sido promovido; no declarar release GO sólo por compilation ni por QA aislada.
