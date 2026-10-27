# Certificación KylGis POS Multiplataforma

Fecha de corte: 2026-10-27

## Estado actual

| Área | Fedora/Linux x64 | Windows x64 | ARM64 | Producción |
|---|---|---|---|---|
| POS ventas | Compilación Java 21 aprobada | Pendiente de equipo físico | Pendiente de hardware objetivo | No sustituir sin autorización |
| Configuración de nodo/roles | Preflight aprobado | Pendiente | Pendiente | Configuración portable |
| Diagnóstico de periféricos | Preflight sin efectos laterales aprobado | Pendiente | Pendiente | No bloquea ventas |
| KitchenScreen | Compilación y paquete autocontenido aprobados | Pendiente | Pendiente | No sustituir sin autorización |
| Empaquetado | App-image POS y Kitchen con runtime Java 21 | Script preparado para ejecutar nativamente | Script preparado para ejecutar nativamente | Requiere SHA y respaldo |
| Java 8 | Sin referencias de compilación detectadas | Pendiente de verificación del instalador | Pendiente | No usar lanzadores antiguos |

## Artefactos Linux x64

Los paquetes generados se guardan fuera de los worktrees:

- /home/fam/NetBeansProjects_BACKUPS/KylGisPortable_20261027/POS_Linux_x64
- /home/fam/NetBeansProjects_BACKUPS/KylGisPortable_20261027/Kitchen_Linux_x64

Cada paquete incluye:

- Aplicación.
- Runtime Java 21 reducido con jlink.
- Librerías nativas del lanzador.
- Archivo de SHA-256.
- Lanzador autocontenido.

## Reproducción

POS:

JAVA_HOME=/usr/lib/jvm/java-21-temurin-jdk bash build-portable.sh

Kitchen:

JAVA_HOME=/usr/lib/jvm/java-21-temurin-jdk KYLGIS_CORE_JAR=/ruta/kylgispos.jar bash build-portable.sh

El script exige un worktree limpio y aborta si no detecta JDK 21.

## Pendientes físicos

1. Ejecutar el POS empaquetado en Windows x64.
2. Ejecutar Kitchen en Windows con segunda pantalla.
3. Validar impresora térmica, cajón, lector, báscula y comunicación remota.
4. Repetir en ARM64 si existe hardware objetivo.
5. Certificar contra las cuatro bases de laboratorio/producción autorizadas sin mezclar datos.
