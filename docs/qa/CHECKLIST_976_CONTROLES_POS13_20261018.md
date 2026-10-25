# Checklist de tooltips localizados — KylGis POS 1.3

- Inventario derivado del código: **976 controles** en **159 clases/paneles**.
- Incluye etiquetas Swing con manejador de clic, además de botones, campos, checks, combos, tablas, pestañas y otros controles.
- **46 controles** presentan una identidad fuente genérica o ambigua: su descripción se debe confirmar en la GUI y sustituir por un significado concreto.
- Cada casilla se marca solo tras comprobar el tooltip visible y la traducción en los locales que existen, sin sustituir nombres de productos, códigos ni datos introducidos.

## Validación funcional por panel

- [ ] Todos los controles están presentes; probar estados deshabilitado/habilitado.
- [ ] Tooltip describe la acción o el dato esperado, con unidades o riesgos cuando proceda.
- [ ] El texto de botones, etiquetas, avisos, errores y títulos está localizado.
- [ ] Textos correctos al cambiar locale y abrir nuevamente el panel.
- [ ] Iconos clicables, campos creados dinámicamente y cuadros modales cubiertos.
- [ ] Ningún tooltip depende de archivos externos al JAR.
- [ ] No se altera el contenido introducido por el usuario al localizar la interfaz.

## Inventario estático completo

### `com.mx.kylgis.pos.admin.JPeopleFinder` (5 elementos)
- [ ] `jButton1` — `JButton` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jButton3` — `JButton` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jListPeople` — `JList` — list people
- [ ] `jcmdCancel` — `JButton` — cancel
- [ ] `jcmdOK` — `JButton` — ok

### `com.mx.kylgis.pos.admin.PeopleView` (6 elementos)
- [ ] `jButton1` — `JButton` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jLabel1` — `JLabel` — setting **REVISIÓN SEMÁNTICA**
- [ ] `m_jName` — `JTextField` — name (user)
- [ ] `m_jRole` — `JComboBox` — role
- [ ] `m_jVisible` — `JCheckBox` — visible
- [ ] `m_jcard` — `JTextField` — card

### `com.mx.kylgis.pos.admin.ResourcesView` (4 elementos)
- [ ] `m_jBtnReadScript` — `JButton` — read script
- [ ] `m_jName` — `JTextField` — name (resource)
- [ ] `m_jText` — `JTextArea` — text (resource)
- [ ] `m_jType` — `JComboBox` — type

### `com.mx.kylgis.pos.admin.RolesView` (2 elementos)
- [ ] `m_jName` — `JTextField` — name (role)
- [ ] `m_jText` — `JTextArea` — text (role)

### `com.mx.kylgis.pos.beans.JCalendarDialog` (2 elementos)
- [ ] `jcmdCancel` — `JButton` — cancel
- [ ] `jcmdOK` — `JButton` — ok

### `com.mx.kylgis.pos.beans.JGuestsPop` (1 elementos)
- [ ] `jcmdOK` — `JButton` — ok

### `com.mx.kylgis.pos.beans.JNumberDialog` (2 elementos)
- [ ] `jcmdCancel` — `JButton` — cancel
- [ ] `jcmdOK` — `JButton` — ok

### `com.mx.kylgis.pos.beans.JNumberKeys` (16 elementos)
- [ ] `m_jCE` — `JButton` — ce
- [ ] `m_jEquals` — `JButton` — equals
- [ ] `m_jKey0` — `JButton` — key
- [ ] `m_jKey1` — `JButton` — key
- [ ] `m_jKey2` — `JButton` — key
- [ ] `m_jKey3` — `JButton` — key
- [ ] `m_jKey4` — `JButton` — key
- [ ] `m_jKey5` — `JButton` — key
- [ ] `m_jKey6` — `JButton` — key
- [ ] `m_jKey7` — `JButton` — key
- [ ] `m_jKey8` — `JButton` — key
- [ ] `m_jKey9` — `JButton` — key
- [ ] `m_jKeyDot` — `JButton` — key dot
- [ ] `m_jMinus` — `JButton` — minus
- [ ] `m_jMultiply` — `JButton` — multiply
- [ ] `m_jPlus` — `JButton` — plus

### `com.mx.kylgis.pos.beans.JNumberPop` (1 elementos)
- [ ] `jcmdOK` — `JButton` — ok

### `com.mx.kylgis.pos.beans.JPasswordDialog` (2 elementos)
- [ ] `jcmdCancel` — `JButton` — cancel
- [ ] `jcmdOK` — `JButton` — ok

### `com.mx.kylgis.pos.catalog.JCatalog` (2 elementos)
- [ ] `m_btnBack1` — `JButton` — m_btn back
- [ ] `m_jListCategories` — `JList` — list categories

### `com.mx.kylgis.pos.catalog.JCatalogTab` (1 elementos)
- [ ] `btn` — `JButton` — btn

### `com.mx.kylgis.pos.catalog.JProductsSelector` (1 elementos)
- [ ] `btn` — `JButton` — btn

### `com.mx.kylgis.pos.config.JPanelConfigCompany` (12 elementos)
- [ ] `jtxtTktFooter1` — `JTextField` — tkt footer
- [ ] `jtxtTktFooter2` — `JTextField` — tkt footer
- [ ] `jtxtTktFooter3` — `JTextField` — tkt footer
- [ ] `jtxtTktFooter4` — `JTextField` — tkt footer
- [ ] `jtxtTktFooter5` — `JTextField` — tkt footer
- [ ] `jtxtTktFooter6` — `JTextField` — tkt footer
- [ ] `jtxtTktHeader1` — `JTextField` — tkt header
- [ ] `jtxtTktHeader2` — `JTextField` — tkt header
- [ ] `jtxtTktHeader3` — `JTextField` — tkt header
- [ ] `jtxtTktHeader4` — `JTextField` — tkt header
- [ ] `jtxtTktHeader5` — `JTextField` — tkt header
- [ ] `jtxtTktHeader6` — `JTextField` — tkt header

### `com.mx.kylgis.pos.config.JPanelConfigDatabase` (26 elementos)
- [ ] `jCBSchema` — `JComboBox` — cb schema
- [ ] `jCBSchema1` — `JComboBox` — cb schema
- [ ] `jbtnConnect` — `JButton` — connect
- [ ] `jbtnConnect1` — `JButton` — connect
- [ ] `jbtnCreateDB` — `JButton` — create db
- [ ] `jbtnDbDriverLib` — `JButton` — db driver lib
- [ ] `jbtnReset` — `JButton` — reset
- [ ] `jbtnReset1` — `JButton` — reset
- [ ] `jbtnSetDB` — `JButton` — set db
- [ ] `jbtnSetDB1` — `JButton` — set db
- [ ] `jcboDBDriver` — `JComboBox` — db driver
- [ ] `jtxtDbDriver` — `JTextField` — db driver
- [ ] `jtxtDbDriverLib` — `JTextField` — db driver lib
- [ ] `jtxtDbName` — `JTextField` — db name
- [ ] `jtxtDbName1` — `JTextField` — db name
- [ ] `jtxtDbOptions` — `JTextField` — db options
- [ ] `jtxtDbOptions1` — `JTextField` — db options
- [ ] `jtxtDbPassword` — `JPasswordField` — db password
- [ ] `jtxtDbPassword1` — `JPasswordField` — db password
- [ ] `jtxtDbSchema` — `JTextField` — db schema
- [ ] `jtxtDbSchema1` — `JTextField` — db schema
- [ ] `jtxtDbURL` — `JTextField` — db url
- [ ] `jtxtDbURL1` — `JTextField` — db url
- [ ] `jtxtDbUser` — `JTextField` — db user
- [ ] `jtxtDbUser1` — `JTextField` — db user
- [ ] `multiDB` — `JCheckBox` — multi db

### `com.mx.kylgis.pos.config.JPanelConfigERP` (6 elementos)
- [ ] `jTextField1` — `JTextField` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jTextField2` — `JTextField` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jtxtId` — `JTextField` — id
- [ ] `jtxtName` — `JTextField` — name **REVISIÓN SEMÁNTICA**
- [ ] `jtxtPassword` — `JPasswordField` — password
- [ ] `jtxtUrl` — `JTextField` — url

### `com.mx.kylgis.pos.config.JPanelConfigGeneral` (15 elementos)
- [ ] `jLblURL` — `JLabel` — url
- [ ] `jbtnClearHTML` — `JButton` — clear html
- [ ] `jbtnHTML` — `JButton` — html
- [ ] `jbtnLogo` — `JButton` — logo
- [ ] `jbtnText` — `JButton` — text **REVISIÓN SEMÁNTICA**
- [ ] `jbtnTextClear` — `JButton` — text clear
- [ ] `jcboLAF` — `JComboBox` — laf
- [ ] `jcboMachineScreenmode` — `JComboBox` — machine screenmode
- [ ] `jcboTicketsBag` — `JComboBox` — tickets bag
- [ ] `jchkHideInfo` — `JCheckBox` — hide info
- [ ] `jtxtMachineDepartment` — `JTextField` — machine department
- [ ] `jtxtMachineHostname` — `JTextField` — machine hostname
- [ ] `jtxtStartupHTML` — `JTextField` — startup html
- [ ] `jtxtStartupLogo` — `JTextField` — startup logo
- [ ] `jtxtStartupText` — `JTextField` — startup text

### `com.mx.kylgis.pos.config.JPanelConfigLocale` (8 elementos)
- [ ] `jcboCurrency` — `JComboBox` — currency
- [ ] `jcboDate` — `JComboBox` — date
- [ ] `jcboDatetime` — `JComboBox` — datetime
- [ ] `jcboDouble` — `JComboBox` — double
- [ ] `jcboInteger` — `JComboBox` — integer
- [ ] `jcboLocale` — `JComboBox` — locale
- [ ] `jcboPercent` — `JComboBox` — percent
- [ ] `jcboTime` — `JComboBox` — time

### `com.mx.kylgis.pos.config.JPanelConfigNodes` (15 elementos)
- [ ] `addModuleButton` — `JButton` — add module button
- [ ] `check` — `JCheckBox` — check **REVISIÓN SEMÁNTICA**
- [ ] `dbName` — `JTextField` — db name
- [ ] `dbPort` — `JTextField` — db port
- [ ] `dbServer` — `JTextField` — db server
- [ ] `moduleCatalog` — `JComboBox` — module catalog
- [ ] `moduleDownButton` — `JButton` — module down button
- [ ] `moduleStackList` — `JList` — module stack list
- [ ] `moduleUpButton` — `JButton` — module up button
- [ ] `nodeSelector` — `JComboBox` — node selector
- [ ] `profile` — `JTextField` — profile
- [ ] `provisionButton` — `JButton` — provision button
- [ ] `reloadButton` — `JButton` — reload button
- [ ] `removeModuleButton` — `JButton` — remove module button
- [ ] `saveButton` — `JButton` — save button

### `com.mx.kylgis.pos.config.JPanelConfigPayment` (3 elementos)
- [ ] `jcboCardReader` — `JComboBox` — card reader
- [ ] `jcboPaymentGateway` — `JComboBox` — payment gateway
- [ ] `jchkPaymentTest` — `JCheckBox` — payment test

### `com.mx.kylgis.pos.config.JPanelConfigPeripheral` (44 elementos)
- [ ] `cboPrinters` — `JComboBox` — printers
- [ ] `digitalMirrorBrowse` — `JButton` — digital mirror browse
- [ ] `digitalMirrorCheck` — `JCheckBox` — digital mirror check
- [ ] `digitalMirrorPath` — `JTextField` — digital mirror path
- [ ] `jcboConnDisplay` — `JComboBox` — conn display
- [ ] `jcboConnPrinter` — `JComboBox` — conn printer
- [ ] `jcboConnPrinter2` — `JComboBox` — conn printer
- [ ] `jcboConnPrinter3` — `JComboBox` — conn printer
- [ ] `jcboConnPrinter4` — `JComboBox` — conn printer
- [ ] `jcboConnPrinter5` — `JComboBox` — conn printer
- [ ] `jcboConnPrinter6` — `JComboBox` — conn printer
- [ ] `jcboMachineDisplay` — `JComboBox` — machine display
- [ ] `jcboMachinePrinter` — `JComboBox` — machine printer
- [ ] `jcboMachinePrinter2` — `JComboBox` — machine printer
- [ ] `jcboMachinePrinter3` — `JComboBox` — machine printer
- [ ] `jcboMachinePrinter4` — `JComboBox` — machine printer
- [ ] `jcboMachinePrinter5` — `JComboBox` — machine printer
- [ ] `jcboMachinePrinter6` — `JComboBox` — machine printer
- [ ] `jcboMachineScale` — `JComboBox` — machine scale
- [ ] `jcboMachineScanner` — `JComboBox` — machine scanner
- [ ] `jcboSerialDisplay` — `JComboBox` — serial display
- [ ] `jcboSerialPrinter` — `JComboBox` — serial printer
- [ ] `jcboSerialPrinter2` — `JComboBox` — serial printer
- [ ] `jcboSerialPrinter3` — `JComboBox` — serial printer
- [ ] `jcboSerialPrinter4` — `JComboBox` — serial printer
- [ ] `jcboSerialPrinter5` — `JComboBox` — serial printer
- [ ] `jcboSerialPrinter6` — `JComboBox` — serial printer
- [ ] `jcboSerialScale` — `JComboBox` — serial scale
- [ ] `jcboSerialScanner` — `JComboBox` — serial scanner
- [ ] `m_jtxtJPOSDrawer` — `JTextField` — jpos drawer
- [ ] `m_jtxtJPOSDrawer2` — `JTextField` — jpos drawer
- [ ] `m_jtxtJPOSDrawer3` — `JTextField` — jpos drawer
- [ ] `m_jtxtJPOSDrawer4` — `JTextField` — jpos drawer
- [ ] `m_jtxtJPOSDrawer5` — `JTextField` — jpos drawer
- [ ] `m_jtxtJPOSDrawer6` — `JTextField` — jpos drawer
- [ ] `m_jtxtJPOSName` — `JTextField` — jpos name
- [ ] `m_jtxtJPOSPrinter` — `JTextField` — jpos printer
- [ ] `m_jtxtJPOSPrinter2` — `JTextField` — jpos printer
- [ ] `m_jtxtJPOSPrinter3` — `JTextField` — jpos printer
- [ ] `m_jtxtJPOSPrinter4` — `JTextField` — jpos printer
- [ ] `m_jtxtJPOSPrinter5` — `JTextField` — jpos printer
- [ ] `m_jtxtJPOSPrinter6` — `JTextField` — jpos printer
- [ ] `moreConfigButton` — `JButton` — more config button
- [ ] `screenMirrorCheck` — `JCheckBox` — screen mirror check

### `com.mx.kylgis.pos.config.JPanelConfigPlatforms` (4 elementos)
- [ ] `add` — `JButton` — add
- [ ] `customPlatforms` — `JList` — custom platforms
- [ ] `newPlatform` — `JTextField` — new platform
- [ ] `remove` — `JButton` — remove

### `com.mx.kylgis.pos.config.JPanelConfigSalesButtons` (6 elementos)
- [ ] `platformOrder` — `JCheckBox` — platform order
- [ ] `remotePrint` — `JCheckBox` — remote print
- [ ] `reprint` — `JCheckBox` — reprint
- [ ] `scale` — `JCheckBox` — scale
- [ ] `split` — `JCheckBox` — split
- [ ] `ticketPreview` — `JCheckBox` — ticket preview

### `com.mx.kylgis.pos.config.JPanelConfigSystem` (22 elementos)
- [ ] `jCheckPrice00` — `JCheckBox` — price
- [ ] `jCloseCashbtn` — `JCheckBox` — close cashbtn
- [ ] `jMaxInstances` — `JSpinner` — max instances
- [ ] `jMoveAMountBoxToTop` — `JCheckBox` — move a mount box to top
- [ ] `jTaxIncluded` — `JCheckBox` — tax included
- [ ] `jTextAutoLogoffTime` — `JTextField` — text auto logoff time
- [ ] `jTextSCRate` — `JTextField` — text sc rate
- [ ] `jTxtautoRefreshTimer` — `JTextField` — auto refresh timer
- [ ] `jchkAutoLogoff` — `JCheckBox` — auto logoff
- [ ] `jchkAutoLogoffToTables` — `JCheckBox` — auto logoff to tables
- [ ] `jchkBarcodetype` — `JCheckBox` — barcodetype
- [ ] `jchkInstance` — `JCheckBox` — instance
- [ ] `jchkOverride` — `JCheckBox` — override
- [ ] `jchkPriceUpdate` — `JCheckBox` — price update
- [ ] `jchkSCOnOff` — `JCheckBox` — sc on off
- [ ] `jchkSCRestaurant` — `JCheckBox` — sc restaurant
- [ ] `jchkShowCustomerDetails` — `JCheckBox` — show customer details
- [ ] `jchkShowWaiterDetails` — `JCheckBox` — show waiter details
- [ ] `jchkTextOverlay` — `JCheckBox` — text overlay
- [ ] `jchkTransBtn` — `JCheckBox` — trans btn
- [ ] `jchkautoRefreshTableMap` — `JCheckBox` — auto refresh table map
- [ ] `jtxtPIN` — `JTextField` — pin

### `com.mx.kylgis.pos.config.JPanelConfiguration` (5 elementos)
- [ ] `jTabbedPane1` — `JTabbedPane` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jbtnApplyPeripherals` — `JButton` — apply peripherals
- [ ] `jbtnExit` — `JButton` — exit
- [ ] `jbtnRestore` — `JButton` — restore
- [ ] `jbtnSave` — `JButton` — save

