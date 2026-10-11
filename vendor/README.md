# Legacy UI build bridge

This directory is an explicit, temporary Maven file repository used only to keep the proven KylGis POS UI binary-compatible while the core moves to Java 21 and current database drivers.

Source of binaries: `tests/multiplataforma/pilot-pos-2fe6955-runtime/lib/`, the certified runtime with commit `2fe6955`.

SHA-256:
- `weblaf-1.2.9.jar`: `fe5ded9563d460f21a378e2e83aa869a4a890e489c0612b7b53f1788ae918bce`
- `weblaf-core-2.1.3.jar`: `a1c483ce8a513087616fb147cf895a566362b7be0cc22c641796d84543564fad`
- `weblaf-ui-2.1.3.jar`: `6bdfa04a5d187179c9be03f84fc411ebc662622235c16a8dadeaefe5afc312ff`
- `trident-1.4.jar`: `40513d203ffda4b73ead63f04b542108afa6e263d224ba5ed3f8ab10193f556e`
- `substance-7.1.00.jar`: `76ee5523552b8dee695e5c3be3002cf0123eabc9e58cfd289c9c4015fe2c632c`

Policy: these are not considered modernized dependencies. They are quarantined compatibility artifacts. UI dependency replacement must be handled as a separate regression-sensitive phase. Review upstream licensing before any public redistribution outside the established KylGis packaging flow.
