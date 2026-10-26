# QA P0 — ledger durable candidato, NO integrado (2026-10-21)

## Problema
Las pruebas de CORE real demostraron COMMIT confirmado por MariaDB pero ACK TCP perdido, por lo que BasicException/TransactionOutcomeUnknownException NO equivale a rollback. TicketInfo contiene bandera local parcial, no ledger durable ni idempotencia multicaJa/retorno del banco.

## Implementación añadida bajo QA exclusivamente
`qa/sandbox/qa_durable_payment_ledger_candidate.sql`: tablas QA separadas `qa_sale_operation_intents` (PK operation_uuid, UNIQUE receipt_id, terminal_id, state) y `qa_external_payment_intents` (PK payment_uuid, FK operation_uuid, UNIQUE provider+provider_reference nullable, currency/amount/state), InnoDB y CHECK. Evita imponer UNIQUE a `payments.TRANSID` del POS legacy. Sin DDL comercial ni cambios DataLogicSales para esto.

## Evidencia ejecutada
- Fedora DC: Maven Java21 `mvn -o -q test` en copia aislada de fuentes con rsync, sin .git, target ni logs, bajo `/tmp/kylgis-core-build-isolated.*`; **BUILD_RC:0**, salida temporal borrada, fuentes originales intactas. Intento anterior como fam en target existente falló por permisos de archivos server-conf: eso NO es error de código ni razón para chmod/chown.
- Fedora DC: contenedor Podman nuevo `kylgis-ledger-qa-leader-20261021`, MariaDB10.11 --network none, --read-only, DB tmpfs; CREATE DATABASE ledger_qa e importación de SQL QA RC0; SHOW CREATE TABLE verificó FK, UNIQUE y checks; `QA_DDL_PASS`, trap detiene contenedor. Ninguna DB compartida utilizada.
- KylGis Recursos job 7ac7327fca37a2af3b536e4c permanece `uncertain` con `UnitMissing`, incluso tras reconcile/cancel; no se declaró PASS falso ni se borró registro. Incidente para líder Utils.

## Gates antes de integrar a código comercial
1. Ensayar colisiones concurrentes provider_reference y receipt_id, no duplicados, y fiscal.sat.enabled=false.
2. Diseñar contratos transicionales con CAS/version y un estado que no autorice auto retry ni cobro duplicado después de COMMIT_UNKNOWN.
3. Registrar intent idempotente ANTES de proveedor/COMMIT en almacenamiento autorizado, comprobar recuperación tras reinicio y otra caja, fallos previos y posteriores al COMMIT y resultados de proveedor APPROVED/UNKNOWN.
4. Fingerprint del esquema versionado y migración compatible, rollback con índice de datos históricos; sin tocar .17/.18.
5. Cerrar QA GUI Swing y banco independiente antes de merge main.

Este es un primer artefacto de integración P0 validado como DDL sintético, NO GO para deploy/merge/uso real.