### `com.mx.kylgis.pos.config.JPanelTicketSetup` (13 elementos)
- [ ] `digitalMirrorBrowse` — `JButton` — digital mirror browse
- [ ] `digitalMirrorCheck` — `JCheckBox` — digital mirror check
- [ ] `digitalMirrorPath` — `JTextField` — digital mirror path
- [ ] `jPickupSize` — `JSpinner` — pickup size
- [ ] `jReceiptSize` — `JSpinner` — receipt size
- [ ] `jTextReceiptPrefix` — `JTextField` — text receipt prefix
- [ ] `jTicketExample` — `JTextField` — ticket example
- [ ] `jTicketWidth` — `JSpinner` — ticket width
- [ ] `jbtnReset` — `JButton` — reset
- [ ] `jcboTicketPaper` — `JComboBox` — ticket paper
- [ ] `m_jReceiptPrintOff` — `JCheckBox` — receipt print off
- [ ] `receiptPrinterRoute` — `JComboBox` — receipt printer route
- [ ] `screenMirrorCheck` — `JCheckBox` — screen mirror check

### `com.mx.kylgis.pos.config.ParametersPrinter` (2 elementos)
- [ ] `jPrinters` — `JComboBox` — printers
- [ ] `jReceiptPrinter` — `JCheckBox` — receipt printer

### `com.mx.kylgis.pos.customers.CustomersPayment` (11 elementos)
- [ ] `btnCustomer` — `JButton` — customer
- [ ] `btnPay` — `JButton` — pay
- [ ] `btnPrePay` — `JButton` — pre pay
- [ ] `btnSave` — `JButton` — save
- [ ] `jButton1` — `JButton` — setting **REVISIÓN SEMÁNTICA**
- [ ] `txtCard` — `JTextField` — card
- [ ] `txtCurdate` — `JTextField` — curdate
- [ ] `txtCurdebt` — `JTextField` — curdebt
- [ ] `txtMaxdebt` — `JTextField` — maxdebt
- [ ] `txtName` — `JTextField` — name (customer)
- [ ] `txtTaxId` — `JTextField` — tax id

