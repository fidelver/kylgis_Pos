# Checkpoint SAT — 2026-10-21
Rama collab/leader-sat-payment-binding-20261021. Base 375505c1c48568859eb36f18b6329b76fc76dae4 (incluye recuperación financiera).

Candidato adicional: hash de payload financiero antes de autorizar, índice de componente, misma UUID en intención bancaria y payments.ID, importes/moneda/referencias inmutables por triggers QA, retención de operaciones y prohibición de retroceder APPROVED a UNKNOWN. No modifica payments.TRANSID. El handler QA rechaza tender voucher legacy, reserva no vinculada y tarjeta sin intención confirmada.

Evidencia final: QA_IMMUTABLE_PAYMENT_BINDING_20261021.md y extractos qa-sat-binding-*.txt. En MariaDB10.11/11.8/12.3.3 se ejecuta el Java real, perdidas de ACK, recuperación entre JVM, dos cajas, banco sintético separado; se conserva gate de catálogo y flag apagado.

Maven Java21 offline package -DskipTests, snapshot aislado nuevo: RC0. Artefacto temporal de pruebas sellado SHA256 aec91a53f5af68d1f5d4c8963f43508d3471ac0bf74bd08e3bf660a171ed72ee. No declarar release productivo ni tests Surefire.

Otras ramas de esta entrega:
- CORE/recuperación: 375505c1c48568859eb36f18b6329b76fc76dae4.
- Kitchen: 9ea6b3fd8c4eff8526f8b826cc5dad3545ba6dec, batería gráfica sintética reproducible.
- Servicio RIF/TLS aislado: c4baeccd6a080bee0a2018f22cd5c3216b191ce0, 37 verificaciones de los 10 controles en el candidato nuevo. No habilita runner operativo.

No-GO producción/main hasta integrar con diálogo y banco autorizados, fingerprint/migración de esquema, revisión POS Swing completo y credenciales/identidad de servicio RIF gestionadas por Utils. Windows y segunda pantalla requieren plataforma/prueba física. Sin .17/.18, gateway, PAC/CFDI ni modificaciones de permisos operativos.
