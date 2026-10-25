# Traducciones heredadas utilizadas — KylGis POS 1.3

Esta auditoría cuenta claves literales utilizadas por llamadas Java
AppLocal.getIntString y bundle.getString, no claves dinámicas o de terceros.
Las variantes regionales heredan primero de su lengua principal.

- Claves Java utilizadas y presentes en base: 867.
- Claves referenciadas sin valor en base: 22.
- Una entrada ausente produce fallback; no es traducción nativa revisada.

| Locale | Claves propias | Claves efectivas | Ausentes de la lengua | Usos faltantes |
|---|---:|---:|---:|---:|
| al_SQ | 640 | 640 | 483 | 700 |
| ar | 461 | 461 | 576 | 821 |
| da | 986 | 986 | 284 | 393 |
| de | 897 | 897 | 373 | 587 |
| el | 530 | 530 | 557 | 812 |
| en_US | 1113 | 1113 | 199 | 294 |
| es | 1324 | 1324 | 0 | 0 |
| es_AR | 575 | 1324 | 0 | 0 |
| es_MX | 643 | 1326 | 0 | 0 |
| et | 475 | 475 | 561 | 815 |
| fr | 1073 | 1073 | 232 | 335 |
| hr | 456 | 456 | 559 | 812 |
| it | 729 | 729 | 456 | 671 |
| nl | 1148 | 1148 | 192 | 283 |
| pt | 432 | 432 | 578 | 858 |
| pt_BR | 378 | 434 | 578 | 858 |

## Prioridades de revisión

### al_SQ — 483 entradas por completar
- [ ] message.uuidcopy (10 usos) — base: <html><center><h4>UUID has been copied to your clipboard — src/main/java/com/mx/kylgis/pos/admin/PeopleView.java
- [ ] label.supplier (7 usos) — base: Supplier — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] message.cannotexecute (7 usos) — base: Unable to execute action — src/main/java/com/mx/kylgis/pos/inventory/CategoriesEditor.java
- [ ] button.executefilter (6 usos) — base: Execute — src/main/java/com/mx/kylgis/pos/admin/JPeopleFinder.java
- [ ] label.namem (6 usos) — base: <html>Name<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/admin/RolesView.java
- [ ] message.ampersand (6 usos) — base: <html><center>Ampersand(&) character not allowed!<br>Please replace with another char — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] tooltip.search.reset (6 usos) — base: Clear search and return to name (Alt+L) — src/main/java/com/mx/kylgis/pos/ticket/ProductFilter.java
- [ ] dialog.productsuppliers.title (5 usos) — base: Product suppliers — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.printerstatusOn (5 usos) — base: Printer ON — src/main/java/com/mx/kylgis/pos/payment/JPaymentSelect.java
- [ ] label.searchkeym (5 usos) — base: <html>Search Key<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] recipe.component.quantity (5 usos) — base: Quantity — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] Button.Test (4 usos) — base: CONNECT — src/main/java/com/mx/kylgis/pos/config/JPanelConfigDatabase.java
- [ ] label.printerstatusOff (4 usos) — base: Printer OFF — src/main/java/com/mx/kylgis/pos/payment/JPaymentSelect.java
- [ ] menu.productsuppliers.new (4 usos) — base: New supplier... — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] message.transfermessage (4 usos) — base: Database Transfer — src/main/java/com/mx/kylgis/pos/transfer/Transfer.java
- [ ] recipe.component.product (4 usos) — base: Component product — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.component.uom (4 usos) — base: UOM — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] tooltip.config.general.text (4 usos) — base: <html><center>Personalise the Main screen by adding your own text — src/main/java/com/mx/kylgis/pos/config/JPanelConfigGeneral.java
- [ ] tooltip.prodhtmldisplayColourChooser (4 usos) — base: <html><center><h4>Right click to select colour for<br> text using Colour button — src/main/java/com/mx/kylgis/pos/config/JPanelConfigSystem.java
- [ ] tooltip.search.execute (4 usos) — base: Search products (Enter) — src/main/java/com/mx/kylgis/pos/panels/JProductFinder.java
- [ ] Button.Cancel (3 usos) — base:  — src/main/java/com/mx/kylgis/pos/customers/JDialogNewCustomer.java
- [ ] Button.OK (3 usos) — base:  — src/main/java/com/mx/kylgis/pos/customers/JDialogNewCustomer.java
- [ ] button.Cancel (3 usos) — base:  — src/main/java/com/mx/kylgis/pos/admin/JPeopleFinder.java
- [ ] label.deleteline (3 usos) — base: Clear Order Check — src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java
- [ ] label.discount (3 usos) — base: Discount — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java

