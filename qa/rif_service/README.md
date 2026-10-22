# Servicio privado RIF/TLS — candidato de laboratorio

Base `4b448114f72fd1a7bda9122a3cbfd764988d3196`, rama `collab/leader-rif-service-staging-20261021`. **No instalado en MCP, no es el release operativo, no habilita G3 ni run_live de Utils.**

Se implementó el perímetro de servicio que faltaba al staging de transcripciones: otro proceso Python, socket Unix privado, peticiones cerradas, captura SQL desde procesos propios, pin del ejecutable Podman por FD, imagen OCI por content-address y SHA del cliente MariaDB dentro de esa imagen, CA y leaf SHA256 en cada sesión SQL. El servicio usa exclusivamente un contenedor sintético `--network none` con etiqueta propia; no admite host, SQL, PhaseResult, stdout, key, pin ni claims del cliente IPC.

La captura fija conserva la transacción read-only/REPEATABLE READ original y agrega una última consulta con capture_id, CONNECTION_ID y @@in_transaction. Se verifica la misma sesión y ausencia de transacción abierta después de COMMIT antes de aceptar el resultado. Cada artifact privado vincula snapshot, fases, manifest de código, imagen/cliente/CA/leaf, UUID y TTL; HMAC privado y registro de consumo O_EXCL+fsync sobreviven reinicio del servicio. No se almacenan transcripciones con GRANT/password en el spool.

Los módulos Utils originales no se editaron. Las cuatro copias versionadas de vendor son byte a byte idénticas, comprobadas por KylGis Recursos:
- `kylgis_recipe_bundle_private_runner_candidate.py`: `ff139986f475acb390635b7fbb985b0eb6e1f687d10c541bdd594e6f0cf39178`.
- `kylgis_recipe_bundle_rif_reader_candidate.py`: `c9a8ef3f986ab2297ac38a9c5dacb7704a2fe33090cde077bb5abddbfd31d190`.
- `kylgis_recipe_bundle_rif_tls_gates_candidate.py`: `6798f9ceb4e6cd8185001968b81e0979679d4202786fb35abd06463b1142d032`.
- `kylgis_recipe_bundle_rif_capture_contract_candidate.py`: `e9be5b889ecd56b567840ee2df93a9add7733d53506e85f981ec8bd949b4f3c9`.

## Ejecución propia y resultado
```bash
python3 -B qa/rif_service/test_service_isolated.py
```
En Fedora, MariaDB **12.3.3** local y certificados/credenciales **generados exclusivamente para fixture**:
**RC0**, `RIF_SERVICE_STAGING_ACCEPTANCE_PASS checks=37`, comprobación adicional de contenedor eliminado, `RIF_SERVICE_FIXTURE_RESOURCES_CLEANED`.

| Control pendiente original | Evidencia nueva en este candidato |
|---|---|
| R05 | Proceso separado; PhaseResult/SQL/claims de cliente rechazados en socket |
| R06 | Manifest verificado antes de importar dependencias y antes de capturar/consumir; manifest incorrecto y SHA incorrecto del cliente MariaDB rechazados |
| R09 | Pin leaf incorrecto bloquea la sesión SQL real del servicio |
| R10 | Servidor presenta leaf vencido y aún no válido, con sus respectivos pins exactos; ambos handshakes rechazados |
| R11 | Rotación real entre preflight y capture rechazada |
| R19 | Egreso SQL de misma CONNECTION_ID verifica @@in_transaction=0; dos fases con sesiones distintas |
| R23 | Snapshot privado modificado no supera HMAC/binding |
| R24 | Dos capturas concurrentes generan UUID/spool independientes y consumibles |
| R26 | TTL con reloj real, replay tras reinicio, permisos de socket/spool y debilitamiento de ACL durante ejecución rechazado |
| R30 | Escritor concurrente real tras counts; snapshot conserva 2 filas/recuento mientras BD posterior tiene 3 |

Los 10 controles tienen evidencia **del nuevo servicio de laboratorio**. La suite congelada anterior conserva sus SKIP frente a su propio runner; no se repinó ni se declaró certificada la instancia operativa final.

## Reproducibilidad, diagnóstico y limpieza
El harness materializa una copia privada local de código verificado para evitar lecturas concurrentes/FUSE y rechaza hash modificado. Imagen CLI fijada a content-address, mismo binario medido y raíz read-only. Clave/usuarios/certificados sólo viven dentro del árbol de fixture; el test termina únicamente sus procesos y contenedor, y borra su propio árbol. Administración de fixture por socket Unix con --skip-ssl para permitir restaurar un certificado vencido; **la cuenta lectora nunca omite TLS**.

Primeras ejecuciones detectaron: log readiness dividido entre stdout/stderr, normalización del prefijo sha256 del ID OCI, plazo de importación desde el montaje y cliente administrativo que rechazaba el leaf vencido. Se corrigieron en el harness; todos esos intentos limpiaron sus recursos y no contaron como PASS. Último log completo: `/tmp/kylgis-rif-service-final-20261021.log`; extracto versionado junto a este documento.

## Límites y condiciones para integrar
- El UID del fixture es el mismo del desarrollador; el key y spool son privados a ese UID. Esto **no** reemplaza separación de identidad de un servicio operativo frente a un usuario host hostil. No resiste root ni manipulación in-place por el dueño.
- Faltan release/manifest aprobado por propietario Utils, identidad OS dedicada, LoadCredential/rotación operativa, directorio/FS protegidos, supervisión/cancelación y política de retención. No es correcto instalar este prototipo tal cual.
- Los archivos, SQL y config admitidos son deliberadamente sintéticos. No lee datos reales RIF ni certifica las 1,075 relaciones ni la combinación SALSA/TOMATE real.
- No se conectó .17/.18, no se cambió gateway, permisos operativos, certificados reales, usuarios reales, main, procesos POS ni el runner original. Claves y CA de prueba no son credenciales autorizadas para integración.
- El servicio limita y rechaza artefactos grandes; aún no es un exportador de 4 MiB operable. No se generalizó transporte/retención para el catálogo real.

Dictamen: **GO revisión técnica y repetición de aceptación del servicio aislado; NO-GO despliegue y certificación RIF de producción.** Las piezas pendientes de código tienen ahora implementación reproducible; el traspaso a identidad/release operativos requiere el control de integración de Utils.
