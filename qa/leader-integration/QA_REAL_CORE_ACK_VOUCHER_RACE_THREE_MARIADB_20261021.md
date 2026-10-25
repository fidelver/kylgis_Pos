# Certificación líder P0 — CORE 1.3 real JDBC: status + voucher concurrente + ACK COMMIT (2026-10-21)

## Identidad, alcance y limpieza
Rama exclusiva `collab/leader-core-sat-integracion-20261021`, padre ya guardado `4f6c625fb379c14a037186e814649468533483eb`; original CORE13 sellado SHA256 `ecc1047d67df07b7178b2bad27eec9d931332bb43285e9a840da3835d86fce2b`. No cambios main, .17/.18, producción, banco/PAC, driver productivo.
QA sintética reproducible: `qa/sandbox/run_real_ticket_status_isolated.sh` recompila DataLogicSales/TicketInfo REALES desde este worktree, utiliza BatchSentenceResource original y JAR sellado como base, MySQL driver 5.1.39 dentro de contenedor Podman `--network none` con datos tmpfs y cleanup trap. Las clases compiladas localmente se cargan antes del JAR sellado. Backend MariaDB y proxy TCP solo loopback dentro del contenedor. Los 3 tests Java son parte del script, no de Surefire.

## Escenarios ejecutados por líder mediante Desktop Commander
Para CADA MariaDB 10.11, 11.8, 12.3.3:
A) `RealTicketStatusSaveProbe`: ticketId42, status7, ticket ajeno ID7/status8, método REAL `DataLogicSales.saveTicket`, backend conserva 7/8 respectivamente; ticketline, receipt y payment consistentes. **10/10 PASS**.
B) `RealVoucherDiscountRaceProbe`: dos sesiones/threads con la MISMA identidad voucher `QA-RACE-ID` e inicio sincronizado, método REAL en ambas. EXACTAMENTE una venta CONFIRMED y la otra rechazada por estado del voucher. Una redemption ACTIVE, un ticket/receipt/line/payment nuevos. **13/13 PASS**.
C) `RealSaveTicketAckLossProbe`: proxy copia protocolo MySQL COM_QUERY hasta `COMMIT`, recibe el OK real de MariaDB y resetea socket antes de entregar ACK al JDBC legacy; `saveTicket` reporta BasicException; nueva conexión JDBC a MariaDB confirma EXACTAMENTE un receipt, ticket, línea, payment, voucher_redemptions y voucher STATUS='D'. **10/10 PASS**. Este caso demuestra que error de COMMIT **NO** significa rollback confirmado.
Total **33 checks/versión, 99/99 PASS combinados**, código de salida 0 en los tres ensayos, `QA_STATUS_CONTAINER_CLEANED` en cada uno. También se ejecutó `bash -n` del runner sin errores.

## Distinciones de seguridad
- Esta ruta de descuento `VoucherDiscountInfo` utiliza `consumeVoucher` con `WHERE ID=?,VOUCHER_NUMBER=?,STATUS='A'` y exige affected=1. La QA SAT negativa de código histórico recreó otro método `getVoucherNonActive` incondicional y pago `voucher`; NO convertir esa reproducción en una falla demostrada de esta rama CORE 1.3.
- Las pruebas no integran pasarela bancaria, retorno transaccional idempotente persistente, interfaz Swing, tickets suspendidos, reinicio de POS ni estructuras realmente desplegadas. El proxy certifica la ventana COMMIT posterior al OK sólo en el laboratorio.
- Actualmente `Transaction.execute()` envuelve SQLException de `Session.commit()` en `BasicException("Transaction error", eSQL)` indistinguible para `JPanelTicket` de algunos errores de transacción; allí el catch `saveTicket` ejecuta `ticket.setTicketId(0)` incluso cuando COMMIT pudo persistir. **Se requiere gate fail-closed**, correlación durable de operación/recibo, reconciliación autorizada antes de retry/recargo/canje; no desplegar hasta certificar.
- Status setter y orden de parámetros corregidos como candidatos aislados, QA real tres versiones PASS; aún falta certificar ticket editado/status históricos, tickettype múltiple y TICKETID duplicados sin unique en esquema diferente.

## Siguientes gates
1. Diseñar y probar excepción explícita de `COMMIT_OUTCOME_UNKNOWN` en infraestructura transaccional, sin false rollback; UI debe bloquear acciones duplicadas y no cambiar folio a cero en estado incierto. Persistencia segura tras reinicio pendiente.
2. Caja bancaria independiente y pagos mixtos reales, sin pagos adicionales ni reembolso automático.
3. Revisar migraciones / índices antes de tocar schema, con autorización separada del operador.
4. Gate RIF sigue separado; G3 no autorizado.