### `com.mx.kylgis.pos.customers.CustomersView` (32 elementos)
- [ ] `jBtnClearCard` — `JButton` — clear card
- [ ] `jBtnCreateCard` — `JButton` — create card
- [ ] `jBtnShowTrans` — `JButton` — show trans
- [ ] `jLabel7` — `JLabel` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jTabbedPane1` — `JTabbedPane` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jTableCustomerTransactions` — `JTable` — table customer transactions
- [ ] `jcard` — `JTextField` — card
- [ ] `m_jCategory` — `JComboBox` — category
- [ ] `m_jName` — `JTextField` — name (customer)
- [ ] `m_jNotes` — `JTextArea` — notes
- [ ] `m_jSearchkey` — `JTextField` — searchkey
- [ ] `m_jTaxID` — `JTextField` — tax id
- [ ] `m_jVip` — `JCheckBox` — vip
- [ ] `m_jVisible` — `JCheckBox` — visible
- [ ] `m_jbtndate` — `JButton` — date
- [ ] `m_jdate` — `JTextField` — date
- [ ] `txtAddress` — `JTextField` — address
- [ ] `txtAddress2` — `JTextField` — address
- [ ] `txtCity` — `JTextField` — city
- [ ] `txtCountry` — `JTextField` — country
- [ ] `txtCurdate` — `JTextField` — curdate
- [ ] `txtCurdebt` — `JTextField` — curdebt
- [ ] `txtDiscount` — `JTextField` — discount
- [ ] `txtEmail` — `JTextField` — email
- [ ] `txtFax` — `JTextField` — fax
- [ ] `txtFirstName` — `JTextField` — first name
- [ ] `txtLastName` — `JTextField` — last name
- [ ] `txtMaxdebt` — `JTextField` — maxdebt
- [ ] `txtPhone` — `JTextField` — phone
- [ ] `txtPhone2` — `JTextField` — phone
- [ ] `txtPostal` — `JTextField` — postal
- [ ] `txtRegion` — `JTextField` — region

### `com.mx.kylgis.pos.customers.JCustomerFinder` (5 elementos)
- [ ] `jListCustomers` — `JList` — list customers
- [ ] `jbtnExecute` — `JButton` — execute
- [ ] `jbtnReset` — `JButton` — reset
- [ ] `jcmdCancel` — `JButton` — cancel
- [ ] `jcmdOK` — `JButton` — ok

### `com.mx.kylgis.pos.customers.JDialogNewCustomer` (14 elementos)
- [ ] `m_jBtnCancel` — `JButton` — cancel
- [ ] `m_jBtnOK` — `JButton` — ok
- [ ] `m_jCategory` — `JComboBox` — category
- [ ] `m_jName` — `JTextField` — name (customer)
- [ ] `m_jSearchkey` — `JTextField` — searchkey
- [ ] `m_jTaxID` — `JTextField` — tax id
- [ ] `m_jVip` — `JCheckBox` — vip
- [ ] `txtDiscount` — `JTextField` — discount
- [ ] `txtEmail` — `JTextField` — email
- [ ] `txtFirstName` — `JTextField` — first name
- [ ] `txtLastName` — `JTextField` — last name
- [ ] `txtMaxdebt` — `JTextField` — maxdebt
- [ ] `txtPhone` — `JTextField` — phone
- [ ] `txtPhone2` — `JTextField` — phone

### `com.mx.kylgis.pos.data.gui.JFind` (6 elementos)
- [ ] `jcmdCancel` — `JButton` — cancel
- [ ] `jcmdOK` — `JButton` — ok
- [ ] `m_jFind` — `JTextField` — find
- [ ] `m_jMatch` — `JComboBox` — match
- [ ] `m_jMatchCase` — `JCheckBox` — match case
- [ ] `m_jWhere` — `JComboBox` — where

### `com.mx.kylgis.pos.data.gui.JImageEditor` (4 elementos)
- [ ] `m_jbtnclose` — `JButton` — close
- [ ] `m_jbtnopen` — `JButton` — open
- [ ] `m_jbtnzoomin` — `JButton` — zoomin
- [ ] `m_jbtnzoomout` — `JButton` — zoomout

### `com.mx.kylgis.pos.data.gui.JImageViewerCustomer` (2 elementos)
- [ ] `m_jbtnzoomin` — `JButton` — zoomin
- [ ] `m_jbtnzoomout` — `JButton` — zoomout

### `com.mx.kylgis.pos.data.gui.JImageViewerProduct` (2 elementos)
- [ ] `m_jbtnzoomin` — `JButton` — zoomin
- [ ] `m_jbtnzoomout` — `JButton` — zoomout

### `com.mx.kylgis.pos.data.gui.JListData` (3 elementos)
- [ ] `m_jCancel` — `JButton` — cancel
- [ ] `m_jData` — `JList` — data
- [ ] `m_jOK` — `JButton` — ok

### `com.mx.kylgis.pos.data.gui.JListNavigator` (1 elementos)
- [ ] `m_jlist` — `JList` — list **REVISIÓN SEMÁNTICA**

### `com.mx.kylgis.pos.data.gui.JMessageDialog` (3 elementos)
- [ ] `jcmdMore` — `JButton` — more
- [ ] `jcmdOK` — `JButton` — ok
- [ ] `jtxtException` — `JTextArea` — exception

### `com.mx.kylgis.pos.data.gui.JNavigator` (8 elementos)
- [ ] `jbtnFind` — `JButton` — find
- [ ] `jbtnFirst` — `JButton` — first
- [ ] `jbtnLast` — `JButton` — last
- [ ] `jbtnNext` — `JButton` — next
- [ ] `jbtnPrev` — `JButton` — prev
- [ ] `jbtnRefresh` — `JButton` — refresh
- [ ] `jbtnReload` — `JButton` — reload
- [ ] `jbtnSort` — `JButton` — sort

### `com.mx.kylgis.pos.data.gui.JSaver` (3 elementos)
- [ ] `jbtnDelete` — `JButton` — delete
- [ ] `jbtnNew` — `JButton` — new
- [ ] `jbtnSave` — `JButton` — save

### `com.mx.kylgis.pos.data.gui.JSort` (5 elementos)
- [ ] `jcmdCancel` — `JButton` — cancel
- [ ] `jcmdOK` — `JButton` — ok
- [ ] `m_jSort1` — `JComboBox` — sort
- [ ] `m_jSort2` — `JComboBox` — sort
- [ ] `m_jSort3` — `JComboBox` — sort

### `com.mx.kylgis.pos.editor.JEditorAbstract` (1 elementos)
- [ ] `m_jText` — `JButton` — text **REVISIÓN SEMÁNTICA**

### `com.mx.kylgis.pos.editor.JEditorKeys` (14 elementos)
- [ ] `m_jCE` — `JButton` — ce
- [ ] `m_jKey0` — `JButton` — key
- [ ] `m_jKey1` — `JButton` — key
- [ ] `m_jKey2` — `JButton` — key
- [ ] `m_jKey3` — `JButton` — key
- [ ] `m_jKey4` — `JButton` — key
- [ ] `m_jKey5` — `JButton` — key
- [ ] `m_jKey6` — `JButton` — key
- [ ] `m_jKey7` — `JButton` — key
- [ ] `m_jKey8` — `JButton` — key
- [ ] `m_jKey9` — `JButton` — key
- [ ] `m_jKeyDot` — `JButton` — key dot
- [ ] `m_jMinus` — `JButton` — minus
- [ ] `m_txtKeys` — `JTextField` — m_txt keys

### `com.mx.kylgis.pos.epm.BreaksView` (3 elementos)
- [ ] `m_jBreakDescription` — `JTextArea` — break description
- [ ] `m_jBreakName` — `JTextField` — break name
- [ ] `m_jVisible` — `JCheckBox` — visible

### `com.mx.kylgis.pos.epm.JEmployeeFinder` (5 elementos)
- [ ] `jButton1` — `JButton` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jButton3` — `JButton` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jListEmployees` — `JList` — list employees
- [ ] `jcmdCancel` — `JButton` — cancel
- [ ] `jcmdOK` — `JButton` — ok

### `com.mx.kylgis.pos.epm.JPanelEmployeePresence` (3 elementos)
- [ ] `btn` — `JButton` — btn
- [ ] `btnCheckIn` — `JButton` — check in
- [ ] `btnCheckOut` — `JButton` — check out

### `com.mx.kylgis.pos.epm.LeavesView` (7 elementos)
- [ ] `btnEmployee` — `JButton` — employee
- [ ] `btnEndDate` — `JButton` — end date
- [ ] `btnStartDate` — `JButton` — start date
- [ ] `m_jEmployeeName` — `JTextField` — employee name
- [ ] `m_jEndDate` — `JTextField` — end date
- [ ] `m_jLeaveNote` — `JTextArea` — leave note
- [ ] `m_jStartDate` — `JTextField` — start date

### `com.mx.kylgis.pos.fiscal.SatProductMetadataDialog` (10 elementos)
- [ ] `cancel` — `JButton` — cancel
- [ ] `claveProd` — `JTextField` — clave prod
- [ ] `claveUnidad` — `JTextField` — clave unidad
- [ ] `cuotaFactor` — `JTextField` — cuota factor
- [ ] `cuotaUnidad` — `JTextField` — cuota unidad
- [ ] `find` — `JButton` — find
- [ ] `objeto` — `JTextField` — objeto
- [ ] `results` — `JComboBox` — results
- [ ] `save` — `JButton` — save
- [ ] `search` — `JTextField` — search

### `com.mx.kylgis.pos.fiscal.SatTaxProfileDialog` (10 elementos)
- [ ] `cancel` — `JButton` — cancel
- [ ] `enabled` — `JCheckBox` — enabled
- [ ] `factor` — `JComboBox` — factor
- [ ] `from` — `JTextField` — from
- [ ] `impuesto` — `JComboBox` — impuesto
- [ ] `nature` — `JComboBox` — nature
- [ ] `rate` — `JTextField` — rate
- [ ] `save` — `JButton` — save
- [ ] `unit` — `JTextField` — unit
- [ ] `until` — `JTextField` — until

### `com.mx.kylgis.pos.forms.JDlgChangePassword` (5 elementos)
- [ ] `jcmdCancel` — `JButton` — cancel
- [ ] `jcmdOK` — `JButton` — ok
- [ ] `jtxtPasswordNew` — `JPasswordField` — password new
- [ ] `jtxtPasswordOld` — `JPasswordField` — password old
- [ ] `jtxtPasswordRepeat` — `JPasswordField` — password repeat

### `com.mx.kylgis.pos.forms.JPanelNull` (1 elementos)
- [ ] `jtxtException` — `JTextArea` — exception

### `com.mx.kylgis.pos.forms.JPrincipalApp` (1 elementos)
- [ ] `jButton1` — `JButton` — setting **REVISIÓN SEMÁNTICA**

### `com.mx.kylgis.pos.forms.JRootApp` (4 elementos)
- [ ] `btn` — `JButton` — btn
- [ ] `m_healthRefreshButton` — `JButton` — m_health refresh button
- [ ] `m_jClose` — `JButton` — close
- [ ] `m_txtKeys` — `JTextField` — m_txt keys

### `com.mx.kylgis.pos.forms.MenuItemDefinition` (1 elementos)
- [ ] `btn` — `JButton` — btn

