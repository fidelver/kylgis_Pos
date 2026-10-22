# CORE/SAT — recuperación real entre procesos y banco independiente
Fecha local: 2026-10-21. Rama `collab/leader-core-sat-integracion-20261021`. Base limpia comprobada `22fbfc1da5b5d1e25ea0567a65152f3d69072a7a`.

## Resultado ejecutado
Se amplió el laboratorio existente, conservando backend MariaDB durante todos los procesos. Compilación Java21 de clases reales del worktree y CORE1.3 sellado. Datos exclusivamente sintéticos en `pos_qa_status`, Podman rootless `--network none`, sin puertos publicados.

| MariaDB | JVM original termina antes de recuperación | Dos JVM, saveTicket real | Banco independiente y POS real | Limpieza |
|---|---|---|---|---|
| 10.11 | PASS | PASS | PASS, contador 83 | RC0, contenedor eliminado |
| 11.8 | PASS | PASS | PASS, contador 83 | RC0, contenedor eliminado |
| 12.3.3 | PASS | PASS | PASS, contador 83 | RC0, contenedor eliminado |

La pasada final conjunta terminó con `FINAL_THREE_MARIADB_MATRIX_PASS`, RC0. Por versión hay 160 líneas PASS (incluyen aserciones repetidas por escenarios; no representan 160 casos únicos). El contador del banco de 83 excluye una comprobación final de cierre del proceso.

### Recuperación después de salida efectiva
`RealSaveTicketAckLossProbe` transmite COMMIT real y descarta el OK recibido. Guarda TicketInfo mediante su Externalizable actual y termina. El shell espera el fin de `podman exec` antes de lanzar `ExitedProcessSaleRecoveryProbe`; no existe proceso padre vendedor vivo en esa recuperación. El segundo proceso recupera identidad/recibo desde disco, consulta ledger y llama el método real `DataLogicSales.saveTicket()`: bloqueado por ledger, no por reutilización de objetos. Permanecen exactamente un receipt, ticket, línea, pago, redemption y voucher consumido; estado COMMIT_UNKNOWN, revisión 2.

### Dos cajas independientes
`IndependentCashboxesProbe` crea dos JVM con PID diferentes, cada una deserializa la misma identidad. Barrera de archivos obliga a estar ambas listas antes de ejecutar saveTicket. Exactamente una confirma y la otra es rechazada antes de guardar. Ledger DB_CONFIRMED:2, una operación por recibo, un pago/canje y voucher D. La reconstrucción de pagos/vouchers en este ensayo es explícita, exclusivamente QA; no valida UI.

### Banco independiente
`SyntheticBankService` es otro proceso Java con socket de loopback privado, token efímero de fixture y registro fsync por UUID. El protocolo es deliberadamente sintético; no es una pasarela comercial ni certificación de autenticación/TLS bancario. El servicio persiste antes de responder y conserva sus registros tras ser terminado/reiniciado.

`IndependentBankFlowProbe` confirma operación y componente de pago en conexiones JDBC independientes antes de cualquier AUTH. CAS PREPARED→UNKNOWN ocurre antes de enviar el cargo. Otra JVM no puede enviar AUTH para el mismo componente. Se prueban:
- APPROVED, DECLINED, UNKNOWN; ni rechazo ni UNKNOWN permiten guardar una venta.
- Timeout real de lectura después de autorización durable: UNKNOWN persiste, segundo AUTH bloqueado, consulta explícita al banco recupera APPROVED.
- Misma clave/payload devuelve el mismo cargo; distinto importe con la misma clave devuelve CONFLICT.
- Token incorrecto no permite consultar; identidad/recibo o terminal discrepantes no reclaman operación.
- Caída del banco observable mientras MariaDB POS sigue disponible; no redispatch, no refund ni reset de folio. Reinicio conserva autorización.
- Dos cajas Java con barrera propia: una sola gana CAS y un único cargo.
- APPROVED más efectivo (6+4) con voucher de descuento (20): se usa saveTicket real y se pierde también su ACK COMMIT; un receipt/ticket/canje, dos componentes de pago con suma exacta 10, reintento bloqueado.

### Cambio del ledger QA
`SaleOperationLedgerQA.prepareAndClaim` admite una reserva PREPARED previamente confirmada solo tras verificar operation_uuid, receipt_id, terminal_id, estado y revisión. Duplicado 1062 por sí solo no autoriza la venta. La transición CAS conserva un único ganador. Componentes externos distintos de APPROVED/RECONCILED impiden guardar. Feature flag apagado por defecto, guardia de catálogo sintético intacta. No es un adaptador de producción ni autentica claims externos aportados por un cajero.

