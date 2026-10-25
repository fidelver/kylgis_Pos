# Barrido complementario de literales de interfaz — POS 1.3

## Alcance

- Busca literales en APIs Swing de textos, tooltips, títulos, pestañas y bordes.
- No traduce rutas, protocolos, datos del usuario ni cadenas creadas dinámicamente.
- Un hallazgo puede ser técnico, plantilla HTML o texto real: requiere clasificación.
- Coincidencias: 199; mapeadas a catálogo nativo: 158; por revisar: 41.

## Tipos detectados

| API | Coincidencias |
|---|---:|
| caption | 81 |
| tooltip | 72 |
| new_label | 31 |
| tab | 11 |
| border | 4 |

## Pendientes por clase

### src/main/java/com/mx/kylgis/pos/inventory/ProductsEditor.java (10)
- [ ] L1629 — tooltip: A Service Item will not be deducted from the Inventory
- [ ] L1995 — tooltip: <html><center><h4>Inserts a Line Break<br> (a new line) for the button text
- [ ] L2006 — tooltip: <html><center><h4>Set the colour <br>for the button text
- [ ] L2016 — tooltip: <html><center><h4>Set the button<br> text to Large
- [ ] L2026 — tooltip: <html><center><h4>Set the button<br>text to Small
- [ ] L2036 — tooltip: <html><center><h4>Set the button<br> text to Italic
- [ ] L2046 — tooltip: <html><center><h4>Set the button<br> text to Italic
- [ ] L2056 — tooltip: <html><center><h4>Insert image from<br>local disk or internet URL
- [ ] L2076 — tooltip: <html><center><h4>Insert <style> tag to change<br>button background colour
- [ ] L2205 — caption: <?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>  \n

### src/main/java/com/mx/kylgis/pos/transfer/Transfer.java (5)
- [ ] L2289 — caption: localhost:
- [ ] L2294 — caption: localhost:
- [ ] L2295 — caption: ?zeroDateTimeBehavior=CONVERT_TO_NULL
- [ ] L2299 — caption: localhost:
- [ ] L2912 — caption: /kylgispos-database

### src/main/java/com/mx/kylgis/pos/config/JPanelConfigDatabase.java (4)
- [ ] L982 — caption: kylgispos
- [ ] L1129 — caption: kylgispos
- [ ] L1145 — caption: kylgispos1
- [ ] L1202 — caption: kylgispos

### src/main/java/com/mx/kylgis/pos/customers/CustomersView.java (3)
- [ ] L978 — caption: México
- [ ] L1000 — caption: Veracruz
- [ ] L1013 — caption: Veracruz

### src/main/java/com/mx/kylgis/pos/fiscal/SatTaxProfileDialog.java (2)
- [ ] L81 — tooltip: AAAA-MM-DD
- [ ] L82 — tooltip: AAAA-MM-DD

### src/main/java/com/mx/kylgis/pos/forms/JRootApp.java (2)
- [ ] L655 — caption: <html><center>
- [ ] L1622 — caption: <html><center>

### src/main/java/com/mx/kylgis/pos/reports/JPanelReport.java (2)
- [ ] L344 — tooltip: Hide/Show Filter
- [ ] L357 — tooltip: Execute Report

### src/main/java/com/mx/kylgis/pos/sales/JTicketsBagTicketBag.java (2)
- [ ] L84 — tooltip: Delete current Ticket
- [ ] L102 — tooltip: Cancel Action

### src/main/java/com/mx/kylgis/pos/sales/restaurant/JTicketsBagRestaurantMap.java (2)
- [ ] L918 — caption: <html><center>
- [ ] L929 — caption: <html><center>

### src/main/java/com/mx/kylgis/pos/catalog/JCatalog.java (1)
- [ ] L376 — caption: <html><div style='width:145px'>

### src/main/java/com/mx/kylgis/pos/config/JPanelConfigPlatforms.java (1)
- [ ] L62 — new_label: \u2713  

### src/main/java/com/mx/kylgis/pos/config/JPanelConfigSystem.java (1)
- [ ] L596 — caption: PIN

### src/main/java/com/mx/kylgis/pos/data/gui/JMessageDialog.java (1)
- [ ] L120 — caption: <html>

### src/main/java/com/mx/kylgis/pos/inventory/PriceImportPanel.java (1)
- [ ] L102 — caption: ScanPal

### src/main/java/com/mx/kylgis/pos/inventory/ProductsPanel.java (1)
- [ ] L99 — caption: ScanPal

### src/main/java/com/mx/kylgis/pos/inventory/StockManagement.java (1)
- [ ] L1336 — tooltip: Delete current Ticket

### src/main/java/com/mx/kylgis/pos/payment/PaymentPanelType.java (1)
- [ ] L197 — caption: MMYY

### src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java (1)
- [ ] L1404 — caption: x0.

