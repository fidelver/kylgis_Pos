"""Native role labels and contextual permission tooltips for node topology."""
ROLES=("master","server","pos","kitchen","printer_service","scale_service",
       "scanner_service","display_service","remote_sessions")
LANG={
"base":("Master","Configuration server","Point of sale","Kitchen display",
"Print service","Scale service","Scanner service","Customer display service","Remote sessions"),
"al_SQ":("Kryesori","Serveri i konfigurimit","Pika e shitjes","Ekrani i kuzhinës",
"Shërbimi i printimit","Shërbimi i peshores","Shërbimi i skanerit","Shërbimi i ekranit të klientit","Sesionet në distancë"),
"ar":("العقدة الرئيسية","خادم الإعدادات","نقطة البيع","شاشة المطبخ",
"خدمة الطباعة","خدمة الميزان","خدمة الماسح","خدمة شاشة العميل","الجلسات البعيدة"),
"da":("Master","Konfigurationsserver","Salgssted","Køkkenskærm",
"Udskrivningstjeneste","Vægttjeneste","Scannertjeneste","Kundedisplaytjeneste","Fjernsessioner"),
"de":("Master","Konfigurationsserver","Kassenplatz","Küchenanzeige",
"Druckdienst","Waagendienst","Scannerdienst","Kundendisplaydienst","Remotesitzungen"),
"el":("Κύριος κόμβος","Διακομιστής ρυθμίσεων","Σημείο πώλησης","Οθόνη κουζίνας",
"Υπηρεσία εκτύπωσης","Υπηρεσία ζυγαριάς","Υπηρεσία σαρωτή","Υπηρεσία οθόνης πελάτη","Απομακρυσμένες συνεδρίες"),
"es":("MASTER","Servidor de configuración","Punto de venta","Pantalla de cocina",
"Servicio de impresión","Servicio de báscula","Servicio de escáner","Servicio de visor de cliente","Sesiones remotas"),
"et":("Põhisõlm","Seadistusserver","Müügikoht","Köögiekraan",
"Printimisteenus","Kaaluteenus","Skanneriteenus","Kliendiekraani teenus","Kaugseansid"),
"fr":("Nœud maître","Serveur de configuration","Point de vente","Écran de cuisine",
"Service d’impression","Service de balance","Service de scanner","Service d’afficheur client","Sessions distantes"),
"hr":("Glavni čvor","Konfiguracijski poslužitelj","Prodajno mjesto","Kuhinjski zaslon",
"Usluga ispisa","Usluga vage","Usluga skenera","Usluga zaslona za kupca","Udaljene sesije"),
"it":("Nodo master","Server di configurazione","Punto vendita","Schermo cucina",
"Servizio stampa","Servizio bilancia","Servizio scanner","Servizio display cliente","Sessioni remote"),
"nl":("Hoofdknooppunt","Configuratieserver","Verkooppunt","Keukenscherm",
"Afdrukservice","Weegschaalservice","Scannerservice","Klantendisplayservice","Externe sessies"),
"pt":("Nó principal","Servidor de configuração","Ponto de venda","Ecrã de cozinha",
"Serviço de impressão","Serviço de balança","Serviço de scanner","Serviço de visor do cliente","Sessões remotas"),
"pt_BR":("Nó principal","Servidor de configuração","Ponto de venda","Tela da cozinha",
"Serviço de impressão","Serviço de balança","Serviço de scanner","Serviço de visor do cliente","Sessões remotas")
}
HELP={
"base":"Enable or disable the {0} role for this node; this controls the services assigned to it.",
"al_SQ":"Aktivizoni ose çaktivizoni rolin {0} të nyjës; kjo përcakton shërbimet e caktuara.",
"ar":"فعّل دور {0} لهذه العقدة أو عطّله؛ يحدد هذا الخدمات المعيّنة لها.",
"da":"Aktivér eller deaktivér rollen {0} for noden; det bestemmer dens tildelte tjenester.",
"de":"Aktiviert oder deaktiviert die Rolle {0} für diesen Knoten und damit seine zugewiesenen Dienste.",
"el":"Ενεργοποιήστε ή απενεργοποιήστε τον ρόλο {0} του κόμβου· αυτό καθορίζει τις υπηρεσίες του.",
"es":"Activa o desactiva el rol {0} de este nodo; determina qué servicios tiene asignados.",
"et":"Lülitage sõlme roll {0} sisse või välja; see määrab sõlmele määratud teenused.",
"fr":"Active ou désactive le rôle {0} du nœud et les services qui lui sont attribués.",
"hr":"Uključite ili isključite ulogu {0} čvora; time se određuju dodijeljene usluge.",
"it":"Attiva o disattiva il ruolo {0} del nodo, determinandone i servizi assegnati.",
"nl":"Schakel de rol {0} voor dit knooppunt in of uit; dit bepaalt de toegewezen services.",
"pt":"Ativa ou desativa a função {0} deste nó e os serviços que lhe estão atribuídos.",
"pt_BR":"Ativa ou desativa a função {0} deste nó e os serviços atribuídos a ele."
}
for m in (LANG,HELP):
 m["en_US"]=m["base"]
 m["es_AR"]=m["es"]
 m["es_MX"]=m["es"]
assert len(LANG)==len(HELP)==17
assert all(len(v)==len(ROLES) for v in LANG.values())
TRANSLATIONS={
 locale:{
 **{"text.nodes.role."+key:label for key,label in zip(ROLES,LANG[locale])},
 "tooltip.nodes.role":HELP[locale]
 }
 for locale in LANG}
