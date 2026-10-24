# Reconciliación de worktrees — 2026-10-24

- KylGis Recursos `read_text` rechazó los nombres de worktree como proyectos autorizados; se utilizó Desktop Commander sólo para comparación local.
- No se modificaron producción, `.17`, `.18` ni `main`. Se retiró únicamente el worktree redundante `p1-scale-integration`; la rama `collab/p1-scale-integration` se conservó.
- `p1-scale-integration` no se integra como rama completa: su diff comprometido reescribe controladores y elimina clases ya presentes en la canónica.
- Se rescataron únicamente el catálogo de comandos y la reconciliación del selector UI sobre la base canónica existente.