### ar — 576 entradas por completar
- [ ] label.supplier (7 usos) — base: Supplier — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.namem (6 usos) — base: <html>Name<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/admin/RolesView.java
- [ ] message.ampersand (6 usos) — base: <html><center>Ampersand(&) character not allowed!<br>Please replace with another char — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] tooltip.search.reset (6 usos) — base: Clear search and return to name (Alt+L) — src/main/java/com/mx/kylgis/pos/ticket/ProductFilter.java
- [ ] button.exit (5 usos) — base: Exit — src/main/java/com/mx/kylgis/pos/imports/JPanelCSVCleardb.java
- [ ] database.UnableToConnect (5 usos) — base: Unable to connect with database! Check database setting in configuration — src/main/java/com/mx/kylgis/pos/sales/JPanelResetPickupId.java
- [ ] dialog.productsuppliers.title (5 usos) — base: Product suppliers — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.epm.employee (5 usos) — base: Employee — src/main/java/com/mx/kylgis/pos/epm/BreaksView.java
- [ ] label.printerstatusOn (5 usos) — base: Printer ON — src/main/java/com/mx/kylgis/pos/payment/JPaymentSelect.java
- [ ] label.searchkeym (5 usos) — base: <html>Search Key<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] recipe.component.quantity (5 usos) — base: Quantity — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] Button.Test (4 usos) — base: CONNECT — src/main/java/com/mx/kylgis/pos/config/JPanelConfigDatabase.java
- [ ] label.printerstatusOff (4 usos) — base: Printer OFF — src/main/java/com/mx/kylgis/pos/payment/JPaymentSelect.java
- [ ] menu.productsuppliers.new (4 usos) — base: New supplier... — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] message.notexiststickettitle (4 usos) — base: Receipt Message — src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java
- [ ] message.transfermessage (4 usos) — base: Database Transfer — src/main/java/com/mx/kylgis/pos/transfer/Transfer.java
- [ ] recipe.component.product (4 usos) — base: Component product — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.component.uom (4 usos) — base: UOM — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] tooltip.config.general.text (4 usos) — base: <html><center>Personalise the Main screen by adding your own text — src/main/java/com/mx/kylgis/pos/config/JPanelConfigGeneral.java
- [ ] tooltip.prodhtmldisplayColourChooser (4 usos) — base: <html><center><h4>Right click to select colour for<br> text using Colour button — src/main/java/com/mx/kylgis/pos/config/JPanelConfigSystem.java
- [ ] tooltip.search.execute (4 usos) — base: Search products (Enter) — src/main/java/com/mx/kylgis/pos/panels/JProductFinder.java
- [ ] Button.OK (3 usos) — base:  — src/main/java/com/mx/kylgis/pos/customers/JDialogNewCustomer.java
- [ ] button.Cancel (3 usos) — base:  — src/main/java/com/mx/kylgis/pos/admin/JPeopleFinder.java
- [ ] label.CatalogueStatusNo (3 usos) — base: Not In Catalogue — src/main/java/com/mx/kylgis/pos/inventory/CategoriesEditor.java
- [ ] label.csvdelimit (3 usos) — base: Separator — src/main/java/com/mx/kylgis/pos/imports/CustomerCSVImport.java

### da — 284 entradas por completar
- [ ] message.ampersand (6 usos) — base: <html><center>Ampersand(&) character not allowed!<br>Please replace with another char — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] tooltip.search.reset (6 usos) — base: Clear search and return to name (Alt+L) — src/main/java/com/mx/kylgis/pos/ticket/ProductFilter.java
- [ ] dialog.productsuppliers.title (5 usos) — base: Product suppliers — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] recipe.component.quantity (5 usos) — base: Quantity — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] menu.productsuppliers.new (4 usos) — base: New supplier... — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] message.transfermessage (4 usos) — base: Database Transfer — src/main/java/com/mx/kylgis/pos/transfer/Transfer.java
- [ ] recipe.component.product (4 usos) — base: Component product — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.component.uom (4 usos) — base: UOM — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] tooltip.search.execute (4 usos) — base: Search products (Enter) — src/main/java/com/mx/kylgis/pos/panels/JProductFinder.java
- [ ] message.override.badpin (3 usos) — base: <html><center><h4>PIN Number Incorrect — src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java
- [ ] recipe.component.waste (3 usos) — base: Planning waste ratio — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.production.location (3 usos) — base: Location — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeProductionPanel.java
- [ ] recipe.reload (3 usos) — base: Reload — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.yield.quantity (3 usos) — base: Yield quantity — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] title.override.enterpin (3 usos) — base: Enter PIN Number — src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java
- [ ] title.voucheredit.authorization (3 usos) — base: Voucher edit authorization — src/main/java/com/mx/kylgis/pos/voucher/VoucherEditAuthorization.java
- [ ] tooltip.search.results (3 usos) — base: Search results. Use Up/Down to navigate and Enter to select. — src/main/java/com/mx/kylgis/pos/panels/JProductFinder.java
- [ ] Menu.RecipeProduction (2 usos) — base: Recipe production — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeProductionPanel.java
- [ ] Menu.Recipes (2 usos) — base: Recipes — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] Menu.TransformationHistory (2 usos) — base: Transformation history — src/main/java/com/mx/kylgis/pos/inventory/transform/TransformationHistoryPanel.java
- [ ] button.connect (2 usos) — base: CONNECT — src/main/java/com/mx/kylgis/pos/config/JPanelConfigDatabase.java
- [ ] button.guest (2 usos) — base: Guests : — src/main/java/com/mx/kylgis/pos/sales/restaurant/JTicketsBagRestaurantMap.java
- [ ] button.priceupdate (2 usos) — base: Update — src/main/java/com/mx/kylgis/pos/sales/JProductLineEdit.java
- [ ] label.DBName (2 usos) — base: Database — src/main/java/com/mx/kylgis/pos/config/JPanelConfigDatabase.java
- [ ] label.DbOptions (2 usos) — base: Options — src/main/java/com/mx/kylgis/pos/config/JPanelConfigDatabase.java

