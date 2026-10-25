"""Native node configuration form labels and detailed field help."""
KEYS=("roles","modules","profile","dbserver","dbport","dbname",
"serverEffective","portEffective","dbEffective","master","moduleOrder",
"bootstrap","credentials")
LANG={
"base":("Roles","Reusable modules","Profile","Database server (override)",
"Database port (override)","Database schema","Effective server",
"Effective port","Effective database","MASTER","Modules (order)","Bootstrap","Credentials"),
"al_SQ":("Rolet","Module të ripërdorshme","Profili","Serveri i BD-së (mbishkrim)",
"Porta e BD-së (mbishkrim)","Skema e BD-së","Serveri efektiv",
"Porta efektive","BD efektive","MASTER","Modulet (renditja)","Nisja fillestare","Kredencialet"),
"ar":("الأدوار","الوحدات القابلة لإعادة الاستخدام","الملف التشغيلي","خادم قاعدة البيانات (تجاوز)",
"منفذ قاعدة البيانات (تجاوز)","مخطط قاعدة البيانات","الخادم الفعّال",
"المنفذ الفعّال","قاعدة البيانات الفعّالة","الرئيسي","الوحدات (الترتيب)","التهيئة الأولية","بيانات الاعتماد"),
"da":("Roller","Genanvendelige moduler","Profil","Databaseserver (tilsidesæt)",
"Databaseport (tilsidesæt)","Databaseskema","Effektiv server",
"Effektiv port","Effektiv database","MASTER","Moduler (rækkefølge)","Bootstrap","Loginoplysninger"),
"de":("Rollen","Wiederverwendbare Module","Profil","Datenbankserver (überschreiben)",
"Datenbankport (überschreiben)","Datenbankschema","Aktiver Server",
"Aktiver Port","Aktive Datenbank","MASTER","Module (Reihenfolge)","Bootstrap","Zugangsdaten"),
"el":("Ρόλοι","Επαναχρησιμοποιήσιμες μονάδες","Προφίλ","Διακομιστής βάσης (παράκαμψη)",
"Θύρα βάσης (παράκαμψη)","Σχήμα βάσης","Ενεργός διακομιστής",
"Ενεργή θύρα","Ενεργή βάση","MASTER","Μονάδες (σειρά)","Αρχική ρύθμιση","Διαπιστευτήρια"),
"es":("Funciones","Módulos reutilizables","Perfil","Servidor BBDD (override)",
"Puerto BBDD (override)","Base de datos","Servidor efectivo",
"Puerto efectivo","BBDD efectiva","MASTER","Módulos (orden)","Arranque inicial","Credenciales"),
"et":("Rollid","Taaskasutatavad moodulid","Profiil","Andmebaasiserver (ülekirjutus)",
"Andmebaasiport (ülekirjutus)","Andmebaasiskeem","Tegelik server",
"Tegelik port","Tegelik andmebaas","MASTER","Moodulid (järjestus)","Algseadistus","Sisselogimisandmed"),
"fr":("Rôles","Modules réutilisables","Profil","Serveur de base (remplacement)",
"Port de base (remplacement)","Schéma de la base","Serveur effectif",
"Port effectif","Base effective","MASTER","Modules (ordre)","Initialisation","Identifiants"),
"hr":("Uloge","Moduli za ponovnu uporabu","Profil","Poslužitelj baze (nadjačavanje)",
"Port baze (nadjačavanje)","Shema baze","Aktivni poslužitelj",
"Aktivni port","Aktivna baza","MASTER","Moduli (redoslijed)","Početne postavke","Prijavni podaci"),
"it":("Ruoli","Moduli riutilizzabili","Profilo","Server database (sovrascrivi)",
"Porta database (sovrascrivi)","Schema database","Server effettivo",
"Porta effettiva","Database effettivo","MASTER","Moduli (ordine)","Bootstrap","Credenziali"),
"nl":("Rollen","Herbruikbare modules","Profiel","Databaseserver (overschrijven)",
"Databasepoort (overschrijven)","Databaseschema","Effectieve server",
"Effectieve poort","Effectieve database","MASTER","Modules (volgorde)","Initiële configuratie","Inloggegevens"),
"pt":("Funções","Módulos reutilizáveis","Perfil","Servidor da BD (substituição)",
"Porta da BD (substituição)","Esquema da BD","Servidor efetivo",
"Porta efetiva","BD efetiva","MASTER","Módulos (ordem)","Inicialização","Credenciais"),
"pt_BR":("Funções","Módulos reutilizáveis","Perfil","Servidor do BD (substituição)",
"Porta do BD (substituição)","Esquema do BD","Servidor efetivo",
"Porta efetiva","BD efetivo","MASTER","Módulos (ordem)","Inicialização","Credenciais")
}
HELP={
"base":(
"Operating profile for this node; it does not change the POS application identity.",
"Leave this field empty to inherit the connection setting from MASTER.",
"Database schema assigned to this node."),
"al_SQ":(
"Profili i nyjës; nuk ndryshon identitetin e aplikacionit POS.",
"Lëreni bosh për të trashëguar cilësimin e lidhjes nga MASTER.",
"Skema e bazës së të dhënave të kësaj nyjeje."),
"ar":(
"الملف التشغيلي لهذه العقدة؛ لا يغير هوية تطبيق نقاط البيع.",
"اترك هذا الحقل فارغًا لوراثة إعداد الاتصال من العقدة الرئيسية MASTER.",
"مخطط قاعدة البيانات المخصص لهذه العقدة."),
"da":(
"Driftsprofil for noden; ændrer ikke POS-programmets identitet.",
"Lad feltet være tomt for at arve forbindelsen fra MASTER.",
"Databaseskema tildelt denne node."),
"de":(
"Betriebsprofil des Knotens; ändert nicht die Identität der POS-Anwendung.",
"Leer lassen, um die Verbindungseinstellung von MASTER zu übernehmen.",
"Datenbankschema, das diesem Knoten zugewiesen ist."),
"el":(
"Προφίλ λειτουργίας του κόμβου· δεν αλλάζει την ταυτότητα του POS.",
"Αφήστε το κενό για κληρονομιά των ρυθμίσεων σύνδεσης από το MASTER.",
"Σχήμα βάσης δεδομένων που έχει ανατεθεί στον κόμβο."),
"es":(
"Perfil operativo del nodo; no modifica la identidad de la aplicación POS.",
"Deje el campo vacío para heredar la conexión del nodo MASTER.",
"Nombre o esquema de base de datos asignado a este nodo."),
"et":(
"Sõlme tööprofiil; ei muuda POS-i rakenduse identiteeti.",
"Jätke tühjaks, et pärida ühenduse seadistus MASTER-ilt.",
"Sellele sõlmele määratud andmebaasiskeem."),
"fr":(
"Profil opérationnel du nœud ; ne modifie pas l’identité de l’application POS.",
"Laissez vide pour hériter du paramètre de connexion de MASTER.",
"Schéma de base de données attribué à ce nœud."),
"hr":(
"Operativni profil čvora; ne mijenja identitet POS aplikacije.",
"Ostavite prazno za nasljeđivanje postavke povezivanja od MASTER-a.",
"Shema baze podataka dodijeljena ovom čvoru."),
"it":(
"Profilo operativo del nodo; non modifica l’identità dell’applicazione POS.",
"Lascia vuoto per ereditare la connessione dal nodo MASTER.",
"Schema del database assegnato a questo nodo."),
"nl":(
"Operationeel knooppuntprofiel; wijzigt de POS-applicatie-identiteit niet.",
"Laat leeg om de verbindingsinstelling van MASTER over te nemen.",
"Databaseschema toegewezen aan dit knooppunt."),
"pt":(
"Perfil operacional do nó; não altera a identidade da aplicação POS.",
"Deixe vazio para herdar a configuração de ligação do MASTER.",
"Esquema de base de dados atribuído a este nó."),
"pt_BR":(
"Perfil operacional do nó; não altera a identidade do aplicativo POS.",
"Deixe vazio para herdar a conexão do nó MASTER.",
"Esquema do banco de dados atribuído a este nó.")
}
for m in (LANG,HELP):
 m["en_US"]=m["base"]
 m["es_AR"]=m["es"]
 m["es_MX"]=m["es"]
