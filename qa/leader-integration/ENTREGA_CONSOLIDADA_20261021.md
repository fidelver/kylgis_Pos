# Entrega consolidada CORE/SAT/Kitchen/RIF — 2026-10-21

## Candidato y procedencia
Rama de revisión: **collab/leader-consolidado-core-sat-rif-20261021**.
Materialización sin conflictos: `9a53e34ed146388a82e6f8167f7b68f52f26b60d`, sobre SAT `a22513824f88d59e5f3e28ca1de8f6ddb0ab1668`, incorporando únicamente el commit RIF `c4baeccd6a080bee0a2018f22cd5c3216b191ce0`.
Plan aplicado SHA256 `ddac2d09c7d1033bbfdf7dfab8d0743a7e3fa8ced04d6808360b9e388e7b36dd`. Orígenes conservados, sin push. Este informe se añade como commit posterior; obtener HEAD final mediante Git.

| Línea | Rama/commit fuente | Resultado ejecutado |
|---|---|---|
| CORE/recuperación | leader-core-sat-integracion-20261021 / 375505c1c48568859eb36f18b6329b76fc76dae4 | Conservación de pagos/vouchers, extensión retrocompatible, guardias UI/backend y copia de efectivo corregida; 28 checks por versión |
| SAT | leader-sat-payment-binding-20261021 / a22513824f88d59e5f3e28ca1de8f6ddb0ab1668 | Hash pre-AUTH, importe/moneda/componente inmutables, UUID correlacionado con payments.ID, retención, CAS y estados sin retroceso; 106 checks bancarios por versión |
| Kitchen | kitchen-java21-modernizacion / 9ea6b3fd8c4eff8526f8b826cc5dad3545ba6dec | Ocho comandas, botones Terminar/Recuperar, F12, cierre y PID/sockets liberados; fallo gráfico inyectado devuelve RC1 |
| RIF/TLS | leader-rif-service-staging-20261021 / c4baeccd6a080bee0a2018f22cd5c3216b191ce0 | Servicio privado sintético nuevo; 37 checks de R05/R06/R09/R10/R11/R19/R23/R24/R26/R30, más limpieza |

## Verificación
- CORE/SAT: Java real y MariaDB 10.11, 11.8 y 12.3.3, todas RC0. JVM original finaliza antes de recuperar; dos cajas independientes, banco separado, ACK perdido, reconciliación, pagos mixtos y voucher descuento. Extractos en qa/leader-integration/evidence/qa-sat-binding-*.txt.
- Maven Java21 offline package -DskipTests sobre snapshot temporal nuevo: RC0. No equivale a una suite Surefire. Artefacto temporal de laboratorio SHA256 **aec91a53f5af68d1f5d4c8963f43508d3471ac0bf74bd08e3bf660a171ed72ee**.
- Kitchen se recompiló y ejecutó de nuevo con ese CORE final. Monitor reproducible SHA256 **ce5b63389b07122370c8c270aafe2a9af5eec9a64c22bd732ef38b3fbc2bc098**. Evidencia qa/leader-integration/evidence/qa-kitchen-final-core-20261021.txt.
- RIF usa MariaDB12.3.3 sin red externa y fixture efímera. Evidencia qa/rif_service/acceptance-evidence-20261021.txt. Los diez controles se ejecutaron en este servicio de laboratorio; el runner operativo original no cambió y no se certifica por sustitución.
- 16 archivos de código/DDL/harness consolidados comprobados SHA256 contra sus ramas de origen: todos idénticos. No se repitió una batería sólo por copiar archivos idénticos entre ramas.
- Inspección final: cero contenedores propios de estas pruebas, cero redes internas Kitchen restantes. No procesos POS productivos terminados ni servicios reiniciados. Main observado permanece en 9f50d98d2c3bec31305843bf2b3b87e8d7bee3a9.

## Documentos canónicos
- qa/leader-integration/QA_PAYMENT_CONTEXT_RECOVERY_20261021.md.
- qa/leader-integration/QA_IMMUTABLE_PAYMENT_BINDING_20261021.md.
- qa/rif_service/README.md.
- Rama Kitchen: tools/QA_CORE_CONTEXT_20261021.md y tools/run_core13_functional_qa.py.
- Las limitaciones detectadas de Utils quedan en NecesidadesyFallosKylGisUtils. No se alteró el job incierto UnitMissing previo.