### de — 373 entradas por completar
- [ ] label.prodbarcode (18 usos) — base: Barcode — src/main/java/com/mx/kylgis/pos/forms/DataLogicSales.java
- [ ] button.OK (17 usos) — base:  — src/main/java/com/mx/kylgis/pos/admin/JPeopleFinder.java
- [ ] label.prodcategory (12 usos) — base: Category — src/main/java/com/mx/kylgis/pos/forms/DataLogicSales.java
- [ ] label.prodref (11 usos) — base: Reference — src/main/java/com/mx/kylgis/pos/forms/DataLogicSales.java
- [ ] label.prodpricebuy (9 usos) — base: Buy Price — src/main/java/com/mx/kylgis/pos/forms/DataLogicSales.java
- [ ] label.namem (6 usos) — base: <html>Name<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/admin/RolesView.java
- [ ] message.ampersand (6 usos) — base: <html><center>Ampersand(&) character not allowed!<br>Please replace with another char — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] tooltip.search.reset (6 usos) — base: Clear search and return to name (Alt+L) — src/main/java/com/mx/kylgis/pos/ticket/ProductFilter.java
- [ ] dialog.productsuppliers.title (5 usos) — base: Product suppliers — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.StartDate (5 usos) — base: Start Date — src/main/java/com/mx/kylgis/pos/epm/DataLogicPresenceManagement.java
- [ ] label.searchkeym (5 usos) — base: <html>Search Key<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] recipe.component.quantity (5 usos) — base: Quantity — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] menu.productsuppliers.new (4 usos) — base: New supplier... — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] recipe.component.product (4 usos) — base: Component product — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.component.uom (4 usos) — base: UOM — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] tooltip.config.general.text (4 usos) — base: <html><center>Personalise the Main screen by adding your own text — src/main/java/com/mx/kylgis/pos/config/JPanelConfigGeneral.java
- [ ] tooltip.prodhtmldisplayColourChooser (4 usos) — base: <html><center><h4>Right click to select colour for<br> text using Colour button — src/main/java/com/mx/kylgis/pos/config/JPanelConfigSystem.java
- [ ] tooltip.search.execute (4 usos) — base: Search products (Enter) — src/main/java/com/mx/kylgis/pos/panels/JProductFinder.java
- [ ] Button.Cancel (3 usos) — base:  — src/main/java/com/mx/kylgis/pos/customers/JDialogNewCustomer.java
- [ ] button.Cancel (3 usos) — base:  — src/main/java/com/mx/kylgis/pos/admin/JPeopleFinder.java
- [ ] label.deleteline (3 usos) — base: Clear Order Check — src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java
- [ ] message.databasechoose (3 usos) — base: <html><h4>Please select a KylGis POS database to use for this session — src/main/java/com/mx/kylgis/pos/forms/AppViewConnection.java
- [ ] message.override.badpin (3 usos) — base: <html><center><h4>PIN Number Incorrect — src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java
- [ ] message.sharedticket (3 usos) — base: <html><center><h3>There are tickets in the stack</h3><br>Unfortunately you're not all — src/main/java/com/mx/kylgis/pos/sales/restaurant/JTicketsBagRestaurantMap.java
- [ ] message.sharedtickettitle (3 usos) — base: Shared Ticket List — src/main/java/com/mx/kylgis/pos/sales/shared/JTicketsBagShared.java

### el — 557 entradas por completar
- [ ] button.OK (17 usos) — base:  — src/main/java/com/mx/kylgis/pos/admin/JPeopleFinder.java
- [ ] label.supplier (7 usos) — base: Supplier — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.namem (6 usos) — base: <html>Name<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/admin/RolesView.java
- [ ] message.ampersand (6 usos) — base: <html><center>Ampersand(&) character not allowed!<br>Please replace with another char — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] tooltip.search.reset (6 usos) — base: Clear search and return to name (Alt+L) — src/main/java/com/mx/kylgis/pos/ticket/ProductFilter.java
- [ ] button.exit (5 usos) — base: Exit — src/main/java/com/mx/kylgis/pos/imports/JPanelCSVCleardb.java
- [ ] database.UnableToConnect (5 usos) — base: Unable to connect with database! Check database setting in configuration — src/main/java/com/mx/kylgis/pos/sales/JPanelResetPickupId.java
- [ ] dialog.productsuppliers.title (5 usos) — base: Product suppliers — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.StartDate (5 usos) — base: Start Date — src/main/java/com/mx/kylgis/pos/epm/DataLogicPresenceManagement.java
- [ ] label.printerstatusOn (5 usos) — base: Printer ON — src/main/java/com/mx/kylgis/pos/payment/JPaymentSelect.java
- [ ] label.searchkeym (5 usos) — base: <html>Search Key<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] recipe.component.quantity (5 usos) — base: Quantity — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] Button.Test (4 usos) — base: CONNECT — src/main/java/com/mx/kylgis/pos/config/JPanelConfigDatabase.java
- [ ] label.printerstatusOff (4 usos) — base: Printer OFF — src/main/java/com/mx/kylgis/pos/payment/JPaymentSelect.java
- [ ] menu.productsuppliers.new (4 usos) — base: New supplier... — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] message.notexiststickettitle (4 usos) — base: Receipt Message — src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java
- [ ] message.transfermessage (4 usos) — base: Database Transfer — src/main/java/com/mx/kylgis/pos/transfer/Transfer.java
- [ ] recipe.component.product (4 usos) — base: Component product — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.component.uom (4 usos) — base: UOM — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] tooltip.config.general.text (4 usos) — base: <html><center>Personalise the Main screen by adding your own text — src/main/java/com/mx/kylgis/pos/config/JPanelConfigGeneral.java
- [ ] tooltip.prodhtmldisplayColourChooser (4 usos) — base: <html><center><h4>Right click to select colour for<br> text using Colour button — src/main/java/com/mx/kylgis/pos/config/JPanelConfigSystem.java
- [ ] tooltip.search.execute (4 usos) — base: Search products (Enter) — src/main/java/com/mx/kylgis/pos/panels/JProductFinder.java
- [ ] button.Cancel (3 usos) — base:  — src/main/java/com/mx/kylgis/pos/admin/JPeopleFinder.java
- [ ] label.CatalogueStatusNo (3 usos) — base: Not In Catalogue — src/main/java/com/mx/kylgis/pos/inventory/CategoriesEditor.java
- [ ] label.csvdelimit (3 usos) — base: Separator — src/main/java/com/mx/kylgis/pos/imports/CustomerCSVImport.java

