# CORE/SAT — integración opt-in del ledger antes de saveTicket (2026-10-21)

## Código incorporado
Rama aislada `collab/leader-core-sat-integracion-20261021`:
- `Session.openIndependentConnectionForQA()` abre conexión física independiente utilizando las credenciales en memoria del Session existente, sin serializarlas ni escribirlas en logs.
- `SaleOperationLedgerQA` tiene guardia DOBLE: `-Dkylgis.qa.durableLedger.enabled=true` y catálogo exacto `pos_qa_status`; no migra esquema automáticamente. Si falla la preparación, no llama `saveTicket()`.
- `DataLogicSales.saveTicket` ahora invoca `prepareAndClaim` antes del `Transaction.execute()` real cuando está opt-in; almacena operation UUID inmutable en propiedad TicketInfo, PREPARED confirmado y reclamo IN_FLIGHT CAS; tras COMMIT validado usa nueva conexión para confirmar recibo/ticket y pasar a DB_CONFIRMED, revisión 2. Cualquier BasicException de la venta deja el ledger IN_FLIGHT/COMMIT_UNKNOWN e impide reclamar de nuevo. Error de verificación post-commit lanza TransactionOutcomeUnknownException para preservar folio y el bloqueo UI.
- Con opt-in apagado (valor **por defecto**), la ejecución comercial conserva la ruta anterior `t.execute()` sin conexión ni lectura de tablas QA.

## Evidencias efectuadas directamente en Fedora
Script `qa/sandbox/run_real_ticket_status_isolated.sh` recompila clases reales desde el worktree contra CORE1.3 sellado y levanta MariaDB Podman --network none, cargador de esquema original; bajo `KYLGIS_QA_LEDGER_OPTIN=1` inserta exclusivamente DDL QA en `pos_qa_status` y enciende el flag JVM **solo en RealTicketStatusSaveProbe**.
MariaDB 10.11 / 11.8 / 12.3.3: cada una `PASS DURABLE_OPERATION_UUID_SET`, `PASS LEDGER_DB_CONFIRMED_AND_VERSION_2`, `PASS LEDGER_EXACTLY_ONE_OPERATION`, `REAL_SAVE_TICKET_STATUS_QA_PASS`, `REAL_VOUCHER_RACE_QA_PASS`, `REAL_SAVE_TICKET_ACK_LOSS_QA_PASS`, `QA_STATUS_CONTAINER_CLEANED`. Proceso conjunto exit0, `CORE_LEDGER_OPTIN_MATRIX_PASS`.
El resto de pruebas de carrera voucher y ACK se ejecutan con flag apagado como regresión para no atribuirles cobertura del ledger.
La revisión de seguridad posterior cambió error post-commit del ledger a TransactionOutcomeUnknownException; requiere repetir regresión JDK antes de cualquier integración adicional.

## Bloqueos explícitos
NO PRODUCTION: solo catálogo pos_qa_status, schema QA, feature flag apagado por defecto. Esto no resuelve proceso POS reiniciado, dos JVM reales, pagos emitidos antes de guardar, proveedor financiero con APPROVED incierto, ledger de banco separado, ni concilia voucher pago de la ruta heredada. No se ha creado migración del POS ni se modifican main, .17/.18, folios, gateway, roles o configuración de producción.
Para habilitar general hay que mover el momento PREPARED por delante de toda autorización externa, manejar atomicidad/lease y fuente bancaria confiable, script de migración reversible, pruebas de reinicio/GUI, asegurar persistencia de opUUID en recuperación de tickets y que todas las cajas rechacen operaciones inciertas.