### `com.mx.kylgis.pos.imports.CustomerCSVImport` (28 elementos)
- [ ] `jCBiso` — `JComboBox` — c biso
- [ ] `jCheckVisible` — `JCheckBox` — visible
- [ ] `jComboAccountID` — `JComboBox` — account id
- [ ] `jComboAddress1` — `JComboBox` — address
- [ ] `jComboAddress2` — `JComboBox` — address
- [ ] `jComboCity` — `JComboBox` — city
- [ ] `jComboEmail` — `JComboBox` — email
- [ ] `jComboFirstName` — `JComboBox` — first name
- [ ] `jComboLastName` — `JComboBox` — last name
- [ ] `jComboName` — `JComboBox` — name **REVISIÓN SEMÁNTICA**
- [ ] `jComboPhone` — `JComboBox` — phone
- [ ] `jComboPhone2` — `JComboBox` — phone
- [ ] `jComboPostal` — `JComboBox` — postal
- [ ] `jComboRegion` — `JComboBox` — region
- [ ] `jComboSearchKey` — `JComboBox` — search key
- [ ] `jComboSeparator` — `JComboBox` — separator
- [ ] `jFileName` — `JTextField` — file name
- [ ] `jbtnFileChoose` — `JButton` — file choose
- [ ] `jbtnImport` — `JButton` — import
- [ ] `jbtnRead` — `JButton` — read
- [ ] `jbtnReset` — `JButton` — reset
- [ ] `jtxtBad` — `JTextField` — bad
- [ ] `jtxtInvalid` — `JTextField` — invalid
- [ ] `jtxtMissing` — `JTextField` — missing
- [ ] `jtxtNew` — `JTextField` — new
- [ ] `jtxtNoChange` — `JTextField` — no change
- [ ] `jtxtRecords` — `JTextField` — records
- [ ] `jtxtUpdate` — `JTextField` — update

### `com.mx.kylgis.pos.imports.JPanelCSVCleardb` (2 elementos)
- [ ] `jBtnCleardb` — `JButton` — cleardb
- [ ] `jEnableButton` — `JCheckBox` — enable button

### `com.mx.kylgis.pos.imports.JPanelCSVImport` (27 elementos)
- [ ] `jCBiso` — `JComboBox` — c biso
- [ ] `jCheckInCatalogue` — `JCheckBox` — in catalogue
- [ ] `jCheckSellIncTax` — `JCheckBox` — sell inc tax
- [ ] `jComboBarcode` — `JComboBox` — barcode
- [ ] `jComboBuy` — `JComboBox` — buy
- [ ] `jComboCategory` — `JComboBox` — category
- [ ] `jComboDefaultCategory` — `JComboBox` — default category
- [ ] `jComboDefaultSupplier` — `JComboBox` — default supplier
- [ ] `jComboName` — `JComboBox` — name **REVISIÓN SEMÁNTICA**
- [ ] `jComboReference` — `JComboBox` — reference
- [ ] `jComboSell` — `JComboBox` — sell
- [ ] `jComboSeparator` — `JComboBox` — separator
- [ ] `jComboSupplier` — `JComboBox` — supplier
- [ ] `jComboTax` — `JComboBox` — tax
- [ ] `jFileName` — `JTextField` — file name
- [ ] `jHeaderRead` — `JButton` — header read
- [ ] `jImport` — `JButton` — import
- [ ] `jTextBadCats` — `JTextField` — text bad cats
- [ ] `jTextBadPrice` — `JTextField` — text bad price
- [ ] `jTextInvalid` — `JTextField` — text invalid
- [ ] `jTextMissing` — `JTextField` — text missing
- [ ] `jTextNew` — `JTextField` — text new
- [ ] `jTextNoChange` — `JTextField` — text no change
- [ ] `jTextRecords` — `JTextField` — text records
- [ ] `jTextUpdate` — `JTextField` — text update
- [ ] `jbtnFileChoose` — `JButton` — file choose
- [ ] `jbtnReset` — `JButton` — reset

### `com.mx.kylgis.pos.imports.StockQtyImport` (8 elementos)
- [ ] `jComboSeparator` — `JComboBox` — separator
- [ ] `jFileName` — `JTextField` — file name
- [ ] `jFileRead` — `JButton` — file read
- [ ] `jImport` — `JButton` — import
- [ ] `jTextRecords` — `JTextField` — text records
- [ ] `jTextUpdate` — `JTextField` — text update
- [ ] `jbtnFileChoose` — `JButton` — file choose
- [ ] `jbtnReset` — `JButton` — reset

### `com.mx.kylgis.pos.inventory.AttributeFilter` (1 elementos)
- [ ] `jAttr` — `JComboBox` — attr

### `com.mx.kylgis.pos.inventory.AttributeSetFilter` (1 elementos)
- [ ] `jAttrSet` — `JComboBox` — attr set

### `com.mx.kylgis.pos.inventory.AttributeSetsEditor` (1 elementos)
- [ ] `m_jName` — `JTextField` — name (attribute set)

### `com.mx.kylgis.pos.inventory.AttributeUseEditor` (2 elementos)
- [ ] `jAttribute` — `JComboBox` — attribute
- [ ] `jLineno` — `JTextField` — lineno

### `com.mx.kylgis.pos.inventory.AttributeValuesEditor` (1 elementos)
- [ ] `jValue` — `JTextField` — value

### `com.mx.kylgis.pos.inventory.AttributesEditor` (1 elementos)
- [ ] `m_jName` — `JTextField` — name (attribute)

### `com.mx.kylgis.pos.inventory.AuxiliarEditor` (6 elementos)
- [ ] `m_jBarcode` — `JTextField` — barcode
- [ ] `m_jEnter1` — `JButton` — enter
- [ ] `m_jEnter2` — `JButton` — enter
- [ ] `m_jProduct` — `JTextField` — product
- [ ] `m_jReference` — `JTextField` — reference
- [ ] `m_jSearch` — `JButton` — search

### `com.mx.kylgis.pos.inventory.BundleEditor` (7 elementos)
- [ ] `m_jBarcode` — `JTextField` — barcode
- [ ] `m_jEnter1` — `JButton` — enter
- [ ] `m_jEnter2` — `JButton` — enter
- [ ] `m_jProduct` — `JTextField` — product
- [ ] `m_jQuantity` — `JTextField` — quantity
- [ ] `m_jReference` — `JTextField` — reference
- [ ] `m_jSearch` — `JButton` — search

### `com.mx.kylgis.pos.inventory.CategoriesEditor` (7 elementos)
- [ ] `jBtnShowTrans` — `JButton` — show trans
- [ ] `jLblName` — `JLabel` — name (category)
- [ ] `m_jCatNameShow` — `JCheckBox` — cat name show
- [ ] `m_jCatOrder` — `JTextField` — cat order
- [ ] `m_jCategory` — `JComboBox` — category
- [ ] `m_jName` — `JTextField` — name (category)
- [ ] `m_jTextTip` — `JTextField` — text tip

### `com.mx.kylgis.pos.inventory.JDialogProductSuppliers` (4 elementos)
- [ ] `cancel` — `JButton` — cancel
- [ ] `newSupplier` — `JButton` — new supplier
- [ ] `save` — `JButton` — save
- [ ] `table` — `JTable` — table **REVISIÓN SEMÁNTICA**

### `com.mx.kylgis.pos.inventory.JDlgUploadProducts` (2 elementos)
- [ ] `jcmdCancel` — `JButton` — cancel
- [ ] `jcmdOK` — `JButton` — ok

### `com.mx.kylgis.pos.inventory.JInventoryLines` (1 elementos)
- [ ] `m_tableinventory` — `JTable` — m_tableinventory

### `com.mx.kylgis.pos.inventory.LocationsView` (3 elementos)
- [ ] `jLabel2` — `JLabel` — setting **REVISIÓN SEMÁNTICA**
- [ ] `m_jAddress` — `JTextField` — address
- [ ] `m_jName` — `JTextField` — name (location)

### `com.mx.kylgis.pos.inventory.MaterialFilter` (5 elementos)
- [ ] `m_jCboName` — `JComboBox` — name (material)
- [ ] `m_jCboPriceBuy` — `JComboBox` — price buy
- [ ] `m_jCboProduct` — `JComboBox` — product
- [ ] `m_jName` — `JTextField` — name (material)
- [ ] `m_jPriceBuy` — `JTextField` — price buy

### `com.mx.kylgis.pos.inventory.PriceImportPanel` (1 elementos)
- [ ] `btnScanPal` — `JButton` — scan pal

