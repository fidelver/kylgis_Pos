# Política de homologación de ramas

## Rama canónica

La línea integrada del POS queda en `collab/homologated-pos-canonical-20261023`. Cada objetivo validado se integra aquí antes de iniciar el siguiente, conservando una sola línea de entrega reproducible.

## Flujo obligatorio

1. Trabajar en una rama de objetivo independiente.
2. Validar el objetivo con pruebas reproducibles y registrar la evidencia.
3. Integrar el cambio en la rama canónica, resolviendo conflictos por comportamiento y no sobrescribiendo funcionalidades ya validadas.
4. Ejecutar el preflight/selftest Java 21 y comprobar que el worktree queda limpio.
5. Sólo después iniciar el siguiente objetivo.

## Límites de seguridad

- Las ramas históricas, experimentales o con worktree sucio se conservan como referencia y no se fusionan a ciegas.
- Las ramas y artefactos de producción, incluidos `.17`, `.18`, RIF y Plataformas, no se modifican durante la homologación local.
- No se eliminan ramas ni se publica al remoto sin autorización explícita.
- Las nuevas ramas de objetivo deben partir de la rama canónica o integrarse en ella al concluir el objetivo.

Fecha de adopción: 2026-10-23.
