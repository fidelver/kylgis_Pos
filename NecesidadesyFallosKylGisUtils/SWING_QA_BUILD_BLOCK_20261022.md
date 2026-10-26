# Swing QA y compilación — 2026-10-22

Se intentó worktree_build_start(name=leader-pos-recovery-flow-20261022, expected_head=23b90ee4c6f4d59370f30e204a0006110316c4d3, jdk=21, task=compile). Rechazo: RuntimeError: Trabajo activo o incierto. El job previo 7ac7327fca37a2af3b536e4c continúa uncertain / UnitMissing; no se canceló, reconcilió ni alteró su identidad sin evidencia suficiente.

Fallback autorizado: Desktop Commander para Java/Podman de laboratorio y, si procede, Maven offline sobre snapshot temporal. KylGis Recursos conserva lecturas, ediciones y operaciones Git. No se cambian permisos de target/, servicios ni instalaciones activas.

La comprobación nativa de sólo lectura git -C <worktree> diff --check devuelve RC129 («No es un repositorio git») en el dispositivo fam para este worktree. El estado y diff de KylGis Recursos sí funcionan; staging y commit se mantienen en esa API con HEAD, rutas e índice esperados. No se modificó .git ni se forzó safe.directory.

El runner cerrado de unittest sólo admite seguridad/test_*.py y no permite este flujo Swing con JVM y MariaDB propios. El harness nuevo usa ventanas privadas y .doClick() en sus propios componentes; no envía entrada global al escritorio ni toca el POS activo.
