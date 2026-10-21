# QA durable ledger constraints — verificación líder 2026-10-21

Rama de trabajo collab/leader-core-sat-integracion-20261021. Artefactos QA únicos: `qa_durable_payment_ledger_candidate.sql` y `run_durable_ledger_constraints_isolated.sh`. No SQL de ventas ni migración productiva.

## Resultados reales
- MariaDB10.11: PASS duplicado receipt, estado inválido, referencia bancaria repetida, FK de operación inexistente, estado pago inválido, marca COMMIT_UNKNOWN; `QA_LEDGER_CONSTRAINTS_PASS`, `QA_CLEANED`, exit0 en ejecución combinada anterior.
- MariaDB11.8: mismos PASS y cleanup.
- MariaDB12.3.3: DDL se aplicó correctamente; tras algunos rechazos esperados, dejó de aceptar conexiones por socket local con `ERROR 2002 HY000`; proceso QA terminó exit1 y limpieza. Segunda corrida con 1GiB y 512MiB tmpfs reprodujo desconexión. Una corrida diagnóstica omitió transitoriamente los CHECK negativos y pasó duplicado/fk, pero igualmente perdió conexión en SELECT de estado; **no es un PASS ni permite concluir que el CHECK provoca el fallo**. El script publicado mantiene TODOS los checks, sin skips ocultos. Logs de contenedor visibles solo de arranque, causa de terminación no determinada: investigar memoria/restart/volúmenes/versión sin tocar producción.

## Build
Job KylGis Recursos 7ac7327fca37a2af3b536e4c quedó `uncertain` UnitMissing incluso tras reconcile y cancel seguro; systemctl --user y ps no mostraron unidad/Maven vivo. Alternativa no destructiva DC: copy rsync de fuentes a directorio NUEVO /tmp, sin .git ni target, Maven Temurin21 `mvn -o -q test`, **BUILD_RC=0**, directorio temporal removido. Compilar sobre target del worktree con fam falló por archivo perteneciente a server-conf; no se cambiaron propietarios/permisos. Maven existente sin Surefire tests aún no demuestra QA funcional completa.

## Estado de seguridad
Schema candidate no contiene vínculos reales ni credenciales ni llamadas a banco. No conectado a DataLogicSales. No instalar ni usar en .17/.18 ni production, no merge main. Se requiere resolver 12.3.3 y pruebas de concurrencia, fallas ACK/restart + proveedor confiable antes de integración de ledger.