## Condiciones abiertas y límites de autorización
**GO revisión del candidato aislado; NO-GO main/producción.** Las correcciones ensayables en laboratorio tienen implementación y evidencia, pero todavía no es una liberación comercial:
1. Conectar identidad/coordinador antes del diálogo de pago real a un banco/adquirente autorizado y comprobar su protocolo, credenciales, autenticación y conciliación. SQL APPROVED no constituye prueba bancaria. No hay reembolso ni segundo cargo automático.
2. Revisar flujo completo de POS Swing: suspensión/recuperación y decisión autorizada de conciliación. Las guardias y persistencia están ejecutadas; no se certifica una sesión completa de cajero por extrapolación.
3. Aprobar fingerprint y migración reversible del esquema vigente, mínimos privilegios y política de conservación. qa_* y flag siguen restringidos al catálogo sintético. No se permite promover este DDL directamente a .17/.18.
4. Definir tratamiento del voucher legacy como tender si aún se necesita. El descuento está probado; la ruta de tender ambigua se bloquea en QA.
5. RIF operativo necesita release aprobado por Utils, identidad OS dedicada, LoadCredential/rotación, filesystem protegido, supervisión y retención. El laboratorio usa mismo UID del desarrollador; no equivale a resistencia frente a dueño/root hostil ni autentica datos RIF reales. G3 no habilitado.
6. Windows y segunda pantalla Kitchen requieren plataforma/prueba física con el mismo hash. No sustituir esa evidencia con una GUI Fedora.
7. Downgrade de tickets financieros a lectores antiguos puede volver a omitir la extensión; no es un rollback comercial seguro sin conciliación/exportación. No se reconstruyen datos históricos que nunca se serializaron.

No se modificó producción, .17/.18, main, gateway, cuentas/certificados reales, SAT/CFDI/PAC ni bancos reales. Las claves, certificados y usuarios de pruebas fueron generados y eliminados sólo en fixtures.

## SHA256 de contenido consolidado
| Archivo | SHA256 |
|---|---|
| `src/main/java/com/mx/kylgis/pos/forms/DataLogicSales.java` | `abf3958327d6f7de65b8d5a786771395df2ab5c86d8d5acd8b434f60141af6da` |
| `src/main/java/com/mx/kylgis/pos/forms/SaleOperationLedgerQA.java` | `8d8b51635a0d50029be4764395ffde5fddf201346c4a82881da5d03ac5a81201` |
| `src/main/java/com/mx/kylgis/pos/forms/SaleOperationPayloadQA.java` | `51a3709e717d467cab9d5c74334ab852c082783a685c3cc7049914d9d374657d` |
| `src/main/java/com/mx/kylgis/pos/ticket/TicketInfo.java` | `90b1a400669f6b0652de299a667a8468978cfcf8de141b19b6a6a908167896e6` |
| `src/main/java/com/mx/kylgis/pos/ticket/TicketPaymentContext.java` | `ce73a9800a1238b126ef8ad2a58a88a1c38bc304f7fd461e86e5745bccb6bb85` |
| `src/main/java/com/mx/kylgis/pos/payment/PaymentInfoCash.java` | `15b8580a089c93324f7fad110b023c39da8613fd9e8c71375459f9af1339f6f7` |
| `src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java` | `cb91b1d36f4c06e937721112f8ff7c9bd0f8792ed9e65959a8b0aebc0407dcf9` |
| `qa/sandbox/qa_durable_payment_ledger_candidate.sql` | `e96e748232bf6663ef0b13336edec69621fcaa06207694013bcb8c2a25e90617` |
| `qa/sandbox/run_real_ticket_status_isolated.sh` | `e5b3110af04d2941aa6322a7878995c4aedee69c0fbbec5d8b967651f086c9a9` |
| `qa/manual/IndependentBankFlowProbe.java` | `8530e74ebba0f536e278ec2887c12b38add85dca76a1cee16b2c20733509c81f` |
| `qa/rif_service/private_service.py` | `da491ff33b81d0cc5ff05e454cbd32d418a6cca527463896bb663ba31e74ca7c` |
| `qa/rif_service/test_service_isolated.py` | `ffc19ceb4c46ae0f979fab1a06c6276704dc26b36fc2ce8c190d21e5b68ae05e` |
| `qa/rif_service/vendor/kylgis_recipe_bundle_private_runner_candidate.py` | `ff139986f475acb390635b7fbb985b0eb6e1f687d10c541bdd594e6f0cf39178` |
| `qa/rif_service/vendor/kylgis_recipe_bundle_rif_reader_candidate.py` | `c9a8ef3f986ab2297ac38a9c5dacb7704a2fe33090cde077bb5abddbfd31d190` |
| `qa/rif_service/vendor/kylgis_recipe_bundle_rif_tls_gates_candidate.py` | `6798f9ceb4e6cd8185001968b81e0979679d4202786fb35abd06463b1142d032` |
| `qa/rif_service/vendor/kylgis_recipe_bundle_rif_capture_contract_candidate.py` | `e9be5b889ecd56b567840ee2df93a9add7733d53506e85f981ec8bd949b4f3c9` |
