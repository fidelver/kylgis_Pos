"""Configuration tabs, rendered after NetBeans initComponents().
Human-reviewed native phrases for each locale, without translating stored data.
"""
TAB_KEYS=("general","locale","payment","peripherals","system","ticket","company","database","sales_buttons","platforms","nodes")
TAB_TITLES={
"base":("General","Language and region","Payment methods","Peripherals","System options","Receipt setup","Company","Database setup","Sales buttons","Platforms","Nodes"),
"al_SQ":("Të përgjithshme","Gjuha dhe rajoni","Mënyrat e pagesës","Pajisjet periferike","Opsionet e sistemit","Konfigurimi i kuponit","Kompania","Konfigurimi i bazës së të dhënave","Butonat e shitjes","Platformat","Nyjet"),
"ar":("عام","اللغة والمنطقة","طرق الدفع","الأجهزة الطرفية","خيارات النظام","إعداد الإيصالات","الشركة","إعداد قاعدة البيانات","أزرار البيع","المنصات","العُقد"),
"da":("Generelt","Sprog og region","Betalingsmetoder","Eksterne enheder","Systemindstillinger","Kvitteringsopsætning","Firma","Databaseopsætning","Salgsknapper","Platforme","Noder"),
"de":("Allgemein","Sprache und Region","Zahlungsarten","Peripheriegeräte","Systemoptionen","Boneinstellungen","Unternehmen","Datenbankeinrichtung","Verkaufsschaltflächen","Plattformen","Knoten"),
"el":("Γενικά","Γλώσσα και περιοχή","Τρόποι πληρωμής","Περιφερειακά","Επιλογές συστήματος","Ρυθμίσεις απόδειξης","Εταιρεία","Ρυθμίσεις βάσης δεδομένων","Κουμπιά πωλήσεων","Πλατφόρμες","Κόμβοι"),
"en_US":None,
"es":("General","Idioma y región","Métodos de pago","Periféricos","Opciones del sistema","Configuración de tickets","Empresa","Configuración de base de datos","Botones de venta","Plataformas","Nodos"),
"es_AR":None,"es_MX":None,
"et":("Üldine","Keel ja piirkond","Makseviisid","Välisseadmed","Süsteemi valikud","Kviitungi seaded","Ettevõte","Andmebaasi seadistus","Müüginupud","Platvormid","Sõlmed"),
"fr":("Général","Langue et région","Modes de paiement","Périphériques","Options du système","Paramètres du reçu","Entreprise","Configuration de la base de données","Boutons de vente","Plateformes","Nœuds"),
"hr":("Općenito","Jezik i regija","Načini plaćanja","Periferni uređaji","Opcije sustava","Postavke računa","Tvrtka","Postavke baze podataka","Prodajni gumbi","Platforme","Čvorovi"),
"it":("Generale","Lingua e area geografica","Metodi di pagamento","Periferiche","Opzioni di sistema","Impostazioni ricevuta","Azienda","Configurazione database","Pulsanti di vendita","Piattaforme","Nodi"),
"nl":("Algemeen","Taal en regio","Betaalmethoden","Randapparaten","Systeemopties","Boninstellingen","Bedrijf","Database-instellingen","Verkoopknoppen","Platforms","Knooppunten"),
"pt":("Geral","Idioma e região","Meios de pagamento","Periféricos","Opções do sistema","Configuração do recibo","Empresa","Configuração da base de dados","Botões de venda","Plataformas","Nós"),
"pt_BR":("Geral","Idioma e região","Formas de pagamento","Periféricos","Opções do sistema","Configuração do comprovante","Empresa","Configuração do banco de dados","Botões de venda","Plataformas","Nós"),
}
TAB_TITLES["en_US"]=TAB_TITLES["base"]
TAB_TITLES["es_AR"]=TAB_TITLES["es"]
TAB_TITLES["es_MX"]=TAB_TITLES["es"]
assert len(TAB_TITLES)==17
assert all(len(v)==len(TAB_KEYS) for v in TAB_TITLES.values())
TRANSLATIONS={locale:{"text.config.tab."+key:title for key,title in zip(TAB_KEYS,titles)}
              for locale,titles in TAB_TITLES.items()}
