# P0 — Contrato de enlace ledger a saveTicket (2026-10-21)

## Invariante principal
El documento DDL `qa_durable_payment_ledger_candidate.sql` está probado solamente en DB sintética. `DataLogicSales.saveTicket()` abre la transacción con `Transaction.execute()` y realiza COMMIT después de `transact()`. **No basta insertar PREPARED dentro de esa misma transacción**, porque su pérdida de conexión o rollback antes del commit dejaría sin identidad durable observable por otras cajas. La intención debe confirmarse en transacción separada ANTES de cualquier despacho bancario y ANTES del inicio de `saveTicket()`. El recibo `ticket.getId()` y el UUID de operación deben permanecer estables en copia, cierre, suspensión y reinicio.

## Secuencia obligatoria fail-closed
1. Caja adquiere la identidad `operation_uuid` (UUID asignado una sola vez por operación), `receipt_id` y `terminal_id`. Persistir `PREPARED` con UNIQUE(receipt_id) y CAS revision=0, usando conexión independiente y COMMIT confirmado. Si el ACK de ese COMMIT no llega, buscar por operation_uuid/receipt_id en conexión distinta; no iniciar cobro ni venta hasta reconciliar.
2. Registrar cada componente de pago con `payment_uuid` durable y referencia del proveedor. No deducir identidad a partir de `payments.TRANSID` legacy; múltiples tenders pueden compartirlo. Ningún AUTH/APPROVED externo puede crearse sin intención durable confirmada.
3. En caso de ACK perdido del proveedor: detener reintentos, consultar estado autenticado del proveedor mediante idempotency key exacta y conservar UNKNOWN mientras no haya prueba suficiente.
4. Antes de `saveTicket` real, adquirir reclamación exclusiva de operación a nivel BD con CAS. La misma operación jamás puede ser ejecutada simultáneamente por otra caja, aunque esta comparta folio de usuario. La sesión de venta usa receipt_id/op_uuid ya reservado.
5. COMMIT del POS confirmado: anotar DB_CONFIRMED con prueba de lectura de receipt/ticket/payments/redemptions en conexión NUEVA. Ante `TransactionOutcomeUnknownException`, **NO** llamar saveTicket de nuevo, rollback, refund, liberar operación, ni reiniciar folio. Dejar COMMIT_UNKNOWN durable solo después de comprobar/conciliar el ledger, que puede estar ya DB_CONFIRMED.
6. Conciliación no equivale a afirmación del invocador: lecturas autenticadas de POS y banco por backend privado; verificar cantidad, moneda, componentes, voucher e identidad del recibo. Si cualquier evidencia difiere, estado BANK_RECONCILIATION inmovilizado y revisión humana.
7. UI consulta estado a cada reanudación y nueva caja. Se deshabilitan botones Cobrar/Finalizar/Volver a cobrar mientras estado no conciliado. Restore Externalizable debe conservar pagos/vouchers o bloquear su reconstrucción incompleta.

## Gates necesarios ANTES de incluir este DDL en POS instalado
- Migración reversible sobre schema versionado, fingerprint de tablas/indexes y filas heredadas en export **autorizado**. Nada sobre .17/.18.
- Dos cajas reales, banco en contenedor distinto, reinicio de proceso/GUI, ACK perdido antes y después del COMMIT, voucher descuento vs pago voucher y pagos mixtos.
- Rechazar evidence JSON, nombres de archivo o claims APPROVED aportados por la caja. Fuente backend confiable.
- La matriz 39/39 QA sintética comprueba estado y serialización de SQL, **no** el workflow antes descrito. GATE NO-GO main/producción.