### `com.mx.kylgis.pos.inventory.ProductsEditor` (51 elementos)
- [ ] `create` — `JMenuItem` — create
- [ ] `jBtnBold` — `JButton` — bold
- [ ] `jBtnBreak` — `JButton` — break
- [ ] `jBtnColour` — `JButton` — colour
- [ ] `jBtnImage` — `JButton` — image
- [ ] `jBtnItalic` — `JButton` — italic
- [ ] `jBtnLarge` — `JButton` — large
- [ ] `jBtnReset` — `JButton` — reset
- [ ] `jBtnShowTrans` — `JButton` — show trans
- [ ] `jBtnSmall` — `JButton` — small
- [ ] `jBtnStyle` — `JButton` — style
- [ ] `jBtnSupplier` — `JButton` — supplier
- [ ] `jBtnXml` — `JButton` — xml
- [ ] `jButtonHTML` — `JButton` — html
- [ ] `jLabel1` — `JLabel` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jTabbedPane1` — `JTabbedPane` — setting **REVISIÓN SEMÁNTICA**
- [ ] `m_jAtt` — `JComboBox` — att
- [ ] `m_jCatalogOrder` — `JTextField` — catalog order
- [ ] `m_jCategory` — `JComboBox` — category
- [ ] `m_jCheckWarrantyReceipt` — `JCheckBox` — warranty receipt
- [ ] `m_jCode` — `JTextField` — code
- [ ] `m_jCodetype` — `JComboBox` — codetype
- [ ] `m_jComment` — `JCheckBox` — comment
- [ ] `m_jConstant` — `JCheckBox` — constant
- [ ] `m_jGrossProfit` — `JTextField` — gross profit
- [ ] `m_jInCatalog` — `JCheckBox` — in catalog
- [ ] `m_jName` — `JTextField` — name (product)
- [ ] `m_jPriceBuy` — `JTextField` — price buy
- [ ] `m_jPriceSell` — `JTextField` — price sell
- [ ] `m_jPriceSellTax` — `JTextField` — price sell tax
- [ ] `m_jPrintKB` — `JCheckBox` — print kb
- [ ] `m_jPrintTo` — `JComboBox` — print to
- [ ] `m_jRecipeIngredient` — `JCheckBox` — recipe ingredient
- [ ] `m_jRef` — `JTextField` — ref
- [ ] `m_jScale` — `JCheckBox` — scale
- [ ] `m_jSendStatus` — `JCheckBox` — send status
- [ ] `m_jService` — `JCheckBox` — service
- [ ] `m_jStockUnits` — `JTextField` — stock units
- [ ] `m_jSupplier` — `JComboBox` — supplier
- [ ] `m_jTax` — `JComboBox` — tax
- [ ] `m_jTextTip` — `JTextField` — text tip
- [ ] `m_jUom` — `JComboBox` — uom
- [ ] `m_jVerpatrib` — `JCheckBox` — verpatrib
- [ ] `m_jVprice` — `JCheckBox` — vprice
- [ ] `m_jbtndate` — `JButton` — date
- [ ] `m_jdate` — `JTextField` — date
- [ ] `m_jmargin` — `JTextField` — margin
- [ ] `m_jstockcost` — `JTextField` — stockcost
- [ ] `m_jstockvolume` — `JTextField` — stockvolume
- [ ] `manage` — `JMenuItem` — manage
- [ ] `txtAttributes` — `JTextArea` — attributes

### `com.mx.kylgis.pos.inventory.ProductsPanel` (1 elementos)
- [ ] `btnScanPal` — `JButton` — scan pal

### `com.mx.kylgis.pos.inventory.ProductsWarehouseEditor` (3 elementos)
- [ ] `m_jMaximum` — `JTextField` — maximum
- [ ] `m_jMinimum` — `JTextField` — minimum
- [ ] `m_jQuantity` — `JTextField` — quantity

### `com.mx.kylgis.pos.inventory.StockDiaryEditor` (16 elementos)
- [ ] `jEditAttributes` — `JButton` — edit attributes
- [ ] `jEditProduct` — `JButton` — edit product
- [ ] `jattributes` — `JTextField` — attributes
- [ ] `jproduct` — `JTextField` — product
- [ ] `m_jEnter` — `JButton` — enter
- [ ] `m_jEnter1` — `JButton` — enter
- [ ] `m_jLocation` — `JComboBox` — location
- [ ] `m_jLocationDes` — `JComboBox` — location des
- [ ] `m_jSupplier` — `JComboBox` — supplier
- [ ] `m_jbtndate` — `JButton` — date
- [ ] `m_jcodebar` — `JTextField` — codebar
- [ ] `m_jdate` — `JTextField` — date
- [ ] `m_jprice` — `JTextField` — price
- [ ] `m_jreason` — `JComboBox` — reason
- [ ] `m_jreference` — `JTextField` — reference
- [ ] `m_junits` — `JTextField` — units

### `com.mx.kylgis.pos.inventory.StockManagement` (17 elementos)
- [ ] `jTextField1` — `JTextField` — setting **REVISIÓN SEMÁNTICA**
- [ ] `m_jBtnDelete` — `JButton` — delete
- [ ] `m_jBtnShowStock` — `JButton` — show stock
- [ ] `m_jDelete` — `JButton` — delete
- [ ] `m_jEditAttributes` — `JButton` — edit attributes
- [ ] `m_jEditLine` — `JButton` — edit line
- [ ] `m_jEnter` — `JButton` — enter
- [ ] `m_jList` — `JButton` — list **REVISIÓN SEMÁNTICA**
- [ ] `m_jLocation` — `JComboBox` — location
- [ ] `m_jLocationDes` — `JComboBox` — location des
- [ ] `m_jSupplier` — `JComboBox` — supplier
- [ ] `m_jSupplierDoc` — `JTextField` — supplier doc
- [ ] `m_jSupplierNew` — `JButton` — supplier new
- [ ] `m_jbtndate` — `JButton` — date
- [ ] `m_jcodebar` — `JLabel` — codebar
- [ ] `m_jdate` — `JTextField` — date
- [ ] `m_jreason` — `JComboBox` — reason

### `com.mx.kylgis.pos.inventory.TaxCategoriesEditor` (2 elementos)
- [ ] `jLabel2` — `JLabel` — setting **REVISIÓN SEMÁNTICA**
- [ ] `m_jName` — `JTextField` — name (tax category)

### `com.mx.kylgis.pos.inventory.TaxCustCategoriesEditor` (2 elementos)
- [ ] `jLabel2` — `JLabel` — setting **REVISIÓN SEMÁNTICA**
- [ ] `m_jName` — `JTextField` — name (customer tax category)

### `com.mx.kylgis.pos.inventory.TaxEditor` (10 elementos)
- [ ] `jCascade` — `JCheckBox` — cascade
- [ ] `jLabel2` — `JLabel` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jOrder` — `JTextField` — order
- [ ] `m_jCustTaxCategory` — `JComboBox` — cust tax category
- [ ] `m_jName` — `JTextField` — name (tax)
- [ ] `m_jRate` — `JTextField` — rate
- [ ] `m_jTaxCategory` — `JComboBox` — tax category
- [ ] `m_jTaxParent` — `JComboBox` — tax parent
- [ ] `satButton` — `JButton` — sat button
- [ ] `satProductButton` — `JButton` — sat product button

### `com.mx.kylgis.pos.inventory.UomEditor` (2 elementos)
- [ ] `jLabel2` — `JLabel` — setting **REVISIÓN SEMÁNTICA**
- [ ] `m_jName` — `JTextField` — name (unit)

### `com.mx.kylgis.pos.inventory.transform.ProductUomConversionPanel` (9 elementos)
- [ ] `active` — `JCheckBox` — active
- [ ] `factor` — `JTextField` — factor
- [ ] `fromUom` — `JComboBox` — from uom
- [ ] `invert` — `JCheckBox` — invert
- [ ] `product` — `JComboBox` — product
- [ ] `reload` — `JButton` — reload
- [ ] `save` — `JButton` — save
- [ ] `table` — `JTable` — table **REVISIÓN SEMÁNTICA**
- [ ] `toggle` — `JButton` — toggle

### `com.mx.kylgis.pos.inventory.transform.RecipeManagementPanel` (20 elementos)
- [ ] `activateVersion` — `JButton` — activate version
- [ ] `addComponent` — `JButton` — add component
- [ ] `clearDraft` — `JButton` — clear draft
- [ ] `componentProduct` — `JComboBox` — component product
- [ ] `componentQuantity` — `JTextField` — component quantity
- [ ] `componentTable` — `JTable` — component table
- [ ] `componentUom` — `JComboBox` — component uom
- [ ] `componentWaste` — `JTextField` — component waste
- [ ] `editVersion` — `JButton` — edit version
- [ ] `notes` — `JTextArea` — notes
- [ ] `previewTable` — `JTable` — preview table
- [ ] `recipeMode` — `JComboBox` — recipe mode
- [ ] `refresh` — `JButton` — refresh
- [ ] `removeComponent` — `JButton` — remove component
- [ ] `resultProduct` — `JComboBox` — result product
- [ ] `retireVersion` — `JButton` — retire version
- [ ] `saveDraft` — `JButton` — save draft
- [ ] `versionTable` — `JTable` — version table
- [ ] `yieldQuantity` — `JTextField` — yield quantity
- [ ] `yieldUom` — `JComboBox` — yield uom

### `com.mx.kylgis.pos.inventory.transform.RecipeProductionPanel` (8 elementos)
- [ ] `actualOutput` — `JTextField` — actual output
- [ ] `batches` — `JTextField` — batches
- [ ] `location` — `JComboBox` — location
- [ ] `notes` — `JTextArea` — notes
- [ ] `post` — `JButton` — post
- [ ] `product` — `JComboBox` — product
- [ ] `reload` — `JButton` — reload
- [ ] `useExpected` — `JButton` — use expected

### `com.mx.kylgis.pos.inventory.transform.SaleRecipeConsumptionPanel` (3 elementos)
- [ ] `alerts` — `JTable` — alerts
- [ ] `recipes` — `JTable` — recipes
- [ ] `refresh` — `JButton` — refresh

### `com.mx.kylgis.pos.inventory.transform.TransformationHistoryPanel` (6 elementos)
- [ ] `limit` — `JTextField` — limit
- [ ] `lineTable` — `JTable` — line table
- [ ] `location` — `JComboBox` — location
- [ ] `reload` — `JButton` — reload
- [ ] `reverse` — `JButton` — reverse
- [ ] `transformationTable` — `JTable` — transformation table

### `com.mx.kylgis.pos.mant.FloorsEditor` (1 elementos)
- [ ] `m_jName` — `JTextField` — name (floor)

### `com.mx.kylgis.pos.mant.PlacesEditor` (7 elementos)
- [ ] `m_jFloor` — `JComboBox` — floor
- [ ] `m_jHeight` — `JTextField` — height
- [ ] `m_jName` — `JTextField` — name (table)
- [ ] `m_jSeats` — `JTextField` — seats
- [ ] `m_jWidth` — `JTextField` — width
- [ ] `m_jX` — `JTextField` — x
- [ ] `m_jY` — `JTextField` — y

### `com.mx.kylgis.pos.panels.AuxiliarFilter` (6 elementos)
- [ ] `Enter1` — `JButton` — enter
- [ ] `Enter2` — `JButton` — enter
- [ ] `m_jBarcode1` — `JTextField` — barcode
- [ ] `m_jReference1` — `JTextField` — reference
- [ ] `m_jSearch` — `JTextField` — search
- [ ] `search` — `JButton` — search

### `com.mx.kylgis.pos.panels.BundleFilter` (6 elementos)
- [ ] `Enter1` — `JButton` — enter
- [ ] `Enter2` — `JButton` — enter
- [ ] `m_jBarcode1` — `JTextField` — barcode
- [ ] `m_jReference1` — `JTextField` — reference
- [ ] `m_jSearch` — `JTextField` — search
- [ ] `search` — `JButton` — search