## Reproducir
Desde el worktree:
```bash
for v in 10.11 11.8 12.3.3; do
  KYLGIS_QA_BANK_OPTIN=1 KYLGIS_QA_LEDGER_OPTIN=1 \
  KYLGIS_QA_LEDGER_ACK_OPTIN=1 KYLGIS_QA_MARIADB_VERSION="$v" \
  bash qa/sandbox/run_real_ticket_status_isolated.sh
done
```
Dependencias y SHA CORE están guardados en el runner. `bash -n` aprobado. Maven offline Java21 `mvn -o -q test` sobre snapshot temporal sin .git/target: RC0, snapshot eliminado. No se equipara ese RC con tests Surefire ni certificación de UI. El job privado `7ac7327fca37a2af3b536e4c` sigue uncertain/UnitMissing; no fue borrado ni manipulado.

## Evidencia preservada
Extractos completos de marcadores de la pasada final en `qa/leader-integration/evidence/qa-bank-processes-<version>-20261021.txt`. Se conserva el orden real, quitando SQL/ruido de aplicación.

SHA256 de stdout completo original:
- 10.11: `71c9bdf82745d11bc1e6c919ebe52d8e6c1bc772409073988ac6d49ac2fe0ac7`.
- 11.8: `9857bbb1e551f628201b5c57f6b3c4013dac6e617e9a2f1d775ce4e1bbf44c4b`.
- 12.3.3: `95810fdc06fbd3bb76edd067cda23ceb89e60f8a6ba178a91a3f8a2a7cf53616`.

## Fuentes comprobadas
| Archivo | SHA256 |
|---|---|
| `src/main/java/com/mx/kylgis/pos/forms/SaleOperationLedgerQA.java` | `123910503a8c92b933a40ecbfc01c2a0f0653e83f3de8f21cf1a7e3c201bc6e9` |
| `qa/manual/RealSaveTicketAckLossProbe.java` | `8258c2d983b2ca2387ec614360e79757ae968385430d57edd530f65f73d671f2` |
| `qa/manual/ExitedProcessSaleRecoveryProbe.java` | `b8cbf4a938750c2696fe32884d9d766b2307201b3f43e2d9a98ada2f4828cfd9` |
| `qa/manual/IndependentCashboxesProbe.java` | `38b453e55fcd4faf9f02b5373d02b6b55b0561caea2892de9547ef03c712c814` |
| `qa/manual/SyntheticBankService.java` | `5a0133f2e8e5b633f4fbaf230fded67af6c8e1ce06344083c0835c58a790367e` |
| `qa/manual/IndependentBankFlowProbe.java` | `97c47cdc1b2be5617a4cb04ec4b8cd146dab85ab6756a743eb1b8090d9a9a543` |
| `qa/sandbox/run_real_ticket_status_isolated.sh` | `a4582a5a0b3f2453fdc56c391a33b6e08ad099dd16c2835fe1ea2aacc1b41ec4` |

## Incidencias de ejecución y corrección
Una pasada preliminar 11.8 se invalidó por editar el runner mientras el shell lo leía: EOF de comillas; contenedor eliminado. Se dejó de editar el código participante durante ejecuciones y la matriz final completa aprobó. Otra envoltura de shell terminó RC127 por `rg` ausente después de que 11.8 aprobara; se usó grep disponible. Ninguno de esos códigos fallidos se contó como PASS de lote. Git desde Fedora no resuelve el archivo .git del worktree que referencia el montaje CT; se mantiene Git por KylGis Recursos.

## Condiciones que siguen bloqueando integración principal
1. No existe esquema/migración comercial autorizado ni fingerprint del clon vigente de producción; las tablas qa_* no se instalan automáticamente.
2. Falta UI Swing completa (suspensión, restauración, bloqueo de botones y reinicio de aplicación); la prueba de proceso cerró JVM de QA, no un POS Swing completo.
3. Externalizable mantiene la omisión histórica de pagos/vouchers. Identidad y barrera durable se conservaron, pero esto no certifica reconstrucción íntegra de una venta suspendida. `copyTicket()` crea otro receipt_id conservando properties: el ledger rechaza ese conflicto, no lo convierte en una nueva autorización.
4. No se certificó voucher legacy como medio de pago. La ruta actual JPaymentVoucher entrega VoucherDiscountInfo y esta batería prueba descuento. No reintroducir automáticamente el significado financiero legacy sin decidirlo y probarlo.
5. Adaptador bancario real, claves previas al diálogo de pago, firma/fuente confiable, moneda/importes inmutables por componente, correlación con payments legacy y conciliación autorizada siguen pendientes. El coordinador sintético no se conecta a la UI ni habilita cargos reales.
6. RIF/TLS final: ver informe separado; diez casos requieren servicio/release/identidad privados del propietario Utils.
7. Kitchen Screen no se volvió a certificar gráficamente en esta sesión.

Dictamen: **GO revisión y reproducción de candidatos QA; NO-GO main, producción y certificación integral de las tres líneas.** Nada sobre .17/.18, gateway, bancos reales, SAT/CFDI/PAC ni procesos POS productivos.