### en_US — 199 entradas por completar
- [ ] message.ampersand (6 usos) — base: <html><center>Ampersand(&) character not allowed!<br>Please replace with another char — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] tooltip.search.reset (6 usos) — base: Clear search and return to name (Alt+L) — src/main/java/com/mx/kylgis/pos/ticket/ProductFilter.java
- [ ] dialog.productsuppliers.title (5 usos) — base: Product suppliers — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] recipe.component.quantity (5 usos) — base: Quantity — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] menu.productsuppliers.new (4 usos) — base: New supplier... — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] recipe.component.product (4 usos) — base: Component product — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.component.uom (4 usos) — base: UOM — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] tooltip.search.execute (4 usos) — base: Search products (Enter) — src/main/java/com/mx/kylgis/pos/panels/JProductFinder.java
- [ ] recipe.component.waste (3 usos) — base: Planning waste ratio — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.production.location (3 usos) — base: Location — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeProductionPanel.java
- [ ] recipe.reload (3 usos) — base: Reload — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.yield.quantity (3 usos) — base: Yield quantity — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] title.voucheredit.authorization (3 usos) — base: Voucher edit authorization — src/main/java/com/mx/kylgis/pos/voucher/VoucherEditAuthorization.java
- [ ] tooltip.search.results (3 usos) — base: Search results. Use Up/Down to navigate and Enter to select. — src/main/java/com/mx/kylgis/pos/panels/JProductFinder.java
- [ ] Menu.RecipeProduction (2 usos) — base: Recipe production — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeProductionPanel.java
- [ ] Menu.Recipes (2 usos) — base: Recipes — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] Menu.TransformationHistory (2 usos) — base: Transformation history — src/main/java/com/mx/kylgis/pos/inventory/transform/TransformationHistoryPanel.java
- [ ] button.connect (2 usos) — base: CONNECT — src/main/java/com/mx/kylgis/pos/config/JPanelConfigDatabase.java
- [ ] button.guest (2 usos) — base: Guests : — src/main/java/com/mx/kylgis/pos/sales/restaurant/JTicketsBagRestaurantMap.java
- [ ] label.DbOptions (2 usos) — base: Options — src/main/java/com/mx/kylgis/pos/config/JPanelConfigDatabase.java
- [ ] label.prodcodeorref (2 usos) — base: Code / reference — src/main/java/com/mx/kylgis/pos/ticket/ProductFilter.java
- [ ] label.productsupplier.associated (2 usos) — base: Linked — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.productsupplier.preferred (2 usos) — base: Preferred — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.seats (2 usos) — base: Seats — src/main/java/com/mx/kylgis/pos/mant/JPanelPlaces.java
- [ ] menu.productsuppliers.manage (2 usos) — base: Manage product suppliers... — src/main/java/com/mx/kylgis/pos/inventory/ProductsEditor.java

### es — 0 entradas por completar

### es_AR — 0 entradas por completar

### es_MX — 0 entradas por completar

### et — 561 entradas por completar
- [ ] message.uuidcopy (10 usos) — base: <html><center><h4>UUID has been copied to your clipboard — src/main/java/com/mx/kylgis/pos/admin/PeopleView.java
- [ ] label.supplier (7 usos) — base: Supplier — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.namem (6 usos) — base: <html>Name<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/admin/RolesView.java
- [ ] message.ampersand (6 usos) — base: <html><center>Ampersand(&) character not allowed!<br>Please replace with another char — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] tooltip.search.reset (6 usos) — base: Clear search and return to name (Alt+L) — src/main/java/com/mx/kylgis/pos/ticket/ProductFilter.java
- [ ] button.exit (5 usos) — base: Exit — src/main/java/com/mx/kylgis/pos/imports/JPanelCSVCleardb.java
- [ ] database.UnableToConnect (5 usos) — base: Unable to connect with database! Check database setting in configuration — src/main/java/com/mx/kylgis/pos/sales/JPanelResetPickupId.java
- [ ] dialog.productsuppliers.title (5 usos) — base: Product suppliers — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.epm.employee (5 usos) — base: Employee — src/main/java/com/mx/kylgis/pos/epm/BreaksView.java
- [ ] label.printerstatusOn (5 usos) — base: Printer ON — src/main/java/com/mx/kylgis/pos/payment/JPaymentSelect.java
- [ ] label.searchkeym (5 usos) — base: <html>Search Key<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] recipe.component.quantity (5 usos) — base: Quantity — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] Button.Test (4 usos) — base: CONNECT — src/main/java/com/mx/kylgis/pos/config/JPanelConfigDatabase.java
- [ ] label.printerstatusOff (4 usos) — base: Printer OFF — src/main/java/com/mx/kylgis/pos/payment/JPaymentSelect.java
- [ ] menu.productsuppliers.new (4 usos) — base: New supplier... — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] message.notexiststickettitle (4 usos) — base: Receipt Message — src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java
- [ ] message.transfermessage (4 usos) — base: Database Transfer — src/main/java/com/mx/kylgis/pos/transfer/Transfer.java
- [ ] recipe.component.product (4 usos) — base: Component product — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.component.uom (4 usos) — base: UOM — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] tooltip.config.general.text (4 usos) — base: <html><center>Personalise the Main screen by adding your own text — src/main/java/com/mx/kylgis/pos/config/JPanelConfigGeneral.java
- [ ] tooltip.prodhtmldisplayColourChooser (4 usos) — base: <html><center><h4>Right click to select colour for<br> text using Colour button — src/main/java/com/mx/kylgis/pos/config/JPanelConfigSystem.java
- [ ] tooltip.search.execute (4 usos) — base: Search products (Enter) — src/main/java/com/mx/kylgis/pos/panels/JProductFinder.java
- [ ] Button.OK (3 usos) — base:  — src/main/java/com/mx/kylgis/pos/customers/JDialogNewCustomer.java
- [ ] button.Cancel (3 usos) — base:  — src/main/java/com/mx/kylgis/pos/admin/JPeopleFinder.java
- [ ] label.CatalogueStatusNo (3 usos) — base: Not In Catalogue — src/main/java/com/mx/kylgis/pos/inventory/CategoriesEditor.java

