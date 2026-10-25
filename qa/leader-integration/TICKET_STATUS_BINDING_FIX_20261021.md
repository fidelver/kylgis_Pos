# Corrección candidata P0 — binding STATUS/TICKETID (2026-10-21)

Rama aislada collab/leader-core-sat-integracion-20261021, basada en CORE13 9f50d98d2c3bec31305843bf2b3b87e8d7bee3a9. Main, producción y .17/.18 intactos.

## Cambio ya aplicado
En DataLogicSales.saveTicket, la sentencia `UPDATE tickets SET STATUS = ? WHERE TICKETTYPE = 0 AND TICKETID = ?` recibe ahora `setInt(1,ticket.getTicketStatus()); setInt(2,ticket.getTicketId());`. Antes era a la inversa. Diff solo cambia 2 líneas, sin alterar SQL ni commit boundary.

## Evidencia
QA negativa previa SAT commit 8bf25e44745d67735d0db53a92d331024fa1b56f reportó 50/50 detectores contra SQL/JDBC sintético en MariaDB 10.11, 11.8, 12.3.3: con ticket A ID42 y B ID7, el binding anterior dañaba B; esa QA no ejecutaba saveTicket real.
Build **propio** KylGis Recursos JDK21, job fde8c457edcc167a62f0d8ac, `maven-compile-jdk21`, 699 fuentes, BUILD SUCCESS RC0. Compilación no equivale a prueba funcional.

## Bloqueadores antes del merge
1. En MariaDB sintética completa, reproducir saveTicket real con ticket nuevo/editado, TicketType 0 y status distintos de ticket ID; demostrar que solo cambia fila destinataria y no otras.
2. Verificar comportamiento de filas afectadas, tickets duplicados por TICKETTYPE/TICKETID y condiciones de edición. Revisar si el UPDATE debe ejecutarse en nuevas ventas o solo ediciones.
3. Ejecutar regresión voucher consumo con canje y pagos; no autorizar refund/cobro automático ante COMMIT ambiguo.
4. Ejecutar pruebas de doble caja y restart con banco mock, y asegurar control idempotente durable sin cambiar indiscriminadamente TRANSID UNIQUE.

## Política
Solo candidato dentro del worktree, sin merge, deploy, reset folios, datos reales o tocar áreas de selector báscula. Mantener fiscal.sat.enabled=false.