### `com.mx.kylgis.pos.panels.JPanelCloseMoney` (16 elementos)
- [ ] `jCBCloseCash` — `JComboBox` — cb close cash
- [ ] `m_jCash` — `JTextField` — cash
- [ ] `m_jCloseCash` — `JButton` — close cash
- [ ] `m_jCount` — `JTextField` — count
- [ ] `m_jLinesRemoved` — `JTextField` — lines removed
- [ ] `m_jMaxDate` — `JTextField` — max date
- [ ] `m_jMinDate` — `JTextField` — min date
- [ ] `m_jNoCashSales` — `JTextField` — no cash sales
- [ ] `m_jPrintCash` — `JButton` — print cash
- [ ] `m_jSales` — `JTextField` — sales
- [ ] `m_jSalesSubtotal` — `JTextField` — sales subtotal
- [ ] `m_jSalesTaxes` — `JTextField` — sales taxes
- [ ] `m_jSalesTotal` — `JTextField` — sales total
- [ ] `m_jSequence` — `JTextField` — sequence
- [ ] `m_jTicketTable` — `JTable` — ticket table
- [ ] `m_jsalestable` — `JTable` — salestable

### `com.mx.kylgis.pos.panels.JPanelCloseMoneyReprint` (14 elementos)
- [ ] `m_jCash` — `JTextField` — cash
- [ ] `m_jCount` — `JTextField` — count
- [ ] `m_jLinesRemoved` — `JTextField` — lines removed
- [ ] `m_jMaxDate` — `JTextField` — max date
- [ ] `m_jMinDate` — `JTextField` — min date
- [ ] `m_jNoCashSales` — `JTextField` — no cash sales
- [ ] `m_jPrint` — `JButton` — print
- [ ] `m_jSales` — `JTextField` — sales
- [ ] `m_jSalesSubtotal` — `JTextField` — sales subtotal
- [ ] `m_jSalesTaxes` — `JTextField` — sales taxes
- [ ] `m_jSalesTotal` — `JTextField` — sales total
- [ ] `m_jSequence` — `JTextField` — sequence
- [ ] `m_jTicketTable` — `JTable` — ticket table
- [ ] `m_jsalestable` — `JTable` — salestable

### `com.mx.kylgis.pos.panels.JPanelPrinter` (1 elementos)
- [ ] `m_jPrinters` — `JTabbedPane` — printers

### `com.mx.kylgis.pos.panels.JProductFinder` (4 elementos)
- [ ] `jButton3` — `JButton` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jListProducts` — `JList` — list products
- [ ] `jcmdCancel` — `JButton` — cancel
- [ ] `jcmdOK` — `JButton` — ok

### `com.mx.kylgis.pos.panels.JTicketsFinder` (14 elementos)
- [ ] `btnCustomer` — `JButton` — customer
- [ ] `btnDateEnd` — `JButton` — date end
- [ ] `btnDateStart` — `JButton` — date start
- [ ] `jComboBoxTicket` — `JComboBox` — ticket
- [ ] `jListTickets` — `JList` — list tickets
- [ ] `jTxtEndDate` — `JTextField` — end date
- [ ] `jTxtStartDate` — `JTextField` — start date
- [ ] `jbtnExecute` — `JButton` — execute
- [ ] `jbtnReset` — `JButton` — reset
- [ ] `jcboMoney` — `JComboBox` — money
- [ ] `jcboUser` — `JComboBox` — user
- [ ] `jcmdCancel` — `JButton` — cancel
- [ ] `jcmdOK` — `JButton` — ok
- [ ] `jtxtCustomer` — `JTextField` — customer

### `com.mx.kylgis.pos.panels.PaymentsEditor` (1 elementos)
- [ ] `m_jreason` — `JComboBox` — reason

### `com.mx.kylgis.pos.payment.ConfigPaymentPanelGeneric` (2 elementos)
- [ ] `jtxtCommerceID` — `JTextField` — commerce id
- [ ] `jtxtCommercePwd` — `JPasswordField` — commerce pwd

### `com.mx.kylgis.pos.payment.JPaymentCashPos` (1 elementos)
- [ ] `btn` — `JButton` — btn

### `com.mx.kylgis.pos.payment.JPaymentDebt` (6 elementos)
- [ ] `jlblMessage` — `JTextArea` — message
- [ ] `m_jName` — `JTextField` — name (debtor)
- [ ] `m_jNotes` — `JTextArea` — notes
- [ ] `txtCurdate` — `JTextField` — curdate
- [ ] `txtCurdebt` — `JTextField` — curdebt
- [ ] `txtMaxdebt` — `JTextField` — maxdebt

### `com.mx.kylgis.pos.payment.JPaymentPaper` (1 elementos)
- [ ] `m_jVoucher` — `JComboBox` — voucher

### `com.mx.kylgis.pos.payment.JPaymentSelect` (6 elementos)
- [ ] `m_jButtonAdd` — `JButton` — add
- [ ] `m_jButtonCancel` — `JButton` — cancel
- [ ] `m_jButtonOK` — `JButton` — ok
- [ ] `m_jButtonPrint` — `JToggleButton` — print
- [ ] `m_jButtonRemove` — `JButton` — remove
- [ ] `m_jTabPayment` — `JTabbedPane` — tab payment

### `com.mx.kylgis.pos.payment.JPaymentVoucher` (3 elementos)
- [ ] `m_directVoucherCode` — `JTextField` — m_direct voucher code
- [ ] `m_directVoucherSearch` — `JButton` — m_direct voucher search
- [ ] `m_jVoucher` — `JComboBox` — voucher

### `com.mx.kylgis.pos.payment.PaymentPanelMagCard` (4 elementos)
- [ ] `clearButton` — `JButton` — clear button
- [ ] `jReset` — `JButton` — reset
- [ ] `jTextArea` — `JTextArea` — text area
- [ ] `m_jKeyFactory` — `JTextArea` — key factory

### `com.mx.kylgis.pos.printer.javapos.DeviceFiscalPrinterJavaPOS` (2 elementos)
- [ ] `jButton1` — `JButton` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jButton2` — `JButton` — setting **REVISIÓN SEMÁNTICA**

### `com.mx.kylgis.pos.reports.JPanelReport` (2 elementos)
- [ ] `jButton1` — `JButton` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jToggleFilter` — `JToggleButton` — toggle filter

### `com.mx.kylgis.pos.reports.JParamsCustomer` (2 elementos)
- [ ] `btnCustomer` — `JButton` — customer
- [ ] `jTextField1` — `JTextField` — setting **REVISIÓN SEMÁNTICA**

### `com.mx.kylgis.pos.reports.JParamsDatesInterval` (4 elementos)
- [ ] `btnDateEnd` — `JButton` — date end
- [ ] `btnDateStart` — `JButton` — date start
- [ ] `jTxtEndDate` — `JTextField` — end date
- [ ] `jTxtStartDate` — `JTextField` — start date

### `com.mx.kylgis.pos.reports.JParamsLocation` (1 elementos)
- [ ] `m_jLocation` — `JComboBox` — location

### `com.mx.kylgis.pos.reports.JParamsReason` (1 elementos)
- [ ] `m_jreason` — `JComboBox` — reason

### `com.mx.kylgis.pos.reports.JParamsSuppliers` (2 elementos)
- [ ] `btnSupplier` — `JButton` — supplier
- [ ] `jTextField1` — `JTextField` — setting **REVISIÓN SEMÁNTICA**

### `com.mx.kylgis.pos.reports.JParamsText` (1 elementos)
- [ ] `txtField` — `JTextField` — field **REVISIÓN SEMÁNTICA**

### `com.mx.kylgis.pos.reports.JParamsUser` (2 elementos)
- [ ] `btnUser` — `JButton` — user
- [ ] `jTextField1` — `JTextField` — setting **REVISIÓN SEMÁNTICA**

### `com.mx.kylgis.pos.sales.JMooringDetails` (3 elementos)
- [ ] `jTableSelector` — `JTable` — table selector
- [ ] `jText` — `JTextField` — text **REVISIÓN SEMÁNTICA**
- [ ] `jbtnCreateTicket` — `JButton` — create ticket

### `com.mx.kylgis.pos.sales.JPanelButtons` (1 elementos)
- [ ] `btn` — `JButton` — btn

### `com.mx.kylgis.pos.sales.JPanelResetPickupId` (2 elementos)
- [ ] `jbtnExit` — `JButton` — exit
- [ ] `jbtnUpdate` — `JButton` — update

### `com.mx.kylgis.pos.sales.JPanelTicket` (17 elementos)
- [ ] `btnReprint1` — `JButton` — reprint
- [ ] `btnSplit` — `JButton` — split
- [ ] `jBtnCustomer` — `JButton` — customer
- [ ] `jCheckStock` — `JButton` — stock
- [ ] `jEditAttributes` — `JButton` — edit attributes
- [ ] `jTBtnShow` — `JToggleButton` — t btn show
- [ ] `j_btnRemotePrt` — `JButton` — _btn remote prt
- [ ] `jbtnMooring` — `JButton` — mooring
- [ ] `m_jDelete` — `JButton` — delete
- [ ] `m_jEditLine` — `JButton` — edit line
- [ ] `m_jEnter` — `JButton` — enter
- [ ] `m_jKeyFactory` — `JTextField` — key factory
- [ ] `m_jList` — `JButton` — list **REVISIÓN SEMÁNTICA**
- [ ] `m_jTax` — `JComboBox` — tax
- [ ] `m_jbtnPlatformOrder` — `JButton` — platform order
- [ ] `m_jbtnScale` — `JButton` — scale
- [ ] `m_jbtnTicketPreview` — `JButton` — ticket preview

### `com.mx.kylgis.pos.sales.JProductAttEdit` (2 elementos)
- [ ] `m_jButtonCancel` — `JButton` — cancel
- [ ] `m_jButtonOK` — `JButton` — ok

### `com.mx.kylgis.pos.sales.JProductAttEdit1` (2 elementos)
- [ ] `m_jButtonCancel` — `JButton` — cancel
- [ ] `m_jButtonOK` — `JButton` — ok

### `com.mx.kylgis.pos.sales.JProductAttEdit2` (2 elementos)
- [ ] `m_jButtonCancel` — `JButton` — cancel
- [ ] `m_jButtonOK` — `JButton` — ok

### `com.mx.kylgis.pos.sales.JProductAttListItem` (1 elementos)
- [ ] `jValues` — `JComboBox` — values

### `com.mx.kylgis.pos.sales.JProductLineEdit` (3 elementos)
- [ ] `m_jBtnPriceUpdate` — `JButton` — price update
- [ ] `m_jButtonCancel` — `JButton` — cancel
- [ ] `m_jButtonOK` — `JButton` — ok

### `com.mx.kylgis.pos.sales.JProductLineEditTax` (3 elementos)
- [ ] `m_jBtnPriceUpdate` — `JButton` — price update
- [ ] `m_jButtonCancel` — `JButton` — cancel
- [ ] `m_jButtonOK` — `JButton` — ok

### `com.mx.kylgis.pos.sales.JRefundLines` (3 elementos)
- [ ] `m_jbtnAddAll` — `JButton` — add all
- [ ] `m_jbtnAddLine` — `JButton` — add line
- [ ] `m_jbtnAddOne` — `JButton` — add one

### `com.mx.kylgis.pos.sales.JTicketLines` (1 elementos)
- [ ] `m_jTicketTable` — `JTable` — ticket table

### `com.mx.kylgis.pos.sales.JTicketsBagTicket` (7 elementos)
- [ ] `jButton1` — `JButton` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jButton2` — `JButton` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jrbRefunds` — `JRadioButton` — rb refunds
- [ ] `jrbSales` — `JRadioButton` — rb sales
- [ ] `m_jEdit` — `JButton` — edit
- [ ] `m_jPrint` — `JButton` — print
- [ ] `m_jRefund` — `JButton` — refund

### `com.mx.kylgis.pos.sales.JTicketsBagTicketBag` (3 elementos)
- [ ] `m_jBtnCancel` — `JButton` — cancel
- [ ] `m_jBtnCancel1` — `JButton` — cancel
- [ ] `m_jBtnDelete` — `JButton` — delete

### `com.mx.kylgis.pos.sales.PlatformOrderDialog` (3 elementos)
- [ ] `code` — `JTextField` — code
- [ ] `customer` — `JTextField` — customer
- [ ] `platform` — `JComboBox` — platform

### `com.mx.kylgis.pos.sales.ReceiptSplit` (6 elementos)
- [ ] `jBtnToLeftAll` — `JButton` — to left all
- [ ] `jBtnToLeftOne` — `JButton` — to left one
- [ ] `jBtnToRightAll` — `JButton` — to right all
- [ ] `jBtnToRightOne` — `JButton` — to right one
- [ ] `m_jButtonCancel` — `JButton` — cancel
- [ ] `m_jButtonOK` — `JButton` — ok

### `com.mx.kylgis.pos.sales.SimpleReceipt` (1 elementos)
- [ ] `btnCustomer` — `JButton` — customer

### `com.mx.kylgis.pos.sales.restaurant.JTicketsBagRestaurant` (5 elementos)
- [ ] `j_btnGuests` — `JButton` — _btn guests
- [ ] `j_btnKitchen` — `JButton` — _btn kitchen
- [ ] `m_DelTicket` — `JButton` — m_del ticket
- [ ] `m_MoveTable` — `JButton` — m_move table
- [ ] `m_TablePlan` — `JButton` — m_table plan

### `com.mx.kylgis.pos.sales.restaurant.JTicketsBagRestaurantMap` (5 elementos)
- [ ] `jTabFloors` — `JTabbedPane` — tab floors
- [ ] `m_jbtnLayout` — `JButton` — layout
- [ ] `m_jbtnRefresh` — `JButton` — refresh
- [ ] `m_jbtnReservations` — `JButton` — reservations
- [ ] `m_jbtnSave` — `JButton` — save

### `com.mx.kylgis.pos.sales.restaurant.JTicketsBagRestaurantRes` (4 elementos)
- [ ] `jButton1` — `JButton` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jbtnShowCalendar` — `JButton` — show calendar
- [ ] `m_jbtnReceive` — `JButton` — receive
- [ ] `m_jbtnTables` — `JButton` — tables

