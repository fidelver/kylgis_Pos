"""Localized semantic domains for unnamed NetBeans text/list controls."""
KEYS=("user","resource","role","customer","attribute set","attribute",
"category","location","material","product","tax category",
"customer tax category","tax","unit","floor","table","debtor","supplier")
NATIVE={
"base":("User","Resource","Role","Customer","Attribute set","Attribute",
"Category","Location","Material","Product","Tax category",
"Customer tax category","Tax","Unit of measure","Floor","Table","Debtor","Supplier"),
"al_SQ":("Përdoruesi","Burimi","Roli","Klienti","Grupi i atributeve","Atributi",
"Kategoria","Vendndodhja","Materiali","Produkti","Kategoria e taksave",
"Kategoria tatimore e klientit","Taksa","Njësia matëse","Kati","Tavolina","Debitori","Furnizuesi"),
"ar":("المستخدم","المورد البرمجي","الدور","العميل","مجموعة السمات","السمة",
"الفئة","الموقع","المادة","المنتج","فئة الضرائب",
"فئة ضريبة العميل","الضريبة","وحدة القياس","الطابق","الطاولة","المدين","المورد"),
"da":("Bruger","Ressource","Rolle","Kunde","Attributsæt","Attribut",
"Kategori","Placering","Materiale","Produkt","Momskategori",
"Kundens momskategori","Moms","Måleenhed","Etage","Bord","Skyldner","Leverandør"),
"de":("Benutzer","Ressource","Rolle","Kunde","Attributsatz","Attribut",
"Kategorie","Standort","Material","Produkt","Steuerkategorie",
"Kundensteuerkategorie","Steuer","Maßeinheit","Etage","Tisch","Schuldner","Lieferant"),
"el":("Χρήστης","Πόρος","Ρόλος","Πελάτης","Σύνολο χαρακτηριστικών","Χαρακτηριστικό",
"Κατηγορία","Τοποθεσία","Υλικό","Προϊόν","Φορολογική κατηγορία",
"Φορολογική κατηγορία πελάτη","Φόρος","Μονάδα μέτρησης","Όροφος","Τραπέζι","Οφειλέτης","Προμηθευτής"),
"es":("Usuario","Recurso","Rol","Cliente","Conjunto de atributos","Atributo",
"Categoría","Ubicación","Material","Producto","Categoría fiscal",
"Categoría fiscal de cliente","Impuesto","Unidad de medida","Piso","Mesa","Deudor","Proveedor"),
"et":("Kasutaja","Ressurss","Roll","Klient","Atribuutide kogum","Atribuut",
"Kategooria","Asukoht","Materjal","Toode","Maksukategooria",
"Kliendi maksukategooria","Maks","Mõõtühik","Korrus","Laud","Võlgnik","Tarnija"),
"fr":("Utilisateur","Ressource","Rôle","Client","Jeu d'attributs","Attribut",
"Catégorie","Emplacement","Matière","Produit","Catégorie fiscale",
"Catégorie fiscale du client","Taxe","Unité de mesure","Étage","Table","Débiteur","Fournisseur"),
"hr":("Korisnik","Resurs","Uloga","Kupac","Skup atributa","Atribut",
"Kategorija","Lokacija","Materijal","Proizvod","Porezna kategorija",
"Porezna kategorija kupca","Porez","Mjerna jedinica","Kat","Stol","Dužnik","Dobavljač"),
"it":("Utente","Risorsa","Ruolo","Cliente","Insieme di attributi","Attributo",
"Categoria","Posizione","Materiale","Prodotto","Categoria fiscale",
"Categoria fiscale cliente","Imposta","Unità di misura","Piano","Tavolo","Debitore","Fornitore"),
"nl":("Gebruiker","Bron","Rol","Klant","Attribuutgroep","Attribuut",
"Categorie","Locatie","Materiaal","Product","Belastingcategorie",
"Belastingcategorie klant","Belasting","Maateenheid","Verdieping","Tafel","Schuldenaar","Leverancier"),
"pt":("Utilizador","Recurso","Função","Cliente","Conjunto de atributos","Atributo",
"Categoria","Localização","Material","Produto","Categoria fiscal",
"Categoria fiscal do cliente","Imposto","Unidade de medida","Piso","Mesa","Devedor","Fornecedor"),
"pt_BR":("Usuário","Recurso","Função","Cliente","Conjunto de atributos","Atributo",
"Categoria","Localização","Material","Produto","Categoria fiscal",
"Categoria fiscal do cliente","Imposto","Unidade de medida","Andar","Mesa","Devedor","Fornecedor")
}
NATIVE["en_US"]=NATIVE["base"]
NATIVE["es_AR"]=NATIVE["es"]
NATIVE["es_MX"]=NATIVE["es"]
assert len(NATIVE)==17
assert all(len(values)==len(KEYS) for values in NATIVE.values())
TRANSLATIONS={locale:dict(zip(KEYS,values)) for locale,values in NATIVE.items()}
