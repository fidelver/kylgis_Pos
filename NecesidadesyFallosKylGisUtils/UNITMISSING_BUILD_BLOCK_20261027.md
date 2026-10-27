# Bloqueo UnitMissing de compilación — 2026-10-27

## Estado

El trabajo de compilación KylGis Recursos `7ac7327fca37a2af3b536e4c`
(`maven-test-jdk21`, proyecto `pos-worktree`) permanece en estado
`uncertain` con el error:

```
UnitMissing: Unidad inexistente
```

## Impacto

El estado incierto impide iniciar nuevas tareas de compilación para el
worktree `canonical-integration-20261026`, aunque el worktree está registrado,
el cambio está staged y `git diff --check` pasa.

## Intentos seguros

- `status`: confirma el estado `uncertain`.
- `reconcile`: conserva el estado `uncertain`.
- `cancel`: no libera la unidad y conserva el mismo estado.
- No se detuvieron procesos de POS.
- No se modificaron producción ni las bases `.17/.18`.
- No se usó Desktop Commander para sustituir la recuperación.

## Recuperación requerida

Activar la herramienta de recuperación controlada UnitMissing
(`preflight → plan → grant → apply → status`) para marcar como perdido
únicamente el trabajo incierto y liberar la capacidad de compilación.