### `com.mx.kylgis.pos.sales.restaurant.Place` (1 elementos)
- [ ] `m_btn` — `JButton` — m_btn

### `com.mx.kylgis.pos.sales.shared.JTicketsBagShared` (5 elementos)
- [ ] `m_jDelTicket` — `JButton` — del ticket
- [ ] `m_jHold` — `JButton` — hold
- [ ] `m_jListTickets` — `JButton` — list tickets
- [ ] `m_jNewTicket` — `JButton` — new ticket
- [ ] `m_jReprintTickets` — `JButton` — reprint tickets

### `com.mx.kylgis.pos.sales.shared.JTicketsBagSharedList` (1 elementos)
- [ ] `m_jButtonCancel` — `JButton` — cancel

### `com.mx.kylgis.pos.sales.shared.JTicketsReprintList` (1 elementos)
- [ ] `m_jButtonCancel` — `JButton` — cancel

### `com.mx.kylgis.pos.sales.simple.JTicketsBagSimple` (1 elementos)
- [ ] `m_jDelTicket` — `JButton` — del ticket

### `com.mx.kylgis.pos.scale.config.ScaleRequestSelectorPanel` (1 elementos)
- [ ] `command` — `JComboBox` — command

### `com.mx.kylgis.pos.suppliers.JDialogNewSupplier` (10 elementos)
- [ ] `m_jBtnCancel` — `JButton` — cancel
- [ ] `m_jBtnOK` — `JButton` — ok
- [ ] `m_jName` — `JTextField` — name (supplier)
- [ ] `m_jSearchkey` — `JTextField` — searchkey
- [ ] `m_jTaxID` — `JTextField` — tax id
- [ ] `txtEmail` — `JTextField` — email
- [ ] `txtFirstName` — `JTextField` — first name
- [ ] `txtLastName` — `JTextField` — last name
- [ ] `txtPhone` — `JTextField` — phone
- [ ] `txtPhone2` — `JTextField` — phone

### `com.mx.kylgis.pos.suppliers.JSupplierFinder` (5 elementos)
- [ ] `jListSuppliers` — `JList` — list suppliers
- [ ] `jbtnExecute` — `JButton` — execute
- [ ] `jbtnReset` — `JButton` — reset
- [ ] `jcmdCancel` — `JButton` — cancel
- [ ] `jcmdOK` — `JButton` — ok