### fr — 232 entradas por completar
- [ ] message.ampersand (6 usos) — base: <html><center>Ampersand(&) character not allowed!<br>Please replace with another char — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] tooltip.search.reset (6 usos) — base: Clear search and return to name (Alt+L) — src/main/java/com/mx/kylgis/pos/ticket/ProductFilter.java
- [ ] dialog.productsuppliers.title (5 usos) — base: Product suppliers — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] recipe.component.quantity (5 usos) — base: Quantity — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] menu.productsuppliers.new (4 usos) — base: New supplier... — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] recipe.component.product (4 usos) — base: Component product — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.component.uom (4 usos) — base: UOM — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] tooltip.search.execute (4 usos) — base: Search products (Enter) — src/main/java/com/mx/kylgis/pos/panels/JProductFinder.java
- [ ] Button.OK (3 usos) — base:  — src/main/java/com/mx/kylgis/pos/customers/JDialogNewCustomer.java
- [ ] message.override.badpin (3 usos) — base: <html><center><h4>PIN Number Incorrect — src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java
- [ ] recipe.component.waste (3 usos) — base: Planning waste ratio — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.production.location (3 usos) — base: Location — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeProductionPanel.java
- [ ] recipe.reload (3 usos) — base: Reload — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.yield.quantity (3 usos) — base: Yield quantity — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] title.override.enterpin (3 usos) — base: Enter PIN Number — src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java
- [ ] title.voucheredit.authorization (3 usos) — base: Voucher edit authorization — src/main/java/com/mx/kylgis/pos/voucher/VoucherEditAuthorization.java
- [ ] tooltip.search.results (3 usos) — base: Search results. Use Up/Down to navigate and Enter to select. — src/main/java/com/mx/kylgis/pos/panels/JProductFinder.java
- [ ] Menu.RecipeProduction (2 usos) — base: Recipe production — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeProductionPanel.java
- [ ] Menu.Recipes (2 usos) — base: Recipes — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] Menu.TransformationHistory (2 usos) — base: Transformation history — src/main/java/com/mx/kylgis/pos/inventory/transform/TransformationHistoryPanel.java
- [ ] button.connect (2 usos) — base: CONNECT — src/main/java/com/mx/kylgis/pos/config/JPanelConfigDatabase.java
- [ ] button.guest (2 usos) — base: Guests : — src/main/java/com/mx/kylgis/pos/sales/restaurant/JTicketsBagRestaurantMap.java
- [ ] label.DBName (2 usos) — base: Database — src/main/java/com/mx/kylgis/pos/config/JPanelConfigDatabase.java
- [ ] label.DbOptions (2 usos) — base: Options — src/main/java/com/mx/kylgis/pos/config/JPanelConfigDatabase.java
- [ ] label.prodcodeorref (2 usos) — base: Code / reference — src/main/java/com/mx/kylgis/pos/ticket/ProductFilter.java

### hr — 559 entradas por completar
- [ ] message.uuidcopy (10 usos) — base: <html><center><h4>UUID has been copied to your clipboard — src/main/java/com/mx/kylgis/pos/admin/PeopleView.java
- [ ] label.supplier (7 usos) — base: Supplier — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.namem (6 usos) — base: <html>Name<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/admin/RolesView.java
- [ ] message.ampersand (6 usos) — base: <html><center>Ampersand(&) character not allowed!<br>Please replace with another char — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] tooltip.search.reset (6 usos) — base: Clear search and return to name (Alt+L) — src/main/java/com/mx/kylgis/pos/ticket/ProductFilter.java
- [ ] button.exit (5 usos) — base: Exit — src/main/java/com/mx/kylgis/pos/imports/JPanelCSVCleardb.java
- [ ] database.UnableToConnect (5 usos) — base: Unable to connect with database! Check database setting in configuration — src/main/java/com/mx/kylgis/pos/sales/JPanelResetPickupId.java
- [ ] dialog.productsuppliers.title (5 usos) — base: Product suppliers — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.epm.employee (5 usos) — base: Employee — src/main/java/com/mx/kylgis/pos/epm/BreaksView.java
- [ ] label.printerstatusOn (5 usos) — base: Printer ON — src/main/java/com/mx/kylgis/pos/payment/JPaymentSelect.java
- [ ] label.searchkeym (5 usos) — base: <html>Search Key<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] recipe.component.quantity (5 usos) — base: Quantity — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] Button.Test (4 usos) — base: CONNECT — src/main/java/com/mx/kylgis/pos/config/JPanelConfigDatabase.java
- [ ] label.printerstatusOff (4 usos) — base: Printer OFF — src/main/java/com/mx/kylgis/pos/payment/JPaymentSelect.java
- [ ] menu.productsuppliers.new (4 usos) — base: New supplier... — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] message.notexiststickettitle (4 usos) — base: Receipt Message — src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java
- [ ] message.transfermessage (4 usos) — base: Database Transfer — src/main/java/com/mx/kylgis/pos/transfer/Transfer.java
- [ ] recipe.component.product (4 usos) — base: Component product — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.component.uom (4 usos) — base: UOM — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] tooltip.config.general.text (4 usos) — base: <html><center>Personalise the Main screen by adding your own text — src/main/java/com/mx/kylgis/pos/config/JPanelConfigGeneral.java
- [ ] tooltip.prodhtmldisplayColourChooser (4 usos) — base: <html><center><h4>Right click to select colour for<br> text using Colour button — src/main/java/com/mx/kylgis/pos/config/JPanelConfigSystem.java
- [ ] tooltip.search.execute (4 usos) — base: Search products (Enter) — src/main/java/com/mx/kylgis/pos/panels/JProductFinder.java
- [ ] Button.OK (3 usos) — base:  — src/main/java/com/mx/kylgis/pos/customers/JDialogNewCustomer.java
- [ ] button.Cancel (3 usos) — base:  — src/main/java/com/mx/kylgis/pos/admin/JPeopleFinder.java
- [ ] label.CatalogueStatusNo (3 usos) — base: Not In Catalogue — src/main/java/com/mx/kylgis/pos/inventory/CategoriesEditor.java

