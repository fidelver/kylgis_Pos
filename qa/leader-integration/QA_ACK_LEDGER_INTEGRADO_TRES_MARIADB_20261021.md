# P0 CORE 1.3 — ACK TCP perdido con ledger durable ACTIVADO (2026-10-21)

## Modificaciones candidatas en rama
- `SaleOperationLedgerQA.connection()` acepta **exclusivamente** en QA con flag activo un URL JDBC de la forma `jdbc:mysql://127.0.0.1:3306/pos_qa_status?...useSSL=false` para aislar las transacciones del ledger del proxy TCP que destruye el ACK del método real `DataLogicSales.saveTicket`. Prohibido usar fuera del contenedor sintético; no alterar producción, driver comercial, credenciales reales ni gateway.
- `RealSaveTicketAckLossProbe` puede activar el ledger solo con `KYLGIS_QA_LEDGER_ACK_OPTIN=1`; entonces `PREPARED` y `IN_FLIGHT` se confirman en la conexión JDBC independiente, el proxy transporta la venta y omite la respuesta del COMMIT de la conexión comercial, y `holdUnknown` persiste `COMMIT_UNKNOWN`.
- Se comprueba desde conexión **NUEVA** que el registro de la operación sigue `COMMIT_UNKNOWN`, `revision=2`, que un intento real adicional de `DataLogicSales.saveTicket` con el mismo objeto recibo es rechazado por la reclamación durable y que sigue habiendo **un pago**.
- Script `run_real_ticket_status_isolated.sh` reenvía opcionalmente el flag ACK a la JVM de prueba; por defecto no cambia la ejecución comercial y no habilita ledger.

## Evidencia reejecutada de extremo a extremo
Comandos en Fedora con Desktop Commander y Podman sintético:
`KYLGIS_QA_LEDGER_OPTIN=1 KYLGIS_QA_LEDGER_ACK_OPTIN=1 KYLGIS_QA_MARIADB_VERSION=10.11 bash qa/sandbox/run_real_ticket_status_isolated.sh` y sucesivamente 11.8 y 12.3.3.

Para **cada versión**:
- `PASS ACK_LEDGER_OPERATION_PERSISTED`
- `PASS ACK_LEDGER_UNKNOWN_DURABLE`
- `PASS ACK_RETRY_SAME_RECEIPT_BLOCKED`
- `PASS ACK_NO_DUPLICATE_PAYMENT_AFTER_RETRY`
- `REAL_SAVE_TICKET_ACK_LOSS_QA_PASS`, `QA_STATUS_CONTAINER_CLEANED`

Además `REAL_SAVE_TICKET_STATUS_QA_PASS`, `REAL_VOUCHER_RACE_QA_PASS` siguen aprobando. Run conjunto 11.8+12.3.3 `ACK_LEDGER_THREE_VERSIONS_PASS`, RC0; MariaDB10.11 RC0. **12 nuevas aserciones ledger (4/versión)** y regresiones originales, no equivalen a 12 cajas reales.

## GATE NO-GO integración principal
- Cada simulación ejecutó dentro de contenedor cerrado `--network none`; no prueba POS Swing reiniciado ni cobranza previa en proveedor.
- El reintento fue con mismo recibo/operación tras error ACK. No se certifica otra caja que reconstruya la venta con nuevo `receipt_id`; faltan identidad pre-pago, ticket compartido y ledger bancario independiente, permisos/tls, timeout/recovery y conciliación autorizada.
- La conexión directa QA de root vacía jamás debe usarse en producción; permanece guardia de catálogo y opt-in explícito. No se creó migración comercial y no se tocó main, .17/.18, banca externa, SAT/CFDI/PAC.
