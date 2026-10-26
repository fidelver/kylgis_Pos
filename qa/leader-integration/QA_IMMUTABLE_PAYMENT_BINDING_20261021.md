# SAT Fase 2 — payload financiero inmutable y UUID de componente
Base `375505c1c48568859eb36f18b6329b76fc76dae4`, rama `collab/leader-sat-payment-binding-20261021`. Hereda conservación de pagos/vouchers, guardias de recuperación UI/backend y corrección de copia de efectivo.

## Pendientes de código resueltos en el candidato QA
1. SaleOperationPayloadQA genera SHA256 determinístico versionado de recibo/caja/cliente/tipo, moneda MXN, productos/cantidades/precios/impuesto, componentes de pago y vouchers con sus importes. Excluye folio y fecha que cambian al guardar, y referencias que llegan después del AUTH. No almacena PAN ni pistas.
2. Operación se reserva con hash **antes de AUTH** por el coordinador sintético. prepareAndClaim verifica hash, identidad completa, revisión y estado antes de CAS. Un mismo UUID no autoriza un importe/ticket modificado.
3. Cada intención externa tiene component_index único por operación. Antes de guardar se comprueba estado APPROVED/RECONCILED, moneda MXN, importe exacto, índice y referencia no vacía, y que apunta a un tender ccard/magcard. No puede faltar la intención de una tarjeta.
4. payments.ID real usa payment_uuid previamente reservado para el componente bancario; el efectivo usa UUID determinístico derivado de operación+índice. Prueba JDBC de JOIN demuestra correlación durable. Con flag desactivado se mantiene UUID legacy.
5. Triggers **sólo qa_*** impiden cambiar hash/UUID/recibo/caja, importe/moneda/índice/proveedor de componente o sustituir referencia ya asignada. Se impide borrar intenciones y evadir la barrera de idempotencia. No cambia payments.TRANSID ni su compatibilidad histórica.
6. Voucher como descuento sigue probado; el tender legacy llamado voucher se rechaza antes de guardar bajo opt-in QA. No se convierte implícitamente de medio de pago a descuento ni se decide su tratamiento fiscal.

## Pruebas reales ejecutadas
Matriz final sobre MariaDB **10.11, 11.8 y 12.3.3**, RC0 y limpieza completa:
- PAYMENT_CONTEXT_SERIALIZATION_PASS, 28 comprobaciones/version.
- EXITED_FIRST_JVM_REAL_SAVE_RECOVERY_PASS.
- TWO_INDEPENDENT_CASHBOX_JVMS_REAL_SAVE_PASS.
- INDEPENDENT_BANK_REAL_POS_FLOW_PASS, **106 comprobaciones/version**.
- Cambiar importe con la misma identidad bloquea saveTicket; nueve intentos de alterar/borrar bindings o retroceder APPROVED a UNKNOWN en los escenarios APPROVED y LOST_ACK son rechazados SQLSTATE45000.
- En APPROVED, LOST_ACK y carrera entre JVM, JOIN de payments con qa_external_payment_intents acredita que el ID reservado antes de autorizar es el ID guardado después del COMMIT real. Un recibo/canje, dos tenders, suma exacta, ningún segundo cargo.
- APPROVED/DECLINED/UNKNOWN, timeout después de autorizar, conciliación, reinicio bancario, caída de ACK POS, dos procesos y regresiones anteriores continúan aprobando.

Comando por versión:
`KYLGIS_QA_BANK_OPTIN=1 KYLGIS_QA_LEDGER_OPTIN=1 KYLGIS_QA_LEDGER_ACK_OPTIN=1 KYLGIS_QA_MARIADB_VERSION=<versión> bash qa/sandbox/run_real_ticket_status_isolated.sh`.

Logs completos originales `/tmp/kylgis-sat-binding-certified-<versión>-20261021.log`, extractos versionados con este informe. Un intento previo falló correctamente en el nuevo assert del ID cuando la edición con CRLF no se había aplicado; no se contó como PASS y se corrigió con reemplazo exacto mediante Utils.

## Límites para integración
- Código y DDL están deliberadamente limitados a pos_qa_status y flag apagado. No existe migración aplicada a POS real.
- Campos de binding son nullable para representar intenciones no vinculadas del prototipo; saveTicket real **rechaza reservas sin hash/índice**. No autocompletar bindings de una operación antigua cuyo cargo sea incierto.
- SQL APPROVED no autentica un banco. Falta conectar el coordinador al diálogo de pago real y al proveedor autorizado; este ensayo utiliza servicio de banco sintético privado.
- El hash vincula el payload financiero acordado, no reemplaza validación comercial/fiscal, tipos de cambio, descuentos calculados por UI ni permisos del operador. Moneda distinta de MXN y reembolsos no se habilitan aquí.
- Persistencia demostrada frente a reinicio de procesos y ACK perdido; no es una prueba de pérdida de alimentación ni del almacenamiento productivo.
- El dueño del esquema y credenciales debe aprobar fingerprint, migración reversible y mínimos privilegios antes de uso operativo. No modificar .17/.18, main, gateway ni SAT/CFDI/PAC.

Dictamen: correcciones de binding/identidad resueltas y reproducibles **en QA**; autorización y adaptación comerciales siguen separadas. No desbloquear ni reembolsar automáticamente operaciones ambiguas.
