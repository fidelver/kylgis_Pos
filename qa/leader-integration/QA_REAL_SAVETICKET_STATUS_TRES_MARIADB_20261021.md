# Líder P0 — CORE1.3 reparación ticket status, QA real 3 MariaDB (2026-10-21)

Rama exclusiva `collab/leader-core-sat-integracion-20261021`; base CORE13 `9f50d98d2c3bec31305843bf2b3b87e8d7bee3a9`. Primer commit `49c5505dc604c2074eec76419e907c79ece0f5b0` ajustó el binding SQL de STATUS/TICKETID en DataLogicSales, compiló 699 fuentes. Esta segunda iteración corrige además `TicketInfo.setTicketStatus` sin tocar serialización ni ABI público.

## Hallazgo confirmado con clase real
La clase ORIGINAL del JAR CORE13 sellado SHA256 `ecc1047d67df07b7178b2bad27eec9d931332bb43285e9a840da3835d86fce2b` falla `TicketStatusSetterProbe`: folio 42 + setTicketStatus(7) -> getTicketStatus()=42. Resultado `AssertionError EXISTING_STATUS_CORRUPTED`, RC1 esperado. Después de compilar únicamente TicketInfo corregido en QA temporal y ejecutarlo sobre el JAR sellado, PASS status nuevo e independiente de ID, RC0.

## Prueba end-to-end real de la ruta comercial
Se crearon `qa/manual/TicketStatusSchemaProbe.java`, `qa/manual/RealTicketStatusSaveProbe.java`, `qa/manual/TicketStatusSetterProbe.java` y `qa/sandbox/run_real_ticket_status_isolated.sh`. El script ejecuta el cargador original parametrizado `BatchSentenceResource`, usa CORE13 sellado junto con clases DataLogicSales/TicketInfo compiladas desde este worktree, JDK21, MySQL JDBC legacy original y MariaDB efímera `--network none`/read-only/tmpfs. Se niega a iniciar si hay contenedor con nombre coincidente, si CORE JAR SHA no coincide, imagen no precargada o JDK/dependencias faltan; limpia contenedor por trap.

Caso: ticket previo receipt QA_UNRELATED_RECEIPT, TICKETID=7 STATUS=8; ticket nuevo con `TicketInfo.setTicketId(42)`, `setTicketStatus(7)`, caja, usuario, producto 30 y pago cash 30. Llamada REAL `DataLogicSales.saveTicket(ticket,"0")`, luego NUEVA conexión al DB para verificar: ticket42=status7, ajeno status8, 1 ticketline, 1 payment, 2 receipts. No simulación SQL del guardado.

- MariaDB 10.11: `REAL_SAVE_TICKET_STATUS_PASS checks=10`, RC0, schema PASS y cleanup PASS.
- MariaDB 11.8: `REAL_SAVE_TICKET_STATUS_PASS checks=10`, RC0, schema PASS y cleanup PASS.
- MariaDB 12.3.3: `REAL_SAVE_TICKET_STATUS_PASS checks=10`, RC0, schema PASS y cleanup PASS.
- Inicial primer intento abortó en guardia JDBC `DatabaseMetaData.productName` que el driver MySQL5.1 informa MySQL frente a servidor MariaDB. Harness corregido para comprobar `@@version` contiene MariaDB y `@@port=3306` del contenedor. Ninguna falla comercial, sin cambios en CORE por ese incidente.
- Primer `javac` CLI externo falló por default Java8 y permisos de dependencias del paquete pertenecientes a server-conf. Se aisló Java21 y se utilizaron dependencias históricas públicas del caché local, sin alterar ACL del host.

## Límites
Prueba certifica solo combinación de dos correcciones con venta nueva de folio predeterminado (42), no actualización histórica/edición, doble TICKETID, concurrencia de voucher, COMMIT ACK perdido ni recuperación de banco. No merge a main ni producción hasta certificar esos gates y aprobar integración. Ninguna conexión a .17/.18 ni proveedor. Mantener fiscal.sat.enabled=false.
