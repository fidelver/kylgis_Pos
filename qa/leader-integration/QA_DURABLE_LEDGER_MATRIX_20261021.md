# QA durable ledger — matriz y diagnóstico resuelto (2026-10-21)

Rama `collab/leader-core-sat-integracion-20261021`; artefactos **SOLO QA**: `qa_durable_payment_ledger_candidate.sql` y `run_durable_ledger_constraints_isolated.sh`. No migración ni transacción real POS.

## Matriz final repetida directamente
Fedora/Podman rootless `--network none --read-only`, tmpfs y cleanup. Script `bash -n` validado; corrida con versiones 10.11, 11.8 y 12.3.3: **15/15 verificaciones por versión, 45/45 PASS, proceso total RC0, `THREE_VERSION_PASS`, `QA_CLEANED` para cada contenedor**. Rechazados receipt duplicado, estados inválidos de operación/pago, referencia bancaria duplicada y FK inválida; probado estado `COMMIT_UNKNOWN` accesible y revisión monotónica mediante compare-and-swap: el primer UPDATE PREPARED/revision=0 afecta una fila y el segundo afecta cero.

## Estado CAS probado
Se añadió columna `revision BIGINT UNSIGNED` al ledger de QA y la batería verifica transición condicional PREPARED -> COMMIT_UNKNOWN sin doble ganador. Se probó además con dos procesos clientes MariaDB independientes lanzados en paralelo sobre una operación `RACE-R2`, con resultados `ROW_COUNT` ordenados `0,1` y revisión persistida `COMMIT_UNKNOWN:1`, en cada versión (PASS_CAS_TWO_PROCESSES_ONE_WIN). Es control local SQL, no autenticación del proveedor ni ledger de POS instalado.

## Incidente y corrección
En corridas iniciales 12.3.3, `ERROR 2002 HY000` socket perdido durante el bootstrap. `podman inspect` confirmó contenedor **running**, exit=0, OOM=false; el runner comprobaba `SELECT 1` antes de que la imagen terminara el bootstrap del servidor MariaDB temporal, el cual se reinicia. No correspondía a una caída comprobada por CHECK. Se corrigió el preflight: esperar por marcador de log `MariaDB init process done. Ready for start up.` y, **después**, `SELECT 1` a MariaDB definitivo. Repetición integral tres versiones exit0. No skips en el script final.

## Herramientas y build
KylGis Recursos job 7ac7327fca37a2af3b536e4c sigue en `uncertain / UnitMissing` tras reconcile/cancel; ps y systemctl no reportaron proceso/unidad; **no se alteraron registros internos**. Build directo con `fam` sobre target preexistente falló al sobrescribir recurso propiedad server-conf; no cambiar chmod/chown. Copia temporal rsync sin .git/target, Maven Temurin21 offline `mvn -o -q test`: `BUILD_RC=0`, temp eliminado; Surefire no descubrió tests.

## NO-GO
El esquema es candidato de diseño, aún NO integrado a `DataLogicSales.saveTicket()`, banco externo ni UI. El filtro UNIQUE provider/reference no sustituye idempotencia de cobro en proveedor; no alcanza recuperación posreinicio ni dos cajas. Quedan por implementar persistencia de intent antes de cobro, transición CAS, reconciliación DB+proveedor, QA de simultaneidad y UI, y migración sobre esquema vigente **previa autorización**. Prohibidos main, despliegue, bases .17/.18, folios, gateway y SAT/CFDI/PAC hasta gates completos.
## Extensión ejecutada: recuperación entre procesos
Se agregó validación en `run_durable_ledger_constraints_isolated.sh`: un proceso cliente distinto consulta COMMIT_UNKNOWN tras finalizar los clientes previos, el UPDATE de reintento PREPARED/revision=0 queda en ROW_COUNT=0, y la conciliación condicional a DB_CONFIRMED sólo gana una vez. No se afirma reinicio del motor MariaDB ni del POS, ni autenticación de tercero. Ejecutado directamente mediante DC con MariaDB 10.11,11.8,12.3.3: 13/13 por versión (39/39), RC0, `DURABLE_RECOVERY_QA_PASS` y `QA_CLEANED` en las tres.
## Puerta PRECOMMIT IN_FLIGHT
Extensión real por líder: estado IN_FLIGHT incorporado exclusivamente al DDL QA. Dos procesos MariaDB competidores sobre receipt CLAIM-R3 ejecutaron un CAS PREPARED/revision0 -> IN_FLIGHT/revision1; resultados exactos [0,1] en las tres versiones. Un intento posterior devuelve ROW_COUNT=0. Matriz ejecutada 2026-10-21: 15/15 por versión (45/45), `LEADER_PRECOMMIT_CLAIM_MATRIX_PASS`, RC0, QA_CLEANED para 10.11,11.8,12.3.3. No se conectó este DDL ni identidad al código `saveTicket` real, no se certifica dispatch bancario.
