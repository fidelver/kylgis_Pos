# Prototipo Java JDBC de intención comercial durable — 2026-10-21

## Alcance y resultado
Proyecto worktree `leader-core-sat-integracion-20261021`, base HEAD `0e32079fd3fd0c4ad7c116728b1ce4903b46606b`. Nuevos `qa/manual/IsolatedSaleOperationLedgerProbe.java` y ampliación de `qa/sandbox/run_durable_ledger_constraints_isolated.sh`. Se trata de **prototipo QA-only**, no modificaciones a `DataLogicSales.saveTicket`, producción, main, DB .17/.18, ni acceso bancario.

El probe usa JDBC de MariaDB en `isolated_ledger_qa` alojada en Podman `--network none` con tmpfs y servidor definitivo verificado. Ejecuta con tres conexiones separadas secuenciales para demostrar PREPARED persistente tras cierre de conexión, reivindicación exclusiva IN_FLIGHT, rechazo de otra caja, COMMIT_UNKNOWN durable y bloqueo de nuevo claim. Asimismo mantiene checks SQL de unicidad, estados, CAS y concurrencia multiproceso.

## Ejecución real
Fedora DC, KylGis Recursos para edición: `bash -n` y ejecución en MariaDB **10.11, 11.8, 12.3.3**. Cada una imprimió `ISOLATED_LEDGER_JDBC_PROBE_PASS`, `QA_LEDGER_CONSTRAINTS_PASS ... checks=23` y `QA_CLEANED`; el lote para 11.8+12.3.3 salió con `LEDGER_JDBC_ALL_VERSIONS_PASS` **RC0**. Por versión 15 checks de SQL + 8 verificaciones JDBC; **69/69** totales. Contenedores efímeros detenidos.

## Distinciones obligatorias
- Tres conexiones JDBC **en un proceso Java**, no reinicio del JVM de POS, de MariaDB ni servicio bancario. Los tests SQL anteriores sí usan dos procesos `mariadb` y CAS, no 2 JVM de caja reales.
- La identidad de operación todavía NO se crea en UI antes de cobro ni se inyecta a saveTicket; no está resuelto el ACK perdido de preparación, la asignación de operación durable en nueva caja ni la conciliación bancaria autenticada.
- El código del probe usa credencial root vacía **exclusivamente dentro del contenedor aislado**, nunca producción o red exterior.
- Por seguridad no se ha insertado una llamada de persistencia que podría romper ventas reales sin schema instalado. NO-GO merge/deploy.

## Siguientes gates
Implementar servicio privado opt-in que posea la conexión independiente y compruebe schema y backend; operación estable antes de cobrar; persistencia tras reboot/restart verdadero; UI bloqueada en todas cajas; banco independiente con recibos autenticados; migración versionada reversibles y QA real sobre guardia de pago; RIF/TLS faltan diez R.
