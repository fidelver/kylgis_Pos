"""Curated action descriptions for controls whose visible caption is ambiguous.

The mapping is identity-based (fully qualified class + field), never based on
runtime captions or user data. English and Spanish are explicit; locales not
listed inherit the English operational description until a native translation
is added.
"""

ACTIONS = {
    "confirm_person": (
        "Confirm the selected person and close the finder.",
        "Confirma la persona seleccionada y cierra el buscador.",
    ),
    "confirm_employee": (
        "Confirm the selected employee and close the finder.",
        "Confirma el empleado seleccionado y cierra el buscador.",
    ),
    "confirm_customer": (
        "Confirm the selected customer and close the finder.",
        "Confirma el cliente seleccionado y cierra el buscador.",
    ),
    "confirm_supplier": (
        "Confirm the selected supplier and close the finder.",
        "Confirma el proveedor seleccionado y cierra el buscador.",
    ),
    "select_customer": (
        "Select the customer associated with the ticket or record.",
        "Selecciona el cliente asociado con el ticket o registro.",
    ),
    "select_import_column": (
        "Select the source column that supplies this customer field during import.",
        "Selecciona la columna de origen que proporcionará este dato del cliente durante la importación.",
    ),
    "confirm_product": (
        "Confirm the selected product and return it to the current form.",
        "Confirma el producto seleccionado y lo devuelve al formulario actual.",
    ),
    "confirm_ticket": (
        "Confirm the selected ticket to open it for consultation or reprinting.",
        "Confirma el ticket seleccionado para consultarlo o reimprimirlo.",
    ),
    "confirm_guests": (
        "Confirm the number of guests and apply it to the current ticket.",
        "Confirma la cantidad de comensales y la aplica al ticket actual.",
    ),
    "confirm_number": (
        "Confirm the numeric value entered in the dialog.",
        "Confirma el valor numérico capturado en el diálogo.",
    ),
    "change_password": (
        "Save the new password and close the password dialog.",
        "Guarda la nueva contraseña y cierra el diálogo de contraseña.",
    ),
    "save_nodes": (
        "Save the node configuration used by this POS instance.",
        "Guarda la configuración de nodos utilizada por esta instancia del POS.",
    ),
    "save_config": (
        "Save the configuration changes made in this panel.",
        "Guarda los cambios de configuración realizados en este panel.",
    ),
    "save_customer": (
        "Save the new customer record and close the customer dialog.",
        "Guarda el nuevo cliente y cierra el diálogo de clientes.",
    ),
    "apply_search": (
        "Apply the current search and return the matching records.",
        "Aplica la búsqueda actual y devuelve los registros coincidentes.",
    ),
    "confirm_list": (
        "Confirm the selected list item and return it to the current form.",
        "Confirma el elemento seleccionado de la lista y lo devuelve al formulario actual.",
    ),
    "ack_message": (
        "Acknowledge the message and close this information dialog.",
        "Acepta el mensaje y cierra este diálogo informativo.",
    ),
    "new_resource": (
        "Start a new resource record in the editor.",
        "Inicia un nuevo registro de recurso en el editor.",
    ),
    "save_resource": (
        "Save the current resource and its contents.",
        "Guarda el recurso actual y su contenido.",
    ),
    "apply_sort": (
        "Apply the selected sort order and close the dialog.",
        "Aplica el orden seleccionado y cierra el diálogo.",
    ),
    "save_sat_product": (
        "Save the SAT metadata associated with this product.",
        "Guarda los metadatos SAT asociados con este producto.",
    ),
    "save_sat_tax": (
        "Save the SAT tax profile configured in this dialog.",
        "Guarda el perfil fiscal SAT configurado en este diálogo.",
    ),
    "save_supplier_relation": (
        "Save the supplier relationship for the selected product.",
        "Guarda la relación con el proveedor del producto seleccionado.",
    ),
    "import_products": (
        "Confirm the product import using the configured file and mappings.",
        "Confirma la importación de productos con el archivo y las correspondencias configuradas.",
    ),
    "save_uom_conversion": (
        "Save the product unit conversion and its direction.",
        "Guarda la conversión de unidades del producto y su orientación.",
    ),
    "accept_product": (
        "Accept the selected product for the recipe.",
        "Acepta el producto seleccionado para la receta.",
    ),
    "save_recipe": (
        "Save the recipe draft with its yield and components.",
        "Guarda el borrador de receta con su rendimiento y componentes.",
    ),
    "confirm_payment": (
        "Confirm the selected payment method for the current transaction.",
        "Confirma el método de pago seleccionado para la transacción actual.",
    ),
    "save_attributes": (
        "Save the selected product attributes and return to the sale.",
        "Guarda los atributos del producto seleccionado y vuelve a la venta.",
    ),
    "save_line": (
        "Save the edited product line and return to the ticket.",
        "Guarda la línea de producto editada y vuelve al ticket.",
    ),
    "save_table_map": (
        "Save the restaurant table assignment and layout changes.",
        "Guarda la asignación de mesas y los cambios de distribución.",
    ),
    "run_report": (
        "Run the report with the selected filters and date range.",
        "Ejecuta el reporte con los filtros y el intervalo de fechas seleccionados.",
    ),
    "cancel_dialog": (
        "Cancel this dialog without applying changes.",
        "Cancela este diálogo sin aplicar cambios.",
    ),
    "exit_app": (
        "Close the POS application.",
        "Cierra la aplicación KylGis POS.",
    ),
    "close_cash": (
        "Close the cash register panel without starting another operation.",
        "Cierra el panel de cierre de caja sin iniciar otra operación.",
    ),
    "cancel_ticket": (
        "Cancel the current ticket without finalizing the sale.",
        "Cancela el ticket actual sin finalizar la venta.",
    ),
    "close_reprint": (
        "Close the ticket reprint list.",
        "Cierra la lista de reimpresión de tickets.",
    ),
    "cancel_split": (
        "Cancel the ticket split and return to the current sale.",
        "Cancela la división del ticket y vuelve a la venta actual.",
    ),
    "cancel_printer": (
        "Close printer selection without changing the destination.",
        "Cierra la selección de impresora sin cambiar el destino.",
    ),
    "select_product": (
        "Select the product that will be used by this operation.",
        "Selecciona el producto que utilizará esta operación.",
    ),
    "select_category": (
        "Select the category used to filter or classify the records.",
        "Selecciona la categoría utilizada para filtrar o clasificar los registros.",
    ),
    "select_uom": (
        "Select the unit of measure used by this conversion or recipe.",
        "Selecciona la unidad de medida utilizada por esta conversión o receta.",
    ),
    "select_printer": (
        "Select the printer that will receive the document or kitchen order.",
        "Selecciona la impresora que recibirá el documento o la comanda de cocina.",
    ),
    "select_supplier": (
        "Select the supplier associated with the product or purchase.",
        "Selecciona el proveedor asociado con el producto o la compra.",
    ),
    "select_tax": (
        "Select the tax configuration applied to the product.",
        "Selecciona la configuración fiscal aplicada al producto.",
    ),
    "select_location": (
        "Select the inventory location used for this movement or production.",
        "Selecciona la ubicación de inventario utilizada por este movimiento o producción.",
    ),
    "filter_text": (
        "Enter the text used to filter the records shown by this panel.",
        "Escribe el texto que utilizará este panel para filtrar los registros.",
    ),
    "filter_code": (
        "Enter a code or reference to locate the matching product.",
        "Escribe un código o referencia para localizar el producto coincidente.",
    ),
    "filter_price": (
        "Enter the price criterion used to filter the product list.",
        "Escribe el criterio de precio que utilizará la lista de productos.",
    ),
    "barcode": (
        "Read or enter the barcode used to locate the product.",
        "Lee o escribe el código de barras para localizar el producto.",
    ),
    "numeric_key": (
        "Enter this digit into the active amount or quantity.",
        "Introduce este dígito en el importe o cantidad activa.",
    ),
    "barcode_button": (
        "Read a barcode and use it to locate the product.",
        "Lee un código de barras y lo utiliza para localizar el producto.",
    ),
    "product_id_button": (
        "Open product lookup using the product identifier.",
        "Abre la búsqueda de productos utilizando el identificador del producto.",
    ),
    "copy_identifier": (
        "Double-click this label to copy the record's internal identifier.",
        "Da doble clic en esta etiqueta para copiar el identificador interno del registro.",
    ),
    "ticket_search": (
        "Open ticket search to locate and load a ticket for consultation or reprinting.",
        "Abre el buscador de tickets para localizar y cargar un ticket para consultarlo o reimprimirlo.",
    ),
    "reservation_customer": (
        "Open customer search and associate the selected customer with this reservation.",
        "Abre la búsqueda de clientes y asocia el cliente seleccionado con esta reservación.",
    ),
    "report_customer_filter": (
        "Enter or select the customer used to filter this report.",
        "Escribe o selecciona el cliente que utilizará este reporte como filtro.",
    ),
    "report_supplier_filter": (
        "Enter or select the supplier used to filter this report.",
        "Escribe o selecciona el proveedor que utilizará este reporte como filtro.",
    ),
    "report_user_filter": (
        "Enter or select the user used to filter this report.",
        "Escribe o selecciona el usuario que utilizará este reporte como filtro.",
    ),
    "ticket_header": (
        "Enter the text that will be printed on this ticket header line.",
        "Escribe el texto que se imprimirá en esta línea del encabezado del ticket.",
    ),
    "ticket_footer": (
        "Enter the text that will be printed on this ticket footer line.",
        "Escribe el texto que se imprimirá en esta línea del pie del ticket.",
    ),
    "jpos_display_name": (
        "Enter the JPOS display device name used by this POS terminal.",
        "Escribe el nombre del dispositivo de pantalla JPOS utilizado por esta terminal.",
    ),
    "jpos_drawer": (
        "Enter the JPOS cash-drawer device name associated with this printer.",
        "Escribe el nombre del dispositivo JPOS del cajón de efectivo asociado con esta impresora.",
    ),
    "erp_organization": (
        "Enter the ERP organization identifier used by this POS.",
        "Escribe el identificador de la organización ERP que utilizará este POS.",
    ),
    "erp_pos": (
        "Enter the ERP point-of-sale identifier used by this terminal.",
        "Escribe el identificador del punto de venta ERP utilizado por esta terminal.",
    ),
    "stock_keypad_input": (
        "Receive scanner or keypad input used to add a product to the stock movement.",
        "Recibe la entrada del escáner o teclado para agregar un producto al movimiento de inventario.",
    ),
    "tabbed_section": (
        "Open a tab to access the settings or data provided by that section.",
        "Abre una pestaña para acceder a la configuración o los datos de esa sección.",
    ),
    "catalog_button": (
        "Open the catalog category or product represented by this button.",
        "Abre la categoría o el producto del catálogo representado por este botón.",
    ),
    "menu_button": (
        "Open the POS panel or operation represented by this menu button.",
        "Abre el panel o la operación del POS representada por este botón del menú.",
    ),
    "cash_preset": (
        "Add this preset amount to the cash payment being entered.",
        "Agrega este importe predefinido al pago en efectivo que se está capturando.",
    ),
    "sale_function_button": (
        "Run the configured sale action or print the ticket assigned to this button.",
        "Ejecuta la acción de venta configurada o imprime el ticket asignado a este botón.",
    ),
    "input_phone": (
        "Enter the telephone number used to contact this person or supplier.",
        "Escribe el número telefónico para contactar a esta persona o proveedor.",
    ),
    "input_email": (
        "Enter the email address used for notices or contact.",
        "Escribe el correo electrónico para avisos o contacto.",
    ),
    "input_first_name": (
        "Enter the person's first name.",
        "Escribe el nombre de pila de la persona.",
    ),
    "input_last_name": (
        "Enter the person's last name.",
        "Escribe los apellidos de la persona.",
    ),
    "input_notes": (
        "Enter notes that will be saved with this record or operation.",
        "Escribe las notas que se guardarán con este registro u operación.",
    ),
    "input_address": (
        "Enter the address associated with this record.",
        "Escribe la dirección asociada con este registro.",
    ),
    "input_visibility": (
        "Enable this option to keep the record available for selection.",
        "Activa esta opción para mantener disponible el registro en los selectores.",
    ),
    "input_credit_limit": (
        "Enter the maximum credit or debt allowed for this account.",
        "Escribe el límite máximo de crédito o deuda permitido para esta cuenta.",
    ),
    "input_current_debt": (
        "Enter the current outstanding balance for this account.",
        "Escribe el saldo pendiente actual de esta cuenta.",
    ),
    "input_debt_date": (
        "Enter the date on which the account balance was updated.",
        "Escribe la fecha en que se actualizó el saldo de la cuenta.",
    ),
    "input_record_date": (
        "Enter the effective date for this record or inventory movement.",
        "Escribe la fecha efectiva de este registro o movimiento de inventario.",
    ),
    "input_card": (
        "Enter the card or identifier number assigned to this user or customer.",
        "Escribe el número de tarjeta o identificador asignado al usuario o cliente.",
    ),
    "input_file": (
        "Choose the file that supplies the records for this import.",
        "Elige el archivo que contiene los registros de esta importación.",
    ),
    "search_query": (
        "Enter the text used to find matching records in this dialog.",
        "Escribe el texto que se utilizará para encontrar registros coincidentes en este diálogo.",
    ),
    "select_reason": (
        "Select the reason that explains this inventory or payment movement.",
        "Selecciona el motivo que explica este movimiento de inventario o pago.",
    ),
    "select_sort": (
        "Select the field and direction used to order the displayed records.",
        "Selecciona el campo y la dirección que ordenarán los registros mostrados.",
    ),
    "select_schema": (
        "Select the database schema that this connection or transfer will use.",
        "Selecciona el esquema de base de datos que utilizará esta conexión o transferencia.",
    ),
    "select_attribute": (
        "Select the attribute set or value that will be applied to the product.",
        "Selecciona el conjunto o valor de atributos que se aplicará al producto.",
    ),
    "recipe_notes": (
        "Enter an internal note for this recipe or production batch.",
        "Escribe una nota interna para esta receta o lote de producción.",
    ),
    "reverse_transformation": (
        "Reverse the selected inventory transformation and restore its source quantities.",
        "Revierte la transformación de inventario seleccionada y restaura las cantidades de origen.",
    ),
    "recipe_history_table": (
        "Select a transformation record to inspect its ingredients, result and traceability.",
        "Selecciona una transformación para consultar sus ingredientes, resultado y trazabilidad.",
    ),
    "edit_ticket": (
        "Open the selected ticket line or record for editing.",
        "Abre la línea o el registro seleccionado para editarlo.",
    ),
    "refund_ticket": (
        "Start a refund for the selected ticket.",
        "Inicia la devolución del ticket seleccionado.",
    ),
    "print_ticket": (
        "Print the selected ticket using the configured printer.",
        "Imprime el ticket seleccionado con la impresora configurada.",
    ),
    "show_calendar": (
        "Show or hide the calendar used to choose the reservation date.",
        "Muestra u oculta el calendario para elegir la fecha de la reservación.",
    ),
    "receive_reservation": (
        "Mark the selected reservation as received by the restaurant.",
        "Marca la reservación seleccionada como recibida por el restaurante.",
    ),
    "show_tables": (
        "Open the table map to assign a table to the reservation.",
        "Abre el mapa de mesas para asignar una mesa a la reservación.",
    ),
    "edit_table_layout": (
        "Open the editor for the restaurant table layout.",
        "Abre el editor de la distribución de mesas del restaurante.",
    ),
    "move_table": (
        "Move the selected ticket to another restaurant table.",
        "Mueve el ticket seleccionado a otra mesa del restaurante.",
    ),
    "table_plan": (
        "Open the restaurant table plan.",
        "Abre el plano de mesas del restaurante.",
    ),
    "hold_ticket": (
        "Hold the current ticket without finalizing the sale.",
        "Pone en espera el ticket actual sin finalizar la venta.",
    ),
    "new_ticket": (
        "Create a new sale ticket.",
        "Crea un nuevo ticket de venta.",
    ),
    "list_tickets": (
        "Open the list of saved tickets to retrieve one.",
        "Abre la lista de tickets guardados para recuperar uno.",
    ),
    "toggle_stock": (
        "Show or hide the stock controls for the current sale.",
        "Muestra u oculta los controles de existencias de la venta actual.",
    ),
    "add_refund_line": (
        "Add the selected ticket line to the refund.",
        "Agrega la línea seleccionada del ticket a la devolución.",
    ),
    "add_refund_all": (
        "Add all ticket lines to the refund.",
        "Agrega todas las líneas del ticket a la devolución.",
    ),
}