### `com.mx.kylgis.pos.suppliers.SuppliersView` (25 elementos)
- [ ] `jBtnShowTrans` — `JButton` — show trans
- [ ] `jLabel7` — `JLabel` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jTabbedPane1` — `JTabbedPane` — setting **REVISIÓN SEMÁNTICA**
- [ ] `jTableSupplierTransactions` — `JTable` — table supplier transactions
- [ ] `m_jName` — `JTextField` — name (supplier)
- [ ] `m_jNotes` — `JTextArea` — notes
- [ ] `m_jSearchkey` — `JTextField` — searchkey
- [ ] `m_jTaxID` — `JTextField` — tax id
- [ ] `m_jVATID` — `JTextField` — vatid
- [ ] `m_jVisible` — `JCheckBox` — visible
- [ ] `txtAddress` — `JTextField` — address
- [ ] `txtAddress2` — `JTextField` — address
- [ ] `txtCity` — `JTextField` — city
- [ ] `txtCountry` — `JTextField` — country
- [ ] `txtCurdate` — `JTextField` — curdate
- [ ] `txtCurdebt` — `JTextField` — curdebt
- [ ] `txtEmail` — `JTextField` — email
- [ ] `txtFax` — `JTextField` — fax
- [ ] `txtFirstName` — `JTextField` — first name
- [ ] `txtLastName` — `JTextField` — last name
- [ ] `txtMaxdebt` — `JTextField` — maxdebt
- [ ] `txtPhone` — `JTextField` — phone
- [ ] `txtPhone2` — `JTextField` — phone
- [ ] `txtPostal` — `JTextField` — postal
- [ ] `txtRegion` — `JTextField` — region

### `com.mx.kylgis.pos.ticket.CategoryFilter` (1 elementos)
- [ ] `m_jCategory` — `JComboBox` — category

### `com.mx.kylgis.pos.ticket.CategoryFilter_1` (1 elementos)
- [ ] `m_jCategory` — `JComboBox` — category

### `com.mx.kylgis.pos.ticket.LocationFilterSales` (3 elementos)
- [ ] `m_jCategory` — `JComboBox` — category
- [ ] `m_jCboPriceBuy` — `JComboBox` — price buy
- [ ] `m_jCboPriceSell` — `JComboBox` — price sell

### `com.mx.kylgis.pos.ticket.ProductFilter` (9 elementos)
- [ ] `jBtnReset` — `JButton` — reset
- [ ] `m_jBarcode` — `JTextField` — barcode
- [ ] `m_jCategory` — `JComboBox` — category
- [ ] `m_jCboName` — `JComboBox` — name (product)
- [ ] `m_jCboPriceBuy` — `JComboBox` — price buy
- [ ] `m_jCboPriceSell` — `JComboBox` — price sell
- [ ] `m_jName` — `JTextField` — name (product)
- [ ] `m_jPriceBuy` — `JTextField` — price buy
- [ ] `m_jPriceSell` — `JTextField` — price sell

### `com.mx.kylgis.pos.ticket.ProductFilter1` (7 elementos)
- [ ] `jBtnReset` — `JButton` — reset
- [ ] `m_jBarcode` — `JTextField` — barcode
- [ ] `m_jCategory` — `JComboBox` — category
- [ ] `m_jCboName` — `JComboBox` — name (product)
- [ ] `m_jCboUnits` — `JComboBox` — units
- [ ] `m_jName` — `JTextField` — name (product)
- [ ] `m_jUnits` — `JTextField` — units

### `com.mx.kylgis.pos.ticket.ProductFilter2` (9 elementos)
- [ ] `jBtnReset` — `JButton` — reset
- [ ] `m_jBarcode` — `JTextField` — barcode
- [ ] `m_jCategory` — `JComboBox` — category
- [ ] `m_jCboName` — `JComboBox` — name (product)
- [ ] `m_jCboPriceBuy` — `JComboBox` — price buy
- [ ] `m_jCboPriceSell` — `JComboBox` — price sell
- [ ] `m_jName` — `JTextField` — name (product)
- [ ] `m_jPriceBuy` — `JTextField` — price buy
- [ ] `m_jPriceSell` — `JTextField` — price sell

### `com.mx.kylgis.pos.ticket.ProductFilter3` (10 elementos)
- [ ] `jBtnReset` — `JButton` — reset
- [ ] `m_jBarcode` — `JTextField` — barcode
- [ ] `m_jCategory` — `JComboBox` — category
- [ ] `m_jCboName` — `JComboBox` — name (product)
- [ ] `m_jCboPriceBuy` — `JComboBox` — price buy
- [ ] `m_jCboPriceSell` — `JComboBox` — price sell
- [ ] `m_jName` — `JTextField` — name (product)
- [ ] `m_jPriceBuy` — `JTextField` — price buy
- [ ] `m_jPriceSell` — `JTextField` — price sell
- [ ] `m_jSupplier` — `JComboBox` — supplier

### `com.mx.kylgis.pos.ticket.ProductFilter4` (5 elementos)
- [ ] `jBtnReset` — `JButton` — reset
- [ ] `m_jBarcode` — `JTextField` — barcode
- [ ] `m_jCategory` — `JComboBox` — category
- [ ] `m_jCboName` — `JComboBox` — name (product)
- [ ] `m_jName` — `JTextField` — name (product)

### `com.mx.kylgis.pos.ticket.ProductFilterSales` (4 elementos)
- [ ] `jBtnReset` — `JButton` — reset
- [ ] `m_jCategory` — `JComboBox` — category
- [ ] `m_jCboPriceBuy` — `JComboBox` — price buy
- [ ] `m_jCboPriceSell` — `JComboBox` — price sell

### `com.mx.kylgis.pos.transfer.Transfer` (8 elementos)
- [ ] `jCBSchema` — `JComboBox` — cb schema
- [ ] `jbtnConnect` — `JButton` — connect
- [ ] `jbtnDbDriverLib` — `JButton` — db driver lib
- [ ] `jbtnReset` — `JButton` — reset
- [ ] `jbtnReset1` — `JButton` — reset
- [ ] `jbtnSet` — `JButton` — set
- [ ] `jbtnTransfer` — `JButton` — transfer
- [ ] `txtOut` — `JTextArea` — out

### `com.mx.kylgis.pos.util.SelectPrinter` (3 elementos)
- [ ] `jPrinters` — `JComboBox` — printers
- [ ] `jcmdCancel` — `JButton` — cancel
- [ ] `jcmdOK` — `JButton` — ok

### `com.mx.kylgis.pos.util.SwingWorkerExample` (2 elementos)
- [ ] `b` — `JButton` — b
- [ ] `listBox` — `JList` — list box

### `com.mx.kylgis.pos.voucher.JDialogReportPanel` (2 elementos)
- [ ] `jcmdCancel` — `JButton` — cancel
- [ ] `jcmdOK` — `JButton` — ok

### `com.mx.kylgis.pos.voucher.VoucherCampaignDialog` (18 elementos)
- [ ] `amount` — `JTextField` — amount
- [ ] `byCustomers` — `JRadioButton` — by customers
- [ ] `byQuantity` — `JRadioButton` — by quantity
- [ ] `campaignHistory` — `JComboBox` — campaign history
- [ ] `campaignName` — `JTextField` — campaign name
- [ ] `clearVisibleCustomers` — `JButton` — clear visible customers
- [ ] `close` — `JButton` — close
- [ ] `copyReportButton` — `JButton` — copy report button
- [ ] `createButton` — `JButton` — create button
- [ ] `customerFilter` — `JTextField` — customer filter
- [ ] `customerTable` — `JTable` — customer table
- [ ] `expiry` — `JTextField` — expiry
- [ ] `exportReportButton` — `JButton` — export report button
- [ ] `loadReportButton` — `JButton` — load report button
- [ ] `prefix` — `JTextField` — prefix
- [ ] `quantity` — `JSpinner` — quantity
- [ ] `reportTable` — `JTable` — report table
- [ ] `selectVisibleCustomers` — `JButton` — select visible customers

### `com.mx.kylgis.pos.voucher.VoucherEditAuthorization` (2 elementos)
- [ ] `password` — `JPasswordField` — password
- [ ] `users` — `JComboBox` — users

### `com.mx.kylgis.pos.voucher.VoucherEditor` (9 elementos)
- [ ] `jButtonPrint` — `JButton` — print
- [ ] `jCBCustomer` — `JComboBox` — cb customer
- [ ] `jChkNoExpiry` — `JCheckBox` — no expiry
- [ ] `m_jAmount` — `JTextField` — amount
- [ ] `m_jBtnExpiresAt` — `JButton` — expires at
- [ ] `m_jCustomer` — `JTextField` — customer
- [ ] `m_jExpiresAt` — `JTextField` — expires at
- [ ] `m_jNumber` — `JTextField` — number
- [ ] `m_jStatus` — `JTextField` — status

### `com.mx.kylgis.pos.voucher.VoucherPanel` (1 elementos)
- [ ] `campaigns` — `JButton` — campaigns


## Identidades poco descriptivas detectadas

- [ ] `com.mx.kylgis.pos.admin.JPeopleFinder#jButton1` — `JButton` — `setting`
- [ ] `com.mx.kylgis.pos.admin.JPeopleFinder#jButton3` — `JButton` — `setting`
- [ ] `com.mx.kylgis.pos.admin.PeopleView#jButton1` — `JButton` — `setting`
- [ ] `com.mx.kylgis.pos.admin.PeopleView#jLabel1` — `JLabel` — `setting`
- [ ] `com.mx.kylgis.pos.config.JPanelConfigERP#jTextField1` — `JTextField` — `setting`
- [ ] `com.mx.kylgis.pos.config.JPanelConfigERP#jTextField2` — `JTextField` — `setting`
- [ ] `com.mx.kylgis.pos.config.JPanelConfigERP#jtxtName` — `JTextField` — `name`
- [ ] `com.mx.kylgis.pos.config.JPanelConfigGeneral#jbtnText` — `JButton` — `text`
- [ ] `com.mx.kylgis.pos.config.JPanelConfigNodes#check` — `JCheckBox` — `check`
- [ ] `com.mx.kylgis.pos.config.JPanelConfiguration#jTabbedPane1` — `JTabbedPane` — `setting`
- [ ] `com.mx.kylgis.pos.customers.CustomersPayment#jButton1` — `JButton` — `setting`
- [ ] `com.mx.kylgis.pos.customers.CustomersView#jLabel7` — `JLabel` — `setting`
- [ ] `com.mx.kylgis.pos.customers.CustomersView#jTabbedPane1` — `JTabbedPane` — `setting`
- [ ] `com.mx.kylgis.pos.data.gui.JListNavigator#m_jlist` — `JList` — `list`
- [ ] `com.mx.kylgis.pos.editor.JEditorAbstract#m_jText` — `JButton` — `text`
- [ ] `com.mx.kylgis.pos.epm.JEmployeeFinder#jButton1` — `JButton` — `setting`
- [ ] `com.mx.kylgis.pos.epm.JEmployeeFinder#jButton3` — `JButton` — `setting`
- [ ] `com.mx.kylgis.pos.forms.JPrincipalApp#jButton1` — `JButton` — `setting`
- [ ] `com.mx.kylgis.pos.imports.CustomerCSVImport#jComboName` — `JComboBox` — `name`
- [ ] `com.mx.kylgis.pos.imports.JPanelCSVImport#jComboName` — `JComboBox` — `name`
- [ ] `com.mx.kylgis.pos.inventory.JDialogProductSuppliers#table` — `JTable` — `table`
- [ ] `com.mx.kylgis.pos.inventory.LocationsView#jLabel2` — `JLabel` — `setting`
- [ ] `com.mx.kylgis.pos.inventory.ProductsEditor#jLabel1` — `JLabel` — `setting`
- [ ] `com.mx.kylgis.pos.inventory.ProductsEditor#jTabbedPane1` — `JTabbedPane` — `setting`
- [ ] `com.mx.kylgis.pos.inventory.StockManagement#jTextField1` — `JTextField` — `setting`
- [ ] `com.mx.kylgis.pos.inventory.StockManagement#m_jList` — `JButton` — `list`
- [ ] `com.mx.kylgis.pos.inventory.TaxCategoriesEditor#jLabel2` — `JLabel` — `setting`
- [ ] `com.mx.kylgis.pos.inventory.TaxCustCategoriesEditor#jLabel2` — `JLabel` — `setting`
- [ ] `com.mx.kylgis.pos.inventory.TaxEditor#jLabel2` — `JLabel` — `setting`
- [ ] `com.mx.kylgis.pos.inventory.UomEditor#jLabel2` — `JLabel` — `setting`
- [ ] `com.mx.kylgis.pos.inventory.transform.ProductUomConversionPanel#table` — `JTable` — `table`
- [ ] `com.mx.kylgis.pos.panels.JProductFinder#jButton3` — `JButton` — `setting`
- [ ] `com.mx.kylgis.pos.printer.javapos.DeviceFiscalPrinterJavaPOS#jButton1` — `JButton` — `setting`
- [ ] `com.mx.kylgis.pos.printer.javapos.DeviceFiscalPrinterJavaPOS#jButton2` — `JButton` — `setting`
- [ ] `com.mx.kylgis.pos.reports.JPanelReport#jButton1` — `JButton` — `setting`
- [ ] `com.mx.kylgis.pos.reports.JParamsCustomer#jTextField1` — `JTextField` — `setting`
- [ ] `com.mx.kylgis.pos.reports.JParamsSuppliers#jTextField1` — `JTextField` — `setting`
- [ ] `com.mx.kylgis.pos.reports.JParamsText#txtField` — `JTextField` — `field`
- [ ] `com.mx.kylgis.pos.reports.JParamsUser#jTextField1` — `JTextField` — `setting`
- [ ] `com.mx.kylgis.pos.sales.JMooringDetails#jText` — `JTextField` — `text`
- [ ] `com.mx.kylgis.pos.sales.JPanelTicket#m_jList` — `JButton` — `list`
- [ ] `com.mx.kylgis.pos.sales.JTicketsBagTicket#jButton1` — `JButton` — `setting`
- [ ] `com.mx.kylgis.pos.sales.JTicketsBagTicket#jButton2` — `JButton` — `setting`
- [ ] `com.mx.kylgis.pos.sales.restaurant.JTicketsBagRestaurantRes#jButton1` — `JButton` — `setting`
- [ ] `com.mx.kylgis.pos.suppliers.SuppliersView#jLabel7` — `JLabel` — `setting`
- [ ] `com.mx.kylgis.pos.suppliers.SuppliersView#jTabbedPane1` — `JTabbedPane` — `setting`
