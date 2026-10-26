"""Reviewed static captions formerly hard-coded in Spanish.
Tuples match SOURCE_LABELS; every existing POS locale has a value.
Keep native terms, arrows, and protocol names stable. No user data is translated.
"""
SOURCE_LABELS=(
"Aplicar periféricos","Buscar","Buscar:","Cantidad libre","Cantidad:",
"Cargar reporte","Cerrar","Código de orden","Código de orden:","Examinar...",
"Exportar CSV","Generar ticket digital","Impresora lógica del recibo:",
"Limpiar visibles","Mostrar copia del recibo en pantalla","Plataforma:",
"Reporte generado","Salidas del ticket","Salidas espejo del recibo principal",
"Seleccionar visibles","Ver opciones ↓","Verificando…","↻ Refrescar"
)
BY_LOCALE={
"base":(
"Apply peripherals","Search","Search:","Custom quantity","Quantity:",
"Load report","Close","Order code","Order code:","Browse...",
"Export CSV","Generate digital receipt","Logical receipt printer:",
"Clear visible selections","Show a copy of the receipt on screen","Platform:",
"Report generated","Receipt outputs","Mirrored outputs of the primary receipt",
"Select visible items","Show options ↓","Verifying…","↻ Refresh"),
"al_SQ":(
"Zbato pajisjet periferike","Kërko","Kërko:","Sasi e lirë","Sasia:",
"Ngarko raportin","Mbyll","Kodi i porosisë","Kodi i porosisë:","Shfleto...",
"Eksporto CSV","Gjenero kuponin digjital","Printeri logjik i kuponit:",
"Pastro përzgjedhjet e dukshme","Shfaq një kopje të kuponit në ekran","Platforma:",
"Raporti u gjenerua","Daljet e kuponit","Daljet pasqyrë të kuponit kryesor",
"Zgjidh elementet e dukshme","Shfaq opsionet ↓","Po verifikohet…","↻ Rifresko"),
"ar":(
"تطبيق الأجهزة الطرفية","بحث","بحث:","كمية مخصصة","الكمية:",
"تحميل التقرير","إغلاق","رمز الطلب","رمز الطلب:","استعراض...",
"تصدير CSV","إنشاء إيصال رقمي","الطابعة المنطقية للإيصال:",
"مسح التحديدات الظاهرة","عرض نسخة من الإيصال على الشاشة","المنصة:",
"تم إنشاء التقرير","وجهات إخراج الإيصال","وجهات النسخ المطابقة للإيصال الرئيسي",
"تحديد العناصر الظاهرة","عرض الخيارات ↓","جارٍ التحقق…","↻ تحديث"),
"da":(
"Anvend eksterne enheder","Søg","Søg:","Valgfrit antal","Antal:",
"Indlæs rapport","Luk","Ordrekode","Ordrekode:","Gennemse...",
"Eksportér CSV","Opret digital kvittering","Logisk kvitteringsprinter:",
"Ryd synlige valg","Vis en kopi af kvitteringen på skærmen","Platform:",
"Rapport genereret","Kvitteringsudgange","Spejlede udgange for hovedkvitteringen",
"Vælg synlige elementer","Vis indstillinger ↓","Kontrollerer…","↻ Opdater"),
"de":(
"Peripheriegeräte übernehmen","Suchen","Suchen:","Freie Menge","Menge:",
"Bericht laden","Schließen","Bestellcode","Bestellcode:","Durchsuchen...",
"CSV exportieren","Digitalen Beleg erzeugen","Logischer Bondrucker:",
"Sichtbare Auswahl löschen","Bonkopie auf dem Bildschirm anzeigen","Plattform:",
"Bericht erstellt","Bonausgaben","Gespiegelte Ausgaben des Hauptbons",
"Sichtbare Elemente auswählen","Optionen anzeigen ↓","Wird überprüft…","↻ Aktualisieren"),
"el":(
"Εφαρμογή περιφερειακών","Αναζήτηση","Αναζήτηση:","Ελεύθερη ποσότητα","Ποσότητα:",
"Φόρτωση αναφοράς","Κλείσιμο","Κωδικός παραγγελίας","Κωδικός παραγγελίας:","Περιήγηση...",
"Εξαγωγή CSV","Δημιουργία ψηφιακής απόδειξης","Λογικός εκτυπωτής απόδειξης:",
"Εκκαθάριση ορατών επιλογών","Εμφάνιση αντιγράφου απόδειξης στην οθόνη","Πλατφόρμα:",
"Η αναφορά δημιουργήθηκε","Έξοδοι απόδειξης","Κατοπτρικές έξοδοι της κύριας απόδειξης",
"Επιλογή ορατών στοιχείων","Εμφάνιση επιλογών ↓","Γίνεται επαλήθευση…","↻ Ανανέωση"),
"es":SOURCE_LABELS,
"et":(
"Rakenda välisseadmed","Otsi","Otsi:","Vaba kogus","Kogus:",
"Laadi aruanne","Sulge","Tellimuse kood","Tellimuse kood:","Sirvi...",
"Ekspordi CSV","Loo digitaalne kviitung","Kviitungi loogiline printer:",
"Tühjenda nähtavad valikud","Kuva kviitungi koopia ekraanil","Platvorm:",
"Aruanne loodud","Kviitungi väljundid","Põhikviitungi peegelväljundid",
"Vali nähtavad elemendid","Näita valikuid ↓","Kontrollimine…","↻ Värskenda"),
"fr":(
"Appliquer les périphériques","Rechercher","Rechercher :","Quantité libre","Quantité :",
"Charger le rapport","Fermer","Code de commande","Code de commande :","Parcourir...",
"Exporter en CSV","Générer un reçu numérique","Imprimante logique du reçu :",
"Effacer les sélections visibles","Afficher une copie du reçu à l’écran","Plateforme :",
"Rapport généré","Sorties du reçu","Sorties miroir du reçu principal",
"Sélectionner les éléments visibles","Afficher les options ↓","Vérification…","↻ Actualiser"),
"hr":(
"Primijeni periferne uređaje","Pretraži","Pretraži:","Slobodna količina","Količina:",
"Učitaj izvještaj","Zatvori","Šifra narudžbe","Šifra narudžbe:","Pregledaj...",
"Izvezi CSV","Izradi digitalni račun","Logički pisač računa:",
"Očisti vidljive odabire","Prikaži kopiju računa na zaslonu","Platforma:",
"Izvještaj je izrađen","Izlazi računa","Zrcalni izlazi glavnog računa",
"Odaberi vidljive stavke","Prikaži mogućnosti ↓","Provjera u tijeku…","↻ Osvježi"),
"it":(
"Applica periferiche","Cerca","Cerca:","Quantità libera","Quantità:",
"Carica report","Chiudi","Codice ordine","Codice ordine:","Sfoglia...",
"Esporta CSV","Genera ricevuta digitale","Stampante logica della ricevuta:",
"Cancella selezioni visibili","Mostra una copia della ricevuta sullo schermo","Piattaforma:",
"Report generato","Uscite della ricevuta","Uscite duplicate della ricevuta principale",
"Seleziona elementi visibili","Mostra opzioni ↓","Verifica in corso…","↻ Aggiorna"),
"nl":(
"Randapparaten toepassen","Zoeken","Zoeken:","Vrije hoeveelheid","Hoeveelheid:",
"Rapport laden","Sluiten","Bestelcode","Bestelcode:","Bladeren...",
"CSV exporteren","Digitale bon genereren","Logische bonprinter:",
"Zichtbare selecties wissen","Een kopie van de bon op het scherm tonen","Platform:",
"Rapport aangemaakt","Bonuitvoer","Gespiegelde uitvoer van de hoofd­bon",
"Zichtbare items selecteren","Opties tonen ↓","Controleren…","↻ Vernieuwen"),
"pt":(
"Aplicar periféricos","Pesquisar","Pesquisar:","Quantidade livre","Quantidade:",
"Carregar relatório","Fechar","Código da encomenda","Código da encomenda:","Procurar...",
"Exportar CSV","Gerar recibo digital","Impressora lógica do recibo:",
"Limpar seleções visíveis","Mostrar cópia do recibo no ecrã","Plataforma:",
"Relatório gerado","Saídas do recibo","Saídas espelhadas do recibo principal",
"Selecionar elementos visíveis","Mostrar opções ↓","A verificar…","↻ Atualizar"),
"pt_BR":(
"Aplicar periféricos","Buscar","Buscar:","Quantidade livre","Quantidade:",
"Carregar relatório","Fechar","Código do pedido","Código do pedido:","Procurar...",
"Exportar CSV","Gerar comprovante digital","Impressora lógica do comprovante:",
"Limpar seleções visíveis","Mostrar cópia do comprovante na tela","Plataforma:",
"Relatório gerado","Saídas do comprovante","Saídas espelhadas do comprovante principal",
"Selecionar itens visíveis","Mostrar opções ↓","Verificando…","↻ Atualizar")
}
BY_LOCALE["en_US"]=BY_LOCALE["base"]
BY_LOCALE["es_AR"]=BY_LOCALE["es"]
BY_LOCALE["es_MX"]=BY_LOCALE["es"]
assert len(BY_LOCALE)==17
assert all(len(x)==len(SOURCE_LABELS) for x in BY_LOCALE.values())
TRANSLATIONS={original:{locale:BY_LOCALE[locale][i] for locale in BY_LOCALE}
              for i,original in enumerate(SOURCE_LABELS)}