### it — 456 entradas por completar
- [ ] button.OK (17 usos) — base:  — src/main/java/com/mx/kylgis/pos/admin/JPeopleFinder.java
- [ ] label.supplier (7 usos) — base: Supplier — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.namem (6 usos) — base: <html>Name<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/admin/RolesView.java
- [ ] message.ampersand (6 usos) — base: <html><center>Ampersand(&) character not allowed!<br>Please replace with another char — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] tooltip.search.reset (6 usos) — base: Clear search and return to name (Alt+L) — src/main/java/com/mx/kylgis/pos/ticket/ProductFilter.java
- [ ] button.exit (5 usos) — base: Exit — src/main/java/com/mx/kylgis/pos/imports/JPanelCSVCleardb.java
- [ ] dialog.productsuppliers.title (5 usos) — base: Product suppliers — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.StartDate (5 usos) — base: Start Date — src/main/java/com/mx/kylgis/pos/epm/DataLogicPresenceManagement.java
- [ ] label.printerstatusOn (5 usos) — base: Printer ON — src/main/java/com/mx/kylgis/pos/payment/JPaymentSelect.java
- [ ] label.searchkeym (5 usos) — base: <html>Search Key<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] recipe.component.quantity (5 usos) — base: Quantity — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] label.printerstatusOff (4 usos) — base: Printer OFF — src/main/java/com/mx/kylgis/pos/payment/JPaymentSelect.java
- [ ] menu.productsuppliers.new (4 usos) — base: New supplier... — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] message.transfermessage (4 usos) — base: Database Transfer — src/main/java/com/mx/kylgis/pos/transfer/Transfer.java
- [ ] recipe.component.product (4 usos) — base: Component product — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.component.uom (4 usos) — base: UOM — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] tooltip.config.general.text (4 usos) — base: <html><center>Personalise the Main screen by adding your own text — src/main/java/com/mx/kylgis/pos/config/JPanelConfigGeneral.java
- [ ] tooltip.prodhtmldisplayColourChooser (4 usos) — base: <html><center><h4>Right click to select colour for<br> text using Colour button — src/main/java/com/mx/kylgis/pos/config/JPanelConfigSystem.java
- [ ] tooltip.search.execute (4 usos) — base: Search products (Enter) — src/main/java/com/mx/kylgis/pos/panels/JProductFinder.java
- [ ] button.Cancel (3 usos) — base:  — src/main/java/com/mx/kylgis/pos/admin/JPeopleFinder.java
- [ ] label.deleteline (3 usos) — base: Clear Order Check — src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java
- [ ] label.discount (3 usos) — base: Discount — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] label.ibutton (3 usos) — base: iButton — src/main/java/com/mx/kylgis/pos/admin/PeopleView.java
- [ ] label.prodsupplier (3 usos) — base: Supplier — src/main/java/com/mx/kylgis/pos/forms/DataLogicSales.java
- [ ] label.tktheader1 (3 usos) — base: Header — src/main/java/com/mx/kylgis/pos/config/JPanelConfigCompany.java

