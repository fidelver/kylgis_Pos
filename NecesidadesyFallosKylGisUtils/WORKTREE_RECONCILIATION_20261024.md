# Reconciliación de worktrees — 2026-10-24

- KylGis Recursos `read_text` rechazó los nombres de worktree como proyectos autorizados; se utilizó Desktop Commander sólo para comparación local.
- No se modificaron producción, `.17`, `.18` ni `main`. Se retiró únicamente el worktree redundante `p1-scale-integration`; la rama `collab/p1-scale-integration` se conservó.
- `p1-scale-integration` no se integra como rama completa: su diff comprometido reescribe controladores y elimina clases ya presentes en la canónica.
- Se rescataron únicamente el catálogo de comandos y la reconciliación del selector UI sobre la base canónica existente.

- Se integró `pos13-rc15-win-accessibility-20261019` en dos commits sobre la canónica (`f7ba5022`, `91edbea3`), resolviendo manualmente los conflictos del selector de báscula y conservando el catálogo localizado; se añadieron ayuda contextual y recursos de UI para los 17 locales.
- El compilado Maven offline con Java 21 y el selftest offline finalizaron correctamente (`17` pruebas, `database_mutated=false`).
- `leader-report-font-gated-release-13-20261018` y `leader-sales-jrxml-java21-heights` no aportan cambios nuevos integrables: sus reportes coinciden con la canónica o quedan superados por alturas/contratos ya consolidados; sus worktrees sucios se conservan.