assert len(LANG)==len(HELP)==17
assert all(len(v)==len(KEYS) for v in LANG.values())
assert all(len(v)==3 for v in HELP.values())
TRANSLATIONS={
 loc:{
 **{"text.nodes.form."+key:label for key,label in zip(KEYS,LANG[loc])},
 **{"tooltip.nodes.form."+key:msg for key,msg in zip(("profile","inherit","dbname"),HELP[loc])}
 } for loc in LANG}


MODULE_MOVES={
"base":("Move the selected module one position up in the execution order.",
        "Move the selected module one position down in the execution order."),
"al_SQ":("Lëvizni modulin e zgjedhur një pozicion lart në renditje.",
         "Lëvizni modulin e zgjedhur një pozicion poshtë në renditje."),
"ar":("انقل الوحدة المحددة موضعًا واحدًا للأعلى في ترتيب التنفيذ.",
       "انقل الوحدة المحددة موضعًا واحدًا للأسفل في ترتيب التنفيذ."),
"da":("Flyt det valgte modul én plads op i rækkefølgen.",
        "Flyt det valgte modul én plads ned i rækkefølgen."),
"de":("Verschiebt das ausgewählte Modul um eine Position nach oben.",
        "Verschiebt das ausgewählte Modul um eine Position nach unten."),
"el":("Μετακινεί την επιλεγμένη μονάδα μία θέση επάνω στη σειρά.",
        "Μετακινεί την επιλεγμένη μονάδα μία θέση κάτω στη σειρά."),
"es":("Mueve el módulo seleccionado una posición arriba en el orden de ejecución.",
       "Mueve el módulo seleccionado una posición abajo en el orden de ejecución."),
"et":("Liiguta valitud moodul täitmisjärjestuses ühe koha võrra üles.",
        "Liiguta valitud moodul täitmisjärjestuses ühe koha võrra alla."),
"fr":("Déplace le module sélectionné d’un rang vers le haut.",
        "Déplace le module sélectionné d’un rang vers le bas."),
"hr":("Pomakni odabrani modul jedno mjesto gore u redoslijedu.",
        "Pomakni odabrani modul jedno mjesto dolje u redoslijedu."),
"it":("Sposta il modulo selezionato di una posizione verso l’alto.",
        "Sposta il modulo selezionato di una posizione verso il basso."),
"nl":("Verplaats de geselecteerde module één plaats omhoog in de volgorde.",
        "Verplaats de geselecteerde module één plaats omlaag in de volgorde."),
"pt":("Move o módulo selecionado uma posição acima na ordem de execução.",
        "Move o módulo selecionado uma posição abaixo na ordem de execução."),
"pt_BR":("Move o módulo selecionado uma posição acima na ordem de execução.",
        "Move o módulo selecionado uma posição abaixo na ordem de execução.")
}
MODULE_MOVES["en_US"]=MODULE_MOVES["base"]
MODULE_MOVES["es_AR"]=MODULE_MOVES["es"]
MODULE_MOVES["es_MX"]=MODULE_MOVES["es"]
assert set(MODULE_MOVES)==set(TRANSLATIONS)
for locale,(up,down) in MODULE_MOVES.items():
 TRANSLATIONS[locale].update({
     "tooltip.nodes.module.up":up,
     "tooltip.nodes.module.down":down,
 })