### nl — 192 entradas por completar
- [ ] message.ampersand (6 usos) — base: <html><center>Ampersand(&) character not allowed!<br>Please replace with another char — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] tooltip.search.reset (6 usos) — base: Clear search and return to name (Alt+L) — src/main/java/com/mx/kylgis/pos/ticket/ProductFilter.java
- [ ] dialog.productsuppliers.title (5 usos) — base: Product suppliers — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] recipe.component.quantity (5 usos) — base: Quantity — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] menu.productsuppliers.new (4 usos) — base: New supplier... — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] recipe.component.product (4 usos) — base: Component product — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.component.uom (4 usos) — base: UOM — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] tooltip.search.execute (4 usos) — base: Search products (Enter) — src/main/java/com/mx/kylgis/pos/panels/JProductFinder.java
- [ ] recipe.component.waste (3 usos) — base: Planning waste ratio — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.production.location (3 usos) — base: Location — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeProductionPanel.java
- [ ] recipe.reload (3 usos) — base: Reload — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.yield.quantity (3 usos) — base: Yield quantity — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] title.voucheredit.authorization (3 usos) — base: Voucher edit authorization — src/main/java/com/mx/kylgis/pos/voucher/VoucherEditAuthorization.java
- [ ] tooltip.search.results (3 usos) — base: Search results. Use Up/Down to navigate and Enter to select. — src/main/java/com/mx/kylgis/pos/panels/JProductFinder.java
- [ ] Menu.RecipeProduction (2 usos) — base: Recipe production — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeProductionPanel.java
- [ ] Menu.Recipes (2 usos) — base: Recipes — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] Menu.TransformationHistory (2 usos) — base: Transformation history — src/main/java/com/mx/kylgis/pos/inventory/transform/TransformationHistoryPanel.java
- [ ] button.connect (2 usos) — base: CONNECT — src/main/java/com/mx/kylgis/pos/config/JPanelConfigDatabase.java
- [ ] button.guest (2 usos) — base: Guests : — src/main/java/com/mx/kylgis/pos/sales/restaurant/JTicketsBagRestaurantMap.java
- [ ] label.prodcodeorref (2 usos) — base: Code / reference — src/main/java/com/mx/kylgis/pos/ticket/ProductFilter.java
- [ ] label.productsupplier.associated (2 usos) — base: Linked — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.productsupplier.preferred (2 usos) — base: Preferred — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] menu.productsuppliers.manage (2 usos) — base: Manage product suppliers... — src/main/java/com/mx/kylgis/pos/inventory/ProductsEditor.java
- [ ] message.productnotscale (2 usos) — base: This product is not configured for sale by weight. — src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java
- [ ] message.productsuppliers.concurrentchange (2 usos) — base: The supplier list changed while this window was open. Reload it before saving so no s — src/main/java/com/mx/kylgis/pos/suppliers/DataLogicSuppliers.java

### pt — 578 entradas por completar
- [ ] message.cannotfindattributes (10 usos) — base: Attributes for this Product have not been set — src/main/java/com/mx/kylgis/pos/inventory/StockDiaryEditor.java
- [ ] message.uuidcopy (10 usos) — base: <html><center><h4>UUID has been copied to your clipboard — src/main/java/com/mx/kylgis/pos/admin/PeopleView.java
- [ ] label.supplier (7 usos) — base: Supplier — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.javapos.drawer (6 usos) — base: Drawer — src/main/java/com/mx/kylgis/pos/config/JPanelConfigPeripheral.java
- [ ] label.javapos.printer (6 usos) — base: Printer — src/main/java/com/mx/kylgis/pos/config/JPanelConfigPeripheral.java
- [ ] label.namem (6 usos) — base: <html>Name<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/admin/RolesView.java
- [ ] message.ampersand (6 usos) — base: <html><center>Ampersand(&) character not allowed!<br>Please replace with another char — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] tooltip.search.reset (6 usos) — base: Clear search and return to name (Alt+L) — src/main/java/com/mx/kylgis/pos/ticket/ProductFilter.java
- [ ] button.exit (5 usos) — base: Exit — src/main/java/com/mx/kylgis/pos/imports/JPanelCSVCleardb.java
- [ ] database.UnableToConnect (5 usos) — base: Unable to connect with database! Check database setting in configuration — src/main/java/com/mx/kylgis/pos/sales/JPanelResetPickupId.java
- [ ] dialog.productsuppliers.title (5 usos) — base: Product suppliers — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.epm.employee (5 usos) — base: Employee — src/main/java/com/mx/kylgis/pos/epm/BreaksView.java
- [ ] label.printerstatusOn (5 usos) — base: Printer ON — src/main/java/com/mx/kylgis/pos/payment/JPaymentSelect.java
- [ ] label.searchkeym (5 usos) — base: <html>Search Key<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] recipe.component.quantity (5 usos) — base: Quantity — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] Button.Test (4 usos) — base: CONNECT — src/main/java/com/mx/kylgis/pos/config/JPanelConfigDatabase.java
- [ ] label.printerstatusOff (4 usos) — base: Printer OFF — src/main/java/com/mx/kylgis/pos/payment/JPaymentSelect.java
- [ ] menu.productsuppliers.new (4 usos) — base: New supplier... — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] message.notexiststickettitle (4 usos) — base: Receipt Message — src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java
- [ ] message.transfermessage (4 usos) — base: Database Transfer — src/main/java/com/mx/kylgis/pos/transfer/Transfer.java
- [ ] recipe.component.product (4 usos) — base: Component product — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.component.uom (4 usos) — base: UOM — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] tooltip.config.general.text (4 usos) — base: <html><center>Personalise the Main screen by adding your own text — src/main/java/com/mx/kylgis/pos/config/JPanelConfigGeneral.java
- [ ] tooltip.prodhtmldisplayColourChooser (4 usos) — base: <html><center><h4>Right click to select colour for<br> text using Colour button — src/main/java/com/mx/kylgis/pos/config/JPanelConfigSystem.java
- [ ] tooltip.search.execute (4 usos) — base: Search products (Enter) — src/main/java/com/mx/kylgis/pos/panels/JProductFinder.java

