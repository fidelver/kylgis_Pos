# Checkpoint líder — 2026-10-21
Rama: `collab/leader-core-sat-integracion-20261021`.
Base al retomar: `22fbfc1da5b5d1e25ea0567a65152f3d69072a7a`. Obtener commit de entrega con git_worktree_log; este archivo se incluye en el commit de la entrega.

## Avance validado
- JVM original de QA termina efectivamente antes de nueva JVM de recuperación; reintento del saveTicket real bloqueado.
- Dos cajas Java independientes con barrera y un solo saveTicket confirmado.
- Banco simulado en proceso independiente: intención durable antes de AUTH, CAS de despacho, APPROVED/DECLINED/UNKNOWN, timeout después de autorización, consulta de conciliación, mismo idempotency key, cambio de importe rechazado, dos JVM/cargo único, caída y reinicio bancario, pagos mixtos y voucher descuento, ACK del COMMIT POS también perdido.
- Matriz final de todo lo anterior: MariaDB 10.11/11.8/12.3.3, RC0, contador banco 83 por versión, 160 marcadores PASS por versión incluyendo aserciones repetidas. Todos los contenedores QA eliminados.
- Maven Java21 offline sobre snapshot nuevo sin .git/target: RC0, snapshot eliminado; no usar como sustituto de pruebas de runtime.
- RIF runner 30/30 y staging 27/27 PASS por KylGis Recursos. Suite independiente: 26 PASS +10 SKIP. TLS/rotación/expiración/MVCC contra MariaDB sintética: RC0. Ninguno de los diez SKIP se convierte en PASS de servicio final.

## Evidencia canónica
- `qa/leader-integration/QA_EXITED_JVM_CASHBOX_BANK_20261021.md`: cambios, comandos, hashes y límites.
- `qa/leader-integration/evidence/qa-bank-processes-*-20261021.txt`: extractos de las tres ejecuciones finales.
- `qa/leader-integration/QA_RIF_TLS_REVALIDACION_Y_GATES_20261021.md`.
- `NecesidadesyFallosKylGisUtils/QA_PROCESOS_INDEPENDIENTES_20261021.md`.

## Estado de entrega y fronteras
**Las tres líneas NO están certificadas integralmente y NO están listas para main/producción.** Los avances actuales son candidatos QA reproducibles. Ledger sigue opt-in y limitado a pos_qa_status. No tocar .17/.18, main, gateway, SAT/CFDI/PAC, banca real, permisos o credenciales. No reiniciar POS productivo.

Pagos/vouchers e identidad de suspensión/copia: implementados y probados en el candidato posterior a 4b448114; ver QA_PAYMENT_CONTEXT_RECOVERY_20261021.md (28 verificaciones por versión, guardia UI/backend, compatibilidad histórica y corrección de copia de efectivo). Kitchen también se reejecutó gráficamente con el CORE temporal nuevo: ocho comandas, botones, F12/cierre y recursos liberados. Pendientes CORE/SAT: interacción completa del POS Swing tras recuperación y conciliación autorizada; adaptador bancario comercial, binding inmutable de importes/componentes y claves previas al diálogo; decisión voucher legacy; esquema/migración y fingerprint autorizados. Windows y segunda pantalla Kitchen aún requieren plataforma/prueba física.

Pendientes RIF/TLS: release privado definitivo y servicio/credenciales aprobados por propietario Utils. El runner observado devuelve PENDING_LEADER/execution_enabled=false y run_live=NO_GO. No habilitarlo para intentar hacer pasar casos. Diez casos pendientes: R05/R06/R09/R10/R11/R19/R23/R24/R26/R30.

Job Maven privado `7ac7327fca37a2af3b536e4c`: sigue uncertain/UnitMissing en lectura nueva; no borrar ni reconciliar sin propietario. Git mediante Utils: el .git del worktree apunta a ruta CT no accesible desde el Git de Fedora.
