# QA durable ledger — matriz y diagnóstico resuelto (2026-10-21)

Rama `collab/leader-core-sat-integracion-20261021`; artefactos **SOLO QA**: `qa_durable_payment_ledger_candidate.sql` y `run_durable_ledger_constraints_isolated.sh`. No migración ni transacción real POS.

## Matriz final repetida directamente
Fedora/Podman rootless `--network none --read-only`, tmpfs y cleanup. Script `bash -n` validado; corrida con versiones 10.11, 11.8 y 12.3.3: **6/6 verificaciones por versión, 18/18 PASS, proceso total RC0, `THREE_VERSION_PASS`, `QA_CLEANED` para cada contenedor**. Rechazados receipt duplicado, estados inválidos de operación/pago, referencia bancaria duplicada y FK inválida; probado estado `COMMIT_UNKNOWN` accesible.

## Incidente y corrección
En corridas iniciales 12.3.3, `ERROR 2002 HY000` socket perdido durante el bootstrap. `podman inspect` confirmó contenedor **running**, exit=0, OOM=false; el runner comprobaba `SELECT 1` antes de que la imagen terminara el bootstrap del servidor MariaDB temporal, el cual se reinicia. No correspondía a una caída comprobada por CHECK. Se corrigió el preflight: esperar por marcador de log `MariaDB init process done. Ready for start up.` y, **después**, `SELECT 1` a MariaDB definitivo. Repetición integral tres versiones exit0. No skips en el script final.

## Herramientas y build
KylGis Recursos job 7ac7327fca37a2af3b536e4c sigue en `uncertain / UnitMissing` tras reconcile/cancel; ps y systemctl no reportaron proceso/unidad; **no se alteraron registros internos**. Build directo con `fam` sobre target preexistente falló al sobrescribir recurso propiedad server-conf; no cambiar chmod/chown. Copia temporal rsync sin .git/target, Maven Temurin21 offline `mvn -o -q test`: `BUILD_RC=0`, temp eliminado; Surefire no descubrió tests.

## NO-GO
El esquema es candidato de diseño, aún NO integrado a `DataLogicSales.saveTicket()`, banco externo ni UI. El filtro UNIQUE provider/reference no sustituye idempotencia de cobro en proveedor; no alcanza recuperación posreinicio ni dos cajas. Quedan por implementar persistencia de intent antes de cobro, transición CAS, reconciliación DB+proveedor, QA de simultaneidad y UI, y migración sobre esquema vigente **previa autorización**. Prohibidos main, despliegue, bases .17/.18, folios, gateway y SAT/CFDI/PAC hasta gates completos.
