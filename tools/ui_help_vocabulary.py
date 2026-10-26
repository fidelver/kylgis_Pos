"""Reviewed contextual glossary in every existing POS locale.
Legacy pos_messages contains misleading aliases (Product = Barcode) and
nontranslated regional keys. These entries take precedence for UI help only.
"""
TERMS=("product","barcode","category","name","supplier","reference","customer",
       "tax","ticket","date","role","location","price","code","amount","scale")
VOCAB={
"base":("Product","Barcode","Category","Name","Supplier","Reference","Customer",
"Tax","Receipt","Date","Role","Location","Price","Code","Amount","Scale"),
"al_SQ":("Produkti","Barkodi","Kategoria","Emri","Furnizuesi","Referenca","Klienti",
"Tatimi","Kuponi","Data","Roli","Vendndodhja","Çmimi","Kodi","Shuma","Peshorja"),
"ar":("المنتج","الباركود","الفئة","الاسم","المورد","المرجع","العميل",
"الضريبة","الإيصال","التاريخ","الدور","الموقع","السعر","الرمز","المبلغ","الميزان"),
"da":("Produkt","Stregkode","Kategori","Navn","Leverandør","Reference","Kunde",
"Moms","Kvittering","Dato","Rolle","Placering","Pris","Kode","Beløb","Vægt"),
"de":("Produkt","Barcode","Kategorie","Name","Lieferant","Referenz","Kunde",
"Steuer","Bon","Datum","Rolle","Standort","Preis","Code","Betrag","Waage"),
"el":("Προϊόν","Γραμμωτός κώδικας","Κατηγορία","Όνομα","Προμηθευτής","Αναφορά","Πελάτης",
"Φόρος","Απόδειξη","Ημερομηνία","Ρόλος","Τοποθεσία","Τιμή","Κωδικός","Ποσό","Ζυγαριά"),
"es":("Producto","Código de barras","Categoría","Nombre","Proveedor","Referencia","Cliente",
"Impuesto","Ticket","Fecha","Rol","Ubicación","Precio","Código","Importe","Báscula"),
"et":("Toode","Vöötkood","Kategooria","Nimi","Tarnija","Viide","Klient",
"Maks","Kviitung","Kuupäev","Roll","Asukoht","Hind","Kood","Summa","Kaal"),
"fr":("Produit","Code-barres","Catégorie","Nom","Fournisseur","Référence","Client",
"Taxe","Reçu","Date","Rôle","Emplacement","Prix","Code","Montant","Balance"),
"hr":("Proizvod","Crtični kod","Kategorija","Naziv","Dobavljač","Referenca","Kupac",
"Porez","Račun","Datum","Uloga","Lokacija","Cijena","Kod","Iznos","Vaga"),
"it":("Prodotto","Codice a barre","Categoria","Nome","Fornitore","Riferimento","Cliente",
"Imposta","Ricevuta","Data","Ruolo","Posizione","Prezzo","Codice","Importo","Bilancia"),
"nl":("Product","Barcode","Categorie","Naam","Leverancier","Referentie","Klant",
"Belasting","Bon","Datum","Rol","Locatie","Prijs","Code","Bedrag","Weegschaal"),
"pt":("Produto","Código de barras","Categoria","Nome","Fornecedor","Referência","Cliente",
"Imposto","Recibo","Data","Função","Localização","Preço","Código","Montante","Balança"),
"pt_BR":("Produto","Código de barras","Categoria","Nome","Fornecedor","Referência","Cliente",
"Imposto","Comprovante","Data","Função","Localização","Preço","Código","Valor","Balança")
}
VOCAB["en_US"]=VOCAB["base"]
VOCAB["es_AR"]=VOCAB["es"]
VOCAB["es_MX"]=VOCAB["es"]
assert len(VOCAB)==17
assert all(len(v)==len(TERMS) for v in VOCAB.values())
TRANSLATIONS={loc:dict(zip(TERMS,values)) for loc,values in VOCAB.items()}
