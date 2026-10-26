# KylGis POS 1.3: avance de verificaciones

## Aislamiento
Candidato de trabajo derivado del RC12 técnico, sin despliegue productivo.

## Implementaciones
- Descripciones específicas y localizadas en los 17 idiomas: 22 controles de operación, 16 de restaurante/venta y alrededor de 40 tooltips literales ingleses.
- Inventario nuevo: 953 controles Swing en 155 clases.
- Barrido de 205 literales: 160 con cobertura verificada, 45 pendientes (originalmente 93).
- 61 claves activas históricas traducidas y comprobadas en español y herencia es_MX/es_AR. Otros locales históricos siguen incompletos.
- Corrección de usuario de BD inicializado con texto HTML de tooltip. Usuario y contraseña ahora con ayudas distintas en 17 locales.
- Retiradas 3 dependencias WebLaF del formulario de base de datos: WebPopOver inactivo, WebSwitch reemplazado por JCheckBox y WebLabel reemplazado por JLabel; Java y formulario NetBeans sincronizados.
- El cambio de estado de la segunda BD continúa siendo funcional y el checkbox recibe una explicación localizada.
- Prueba de renderizado offscreen del panel compilado aisladamente: 25 imágenes en es-MX, fr, de, ar y en-US, cinco paneles; cero tooltips ausentes y cero avisos de truncamiento de JLabel en el muestreo. Log en /home/fam/POS/QA_Release13/rc13_c270800_20261019/gui_paint_pureswing.log.

## Verificaciones pendientes
- Construir nuevo JAR con la sustitución de WebLaF y repetir la regresión RC12, más 17 idiomas y muestra gráfica.
- Clasificar 45 literales de interfaz; los valores de protocolo, rutas e identificadores no son traducibles automáticamente.
- Revisar 59 identificadores genéricos restantes del inventario.
- Completar traducciones históricas del resto de idiomas y probar interfaz total de 155 clases, RTL, Windows/macOS y empaquetado nativo.

No se modificó 192.168.58.17 ni el lanzador POS productivo.