### pt_BR — 578 entradas por completar
- [ ] message.cannotfindattributes (10 usos) — base: Attributes for this Product have not been set — src/main/java/com/mx/kylgis/pos/inventory/StockDiaryEditor.java
- [ ] message.uuidcopy (10 usos) — base: <html><center><h4>UUID has been copied to your clipboard — src/main/java/com/mx/kylgis/pos/admin/PeopleView.java
- [ ] label.supplier (7 usos) — base: Supplier — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.javapos.drawer (6 usos) — base: Drawer — src/main/java/com/mx/kylgis/pos/config/JPanelConfigPeripheral.java
- [ ] label.javapos.printer (6 usos) — base: Printer — src/main/java/com/mx/kylgis/pos/config/JPanelConfigPeripheral.java
- [ ] label.namem (6 usos) — base: <html>Name<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/admin/RolesView.java
- [ ] message.ampersand (6 usos) — base: <html><center>Ampersand(&) character not allowed!<br>Please replace with another char — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] tooltip.search.reset (6 usos) — base: Clear search and return to name (Alt+L) — src/main/java/com/mx/kylgis/pos/ticket/ProductFilter.java
- [ ] button.exit (5 usos) — base: Exit — src/main/java/com/mx/kylgis/pos/imports/JPanelCSVCleardb.java
- [ ] database.UnableToConnect (5 usos) — base: Unable to connect with database! Check database setting in configuration — src/main/java/com/mx/kylgis/pos/sales/JPanelResetPickupId.java
- [ ] dialog.productsuppliers.title (5 usos) — base: Product suppliers — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] label.epm.employee (5 usos) — base: Employee — src/main/java/com/mx/kylgis/pos/epm/BreaksView.java
- [ ] label.printerstatusOn (5 usos) — base: Printer ON — src/main/java/com/mx/kylgis/pos/payment/JPaymentSelect.java
- [ ] label.searchkeym (5 usos) — base: <html>Search Key<font color=FB00FF> * — src/main/java/com/mx/kylgis/pos/customers/CustomersView.java
- [ ] recipe.component.quantity (5 usos) — base: Quantity — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] Button.Test (4 usos) — base: CONNECT — src/main/java/com/mx/kylgis/pos/config/JPanelConfigDatabase.java
- [ ] label.printerstatusOff (4 usos) — base: Printer OFF — src/main/java/com/mx/kylgis/pos/payment/JPaymentSelect.java
- [ ] menu.productsuppliers.new (4 usos) — base: New supplier... — src/main/java/com/mx/kylgis/pos/inventory/JDialogProductSuppliers.java
- [ ] message.notexiststickettitle (4 usos) — base: Receipt Message — src/main/java/com/mx/kylgis/pos/sales/JPanelTicket.java
- [ ] message.transfermessage (4 usos) — base: Database Transfer — src/main/java/com/mx/kylgis/pos/transfer/Transfer.java
- [ ] recipe.component.product (4 usos) — base: Component product — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] recipe.component.uom (4 usos) — base: UOM — src/main/java/com/mx/kylgis/pos/inventory/transform/RecipeManagementPanel.java
- [ ] tooltip.config.general.text (4 usos) — base: <html><center>Personalise the Main screen by adding your own text — src/main/java/com/mx/kylgis/pos/config/JPanelConfigGeneral.java
- [ ] tooltip.prodhtmldisplayColourChooser (4 usos) — base: <html><center><h4>Right click to select colour for<br> text using Colour button — src/main/java/com/mx/kylgis/pos/config/JPanelConfigSystem.java
- [ ] tooltip.search.execute (4 usos) — base: Search products (Enter) — src/main/java/com/mx/kylgis/pos/panels/JProductFinder.java

## Claves Java sin equivalente base

- Check — src/main/java/com/mx/kylgis/pos/voucher/VoucherEditor.java
- Label.Name — src/main/java/com/mx/kylgis/pos/config/JPanelConfigPeripheral.java
- Menu.StockImport — src/main/java/com/mx/kylgis/pos/inventory/PriceImportPanel.java
- Nope — src/main/java/com/mx/kylgis/pos/panels/JPanelCloseMoneyReprint.java
- db.ip — src/main/java/com/mx/kylgis/pos/forms/JRootApp.java
- label.erpid — src/main/java/com/mx/kylgis/pos/config/JPanelConfigERP.java
- label.erporg — src/main/java/com/mx/kylgis/pos/config/JPanelConfigERP.java
- label.erppassword — src/main/java/com/mx/kylgis/pos/config/JPanelConfigERP.java
- label.erppos — src/main/java/com/mx/kylgis/pos/config/JPanelConfigERP.java
- label.erpurl — src/main/java/com/mx/kylgis/pos/config/JPanelConfigERP.java
- label.erpuser — src/main/java/com/mx/kylgis/pos/config/JPanelConfigERP.java
- label.status — src/main/java/com/mx/kylgis/pos/voucher/VoucherPanel.java
- label.voucherCustomer — src/main/java/com/mx/kylgis/pos/voucher/VoucherEditor.java
- message.attsetnotexists — src/main/java/com/mx/kylgis/pos/sales/JProductAttEdit.java
- message.emptystatus — src/main/java/com/mx/kylgis/pos/voucher/VoucherEditor.java
- message.message.emptynumber — src/main/java/com/mx/kylgis/pos/voucher/VoucherEditor.java
- message.nosave — src/main/java/com/mx/kylgis/pos/customers/CustomersPayment.java
- message.reprintticket — src/main/java/com/mx/kylgis/pos/sales/shared/JTicketsBagShared.java
- message.reprinttickettitle — src/main/java/com/mx/kylgis/pos/sales/shared/JTicketsBagShared.java
- message.suppliernotfound — src/main/java/com/mx/kylgis/pos/suppliers/JSupplierFinder.java
- message.title.supplierinvalid — src/main/java/com/mx/kylgis/pos/inventory/StockManagement.java
- transpayment. — src/main/java/com/mx/kylgis/pos/panels/JPanelCloseMoney.java

## Compuerta de certificación

No declarar la localización integral terminada sin completar las ausencias,
evaluar los textos dinámicos y realizar inspecciones GUI en los 17 locales.
La ayuda sellada dentro del JAR se certifica por separado.

