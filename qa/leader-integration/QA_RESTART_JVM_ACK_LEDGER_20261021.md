# CORE/SAT P0 — segundo JVM posterior a COMMIT con ACK perdido (2026-10-21)

## Cambio probado
Se incorporó `qa/manual/RestartedSaleLedgerProbe.java`, que se ejecuta como **proceso Java independiente** desde `RealSaveTicketAckLossProbe` luego de que el servidor MariaDB confirma COMMIT pero el proxy elimina su ACK. No altera `DataLogicSales` ni producción.

El proceso hijo abre su propia conexión al catálogo sintético `pos_qa_status`, valida UUID de operación y estado `COMMIT_UNKNOWN:2`, recibo y pago persistidos y que un CAS nuevo `PREPARED -> IN_FLIGHT` afecta cero filas. Se registra `RESTART_SEPARATE_JVM_FAIL_CLOSED_PASS`.

## Incidencia solucionada
Primer intento en MariaDB10.11: `posix_spawn failed: Permission denied` al crear el proceso hijo con JVM Temurin21 montada read-only en contenedor Podman; QA salió RC1, contenedor limpiado. En el runner sintético, no en el POS, se estableció `-Djdk.lang.Process.launchMechanism=FORK` para lanzar la JVM hija; repetición exit0. La configuración no se aplica al POS productivo.

## Matriz repetida directamente
`KYLGIS_QA_LEDGER_OPTIN=1 KYLGIS_QA_LEDGER_ACK_OPTIN=1 KYLGIS_QA_MARIADB_VERSION=<10.11|11.8|12.3.3> bash qa/sandbox/run_real_ticket_status_isolated.sh`

Cada versión: `RESTART_SEPARATE_JVM_FAIL_CLOSED_PASS`, `ACK_RESTART_SEPARATE_JVM_PASS`, `REAL_SAVE_TICKET_ACK_LOSS_QA_PASS`, `QA_STATUS_CONTAINER_CLEANED`, RC0. Lote 11.8+12.3.3 `RESTART_JVM_ALL_VERSIONS_PASS` RC0. Los tres escenarios también conservan suite de estado ticket y carrera voucher existente.

## Alcance exacto y bloqueos
Este ensayo representa un **nuevo proceso JVM de consulta/recuperación**, no reinicia toda la aplicación Swing POS ni servidor BD. No comprueba pago bancario previo, banco mock independiente, lectura de tickets suspendidos, múltiples recibos con nueva identidad, ni reconstrucción completa de pagos/vouchers. No se conectó PAC/CFDI. No promover a main ni usar en .17/.18 hasta pasar gates reales.

Se mantiene feature flag apagado y catálogo sintético obligatorio, sin despliegue ni modificaciones productivas.
