# Auditoría de traducciones y exclusiones POS 1.3 (18/10/2026)

## Nueva infraestructura de tooltips y etiquetas localizadas

- **17 paquetes localizados** incluidos dentro de kylgispos.jar.
- **952 controles Swing declarados** en 155 clases; falta auditoría interactiva de controles dinámicos.
- **78 textos estáticos** con traducción específica para 17 locales; catálogos de sistema, campañas y acciones.
- **4 recursos HTML/de marca** localizados para los 17 locales y consultados internamente.
- **31 literales restantes**: principalmente protocolos, identificadores, marcado HTML, contenidos de prueba o valores regionales.

## Literales restantes: exclusión técnica o revisión individual

- [ ] ' - '
- [ ] '#000000'
- [ ] '***'
- [ ] '/kylgispos-database'
- [ ] '10:20'
- [ ] '<html>'
- [ ] '<html><center>'
- [ ] '<html><center>KylGis POS Punto de Venta Táctil<br>'
- [ ] "<html><div style='width:145px'>"
- [ ] '<html><table>'
- [ ] '<html>This is a test comment that shows how a long line is printed with this renderer.'
- [ ] '?zeroDateTimeBehavior=CONVERT_TO_NULL'
- [ ] 'MMYY'
- [ ] 'México'
- [ ] 'PIN'
- [ ] 'ScanPal'
- [ ] 'Veracruz'
- [ ] 'jCheckBox1'
- [ ] 'jCheckBox2'
- [ ] 'jCheckBox3'
- [ ] 'jCheckBox4'
- [ ] 'jCheckBox5'
- [ ] 'jlblErrorCode'
- [ ] 'jlblMessage'
- [ ] 'jline1'
- [ ] 'jline2'
- [ ] 'kylgispos'
- [ ] 'kylgispos1'
- [ ] 'localhost:'
- [ ] 'org.apache.derby.jdbc.EmbeddedDriver'
- [ ] 'x0.'

## Deuda histórica de los recursos originales

Los pos_messages_*.properties anteriores a esta función tienen cobertura desigual. La herencia por fallback no significa traducción nativa.

| Locale | Claves propias | Ausentes frente al base |
|---|---:|---:|
| al_SQ | 640 | 737 |
| ar | 461 | 912 |
| da | 986 | 391 |
| de | 897 | 493 |
| el | 530 | 856 |
| en_US | 1113 | 259 |
| es | 1263 | 112 |
| es_AR | 575 | 798 |
| es_MX | 643 | 730 |
| et | 475 | 898 |
| fr | 1073 | 299 |
| hr | 456 | 917 |
| it | 729 | 662 |
| nl | 1148 | 223 |
| pt | 432 | 941 |
| pt_BR | 378 | 995 |

**Compuerta de release:** no aprobar traducción nativa total solo por tener claves presentes en los paquetes nuevos.
Verificar textos reales panel por panel y completar progresivamente las traducciones heredadas; no inventar mensajes en idiomas desconocidos.

**Prueba de integridad completada:** 17 locales, 940 descripciones estructuradas, 0 cadenas vacías en los recursos nuevos; resistencia a sustitución por archivos locales verificada.


## Implementaciones posteriores

- Vocabulario y contextos curados en `tools/ui_help_vocabulary.py`, `tools/ui_help_domain_hints.py` y `tools/ui_help_domain_translations.py`.
- `JPanelConfigNodes`: `tools/ui_help_node_roles.py` y `tools/ui_help_node_form.py` con roles, etiquetas, ayudas de conexión e iconos de orden, 17 locales.
- Los conteos de textos pendientes del análisis estático original son históricos; regenerar los dos barridos antes de cierre final y clasificar explícitamente las excepciones técnicas.
