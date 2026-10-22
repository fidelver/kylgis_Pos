# Recuperación de pagos/vouchers — candidato CORE/SAT 2026-10-21

Base `4b448114f72fd1a7bda9122a3cbfd764988d3196`, rama `collab/leader-core-sat-integracion-20261021`. Sin producción ni main.

## Correcciones
- TicketInfo Externalizable incorpora extensión KPC1 versionada y acotada: 256 componentes, 256 KiB, números finitos, campos limitados. Conserva importes, pagado/entregado/cambio, tipo de pago, referencia del proveedor, código/ID/cliente del voucher, valor nominal/aplicado, caja y estado procesado.
- El formato mantiene serialVersionUID y prefijo histórico. El lector nuevo acepta ausencia completa de extensión (tickets antiguos); una extensión parcial, corrupta, sobredimensionada o de versión desconocida falla. Prueba con clase histórica real de CORE sellado confirma ambos sentidos y límites de objetos consecutivos.
- Los pagos recuperados son registros inertes; no reconstruyen PaymentInfoMagcard ni PAN, pistas, claves o credenciales. Toda recuperación con contexto financiero requiere conciliación.
- Guardia compartida TicketInfo.requiresPaymentReconciliation en JPanelTicket **antes del diálogo de pago** y en DataLogicSales.saveTicket **antes de acceder a BD**. No existe botón/API de desbloqueo automático.
- copyTicket con UUID de operación o estado pendiente conserva origen y bloquea el nuevo recibo: una copia no obtiene otra autorización bancaria.
- PaymentInfoCash: corregido constructor de copia que invertía pagado y entregado.
- ExitedProcessSaleRecoveryProbe verifica ahora conservación real de pago/voucher, luego elimina exclusivamente la guardia local en QA para probar de forma independiente que el ledger durable también impide reintentar.

## Resultado propio
`PaymentContextSerializationProbe`: **28 verificaciones PASS por versión**, incluyendo compatibilidad histórica, dos suspensiones, referencias, mezcla de pagos, efectivo con importes distintos, vouchers, copia correlacionada, backend sin BD, número no finito, cardinalidad y cuatro corrupciones de extensión.

Matriz completa ejecutada con `KYLGIS_QA_BANK_OPTIN=1 KYLGIS_QA_LEDGER_OPTIN=1 KYLGIS_QA_LEDGER_ACK_OPTIN=1 KYLGIS_QA_MARIADB_VERSION=<10.11|11.8|12.3.3> bash qa/sandbox/run_real_ticket_status_isolated.sh`: RC0 en las tres; recuperación entre procesos, dos cajas, banco independiente (83 verificaciones) y limpieza aprobados. Logs fuente `/tmp/kylgis-payment-context-final-<versión>-20261021.log`.

Maven Java21 offline, snapshot nuevo sin .git/target, `mvn -o -q package -DskipTests`: RC0. No se cuentan pruebas Surefire. CORE temporal sellado para QA Kitchen:
`abe3d9166396fa0db74d1dcdc329f73966825f59cfe7945af54656efcb7645bf`.
Salida temporal `/tmp/kylgis-core-payment-context-<SHA>.jar`; no desplegada.

## Alcance y rollback
Compatibilidad con lector antiguo significa que puede leer el prefijo y omitir la extensión; **no garantiza preservación financiera si se vuelve a guardar con una versión antigua**. No realizar downgrade operacional de tickets financieros con este formato sin conciliación/exportación. Los tickets históricos que omitieron pagos/vouchers no permiten reconstruir información que nunca guardaron; los que conservan una operación/resultado incierto quedan inmovilizados.

No hay conciliación bancaria comercial autorizada ni migración de qa_* a producción. Ledger sigue opt-in/sintético. La comprobación del guardián backend es ejecutada; la interacción completa del POS Swing y una decisión humana de conciliación siguen pendientes. No aplicar automáticamente cargos, devoluciones, desbloqueos ni reset de folios.
