"""Human-reviewed context of weak NetBeans field names.

Only enrich fields with verified business context; an unknown jTextField1
must not be assigned a guessed purpose. Source ids are the stable
fully-qualified package.Class#field names in ui_help_catalog.properties.
"""
DOMAINS={
"admin.PeopleView":"user",
"admin.ResourcesView":"resource",
"admin.RolesView":"role",
"customers.CustomersPayment":"customer",
"customers.CustomersView":"customer",
"customers.JDialogNewCustomer":"customer",
"inventory.AttributeSetsEditor":"attribute set",
"inventory.AttributesEditor":"attribute",
"inventory.CategoriesEditor":"category",
"inventory.LocationsView":"location",
"inventory.MaterialFilter":"material",
"inventory.ProductsEditor":"product",
"inventory.TaxCategoriesEditor":"tax category",
"inventory.TaxCustCategoriesEditor":"customer tax category",
"inventory.TaxEditor":"tax",
"inventory.UomEditor":"unit",
"mant.FloorsEditor":"floor",
"mant.PlacesEditor":"table",
"payment.JPaymentDebt":"debtor",
"suppliers.JDialogNewSupplier":"supplier",
"suppliers.SuppliersView":"supplier",
"ticket.ProductFilter":"product",
"ticket.ProductFilter1":"product",
"ticket.ProductFilter2":"product",
"ticket.ProductFilter3":"product",
"ticket.ProductFilter4":"product",
}
# No automatic edits to resources, role scripts, fiscal commands, passwords.
# The explicit suffix chooses only classes for which field context was verified.
CONTEXTS={"com.mx.kylgis.pos."+owner:domain for owner,domain in DOMAINS.items()}