def _owner(identity):
    return identity.rsplit("#", 1)[0].lower()

def _field(identity):
    return identity.rsplit("#", 1)[-1].lower()

def action_for(identity):
    owner = _owner(identity)
    field = _field(identity)

    if field in {"jcmdcancel", "m_jbtncancel", "m_jbtncancel1", "m_jbuttoncancel", "m_jcancel", "close"}:
        if "jrootapp" in owner:
            return "exit_app"
        if "panelclosemoney" in owner:
            return "close_cash"
        if "ticketsbagticketbag" in owner:
            return "cancel_ticket"
        if "ticketsreprintlist" in owner:
            return "close_reprint"
        if "receiptsplit" in owner:
            return "cancel_split"
        if "selectprinter" in owner:
            return "cancel_printer"
        return "cancel_dialog"

    if field == "jcmdok":
        if "jpeoplefinder" in owner:
            return "confirm_person"
        if "jemployeefinder" in owner:
            return "confirm_employee"
        if "jcustomerfinder" in owner:
            return "confirm_customer"
        if "jsupplierfinder" in owner:
            return "confirm_supplier"
        if "jproductfinder" in owner:
            return "confirm_product"
        if "jticketsfinder" in owner:
            return "confirm_ticket"
        if "jfind" in owner:
            return "apply_search"
        if "jsort" in owner:
            return "apply_sort"
        if "jdlguploadproducts" in owner:
            return "import_products"
        if "jdialogreportpanel" in owner:
            return "run_report"
        if "jmessagedialog" in owner:
            return "ack_message"
        if "jpassworddialog" in owner or "jdlgchangepassword" in owner:
            return "change_password"
        return "confirm_list"

    if field in {"m_jbuttonok", "m_jbtnok"}:
        if "jpaymentselect" in owner:
            return "confirm_payment"
        if "jproductattedit" in owner:
            return "save_attributes"
        if "jproductlineedit" in owner:
            return "save_line"
        if "jdialognewcustomer" in owner:
            return "save_customer"
        if "jdialognewsupplier" in owner:
            return "save_supplier_relation"
        return "confirm_list"

    if field == "savebutton":
        return "save_nodes" if "panelconfignodes" in owner else "save_config"
    if field == "jbtnsave":
        if "jticketsbagrestaurantmap" in owner:
            return "save_table_map"
        if "jsaver" in owner:
            return "save_resource"
    if field == "accept" and "recipemanagement" in owner:
        return "accept_product"

    if field == "save":
        if "satproductmetadata" in owner:
            return "save_sat_product"
        if "sattaxprofile" in owner:
            return "save_sat_tax"
        if "jdialogproductsuppliers" in owner:
            return "save_supplier_relation"
        if "productuomconversion" in owner:
            return "save_uom_conversion"
        if "recipemanagement" in owner:
            return "accept_product"
        return "save_config"
    if field == "jbtnnew" and "jsaver" in owner:
        return "new_resource"
    if field == "savedraft" and "recipemanagement" in owner:
        return "save_recipe"

    if field in {"m_jenter", "m_jenter1", "m_jenter2"}:
        if "stockdiaryeditor" in owner and field == "m_jenter":
            return "barcode_button"
        if "stockdiaryeditor" in owner and field == "m_jenter1":
            return "product_id_button"
        if field.endswith("2"):
            return "barcode_button"
        return "select_product"

    if "jeditorkeys" in owner and field.startswith("m_jkey"):
        return "numeric_key"

    # Catalog, inventory and ticket filters: use the field identity so a
    # generic owner name such as CustomersView cannot mislabel a category
    # selector as a customer selector.
    if any(token in field for token in ("printer", "jprinters", "receiptprinterroute")):
        return "select_printer"
    if any(token in field for token in ("supplier", "provsupplier")):
        return "select_supplier"
    if any(token in field for token in ("tax", "impuesto")):
        return "select_tax"
    if any(token in field for token in ("uom", "unit", "units")):
        return "select_uom"
    if any(token in field for token in ("location", "warehouse", "stocklocation")):
        return "select_location"
    if "category" in field:
        return "select_category"
    if ("customercsvimport" in owner or "panelcsvimport" in owner) and field.startswith("jcombo"):
        return "select_import_column"
    if "customer" in field or "jcbcustomer" in field:
        return "select_customer"
    if any(token in field for token in ("barcode", "barcod")):
        return "barcode"
    if any(token in field for token in ("pricebuy", "pricesell", "price")) and (
            "filter" in owner or "voucher" in owner or "materialfilter" in owner):
        return "filter_price"
    if any(token in field for token in ("reference", "searchkey", "code")):
        return "filter_code"
    if any(token in field for token in ("product", "jcboproduct", "cboname")):
        return "select_product"
    if "search" in field or "filter" in field:
        return "filter_text"

    if field in {"txtphone", "txtphone2"}:
        return "input_phone"
    if field == "txtemail":
        return "input_email"
    if field == "txtfirstname":
        return "input_first_name"
    if field == "txtlastname":
        return "input_last_name"
    if field in {"m_jnotes", "notes"}:
        return "recipe_notes" if "recipe" in owner else "input_notes"
    if field in {"txtaddress", "txtaddress2", "m_jaddress"}:
        return "input_address"
    if field in {"m_jvisible", "jcheckvisible"}:
        return "input_visibility"
    if field in {"txtmaxdebt", "maxdebt"}:
        return "input_credit_limit"
    if field in {"txtcurdebt", "curdebt"}:
        return "input_current_debt"
    if field in {"txtcurdate", "curdate"}:
        return "input_debt_date"
    if field in {"m_jdate", "jdate"}:
        return "input_record_date"
    if field in {"m_jcard", "jcard", "txtcard"}:
        return "input_card"
    if field in {"jfilename", "filename"}:
        return "input_file"
    if field in {"m_jfind", "find"}:
        return "search_query"
    if field in {"m_jreason", "reason"}:
        return "select_reason"
    if field.startswith("m_jsort") or field == "sort":
        return "select_sort"
    if field in {"jcbschema", "jcbschema1", "schema"}:
        return "select_schema"
    if field in {"m_jatt", "jvalues"}:
        return "select_attribute"
    if field == "m_jprintto" or field == "results" and "satproductmetadata" in owner:
        return "select_printer" if field == "m_jprintto" else "select_product"
    if ("productfilter" in owner or "materialfilter" in owner) and field == "m_jname":
        return "filter_text"
    if "transformationhistorypanel" in owner and field == "reverse":
        return "reverse_transformation"
    if "transformationhistorypanel" in owner and field in {"linetable", "transformationtable"}:
        return "recipe_history_table"
    if "salerecipeconsumptionpanel" in owner and field == "recipes":
        return "recipe_history_table"

    if field in {"m_jedit", "jedit", "edit"}:
        return "edit_ticket"
    if field in {"m_jrefund", "jrefund", "refund"}:
        return "refund_ticket"
    if field in {"m_jprint", "jprint", "print"}:
        return "print_ticket"
    if field in {"jbtnshowcalendar", "showcalendar"}:
        return "show_calendar"
    if field in {"m_jbtnreceive", "receive"}:
        return "receive_reservation"
    if field in {"m_jbtntables", "tables"}:
        return "show_tables"
    if field in {"m_jbtnlayout", "layout"}:
        return "edit_table_layout"
    if field in {"m_movetable", "movetable"}:
        return "move_table"
    if field in {"m_tableplan", "tableplan"}:
        return "table_plan"
    if field in {"m_jhold", "hold"}:
        return "hold_ticket"
    if field in {"m_jnewticket", "newticket"}:
        return "new_ticket"
    if field in {"m_jlisttickets", "listtickets"}:
        return "list_tickets"
    if field in {"jcheckstock", "stock"}:
        return "toggle_stock"
    if field in {"m_jbtnaddall", "addall"}:
        return "add_refund_all"
    if field in {"m_jbtnaddline", "m_jbtnaddone", "addline", "addone"}:
        return "add_refund_line"

    # Explicit controls whose generated caption is an opaque placeholder.
    if field in {"jlabel1", "jlabel2", "jlabel7"}:
        copy_owners = (
            "peopleview", "locationsview", "productseditor",
            "taxcategorieseditor", "taxcustcategorieseditor",
            "taxeditor", "uomeditor", "customersview", "suppliersview",
        )
        if any(token in owner for token in copy_owners):
            return "copy_identifier"
    if field == "jtabbedpane1" and any(token in owner for token in (
        "jpanelconfiguration", "productseditor", "customersview", "suppliersview",
    )):
        return "tabbed_section"
    if field.startswith("jtxttkth"):
        return "ticket_header"
    if field.startswith("jtxttktf"):
        return "ticket_footer"
    if field == "mjtxtjposname" or field == "m_jtxtjposname":
        return "jpos_display_name"
    if field.startswith("m_jtxtjposdrawer") or field.startswith("mjtxtjposdrawer"):
        return "jpos_drawer"
    if "jpanelconfigerp" in owner and field == "jtextfield1":
        return "erp_organization"
    if "jpanelconfigerp" in owner and field == "jtextfield2":
        return "erp_pos"
    if "stockmanagement" in owner and field == "jtextfield1":
        return "stock_keypad_input"
    if "jparamscustomer" in owner and field == "jtextfield1":
        return "report_customer_filter"
    if "jparamssuppliers" in owner and field == "jtextfield1":
        return "report_supplier_filter"
    if "jparamsuser" in owner and field == "jtextfield1":
        return "report_user_filter"
    if "jcatalogtab" in owner and field == "btn":
        return "catalog_button"
    if "jrootapp" in owner and field == "btn":
        return "menu_button"
    if "menuitemdefinition" in owner and field == "btn":
        return "menu_button"
    if "jpaymentcashpos" in owner and field == "btn":
        return "cash_preset"
    if "jpanelbuttons" in owner and field == "btn":
        return "sale_function_button"
    if "jticketsbagticket" in owner and field == "jbutton2":
        return "ticket_search"
    if "jticketsbagrestaurantres" in owner and field == "jbutton1":
        return "reservation_customer"
    return None

def text_for(locale, action):
    english, spanish = ACTIONS[action]
    return spanish if locale in {"es", "es_AR", "es_MX"} else english
