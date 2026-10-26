# Checkpoint de apagado y reanudación — 2026-10-22

El usuario solicitó guardar el estado y posteriormente confirmó que el apagado ya se realizó y pidió continuar en este mismo chat. La pausa terminó: reanudar el trabajo autorizado. No apagar equipos mediante herramientas.

## Estado que debe retomarse
- Proyecto: /home/fam/00-MultiPlataforma (alias /home/fam/00-ChatGPT/00-MultiPlataforma).
- Worktree actual: /home/fam/00-MultiPlataforma/worktrees/leader-pos-durable-suspend-20261022.
- Rama: collab/leader-pos-durable-suspend-20261022.
- Último commit de implementación y evidencia: 5cfcb80e0a0f0f7add47d162b82b4660a8d3388e; padre 5f2f73b2c382987b876f346014016c0e0ea3a04e.
- El worktree estaba limpio al verificarlo antes del checkpoint. Este archivo se guarda como un commit documental posterior; obtener su HEAD al reanudar.
- Canon: qa/leader-integration/ENTREGA_ACTUALIZADA_20261022.md y QA_DURABLE_SUSPENSION_20261022.md en el mismo directorio.

## Implementación y validación terminadas
Recuperación financiera estándar conserva sharedtickets al abrir, propietario aleatorio de 20 caracteres compatible con LOCKED VARCHAR(20), persistencia/liberación en un UPDATE condicional, serialización estricta antes de SQL, retención de modelo/ID ante fallo de Desactivar/Nuevo/Hold, login que respeta desactivación rechazada y bloqueo de una operación pre-AUTH sin componentes.

Matriz MariaDB10.11/11.8/12.3.3: 383 aserciones Java por versión / 1,149 totales, todas RC0. Incluye SIGKILL/reinicio de JVM propia, carrera de dos JVM con un único dueño, fallos de serialización y SQL, y ACK perdido real del UPDATE de suspensión tras observar OK del servidor. La GUI sigue retenida sin sobrescribir una DB ya confirmada. Tres regresiones contra el CORE anterior documentadas por separado.

Regresión SAT/banco sintético en 10.11 RC0: serialización28, una venta entre dos cajas, ACK de COMMIT perdido y banco106. Kitchen con CORE final RC0: ocho comandas, botones, F12, cierre, cuatro JVM liberadas y fallo inyectado RC1. Monitor SHA256 ce5b63389b07122370c8c270aafe2a9af5eec9a64c22bd732ef38b3fbc2bc098. Evidencias ya versionadas en qa/leader-integration/evidence/qa-durable-suspend-*.txt y qa-kitchen-durable-suspend-20261022.txt; no dependen de /tmp.

## Artefactos temporales y apagado
CORE de laboratorio generado por Maven Java21 offline package -DskipTests: SHA256 9a41467bf17b50f7d4a88697d084e462324795ca4ad1485714507ec032ecd1b8. Ubicación antes de apagar: /tmp/kylgis-core-durable-suspend-<SHA>.jar. Metadata Git UNSET del snapshot; no release comercial ni instalado.

Se intentó copiar ese JAR fuera de /tmp para conservarlo, pero Desktop Commander devolvió «No Desktop Commander device is currently online». La copia NO se ejecutó y NO debe asumirse persistida. Si /tmp se limpia, reconstruir desde el commit conservado; no perderemos fuentes ni evidencia. El JAR base de pruebas anterior que también reside en /tmp puede desaparecer: los baselines históricos ya tienen evidencia versionada y no hay que fingir una reproducción si falta el binario original.

Última comprobación de runtime que sí terminó: cero contenedores propios, cero redes propias y cero JVM de pruebas de esta batería. El JAR tenía el hash indicado. No se terminaron POS productivos ni otros procesos. No hay trabajos nuevos de pruebas deliberadamente dejados en ejecución. El job Utils antiguo 7ac7327fca37a2af3b536e4c sigue uncertain/UnitMissing; no se canceló ni se alteró.

## Reanudación
1. Usar KylGis Recursos primero: estado Git por nombre, HEAD y estos documentos; no repetir ni sobrescribir trabajo ya validado. Confirmar disponibilidad del worktree y que no haya cambios de otros colaboradores.
2. Consultar capacidades de build si cambiaron; el último worktree_build_start estaba bloqueado por el job incierto. Desktop Commander sólo es fallback para runtime/builds que Utils no cubra o falle. Dispositivo Fedora conocido: 219b2047-97f0-41b8-9a1c-7ef86b5957c2; comprobar que esté online antes de ejecutar.
3. Si falta el JAR temporal, reconstruir en un snapshot nuevo excluyendo .git/target, con /usr/lib/jvm/temurin-21-jdk y mvn -o -q package -DskipTests; sellar el hash nuevo sin atribuirle automáticamente el hash previo. No sobrescribir target de otros usuarios. Los runners aceptan --core-jar y --core-sha256 y rechazan usar artefactos de target como entrada sellada.
4. Continuar pendientes autorizados: flujos completos de modos simple/restaurante y coordinador durable anterior a autorización bancaria. No crear un desbloqueo financiero sin decisión autorizada ni inventar banco/protocolo/credenciales. La recuperación tras caída deja el ticket retenido para revisión, no habilita recobro.
5. Mantener gates pendientes: banco/adquirente autorizado, conciliación, fingerprint/migración/retención, clientes compatibles con candados, voucher tender legacy, RIF operativo e identidad OS dedicada, Windows/segunda pantalla. No se garantiza recuperar hechos anteriores a su primera persistencia ni CONTENT NULL históricos.

## Límites y ramas de procedencia
No producción, .17/.18, main, gateway, cuentas/certificados reales ni SAT/CFDI/PAC; sin push/merge/despliegue. Main observado: 9f50d98d2c3bec31305843bf2b3b87e8d7bee3a9.

Procedencia conservada: consolidado23b90ee4; CORE375505c1; SATa2251382; RIFc4baeccd; Kitchen9ea6b3fd; recuperación Swing5f2f73b2. No cambiar estas ramas fuente para reanudar. El candidato actual incorpora los cambios sobre esas líneas y sólo se continúa con aislamiento, guardas y evidencia.
