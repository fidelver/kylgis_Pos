# RIF/TLS — revalidación líder 2026-10-21

Runner `ff139986f475acb390635b7fbb985b0eb6e1f687d10c541bdd594e6f0cf39178`, orquestador `598aba6862483cc8b5dd7c93b54b5dabe0648f5bab4a2d30d202caddc041b29b`: hashes actuales comprobados por KylGis Recursos, sin edición de estos archivos.

## Ejecución propia en esta sesión
- `python_unittest_security(test_recipe_bundle_private_runner_candidate.py)`: RC0, 30/30 PASS; snapshot `d118df25fa1320197fc00d4e48cf1df15f4312a1d382acf3ea8a0033c3b8c785`.
- `python_unittest_security(test_recipe_bundle_private_orchestrator_staging_candidate.py)`: RC0, 27/27 PASS; snapshot `541bb9a1ba0b737061c017104228d0d63697235f33e084e163a7291b10fc4ece`.
- `PYTHONDONTWRITEBYTECODE=1 python3 -B COORDINACION/qa/test_runner_orquestador_R01_R30_STAGE_598aba68_20261021.py -q`: RC0, 36 registrados, 26 PASS y 10 SKIP.
- `PYTHONDONTWRITEBYTECODE=1 bash /home/fam/00-ChatGPT/seguridad_server/qa_recipe_bundle_mariadb_rotation_candidate.sh`: RC0, `ISOLATED_RIF_TLS_ROTATION_QA_PASS`, MariaDB sintética --network none, cliente 12.3.3. Se verificaron CA/SAN, rechazo sin TLS, pin SHA256 en sesión SQL real, pin incorrecto, recuentos y captura, truncamiento, MVCC con escritor concurrente, rotación del certificado entre fases y rechazo del certificado realmente presentado y vencido. Se generaron exclusivamente certificados/usuarios de fixture dentro del sandbox; no credenciales operativas. No es el servicio final.

## Bloqueo concreto de certificación final
La lectura actual de `kylgis_recipe_bundle_private_runner_candidate.py` confirma:
- capabilities devuelve `release_id=PENDING_LEADER`, `execution_enabled=false`, `receipt_authenticity=NOT_CERTIFIED`.
- `run_live()` lanza `NO_GO: private runner/CA/key/credentials not approved`.
- dispatch únicamente permite capacidades.

Por ello permanecen **sin certificar contra servicio final** R05, R06, R09, R10, R11, R19, R23, R24, R26 y R30. Los ensayos aislados de TLS/rotación/MVCC aportan evidencia de componentes, pero no cambian esos SKIP a PASS. Su cierre requiere el release y perímetro privados del propietario Utils, con fuente de resultados, cliente/CA/pin, cierre de transacción y almacén de evidencias/TTL propios del servicio.

No se habilitó run_live, G3, gateway ni acceso .17/.18. Ningún cambio de configuración/clave/servicio real. El informe del sandbox TLS original se conserva fuera de fuentes en `/tmp/kylgis-leader-rif-tls-rotation-20261021.log`; las conclusiones reproducibles quedan aquí versionadas.
